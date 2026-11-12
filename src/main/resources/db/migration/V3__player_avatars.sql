CREATE TABLE player_avatars (
    player_id BIGINT PRIMARY KEY,
    content_type VARCHAR(20) NOT NULL,
    image_data BYTEA NOT NULL,

    CONSTRAINT fk_player_avatar_player
        FOREIGN KEY (player_id)
            REFERENCES players(id) ON DELETE CASCADE,

    CONSTRAINT chk_player_avatar_content_type
        CHECK (content_type IN ('image/png', 'image/jpeg'))
);
