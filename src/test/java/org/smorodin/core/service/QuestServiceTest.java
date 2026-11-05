package org.smorodin.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestEntity;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.persistence.quest.QuestEntity;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    private static final Long PLAYER_ID = 1L;
    private static final Long DAILY_ID = 10L;
    private static final Long HIDDEN_ID = 11L;

    private static final Map<String, Integer> TARGETS = Map.of(
            "pushups", 20,
            "squats", 30,
            "plank", 60
    );

    @Mock
    private QuestRepository questRepository;

    @Mock
    private PlayerQuestRepository playerQuestRepository;

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private QuestService questService;

    private QuestEntity daily;
    private QuestEntity hidden;
    private PlayerQuestEntity dailyProgress;
    private PlayerQuestEntity hiddenProgress;

    @BeforeEach
    void setUp() {
        daily = mock(QuestEntity.class);
        hidden = mock(QuestEntity.class);

        Quest dailyDomain = mock(Quest.class);
        Quest hiddenDomain = mock(Quest.class);

        lenient().when(daily.getId()).thenReturn(DAILY_ID);
        lenient().when(daily.getTargets()).thenReturn(TARGETS);
        lenient().when(daily.getExpReward()).thenReturn(100);
        lenient().when(daily.toDomain()).thenReturn(dailyDomain);

        lenient().when(dailyDomain.getResetInterval())
                .thenReturn(ResetInterval.DAILY);

        lenient().when(hidden.getId()).thenReturn(HIDDEN_ID);
        lenient().when(hidden.getExpReward()).thenReturn(200);
        lenient().when(hidden.toDomain()).thenReturn(hiddenDomain);

        lenient().when(hiddenDomain.getResetInterval())
                .thenReturn(ResetInterval.DAILY);

        dailyProgress = createProgress(DAILY_ID);
        hiddenProgress = createProgress(HIDDEN_ID);
    }

    private PlayerQuestEntity createProgress(Long questId) {
        PlayerQuestEntity entity = new PlayerQuestEntity();

        entity.setId(questId + 100);
        entity.setPlayerId(PLAYER_ID);
        entity.setQuestId(questId);
        entity.setProgress(new HashMap<>());
        entity.setStatus(QuestStatus.ACTIVE);
        entity.setLastResetDate(LocalDate.now());

        return entity;
    }


    private void configureDailyOnly() {
        when(questRepository.findByType(QuestType.DAILY))
                .thenReturn(List.of(daily));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, DAILY_ID
        )).thenReturn(Optional.of(dailyProgress));
    }


    private void configureDailyAndHidden() {
        when(questRepository.findByType(QuestType.DAILY))
                .thenReturn(List.of(daily));

        when(questRepository.findByType(QuestType.HIDDEN))
                .thenReturn(List.of(hidden));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, DAILY_ID
        )).thenReturn(Optional.of(dailyProgress));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, HIDDEN_ID
        )).thenReturn(Optional.of(hiddenProgress));

        when(playerQuestRepository.save(any(PlayerQuestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldIncreaseDailyProgress() {
        configureDailyOnly();

        when(questRepository.findByType(QuestType.HIDDEN))
                .thenReturn(List.of());

        when(playerQuestRepository.save(any(PlayerQuestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestService.QuestUpdateResult result =
                questService.updateProgress(
                        PLAYER_ID, "pushups", 5
                );

        assertEquals(
                5,
                result.getPlayerQuest()
                        .getProgress().get("pushups")
        );

        assertFalse(result.isMainCompleted());

        verify(rewardService, never())
                .addExp(anyLong(), anyInt());
    }

    @Test
    void shouldCompleteDailyAndGiveReward() {
        configureDailyOnly();

        when(questRepository.findByType(QuestType.HIDDEN))
                .thenReturn(List.of());

        when(playerQuestRepository.save(any(PlayerQuestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dailyProgress.setProgress(new HashMap<>(Map.of(
                "pushups", 20,
                "squats", 30,
                "plank", 59
        )));

        QuestService.QuestUpdateResult result =
                questService.updateProgress(
                        PLAYER_ID, "plank", 1
                );

        assertTrue(result.isMainCompleted());
        assertTrue(result.getPlayerQuest().isCompleted());

        verify(rewardService, times(1))
                .addExp(PLAYER_ID, 100);
    }

    @Test
    void shouldNotRewardCompletedDailyTwice() {
        configureDailyOnly();

        when(questRepository.findByType(QuestType.HIDDEN))
                .thenReturn(List.of());

        when(playerQuestRepository.save(any(PlayerQuestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dailyProgress.setStatus(QuestStatus.COMPLETED);
        dailyProgress.setProgress(new HashMap<>(TARGETS));

        QuestService.QuestUpdateResult result =
                questService.updateProgress(
                        PLAYER_ID, "pushups", 1
                );

        assertFalse(result.isMainCompleted());

        assertEquals(
                21,
                result.getPlayerQuest()
                        .getProgress().get("pushups")
        );

        verify(rewardService, never())
                .addExp(anyLong(), anyInt());
    }

    @Test
    void shouldNotCompleteHiddenAtNormalDailyTargets() {
        configureDailyAndHidden();

        dailyProgress.setProgress(new HashMap<>(Map.of(
                "pushups", 20,
                "squats", 30,
                "plank", 59
        )));

        QuestService.QuestUpdateResult result =
                questService.updateProgress(
                        PLAYER_ID, "plank", 1
                );

        assertTrue(result.isMainCompleted());
        assertFalse(result.isHiddenCompleted());

        verify(rewardService).addExp(PLAYER_ID, 100);
        verify(rewardService, never())
                .addExp(PLAYER_ID, 200);
    }

    @Test
    void shouldCompleteHiddenAtDoubleTargets() {
        configureDailyAndHidden();

        dailyProgress.setStatus(QuestStatus.COMPLETED);

        dailyProgress.setProgress(new HashMap<>(Map.of(
                "pushups", 40,
                "squats", 60,
                "plank", 119
        )));

        QuestService.QuestUpdateResult result =
                questService.updateProgress(
                        PLAYER_ID, "plank", 1
                );

        assertTrue(result.isHiddenCompleted());
        assertFalse(result.isMainCompleted());

        verify(rewardService, times(1))
                .addExp(PLAYER_ID, 200);

        verify(rewardService, never())
                .addExp(PLAYER_ID, 100);
    }

    @Test
    void shouldNotRewardHiddenTwice() {
        configureDailyAndHidden();

        dailyProgress.setStatus(QuestStatus.COMPLETED);

        dailyProgress.setProgress(new HashMap<>(Map.of(
                "pushups", 40,
                "squats", 60,
                "plank", 120
        )));

        hiddenProgress.setStatus(QuestStatus.COMPLETED);

        QuestService.QuestUpdateResult result =
                questService.updateProgress(
                        PLAYER_ID, "plank", 1
                );

        assertFalse(result.isHiddenCompleted());

        verify(rewardService, never())
                .addExp(anyLong(), anyInt());
    }

    @Test
    void shouldRejectUnknownAction() {
        configureDailyOnly();

        assertThrows(
                IllegalArgumentException.class,
                () -> questService.updateProgress(
                        PLAYER_ID, "unknown", 1
                )
        );

        verify(rewardService, never())
                .addExp(anyLong(), anyInt());
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        configureDailyOnly();

        assertThrows(
                IllegalArgumentException.class,
                () -> questService.updateProgress(
                        PLAYER_ID, "pushups", 0
                )
        );
    }

    @Test
    void shouldResetDailyOnNextDay() {
        when(questRepository.findByType(QuestType.DAILY))
                .thenReturn(List.of(daily));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, DAILY_ID
        )).thenReturn(Optional.of(dailyProgress));

        when(playerQuestRepository.save(any(PlayerQuestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dailyProgress.setLastResetDate(
                LocalDate.now().minusDays(1)
        );

        dailyProgress.setStatus(QuestStatus.COMPLETED);
        dailyProgress.setProgress(new HashMap<>(TARGETS));

        QuestService.DailyQuestResult result =
                questService.getDailyQuest(PLAYER_ID);

        assertEquals(
                QuestStatus.ACTIVE,
                result.getPlayerQuest().getStatus()
        );

        assertTrue(
                result.getPlayerQuest().getProgress().isEmpty()
        );

        assertEquals(
                LocalDate.now(),
                result.getPlayerQuest().getLastResetDate()
        );
    }
}
