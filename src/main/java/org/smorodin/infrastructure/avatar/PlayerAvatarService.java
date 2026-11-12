package org.smorodin.infrastructure.avatar;

import org.smorodin.infrastructure.persistence.PlayerAvatarEntity;
import org.smorodin.infrastructure.persistence.PlayerAvatarRepository;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Service
public class PlayerAvatarService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 2048;

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
                    HttpStatus.PAYLOAD_TOO_LARGE, "Avatar must not exceed 2 MB"
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
                    HttpStatus.PAYLOAD_TOO_LARGE, "Avatar must not exceed 2 MB"
            );
        }
        validateImage(bytes, contentType);

        if (!playerRepository.existsById(playerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }

        PlayerAvatarEntity avatar = avatarRepository.findById(playerId)
                .orElseGet(() -> new PlayerAvatarEntity(playerId, contentType, bytes));
        avatar.setContentType(contentType);
        avatar.setImageData(bytes);
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

    private static void validateImage(byte[] bytes, String declaredType) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes)
        )) {
            if (stream == null) {
                throw new IllegalArgumentException("Invalid avatar image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Invalid avatar image");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean matches = ("image/png".equals(declaredType) && "png".equals(format))
                        || ("image/jpeg".equals(declaredType)
                        && ("jpeg".equals(format) || "jpg".equals(format)));
                if (!matches) {
                    throw new IllegalArgumentException("Image content does not match its type");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1
                        || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                    throw new IllegalArgumentException(
                            "Avatar dimensions must not exceed 2048 x 2048"
                    );
                }

                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("Invalid avatar image");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid avatar image", exception);
        }
    }

    public record AvatarData(byte[] bytes, String contentType) {
    }
}
