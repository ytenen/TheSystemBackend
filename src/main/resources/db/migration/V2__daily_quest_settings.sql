
CREATE TABLE player_daily_quest_settings (
                                             player_id BIGINT PRIMARY KEY,
                                             active_targets JSON NOT NULL,
                                             pending_targets JSON,
                                             pending_effective_date DATE,

                                             CONSTRAINT fk_daily_settings_player
                                                 FOREIGN KEY (player_id)
                                                     REFERENCES players(id),

                                             CONSTRAINT chk_daily_pending
                                                 CHECK (
                                                     (pending_targets IS NULL AND pending_effective_date IS NULL)
                                                         OR
                                                     (pending_targets IS NOT NULL AND pending_effective_date IS NOT NULL)
                                                     )
);
