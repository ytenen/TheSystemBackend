package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.StatType;
import org.smorodin.core.service.DailyQuestSettingsService;
import org.smorodin.core.service.PlayerService;
import org.smorodin.core.service.QuestService;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.smorodin.infrastructure.web.dto.quest.UpdateDailyQuestRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class StatXpRewardRollbackIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private DailyQuestSettingsService settingsService;
    @Autowired private QuestService questService;
    @Autowired private PlayerQuestRepository playerQuestRepository;
    @Autowired private QuestRepository questRepository;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @SpyBean
    private PlayerService playerService;

    @Test
    void shouldRollbackQuestAndExpIfStatXpGrantFails() throws Exception {
        AuthResponse user = authService.register(
                "stat_rollback_" + UUID.randomUUID().toString().replace("-", ""),
                "TestPassword123!"
        );
        Long id = user.getUserId();
        UpdateDailyQuestRequestDto setting = new UpdateDailyQuestRequestDto();
        setting.setMode(UpdateDailyQuestRequestDto.UpdateMode.NOW);
        setting.setTargets(Map.of("pushups", 1));
        settingsService.updateNow(id, setting);
        long questId = questService.getDailyQuest(id).getQuest().getId();
        long startingExp = playerService.getPlayer(id).getExp();
        int startingStrength = playerService.getPlayer(id).getStats().getStrength();

        // General EXP has been granted first when stat XP fails; both must roll back.
        doThrow(new IllegalStateException("Simulated stat XP persistence failure"))
                .when(playerService).addStatXp(eq(id), eq(StatType.STRENGTH), eq(25));

        String body = mapper.writeValueAsString(Map.of("action", "pushups", "amount", 1));
        assertThrows(Exception.class, () -> mvc.perform(
                post("/api/quests/daily/progress")
                        .header("Authorization", "Bearer " + user.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ));

        assertEquals(startingExp, playerService.getPlayer(id).getExp());
        assertEquals(startingStrength, playerService.getPlayer(id).getStats().getStrength());
        assertEquals(0, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
        var progress = playerQuestRepository.findByPlayerIdAndQuestId(id, questId).orElseThrow();
        assertEquals(QuestStatus.ACTIVE, progress.getStatus());
        assertEquals(0, progress.getProgress().getOrDefault("pushups", 0));
    }
}
