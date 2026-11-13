package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class I18nApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuthService authService;
    @Autowired ObjectMapper objectMapper;

    private AuthResponse user() {
        return authService.register(
                "i18n_" + UUID.randomUUID().toString().replace("-", ""),
                "TestPassword123!"
        );
    }

    private String bearer(AuthResponse user) {
        return "Bearer " + user.getToken();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private MockHttpServletRequestBuilder language(
            MockHttpServletRequestBuilder request, String lang
    ) {
        return lang == null ? request : request.header("Accept-Language", lang);
    }

    @Test
    void shouldTranslateSystemDailyQuestWithoutChangingKeysOrRewards() throws Exception {
        AuthResponse user = user();
        for (String lang : new String[]{"ru", "ru-RU", "en", "fr", null}) {
            boolean russian = "ru".equals(lang) || "ru-RU".equals(lang);
            mockMvc.perform(language(get("/api/quests/daily")
                            .header("Authorization", bearer(user)), lang))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value(
                            russian ? "Ежедневная тренировка" : "Daily Training"))
                    .andExpect(jsonPath("$.description").value(
                            russian ? "Выполни ежедневную тренировку"
                                    : "Complete your daily training"))
                    .andExpect(jsonPath("$.targets.pushups").value(20))
                    .andExpect(jsonPath("$.targets.squats").value(30))
                    .andExpect(jsonPath("$.targets.plank").value(60))
                    .andExpect(jsonPath("$.expReward").value(100));
        }
    }

    @Test
    void shouldLocalizeDailyAndHiddenCompletionWithoutChangingRewards() throws Exception {
        for (String lang : new String[]{"ru", "en"}) {
            AuthResponse user = user();
            mockMvc.perform(language(put("/api/quests/daily/settings")
                            .header("Authorization", bearer(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "mode", "NOW",
                                    "targets", Map.of("pushups", 1)
                            ))), lang))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/quests/daily")
                            .header("Authorization", bearer(user)))
                    .andExpect(status().isOk());

            MockHttpServletRequestBuilder first = post("/api/quests/daily/progress")
                    .header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("action", "pushups", "amount", 1)));
            mockMvc.perform(language(first, lang))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.hiddenCompleted").value(false))
                    .andExpect(jsonPath("$.message").value(
                            "ru".equals(lang) ? "Ежедневный квест завершён!"
                                    : "Daily quest completed!"));

            MockHttpServletRequestBuilder second = post("/api/quests/daily/progress")
                    .header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("action", "pushups", "amount", 1)));
            mockMvc.perform(language(second, lang))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hiddenCompleted").value(true))
                    .andExpect(jsonPath("$.message").value(
                            "ru".equals(lang) ? "Скрытый квест завершён!"
                                    : "Hidden quest completed!"));
        }
    }

    @Test
    void shouldLocalizeProgressUpdateAndFallBackToEnglish() throws Exception {
        AuthResponse user = user();
        mockMvc.perform(get("/api/quests/daily")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        for (String lang : new String[]{"ru", "en", "fr", null}) {
            mockMvc.perform(language(post("/api/quests/daily/progress")
                            .header("Authorization", bearer(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("action", "pushups", "amount", 1))), lang))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "ru".equals(lang) ? "Прогресс квеста обновлён"
                                    : "Quest progress updated"))
                    .andExpect(jsonPath("$.progress.pushups").isNumber());
        }
    }

    @Test
    void shouldLocalizeAuthErrorsIncludingSecurityFilter() throws Exception {
        for (String lang : new String[]{"ru", "en", "fr", null}) {
            mockMvc.perform(language(get("/api/player"), lang))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value(
                            "ru".equals(lang) ? "Необходима авторизация" : "Unauthorized"));
        }

        AuthResponse user = user();
        mockMvc.perform(post("/api/auth/login")
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", user.getUsername(),
                                "password", "wrong-password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Неверное имя пользователя или пароль"));

        mockMvc.perform(post("/api/auth/register")
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", user.getUsername(),
                                "password", "TestPassword123!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Пользователь с таким именем уже существует"));
    }

    @Test
    void shouldLocalizeValidationAndAvatarErrors() throws Exception {
        AuthResponse user = user();
        mockMvc.perform(put("/api/quests/daily/settings")
                        .header("Authorization", bearer(user))
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mode", "NOW", "targets", Map.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Тренировка должна содержать от 1 до 20 упражнений"));

        mockMvc.perform(post("/api/auth/login")
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Некорректный запрос"));

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/player/avatar")
                        .file(new MockMultipartFile("file", "fake.svg", "image/svg+xml",
                                new byte[]{1, 2, 3}))
                        .header("Accept-Language", "ru")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Поддерживаются только аватары PNG и JPEG"));

        mockMvc.perform(get("/api/player/{id}/avatar", user.getUserId())
                        .header("Accept-Language", "ru"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Аватар не найден"));
    }

    @Test
    void shouldNotTranslatePersonalQuestOrActionKeys() throws Exception {
        AuthResponse user = user();
        mockMvc.perform(post("/api/quests/personal")
                        .header("Authorization", bearer(user))
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "My very own training",
                                "description", "My original instructions",
                                "targets", Map.of("custom_move", 7)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My very own training"))
                .andExpect(jsonPath("$.description").value("My original instructions"))
                .andExpect(jsonPath("$.targets.custom_move").value(7))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/quests/daily")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/quests/daily/progress")
                        .header("Authorization", bearer(user))
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("action", "custom_move", "amount", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Неизвестное действие квеста: custom_move"));
    }
}
