package de.project.ae2virtualwell.network;

import de.project.ae2virtualwell.AE2VirtualWell;
import de.project.ae2virtualwell.cell.partition.WellCellPartitionList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetPartitionsPayload(WellCellPartitionList partitions) implements CustomPacketPayload {
    public static final Type<SetPartitionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AE2VirtualWell.MODID, "set_partitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPartitionsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    WellCellPartitionList.STREAM_CODEC,
                    SetPartitionsPayload::partitions,
                    SetPartitionsPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
