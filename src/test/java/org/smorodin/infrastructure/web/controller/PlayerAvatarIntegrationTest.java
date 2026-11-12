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
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();

        assertArrayEquals(png, saved);

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
        assertArrayEquals(jpeg, saved);

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
                        .file(file("big.png", "image/png", new byte[2 * 1024 * 1024 + 1]))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isPayloadTooLarge());

        BufferedImage wide = new BufferedImage(2049, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(wide, "png", output));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/player/avatar")
                        .file(file("wide.png", "image/png", output.toByteArray()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());
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
        assertArrayEquals(firstImage, saved);

        mockMvc.perform(get("/api/player/{id}/avatar", second.getUserId()))
                .andExpect(status().isNotFound());
    }
}
