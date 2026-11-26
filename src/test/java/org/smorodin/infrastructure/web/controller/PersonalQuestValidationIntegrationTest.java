package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;
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
class PersonalQuestValidationIntegrationTest {

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

    @Autowired
    private QuestRepository questRepository;

    private AuthResponse createUser() {
        return authService.register(
                "validation_" + UUID.randomUUID()
                        .toString().replace("-", ""),
                "TestPassword123!"
        );
    }

    private String bearer(AuthResponse user) {
        return "Bearer " + user.getToken();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private long createQuest(AuthResponse user)
            throws Exception {

        String response = mockMvc.perform(
                        post("/api/quests/personal")
                                .header(
                                        "Authorization",
                                        bearer(user)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json(Map.of(
                                        "title", "Validation Quest",
                                        "targets", Map.of("lessons", 10)
                                ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    private void sendProgress(
            AuthResponse user,
            long questId,
            String action,
            int amount,
            int expectedStatus
    ) throws Exception {

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
                                .content(json(Map.of(
                                        "action", action,
                                        "amount", amount
                                ))))
                .andExpect(status().is(expectedStatus));
    }

    private int savedProgress(
            AuthResponse user,
            long questId
    ) {
        return playerQuestRepository
                .findByPlayerIdAndQuestId(
                        user.getUserId(),
                        questId
                )
                .orElseThrow()
                .getProgress()
                .getOrDefault("lessons", 0);
    }

    private long savedExp(AuthResponse user) {
        return playerRepository
                .findById(user.getUserId())
                .orElseThrow()
                .getExp();
    }

    @Test
    void shouldRejectNonPositiveProgress()
            throws Exception {

        AuthResponse user = createUser();
        long questId = createQuest(user);

        long initialExp = savedExp(user);

        sendProgress(
                user, questId, "lessons", 0, 400
        );

        sendProgress(
                user, questId, "lessons", -5, 400
        );

        assertEquals(0, savedProgress(user, questId));
        assertEquals(initialExp, savedExp(user));
    }

    @Test
    void shouldRejectUnknownAction()
            throws Exception {

        AuthResponse user = createUser();
        long questId = createQuest(user);

        sendProgress(
                user, questId, "unknown", 5, 400
        );

        assertEquals(0, savedProgress(user, questId));
    }

    @Test
    void shouldRejectQuestWithoutTargets()
            throws Exception {

        AuthResponse user = createUser();

        long before = questRepository.count();

        mockMvc.perform(
                        post("/api/quests/personal")
                                .header(
                                        "Authorization",
                                        bearer(user)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(json(Map.of(
                                        "title", "Empty Quest",
                                        "targets", Map.of()
                                ))))
                .andExpect(status().isBadRequest());

        assertEquals(
                before,
                questRepository.count()
        );
    }

    @Test
    void shouldRejectProgressAfterCompletion()
            throws Exception {

        AuthResponse user = createUser();
        long questId = createQuest(user);

        sendProgress(
                user, questId, "lessons", 10, 200
        );

        long completedExp = savedExp(user);

        sendProgress(
                user, questId, "lessons", 1, 409
        );

        assertEquals(
                10,
                savedProgress(user, questId)
        );

        assertEquals(
                completedExp,
                savedExp(user)
        );
    }

    @Test
    void shouldNotChangeDatabaseAfterInvalidRequest()
            throws Exception {

        AuthResponse user = createUser();
        long questId = createQuest(user);

        sendProgress(
                user, questId, "lessons", 4, 200
        );

        int progressBefore = savedProgress(user, questId);
        long expBefore = savedExp(user);

        sendProgress(
                user, questId, "unknown", 100, 400
        );

        assertEquals(
                progressBefore,
                savedProgress(user, questId)
        );

        assertEquals(
                expBefore,
                savedExp(user)
        );

        var saved = playerQuestRepository
                .findByPlayerIdAndQuestId(
                        user.getUserId(),
                        questId
                )
                .orElseThrow();

        assertEquals(
                org.smorodin.core.model.enums.QuestStatus.ACTIVE,
                saved.getStatus()
        );
    }
}
