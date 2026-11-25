package org.smorodin.core.model;

/**
 * Centralized progression cost. Future balancing can use the current stat value
 * without changing callers or persisted XP.
 */
public final class StatXpRule {
    private StatXpRule() {}

    public static int requiredXp(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Stat value cannot be negative");
        }
        return 100;
    }
}
