package org.smorodin.core.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.smorodin.core.model.PlayerQuest;
import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestEntity;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.persistence.quest.QuestEntity;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private PlayerQuestRepository playerQuestRepository;

    @Autowired
    private RewardService rewardService;


    // === Получить ежедневный квест игрока (или создать при первом запросе) ===
    @Transactional
    public DailyQuestResult getDailyQuest(Long playerId) {
        QuestEntity dailyTemplate = questRepository.findByType(QuestType.DAILY)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Daily quest template not found"));

        Quest quest = dailyTemplate.toDomain();

        PlayerQuestEntity pq = playerQuestRepository
                .findByPlayerIdAndQuestId(playerId, dailyTemplate.getId())
                .orElseGet(() -> {
                    PlayerQuestEntity newPq = new PlayerQuestEntity();
                    newPq.setPlayerId(playerId);
                    newPq.setQuestId(dailyTemplate.getId());
                    newPq.setProgress(new HashMap<>());
                    newPq.setCompleted(false);
                    newPq.setLastResetDate(LocalDate.now());

                    return playerQuestRepository.save(newPq);
                });

        if (needReset(pq, quest)) {
            PlayerQuest reset = pq.toDomain().reset();

            PlayerQuestEntity saved = PlayerQuestEntity.fromDomain(reset);
            saved.setId(pq.getId());

            pq = playerQuestRepository.save(saved);
        }

        return new DailyQuestResult(
                quest,
                pq.toDomain()
        );
    }

    // === Обновить прогресс по конкретному действию ===
    @Transactional
    public QuestUpdateResult updateProgress(Long playerId, String action, int amount) {
        QuestEntity dailyTemplate = questRepository.findByType(QuestType.DAILY)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Daily quest not found"));

        PlayerQuestEntity pqEntity = playerQuestRepository
                .findByPlayerIdAndQuestId(playerId, dailyTemplate.getId())
                .orElseThrow(() -> new RuntimeException("Player quest not found"));

        // Сброс если новый день
        if (needReset(pqEntity, dailyTemplate.toDomain())) {
            PlayerQuest reset = pqEntity.toDomain().reset();
            pqEntity = PlayerQuestEntity.fromDomain(reset);
            pqEntity.setId(pqEntity.getId());
            playerQuestRepository.save(pqEntity);
        }

        PlayerQuest domain = pqEntity.toDomain();

        if (amount <= 0) {
            throw new IllegalArgumentException("Progress amount must be positive");
        }

        if (!dailyTemplate.getTargets().containsKey(action)) {
            throw new IllegalArgumentException("Unknown quest action: " + action);
        }

        // Обновляем прогресс
        domain = domain.increaseProgress(action, amount);

        boolean mainCompleted = false;
        boolean hiddenCompleted = false;

        // Проверяем завершение основного квеста
        if (!domain.isCompleted() && domain.isAllTargetsMet(dailyTemplate.getTargets())) {
            rewardService.addExp(playerId, dailyTemplate.getExpReward());
            if (dailyTemplate.getStatReward() != null && dailyTemplate.getStatRewardAmount() > 0) {
                rewardService.increaseStat(playerId, dailyTemplate.getStatReward(), dailyTemplate.getStatRewardAmount());
            }
            domain = domain.markCompleted();
            mainCompleted = true;
        }

        // Проверяем скрытый квест (двойная цель)
        Optional<QuestEntity> hiddenOpt = questRepository.findByType(QuestType.HIDDEN).stream().findFirst();
        if (hiddenOpt.isPresent()) {
            QuestEntity hidden = hiddenOpt.get();
            PlayerQuestEntity hiddenPq = playerQuestRepository
                    .findByPlayerIdAndQuestId(playerId, hidden.getId())
                    .orElseGet(() -> {
                        PlayerQuestEntity newHidden = new PlayerQuestEntity();
                        newHidden.setPlayerId(playerId);
                        newHidden.setQuestId(hidden.getId());
                        newHidden.setProgress(new HashMap<>());
                        newHidden.setCompleted(false);
                        newHidden.setLastResetDate(LocalDate.now());
                        return playerQuestRepository.save(newHidden);
                    });

            // Сброс скрытого если новый день
            if (needReset(hiddenPq, hidden.toDomain())) {
                PlayerQuest resetHidden = hiddenPq.toDomain().reset();
                hiddenPq = PlayerQuestEntity.fromDomain(resetHidden);
                hiddenPq.setId(hiddenPq.getId());
                playerQuestRepository.save(hiddenPq);
            }

            if (!hiddenPq.isCompleted() && domain.isDoubleTargetsMet(dailyTemplate.getTargets())) {
                rewardService.addExp(playerId, hidden.getExpReward());
                if (hidden.getStatReward() != null && hidden.getStatRewardAmount() > 0) {
                    rewardService.increaseStat(playerId, hidden.getStatReward(), hidden.getStatRewardAmount());
                }
                PlayerQuest hiddenDomain = hiddenPq.toDomain().markCompleted();
                PlayerQuestEntity savedHidden = PlayerQuestEntity.fromDomain(hiddenDomain);
                savedHidden.setId(hiddenPq.getId());
                playerQuestRepository.save(savedHidden);
                hiddenCompleted = true;
            }
        }

        // Сохраняем основной
        PlayerQuestEntity savedMain = PlayerQuestEntity.fromDomain(domain);
        savedMain.setId(pqEntity.getId());
        playerQuestRepository.save(savedMain);

        return new QuestUpdateResult(domain, mainCompleted, hiddenCompleted);
    }

    // === Проверка, нужен ли сброс ===
    private boolean needReset(PlayerQuestEntity pq, Quest quest) {
        if (pq.isCompleted() && pq.getLastResetDate().equals(LocalDate.now())) {
            return false;
        }
        if (quest.getResetInterval() == ResetInterval.DAILY) {
            return !pq.getLastResetDate().equals(LocalDate.now());
        }
        return false;
    }

    @Getter
    @AllArgsConstructor
    public static class DailyQuestResult {
        private Quest quest;
        private PlayerQuest playerQuest;
    }

    // === DTO-подобный результат ===
    public static class QuestUpdateResult {
        private final PlayerQuest playerQuest;
        private final boolean mainCompleted;
        private final boolean hiddenCompleted;

        public QuestUpdateResult(PlayerQuest playerQuest, boolean mainCompleted, boolean hiddenCompleted) {
            this.playerQuest = playerQuest;
            this.mainCompleted = mainCompleted;
            this.hiddenCompleted = hiddenCompleted;
        }

        public PlayerQuest getPlayerQuest() { return playerQuest; }
        public boolean isMainCompleted() { return mainCompleted; }
        public boolean isHiddenCompleted() { return hiddenCompleted; }
    }
}