package de.project.ae2virtualwell;

import de.project.ae2virtualwell.recipe.WellDropEntry;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WellDropEntryTest {

    @Test
    public void testRecordFields() {
        WellDropEntry entry = new WellDropEntry(null, 80, 500, 2000);
        assertNull(entry.fluid());
        assertEquals(80, entry.weight());
        assertEquals(500, entry.minMilliBuckets());
        assertEquals(2000, entry.maxMilliBuckets());
    }

    @Test
    public void testRollAmountFixed() {
        WellDropEntry fixed = new WellDropEntry(null, 100, 1000, 1000);
        RandomSource random = RandomSource.create(42L);

        for (int i = 0; i < 50; i++) {
            int amount = fixed.rollAmount(random);
            assertEquals(1000, amount);
        }
    }

    @Test
    public void testRollAmountRange() {
        WellDropEntry ranged = new WellDropEntry(null, 100, 500, 1500);
        RandomSource random = RandomSource.create(12345L);

        boolean seenMin = false;
        boolean seenMax = false;
        boolean seenMid = false;

        for (int i = 0; i < 500; i++) {
            int amount = ranged.rollAmount(random);
            assertTrue(amount >= 500 && amount <= 1500, "Rolled amount out of bounds: " + amount);
            if (amount == 500) seenMin = true;
            if (amount == 1500) seenMax = true;
            if (amount > 500 && amount < 1500) seenMid = true;
        }

        assertTrue(seenMid, "Should roll values in between min and max");
    }

    @Test
    public void testRollAmountEdgeCases() {
        WellDropEntry negative = new WellDropEntry(null, 10, -100, -50);
        RandomSource random = RandomSource.create(99L);
        int amount = negative.rollAmount(random);
        assertTrue(amount >= 1, "Should clamp to at least 1 mB");

        WellDropEntry inverted = new WellDropEntry(null, 10, 2000, 500);
        int invAmount = inverted.rollAmount(random);
        assertEquals(2000, invAmount, "Should handle min > max gracefully by clamping");
    }
}

