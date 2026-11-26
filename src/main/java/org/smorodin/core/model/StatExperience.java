package org.smorodin.core.model;

/**
 * Immutable progress for one characteristic. It does not affect Player.exp.
 */
public record StatExperience(int value, int currentXp) {

    public StatExperience {
        if (value < 0) {
            throw new IllegalArgumentException("Stat value cannot be negative");
        }
        if (currentXp < 0 || currentXp >= StatXpRule.requiredXp(value)) {
            throw new IllegalArgumentException("Stat XP must be between zero and the next threshold");
        }
    }

    public int requiredXp() {
        return StatXpRule.requiredXp(value);
    }

    public StatExperience addXp(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Stat XP amount must be positive");
        }
        long remaining = (long) currentXp + amount;
        int nextValue = value;
        int required = StatXpRule.requiredXp(nextValue);

        while (remaining >= required) {
            remaining -= required;
            nextValue = Math.addExact(nextValue, 1);
            required = StatXpRule.requiredXp(nextValue);
        }
        return new StatExperience(nextValue, Math.toIntExact(remaining));
    }
}
