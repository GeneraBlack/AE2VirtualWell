package de.project.ae2virtualwell.network;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.blockentities.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import de.project.ae2virtualwell.cell.IVirtualWellCell;
import de.project.ae2virtualwell.cell.partition.WellCellPartition;
import de.project.ae2virtualwell.cell.partition.WellCellPartitionList;
import de.project.ae2virtualwell.config.VirtualWellConfig;
import de.project.ae2virtualwell.recipe.WellDropEntry;
import de.project.ae2virtualwell.recipe.WellDropRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VirtualWellGridService implements IGridServiceProvider, IVirtualWellGridService {

    private final IGrid grid;
    private int tickCounter = 0;
    private final Map<Integer, Integer> cellProgress = new HashMap<>();

    public VirtualWellGridService(IGrid grid) {
        this.grid = grid;
    }

    @Override
    public void onLevelEndTick(Level level) {
        if (level.isClientSide()) {
            return;
        }

        tickCounter++;
        if (tickCounter % 5 != 0) {
            return;
        }

        IEnergyService energyService = grid.getEnergyService();
        boolean requireEnergy = VirtualWellConfig.REQUIRE_AE_ENERGY.get();
        if (requireEnergy && !energyService.isNetworkPowered()) {
            return;
        }

        boolean altered = false;
        RandomSource random = level.getRandom();
        Set<IChestOrDrive> visitedDrives = new HashSet<>();

        for (IGridNode node : grid.getNodes()) {
            if (!node.isActive()) {
                continue;
            }
            if (node.getOwner() instanceof IChestOrDrive drive && visitedDrives.add(drive)) {
                if (!drive.isPowered()) {
                    continue;
                }

                for (int i = 0; i < drive.getCellCount(); i++) {
                    StorageCell cell = drive.getOriginalCellInventory(i);
                    if (cell instanceof IVirtualWellCell wellCell) {
                        altered |= tickCell(wellCell, level, energyService, requireEnergy, random);
                    }
                }
            }
        }

        if (altered) {
            grid.getStorageService().invalidateCache();
        }
    }

    private boolean tickCell(IVirtualWellCell wellCell, Level level, IEnergyService energyService, boolean requireEnergy, RandomSource random) {
        IUpgradeInventory upgrades = UpgradeInventories.forItem(wellCell.getItemStack(), 5);
        int speedCards = Math.min(4, upgrades.getInstalledUpgrades(AEItems.SPEED_CARD.asItem()));
        int baseInterval = VirtualWellConfig.BASE_TICK_INTERVAL.get();

        int targetInterval = switch (speedCards) {
            case 1 -> (int) (baseInterval * 0.70);
            case 2 -> (int) (baseInterval * 0.45);
            case 3 -> (int) (baseInterval * 0.30);
            case 4 -> Math.max(10, (int) (baseInterval * 0.20));
            default -> baseInterval;
        };

        int key = System.identityHashCode(wellCell.getItemStack());
        int progress = cellProgress.getOrDefault(key, 0) + 5;
        if (progress >= targetInterval) {
            cellProgress.put(key, 0);
            return processCell(wellCell, level, energyService, requireEnergy, random, speedCards, upgrades);
        } else {
            cellProgress.put(key, progress);
            return false;
        }
    }

    private boolean processCell(IVirtualWellCell wellCell, Level level, IEnergyService energyService, boolean requireEnergy, RandomSource random, int speedCards, IUpgradeInventory upgrades) {
        // 1. If whole cell is full, stop immediately
        if (wellCell.isFull() || wellCell.getStatus() == CellState.FULL) {
            return false;
        }

        WellCellPartitionList partitionList = wellCell.getPartitions();
        if (partitionList.isEmpty()) {
            return false;
        }

        int totalMilliBuckets = wellCell.getTier().getGenerationMilliBuckets();
        if (totalMilliBuckets <= 0) {
            return false;
        }

        double baseEnergy = VirtualWellConfig.ENERGY_PER_BUCKET.get();
        double energyMultiplier = Math.pow(1.5, speedCards);
        double energyPerBucket = baseEnergy * energyMultiplier;
        boolean anyInserted = false;

        boolean globalVoidSecondary = de.project.ae2virtualwell.util.VirtualCellAdapter.hasVoidSecondaryCard(upgrades);

        int remainingMb = totalMilliBuckets;

        while (remainingMb > 0) {
            if (wellCell.isFull() || wellCell.getStatus() == CellState.FULL) {
                break;
            }

            // Weighted selection across partitions (0 to 99)
            int roll = random.nextInt(100);
            int cumulative = 0;
            WellCellPartition selectedPartition = null;

            for (WellCellPartition p : partitionList.partitions()) {
                cumulative += p.percent();
                if (roll < cumulative) {
                    selectedPartition = p;
                    break;
                }
            }

            if (selectedPartition == null) {
                remainingMb -= Math.min(remainingMb, 1000);
                continue;
            }

            if (wellCell.isPartitionFull(selectedPartition)) {
                remainingMb -= Math.min(remainingMb, 1000);
                continue;
            }

            Fluid target = selectedPartition.target();
            if (target == null || !WellDropRegistry.isValidFluidTarget(target, level)) {
                remainingMb -= Math.min(remainingMb, 1000);
                continue;
            }

            List<WellDropEntry> dropEntries = WellDropRegistry.getDropEntries(target, level, wellCell.getTier());
            if (dropEntries.isEmpty()) {
                remainingMb -= Math.min(remainingMb, 1000);
                continue;
            }

            WellDropRegistry.RolledDrop rolled = WellDropRegistry.rollDropWithIndex(dropEntries, random);
            if (rolled.isEmpty()) {
                remainingMb -= Math.min(remainingMb, 1000);
                continue;
            }

            int rolledMb = Math.max(1, rolled.amount());
            int stepMb = Math.min(remainingMb, rolledMb);

            // Check if this drop is a secondary byproduct
            boolean isSecondary = rolled.isSecondary();
            boolean voidThisSecondary = globalVoidSecondary && selectedPartition.voidSecondary();

            if (voidThisSecondary && isSecondary) {
                if (requireEnergy && energyPerBucket > 0) {
                    double energyNeeded = (stepMb / 1000.0) * energyPerBucket;
                    energyService.extractAEPower(energyNeeded, Actionable.MODULATE, PowerMultiplier.CONFIG);
                }
                remainingMb -= stepMb;
                continue;
            }

            AEFluidKey key = AEFluidKey.of(rolled.fluid());

            long canInsert = wellCell.injectGeneratedFluid(key, stepMb, Actionable.SIMULATE);
            if (canInsert <= 0) {
                remainingMb -= stepMb;
                continue;
            }

            if (requireEnergy && energyPerBucket > 0) {
                double energyNeeded = (canInsert / 1000.0) * energyPerBucket;
                double extracted = energyService.extractAEPower(energyNeeded, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                if (extracted < energyNeeded) {
                    break;
                }
                energyService.extractAEPower(energyNeeded, Actionable.MODULATE, PowerMultiplier.CONFIG);
            }

            long inserted = wellCell.injectGeneratedFluid(key, canInsert, Actionable.MODULATE);
            if (inserted > 0) {
                anyInserted = true;
            }
            remainingMb -= stepMb;
        }

        if (anyInserted) {
            wellCell.persist();
        }

        return anyInserted;
    }
}
