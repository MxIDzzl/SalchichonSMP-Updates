package com.escuadronsalchichon.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllersMySqlIntegrationTest {
    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void initializeMySqlSchema() throws Exception {
        String url = System.getenv("SALCHICHON_MYSQL_TEST_URL");
        Assumptions.assumeTrue(url != null, "MySQL integration service is not configured");
        dataSource = new DriverManagerDataSource(
                url,
                System.getenv("SALCHICHON_MYSQL_TEST_USER"),
                System.getenv("SALCHICHON_MYSQL_TEST_PASSWORD"));
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid CHAR(36) NOT NULL PRIMARY KEY,
                        username VARCHAR(16) NOT NULL,
                        last_death_date DATE NULL,
                        totem_uses BIGINT NOT NULL DEFAULT 0
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        player_uuid CHAR(36) NOT NULL PRIMARY KEY,
                        official_deaths BIGINT NOT NULL DEFAULT 0,
                        playtime_ticks BIGINT NOT NULL DEFAULT 0,
                        player_value BIGINT NOT NULL DEFAULT 0,
                        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        CONSTRAINT fk_api_admin_test_player_stats_player FOREIGN KEY (player_uuid)
                            REFERENCES player_data(uuid) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS stat_events (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        player_uuid CHAR(36) NOT NULL,
                        stat_key VARCHAR(64) NOT NULL,
                        previous_value BIGINT NOT NULL,
                        new_value BIGINT NOT NULL,
                        actor VARCHAR(64) NOT NULL,
                        reason TEXT NOT NULL,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        CONSTRAINT fk_api_admin_test_stat_events_player FOREIGN KEY (player_uuid)
                            REFERENCES player_data(uuid) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .locations("classpath:db/migration/mysql")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void adminStatCorrectionUsesGeneratedKeyAndPreservesPlayerValue() {
        UUID player = UUID.randomUUID();
        jdbc.update("INSERT INTO player_data(uuid, username) VALUES (?, 'AdminTest')", player.toString());
        jdbc.update("""
                INSERT INTO player_stats(player_uuid, official_deaths, playtime_ticks, player_value)
                VALUES (?, 2, 1200, 987)
                """, player.toString());

        var controller = new AdminPlayerStatsController(
                jdbc, new DataSourceTransactionManager(dataSource), "integration-admin");
        var response = controller.correctStat(player, "deaths",
                new AdminPlayerStatsController.StatCorrection(9, "Verified correction"));

        assertEquals(player, response.get("playerUuid"));
        assertEquals(9L, response.get("value"));
        assertTrue(((Number) response.get("eventId")).longValue() > 0);
        assertEquals(9L, jdbc.queryForObject(
                "SELECT official_deaths FROM player_stats WHERE player_uuid=?", Long.class, player.toString()));
        assertEquals(987L, jdbc.queryForObject(
                "SELECT player_value FROM player_stats WHERE player_uuid=?", Long.class, player.toString()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM stat_events WHERE player_uuid=? AND new_value=9",
                Integer.class, player.toString()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM public_admin_audit WHERE actor='integration-admin' AND action='PLAYER_STAT_CORRECTED'",
                Integer.class));
    }

    @Test
    void adminAnnouncementCreateEditAndPublicationUseGeneratedKeysAndAudit() {
        var controller = new AdminAnnouncementController(
                jdbc, new DataSourceTransactionManager(dataSource), "integration-admin", new ObjectMapper());
        OffsetDateTime scheduled = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);
        var created = controller.create(new AdminAnnouncementController.CreateAnnouncement(
                "Admin integration announcement", "Body", "UPDATE", true,
                OffsetDateTime.now(ZoneOffset.UTC), null, scheduled, true, "Test creation"));
        long id = ((Number) created.get("id")).longValue();
        assertTrue(id > 0);

        controller.edit(id, new AdminAnnouncementController.EditAnnouncement(
                "Edited integration announcement", "Edited body", "NOTICE",
                scheduled.plusDays(1), null, false, "Test edit"));
        controller.setPublication(id, new AdminAnnouncementController.PublicationChange(
                false, "Test unpublish"));

        var rows = controller.list(100, 0);
        assertTrue(rows.stream().anyMatch(row -> ((Number) row.get("id")).longValue() == id
                && row.get("title").equals("Edited integration announcement")
                && Boolean.FALSE.equals(row.get("is_published"))));
        var audit = new AdminAuditController(jdbc).recent(100, 0);
        assertTrue(audit.stream().anyMatch(row -> row.get("entity_id") != null
                && ((Number) row.get("entity_id")).longValue() == id
                && row.get("actor").equals("integration-admin")));
    }
}
