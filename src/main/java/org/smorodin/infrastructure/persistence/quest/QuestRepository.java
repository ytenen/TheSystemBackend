
package org.smorodin.infrastructure.persistence.quest;

import org.smorodin.core.model.enums.QuestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestRepository
        extends JpaRepository<QuestEntity, Long> {

    List<QuestEntity> findByType(QuestType type);

    List<QuestEntity> findByOwnerIdAndType(
            Long ownerId,
            QuestType type
    );

    Optional<QuestEntity> findByIdAndOwnerIdAndType(
            Long id,
            Long ownerId,
            QuestType type
    );
}
