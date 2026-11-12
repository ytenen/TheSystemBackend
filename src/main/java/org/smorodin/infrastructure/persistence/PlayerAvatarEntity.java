package org.smorodin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_avatars")
@Getter
@Setter
@NoArgsConstructor
public class PlayerAvatarEntity {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    // Deliberately no @Lob: PostgreSQL BYTEA, not the OID large-object type.
    @Column(name = "image_data", nullable = false, columnDefinition = "bytea")
    private byte[] imageData;

    public PlayerAvatarEntity(Long playerId, String contentType, byte[] imageData) {
        this.playerId = playerId;
        this.contentType = contentType;
        this.imageData = imageData;
    }
}
