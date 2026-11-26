package org.smorodin.infrastructure.web.controller;

import org.smorodin.infrastructure.auth.CustomUserDetails;
import org.smorodin.infrastructure.avatar.PlayerAvatarService;
import org.smorodin.infrastructure.avatar.PlayerAvatarService.AvatarData;
import org.smorodin.infrastructure.web.dto.AvatarResponseDto;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/player")
public class PlayerAvatarController {

    private final PlayerAvatarService avatarService;

    public PlayerAvatarController(PlayerAvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarResponseDto> upload(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("file") MultipartFile file
    ) {
        Long userId = user.getUserId();
        avatarService.upload(userId, file);
        return ResponseEntity.ok(
                new AvatarResponseDto("/api/player/" + userId + "/avatar")
        );
    }

    @GetMapping(value = "/{id}/avatar")
    public ResponseEntity<byte[]> get(@PathVariable Long id) {
        AvatarData avatar = avatarService.getAvatar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(avatar.bytes());
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        avatarService.delete(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
