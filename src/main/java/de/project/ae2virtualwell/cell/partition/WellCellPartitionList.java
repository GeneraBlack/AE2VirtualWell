package de.project.ae2virtualwell.cell.partition;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record WellCellPartitionList(List<WellCellPartition> partitions) {

    public static final WellCellPartitionList EMPTY = new WellCellPartitionList(List.of());

    public static final Codec<WellCellPartitionList> CODEC =
            WellCellPartition.CODEC.listOf().xmap(WellCellPartitionList::new, WellCellPartitionList::partitions);

    public static final StreamCodec<RegistryFriendlyByteBuf, WellCellPartitionList> STREAM_CODEC =
            WellCellPartition.STREAM_CODEC.apply(ByteBufCodecs.list()).map(WellCellPartitionList::new, WellCellPartitionList::partitions);

    public boolean isEmpty() {
        return partitions == null || partitions.isEmpty();
    }

    public int size() {
        return partitions == null ? 0 : partitions.size();
    }

    public int getTotalPercent() {
        if (partitions == null) return 0;
        int total = 0;
        for (WellCellPartition p : partitions) {
            total += p.percent();
        }
        return total;
    }

    public int getUnallocatedPercent() {
        return Math.max(0, 100 - getTotalPercent());
    }

    @Nullable
    public WellCellPartition getPartition(Fluid fluid) {
        if (partitions == null) return null;
        for (WellCellPartition p : partitions) {
            if (p.target() == fluid) {
                return p;
            }
        }
        return null;
    }

    public boolean contains(Fluid fluid) {
        return getPartition(fluid) != null;
    }

    public long getAllocatedByteLimit(WellCellPartition partition, long totalBytes) {
        if (partition == null || partition.percent() <= 0) {
            return 0L;
        }
        return (totalBytes * partition.percent()) / 100L;
    }

    public WellCellPartitionList withAddedOrReplaced(WellCellPartition newPartition) {
        List<WellCellPartition> updated = new ArrayList<>();
        boolean replaced = false;
        for (WellCellPartition p : (partitions == null ? List.<WellCellPartition>of() : partitions)) {
            if (p.target() == newPartition.target()) {
                updated.add(newPartition);
                replaced = true;
            } else {
                updated.add(p);
            }
        }
        if (!replaced) {
            updated.add(newPartition);
        }
        return new WellCellPartitionList(List.copyOf(updated));
    }
}
