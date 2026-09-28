package de.project.ae2virtualwell.block;

import de.project.ae2virtualwell.cell.VirtualWellCellItem;
import de.project.ae2virtualwell.menu.VirtualPartitionerMenu;
import de.project.ae2virtualwell.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VirtualPartitionerBlockEntity extends BlockEntity implements MenuProvider, Container {
    private ItemStack cellStack = ItemStack.EMPTY;

    public VirtualPartitionerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIRTUAL_PARTITIONER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.ae2virtualwell.virtual_partitioner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VirtualPartitionerMenu(containerId, playerInventory, this,
                this.level != null ? ContainerLevelAccess.create(this.level, this.worldPosition) : ContainerLevelAccess.NULL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.cellStack.isEmpty()) {
            tag.put("CellItem", this.cellStack.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CellItem")) {
            this.cellStack = ItemStack.parseOptional(registries, tag.getCompound("CellItem"));
        } else {
            this.cellStack = ItemStack.EMPTY;
        }
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.cellStack.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.cellStack : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot == 0 && !this.cellStack.isEmpty()) {
            ItemStack split = this.cellStack.split(count);
            if (this.cellStack.isEmpty()) {
                this.cellStack = ItemStack.EMPTY;
            }
            setChanged();
            return split;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) {
            ItemStack item = this.cellStack;
            this.cellStack = ItemStack.EMPTY;
            return item;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            this.cellStack = stack;
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.cellStack = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof VirtualWellCellItem;
    }
}
