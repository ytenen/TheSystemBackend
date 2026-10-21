package org.smorodin.infrastructure.web.controller;

import org.smorodin.core.model.Player;
import org.smorodin.core.service.PlayerService;
import org.smorodin.infrastructure.auth.CustomUserDetails;
import org.smorodin.infrastructure.web.dto.AddExpRequestDto;
import org.smorodin.infrastructure.web.dto.IncreaseStatRequestDto;
import org.smorodin.infrastructure.web.dto.PlayerResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/player")
public class PlayerController {

    @Autowired
    PlayerService playerService;

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponseDto> getPlayer(@PathVariable Long id){
        try{
            Player player = playerService.getPlayer(id);
            PlayerResponseDto playerResponseDto = PlayerResponseDto.of(player,id);
            return ResponseEntity.ok(playerResponseDto);
        }
        catch (RuntimeException e){
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/exp")
    public ResponseEntity<?> addExp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int amount) {

        Player player = playerService.addExp(userDetails.getUserId(), amount);
        return ResponseEntity.ok(player);
    }

    @PostMapping("/stat")
    public ResponseEntity<?> increaseStat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String stat,
            @RequestParam int amount) {

        Player player = playerService.increaseStat(
                userDetails.getUserId(),
                stat,
                amount
        );

        return ResponseEntity.ok(player);
    }

    @GetMapping
    public ResponseEntity<PlayerResponseDto> getCurrentPlayer() {
        try {
            Long userId = getCurrentUserId();
            Player player = playerService.getPlayer(userId);
            return ResponseEntity.ok(PlayerResponseDto.of(player, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        }
        throw new RuntimeException("Invalid authentication");
    }
    
}
