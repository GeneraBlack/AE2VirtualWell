package de.project.ae2virtualwell.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

public record WellDropEntry(Fluid fluid, int weight, int minMilliBuckets, int maxMilliBuckets) {
    public static final Codec<WellDropEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(WellDropEntry::fluid),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(WellDropEntry::weight),
            Codec.INT.optionalFieldOf("min_milli_buckets", 1000).forGetter(WellDropEntry::minMilliBuckets),
            Codec.INT.optionalFieldOf("max_milli_buckets", 1000).forGetter(WellDropEntry::maxMilliBuckets)
    ).apply(instance, WellDropEntry::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WellDropEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), WellDropEntry::fluid,
            ByteBufCodecs.VAR_INT, WellDropEntry::weight,
            ByteBufCodecs.VAR_INT, WellDropEntry::minMilliBuckets,
            ByteBufCodecs.VAR_INT, WellDropEntry::maxMilliBuckets,
            WellDropEntry::new
    );
}
