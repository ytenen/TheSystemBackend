package org.smorodin.infrastructure.avatar;

import org.smorodin.infrastructure.persistence.PlayerAvatarEntity;
import org.smorodin.infrastructure.persistence.PlayerAvatarRepository;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
public class PlayerAvatarService {

    private static final long MAX_AVATAR_BYTES = AvatarImageProcessor.MAX_UPLOAD_BYTES;

    private final PlayerAvatarRepository avatarRepository;
    private final PlayerRepository playerRepository;

    public PlayerAvatarService(
            PlayerAvatarRepository avatarRepository,
            PlayerRepository playerRepository
    ) {
        this.avatarRepository = avatarRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public void upload(Long playerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar image is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Avatar upload must not exceed 10 MB"
            );
        }

        String contentType = file.getContentType();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            throw new IllegalArgumentException("Only PNG and JPEG avatars are supported");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read avatar image", exception);
        }
        if (bytes.length > MAX_AVATAR_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Avatar upload must not exceed 10 MB"
            );
        }
        AvatarData processed = AvatarImageProcessor.process(bytes, contentType);

        if (!playerRepository.existsById(playerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }

        PlayerAvatarEntity avatar = avatarRepository.findById(playerId)
                .orElseGet(() -> new PlayerAvatarEntity(playerId, processed.contentType(), processed.bytes()));
        avatar.setContentType(processed.contentType());
        avatar.setImageData(processed.bytes());
        avatarRepository.save(avatar);
    }

    @Transactional(readOnly = true)
    public AvatarData getAvatar(Long playerId) {
        PlayerAvatarEntity avatar = avatarRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Avatar not found"
                ));
        return new AvatarData(avatar.getImageData(), avatar.getContentType());
    }

    @Transactional(readOnly = true)
    public boolean hasAvatar(Long playerId) {
        return avatarRepository.existsById(playerId);
    }

    @Transactional
    public void delete(Long playerId) {
        avatarRepository.deleteById(playerId);
    }

    public record AvatarData(byte[] bytes, String contentType) {
    }
}
