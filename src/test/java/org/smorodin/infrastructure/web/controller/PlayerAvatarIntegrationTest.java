package org.smorodin.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.persistence.PlayerAvatarRepository;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PlayerAvatarIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerAvatarRepository avatarRepository;

    private AuthResponse createUser() {
        return authService.register(
                "avatar_" + UUID.randomUUID().toString().replace("-", ""),
                "TestPassword123!"
        );
    }

    private String bearer(AuthResponse user) {
        return "Bearer " + user.getToken();
    }

    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x3366CC);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, format, output));
            return output.toByteArray();
        }
    }

    private MockMultipartFile file(String filename, String type, byte[] bytes) {
        return new MockMultipartFile("file", filename, type, bytes);
    }

    @Test
    void shouldUploadReadAvatarAndExposeProfileUrl() throws Exception {
        AuthResponse user = createUser();
        byte[] png = image("png");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("avatar.png", "image/png", png))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(
                        "/api/player/" + user.getUserId() + "/avatar"
                ));

        byte[] saved = mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();

        assertEquals(2, ImageIO.read(new java.io.ByteArrayInputStream(saved)).getWidth());

        mockMvc.perform(get("/api/player")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(
                        "/api/player/" + user.getUserId() + "/avatar"
                ));
    }

    @Test
    void shouldReplaceAndDeleteAvatar() throws Exception {
        AuthResponse user = createUser();
        byte[] png = image("png");
        byte[] jpeg = image("jpeg");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("first.png", "image/png", png))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("next.jpg", "image/jpeg", jpeg))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        byte[] saved = mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(2, ImageIO.read(new java.io.ByteArrayInputStream(saved)).getWidth());

        mockMvc.perform(delete("/api/player/avatar")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/player")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isEmpty());
    }

    @Test
    void shouldNotAllowUnauthenticatedWrites() throws Exception {
        byte[] png = image("png");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("avatar.png", "image/png", png)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/player/avatar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidOrMismatchedImages() throws Exception {
        AuthResponse user = createUser();

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("avatar.svg", "image/svg+xml", "<svg/>".getBytes()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("fake.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("fake.jpg", "image/jpeg", image("png")))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("empty.png", "image/png", new byte[0]))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectTooLargeUploadAndDimensions() throws Exception {
        AuthResponse user = createUser();
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("big.png", "image/png", new byte[10 * 1024 * 1024 + 1]))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isPayloadTooLarge());

        BufferedImage wide = new BufferedImage(2049, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(wide, "png", output));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("wide.png", "image/png", output.toByteArray()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldOnlyModifyAuthenticatedPlayersAvatar() throws Exception {
        AuthResponse first = createUser();
        AuthResponse second = createUser();
        byte[] firstImage = image("png");
        byte[] secondImage = image("jpeg");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("first.png", "image/png", firstImage))
                        .header("Authorization", bearer(first)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("second.jpg", "image/jpeg", secondImage))
                        .header("Authorization", bearer(second)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/player/avatar")
                        .header("Authorization", bearer(second)))
                .andExpect(status().isNoContent());

        assertTrue(avatarRepository.existsById(first.getUserId()));

        byte[] saved = mockMvc.perform(get("/api/player/{id}/avatar", first.getUserId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(2, ImageIO.read(new java.io.ByteArrayInputStream(saved)).getWidth());

        mockMvc.perform(get("/api/player/{id}/avatar", second.getUserId()))
                .andExpect(status().isNotFound());
    }
    @Test
    void shouldResizeLargePhotoKeepingAspectRatio() throws Exception {
        AuthResponse user = createUser();
        BufferedImage original = new BufferedImage(4032, 3024, BufferedImage.TYPE_INT_RGB);
        original.setRGB(0, 0, 0x3366CC);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(original, "jpeg", output));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("large.jpg", "image/jpeg", output.toByteArray()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        byte[] stored = mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();

        BufferedImage resized = ImageIO.read(new java.io.ByteArrayInputStream(stored));
        assertEquals(512, resized.getWidth());
        assertEquals(384, resized.getHeight());
        assertTrue(stored.length < 2 * 1024 * 1024);
    }

    @Test
    void shouldAcceptLargeUploadAndCompressIt() throws Exception {
        AuthResponse user = createUser();
        BufferedImage original = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        java.util.Random random = new java.util.Random(1234);
        for (int y = 0; y < 1024; y++) {
            for (int x = 0; x < 1024; x++) {
                original.setRGB(x, y, random.nextInt(0x1000000));
            }
        }
        ByteArrayOutputStream source = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(original, "png", source));
        assertTrue(source.size() > 2 * 1024 * 1024);
        assertTrue(source.size() < 10 * 1024 * 1024);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("large.png", "image/png", source.toByteArray()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        byte[] stored = mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(512, ImageIO.read(new java.io.ByteArrayInputStream(stored)).getWidth());
        assertTrue(stored.length < source.size());
    }

    @Test
    void shouldPreservePngTransparency() throws Exception {
        AuthResponse user = createUser();
        BufferedImage original = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);
        original.setRGB(400, 200, 0xFF3366CC);
        ByteArrayOutputStream source = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(original, "png", source));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("transparent.png", "image/png", source.toByteArray()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        byte[] stored = mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage resized = ImageIO.read(new java.io.ByteArrayInputStream(stored));
        assertEquals(512, resized.getWidth());
        assertEquals(256, resized.getHeight());
        assertEquals(0, resized.getRGB(0, 0) >>> 24);
    }

}
