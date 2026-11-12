package org.smorodin.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerAvatarRepository extends JpaRepository<PlayerAvatarEntity, Long> {
}
