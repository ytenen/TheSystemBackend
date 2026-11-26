package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalQuestProgressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerQuestRepository playerQuestRepository;

    private AuthResponse createUser() {
        return authService.register(
                "progress_" + UUID.randomUUID()
                        .toString().replace("-", ""),
                "TestPassword123!"
        );
    }

    private String bearer(AuthResponse user) {
        return "Bearer " + user.getToken();
    }

    private long createQuest(AuthResponse user)
            throws Exception {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "title", "Integration Quest",
                        "description", "Test quest progress",
                        "targets", Map.of("lessons", 10)
                )
        );

        String response = mockMvc.perform(
                        post("/api/quests/personal")
                                .header(
                                        "Authorization",
                                        bearer(user)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    private String progressJson(
            String action,
            int amount
    ) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of(
                        "action", action,
                        "amount", amount
                )
        );
    }

    private JsonNode updateProgress(
            AuthResponse user,
            long questId,
            int amount
    ) throws Exception {

        String response = mockMvc.perform(
                        post(
                                "/api/quests/personal/{id}/progress",
                                questId
                        )
                                .header(
                                        "Authorization",
                                        bearer(user)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        progressJson("lessons", amount)
                                ))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    @Test
    void shouldPersistProgressInDatabase()
            throws Exception {

        AuthResponse user = createUser();
        long questId = createQuest(user);

        JsonNode result = updateProgress(
                user, questId, 4
        );

        assertEquals(
                4,
                result.path("progress")
                        .path("lessons")
                        .asInt()
        );

        assertEquals(
                "ACTIVE",
                result.path("status").asText()
        );

        var saved = playerQuestRepository
                .findByPlayerIdAndQuestId(
                        user.getUserId(),
                        questId
                )
                .orElseThrow();

        assertEquals(
                4,
                saved.getProgress().get("lessons")
        );

        mockMvc.perform(
                        get("/api/quests/personal/{id}", questId)
                                .header(
                                        "Authorization",
                                        bearer(user)
                                ))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.progress.lessons").value(4)
                );
    }

    @Test
    void shouldGiveExpOnceAfterCompletion()
            throws Exception {

        AuthResponse user = createUser();
        long questId = createQuest(user);

        long initialExp = playerRepository
                .findById(user.getUserId())
                .orElseThrow()
                .getExp();

        updateProgress(user, questId, 4);

        long intermediateExp = playerRepository
                .findById(user.getUserId())
                .orElseThrow()
                .getExp();

        assertEquals(initialExp, intermediateExp);

        JsonNode completed = updateProgress(
                user, questId, 6
        );

        assertEquals(
                "COMPLETED",
                completed.path("status").asText()
        );

        assertEquals(
                10,
                completed.path("progress")
                        .path("lessons")
                        .asInt()
        );

        long completedExp = playerRepository
                .findById(user.getUserId())
                .orElseThrow()
                .getExp();

        assertEquals(
                initialExp + 100,
                completedExp
        );

        mockMvc.perform(
                        post(
                                "/api/quests/personal/{id}/progress",
                                questId
                        )
                                .header(
                                        "Authorization",
                                        bearer(user)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        progressJson("lessons", 1)
                                ))
                .andExpect(status().isConflict());

        long finalExp = playerRepository
                .findById(user.getUserId())
                .orElseThrow()
                .getExp();

        assertEquals(completedExp, finalExp);
    }

    @Test
    void shouldCapProgressAtTarget()
            throws Exception {

        AuthResponse user = createUser();
        long questId = createQuest(user);

        JsonNode result = updateProgress(
                user, questId, 100
        );

        assertEquals(
                10,
                result.path("progress")
                        .path("lessons")
                        .asInt()
        );

        assertEquals(
                "COMPLETED",
                result.path("status").asText()
        );

        var saved = playerQuestRepository
                .findByPlayerIdAndQuestId(
                        user.getUserId(),
                        questId
                )
                .orElseThrow();

        assertEquals(
                10,
                saved.getProgress().get("lessons")
        );
    }

    @Test
    void shouldRejectForeignProgressUpdate()
            throws Exception {

        AuthResponse owner = createUser();
        AuthResponse stranger = createUser();

        long questId = createQuest(owner);

        long initialExp = playerRepository
                .findById(stranger.getUserId())
                .orElseThrow()
                .getExp();

        mockMvc.perform(
                        post(
                                "/api/quests/personal/{id}/progress",
                                questId
                        )
                                .header(
                                        "Authorization",
                                        bearer(stranger)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        progressJson("lessons", 10)
                                ))
                .andExpect(status().isNotFound());

        var saved = playerQuestRepository
                .findByPlayerIdAndQuestId(
                        owner.getUserId(),
                        questId
                )
                .orElseThrow();

        assertTrue(saved.getProgress().isEmpty());

        long finalExp = playerRepository
                .findById(stranger.getUserId())
                .orElseThrow()
                .getExp();

        assertEquals(initialExp, finalExp);
    }
}
