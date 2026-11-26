package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalQuestIntegrationTest {

    private static final AtomicInteger USER_COUNTER =
            new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private String createToken() {
        String username = "integration_user_"
                + USER_COUNTER.incrementAndGet();
        String uniqueUsername =
                username + "_" + UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        AuthResponse response = authService.register(
                uniqueUsername,
                "TestPassword123!"
        );

        return response.getToken();
    }

    private String questJson() throws Exception {
        return objectMapper.writeValueAsString(
                Map.of(
                        "title", "Learn Spring Boot",
                        "description", "Study backend development",
                        "targets", Map.of("lessons", 10)
                )
        );
    }

    @Test
    void shouldCreatePersonalQuest() throws Exception {
        String token = createToken();

        mockMvc.perform(post("/api/quests/personal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title")
                        .value("Learn Spring Boot"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.targets.lessons")
                        .value(10));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/quests/personal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCreatedQuest() throws Exception {
        String token = createToken();

        String response = mockMvc.perform(
                        post("/api/quests/personal")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(questJson()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long questId = objectMapper.readTree(response)
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/quests/personal/{id}", questId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questId))
                .andExpect(jsonPath("$.title")
                        .value("Learn Spring Boot"));
    }

    @Test
    void shouldNotAccessAnotherUsersQuest() throws Exception {
        String ownerToken = createToken();
        String otherToken = createToken();

        String response = mockMvc.perform(
                        post("/api/quests/personal")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(questJson()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long questId = objectMapper.readTree(response)
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/quests/personal/{id}", questId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }
}
