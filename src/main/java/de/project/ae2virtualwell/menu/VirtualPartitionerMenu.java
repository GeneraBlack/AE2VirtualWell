package de.project.ae2virtualwell.menu;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import de.project.ae2virtualwell.cell.VirtualWellCellItem;
import de.project.ae2virtualwell.cell.partition.WellCellPartition;
import de.project.ae2virtualwell.cell.partition.WellCellPartitionList;
import de.project.ae2virtualwell.config.VirtualWellConfig;
import de.project.ae2virtualwell.recipe.WellDropRegistry;
import de.project.ae2virtualwell.registry.ModBlocks;
import de.project.ae2virtualwell.registry.ModDataComponents;
import de.project.ae2virtualwell.registry.ModItems;
import de.project.ae2virtualwell.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VirtualPartitionerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;

    private boolean loadingUpgrades = false;
    private ItemStack lastCellInSlot0 = ItemStack.EMPTY;

    private final SimpleContainer upgradeContainer = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!loadingUpgrades) {
                saveUpgradesToCell();
            }
        }
    };

    public VirtualPartitionerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(1),
                playerInventory.player.level() != null ?
                        ContainerLevelAccess.create(playerInventory.player.level(), extraData.readBlockPos()) :
                        ContainerLevelAccess.NULL);
    }

    public VirtualPartitionerMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access) {
        super(ModMenus.PARTITIONER_MENU.get(), containerId);
        checkContainerSize(container, 1);
        this.container = container;
        this.access = access;
        container.startOpen(playerInventory.player);

        // Slot 0: Cell slot
        this.addSlot(new Slot(container, 0, 16, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof VirtualWellCellItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                loadUpgradesFromCell();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                loadUpgradesFromCell();
            }
        });

        // Slots 1..4: 4 Acceleration Card Slots (indices 0..3 in upgradeContainer)
        // Positioned: x = 52 + i * 18, y = 155
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(this.upgradeContainer, i, 52 + i * 18, 155) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !container.getItem(0).isEmpty() && stack.is(AEItems.SPEED_CARD.asItem());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return !container.getItem(0).isEmpty();
                }
            });
        }

        // Slot 5: 1 Void Secondary Output Card Slot (index 4 in upgradeContainer)
        // Positioned with a gap: x = 138, y = 155
        this.addSlot(new Slot(this.upgradeContainer, 4, 138, 155) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !container.getItem(0).isEmpty() && (
                        stack.is(ModItems.VOID_SECONDARY_CARD.get()) || stack.is(AEItems.VOID_CARD.asItem())
                );
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return !container.getItem(0).isEmpty();
            }
        });

        // Slots 6..32: Player Inventory (3 rows x 9 columns)
        int invStartX = 30;
        int invStartY = 180;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invStartX + col * 18, invStartY + row * 18));
            }
        }

        // Slots 33..41: Player Hotbar (9 slots)
        int hotbarStartY = 238;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, hotbarStartY));
        }

        // Initialize upgradeContainer from the initial cell if present
        loadUpgradesFromCell();
    }

    private void loadUpgradesFromCell() {
        loadingUpgrades = true;
        try {
            ItemStack cell = container.getItem(0);
            if (cell.isEmpty() || !(cell.getItem() instanceof VirtualWellCellItem)) {
                for (int i = 0; i < 5; i++) {
                    upgradeContainer.setItem(i, ItemStack.EMPTY);
                }
            } else {
                var upgrades = UpgradeInventories.forItem(cell, 5);
                int speedSlot = 0;
                ItemStack voidCard = ItemStack.EMPTY;

                for (int i = 0; i < 4; i++) {
                    upgradeContainer.setItem(i, ItemStack.EMPTY);
                }
                upgradeContainer.setItem(4, ItemStack.EMPTY);

                for (int i = 0; i < upgrades.size(); i++) {
                    ItemStack card = upgrades.getStackInSlot(i);
                    if (!card.isEmpty()) {
                        if (card.is(AEItems.SPEED_CARD.asItem())) {
                            if (speedSlot < 4) {
                                upgradeContainer.setItem(speedSlot++, card.copy());
                            }
                        } else if (card.is(ModItems.VOID_SECONDARY_CARD.get()) || card.is(AEItems.VOID_CARD.asItem())) {
                            if (voidCard.isEmpty()) {
                                voidCard = card.copy();
                            }
                        }
                    }
                }
                upgradeContainer.setItem(4, voidCard);
            }
        } finally {
            loadingUpgrades = false;
        }
    }

    public void saveUpgradesToCell() {
        ItemStack cell = container.getItem(0);
        if (!cell.isEmpty() && cell.getItem() instanceof VirtualWellCellItem) {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                ItemStack stack = upgradeContainer.getItem(i);
                if (!stack.isEmpty()) {
                    list.add(stack.copy());
                }
            }
            if (list.isEmpty()) {
                cell.remove(AEComponents.UPGRADES);
            } else {
                cell.set(AEComponents.UPGRADES, net.minecraft.world.item.component.ItemContainerContents.fromItems(list));
            }
            container.setChanged();
            Slot cellSlot = this.slots.get(0);
            if (cellSlot != null) {
                cellSlot.setChanged();
            }
            broadcastChanges();
        }
    }

    public Container getPartitionerContainer() {
        return container;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.VIRTUAL_PARTITIONER.get());
    }

    @Override
    public void broadcastChanges() {
        ItemStack currentCell = container.getItem(0);
        if (!ItemStack.matches(currentCell, lastCellInSlot0)) {
            lastCellInSlot0 = currentCell.copy();
            loadUpgradesFromCell();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == 0) {
                // Move cell to player inventory / hotbar (slots 6..42)
                if (!this.moveItemStackTo(stackInSlot, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index <= 5) {
                // Move upgrade to player inventory / hotbar (slots 6..42)
                if (!this.moveItemStackTo(stackInSlot, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From inventory / hotbar (index >= 6)
                if (stackInSlot.getItem() instanceof VirtualWellCellItem) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stackInSlot.is(AEItems.SPEED_CARD.asItem())) {
                    // Try moving to acceleration slots (1..5)
                    if (!this.moveItemStackTo(stackInSlot, 1, 5, false)) {
                        if (index >= 6 && index < 33) {
                            if (!this.moveItemStackTo(stackInSlot, 33, 42, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (index >= 33 && index < 42) {
                            if (!this.moveItemStackTo(stackInSlot, 6, 33, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                } else if (stackInSlot.is(ModItems.VOID_SECONDARY_CARD.get())
                        || stackInSlot.is(AEItems.VOID_CARD.asItem())) {
                    // Try moving to void slot (5..6)
                    if (!this.moveItemStackTo(stackInSlot, 5, 6, false)) {
                        if (index >= 6 && index < 33) {
                            if (!this.moveItemStackTo(stackInSlot, 33, 42, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (index >= 33 && index < 42) {
                            if (!this.moveItemStackTo(stackInSlot, 6, 33, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                } else if (index >= 6 && index < 33) {
                    if (!this.moveItemStackTo(stackInSlot, 33, 42, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 33 && index < 42) {
                    if (!this.moveItemStackTo(stackInSlot, 6, 33, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public void applyPartitions(Player player, WellCellPartitionList newPartitions) {
        Slot cellSlot = this.slots.get(0);
        if (cellSlot == null || !cellSlot.hasItem()) {
            return;
        }
        ItemStack cellStack = cellSlot.getItem();
        if (!(cellStack.getItem() instanceof VirtualWellCellItem)) {
            return;
        }

        if (newPartitions == null || newPartitions.isEmpty()) {
            cellStack.remove(ModDataComponents.PARTITIONS.get());
            cellStack.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
            cellSlot.setChanged();
            broadcastChanges();
            return;
        }

        // Validate
        List<WellCellPartition> validList = new ArrayList<>();
        Set<Fluid> seen = new HashSet<>();
        int totalPercent = 0;

        for (WellCellPartition p : newPartitions.partitions()) {
            if (p == null || p.target() == null || p.target() == Fluids.EMPTY || p.percent() <= 0) continue;
            Fluid normalized = WellDropRegistry.normalizeFluid(p.target());
            if (!seen.add(normalized)) continue; // No duplicates
            if (!WellDropRegistry.isValidFluidTarget(normalized, player.level())) continue;

            if (VirtualWellConfig.isInventoryCheckEnforced() && !player.isCreative()) {
                if (!VirtualWellCellItem.playerHasFluid(player, normalized)) {
                    continue;
                }
            }

            totalPercent += p.percent();
            validList.add(new WellCellPartition(normalized, p.percent(), p.voidSecondary()));
        }

        if (totalPercent > 100) {
            int sum = 0;
            List<WellCellPartition> adjusted = new ArrayList<>();
            for (WellCellPartition p : validList) {
                int allowed = Math.min(p.percent(), 100 - sum);
                if (allowed > 0) {
                    adjusted.add(new WellCellPartition(p.target(), allowed, p.voidSecondary()));
                    sum += allowed;
                }
            }
            validList = adjusted;
        }

        if (validList.isEmpty()) {
            cellStack.remove(ModDataComponents.PARTITIONS.get());
            cellStack.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
        } else {
            cellStack.set(ModDataComponents.PARTITIONS.get(), new WellCellPartitionList(validList));
            // Keep AE2 config synced with primary target for GUI compatibility
            cellStack.set(AEComponents.STORAGE_CELL_CONFIG_INV,
                    List.of(new GenericStack(AEFluidKey.of(validList.get(0).target()), 1)));
        }

        cellSlot.setChanged();
        broadcastChanges();

        if (player.level() != null && !player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
    }
}
