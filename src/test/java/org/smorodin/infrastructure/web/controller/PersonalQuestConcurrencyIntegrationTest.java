package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.persistence.PlayerRepository;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalQuestConcurrencyIntegrationTest {

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
        String username = "concurrency_"
                + UUID.randomUUID().toString()
                .replace("-", "");

        return authService.register(
                username,
                "TestPassword123!"
        );
    }

    private String bearer(AuthResponse user) {
        return "Bearer " + user.getToken();
    }

    private long createQuest(AuthResponse user)
            throws Exception {

        String requestBody = objectMapper.writeValueAsString(
                Map.of(
                        "title", "Concurrent Quest",
                        "description", "Concurrency test",
                        "targets", Map.of("lessons", 10)
                )
        );

        MvcResult result = mockMvc.perform(
                        post("/api/quests/personal")
                                .header(
                                        "Authorization",
                                        bearer(user)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("id")
                .asLong();
    }

    private int completeQuest(
            AuthResponse user,
            long questId
    ) throws Exception {

        String requestBody = objectMapper.writeValueAsString(
                Map.of(
                        "action", "lessons",
                        "amount", 10
                )
        );

        MvcResult result = mockMvc.perform(
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
                                .content(requestBody))
                .andReturn();

        return result.getResponse().getStatus();
    }

    @Test
    void shouldRewardQuestOnlyOnceUnderConcurrentRequests()
            throws Exception {

        // Arrange: создаём пользователя и квест.

        AuthResponse user = createUser();
        long questId = createQuest(user);

        long initialExp = playerRepository
                .findById(user.getUserId())
                .orElseThrow()
                .getExp();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> task = () -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new TimeoutException(
                        "Start signal was not received"
                );
            }

            return completeQuest(user, questId);
        };

        try {
            // Act: оба потока ожидают общего сигнала.

            Future<Integer> first = executor.submit(task);
            Future<Integer> second = executor.submit(task);

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both workers must be ready"
            );

            start.countDown();

            int firstStatus =
                    first.get(30, TimeUnit.SECONDS);

            int secondStatus =
                    second.get(30, TimeUnit.SECONDS);

            System.out.println(
                    "First request status: " + firstStatus
            );

            System.out.println(
                    "Second request status: " + secondStatus
            );

            // Assert: только один запрос должен
            // успешно завершить квест.

            long successfulRequests =
                    (firstStatus == 200 ? 1 : 0)
                            + (secondStatus == 200 ? 1 : 0);

            assertEquals(
                    1,
                    successfulRequests,
                    "Exactly one request should succeed"
            );

            // Проверяем состояние в базе данных.

            var savedQuest = playerQuestRepository
                    .findByPlayerIdAndQuestId(
                            user.getUserId(),
                            questId
                    )
                    .orElseThrow();

            assertEquals(
                    QuestStatus.COMPLETED,
                    savedQuest.getStatus()
            );

            assertEquals(
                    10,
                    savedQuest.getProgress().get("lessons")
            );

            long finalExp = playerRepository
                    .findById(user.getUserId())
                    .orElseThrow()
                    .getExp();

            assertEquals(
                    initialExp + 100,
                    finalExp,
                    "Quest must reward exactly 100 EXP"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            10, TimeUnit.SECONDS
                    ),
                    "Worker threads did not terminate"
            );
        }
    }
}
