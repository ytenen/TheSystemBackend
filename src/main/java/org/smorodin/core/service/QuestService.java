package org.smorodin.core.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.smorodin.core.model.PlayerQuest;
import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.StatType;
import org.smorodin.core.model.enums.ResetInterval;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestEntity;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.persistence.quest.QuestEntity;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private DailyQuestSettingsService dailyQuestSettingsService;

    @Autowired
    private DailyQuestLockService dailyQuestLockService;

    @Autowired
    private PlayerQuestRepository playerQuestRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private Clock clock;

    @Transactional
    public DailyQuestResult getDailyQuest(Long playerId) {
        dailyQuestLockService.lockPlayer(playerId);
        QuestEntity dailyTemplate = questRepository
                .findByType(QuestType.DAILY)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Daily quest template not found"
                ));

        Map<String, Integer> targets =
                dailyQuestSettingsService.applyPendingIfDue(playerId);

        Quest quest = withTargets(dailyTemplate, targets);

        PlayerQuestEntity pq = playerQuestRepository
                .findByPlayerIdAndQuestId(
                        playerId, dailyTemplate.getId()
                )
                .orElseGet(() -> {
                    PlayerQuestEntity newPq = new PlayerQuestEntity();
                    newPq.setPlayerId(playerId);
                    newPq.setQuestId(dailyTemplate.getId());
                    newPq.setProgress(new HashMap<>());
                    newPq.setStatus(QuestStatus.ACTIVE);
                    newPq.setLastResetDate(LocalDate.now(clock));
                    return playerQuestRepository.save(newPq);
                });

        if (needReset(pq, quest)) {
            PlayerQuest reset = pq.toDomain().reset(clock);
            PlayerQuestEntity saved =
                    PlayerQuestEntity.fromDomain(reset);

            saved.setId(pq.getId());
            pq = playerQuestRepository.save(saved);
        }

        return new DailyQuestResult(quest, pq.toDomain());
    }

    @Transactional
    public QuestUpdateResult updateProgress(
            Long playerId,
            String action,
            int amount
    ) {
        dailyQuestLockService.lockPlayer(playerId);
        Map<String, Integer> targets =
                dailyQuestSettingsService.applyPendingIfDue(playerId);
        QuestEntity dailyTemplate = questRepository
                .findByType(QuestType.DAILY)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Daily quest not found"
                ));

        PlayerQuestEntity pqEntity = playerQuestRepository
                .findByPlayerIdAndQuestId(
                        playerId, dailyTemplate.getId()
                )
                .orElseThrow(() -> new RuntimeException(
                        "Player quest not found"
                ));

        if (needReset(pqEntity, dailyTemplate.toDomain())) {
            Long playerQuestId = pqEntity.getId();

            PlayerQuest reset = pqEntity.toDomain().reset(clock);

            pqEntity = PlayerQuestEntity.fromDomain(reset);
            pqEntity.setId(playerQuestId);
            pqEntity = playerQuestRepository.save(pqEntity);
        }

        PlayerQuest domain = pqEntity.toDomain();

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Progress amount must be positive"
            );
        }

        if (!targets.containsKey(action)) {
            throw new IllegalArgumentException(
                    "Unknown quest action: " + action
            );
        }

        domain = domain.increaseProgress(action, amount);

        boolean mainCompleted = false;
        boolean hiddenCompleted = false;
        List<GrantedReward> rewardsGranted = new ArrayList<>();

        if (!domain.isCompleted()
                && domain.isAllTargetsMet(targets)) {

            rewardsGranted.add(grantRewards(playerId, dailyTemplate));

            domain = domain.markCompleted(clock);
            mainCompleted = true;
        }

        Optional<QuestEntity> hiddenOpt = questRepository
                .findByType(QuestType.HIDDEN)
                .stream()
                .findFirst();

        if (hiddenOpt.isPresent()) {
            QuestEntity hidden = hiddenOpt.get();

            PlayerQuestEntity hiddenPq = playerQuestRepository
                    .findByPlayerIdAndQuestId(
                            playerId, hidden.getId()
                    )
                    .orElseGet(() -> {
                        PlayerQuestEntity newHidden =
                                new PlayerQuestEntity();

                        newHidden.setPlayerId(playerId);
                        newHidden.setQuestId(hidden.getId());
                        newHidden.setProgress(new HashMap<>());
                        newHidden.setStatus(QuestStatus.ACTIVE);
                        newHidden.setLastResetDate(LocalDate.now(clock));

                        return playerQuestRepository.save(newHidden);
                    });

            if (needReset(hiddenPq, hidden.toDomain())) {
                Long hiddenQuestId = hiddenPq.getId();

                PlayerQuest resetHidden =
                        hiddenPq.toDomain().reset(clock);

                hiddenPq = PlayerQuestEntity.fromDomain(resetHidden);
                hiddenPq.setId(hiddenQuestId);
                hiddenPq = playerQuestRepository.save(hiddenPq);
            }

            if (!hiddenPq.isCompleted()
                    && domain.isDoubleTargetsMet(targets)) {

                rewardsGranted.add(grantRewards(playerId, hidden));

                PlayerQuest hiddenDomain =
                        hiddenPq.toDomain().markCompleted(clock);

                PlayerQuestEntity savedHidden =
                        PlayerQuestEntity.fromDomain(hiddenDomain);

                savedHidden.setId(hiddenPq.getId());
                playerQuestRepository.save(savedHidden);

                hiddenCompleted = true;
            }
        }

        PlayerQuestEntity savedMain =
                PlayerQuestEntity.fromDomain(domain);

        savedMain.setId(pqEntity.getId());
        playerQuestRepository.save(savedMain);

        return new QuestUpdateResult(
                domain,
                mainCompleted,
                hiddenCompleted,
                List.copyOf(rewardsGranted)
        );
    }

    private GrantedReward grantRewards(Long playerId, QuestEntity quest) {
        int expReward = quest.getExpReward();
        Map<String, Integer> statXpRewards = quest.getStatXpRewards() == null
                ? Map.of() : Map.copyOf(quest.getStatXpRewards());

        if (expReward > 0) {
            rewardService.addExp(playerId, expReward);
        }
        for (Map.Entry<String, Integer> entry : statXpRewards.entrySet()) {
            rewardService.addStatXp(
                    playerId, StatType.valueOf(entry.getKey()), entry.getValue()
            );
        }
        return new GrantedReward(quest.getType(), expReward, statXpRewards);
    }

    public record GrantedReward(
            QuestType questType,
            int expReward,
            Map<String, Integer> statXpRewards
    ) {}

    private boolean needReset(
            PlayerQuestEntity pq,
            Quest quest
    ) {
        LocalDate today = LocalDate.now(clock);

        if (pq.isCompleted()
                && today.equals(pq.getLastResetDate())) {
            return false;
        }

        if (quest.getResetInterval() == ResetInterval.DAILY) {
            return !today.equals(pq.getLastResetDate());
        }

        return false;
    }



    private Quest withTargets(
            QuestEntity template,
            Map<String, Integer> targets
    ) {
        Quest original = template.toDomain();

        return new Quest.Builder()
                .id(original.getId())
                .ownerId(original.getOwnerId())
                .type(original.getType())
                .title(original.getTitle())
                .description(original.getDescription())
                .targets(targets)
                .expReward(original.getExpReward())
                .statXpRewards(original.getStatXpRewards())
                .resetInterval(original.getResetInterval())
                .startDate(original.getStartDate())
                .endDate(original.getEndDate())
                .aiContext(original.getAiContext())
                .fileReference(original.getFileReference())
                .build();
    }



    @Getter
    @AllArgsConstructor
    public static class DailyQuestResult {
        private Quest quest;
        private PlayerQuest playerQuest;
    }

    public static class QuestUpdateResult {

        private final PlayerQuest playerQuest;
        private final boolean mainCompleted;
        private final boolean hiddenCompleted;
        private final List<GrantedReward> rewardsGranted;

        public QuestUpdateResult(
                PlayerQuest playerQuest,
                boolean mainCompleted,
                boolean hiddenCompleted,
                List<GrantedReward> rewardsGranted
        ) {
            this.playerQuest = playerQuest;
            this.mainCompleted = mainCompleted;
            this.hiddenCompleted = hiddenCompleted;
            this.rewardsGranted = List.copyOf(rewardsGranted);
        }

        public PlayerQuest getPlayerQuest() {
            return playerQuest;
        }

        public boolean isMainCompleted() {
            return mainCompleted;
        }

        public boolean isHiddenCompleted() {
            return hiddenCompleted;
        }

        public List<GrantedReward> getRewardsGranted() {
            return rewardsGranted;
        }
    }
}
