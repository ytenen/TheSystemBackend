package org.smorodin.core.service;

import lombok.RequiredArgsConstructor;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.infrastructure.persistence.quest.*;
import org.smorodin.infrastructure.web.dto.quest.UpdateDailyQuestRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyQuestSettingsService {

    private final PlayerDailyQuestSettingsRepository settingsRepository;
    private final QuestRepository questRepository;
    private final Clock clock;
    private final DailyQuestLockService dailyQuestLockService;
    private final PlayerQuestRepository playerQuestRepository;

    @Transactional(readOnly = true)
    public Map<String, Integer> getActiveTargets(Long playerId) {
        return settingsRepository.findById(playerId)
                .map(PlayerDailyQuestSettingsEntity::getActiveTargets)
                .map(Map::copyOf)
                .orElseGet(this::getDefaultTargets);
    }


    @Transactional
    public void updateNow(
            Long playerId,
            UpdateDailyQuestRequestDto request
    ) {
        if (request == null
                || request.getMode() != UpdateDailyQuestRequestDto.UpdateMode.NOW) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Update mode must be NOW"
            );
        }

        validateTargets(request.getTargets());

        // Сначала блокируем игрока
        dailyQuestLockService.lockPlayer(playerId);

        LocalDate today = LocalDate.now(clock);

        // Загружаем или создаём персональные настройки
        PlayerDailyQuestSettingsEntity settings =
                settingsRepository.findForUpdate(playerId)
                        .orElseGet(() -> {
                            PlayerDailyQuestSettingsEntity entity =
                                    new PlayerDailyQuestSettingsEntity();
                            entity.setPlayerId(playerId);
                            entity.setActiveTargets(
                                    new HashMap<>(getDefaultTargets())
                            );
                            return entity;
                        });

        // Применяем новые цели немедленно
        settings.setActiveTargets(
                new HashMap<>(request.getTargets())
        );

        // Отменяем ранее запланированное изменение
        settings.setPendingTargets(null);
        settings.setPendingEffectiveDate(null);

        settingsRepository.save(settings);

        // Находим системный Daily Quest
        QuestEntity daily = questRepository
                .findByType(QuestType.DAILY)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Daily quest template not found"
                        )
                );

        // Сбрасываем прогресс Daily, но не награду
        playerQuestRepository
                .findByPlayerIdAndQuestId(playerId, daily.getId())
                .ifPresent(progress ->
                        clearProgressKeepingTodayCompletion(progress, today)
                );

        // Аналогично для Hidden Quest
        questRepository.findByType(QuestType.HIDDEN)
                .stream()
                .findFirst()
                .ifPresent(hidden ->
                        playerQuestRepository
                                .findByPlayerIdAndQuestId(
                                        playerId,
                                        hidden.getId()
                                )
                                .ifPresent(progress ->
                                        clearProgressKeepingTodayCompletion(
                                                progress, today
                                        )
                                )
                );
    }

    private void clearProgressKeepingTodayCompletion(
            PlayerQuestEntity progress,
            LocalDate today
    ) {
        boolean completedToday =
                progress.getStatus() == QuestStatus.COMPLETED
                        && today.equals(progress.getLastResetDate());

        // Любой прежний прогресс больше не относится к новой тренировке
        progress.setProgress(new HashMap<>());

        if (!completedToday) {
            progress.setStatus(QuestStatus.ACTIVE);
            progress.setCompletedAt(null);
        }

        progress.setLastResetDate(today);

        playerQuestRepository.save(progress);
    }


    @Transactional
    public void scheduleUpdate(
            Long playerId,
            UpdateDailyQuestRequestDto request
    ) {
        dailyQuestLockService.lockPlayer(playerId);
        if (request == null || request.getMode() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Update mode is required"
            );
        }

        validateTargets(request.getTargets());

        if (request.getMode()
                != UpdateDailyQuestRequestDto.UpdateMode.TOMORROW) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This method only supports TOMORROW"
            );
        }

        PlayerDailyQuestSettingsEntity settings =
                settingsRepository.findForUpdate(playerId)
                        .orElseGet(() -> {
                            PlayerDailyQuestSettingsEntity entity =
                                    new PlayerDailyQuestSettingsEntity();

                            entity.setPlayerId(playerId);
                            entity.setActiveTargets(
                                    new HashMap<>(getDefaultTargets())
                            );

                            return entity;
                        });

        settings.setPendingTargets(
                new HashMap<>(request.getTargets())
        );

        settings.setPendingEffectiveDate(
                LocalDate.now(clock).plusDays(1)
        );

        settingsRepository.save(settings);
    }

    private Map<String, Integer> getDefaultTargets() {
        QuestEntity template = questRepository
                .findByType(QuestType.DAILY)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Daily quest template not found"
                ));

        return Map.copyOf(template.getTargets());
    }


    @Transactional
    public Map<String, Integer> applyPendingIfDue(Long playerId) {
        dailyQuestLockService.lockPlayer(playerId);
        PlayerDailyQuestSettingsEntity settings =
                settingsRepository.findForUpdate(playerId)
                        .orElse(null);

        if (settings == null) {
            return getDefaultTargets();
        }

        LocalDate today = LocalDate.now(clock);

        if (settings.getPendingTargets() != null
                && settings.getPendingEffectiveDate() != null
                && !settings.getPendingEffectiveDate().isAfter(today)) {

            settings.setActiveTargets(
                    new HashMap<>(settings.getPendingTargets())
            );

            settings.setPendingTargets(null);
            settings.setPendingEffectiveDate(null);

            settingsRepository.save(settings);
        }

        return Map.copyOf(settings.getActiveTargets());
    }


    private void validateTargets(Map<String, Integer> targets) {
        if (targets == null || targets.isEmpty()
                || targets.size() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Training must contain 1 to 20 exercises"
            );
        }

        for (Map.Entry<String, Integer> entry : targets.entrySet()) {
            String action = entry.getKey();
            Integer target = entry.getValue();

            if (action == null || action.isBlank()
                    || action.length() > 100) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid exercise name"
                );
            }

            if (target == null || target <= 0
                    || target > 100000) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Exercise target must be between 1 and 100000"
                );
            }
        }
    }
}
