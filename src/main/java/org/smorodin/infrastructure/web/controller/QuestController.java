package org.smorodin.infrastructure.web.controller;

import org.smorodin.core.model.PlayerQuest;
import org.smorodin.core.model.Quest;
import org.smorodin.core.service.DailyQuestSettingsService;
import org.smorodin.core.service.QuestService;
import org.smorodin.infrastructure.auth.CustomUserDetails;
import org.smorodin.infrastructure.i18n.ApiText;
import org.smorodin.infrastructure.i18n.SystemQuestText;
import java.util.Locale;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;
import org.smorodin.infrastructure.web.dto.quest.ProgressRequestDto;
import org.smorodin.infrastructure.web.dto.quest.ProgressResponseDto;
import org.smorodin.infrastructure.web.dto.quest.QuestResponseDto;

import org.smorodin.infrastructure.web.dto.quest.UpdateDailyQuestRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;
    private final DailyQuestSettingsService dailyQuestSettingsService;
    private final ApiText text;
    private final SystemQuestText systemQuestText;

    public QuestController(
            QuestService questService,
            DailyQuestSettingsService dailyQuestSettingsService,
            ApiText text,
            SystemQuestText systemQuestText
    ) {
        this.questService = questService;
        this.dailyQuestSettingsService = dailyQuestSettingsService;
        this.text = text;
        this.systemQuestText = systemQuestText;
    }

    @GetMapping("/daily")
    public ResponseEntity<QuestResponseDto> getDailyQuest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Locale locale
    ) {
        QuestService.DailyQuestResult result =
                questService.getDailyQuest(userDetails.getUserId());

        Quest quest = result.getQuest();
        PlayerQuest playerQuest = result.getPlayerQuest();

        QuestResponseDto response = QuestResponseDto.builder()
                .id(quest.getId())
                .title(systemQuestText.title(quest, locale))
                .description(systemQuestText.description(quest, locale))
                .targets(quest.getTargets())
                .progress(playerQuest.getProgress())
                .expReward(quest.getExpReward())
                .completed(playerQuest.isCompleted())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/daily/progress")
    public ResponseEntity<ProgressResponseDto> updateDailyProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ProgressRequestDto request,
            Locale locale
    ) {
        QuestService.QuestUpdateResult result =
                questService.updateProgress(
                        userDetails.getUserId(),
                        request.getAction(),
                        request.getAmount()
                );

        PlayerQuest playerQuest = result.getPlayerQuest();

        String message;

        if (result.isHiddenCompleted()) {
            message = text.get("progress.hidden.completed", locale);
        } else if (result.isMainCompleted()) {
            message = text.get("progress.daily.completed", locale);
        } else {
            message = text.get("progress.updated", locale);
        }

        ProgressResponseDto response = ProgressResponseDto.builder()
                .progress(playerQuest.getProgress())
                .completed(playerQuest.isCompleted())
                .hiddenCompleted(result.isHiddenCompleted())
                .message(message)
                .build();

        return ResponseEntity.ok(response);
    }


    @PutMapping("/daily/settings")
    public ResponseEntity<Void> updateDailySettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateDailyQuestRequestDto request
    ) {
        if (request == null || request.getMode() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Update mode is required"
            );
        }

        Long playerId = userDetails.getUserId();

        switch (request.getMode()) {
            case NOW ->
                    dailyQuestSettingsService.updateNow(playerId, request);

            case TOMORROW ->
                    dailyQuestSettingsService.scheduleUpdate(playerId, request);
        }

        return ResponseEntity.noContent().build();
    }

}