package org.smorodin.core.service;

import jakarta.transaction.Transactional;
import org.smorodin.config.PlayerConfig;
import org.smorodin.core.model.Player;
import org.smorodin.core.model.StatExperience;
import org.smorodin.infrastructure.persistence.PlayerStatProgressEntity;
import org.smorodin.infrastructure.persistence.PlayerStatProgressRepository;
import org.smorodin.infrastructure.persistence.StatsEmbedded;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.smorodin.core.model.enums.StatType;
import org.smorodin.core.model.Stats;
import org.smorodin.infrastructure.persistence.PlayerEntity;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PlayerService implements RewardService{

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerStatProgressRepository statProgressRepository;

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
        PlayerEntity saved = playerRepository.saveAndFlush(entity);
        seedStatProgress(id);
        return saved.toDomain();
    }

    @Transactional
    public Player getOrCreateDefault(Long id){
        return playerRepository.findById(id)
                .map(PlayerEntity::toDomain)
                .orElseGet(() -> {
                    Player defaultPlayer = Player.defaultPlayer();
                    PlayerEntity entity = PlayerEntity.of(defaultPlayer, id);
                    PlayerEntity saved = playerRepository.saveAndFlush(entity);
                    seedStatProgress(id);
                    return saved.toDomain();
                });
    }


    @Override
    @Transactional
    public Player addExp(Long id, int amount) {

        PlayerEntity player = playerRepository
                .findForUpdate(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Player with id: " + id + " not found"
                        )
                );

        Player domainPlayer = player.toDomain()
                .addExp(amount);

        // Mutate the locked entity; merging a stale detached snapshot could
        // overwrite a characteristic level changed by an XP grant.
        player.setLevel(domainPlayer.getLevel());
        player.setExp(domainPlayer.getExp());
        playerRepository.save(player);

        return domainPlayer;
    }


    /**
     * Reads all six persisted progress bars without changing player EXP.
     */
    @Transactional
    public Map<StatType, StatExperience> getStatProgress(Long playerId) {
        PlayerEntity player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        Map<StatType, StatExperience> result = new EnumMap<>(StatType.class);
        for (PlayerStatProgressEntity entry : statProgressRepository.findAllByPlayerId(playerId)) {
            result.put(entry.getStatType(), new StatExperience(
                    player.getStats().toDomain().valueOf(entry.getStatType()),
                    entry.getCurrentXp()
            ));
        }
        if (result.size() != StatType.values().length) {
            throw new IllegalStateException("Missing persisted stat progress for player " + playerId);
        }
        return Map.copyOf(result);
    }

    @Override
    @Transactional
    public void addStatXp(Long playerId, StatType statType, int amount) {
        if (statType == null || amount <= 0) {
            throw new IllegalArgumentException("Stat XP amount and type must be valid");
        }

        // Same player-row lock as quests/EXP: concurrent grants cannot lose XP.
        PlayerEntity player = playerRepository.findForUpdate(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        PlayerStatProgressEntity progress = statProgressRepository
                .findByPlayerIdAndStatType(playerId, statType)
                .orElseThrow(() -> new IllegalStateException("Missing stat progress"));

        Stats oldStats = player.getStats().toDomain();
        StatExperience before = new StatExperience(
                oldStats.valueOf(statType), progress.getCurrentXp()
        );
        StatExperience after = before.addXp(amount);
        int gainedLevels = after.value() - before.value();

        progress.setCurrentXp(after.currentXp());
        statProgressRepository.save(progress);
        if (gainedLevels > 0) {
            player.setStats(StatsEmbedded.of(oldStats.increaseStat(statType, gainedLevels)));
            playerRepository.save(player);
        }
    }

    private void seedStatProgress(Long playerId) {
        statProgressRepository.saveAll(
                Arrays.stream(StatType.values())
                        .map(type -> new PlayerStatProgressEntity(playerId, type, 0))
                        .toList()
        );
    }

    /**
     * Admin/debug-style direct level increase (not used for quest rewards).
     * XP stays intact, so this endpoint cannot bypass quest XP accounting.
     */
    @Override
    @Transactional
    public Player increaseStat(Long id, String statName, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Stat increase must be positive");
        }
        StatType type = StatType.valueOf(statName);
        PlayerEntity entity = playerRepository.findForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        Stats newStats = entity.getStats().toDomain().increaseStat(type, amount);
        entity.setStats(StatsEmbedded.of(newStats));
        playerRepository.save(entity);
        return entity.toDomain();
    }
}
