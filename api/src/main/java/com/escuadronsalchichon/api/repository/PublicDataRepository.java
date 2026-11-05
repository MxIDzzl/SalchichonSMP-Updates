package com.escuadronsalchichon.api.repository;

import com.escuadronsalchichon.api.dto.AnnouncementResponse;
import com.escuadronsalchichon.api.dto.PlayerSummaryResponse;
import com.escuadronsalchichon.api.dto.UpcomingUpdateResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PublicDataRepository {
    private final JdbcTemplate jdbc;

    public PublicDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OverviewTotals overviewTotals() {
        return jdbc.queryForObject("""
                SELECT COUNT(pd.uuid),
                       COALESCE(SUM(ps.official_deaths), 0),
                       COALESCE(SUM(ps.playtime_ticks), 0),
                       COALESCE(SUM(ps.player_value), 0)
                FROM player_data pd
                LEFT JOIN player_stats ps ON ps.player_uuid = pd.uuid
                """, (rs, row) -> new OverviewTotals(
                rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getBigDecimal(4).toPlainString()
        ));
    }

    public List<PlayerSummaryResponse> findPlayers(int limit, int offset, String sort, LocalDate today) {
        String orderBy = orderBy(sort);
        String sql = """
                SELECT pd.uuid, pd.username, pskin.texture_hash AS skin_texture_hash, pd.last_death_date, pd.totem_uses,
                       COALESCE(ps.official_deaths, 0) AS official_deaths,
                       COALESCE(ps.playtime_ticks, 0) AS playtime_ticks,
                       COALESCE(ps.player_value, 0) AS player_value
                FROM player_data pd
                LEFT JOIN player_stats ps ON ps.player_uuid = pd.uuid
                LEFT JOIN player_skins pskin ON pskin.player_uuid = pd.uuid
                ORDER BY %s
                LIMIT ? OFFSET ?
                """.formatted(orderBy);
        return jdbc.query(sql, (rs, row) -> mapPlayer(rs, today), limit, offset);
    }

    public Optional<PlayerSummaryResponse> findPlayer(UUID uuid, LocalDate today) {
        List<PlayerSummaryResponse> rows = jdbc.query("""
                SELECT pd.uuid, pd.username, pskin.texture_hash AS skin_texture_hash, pd.last_death_date, pd.totem_uses,
                       COALESCE(ps.official_deaths, 0) AS official_deaths,
                       COALESCE(ps.playtime_ticks, 0) AS playtime_ticks,
                       COALESCE(ps.player_value, 0) AS player_value
                FROM player_data pd
                LEFT JOIN player_stats ps ON ps.player_uuid = pd.uuid
                LEFT JOIN player_skins pskin ON pskin.player_uuid = pd.uuid
                WHERE pd.uuid = ?
                """, (rs, row) -> mapPlayer(rs, today), uuid);
        return rows.stream().findFirst();
    }

    private PlayerSummaryResponse mapPlayer(ResultSet rs, LocalDate today) throws SQLException {
        LocalDate lastDeathDate = rs.getObject("last_death_date", LocalDate.class);
        boolean hasLife = hasLifeOn(lastDeathDate, today);
        return new PlayerSummaryResponse(
                rs.getObject("uuid", UUID.class),
                rs.getString("username"),
                rs.getString("skin_texture_hash"),
                lastDeathDate,
                hasLife,
                rs.getLong("totem_uses"),
                rs.getLong("official_deaths"),
                rs.getLong("playtime_ticks"),
                rs.getBigDecimal("player_value").toPlainString()
        );
    }

    public List<AnnouncementResponse> findAnnouncements(int limit) {
        return jdbc.query("""
                SELECT id, title, body, category, published_at, expires_at
                FROM public_announcements
                WHERE is_published = TRUE
                  AND published_at <= now()
                  AND (expires_at IS NULL OR expires_at > now())
                ORDER BY published_at DESC, id DESC
                LIMIT ?
                """, (rs, row) -> new AnnouncementResponse(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("category"),
                rs.getObject("published_at", java.time.OffsetDateTime.class),
                rs.getObject("expires_at", java.time.OffsetDateTime.class)
        ), limit);
    }

    public Optional<UpcomingUpdateResponse> findUpcomingUpdate() {
        List<UpcomingUpdateResponse> updates = jdbc.query("""
                SELECT id, title, body, category, scheduled_at, is_teaser
                FROM public_announcements
                WHERE is_published = TRUE
                  AND scheduled_at IS NOT NULL
                  AND scheduled_at > now()
                  AND (expires_at IS NULL OR expires_at > now())
                ORDER BY scheduled_at ASC, id ASC
                LIMIT 1
                """, (rs, row) -> new UpcomingUpdateResponse(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("category"),
                rs.getObject("scheduled_at", java.time.OffsetDateTime.class),
                rs.getBoolean("is_teaser")
        ));
        return updates.stream().findFirst();
    }

    public boolean databaseReady() {
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        return result != null && result == 1;
    }

    static boolean hasLifeOn(LocalDate lastDeathDate, LocalDate today) {
        return lastDeathDate == null || lastDeathDate.isBefore(today);
    }

    static String orderBy(String sort) {
        return switch (sort.toLowerCase(java.util.Locale.ROOT)) {
            case "deaths" -> "official_deaths DESC, pd.uuid ASC";
            case "playtime" -> "playtime_ticks DESC, pd.uuid ASC";
            case "totems" -> "pd.totem_uses DESC, pd.uuid ASC";
            default -> "player_value DESC, pd.uuid ASC";
        };
    }

    public record OverviewTotals(long players, long officialDeaths, long playtimeTicks, String playerValue) {}
}
