package org.smorodin.infrastructure.persistence;

import org.smorodin.core.model.enums.StatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerStatProgressRepository
        extends JpaRepository<PlayerStatProgressEntity, PlayerStatProgressId> {

    List<PlayerStatProgressEntity> findAllByPlayerId(Long playerId);

    Optional<PlayerStatProgressEntity> findByPlayerIdAndStatType(
            Long playerId, StatType statType
    );
}
