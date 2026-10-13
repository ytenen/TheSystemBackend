package org.smorodin.infrastructure.persistence.quest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerQuestRepository extends JpaRepository<PlayerQuestEntity, Long> {
    Optional<PlayerQuestEntity> findByPlayerIdAndQuestId(Long playerId, Long questId);
    List<PlayerQuestEntity> findByPlayerId(Long playerId);
}