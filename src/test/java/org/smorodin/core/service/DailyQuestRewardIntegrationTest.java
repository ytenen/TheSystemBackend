package org.smorodin.core.service;

import org.junit.jupiter.api.Test;
import org.smorodin.core.model.Player;
import org.smorodin.infrastructure.auth.AuthService;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.smorodin.infrastructure.web.dto.quest.UpdateDailyQuestRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DailyQuestRewardIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private QuestService questService;

    @Autowired
    private DailyQuestSettingsService settingsService;

    private Long createPlayer() {
        String username = "reward_test_"
                + UUID.randomUUID().toString().replace("-", "");

        AuthResponse response =
                authService.register(username, "TestPassword123!");

        return response.getUserId();
    }

    private UpdateDailyQuestRequestDto request(
            Map<String, Integer> targets
    ) {
        UpdateDailyQuestRequestDto request =
                new UpdateDailyQuestRequestDto();

        request.setMode(
                UpdateDailyQuestRequestDto.UpdateMode.NOW
        );
        request.setTargets(targets);

        return request;
    }

    private long exp(Long playerId) {
        Player player = playerService.getPlayer(playerId);
        return player.getExp();
    }

    @Test
    void shouldNotRewardDailyOrHiddenTwiceAfterNowUpdate() {
        Long playerId = createPlayer();

        Map<String, Integer> firstTargets =
                Map.of("pushups", 20);

        settingsService.updateNow(
                playerId,
                request(firstTargets)
        );

        // Создаём прогресс игрока.
        questService.getDailyQuest(playerId);

        long initialExp = exp(playerId);

        // Завершаем Daily.
        questService.updateProgress(playerId, "pushups", 20);

        assertEquals(initialExp + 100, exp(playerId));

        // Меняем цели после получения награды.
        settingsService.updateNow(
                playerId,
                request(Map.of("pushups", 30))
        );

        // Выполняем новые цели.
        questService.updateProgress(playerId, "pushups", 30);

        // Daily не должен выдать награду повторно.
        assertEquals(initialExp + 100, exp(playerId));

        // Достигаем удвоенной цели.
        questService.updateProgress(playerId, "pushups", 30);

        // Hidden выдаёт 200 EXP.
        assertEquals(initialExp + 300, exp(playerId));

        // Снова меняем цели.
        settingsService.updateNow(
                playerId,
                request(Map.of("pushups", 40))
        );

        questService.updateProgress(playerId, "pushups", 80);

        // Награды за сегодняшний день уже получены.
        assertEquals(initialExp + 300, exp(playerId));
    }
}
