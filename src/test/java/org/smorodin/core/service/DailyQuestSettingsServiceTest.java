package org.smorodin.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.infrastructure.persistence.quest.*;
import org.smorodin.infrastructure.web.dto.quest.UpdateDailyQuestRequestDto;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyQuestSettingsServiceTest {

    private static final long PLAYER_ID = 1L;
    private static final long DAILY_ID = 10L;
    private static final long HIDDEN_ID = 11L;

    private static final Map<String, Integer> DEFAULT =
            Map.of("pushups", 20, "squats", 30, "plank", 60);

    private static final Map<String, Integer> NEW =
            Map.of("pushups", 30, "squats", 40, "plank", 90);

    private static final Map<String, Integer> LATER =
            Map.of("pushups", 50, "squats", 60, "plank", 120);

    @Mock
    private PlayerDailyQuestSettingsRepository settingsRepository;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private DailyQuestLockService dailyQuestLockService;

    @Mock
    private PlayerQuestRepository playerQuestRepository;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-07T12:00:00Z"),
            ZoneOffset.UTC
    );

    private DailyQuestSettingsService service;

    @BeforeEach
    void setUp() {
        service = new DailyQuestSettingsService(
                settingsRepository,
                questRepository,
                clock,
                dailyQuestLockService,
                playerQuestRepository
        );
    }

    private UpdateDailyQuestRequestDto request(
            UpdateDailyQuestRequestDto.UpdateMode mode,
            Map<String, Integer> targets
    ) {
        UpdateDailyQuestRequestDto dto =
                new UpdateDailyQuestRequestDto();

        dto.setMode(mode);
        dto.setTargets(targets);

        return dto;
    }

    private PlayerDailyQuestSettingsEntity settings() {
        PlayerDailyQuestSettingsEntity settings =
                new PlayerDailyQuestSettingsEntity();

        settings.setPlayerId(PLAYER_ID);
        settings.setActiveTargets(new HashMap<>(DEFAULT));

        return settings;
    }

    private PlayerQuestEntity progress(
            long questId,
            QuestStatus status,
            LocalDate date
    ) {
        PlayerQuestEntity pq = new PlayerQuestEntity();

        pq.setPlayerId(PLAYER_ID);
        pq.setQuestId(questId);
        pq.setStatus(status);
        pq.setLastResetDate(date);
        pq.setProgress(
                new HashMap<>(Map.of("pushups", 20))
        );

        return pq;
    }

    private void templates() {
        QuestEntity daily = mock(QuestEntity.class);
        QuestEntity hidden = mock(QuestEntity.class);

        when(daily.getId()).thenReturn(DAILY_ID);
        when(hidden.getId()).thenReturn(HIDDEN_ID);

        when(questRepository.findByType(QuestType.DAILY))
                .thenReturn(List.of(daily));

        when(questRepository.findByType(QuestType.HIDDEN))
                .thenReturn(List.of(hidden));
    }

    @Test
    void nowClearsProgressAndKeepsTodaysCompletions() {
        templates();

        PlayerDailyQuestSettingsEntity settings = settings();
        settings.setPendingTargets(new HashMap<>(LATER));
        settings.setPendingEffectiveDate(
                LocalDate.of(2026, 11, 8)
        );

        PlayerQuestEntity daily = progress(
                DAILY_ID,
                QuestStatus.COMPLETED,
                LocalDate.of(2026, 11, 7)
        );

        PlayerQuestEntity hidden = progress(
                HIDDEN_ID,
                QuestStatus.COMPLETED,
                LocalDate.of(2026, 11, 7)
        );

        when(settingsRepository.findForUpdate(PLAYER_ID))
                .thenReturn(Optional.of(settings));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, DAILY_ID
        )).thenReturn(Optional.of(daily));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, HIDDEN_ID
        )).thenReturn(Optional.of(hidden));

        service.updateNow(
                PLAYER_ID,
                request(
                        UpdateDailyQuestRequestDto.UpdateMode.NOW,
                        NEW
                )
        );

        assertEquals(NEW, settings.getActiveTargets());
        assertNull(settings.getPendingTargets());
        assertNull(settings.getPendingEffectiveDate());

        assertTrue(daily.getProgress().isEmpty());
        assertTrue(hidden.getProgress().isEmpty());

        assertEquals(
                QuestStatus.COMPLETED,
                daily.getStatus()
        );

        assertEquals(
                QuestStatus.COMPLETED,
                hidden.getStatus()
        );

        verify(playerQuestRepository, times(2))
                .save(any(PlayerQuestEntity.class));

        verify(dailyQuestLockService)
                .lockPlayer(PLAYER_ID);
    }

    @Test
    void nowResetsUnfinishedQuest() {
        templates();

        PlayerDailyQuestSettingsEntity settings = settings();

        PlayerQuestEntity daily = progress(
                DAILY_ID,
                QuestStatus.ACTIVE,
                LocalDate.of(2026, 11, 7)
        );

        when(settingsRepository.findForUpdate(PLAYER_ID))
                .thenReturn(Optional.of(settings));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, DAILY_ID
        )).thenReturn(Optional.of(daily));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, HIDDEN_ID
        )).thenReturn(Optional.empty());

        service.updateNow(
                PLAYER_ID,
                request(
                        UpdateDailyQuestRequestDto.UpdateMode.NOW,
                        NEW
                )
        );

        assertEquals(
                QuestStatus.ACTIVE,
                daily.getStatus()
        );

        assertTrue(daily.getProgress().isEmpty());

        assertEquals(
                LocalDate.of(2026, 11, 7),
                daily.getLastResetDate()
        );
    }

    @Test
    void nowDoesNotKeepYesterdaysCompletion() {
        templates();

        PlayerQuestEntity daily = progress(
                DAILY_ID,
                QuestStatus.COMPLETED,
                LocalDate.of(2026, 11, 6)
        );

        when(settingsRepository.findForUpdate(PLAYER_ID))
                .thenReturn(Optional.of(settings()));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, DAILY_ID
        )).thenReturn(Optional.of(daily));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, HIDDEN_ID
        )).thenReturn(Optional.empty());

        service.updateNow(
                PLAYER_ID,
                request(
                        UpdateDailyQuestRequestDto.UpdateMode.NOW,
                        NEW
                )
        );

        assertEquals(
                QuestStatus.ACTIVE,
                daily.getStatus()
        );

        assertNull(daily.getCompletedAt());
        assertTrue(daily.getProgress().isEmpty());
    }

    @Test
    void tomorrowDoesNotChangeTodaysTargets() {
        PlayerDailyQuestSettingsEntity settings = settings();

        when(settingsRepository.findForUpdate(PLAYER_ID))
                .thenReturn(Optional.of(settings));

        service.scheduleUpdate(
                PLAYER_ID,
                request(
                        UpdateDailyQuestRequestDto.UpdateMode.TOMORROW,
                        NEW
                )
        );

        assertEquals(
                DEFAULT,
                settings.getActiveTargets()
        );

        assertEquals(
                NEW,
                settings.getPendingTargets()
        );

        assertEquals(
                LocalDate.of(2026, 11, 8),
                settings.getPendingEffectiveDate()
        );

        verifyNoInteractions(playerQuestRepository);
    }

    @Test
    void latestTomorrowRequestWins() {
        PlayerDailyQuestSettingsEntity settings = settings();

        when(settingsRepository.findForUpdate(PLAYER_ID))
                .thenReturn(Optional.of(settings));

        service.scheduleUpdate(
                PLAYER_ID,
                request(
                        UpdateDailyQuestRequestDto.UpdateMode.TOMORROW,
                        NEW
                )
        );

        service.scheduleUpdate(
                PLAYER_ID,
                request(
                        UpdateDailyQuestRequestDto.UpdateMode.TOMORROW,
                        LATER
                )
        );

        assertEquals(
                LATER,
                settings.getPendingTargets()
        );

        assertEquals(
                DEFAULT,
                settings.getActiveTargets()
        );
    }

    @Test
    void pendingTargetsActivateOnDueDate() {
        PlayerDailyQuestSettingsEntity settings = settings();

        settings.setPendingTargets(new HashMap<>(NEW));
        settings.setPendingEffectiveDate(
                LocalDate.of(2026, 11, 7)
        );

        when(settingsRepository.findForUpdate(PLAYER_ID))
                .thenReturn(Optional.of(settings));

        Map<String, Integer> result =
                service.applyPendingIfDue(PLAYER_ID);

        assertEquals(NEW, result);

        assertEquals(
                NEW,
                settings.getActiveTargets()
        );

        assertNull(settings.getPendingTargets());
        assertNull(settings.getPendingEffectiveDate());

        verify(settingsRepository).save(settings);
    }

    @Test
    void pendingTargetsDoNotActivateEarly() {
        PlayerDailyQuestSettingsEntity settings = settings();

        settings.setPendingTargets(new HashMap<>(NEW));
        settings.setPendingEffectiveDate(
                LocalDate.of(2026, 11, 8)
        );

        when(settingsRepository.findForUpdate(PLAYER_ID))
                .thenReturn(Optional.of(settings));

        assertEquals(
                DEFAULT,
                service.applyPendingIfDue(PLAYER_ID)
        );

        assertEquals(
                NEW,
                settings.getPendingTargets()
        );

        verify(settingsRepository, never()).save(any());
    }

    @Test
    void invalidTargetsReturnBadRequest() {
        UpdateDailyQuestRequestDto invalid = request(
                UpdateDailyQuestRequestDto.UpdateMode.NOW,
                Map.of("pushups", 0)
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.updateNow(PLAYER_ID, invalid)
        );

        assertEquals(
                400,
                error.getStatusCode().value()
        );

        verifyNoInteractions(
                settingsRepository,
                playerQuestRepository
        );
    }
}
