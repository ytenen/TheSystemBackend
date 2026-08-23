package org.smorodin.core.model;

import org.smorodin.config.PlayerConfig;

import java.util.Objects;

public class Stats {
    private final int strength;
    private final int agility;
    private final int vitality;
    private final int intelligence;
    private final int perception;
    private final int will;

    private Stats(Builder builder) {
        this.strength = builder.strength;
        this.agility = builder.agility;
        this.vitality = builder.vitality;
        this.intelligence = builder.intelligence;
        this.perception = builder.perception;
        this.will = builder.will;
        validate();
    }

    private void validate() {
        if (strength < 0) throw new IllegalArgumentException("Сила не может быть отрицательной");
        if (agility < 0) throw new IllegalArgumentException("Ловкость не может быть отрицательной");
        if (vitality < 0) throw new IllegalArgumentException("Живучесть не может быть отрицательной");
        if (intelligence < 0) throw new IllegalArgumentException("Интеллект не может быть отрицательным");
        if (perception < 0) throw new IllegalArgumentException("Восприятие не может быть отрицательным");
        if (will < 0) throw new IllegalArgumentException("Воля не может быть отрицательной");
    }

    // Геттеры
    public int getStrength() { return strength; }
    public int getAgility() { return agility; }
    public int getVitality() { return vitality; }
    public int getIntelligence() { return intelligence; }
    public int getPerception() { return perception; }
    public int getWill() { return will; }

    public static class Builder {
        private int strength = 8;
        private int agility = 5;
        private int vitality = 10;
        private int intelligence = 7;
        private int perception = 6;
        private int will = 9;

        public Builder strength(int strength) {
            this.strength = strength;
            return this;
        }

        public Builder agility(int agility) {
            this.agility = agility;
            return this;
        }

        public Builder vitality(int vitality) {
            this.vitality = vitality;
            return this;
        }

        public Builder intelligence(int intelligence) {
            this.intelligence = intelligence;
            return this;
        }

        public Builder perception(int perception) {
            this.perception = perception;
            return this;
        }

        public Builder will(int will) {
            this.will = will;
            return this;
        }

        public Stats build() {
            return new Stats(this);
        }
    }

    // DEFAULT статистика
    public static Stats defaultStats() {
        return new Builder().strength(PlayerConfig.START_STRENGTH)
                .agility(PlayerConfig.START_AGILITY)
                .vitality(PlayerConfig.START_VITALITY)
                .intelligence(PlayerConfig.START_INTELLIGENCE)
                .perception(PlayerConfig.START_PERCEPTION)
                .will(PlayerConfig.START_WILL).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stats stats = (Stats) o;
        return strength == stats.strength &&
                agility == stats.agility &&
                vitality == stats.vitality &&
                intelligence == stats.intelligence &&
                perception == stats.perception &&
                will == stats.will;
    }

    @Override
    public int hashCode() {
        return Objects.hash(strength, agility, vitality, intelligence, perception, will);
    }

    @Override
    public String toString() {
        return "Stats{" +
                "strength=" + strength +
                ", agility=" + agility +
                ", vitality=" + vitality +
                ", intelligence=" + intelligence +
                ", perception=" + perception +
                ", will=" + will +
                '}';
    }

    public Stats increaseStat(StatType statName, int amount){
        return switch (statName) {
            case STRENGTH -> new Builder().strength(this.strength + amount)
                    .agility(this.agility)
                    .vitality(this.vitality)
                    .intelligence(this.intelligence)
                    .perception(this.perception)
                    .will(this.will).build();
            case AGILITY -> new Builder().strength(this.strength)
                    .agility(this.agility + amount)
                    .vitality(this.vitality)
                    .intelligence(this.intelligence)
                    .perception(this.perception)
                    .will(this.will).build();
            case VITALITY -> new Builder().strength(this.strength)
                    .agility(this.agility)
                    .vitality(this.vitality + amount)
                    .intelligence(this.intelligence)
                    .perception(this.perception)
                    .will(this.will).build();
            case INTELLIGENCE -> new Builder().strength(this.strength)
                    .agility(this.agility)
                    .vitality(this.vitality)
                    .intelligence(this.intelligence + amount)
                    .perception(this.perception)
                    .will(this.will).build();
            case PERCEPTION -> new Builder().strength(this.strength)
                    .agility(this.agility)
                    .vitality(this.vitality)
                    .intelligence(this.intelligence)
                    .perception(this.perception + amount)
                    .will(this.will).build();
            case WILL -> new Builder().strength(this.strength)
                    .agility(this.agility)
                    .vitality(this.vitality)
                    .intelligence(this.intelligence)
                    .perception(this.perception)
                    .will(this.will + amount).build();
            default -> throw new IllegalArgumentException("Unknown stat: " + statName);
        };
    }
}