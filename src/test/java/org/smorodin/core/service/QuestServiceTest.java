package org.smorodin.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smorodin.core.model.Quest;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.core.model.enums.ResetInterval;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestEntity;
import org.smorodin.infrastructure.persistence.quest.PlayerQuestRepository;
import org.smorodin.infrastructure.persistence.quest.QuestEntity;
import org.smorodin.infrastructure.persistence.quest.QuestRepository;

import java.time.*;
import java.util.*;

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

    @Spy
    private MutableClock clock = new MutableClock(
            Instant.parse("2026-11-05T12:00:00Z"),
            ZoneOffset.UTC
    );

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
        entity.setLastResetDate(LocalDate.now(clock));

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
                LocalDate.now(clock).minusDays(1)
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
                LocalDate.now(clock),
                result.getPlayerQuest().getLastResetDate()
        );
    }


    @Test
    void shouldResetDailyWhenDateChanges() {
        when(questRepository.findByType(QuestType.DAILY))
                .thenReturn(List.of(daily));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, DAILY_ID
        )).thenReturn(Optional.of(dailyProgress));

        when(playerQuestRepository.save(any(PlayerQuestEntity.class)))
                .thenAnswer(invocation -> {
                    PlayerQuestEntity saved = invocation.getArgument(0);
                    dailyProgress = saved;
                    return saved;
                });

        dailyProgress.setStatus(QuestStatus.COMPLETED);
        dailyProgress.setProgress(new HashMap<>(TARGETS));

        QuestService.DailyQuestResult firstDay =
                questService.getDailyQuest(PLAYER_ID);

        assertEquals(
                QuestStatus.COMPLETED,
                firstDay.getPlayerQuest().getStatus()
        );

        clock.setInstant(
                Instant.parse("2026-11-06T12:00:00Z")
        );

        QuestService.DailyQuestResult secondDay =
                questService.getDailyQuest(PLAYER_ID);

        assertEquals(
                QuestStatus.ACTIVE,
                secondDay.getPlayerQuest().getStatus()
        );

        assertTrue(
                secondDay.getPlayerQuest().getProgress().isEmpty()
        );

        assertEquals(
                LocalDate.of(2026, 11, 6),
                secondDay.getPlayerQuest().getLastResetDate()
        );
    }

    @Test
    void shouldResetHiddenOnNextDay() {
        configureDailyAndHidden();

        dailyProgress.setStatus(QuestStatus.COMPLETED);
        dailyProgress.setProgress(new HashMap<>(TARGETS));

        hiddenProgress.setStatus(QuestStatus.COMPLETED);

        clock.setInstant(
                Instant.parse("2026-11-06T12:00:00Z")
        );

        questService.updateProgress(
                PLAYER_ID, "pushups", 1
        );

        verify(playerQuestRepository, atLeastOnce())
                .save(argThat(entity ->
                        entity.getQuestId().equals(HIDDEN_ID)
                                && entity.getStatus() == QuestStatus.ACTIVE
                                && entity.getLastResetDate().equals(
                                LocalDate.of(2026, 11, 6)
                        )
                ));

        verify(rewardService, never())
                .addExp(PLAYER_ID, 200);
    }

    @Test
    void shouldRewardDailyAndHiddenAgainAfterReset() {
        configureDailyAndHidden();

        dailyProgress.setStatus(QuestStatus.COMPLETED);
        dailyProgress.setProgress(new HashMap<>(Map.of(
                "pushups", 40,
                "squats", 60,
                "plank", 120
        )));

        hiddenProgress.setStatus(QuestStatus.COMPLETED);

        clock.setInstant(
                Instant.parse("2026-11-06T12:00:00Z")
        );

        dailyProgress.setLastResetDate(
                LocalDate.of(2026, 11, 5)
        );

        hiddenProgress.setLastResetDate(
                LocalDate.of(2026, 11, 5)
        );

        // После смены суток выполняем Daily.
        dailyProgress.setProgress(new HashMap<>(Map.of(
                "pushups", 20,
                "squats", 30,
                "plank", 59
        )));

        dailyProgress.setLastResetDate(
                LocalDate.of(2026, 11, 6)
        );

        dailyProgress.setStatus(QuestStatus.ACTIVE);

        QuestService.QuestUpdateResult dailyResult =
                questService.updateProgress(
                        PLAYER_ID, "plank", 1
                );

        assertTrue(dailyResult.isMainCompleted());

        verify(rewardService, times(1))
                .addExp(PLAYER_ID, 100);

        // Проверяем повторную выдачу награды Hidden.
        // Состояние Hidden после нового дня сбрасывается.
        hiddenProgress.setStatus(QuestStatus.ACTIVE);
        hiddenProgress.setLastResetDate(
                LocalDate.of(2026, 11, 6)
        );

        dailyProgress.setStatus(QuestStatus.COMPLETED);
        dailyProgress.setProgress(new HashMap<>(Map.of(
                "pushups", 40,
                "squats", 60,
                "plank", 119
        )));

        QuestService.QuestUpdateResult hiddenResult =
                questService.updateProgress(
                        PLAYER_ID, "plank", 1
                );

        assertTrue(hiddenResult.isHiddenCompleted());

        verify(rewardService, times(1))
                .addExp(PLAYER_ID, 200);
    }

    private static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = Objects.requireNonNull(instant);
            this.zone = Objects.requireNonNull(zone);
        }

        void setInstant(Instant instant) {
            this.instant = Objects.requireNonNull(instant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

}
