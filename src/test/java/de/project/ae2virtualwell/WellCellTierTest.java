package de.project.ae2virtualwell;

import de.project.ae2virtualwell.cell.WellCellTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WellCellTierTest {

    @Test
    public void testTierProperties() {
        assertEquals(5, WellCellTier.values().length);

        // 1k
        assertEquals("1k", WellCellTier.TIER_1K.getTierName());
        assertEquals(1024, WellCellTier.TIER_1K.getTotalBytes());
        assertEquals(8, WellCellTier.TIER_1K.getBytesPerType());
        assertEquals(18, WellCellTier.TIER_1K.getTotalTypes());
        assertEquals(0.5, WellCellTier.TIER_1K.getIdleDrain());

        // 4k
        assertEquals("4k", WellCellTier.TIER_4K.getTierName());
        assertEquals(4096, WellCellTier.TIER_4K.getTotalBytes());
        assertEquals(32, WellCellTier.TIER_4K.getBytesPerType());
        assertEquals(18, WellCellTier.TIER_4K.getTotalTypes());
        assertEquals(1.0, WellCellTier.TIER_4K.getIdleDrain());

        // 16k
        assertEquals("16k", WellCellTier.TIER_16K.getTierName());
        assertEquals(16384, WellCellTier.TIER_16K.getTotalBytes());
        assertEquals(128, WellCellTier.TIER_16K.getBytesPerType());
        assertEquals(18, WellCellTier.TIER_16K.getTotalTypes());
        assertEquals(2.0, WellCellTier.TIER_16K.getIdleDrain());

        // 64k
        assertEquals("64k", WellCellTier.TIER_64K.getTierName());
        assertEquals(65536, WellCellTier.TIER_64K.getTotalBytes());
        assertEquals(512, WellCellTier.TIER_64K.getBytesPerType());
        assertEquals(18, WellCellTier.TIER_64K.getTotalTypes());
        assertEquals(4.0, WellCellTier.TIER_64K.getIdleDrain());

        // 256k
        assertEquals("256k", WellCellTier.TIER_256K.getTierName());
        assertEquals(262144, WellCellTier.TIER_256K.getTotalBytes());
        assertEquals(2048, WellCellTier.TIER_256K.getBytesPerType());
        assertEquals(18, WellCellTier.TIER_256K.getTotalTypes());
        assertEquals(8.0, WellCellTier.TIER_256K.getIdleDrain());
    }
}
