package org.smorodin.infrastructure.persistence.quest;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerQuestRepository
        extends JpaRepository<PlayerQuestEntity, Long> {

    Optional<PlayerQuestEntity> findByPlayerIdAndQuestId(
            Long playerId,
            Long questId
    );

    List<PlayerQuestEntity> findByPlayerId(Long playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT pq
            FROM PlayerQuestEntity pq
            WHERE pq.playerId = :playerId
              AND pq.questId = :questId
            """)
    Optional<PlayerQuestEntity> findForUpdate(
            @Param("playerId") Long playerId,
            @Param("questId") Long questId
    );
}
