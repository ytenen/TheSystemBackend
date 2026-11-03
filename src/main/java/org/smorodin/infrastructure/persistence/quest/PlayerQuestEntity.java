package org.smorodin.infrastructure.persistence.quest;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.smorodin.core.model.PlayerQuest;
import org.smorodin.core.model.enums.QuestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "player_quests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerQuestEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "player_id",
            nullable = false
    )
    private Long playerId;

    @Column(
            name = "quest_id",
            nullable = false
    )
    private Long questId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Integer> progress =
            new HashMap<>();

    /*
     * Новая модель состояния Quest Engine v2.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestStatus status =
            QuestStatus.ACTIVE;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(
            name = "last_reset_date",
            nullable = false
    )
    private LocalDate lastResetDate;

    /*
     * Compatibility helper.
     *
     * QuestService пока вызывает pq.isCompleted().
     */
    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }

    // Domain -> Entity
    public static PlayerQuestEntity fromDomain(
            PlayerQuest playerQuest
    ) {
        PlayerQuestEntity entity =
                new PlayerQuestEntity();

        entity.setId(
                playerQuest.getId()
        );

        entity.setPlayerId(
                playerQuest.getPlayerId()
        );

        entity.setQuestId(
                playerQuest.getQuestId()
        );

        entity.setProgress(
                new HashMap<>(
                        playerQuest.getProgress()
                )
        );

        entity.setStatus(
                playerQuest.getStatus()
        );

        entity.setCompletedAt(
                playerQuest.getCompletedAt()
        );

        entity.setLastResetDate(
                playerQuest.getLastResetDate()
        );

        return entity;
    }

    // Entity -> Domain
    public PlayerQuest toDomain() {
        return new PlayerQuest.Builder()
                .id(this.id)
                .playerId(this.playerId)
                .questId(this.questId)
                .progress(
                        new HashMap<>(
                                this.progress
                        )
                )
                .status(this.status)
                .completedAt(this.completedAt)
                .lastResetDate(
                        this.lastResetDate
                )
                .build();
    }
}