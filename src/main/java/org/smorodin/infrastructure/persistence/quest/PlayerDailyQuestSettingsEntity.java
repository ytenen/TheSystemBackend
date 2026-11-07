package org.smorodin.infrastructure.persistence.quest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "player_daily_quest_settings")
@Getter
@Setter
@NoArgsConstructor
public class PlayerDailyQuestSettingsEntity {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "active_targets", columnDefinition = "json", nullable = false)
    private Map<String, Integer> activeTargets = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pending_targets", columnDefinition = "json")
    private Map<String, Integer> pendingTargets;

    @Column(name = "pending_effective_date")
    private LocalDate pendingEffectiveDate;
}
