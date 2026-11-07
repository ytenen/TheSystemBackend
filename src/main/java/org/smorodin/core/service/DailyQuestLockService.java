package org.smorodin.core.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyQuestLockService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPlayer(Long playerId) {
        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("Invalid player ID");
        }

        var rows = entityManager.createNativeQuery("""
                SELECT id
                FROM players
                WHERE id = :playerId
                FOR UPDATE
                """)
                .setParameter("playerId", playerId)
                .getResultList();

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Player not found");
        }
    }
}
