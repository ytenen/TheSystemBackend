package org.smorodin.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Replays V4 against a disposable pre-migration schema to verify legacy data.
 * Does not modify the application's public schema.
 */
@SpringBootTest
class StatProgressMigrationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldBackfillExistingPlayersAndReplaceStoredQuestRewards() throws Exception {
        String schema = "stat_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA " + schema);
            try {
                connection.setSchema(schema);
                stmt.execute("""
                        CREATE TABLE players (
                            id BIGINT PRIMARY KEY,
                            exp BIGINT NOT NULL,
                            level INTEGER NOT NULL,
                            strength INTEGER NOT NULL,
                            agility INTEGER NOT NULL,
                            vitality INTEGER NOT NULL,
                            intelligence INTEGER NOT NULL,
                            perception INTEGER NOT NULL,
                            will INTEGER NOT NULL
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE quests (
                            id BIGINT PRIMARY KEY,
                            type VARCHAR(255) NOT NULL,
                            owner_id BIGINT,
                            exp_reward INTEGER NOT NULL,
                            stat_reward VARCHAR(255),
                            stat_reward_amount INTEGER
                        )
                        """);
                stmt.execute("""
                        INSERT INTO players VALUES (42, 650, 3, 15, 6, 11, 8, 7, 10)
                        """);
                stmt.execute("""
                        INSERT INTO quests VALUES
                        (1, 'DAILY', NULL, 100, 'STRENGTH', 1),
                        (2, 'HIDDEN', NULL, 200, 'AGILITY', 1),
                        (3, 'PERSONAL', 42, 100, NULL, NULL)
                        """);

                ScriptUtils.executeSqlScript(connection,
                        new ClassPathResource("db/migration/V4__stat_experience_system.sql"));

                try (ResultSet rs = stmt.executeQuery("""
                        SELECT exp, level, strength, agility, vitality,
                               intelligence, perception, will
                        FROM players WHERE id = 42
                        """)) {
                    assertTrue(rs.next());
                    assertEquals(650, rs.getLong("exp"));
                    assertEquals(3, rs.getInt("level"));
                    assertEquals(15, rs.getInt("strength"));
                    assertEquals(6, rs.getInt("agility"));
                    assertEquals(11, rs.getInt("vitality"));
                    assertEquals(8, rs.getInt("intelligence"));
                    assertEquals(7, rs.getInt("perception"));
                    assertEquals(10, rs.getInt("will"));
                }

                try (ResultSet rs = stmt.executeQuery("""
                        SELECT stat_type, current_xp FROM player_stat_progress
                        WHERE player_id = 42 ORDER BY stat_type
                        """)) {
                    int count = 0;
                    while (rs.next()) {
                        assertEquals(0, rs.getInt("current_xp"));
                        count++;
                    }
                    assertEquals(6, count);
                }

                try (ResultSet rs = stmt.executeQuery("""
                        SELECT type, exp_reward, stat_xp_rewards::text AS rewards
                        FROM quests ORDER BY id
                        """)) {
                    assertTrue(rs.next());
                    assertEquals("DAILY", rs.getString("type"));
                    assertEquals(100, rs.getInt("exp_reward"));
                    assertTrue(rs.getString("rewards").contains("STRENGTH"));
                    assertTrue(rs.getString("rewards").contains("25"));

                    assertTrue(rs.next());
                    assertEquals("HIDDEN", rs.getString("type"));
                    assertEquals(200, rs.getInt("exp_reward"));
                    assertTrue(rs.getString("rewards").contains("STRENGTH"));
                    assertTrue(rs.getString("rewards").contains("WILL"));
                    assertFalse(rs.getString("rewards").contains("AGILITY"));

                    assertTrue(rs.next());
                    assertEquals("PERSONAL", rs.getString("type"));
                    assertEquals("{}", rs.getString("rewards"));
                }

                try (ResultSet rs = connection.getMetaData().getColumns(
                        null, schema, "quests", "stat_reward")) {
                    assertFalse(rs.next());
                }
            } finally {
                connection.setSchema("public");
                stmt.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
