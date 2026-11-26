package org.smorodin.infrastructure.persistence;

import org.smorodin.core.model.enums.StatType;

import java.io.Serializable;
import java.util.Objects;

public class PlayerStatProgressId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long playerId;
    private StatType statType;

    public PlayerStatProgressId() {}

    public PlayerStatProgressId(Long playerId, StatType statType) {
        this.playerId = playerId;
        this.statType = statType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlayerStatProgressId other)) return false;
        return Objects.equals(playerId, other.playerId) && statType == other.statType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, statType);
    }
}
