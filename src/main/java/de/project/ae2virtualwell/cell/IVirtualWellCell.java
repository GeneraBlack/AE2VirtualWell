package de.project.ae2virtualwell.cell;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public interface IVirtualWellCell extends StorageCell {
    ItemStack getItemStack();

    @Nullable
    ISaveProvider getSaveProvider();

    WellCellTier getTier();

    @Nullable
    Fluid getConfiguredTarget();

    boolean isFull();

    long injectGeneratedFluid(AEFluidKey key, long amount, Actionable mode);
}
