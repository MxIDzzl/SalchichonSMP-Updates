package com.escuadronsalchichon.api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Read-only inspection surface for trade operations that need staff reconciliation.
 * This controller deliberately has no mutation or "force deliver" endpoint.
 */
@RestController
@RequestMapping("/api/v1/admin/trades")
@ConditionalOnProperty(prefix = "salchichon.admin", name = "enabled", havingValue = "true")
public class AdminTradeRecoveryController {
    private static final String RECOVERY_QUERY = """
            SELECT
                s.trade_id,
                s.status AS trade_status,
                s.revision,
                s.created_at AS trade_created_at,
                s.updated_at AS trade_updated_at,
                a.uuid AS player_a_uuid,
                a.username AS player_a_name,
                b.uuid AS player_b_uuid,
                b.username AS player_b_name,
                o.operation_id,
                o.status AS operation_status,
                o.owner_uuid,
                owner.username AS owner_name,
                o.recipient_uuid,
                recipient.username AS recipient_name,
                o.item_fingerprint,
                o.created_at AS operation_created_at,
                o.updated_at AS operation_updated_at
            FROM trade_sessions s
            JOIN player_data a ON a.uuid = s.player_a
            JOIN player_data b ON b.uuid = s.player_b
            LEFT JOIN trade_item_operations o ON o.trade_id = s.trade_id
            LEFT JOIN player_data owner ON owner.uuid = o.owner_uuid
            LEFT JOIN player_data recipient ON recipient.uuid = o.recipient_uuid
            WHERE s.status = 'RECOVERY_REQUIRED'
               OR o.status IN ('PREPARED', 'DELIVERY_PENDING', 'DELIVERING',
                               'RETURN_PENDING', 'RECOVERY_REQUIRED')
            ORDER BY COALESCE(o.updated_at, s.updated_at) ASC, s.trade_id, o.operation_id
            LIMIT ? OFFSET ?
            """;

    private final JdbcTemplate jdbc;

    public AdminTradeRecoveryController(@Qualifier("adminJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/recovery")
    public List<Map<String, Object>> recoveryQueue(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        validatePagination(limit, offset);
        return jdbc.queryForList(RECOVERY_QUERY, limit, offset);
    }

    static void validatePagination(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0 || offset > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
    }
}
