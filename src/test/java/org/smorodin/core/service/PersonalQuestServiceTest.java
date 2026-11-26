package org.smorodin.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smorodin.core.model.enums.QuestStatus;
import org.smorodin.core.model.enums.QuestType;
import org.smorodin.infrastructure.persistence.quest.*;
import org.smorodin.infrastructure.web.dto.quest.CreatePersonalQuestRequestDto;
import org.smorodin.infrastructure.web.dto.quest.PersonalQuestResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalQuestServiceTest {

    private static final Long PLAYER_ID = 1L;
    private static final Long OTHER_PLAYER_ID = 2L;
    private static final Long QUEST_ID = 10L;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private PlayerQuestRepository playerQuestRepository;

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private PersonalQuestService service;

    private QuestEntity quest;
    private PlayerQuestEntity progress;

    @BeforeEach
    void setUp() {
        quest = mock(QuestEntity.class);

        progress = new PlayerQuestEntity();
        progress.setId(100L);
        progress.setPlayerId(PLAYER_ID);
        progress.setQuestId(QUEST_ID);
        progress.setProgress(new HashMap<>());
        progress.setStatus(QuestStatus.ACTIVE);
        progress.setLastResetDate(LocalDate.now());
    }


    private void configureOwnedQuest() {
        when(questRepository.findByIdAndOwnerIdAndType(
                QUEST_ID, PLAYER_ID, QuestType.PERSONAL
        )).thenReturn(Optional.of(quest));

        when(quest.getTargets()).thenReturn(
                Map.of("lessons", 3, "projects", 1)
        );
    }


    private void configureProgressUpdate() {
        configureOwnedQuest();

        when(playerQuestRepository.findForUpdate(
                PLAYER_ID, QUEST_ID
        )).thenReturn(Optional.of(progress));
    }

    private void configureSave() {
        when(playerQuestRepository.save(any(PlayerQuestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreatePersonalQuest() {
        CreatePersonalQuestRequestDto request =
                new CreatePersonalQuestRequestDto();

        request.setTitle("Learn Spring Boot");
        request.setDescription("Complete lessons");
        request.setTargets(Map.of("lessons", 3));

        when(questRepository.save(any(QuestEntity.class)))
                .thenAnswer(invocation -> {
                    QuestEntity entity = invocation.getArgument(0);
                    entity.setId(QUEST_ID);
                    return entity;
                });

        configureSave();

        PersonalQuestResponseDto result =
                service.create(PLAYER_ID, request);

        assertEquals(QUEST_ID, result.getId());
        assertEquals("Learn Spring Boot", result.getTitle());
        assertEquals(QuestStatus.ACTIVE, result.getStatus());
        assertEquals(100, result.getExpReward());
        assertTrue(result.getProgress().isEmpty());

        verify(playerQuestRepository).save(argThat(entity ->
                PLAYER_ID.equals(entity.getPlayerId())
                        && QUEST_ID.equals(entity.getQuestId())
                        && entity.getStatus() == QuestStatus.ACTIVE
        ));
    }

    @Test
    void shouldRejectEmptyTargets() {
        CreatePersonalQuestRequestDto request =
                new CreatePersonalQuestRequestDto();

        request.setTitle("Invalid quest");
        request.setTargets(Map.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(PLAYER_ID, request)
        );

        verifyNoInteractions(
                questRepository,
                playerQuestRepository,
                rewardService
        );
    }

    @Test
    void shouldReturnOnlyOwnedQuests() {
        when(questRepository.findByOwnerIdAndType(
                PLAYER_ID, QuestType.PERSONAL
        )).thenReturn(List.of(quest));

        when(quest.getId()).thenReturn(QUEST_ID);
        when(quest.getTargets()).thenReturn(Map.of("lessons", 3));

        when(playerQuestRepository.findByPlayerIdAndQuestId(
                PLAYER_ID, QUEST_ID
        )).thenReturn(Optional.of(progress));

        List<PersonalQuestResponseDto> result =
                service.getAll(PLAYER_ID);

        assertEquals(1, result.size());
        assertEquals(QUEST_ID, result.get(0).getId());

        verify(questRepository).findByOwnerIdAndType(
                PLAYER_ID, QuestType.PERSONAL
        );
    }

    @Test
    void shouldReturnNotFoundForForeignQuest() {
        when(questRepository.findByIdAndOwnerIdAndType(
                QUEST_ID, OTHER_PLAYER_ID, QuestType.PERSONAL
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getById(OTHER_PLAYER_ID, QUEST_ID)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verifyNoInteractions(playerQuestRepository, rewardService);
    }

    @Test
    void shouldIncreaseProgress() {
        configureProgressUpdate();
        when(quest.getId()).thenReturn(QUEST_ID);
        configureSave();

        PersonalQuestResponseDto result =
                service.updateProgress(
                        PLAYER_ID, QUEST_ID, "lessons", 2
                );

        assertEquals(2, result.getProgress().get("lessons"));
        assertEquals(QuestStatus.ACTIVE, result.getStatus());

        verifyNoInteractions(rewardService);
    }

    @Test
    void shouldCapProgressAtTarget() {
        configureProgressUpdate();
        when(quest.getId()).thenReturn(QUEST_ID);
        configureSave();

        PersonalQuestResponseDto result =
                service.updateProgress(
                        PLAYER_ID, QUEST_ID, "lessons", 100
                );

        assertEquals(3, result.getProgress().get("lessons"));
        assertEquals(QuestStatus.ACTIVE, result.getStatus());

        verifyNoInteractions(rewardService);
    }

    @Test
    void shouldCompleteQuestAndGiveExp() {
        configureProgressUpdate();
        when(quest.getId()).thenReturn(QUEST_ID);
        configureSave();

        progress.setProgress(new HashMap<>(Map.of(
                "lessons", 3
        )));

        when(quest.getExpReward()).thenReturn(100);

        PersonalQuestResponseDto result =
                service.updateProgress(
                        PLAYER_ID, QUEST_ID, "projects", 1
                );

        assertEquals(QuestStatus.COMPLETED, result.getStatus());
        assertEquals(1, result.getProgress().get("projects"));

        verify(rewardService, times(1))
                .addExp(PLAYER_ID, 100);
    }

    @Test
    void shouldRejectAlreadyCompletedQuest() {
        configureProgressUpdate();

        progress.setStatus(QuestStatus.COMPLETED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateProgress(
                        PLAYER_ID, QUEST_ID, "lessons", 1
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(playerQuestRepository, never())
                .save(any(PlayerQuestEntity.class));

        verifyNoInteractions(rewardService);
    }

    @Test
    void shouldRejectLockedQuest() {
        configureProgressUpdate();

        progress.setStatus(QuestStatus.LOCKED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateProgress(
                        PLAYER_ID, QUEST_ID, "lessons", 1
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(rewardService);
    }

    @Test
    void shouldRejectUnknownAction() {
        configureOwnedQuest();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateProgress(
                        PLAYER_ID, QUEST_ID, "unknown", 1
                )
        );

        verify(playerQuestRepository, never())
                .findForUpdate(anyLong(), anyLong());

        verifyNoInteractions(rewardService);
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateProgress(
                        PLAYER_ID, QUEST_ID, "lessons", 0
                )
        );

        verifyNoInteractions(
                questRepository,
                playerQuestRepository,
                rewardService
        );
    }
}
