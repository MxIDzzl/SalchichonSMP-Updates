package com.escuadronsalchichon.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/admin/announcements")
@ConditionalOnProperty(prefix = "salchichon.admin", name = "enabled", havingValue = "true")
public class AdminAnnouncementController {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final String actor;
    private final ObjectMapper objectMapper;

    public AdminAnnouncementController(
            @Qualifier("adminJdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("adminTransactionManager") PlatformTransactionManager transactionManager,
            @Value("${salchichon.admin.actor:configured-admin}") String actor,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.actor = actor;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public java.util.List<Map<String, Object>> list(@RequestParam(defaultValue = "50") int limit,
                                                    @RequestParam(defaultValue = "0") int offset) {
        validatePagination(limit, offset);
        return jdbc.queryForList("""
                SELECT id, title, body, category, is_published, published_at, expires_at, scheduled_at,
                       is_teaser, created_by, created_at
                FROM public_announcements
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """, limit, offset);
    }

    @PutMapping("/{id}")
    public Map<String, Object> edit(@PathVariable long id, @Valid @RequestBody EditAnnouncement request) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid announcement id");
        Boolean updated = transaction.execute(status -> {
            int changed = jdbc.update("""
                    UPDATE public_announcements
                    SET title = ?, body = ?, category = ?, scheduled_at = ?, expires_at = ?, is_teaser = ?
                    WHERE id = ?
                    """, request.title().trim(), request.body().trim(), request.category(),
                    request.scheduledAt(), request.expiresAt(), request.teaser(), id);
            if (changed == 0) return false;
            audit("ANNOUNCEMENT_EDITED", id, request.reason(),
                    Map.of("teaser", request.teaser()));
            return true;
        });
        if (!Boolean.TRUE.equals(updated)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
        return Map.of("id", id, "updated", true);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateAnnouncement request) {
        Long id = transaction.execute(status -> {
            Long created = jdbc.queryForObject("""
                    INSERT INTO public_announcements
                        (title, body, category, is_published, published_at, expires_at, scheduled_at, is_teaser, created_by)
                    VALUES (?, ?, ?, ?, COALESCE(?, now()), ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class, request.title().trim(), request.body().trim(), request.category(),
                    request.published(), request.publishedAt(), request.expiresAt(), request.scheduledAt(),
                    request.teaser(), actor);
            audit("ANNOUNCEMENT_CREATED", created, request.reason(),
                    Map.of("published", request.published(), "category", request.category()));
            return created;
        });
        return Map.of("id", id, "status", request.published() ? "PUBLISHED" : "DRAFT");
    }

    @PatchMapping("/{id}/publication")
    public Map<String, Object> setPublication(@PathVariable long id, @Valid @RequestBody PublicationChange request) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid announcement id");
        Boolean updated = transaction.execute(status -> {
            int changed = jdbc.update("""
                    UPDATE public_announcements
                    SET is_published = ?, published_at = CASE WHEN ? THEN COALESCE(published_at, now()) ELSE published_at END
                    WHERE id = ?
                    """, request.published(), request.published(), id);
            if (changed == 0) return false;
            audit(request.published() ? "ANNOUNCEMENT_PUBLISHED" : "ANNOUNCEMENT_UNPUBLISHED",
                    id, request.reason(), Map.of("published", request.published()));
            return true;
        });
        if (!Boolean.TRUE.equals(updated)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
        return Map.of("id", id, "published", request.published());
    }

    private void audit(String action, long entityId, String reason, Map<String, Object> details) {
        jdbc.update("""
                INSERT INTO public_admin_audit(actor, action, entity_type, entity_id, reason, details)
                VALUES (?, ?, 'ANNOUNCEMENT', ?, ?, CAST(? AS jsonb))
                """, actor, action, entityId, reason.trim(), toJson(details));
    }

    private static void validatePagination(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0 || offset > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
    }

    private String toJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize admin audit details", exception);
        }
    }

    public record CreateAnnouncement(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 20_000) String body,
            @NotBlank @Pattern(regexp = "UPDATE|EVENT|NOTICE|LEAK|MAINTENANCE") String category,
            boolean published,
            OffsetDateTime publishedAt,
            OffsetDateTime expiresAt,
            OffsetDateTime scheduledAt,
            boolean teaser,
            @NotBlank @Size(max = 500) String reason) {}

    public record EditAnnouncement(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 20_000) String body,
            @NotBlank @Pattern(regexp = "UPDATE|EVENT|NOTICE|LEAK|MAINTENANCE") String category,
            OffsetDateTime scheduledAt,
            OffsetDateTime expiresAt,
            boolean teaser,
            @NotBlank @Size(max = 500) String reason) {}

    public record PublicationChange(boolean published, @NotBlank @Size(max = 500) String reason) {}
}
