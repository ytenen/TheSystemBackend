package org.smorodin.core.service;

import jakarta.transaction.Transactional;
import org.smorodin.config.PlayerConfig;
import org.smorodin.core.model.Player;
import org.smorodin.core.model.StatType;
import org.smorodin.core.model.Stats;
import org.smorodin.infrastructure.persistence.PlayerEntity;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    public Player getPlayer(Long id){
        PlayerEntity player = playerRepository.findById(id).orElseThrow(() -> new RuntimeException("Player with id: " + id + " not found"));
        return player.toDomain();
    }

    @Transactional
    public Player createPlayer(Long id, String name) {
        Player player = new Player.Builder()
                .name(name)
                .level(PlayerConfig.START_LEVEL)
                .exp(PlayerConfig.START_EXP)
                .stats(Stats.defaultStats())
                .build();

        PlayerEntity entity = PlayerEntity.of(player, id);
        PlayerEntity saved = playerRepository.save(entity);
        return saved.toDomain();
    }

    public Player getOrCreateDefault(Long id){
        return playerRepository.findById(id)
                .map(PlayerEntity::toDomain)
                .orElseGet(() -> {
                    Player defaultPlayer = Player.defaultPlayer();
                    PlayerEntity entity = PlayerEntity.of(defaultPlayer, id);
                    PlayerEntity saved = playerRepository.save(entity);
                    return saved.toDomain();
                });
    }

    @Transactional
    public Player addExp(Long id, int amount){
        PlayerEntity player = playerRepository.findById(id).orElseThrow(() -> new RuntimeException("Player with id: " + id + " not found"));
        Player domainPlayer = player.toDomain().addExp(amount);
        playerRepository.save(PlayerEntity.of(domainPlayer, id));
        return domainPlayer;
    }

    @Transactional
    public Player increaseStat(Long id, String statName, int amount){
        PlayerEntity player = playerRepository.findById(id).orElseThrow(() -> new RuntimeException("Player with id: " + id + " not found"));
        Player domainPlayer = player.toDomain();
        Player finalPlayer = domainPlayer.withIncreasedStat(StatType.valueOf(statName), amount);
        playerRepository.save(PlayerEntity.of(finalPlayer, id));
        return finalPlayer;
    }
}
