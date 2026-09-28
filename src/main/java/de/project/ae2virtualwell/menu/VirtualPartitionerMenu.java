package de.project.ae2virtualwell.menu;

import appeng.api.ids.AEComponents;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import de.project.ae2virtualwell.registry.ModBlocks;
import de.project.ae2virtualwell.registry.ModMenus;
import de.project.ae2virtualwell.util.VirtualCellAdapter;
import de.project.ae2virtualwell.util.VirtualCellAdapter.UniversalPartition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VirtualPartitionerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerLevelAccess access;
    private ItemStack lastCellInSlot0 = ItemStack.EMPTY;
    private boolean loadingUpgrades = false;

    public final Container upgradeContainer = new SimpleContainer(5) {
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
                return VirtualCellAdapter.isVirtualStorageCell(stack);
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

        // Slot 5: 1 Void Secondary Card Slot (index 4 in upgradeContainer)
        // Positioned: x = 138, y = 155
        this.addSlot(new Slot(this.upgradeContainer, 4, 138, 155) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !container.getItem(0).isEmpty() && VirtualCellAdapter.isVoidSecondaryCard(stack);
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

        // Slots 6..32: Player Inventory (3 rows of 9) - shifted down to y=181
        int invStartX = 30;
        int invStartY = 181;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invStartX + col * 18, invStartY + row * 18));
            }
        }

        // Slots 33..41: Player Hotbar (1 row of 9) - shifted down to y=239
        int hotbarStartY = 239;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, hotbarStartY));
        }

        loadUpgradesFromCell();
    }

    private void loadUpgradesFromCell() {
        loadingUpgrades = true;
        try {
            ItemStack cell = this.container.getItem(0);
            this.lastCellInSlot0 = cell.copy();

            for (int i = 0; i < 5; i++) {
                this.upgradeContainer.setItem(i, ItemStack.EMPTY);
            }

            if (!cell.isEmpty() && VirtualCellAdapter.isVirtualStorageCell(cell)) {
                var upgrades = UpgradeInventories.forItem(cell, 5);
                if (upgrades != null) {
                    int speedIdx = 0;
                    for (int i = 0; i < upgrades.size(); i++) {
                        ItemStack upgrade = upgrades.getStackInSlot(i);
                        if (upgrade.isEmpty()) continue;
                        if (upgrade.is(AEItems.SPEED_CARD.asItem())) {
                            if (speedIdx < 4) {
                                this.upgradeContainer.setItem(speedIdx++, upgrade.copyWithCount(1));
                            }
                        } else if (VirtualCellAdapter.isVoidSecondaryCard(upgrade)) {
                            this.upgradeContainer.setItem(4, upgrade.copyWithCount(1));
                        }
                    }
                }
            }
        } finally {
            loadingUpgrades = false;
        }
    }

    private void saveUpgradesToCell() {
        ItemStack cell = this.container.getItem(0);
        if (cell.isEmpty() || !VirtualCellAdapter.isVirtualStorageCell(cell)) {
            return;
        }

        List<ItemStack> upgradeStacks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = this.upgradeContainer.getItem(i);
            if (!stack.isEmpty()) {
                upgradeStacks.add(stack.copy());
            }
        }

        if (upgradeStacks.isEmpty()) {
            cell.remove(AEComponents.UPGRADES);
        } else {
            cell.set(AEComponents.UPGRADES, net.minecraft.world.item.component.ItemContainerContents.fromItems(upgradeStacks));
        }
        this.container.setChanged();
        Slot cellSlot = this.slots.get(0);
        if (cellSlot != null) {
            cellSlot.setChanged();
        }
        broadcastChanges();
    }

    @Override
    public void broadcastChanges() {
        ItemStack currentCell = this.container.getItem(0);
        if (!ItemStack.matches(currentCell, this.lastCellInSlot0)) {
            loadUpgradesFromCell();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index == 0) {
                // Moving cell out of partitioner to player inventory
                if (!this.moveItemStackTo(slotStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else if (index >= 1 && index <= 5) {
                // Moving upgrade cards out of upgrade slots to player inventory
                if (!this.moveItemStackTo(slotStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else {
                // Player inventory slots
                if (VirtualCellAdapter.isVirtualStorageCell(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(AEItems.SPEED_CARD.asItem()) && !this.container.getItem(0).isEmpty()) {
                    // Try to insert into one of the 4 speed card slots (1..4)
                    if (!this.moveItemStackTo(slotStack, 1, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (VirtualCellAdapter.isVoidSecondaryCard(slotStack) && !this.container.getItem(0).isEmpty()) {
                    // Try to insert into void secondary slot (5)
                    if (!this.moveItemStackTo(slotStack, 5, 6, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 6 && index < 33) {
                    if (!this.moveItemStackTo(slotStack, 33, 42, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 33 && index < 42) {
                    if (!this.moveItemStackTo(slotStack, 6, 33, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.VIRTUAL_PARTITIONER.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public void applyPartitions(Player player, List<UniversalPartition> newPartitions) {
        ItemStack cell = this.container.getItem(0);
        if (cell.isEmpty() || !VirtualCellAdapter.isVirtualStorageCell(cell)) {
            return;
        }

        if (newPartitions == null || newPartitions.isEmpty()) {
            VirtualCellAdapter.writePartitions(cell, List.of());
            this.container.setChanged();
            if (player.level() != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            return;
        }

        // Validate partitions
        int totalPercent = 0;
        Set<ResourceLocation> seen = new HashSet<>();
        List<UniversalPartition> validList = new ArrayList<>();
        boolean voidCardInstalled = VirtualCellAdapter.hasVoidCardInstalled(this.upgradeContainer, cell);
        boolean isFluid = VirtualCellAdapter.isFluidCell(cell);

        for (UniversalPartition p : newPartitions) {
            ResourceLocation targetId = p.targetId();
            if (targetId == null || seen.contains(targetId)) {
                continue;
            }

            // Reject invalid targets
            if (!VirtualCellAdapter.isValidTarget(cell, targetId, player.level())) {
                continue;
            }

            seen.add(targetId);
            int percent = Math.max(1, Math.min(100, p.percent()));
            totalPercent += percent;

            boolean voidSecondary = voidCardInstalled && p.voidSecondary();
            validList.add(new UniversalPartition(targetId, isFluid, percent, voidSecondary));
        }

        if (totalPercent > 100 || validList.isEmpty()) {
            return;
        }

        VirtualCellAdapter.writePartitions(cell, validList);
        this.container.setChanged();

        if (player.level() != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.5f);
        }
    }
}
