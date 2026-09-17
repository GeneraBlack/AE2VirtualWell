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
import de.project.ae2virtualwell.cell.IVirtualWellCell;
import de.project.ae2virtualwell.config.VirtualWellConfig;
import de.project.ae2virtualwell.recipe.WellDropEntry;
import de.project.ae2virtualwell.recipe.WellDropRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VirtualWellGridService implements IGridServiceProvider, IVirtualWellGridService {

    private final IGrid grid;
    private int tickCounter = 0;

    public VirtualWellGridService(IGrid grid) {
        this.grid = grid;
    }

    @Override
    public void onLevelEndTick(Level level) {
        if (level.isClientSide()) {
            return;
        }

        tickCounter++;
        int interval = VirtualWellConfig.BASE_TICK_INTERVAL.get();
        if (tickCounter < interval) {
            return;
        }
        tickCounter = 0;

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
                        altered |= processCell(wellCell, level, energyService, requireEnergy, random);
                    }
                }
            }
        }

        if (altered) {
            grid.getStorageService().invalidateCache();
        }
    }

    private boolean processCell(IVirtualWellCell wellCell, Level level, IEnergyService energyService, boolean requireEnergy, RandomSource random) {
        // 1. If cell is full, stop immediately and do not generate or consume power
        if (wellCell.isFull() || wellCell.getStatus() == CellState.FULL) {
            return false;
        }

        Fluid target = wellCell.getConfiguredTarget();
        if (target == null) {
            return false;
        }

        List<WellDropEntry> dropEntries = WellDropRegistry.getDropEntries(target, level);
        if (dropEntries.isEmpty()) {
            return false;
        }

        int totalMilliBuckets = wellCell.getTier().getGenerationMilliBuckets();
        if (totalMilliBuckets <= 0) {
            return false;
        }

        double energyPerBucket = VirtualWellConfig.ENERGY_PER_BUCKET.get();
        boolean anyInserted = false;
        int remainingMb = totalMilliBuckets;

        while (remainingMb > 0) {
            if (wellCell.isFull() || wellCell.getStatus() == CellState.FULL) {
                break; // Stop generating, cell is full
            }

            WellDropEntry entry = WellDropRegistry.rollDrop(dropEntries, random);
            if (entry == null || entry.fluid() == null) {
                break;
            }

            int rolledMb = entry.rollAmount(random);
            int stepMb = Math.min(remainingMb, rolledMb);
            AEFluidKey key = AEFluidKey.of(entry.fluid());

            // Test if the cell has space to accept this liquid
            long canInsert = wellCell.injectGeneratedFluid(key, stepMb, Actionable.SIMULATE);
            if (canInsert <= 0) {
                // Cell is full or cannot accept this liquid, stop immediately
                break;
            }

            // Only consume AE power for fluid that actually fits into the cell
            if (requireEnergy && energyPerBucket > 0) {
                double energyNeeded = (canInsert / 1000.0) * energyPerBucket;
                double extracted = energyService.extractAEPower(energyNeeded, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                if (extracted < energyNeeded) {
                    break; // Network ran out of power
                }
                energyService.extractAEPower(energyNeeded, Actionable.MODULATE, PowerMultiplier.CONFIG);
            }

            long inserted = wellCell.injectGeneratedFluid(key, canInsert, Actionable.MODULATE);
            if (inserted > 0) {
                anyInserted = true;
                remainingMb -= inserted;
            } else {
                break;
            }
        }

        if (anyInserted) {
            wellCell.persist();
        }

        return anyInserted;
    }
}
