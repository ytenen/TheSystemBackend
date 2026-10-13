package org.smorodin.core.service;

import org.smorodin.core.model.Player;

public interface RewardService {
    Player addExp(Long playerId, int amount);
    Player increaseStat(Long playerId, String statName, int amount);
}