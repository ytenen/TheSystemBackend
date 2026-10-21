package org.smorodin.infrastructure.web.controller;

import org.smorodin.core.model.PlayerQuest;
import org.smorodin.core.model.Quest;
import org.smorodin.core.service.QuestService;
import org.smorodin.infrastructure.auth.CustomUserDetails;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;
import org.smorodin.infrastructure.web.dto.quest.ProgressRequestDto;
import org.smorodin.infrastructure.web.dto.quest.ProgressResponseDto;
import org.smorodin.infrastructure.web.dto.quest.QuestResponseDto;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @GetMapping("/daily")
    public ResponseEntity<QuestResponseDto> getDailyQuest(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        QuestService.DailyQuestResult result =
                questService.getDailyQuest(userDetails.getUserId());

        Quest quest = result.getQuest();
        PlayerQuest playerQuest = result.getPlayerQuest();

        QuestResponseDto response = QuestResponseDto.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
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
            @RequestBody ProgressRequestDto request
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
            message = "Hidden quest completed!";
        } else if (result.isMainCompleted()) {
            message = "Daily quest completed!";
        } else {
            message = "Quest progress updated";
        }

        ProgressResponseDto response = ProgressResponseDto.builder()
                .progress(playerQuest.getProgress())
                .completed(playerQuest.isCompleted())
                .hiddenCompleted(result.isHiddenCompleted())
                .message(message)
                .build();

        return ResponseEntity.ok(response);
    }
}