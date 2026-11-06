package com.escuadronsalchichon.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/admin/players")
@ConditionalOnProperty(prefix = "salchichon.admin", name = "enabled", havingValue = "true")
public class AdminPlayerStatsController {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final String actor;

    public AdminPlayerStatsController(
            @Qualifier("adminJdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("adminTransactionManager") PlatformTransactionManager transactionManager,
            @Value("${salchichon.admin.actor:configured-admin}") String actor) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.actor = actor;
    }

    @PutMapping("/{uuid}/stats/{stat}")
    public Map<String, Object> correctStat(@PathVariable UUID uuid,
                                           @PathVariable String stat,
                                           @Valid @RequestBody StatCorrection request) {
        if (uuid == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player UUID");
        String column = columnFor(stat);
        if (column == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported statistic");

        Long eventId = transaction.execute(status -> {
            var player = jdbc.query("SELECT uuid FROM player_data WHERE uuid = ?",
                    (rs, row) -> UUID.fromString(rs.getString(1)), uuid.toString());
            if (player.isEmpty()) return null;

            jdbc.update("""
                    INSERT IGNORE INTO player_stats(player_uuid, official_deaths, playtime_ticks, updated_at)
                    VALUES (?, 0, 0, UTC_TIMESTAMP(6))
                    """, uuid.toString());

            Long previous = jdbc.queryForObject(
                    "SELECT " + column + " FROM player_stats WHERE player_uuid = ? FOR UPDATE",
                    Long.class, uuid.toString());
            int changed = jdbc.update(
                    "UPDATE player_stats SET " + column + " = ?, updated_at = UTC_TIMESTAMP(6) WHERE player_uuid = ?",
                    request.value(), uuid.toString());
            if (changed != 1) throw new IllegalStateException("Player statistic update was not applied");

            Long createdEvent = insertStatEvent(uuid, column, previous, request);

            jdbc.update("""
                    INSERT INTO public_admin_audit(actor, action, entity_type, entity_id, reason, details)
                    VALUES (?, 'PLAYER_STAT_CORRECTED', 'PLAYER_STAT_EVENT', ?, ?, ?)
                    """, actor, createdEvent, request.reason().trim(),
                    "{\"playerUuid\":\"" + uuid + "\",\"stat\":\"" + column
                            + "\",\"previous\":" + previous + ",\"value\":" + request.value() + "}");
            return createdEvent;
        });

        if (eventId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        return Map.of("playerUuid", uuid, "stat", column, "value", request.value(), "eventId", eventId);
    }

    private Long insertStatEvent(UUID uuid, String column, Long previous, StatCorrection request) {
        String sql = """
                INSERT INTO stat_events(player_uuid, stat_key, previous_value, new_value, actor, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, uuid.toString());
            statement.setString(2, column);
            statement.setLong(3, previous);
            statement.setLong(4, request.value());
            statement.setString(5, actor);
            statement.setString(6, request.reason().trim());
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("Stat event insert did not return an id");
        return key.longValue();
    }

    static String columnFor(String stat) {
        if (stat == null) return null;
        return switch (stat.toLowerCase(java.util.Locale.ROOT)) {
            case "deaths", "official_deaths" -> "official_deaths";
            case "playtime", "playtime_ticks" -> "playtime_ticks";
            default -> null;
        };
    }

    public record StatCorrection(@Min(0) long value, @NotBlank @Size(max = 500) String reason) {}
}
