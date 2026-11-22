package org.smorodin.core.service;

import org.smorodin.core.model.Player;
import org.smorodin.core.model.enums.StatType;

public interface RewardService {
    Player addExp(Long playerId, int amount);
    Player increaseStat(Long playerId, String statName, int amount);
    void addStatXp(Long playerId, StatType statType, int amount);
}