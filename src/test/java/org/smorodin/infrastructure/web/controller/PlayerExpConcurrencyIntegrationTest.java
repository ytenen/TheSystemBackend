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
class PlayerExpConcurrencyIntegrationTest {

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
                "exp_concurrency_" + UUID.randomUUID()
                        .toString().replace("-", ""),
                "TestPassword123!"
        );
    }

    private String bearer(AuthResponse user) {
        return "Bearer " + user.getToken();
    }

    private long createQuest(
            AuthResponse user,
            String title
    ) throws Exception {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "title", title,
                        "description", "EXP concurrency test",
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
                                .content(body))
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

        String body = objectMapper.writeValueAsString(
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
                                .content(body))
                .andReturn();

        return result.getResponse().getStatus();
    }

    private void assertCompleted(
            AuthResponse user,
            long questId
    ) {
        var saved = playerQuestRepository
                .findByPlayerIdAndQuestId(
                        user.getUserId(),
                        questId
                )
                .orElseThrow();

        assertEquals(
                QuestStatus.COMPLETED,
                saved.getStatus()
        );

        assertEquals(
                10,
                saved.getProgress().get("lessons")
        );
    }

    @Test
    void shouldKeepBothExpRewardsWhenDifferentQuestsCompleteConcurrently()
            throws Exception {

        // Arrange: один игрок, два разных квеста.

        AuthResponse user = createUser();

        long firstQuestId = createQuest(
                user, "Concurrent Quest A"
        );

        long secondQuestId = createQuest(
                user, "Concurrent Quest B"
        );

        long initialExp = playerRepository
                .findById(user.getUserId())
                .orElseThrow()
                .getExp();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Integer> firstTask = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new TimeoutException(
                            "Start signal timed out"
                    );
                }

                return completeQuest(
                        user, firstQuestId
                );
            };

            Callable<Integer> secondTask = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new TimeoutException(
                            "Start signal timed out"
                    );
                }

                return completeQuest(
                        user, secondQuestId
                );
            };

            Future<Integer> first =
                    executor.submit(firstTask);

            Future<Integer> second =
                    executor.submit(secondTask);

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both workers must be ready"
            );

            // Act: одновременно запускаем оба запроса.

            start.countDown();

            int firstStatus =
                    first.get(30, TimeUnit.SECONDS);

            int secondStatus =
                    second.get(30, TimeUnit.SECONDS);

            System.out.println(
                    "Quest A status: " + firstStatus
            );

            System.out.println(
                    "Quest B status: " + secondStatus
            );

            // Оба разных квеста должны завершиться.

            assertEquals(200, firstStatus);
            assertEquals(200, secondStatus);

            assertCompleted(user, firstQuestId);
            assertCompleted(user, secondQuestId);

            // Assert: обе награды должны сохраниться.

            long finalExp = playerRepository
                    .findById(user.getUserId())
                    .orElseThrow()
                    .getExp();

            System.out.println(
                    "Initial EXP: " + initialExp
            );

            System.out.println(
                    "Final EXP: " + finalExp
            );

            System.out.println(
                    "EXP gained: " + (finalExp - initialExp)
            );

            assertEquals(
                    initialExp + 200,
                    finalExp,
                    "Both quest rewards must be preserved"
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
