package org.smorodin.infrastructure.web.controller;

import org.smorodin.core.model.Player;
import org.smorodin.core.service.PlayerService;
import org.smorodin.infrastructure.auth.CustomUserDetails;
import org.smorodin.infrastructure.avatar.PlayerAvatarService;
import org.smorodin.infrastructure.web.dto.PlayerResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerAvatarService avatarService;

    public PlayerController(PlayerService playerService, PlayerAvatarService avatarService) {
        this.playerService = playerService;
        this.avatarService = avatarService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponseDto> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(profile(id));
    }

    @GetMapping
    public ResponseEntity<PlayerResponseDto> getCurrentPlayer(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(profile(userDetails.getUserId()));
    }

    private PlayerResponseDto profile(Long id) {
        Player player = playerService.getPlayer(id);
        return PlayerResponseDto.of(
                player, id, avatarService.hasAvatar(id), playerService.getStatProgress(id)
        );
    }
}
