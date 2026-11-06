package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.service.PlayerService;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalQuestRollbackIntegrationTest {

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

    @SpyBean
    private PlayerService playerService;

    @Test
    void shouldRollbackQuestCompletionWhenRewardFails()
            throws Exception {

        // Arrange: создаём пользователя.
        AuthResponse user = authService.register(
                "rollback_" + UUID.randomUUID()
                        .toString().replace("-", ""),
                "TestPassword123!"
        );

        Long playerId = user.getUserId();
        String token = "Bearer " + user.getToken();

        // Создаём персональный квест.
        String createBody = objectMapper.writeValueAsString(
                Map.of(
                        "title", "Rollback Quest",
                        "description", "Transaction rollback test",
                        "targets", Map.of("lessons", 10)
                )
        );

        MvcResult created = mockMvc.perform(
                        post("/api/quests/personal")
                                .header("Authorization", token)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        long questId = objectMapper
                .readTree(
                        created.getResponse()
                                .getContentAsString()
                )
                .get("id")
                .asLong();

        long initialExp = playerRepository
                .findById(playerId)
                .orElseThrow()
                .getExp();

        // Искусственно вызываем ошибку начисления EXP.
        doThrow(new IllegalStateException(
                "Simulated reward failure"
        )).when(playerService).addExp(
                eq(playerId),
                eq(100)
        );

        String progressBody = objectMapper.writeValueAsString(
                Map.of(
                        "action", "lessons",
                        "amount", 10
                )
        );

        // Act: завершение квеста должно выбросить ошибку.
        Exception thrown = assertThrows(
                Exception.class,
                () -> mockMvc.perform(
                                post(
                                        "/api/quests/personal/{id}/progress",
                                        questId
                                )
                                        .header("Authorization", token)
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(progressBody))
                        .andReturn()
        );

        // Проверяем, что ошибка связана
        // именно с начислением награды.
        Throwable cause = thrown;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        assertInstanceOf(
                IllegalStateException.class,
                cause
        );

        assertEquals(
                "Simulated reward failure",
                cause.getMessage()
        );

        // Assert: транзакция должна откатиться.
        var savedQuest = playerQuestRepository
                .findByPlayerIdAndQuestId(playerId, questId)
                .orElseThrow();

        assertEquals(
                QuestStatus.ACTIVE,
                savedQuest.getStatus(),
                "Quest must remain ACTIVE"
        );

        assertEquals(
                0,
                savedQuest.getProgress()
                        .getOrDefault("lessons", 0),
                "Progress must be rolled back"
        );

        long finalExp = playerRepository
                .findById(playerId)
                .orElseThrow()
                .getExp();

        assertEquals(
                initialExp,
                finalExp,
                "EXP must not change"
        );

        System.out.println(
                "Rollback test passed"
        );

        System.out.println(
                "Quest status: " + savedQuest.getStatus()
        );

        System.out.println(
                "Quest progress: " + savedQuest.getProgress()
        );

        System.out.println(
                "EXP before: " + initialExp
        );

        System.out.println(
                "EXP after: " + finalExp
        );
    }
}
