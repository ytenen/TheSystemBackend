package org.smorodin.core.model;

public enum Rank {
    E(1),
    D(10),
    C(25),
    B(45),
    A(70),
    S(100),
    NATIONAL_LEVEL(140);

    private final int minLevel;

    Rank(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public static Rank fromLevel(int level) {
        for (Rank rank : values()) {
            if (level >= rank.minLevel) {
                return rank;
            }
        }
        return E;
    }
}
