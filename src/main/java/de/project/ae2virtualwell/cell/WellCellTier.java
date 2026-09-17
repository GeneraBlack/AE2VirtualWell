package de.project.ae2virtualwell.cell;

import de.project.ae2virtualwell.config.VirtualWellConfig;

public enum WellCellTier {
    TIER_1K("1k", 1024, 8, 18, 0.5),
    TIER_4K("4k", 4096, 32, 18, 1.0),
    TIER_16K("16k", 16384, 128, 18, 2.0),
    TIER_64K("64k", 65536, 512, 18, 4.0),
    TIER_256K("256k", 262144, 2048, 18, 8.0);

    private final String name;
    private final int totalBytes;
    private final int bytesPerType;
    private final int totalTypes;
    private final double idleDrain;

    WellCellTier(String name, int totalBytes, int bytesPerType, int totalTypes, double idleDrain) {
        this.name = name;
        this.totalBytes = totalBytes;
        this.bytesPerType = bytesPerType;
        this.totalTypes = totalTypes;
        this.idleDrain = idleDrain;
    }

    public String getTierName() {
        return name;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public int getBytesPerType() {
        return bytesPerType;
    }

    public int getTotalTypes() {
        return totalTypes;
    }

    public double getIdleDrain() {
        return idleDrain;
    }

    public int getGenerationMilliBuckets() {
        return switch (this) {
            case TIER_1K -> VirtualWellConfig.TIER_1K_MB.get();
            case TIER_4K -> VirtualWellConfig.TIER_4K_MB.get();
            case TIER_16K -> VirtualWellConfig.TIER_16K_MB.get();
            case TIER_64K -> VirtualWellConfig.TIER_64K_MB.get();
            case TIER_256K -> VirtualWellConfig.TIER_256K_MB.get();
        };
    }
}
