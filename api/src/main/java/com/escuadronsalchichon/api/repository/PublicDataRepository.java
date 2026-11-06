package com.escuadronsalchichon.api.repository;

import com.escuadronsalchichon.api.dto.AnnouncementResponse;
import com.escuadronsalchichon.api.dto.ClanDetailResponse;
import com.escuadronsalchichon.api.dto.ClanMemberResponse;
import com.escuadronsalchichon.api.dto.ClanSummaryResponse;
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
                       COALESCE(ps.player_value, 0) AS player_value,
                       pc.clan_uuid, pc.name AS clan_name, pc.tag AS clan_tag,
                       pc.level AS clan_level, pcm.rank_id AS clan_rank
                FROM player_data pd
                LEFT JOIN player_stats ps ON ps.player_uuid = pd.uuid
                LEFT JOIN player_skins pskin ON pskin.player_uuid = pd.uuid
                LEFT JOIN player_clan_memberships pcm
                    ON pcm.player_uuid = pd.uuid AND pcm.is_active = TRUE
                LEFT JOIN public_clans pc
                    ON pc.clan_uuid = pcm.clan_uuid AND pc.is_active = TRUE
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
                       COALESCE(ps.player_value, 0) AS player_value,
                       pc.clan_uuid, pc.name AS clan_name, pc.tag AS clan_tag,
                       pc.level AS clan_level, pcm.rank_id AS clan_rank
                FROM player_data pd
                LEFT JOIN player_stats ps ON ps.player_uuid = pd.uuid
                LEFT JOIN player_skins pskin ON pskin.player_uuid = pd.uuid
                LEFT JOIN player_clan_memberships pcm
                    ON pcm.player_uuid = pd.uuid AND pcm.is_active = TRUE
                LEFT JOIN public_clans pc
                    ON pc.clan_uuid = pcm.clan_uuid AND pc.is_active = TRUE
                WHERE pd.uuid = ?
                """, (rs, row) -> mapPlayer(rs, today), uuid);
        return rows.stream().findFirst();
    }

    public List<ClanSummaryResponse> findClans(int limit, int offset, String sort) {
        String orderBy = clanOrderBy(sort);
        String sql = """
                SELECT clan_uuid, name, tag, description, leader_uuid, created_at, member_count,
                       max_members, points, level, kills, deaths, kdr
                FROM public_clans
                WHERE is_active = TRUE
                ORDER BY %s
                LIMIT ? OFFSET ?
                """.formatted(orderBy);
        return jdbc.query(sql, this::mapClanSummary, limit, offset);
    }

    public Optional<ClanDetailResponse> findClan(UUID uuid) {
        List<ClanSummaryResponse> rows = jdbc.query("""
                SELECT clan_uuid, name, tag, description, leader_uuid, created_at, member_count,
                       max_members, points, level, kills, deaths, kdr
                FROM public_clans
                WHERE clan_uuid = ? AND is_active = TRUE
                """, this::mapClanSummary, uuid);
        if (rows.isEmpty()) return Optional.empty();

        List<ClanMemberResponse> members = jdbc.query("""
                SELECT pcm.player_uuid, COALESCE(pd.username, CAST(pcm.player_uuid AS VARCHAR)) AS username,
                       pcm.rank_id, pcm.joined_at, pcm.kills, pcm.deaths, pcm.kdr
                FROM player_clan_memberships pcm
                LEFT JOIN player_data pd ON pd.uuid = pcm.player_uuid
                WHERE pcm.clan_uuid = ? AND pcm.is_active = TRUE
                ORDER BY pcm.rank_id DESC, username ASC
                """, (rs, row) -> new ClanMemberResponse(
                rs.getObject("player_uuid", UUID.class),
                rs.getString("username"),
                rs.getString("rank_id"),
                rs.getObject("joined_at", java.time.OffsetDateTime.class).toInstant(),
                rs.getLong("kills"),
                rs.getLong("deaths"),
                rs.getDouble("kdr")
        ), uuid);
        return Optional.of(new ClanDetailResponse(rows.get(0), members));
    }

    private ClanSummaryResponse mapClanSummary(ResultSet rs, int row) throws SQLException {
        return new ClanSummaryResponse(
                rs.getObject("clan_uuid", UUID.class),
                rs.getString("name"),
                rs.getString("tag"),
                rs.getString("description"),
                rs.getObject("leader_uuid", UUID.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
                rs.getInt("member_count"),
                rs.getInt("max_members"),
                rs.getLong("points"),
                rs.getInt("level"),
                rs.getLong("kills"),
                rs.getLong("deaths"),
                rs.getDouble("kdr")
        );
    }

    private PlayerSummaryResponse mapPlayer(ResultSet rs, LocalDate today) throws SQLException {
        LocalDate lastDeathDate = rs.getObject("last_death_date", LocalDate.class);
        boolean hasLife = hasLifeOn(lastDeathDate, today);
        return new PlayerSummaryResponse(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                normalizeSkinTextureHash(rs.getString("skin_texture_hash")),
                lastDeathDate,
                hasLife,
                rs.getLong("totem_uses"),
                rs.getLong("official_deaths"),
                rs.getLong("playtime_ticks"),
                rs.getBigDecimal("player_value").toPlainString(),
                rs.getObject("clan_uuid", UUID.class),
                rs.getString("clan_name"),
                rs.getString("clan_tag"),
                rs.getObject("clan_level", Integer.class),
                rs.getString("clan_rank")
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

    static String normalizeSkinTextureHash(String textureHash) {
        if (textureHash == null || !textureHash.matches("(?i)[0-9a-f]{32,64}")) return null;
        return textureHash.toLowerCase(java.util.Locale.ROOT);
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

    static String clanOrderBy(String sort) {
        return switch (sort == null ? "points" : sort.toLowerCase(java.util.Locale.ROOT)) {
            case "kills" -> "kills DESC, name ASC";
            case "members" -> "member_count DESC, name ASC";
            case "level" -> "level DESC, name ASC";
            case "name" -> "name ASC";
            default -> "points DESC, name ASC";
        };
    }

    public record OverviewTotals(long players, long officialDeaths, long playtimeTicks, String playerValue) {}
}
