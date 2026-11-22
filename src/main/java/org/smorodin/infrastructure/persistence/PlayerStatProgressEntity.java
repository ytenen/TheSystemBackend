package org.smorodin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.smorodin.core.model.enums.StatType;

@Entity
@Table(name = "player_stat_progress")
@IdClass(PlayerStatProgressId.class)
@Getter
@Setter
@NoArgsConstructor
public class PlayerStatProgressEntity {

    @Id
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 32)
    private StatType statType;

    @Column(name = "current_xp", nullable = false)
    private int currentXp;

    public PlayerStatProgressEntity(Long playerId, StatType statType, int currentXp) {
        this.playerId = playerId;
        this.statType = statType;
        this.currentXp = currentXp;
    }
}
