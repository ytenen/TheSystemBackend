package org.smorodin.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatExperienceTest {

    @Test
    void shouldStartAtZeroXpWithA100XpThreshold() {
        StatExperience stat = new StatExperience(9, 0);
        assertEquals(9, stat.value());
        assertEquals(0, stat.currentXp());
        assertEquals(100, stat.requiredXp());
    }

    @Test
    void shouldAccumulateWithoutLeveling() {
        StatExperience stat = new StatExperience(9, 0).addXp(75);
        assertEquals(9, stat.value());
        assertEquals(75, stat.currentXp());
    }

    @Test
    void shouldLevelUpAtExactThreshold() {
        StatExperience stat = new StatExperience(9, 0).addXp(100);
        assertEquals(10, stat.value());
        assertEquals(0, stat.currentXp());
    }

    @Test
    void shouldCarryExcessXpForward() {
        StatExperience stat = new StatExperience(9, 75).addXp(50);
        assertEquals(10, stat.value());
        assertEquals(25, stat.currentXp());
    }

    @Test
    void shouldAllowMultipleLevelUpsInOneGrant() {
        StatExperience stat = new StatExperience(9, 0).addXp(375);
        assertEquals(12, stat.value());
        assertEquals(75, stat.currentXp());
    }

    @Test
    void shouldRejectInvalidProgressOrAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new StatExperience(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new StatExperience(9, -1));
        assertThrows(IllegalArgumentException.class, () -> new StatExperience(9, 100));
        assertThrows(IllegalArgumentException.class, () -> new StatExperience(9, 0).addXp(0));
        assertThrows(IllegalArgumentException.class, () -> new StatExperience(9, 0).addXp(-5));
    }

    @Test
    void shouldGuardStatValueIntegerOverflow() {
        assertThrows(ArithmeticException.class,
                () -> new StatExperience(Integer.MAX_VALUE, 99).addXp(1));
    }
}
