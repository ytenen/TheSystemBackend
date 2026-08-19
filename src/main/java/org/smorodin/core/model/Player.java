package org.smorodin.core.model;

import java.util.Objects;

public class Player {
    private final String name;
    private final int level;
    private final long exp;
    private final Stats stats;

    private Player(Builder builder) {
        this.name = builder.name;
        this.level = builder.level;
        this.exp = builder.exp;
        this.stats = builder.stats;
        validate();
    }

    private void validate() {
        if (name == null || name.length() <= 5) {
            throw new IllegalArgumentException("Name must be longer than 5 symbols");
        }
        if (level < 1) {
            throw new IllegalArgumentException("Level cant be lower than 1");
        }
        if (exp < 0) {
            throw new IllegalArgumentException("Exp cant be lower than 0");
        }
        long expToNext = getExpToNextLevel();
        if (expToNext < 0) {
            throw new IllegalArgumentException("exp to next level cant be lower than 0");
        }
    }

    // Геттеры
    public String getName() { return name; }
    public int getLevel() { return level; }
    public long getExp() { return exp; }
    public Stats getStats() { return stats; }

    // Вычисляемые поля
    public long getExpToNextLevel() {
        return level * 100L - exp;
    }

    public Rank getRank() {
        return Rank.fromLevel(level);
    }

    // Метод добавления опыта (возвращает нового Player)
    public Player addExp(int amount) {
        long totalExp = this.exp + amount;
        long expToNext = getExpToNextLevel();

        if (totalExp >= expToNext) {
            return new Builder()
                    .name(this.name)
                    .level(this.level + 1)
                    .exp(totalExp - expToNext)
                    .stats(this.stats)
                    .build();
        } else {
            return new Builder()
                    .name(this.name)
                    .level(this.level)
                    .exp(totalExp)
                    .stats(this.stats)
                    .build();
        }
    }

    // Билдер
    public static class Builder {
        private String name = "DefaultName";
        private int level = 1;
        private long exp = 0;
        private Stats stats = Stats.defaultStats();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder level(int level) {
            this.level = level;
            return this;
        }

        public Builder exp(long exp) {
            this.exp = exp;
            return this;
        }

        public Builder stats(Stats stats) {
            this.stats = stats;
            return this;
        }

        public Player build() {
            return new Player(this);
        }
    }

    // DEFAULT игрок
    public static Player defaultPlayer() {
        return new Builder().build();
    }

    public Player withIncreasedStat(StatType statType, int amount) {
        return new Builder()
                .name(this.name)
                .level(this.level)
                .exp(this.exp)
                .stats(this.stats.increaseStat(statType, amount))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return level == player.level &&
                exp == player.exp &&
                Objects.equals(name, player.name) &&
                Objects.equals(stats, player.stats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, level, exp, stats);
    }

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", level=" + level +
                ", exp=" + exp +
                ", stats=" + stats +
                '}';
    }
}