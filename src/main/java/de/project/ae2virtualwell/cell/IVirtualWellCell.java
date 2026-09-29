package de.project.ae2virtualwell.cell;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import de.project.ae2virtualwell.cell.partition.WellCellPartition;
import de.project.ae2virtualwell.cell.partition.WellCellPartitionList;
import de.project.ae2virtualwell.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IVirtualWellCell extends StorageCell {
    ItemStack getItemStack();

    @Nullable
    ISaveProvider getSaveProvider();

    WellCellTier getTier();

    @Nullable
    Fluid getConfiguredTarget();

    boolean isFull();

    long injectGeneratedFluid(AEFluidKey key, long amount, Actionable mode);

    void persist();

    default WellCellPartitionList getPartitions() {
        ItemStack stack = getItemStack();
        if (stack.has(ModDataComponents.PARTITIONS.get())) {
            WellCellPartitionList list = stack.get(ModDataComponents.PARTITIONS.get());
            if (list != null && !list.isEmpty()) {
                return list;
            }
        }
        Fluid single = getConfiguredTarget();
        if (single != null) {
            return new WellCellPartitionList(List.of(new WellCellPartition(single, 100, true)));
        }
        return WellCellPartitionList.EMPTY;
    }

    default long getStoredAmountForFluid(Fluid target) {
        return 0;
    }

    default boolean isPartitionFull(WellCellPartition partition) {
        if (isFull()) {
            return true;
        }
        if (partition == null || partition.percent() <= 0) {
            return true;
        }
        long allocatedBytes = (getTier().getTotalBytes() * partition.percent()) / 100L;
        long storedAmount = getStoredAmountForFluid(partition.target());
        long bytesForFluid = (storedAmount + 8000L - 1L) / 8000L + (long) getTier().getBytesPerType();
        return bytesForFluid >= allocatedBytes;
    }
}
