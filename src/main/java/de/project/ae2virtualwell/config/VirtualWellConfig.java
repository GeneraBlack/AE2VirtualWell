package de.project.ae2virtualwell.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VirtualWellConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue BASE_TICK_INTERVAL;
    public static final ModConfigSpec.DoubleValue ENERGY_PER_BUCKET;
    public static final ModConfigSpec.BooleanValue REQUIRE_AE_ENERGY;

    public static final ModConfigSpec.IntValue TIER_1K_MB;
    public static final ModConfigSpec.IntValue TIER_4K_MB;
    public static final ModConfigSpec.IntValue TIER_16K_MB;
    public static final ModConfigSpec.IntValue TIER_64K_MB;
    public static final ModConfigSpec.IntValue TIER_256K_MB;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("General Virtual Well Settings").push("general");

        BASE_TICK_INTERVAL = builder
                .comment("Interval in world ticks between liquid generation cycles (20 ticks = 1 second, default 60 = 3 seconds)")
                .defineInRange("baseTickInterval", 60, 1, 72000);

        REQUIRE_AE_ENERGY = builder
                .comment("Whether generating liquids requires AE energy from the ME Network")
                .define("requireAeEnergy", true);

        ENERGY_PER_BUCKET = builder
                .comment("AE energy consumed per 1,000 mB (1 Bucket) of liquid generated")
                .defineInRange("energyPerBucket", 10.0, 0.0, 100000.0);

        builder.pop();

        builder.comment("Tier Liquid Yield Amounts (in millibuckets, 1,000 mB = 1 Bucket)").push("tiers");

        TIER_1K_MB = builder
                .comment("Liquid yield per cycle in mB for 1k Virtual Well Storage Cell (Default: 1,000 mB = 1 Bucket)")
                .defineInRange("tier1kMilliBuckets", 1000, 1, 64000);

        TIER_4K_MB = builder
                .comment("Liquid yield per cycle in mB for 4k Virtual Well Storage Cell (Default: 4,000 mB = 4 Buckets)")
                .defineInRange("tier4kMilliBuckets", 4000, 1, 256000);

        TIER_16K_MB = builder
                .comment("Liquid yield per cycle in mB for 16k Virtual Well Storage Cell (Default: 16,000 mB = 16 Buckets)")
                .defineInRange("tier16kMilliBuckets", 16000, 1, 1024000);

        TIER_64K_MB = builder
                .comment("Liquid yield per cycle in mB for 64k Virtual Well Storage Cell (Default: 64,000 mB = 64 Buckets)")
                .defineInRange("tier64kMilliBuckets", 64000, 1, 4096000);

        TIER_256K_MB = builder
                .comment("Liquid yield per cycle in mB for 256k Virtual Well Storage Cell (Default: 256,000 mB = 256 Buckets)")
                .defineInRange("tier256kMilliBuckets", 256000, 1, 16384000);

        builder.pop();

        SPEC = builder.build();
    }
}
