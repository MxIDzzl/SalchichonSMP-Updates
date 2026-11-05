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

@RestController
@RequestMapping("/api/v1/admin/audit")
@ConditionalOnProperty(prefix = "salchichon.admin", name = "enabled", havingValue = "true")
public class AdminAuditController {
    private final JdbcTemplate jdbc;

    public AdminAuditController(@Qualifier("adminJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> recent(@RequestParam(defaultValue = "50") int limit,
                                            @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1 || limit > 100 || offset < 0 || offset > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        return jdbc.queryForList("""
                SELECT id, actor, action, entity_type, entity_id, reason, details, created_at
                FROM public_admin_audit
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """, limit, offset);
    }
}
