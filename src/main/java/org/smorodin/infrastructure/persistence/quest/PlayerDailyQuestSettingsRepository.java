package org.smorodin.infrastructure.persistence.quest;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerDailyQuestSettingsRepository
        extends JpaRepository<PlayerDailyQuestSettingsEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT settings
        FROM PlayerDailyQuestSettingsEntity settings
        WHERE settings.playerId = :playerId
        """)
    Optional<PlayerDailyQuestSettingsEntity> findForUpdate(
            @Param("playerId") Long playerId
    );
}
