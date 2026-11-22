package org.smorodin.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smorodin.core.model.Player;
import org.smorodin.core.model.StatExperience;
import org.smorodin.core.model.enums.StatType;
import org.smorodin.core.service.DailyQuestSettingsService;
import org.smorodin.core.service.PlayerService;
import org.smorodin.core.service.QuestService;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.persistence.PlayerStatProgressRepository;
import org.smorodin.infrastructure.persistence.quest.PlayerDailyQuestSettingsRepository;
import org.smorodin.infrastructure.persistence.quest.PlayerDailyQuestSettingsEntity;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.smorodin.infrastructure.web.dto.quest.UpdateDailyQuestRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatExperienceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private PlayerService playerService;
    @Autowired private QuestService questService;
    @Autowired private DailyQuestSettingsService settingsService;
    @Autowired private PlayerStatProgressRepository progressRepository;
    @Autowired private PlayerDailyQuestSettingsRepository settingsRepository;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private AuthResponse user() {
        return authService.register(
                "stat_xp_" + UUID.randomUUID().toString().replace("-", ""),
                "TestPassword123!"
        );
    }

    private static UpdateDailyQuestRequestDto settings(
            UpdateDailyQuestRequestDto.UpdateMode mode, int target
    ) {
        UpdateDailyQuestRequestDto request = new UpdateDailyQuestRequestDto();
        request.setMode(mode);
        request.setTargets(Map.of("pushups", target));
        return request;
    }

    @Test
    void shouldCreateSixIndependentZeroXpBarsAndExposeThemInProfile() throws Exception {
        AuthResponse user = user();
        Map<StatType, StatExperience> progress = playerService.getStatProgress(user.getUserId());
        assertEquals(6, progress.size());
        assertEquals(6, progressRepository.findAllByPlayerId(user.getUserId()).size());
        for (StatType stat : StatType.values()) {
            assertEquals(0, progress.get(stat).currentXp());
            assertEquals(100, progress.get(stat).requiredXp());
        }

        mvc.perform(get("/api/player")
                        .header("Authorization", "Bearer " + user.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.strength").isNumber())
                .andExpect(jsonPath("$.statProgress.strength.currentXp").value(0))
                .andExpect(jsonPath("$.statProgress.strength.requiredXp").value(100))
                .andExpect(jsonPath("$.statProgress.agility.currentXp").value(0))
                .andExpect(jsonPath("$.statProgress.vitality.currentXp").value(0))
                .andExpect(jsonPath("$.statProgress.intelligence.currentXp").value(0))
                .andExpect(jsonPath("$.statProgress.perception.currentXp").value(0))
                .andExpect(jsonPath("$.statProgress.will.currentXp").value(0));
    }

    @Test
    void shouldPersistIndependentXpRemainderAndMultipleLevelUps() {
        AuthResponse user = user();
        Long id = user.getUserId();
        Player before = playerService.getPlayer(id);
        long expBefore = before.getExp();

        playerService.addStatXp(id, StatType.STRENGTH, 75);
        playerService.addStatXp(id, StatType.STRENGTH, 50);
        playerService.addStatXp(id, StatType.WILL, 350);

        Player after = playerService.getPlayer(id);
        assertEquals(before.getStats().getStrength() + 1, after.getStats().getStrength());
        assertEquals(before.getStats().getWill() + 3, after.getStats().getWill());
        assertEquals(before.getStats().getAgility(), after.getStats().getAgility());
        assertEquals(expBefore, after.getExp());
        assertEquals(25, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
        assertEquals(50, playerService.getStatProgress(id).get(StatType.WILL).currentXp());
        assertEquals(0, playerService.getStatProgress(id).get(StatType.AGILITY).currentXp());
    }

    @Test
    void shouldKeepDirectStatIncreaseSeparateFromXp() {
        AuthResponse user = user();
        Long id = user.getUserId();
        int start = playerService.getPlayer(id).getStats().getStrength();
        playerService.addStatXp(id, StatType.STRENGTH, 75);
        playerService.increaseStat(id, "STRENGTH", 1);
        assertEquals(start + 1, playerService.getPlayer(id).getStats().getStrength());
        assertEquals(75, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
    }

    @Test
    void shouldGrantDailyAndHiddenOnceWithCorrectXpAndExposeActualRewards() throws Exception {
        AuthResponse user = user();
        Long id = user.getUserId();
        settingsService.updateNow(id, settings(UpdateDailyQuestRequestDto.UpdateMode.NOW, 1));
        questService.getDailyQuest(id);
        long initialExp = playerService.getPlayer(id).getExp();

        mvc.perform(get("/api/quests/daily")
                        .header("Authorization", "Bearer " + user.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statXpRewards.STRENGTH").value(25))
                .andExpect(jsonPath("$.statXpRewards.WILL").doesNotExist());

        String body = mapper.writeValueAsString(Map.of("action", "pushups", "amount", 2));
        mvc.perform(post("/api/quests/daily/progress")
                        .header("Authorization", "Bearer " + user.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.hiddenCompleted").value(true))
                .andExpect(jsonPath("$.rewardsGranted.length()").value(2))
                .andExpect(jsonPath("$.rewardsGranted[0].questType").value("DAILY"))
                .andExpect(jsonPath("$.rewardsGranted[0].expReward").value(100))
                .andExpect(jsonPath("$.rewardsGranted[0].statXpRewards.STRENGTH").value(25))
                .andExpect(jsonPath("$.rewardsGranted[1].questType").value("HIDDEN"))
                .andExpect(jsonPath("$.rewardsGranted[1].expReward").value(200))
                .andExpect(jsonPath("$.rewardsGranted[1].statXpRewards.STRENGTH").value(25))
                .andExpect(jsonPath("$.rewardsGranted[1].statXpRewards.WILL").value(25));

        assertEquals(initialExp + 300, playerService.getPlayer(id).getExp());
        assertEquals(50, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
        assertEquals(25, playerService.getStatProgress(id).get(StatType.WILL).currentXp());
        assertEquals(0, playerService.getStatProgress(id).get(StatType.AGILITY).currentXp());

        mvc.perform(post("/api/quests/daily/progress")
                        .header("Authorization", "Bearer " + user.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardsGranted.length()").value(0));

        settingsService.updateNow(id, settings(UpdateDailyQuestRequestDto.UpdateMode.NOW, 2));
        mvc.perform(post("/api/quests/daily/progress")
                        .header("Authorization", "Bearer " + user.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("action", "pushups", "amount", 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardsGranted.length()").value(0));

        assertEquals(initialExp + 300, playerService.getPlayer(id).getExp());
        assertEquals(50, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
    }

    @Test
    void shouldKeepTomorrowPendingAndApplyWhenDateIsDue() {
        AuthResponse user = user();
        Long id = user.getUserId();
        questService.getDailyQuest(id);
        settingsService.scheduleUpdate(id, settings(
                UpdateDailyQuestRequestDto.UpdateMode.TOMORROW, 1
        ));

        assertEquals(20, questService.getDailyQuest(id).getQuest().getTargets().get("pushups"));

        PlayerDailyQuestSettingsEntity entry = settingsRepository.findById(id).orElseThrow();
        entry.setPendingEffectiveDate(LocalDate.now().minusDays(1));
        settingsRepository.save(entry);

        assertEquals(1, questService.getDailyQuest(id).getQuest().getTargets().get("pushups"));
        long previousExp = playerService.getPlayer(id).getExp();
        questService.updateProgress(id, "pushups", 1);
        assertEquals(previousExp + 100, playerService.getPlayer(id).getExp());
        assertEquals(25, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
    }

    @Test
    void shouldSerializeConcurrentStatGrantsWithoutLosingXp() throws Exception {
        AuthResponse user = user();
        Long id = user.getUserId();
        int originalValue = playerService.getPlayer(id).getStats().getStrength();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<Void> work = () -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) {
                    throw new TimeoutException("Start signal timed out");
                }
                playerService.addStatXp(id, StatType.STRENGTH, 75);
                return null;
            };
            Future<Void> first = executor.submit(work);
            Future<Void> second = executor.submit(work);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            go.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            go.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(originalValue + 1, playerService.getPlayer(id).getStats().getStrength());
        assertEquals(50, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
    }

    @Test
    void shouldRejectInvalidStatXpWithoutChangingProgress() {
        AuthResponse user = user();
        Long id = user.getUserId();
        assertThrows(IllegalArgumentException.class,
                () -> playerService.addStatXp(id, StatType.STRENGTH, 0));
        assertThrows(IllegalArgumentException.class,
                () -> playerService.addStatXp(id, StatType.STRENGTH, -1));
        assertEquals(0, playerService.getStatProgress(id).get(StatType.STRENGTH).currentXp());
    }
}
