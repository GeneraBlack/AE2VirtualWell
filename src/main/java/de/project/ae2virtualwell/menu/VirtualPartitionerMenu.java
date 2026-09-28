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

        // Slot 5: 1 Void Secondary Card Slot (index 4 in upgradeContainer)
        // Positioned: x = 138, y = 155
        this.addSlot(new Slot(this.upgradeContainer, 4, 138, 155) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !container.getItem(0).isEmpty() &&
                        (stack.is(ModItems.VOID_SECONDARY_CARD.get()) || stack.is(AEItems.VOID_CARD.asItem()));
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

        // Slots 33..41: Player Hotbar (9 slots) - shifted down to y=239
        int hotbarStartY = 239;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invStartX + col * 18, hotbarStartY));
        }

        loadUpgradesFromCell();
    }

    private void loadUpgradesFromCell() {
        loadingUpgrades = true;
        ItemStack cell = this.container.getItem(0);
        this.lastCellInSlot0 = cell.copy();

        for (int i = 0; i < 5; i++) {
            this.upgradeContainer.setItem(i, ItemStack.EMPTY);
        }

        if (!cell.isEmpty() && cell.getItem() instanceof VirtualWellCellItem) {
            var upgrades = UpgradeInventories.forItem(cell, 5);
            if (upgrades != null) {
                int speedCount = Math.min(4, upgrades.getInstalledUpgrades(AEItems.SPEED_CARD.asItem()));
                for (int i = 0; i < speedCount; i++) {
                    this.upgradeContainer.setItem(i, new ItemStack(AEItems.SPEED_CARD.asItem()));
                }

                if (upgrades.isInstalled(ModItems.VOID_SECONDARY_CARD.get())) {
                    this.upgradeContainer.setItem(4, new ItemStack(ModItems.VOID_SECONDARY_CARD.get()));
                } else if (upgrades.isInstalled(AEItems.VOID_CARD.asItem())) {
                    this.upgradeContainer.setItem(4, new ItemStack(AEItems.VOID_CARD.asItem()));
                }
            }
        }
        loadingUpgrades = false;
    }

    private void saveUpgradesToCell() {
        ItemStack cell = this.container.getItem(0);
        if (cell.isEmpty() || !(cell.getItem() instanceof VirtualWellCellItem)) {
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
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        ItemStack currentCell = this.container.getItem(0);
        if (!ItemStack.matches(currentCell, this.lastCellInSlot0)) {
            loadUpgradesFromCell();
        }
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
                if (slotStack.getItem() instanceof VirtualWellCellItem) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.is(AEItems.SPEED_CARD.asItem()) && !this.container.getItem(0).isEmpty()) {
                    // Try to insert into one of the 4 speed card slots (1..4)
                    if (!this.moveItemStackTo(slotStack, 1, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if ((slotStack.is(ModItems.VOID_SECONDARY_CARD.get()) || slotStack.is(AEItems.VOID_CARD.asItem())) && !this.container.getItem(0).isEmpty()) {
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

    public void applyPartitions(Player player, WellCellPartitionList partitionList) {
        ItemStack cell = this.container.getItem(0);
        if (cell.isEmpty() || !(cell.getItem() instanceof VirtualWellCellItem)) {
            return;
        }

        if (partitionList.isEmpty()) {
            cell.remove(ModDataComponents.PARTITIONS.get());
            cell.remove(AEComponents.STORAGE_CELL_CONFIG_INV);
            this.container.setChanged();
            if (player.level() != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            return;
        }

        // Validate partitions
        int totalPercent = 0;
        Set<Fluid> seen = new HashSet<>();
        List<WellCellPartition> validList = new ArrayList<>();
        boolean voidCardInstalled = hasVoidSecondaryCardInstalled(cell);

        for (WellCellPartition p : partitionList.partitions()) {
            Fluid fluid = p.target();
            if (fluid == null || fluid == Fluids.EMPTY || seen.contains(fluid)) {
                continue;
            }

            // Reject invalid fluids
            if (!WellDropRegistry.isValidFluidTarget(fluid, player.level())) {
                continue;
            }

            // If inventory possession check is active, verify player holds this fluid
            if (VirtualWellConfig.isInventoryCheckEnforced() && !player.isCreative()) {
                if (!VirtualWellCellItem.playerHasFluid(player, fluid)) {
                    continue;
                }
            }

            seen.add(fluid);
            int percent = Math.max(1, Math.min(100, p.percent()));
            totalPercent += percent;

            boolean voidSecondary = voidCardInstalled && p.voidSecondary();
            validList.add(new WellCellPartition(fluid, percent, voidSecondary));
        }

        if (totalPercent > 100 || validList.isEmpty()) {
            return;
        }

        WellCellPartitionList cleanList = new WellCellPartitionList(validList);
        cell.set(ModDataComponents.PARTITIONS.get(), cleanList);

        // Sync legacy AE2 config inv with the primary partition's fluid key
        Fluid primaryFluid = validList.get(0).target();
        cell.set(AEComponents.STORAGE_CELL_CONFIG_INV, List.of(new GenericStack(AEFluidKey.of(primaryFluid), 1)));

        this.container.setChanged();

        if (player.level() != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.5f);
        }
    }

    private boolean hasVoidSecondaryCardInstalled(ItemStack cell) {
        if (this.upgradeContainer.getItem(4).is(ModItems.VOID_SECONDARY_CARD.get())
                || this.upgradeContainer.getItem(4).is(AEItems.VOID_CARD.asItem())) {
            return true;
        }
        var upgrades = UpgradeInventories.forItem(cell, 5);
        return upgrades != null && (upgrades.isInstalled(ModItems.VOID_SECONDARY_CARD.get())
                || upgrades.isInstalled(AEItems.VOID_CARD.asItem()));
    }
}
