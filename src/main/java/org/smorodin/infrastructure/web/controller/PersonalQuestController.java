package org.smorodin.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.smorodin.core.service.PersonalQuestService;
import org.smorodin.infrastructure.auth.CustomUserDetails;
import org.smorodin.infrastructure.web.dto.quest.CreatePersonalQuestRequestDto;
import org.smorodin.infrastructure.web.dto.quest.PersonalQuestResponseDto;
import org.smorodin.infrastructure.web.dto.quest.ProgressRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quests/personal")
@RequiredArgsConstructor
public class PersonalQuestController {

    private final PersonalQuestService personalQuestService;

    @PostMapping
    public ResponseEntity<PersonalQuestResponseDto> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CreatePersonalQuestRequestDto request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(personalQuestService.create(
                        user.getUserId(),
                        request
                ));
    }

    @GetMapping
    public ResponseEntity<List<PersonalQuestResponseDto>> getAll(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(
                personalQuestService.getAll(user.getUserId())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonalQuestResponseDto> getById(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                personalQuestService.getById(
                        user.getUserId(),
                        id
                )
        );
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<PersonalQuestResponseDto> updateProgress(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody ProgressRequestDto request
    ) {
        return ResponseEntity.ok(
                personalQuestService.updateProgress(
                        user.getUserId(),
                        id,
                        request.getAction(),
                        request.getAmount()
                )
        );
    }
}
