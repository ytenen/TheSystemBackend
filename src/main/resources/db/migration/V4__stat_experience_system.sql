-- Existing characteristic values and player EXP are deliberately not modified.
CREATE TABLE player_stat_progress (
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    stat_type VARCHAR(32) NOT NULL,
    current_xp INTEGER NOT NULL DEFAULT 0 CHECK (current_xp >= 0),
    PRIMARY KEY (player_id, stat_type),
    CONSTRAINT chk_stat_progress_type CHECK (stat_type IN (
        'STRENGTH', 'AGILITY', 'VITALITY',
        'INTELLIGENCE', 'PERCEPTION', 'WILL'
    ))
);

-- Backfill all six independent XP bars for existing players.
INSERT INTO player_stat_progress (player_id, stat_type, current_xp)
SELECT p.id, s.stat_type, 0
FROM players p
CROSS JOIN (VALUES
    ('STRENGTH'), ('AGILITY'), ('VITALITY'),
    ('INTELLIGENCE'), ('PERCEPTION'), ('WILL')
) AS s(stat_type);

-- A single reward configuration source supports multiple characteristics.
ALTER TABLE quests
    ADD COLUMN stat_xp_rewards JSON NOT NULL DEFAULT '{}'::json;

-- Update already persisted system quest templates, not only new installs.
UPDATE quests
SET exp_reward = 100,
    stat_xp_rewards = '{"STRENGTH":25}'::json
WHERE type = 'DAILY' AND owner_id IS NULL;

UPDATE quests
SET exp_reward = 200,
    stat_xp_rewards = '{"STRENGTH":25,"WILL":25}'::json
WHERE type = 'HIDDEN' AND owner_id IS NULL;

-- Remove obsolete direct-stat rewards so there is no competing authority.
ALTER TABLE quests
    DROP COLUMN stat_reward,
    DROP COLUMN stat_reward_amount;
