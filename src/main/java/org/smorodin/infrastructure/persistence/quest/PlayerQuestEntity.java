package org.smorodin.infrastructure.persistence.quest;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.smorodin.core.model.PlayerQuest;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "quest_id", nullable = false)
    private Long questId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Integer> progress = new HashMap<>();

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_reset_date", nullable = false)
    private LocalDate lastResetDate;

    // === Конвертация Domain → Entity ===
    public static PlayerQuestEntity fromDomain(PlayerQuest playerQuest) {
        PlayerQuestEntity entity = new PlayerQuestEntity();
        entity.setId(playerQuest.getId());
        entity.setPlayerId(playerQuest.getPlayerId());
        entity.setQuestId(playerQuest.getQuestId());
        entity.setProgress(new HashMap<>(playerQuest.getProgress()));
        entity.setCompleted(playerQuest.isCompleted());
        entity.setCompletedAt(playerQuest.getCompletedAt());
        entity.setLastResetDate(playerQuest.getLastResetDate());
        return entity;
    }

    // === Конвертация Entity → Domain ===
    public PlayerQuest toDomain() {
        return new PlayerQuest.Builder()
                .id(this.id)
                .playerId(this.playerId)
                .questId(this.questId)
                .progress(new HashMap<>(this.progress))
                .completed(this.completed)
                .completedAt(this.completedAt)
                .lastResetDate(this.lastResetDate)
                .build();
    }
}