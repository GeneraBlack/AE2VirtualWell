package de.project.ae2virtualwell;

import de.project.ae2virtualwell.cell.WellCellTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WellStorageCalculationsTest {

    private static final long AMOUNT_PER_BYTE = 8000L; // 8,000 mB per byte

    private static long computeUsedBytes(long storedFluidAmount, int storedTypes, int bytesPerType) {
        long bytesForFluid = (storedFluidAmount + AMOUNT_PER_BYTE - 1L) / AMOUNT_PER_BYTE;
        return (long) storedTypes * bytesPerType + bytesForFluid;
    }

    private static int computeUnusedFluidAmount(long storedFluidAmount) {
        int rem = (int) (storedFluidAmount % AMOUNT_PER_BYTE);
        return rem == 0 ? 0 : (int) (AMOUNT_PER_BYTE - rem);
    }

    private static long computeRemainingFluidCapacity(long totalBytes, long storedFluidAmount, int storedTypes, int bytesPerType) {
        long usedBytes = computeUsedBytes(storedFluidAmount, storedTypes, bytesPerType);
        long freeBytes = Math.max(0L, totalBytes - usedBytes);
        int unused = computeUnusedFluidAmount(storedFluidAmount);
        return freeBytes * AMOUNT_PER_BYTE + (long) unused;
    }

    @Test
    public void testEmptyCellCalculations() {
        WellCellTier tier = WellCellTier.TIER_1K;
        long usedBytes = computeUsedBytes(0, 0, tier.getBytesPerType());
        assertEquals(0, usedBytes);

        long capacity = computeRemainingFluidCapacity(tier.getTotalBytes(), 0, 0, tier.getBytesPerType());
        // 1024 bytes * 8000 mB/byte = 8,192,000 mB
        assertEquals(1024 * 8000L, capacity);
    }

    @Test
    public void testSingleTypeStorageProgression() {
        WellCellTier tier = WellCellTier.TIER_1K; // 1024 bytes, 8 bytes per type
        int bpt = tier.getBytesPerType();

        // 1 mB stored
        long usedBytes = computeUsedBytes(1, 1, bpt);
        // 1 type * 8 bytes + 1 byte for fluid = 9 bytes used
        assertEquals(9, usedBytes);

        int unused = computeUnusedFluidAmount(1);
        assertEquals(7999, unused);

        long remaining = computeRemainingFluidCapacity(tier.getTotalBytes(), 1, 1, bpt);
        // 1024 - 9 = 1015 free bytes. 1015 * 8000 + 7999 = 8,127,999 mB
        assertEquals(8_127_999L, remaining);
        // Stored + remaining = 8,128,000 mB (exactly 1016 bytes for fluid)
        assertEquals(8_128_000L, 1 + remaining);

        // Exactly 8000 mB stored
        usedBytes = computeUsedBytes(8000, 1, bpt);
        assertEquals(9, usedBytes);
        unused = computeUnusedFluidAmount(8000);
        assertEquals(0, unused);
        remaining = computeRemainingFluidCapacity(tier.getTotalBytes(), 8000, 1, bpt);
        assertEquals(1015 * 8000L, remaining);
        assertEquals(8_128_000L, 8000 + remaining);
    }

    @Test
    public void testFullCellCapacity() {
        WellCellTier tier = WellCellTier.TIER_1K;
        int bpt = tier.getBytesPerType();

        // Max fluid for 1 type: (1024 - 8) * 8000 = 1016 * 8000 = 8,128,000 mB
        long maxFluid = 1016 * 8000L;
        long usedBytes = computeUsedBytes(maxFluid, 1, bpt);
        assertEquals(1024, usedBytes);

        long remaining = computeRemainingFluidCapacity(tier.getTotalBytes(), maxFluid, 1, bpt);
        assertEquals(0, remaining);
    }

    @Test
    public void testAllTiersCapacity() {
        for (WellCellTier tier : WellCellTier.values()) {
            long maxBytes = tier.getTotalBytes();
            long bpt = tier.getBytesPerType();
            long maxAvailableFluidBytes = maxBytes - bpt;
            long maxCapacity = maxAvailableFluidBytes * 8000L;

            long remaining = computeRemainingFluidCapacity(maxBytes, maxCapacity, 1, (int) bpt);
            assertEquals(0, remaining, "Tier " + tier.getTierName() + " should have 0 remaining at max capacity");
        }
    }

    @Test
    public void testByteBoundaryTransitions() {
        WellCellTier tier = WellCellTier.TIER_1K;
        int bpt = tier.getBytesPerType();

        // Storing 7999 mB: 1 byte used for fluid
        assertEquals(1, computeUnusedFluidAmount(7999));
        long used7999 = computeUsedBytes(7999, 1, bpt);
        assertEquals(9, used7999);

        // Storing 8000 mB: exact byte boundary, 0 unused
        assertEquals(0, computeUnusedFluidAmount(8000));
        long used8000 = computeUsedBytes(8000, 1, bpt);
        assertEquals(9, used8000);

        // Storing 8001 mB: 2 bytes used for fluid
        assertEquals(7999, computeUnusedFluidAmount(8001));
        long used8001 = computeUsedBytes(8001, 1, bpt);
        assertEquals(10, used8001);
    }

    @Test
    public void testMultiTypeSpaceAllocation() {
        WellCellTier tier = WellCellTier.TIER_1K;
        int bpt = tier.getBytesPerType(); // 8

        // 18 types with 0 fluid: 18 * 8 = 144 bytes used
        long used = computeUsedBytes(0, 18, bpt);
        assertEquals(144, used);

        long remaining = computeRemainingFluidCapacity(tier.getTotalBytes(), 0, 18, bpt);
        // (1024 - 144) * 8000 = 880 * 8000 = 7,040,000 mB
        assertEquals(880 * 8000L, remaining);
    }

    @Test
    public void testOverCapacityClamp() {
        WellCellTier tier = WellCellTier.TIER_1K;
        int bpt = tier.getBytesPerType();

        // Storing 100,000,000 mB (well over total bytes)
        long overFluid = 100_000_000L;
        long usedBytes = computeUsedBytes(overFluid, 1, bpt);
        assertTrue(usedBytes > tier.getTotalBytes());

        long remaining = computeRemainingFluidCapacity(tier.getTotalBytes(), overFluid, 1, bpt);
        // Free bytes is clamped to 0, unused is remainder
        int unused = computeUnusedFluidAmount(overFluid);
        assertEquals(unused, remaining);
    }
}
