package de.project.ae2virtualwell.cell.partition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

public record WellCellPartition(
        Fluid target,
        int percent,
        boolean voidSecondary
) {
    public static final Codec<WellCellPartition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("target").forGetter(WellCellPartition::target),
            Codec.INT.fieldOf("percent").forGetter(WellCellPartition::percent),
            Codec.BOOL.optionalFieldOf("void_secondary", false).forGetter(WellCellPartition::voidSecondary)
    ).apply(instance, WellCellPartition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WellCellPartition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), WellCellPartition::target,
            ByteBufCodecs.VAR_INT, WellCellPartition::percent,
            ByteBufCodecs.BOOL, WellCellPartition::voidSecondary,
            WellCellPartition::new
    );
}
