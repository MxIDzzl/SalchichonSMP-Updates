package com.escuadronsalchichon.api.repository;

import com.escuadronsalchichon.api.dto.ClanDetailResponse;
import com.escuadronsalchichon.api.dto.PlayerSummaryResponse;
import com.escuadronsalchichon.api.dto.UpcomingUpdateResponse;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PublicDataRepositoryMySqlIntegrationTest {
    private static JdbcTemplate jdbc;
    private static PublicDataRepository repository;

    @BeforeAll
    static void initializeMySqlSchema() throws Exception {
        String url = System.getenv("SALCHICHON_MYSQL_TEST_URL");
        Assumptions.assumeTrue(url != null, "MySQL integration service is not configured");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
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
                        CONSTRAINT fk_api_test_player_stats_player FOREIGN KEY (player_uuid)
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
        repository = new PublicDataRepository(jdbc);
    }

    @Test
    void readsPlayerDirectoryAndProfileFromMySql() {
        UUID playerId = UUID.randomUUID();
        insertPlayer(playerId, "TestPlayer", 7, 2400, 1234);
        jdbc.update("INSERT INTO player_skins(player_uuid, texture_hash) VALUES (?, ?)",
                playerId.toString(), "ABCDEF0123456789ABCDEF0123456789");

        var totals = repository.overviewTotals();
        assertTrue(totals.players() >= 1);
        assertTrue(totals.officialDeaths() >= 7);
        assertTrue(totals.playtimeTicks() >= 2400);
        assertNotNull(totals.playerValue());

        PlayerSummaryResponse profile = repository.findPlayer(playerId, LocalDate.now()).orElseThrow();
        assertEquals("TestPlayer", profile.username());
        assertEquals("abcdef0123456789abcdef0123456789", profile.skinTextureHash());
        assertEquals(7, profile.officialDeaths());
        assertEquals(2400, profile.playtimeTicks());
        assertEquals("1234", profile.playerValue());

        assertTrue(repository.findPlayers(50, 0, "value", LocalDate.now())
                .stream().anyMatch(player -> player.uuid().equals(playerId)));
    }

    @Test
    void readsClanDirectoryAndMemberProfileFromMySql() {
        UUID playerId = UUID.randomUUID();
        UUID clanId = UUID.randomUUID();
        insertPlayer(playerId, "ClanPlayer", 1, 200, 50);
        jdbc.update("""
                INSERT INTO public_clans(
                    clan_uuid, name, tag, description, leader_uuid, created_at,
                    member_count, max_members, points, level, kills, deaths, kdr, is_active
                ) VALUES (?, 'Kessoku', 'KS', 'Test clan', ?, UTC_TIMESTAMP(6),
                          1, 20, 500, 3, 12, 4, 3.0, TRUE)
                """, clanId.toString(), playerId.toString());
        jdbc.update("""
                INSERT INTO player_clan_memberships(
                    player_uuid, clan_uuid, rank_id, joined_at, kills, deaths, kdr, is_active
                ) VALUES (?, ?, 'LEADER', UTC_TIMESTAMP(6), 12, 4, 3.0, TRUE)
                """, playerId.toString(), clanId.toString());

        ClanDetailResponse detail = repository.findClan(clanId).orElseThrow();
        assertEquals("Kessoku", detail.clan().name());
        assertEquals("KS", detail.clan().tag());
        assertEquals(1, detail.members().size());
        assertEquals(playerId, detail.members().get(0).playerUuid());
        assertEquals("ClanPlayer", detail.members().get(0).username());
        assertEquals("LEADER", detail.members().get(0).rankId());
        assertTrue(repository.findClans(20, 0, "points").stream()
                .anyMatch(clan -> clan.uuid().equals(clanId)));
    }

    @Test
    void readsPublishedAnnouncementsAndUpcomingTeasersFromMySql() {
        jdbc.update("""
                INSERT INTO public_announcements(title, body, category, is_published, published_at)
                VALUES ('Integration update', 'Published body', 'UPDATE', TRUE, UTC_TIMESTAMP(6))
                """);
        jdbc.update("""
                INSERT INTO public_announcements(
                    title, body, category, is_published, published_at, scheduled_at, is_teaser
                ) VALUES ('Upcoming update', 'Preview body', 'UPDATE', TRUE,
                          UTC_TIMESTAMP(6), DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 1 DAY), TRUE)
                """);

        assertTrue(repository.findAnnouncements(50).stream()
                .anyMatch(announcement -> announcement.title().equals("Integration update")));
        UpcomingUpdateResponse upcoming = repository.findUpcomingUpdate().orElseThrow();
        assertEquals("Upcoming update", upcoming.title());
        assertTrue(upcoming.teaser());
    }

    private static void insertPlayer(UUID uuid, String username, long deaths, long playtime, long value) {
        jdbc.update("INSERT INTO player_data(uuid, username, totem_uses) VALUES (?, ?, 2)",
                uuid.toString(), username);
        jdbc.update("""
                INSERT INTO player_stats(player_uuid, official_deaths, playtime_ticks, player_value)
                VALUES (?, ?, ?, ?)
                """, uuid.toString(), deaths, playtime, value);
    }
}
