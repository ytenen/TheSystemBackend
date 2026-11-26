package org.smorodin.core.service;

import lombok.RequiredArgsConstructor;
import org.smorodin.core.model.PlayerQuest;
import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;
import org.smorodin.infrastructure.persistence.quest.*;
import org.smorodin.infrastructure.web.dto.quest.CreatePersonalQuestRequestDto;
import org.smorodin.infrastructure.web.dto.quest.PersonalQuestResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PersonalQuestService {

    private final QuestRepository questRepository;
    private final PlayerQuestRepository playerQuestRepository;
    private final RewardService rewardService;

    @Transactional
    public PersonalQuestResponseDto create(
            Long playerId,
            CreatePersonalQuestRequestDto request
    ) {
        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("Invalid player ID");
        }

        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }

        if (request.getDescription() != null
                && request.getDescription().length() > 1000) {
            throw new IllegalArgumentException("Description is too long");
        }

        if (request.getEndDate() != null
                && request.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Deadline cannot be in the past"
            );
        }

        Map<String, Integer> targets = request.getTargets();

        if (targets == null || targets.isEmpty()
                || targets.size() > 20) {
            throw new IllegalArgumentException(
                    "Quest must contain between 1 and 20 targets"
            );
        }

        for (String action : targets.keySet()) {
            if (action == null || action.isBlank()
                    || action.length() > 100) {
                throw new IllegalArgumentException(
                        "Invalid target action"
                );
            }
        }

        Quest quest = new Quest.Builder()
                .ownerId(playerId)
                .type(QuestType.PERSONAL)
                .title(request.getTitle())
                .description(request.getDescription())
                .targets(targets)
                .expReward(100)
                .resetInterval(ResetInterval.NONE)
                .startDate(LocalDate.now())
                .endDate(request.getEndDate())
                .build();

        QuestEntity savedQuest = questRepository.save(
                QuestEntity.fromDomain(quest)
        );

        PlayerQuestEntity playerQuest = new PlayerQuestEntity();
        playerQuest.setPlayerId(playerId);
        playerQuest.setQuestId(savedQuest.getId());
        playerQuest.setProgress(new HashMap<>());
        playerQuest.setStatus(QuestStatus.ACTIVE);
        playerQuest.setLastResetDate(LocalDate.now());

        PlayerQuestEntity savedProgress =
                playerQuestRepository.save(playerQuest);

        return toResponse(savedQuest, savedProgress);
    }

    @Transactional(readOnly = true)
    public List<PersonalQuestResponseDto> getAll(Long playerId) {
        return questRepository
                .findByOwnerIdAndType(playerId, QuestType.PERSONAL)
                .stream()
                .map(quest -> {
                    PlayerQuestEntity progress =
                            playerQuestRepository
                                    .findByPlayerIdAndQuestId(
                                            playerId,
                                            quest.getId()
                                    )
                                    .orElseThrow(() ->
                                            new IllegalStateException(
                                                    "Quest progress not found"
                                            )
                                    );

                    return toResponse(quest, progress);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonalQuestResponseDto getById(
            Long playerId,
            Long questId
    ) {
        QuestEntity quest = findOwnedQuest(playerId, questId);

        PlayerQuestEntity progress = playerQuestRepository
                .findByPlayerIdAndQuestId(playerId, questId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Quest progress not found"
                        )
                );

        return toResponse(quest, progress);
    }

    @Transactional
    public PersonalQuestResponseDto updateProgress(
            Long playerId,
            Long questId,
            String action,
            int amount
    ) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Action cannot be empty"
            );
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Progress amount must be positive"
            );
        }

        // Сначала проверяем, что квест принадлежит игроку.
        QuestEntity quest = findOwnedQuest(playerId, questId);

        if (!quest.getTargets().containsKey(action)) {
            throw new IllegalArgumentException(
                    "Unknown quest action: " + action
            );
        }

        // Блокируем запись до конца транзакции.
        PlayerQuestEntity entity = playerQuestRepository
                .findForUpdate(playerId, questId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Quest progress not found"
                        )
                );

        if (entity.getStatus() == QuestStatus.LOCKED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Quest is locked"
            );
        }

        if (entity.getStatus() == QuestStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Quest is already completed"
            );
        }

        PlayerQuest playerQuest = entity.toDomain();

        int current = playerQuest.getProgress()
                .getOrDefault(action, 0);

        int target = quest.getTargets().get(action);

        // Не даём прогрессу превысить цель.
        int remaining = Math.max(0, target - current);

        if (remaining == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Target is already completed"
            );
        }

        int actualAmount = Math.min(amount, remaining);

        playerQuest = playerQuest.increaseProgress(
                action,
                actualAmount
        );

        boolean completed = playerQuest.isAllTargetsMet(
                quest.getTargets()
        );

        if (completed) {
            playerQuest = playerQuest.markCompleted();
        }

        // Сохраняем состояние квеста.
        PlayerQuestEntity updated =
                PlayerQuestEntity.fromDomain(playerQuest);

        updated.setId(entity.getId());

        PlayerQuestEntity saved =
                playerQuestRepository.save(updated);

        // Награда выдаётся только при переходе
        // ACTIVE -> COMPLETED.
        if (completed && quest.getExpReward() > 0) {
            rewardService.addExp(
                    playerId,
                    quest.getExpReward()
            );
        }

        return toResponse(quest, saved);
    }

    private QuestEntity findOwnedQuest(
            Long playerId,
            Long questId
    ) {
        return questRepository
                .findByIdAndOwnerIdAndType(
                        questId,
                        playerId,
                        QuestType.PERSONAL
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Quest not found"
                        )
                );
    }

    private PersonalQuestResponseDto toResponse(
            QuestEntity quest,
            PlayerQuestEntity playerQuest
    ) {
        return PersonalQuestResponseDto.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .targets(Map.copyOf(quest.getTargets()))
                .progress(Map.copyOf(playerQuest.getProgress()))
                .status(playerQuest.getStatus())
                .expReward(quest.getExpReward())
                .endDate(quest.getEndDate())
                .build();
    }
}
