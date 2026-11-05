package com.escuadronsalchichon.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Pattern;
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

    public AdminAnnouncementController(
            @Qualifier("adminJdbcTemplate") JdbcTemplate jdbc,
            @Qualifier("adminTransactionManager") PlatformTransactionManager transactionManager,
            @Value("${salchichon.admin.actor:configured-admin}") String actor) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        this.actor = actor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateAnnouncement request,
                                      @RequestHeader("X-Admin-Actor") @Size(min = 2, max = 64) String actor) {
        Long id = transaction.execute(status -> {
            Long created = jdbc.queryForObject("""
                    INSERT INTO public_announcements
                        (title, body, category, is_published, published_at, expires_at, scheduled_at, is_teaser, created_by)
                    VALUES (?, ?, ?, ?, COALESCE(?, now()), ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class, request.title().trim(), request.body().trim(), request.category(),
                    request.published(), request.publishedAt(), request.expiresAt(), request.scheduledAt(),
                    request.teaser(), actor);
            audit(actor, "ANNOUNCEMENT_CREATED", created, request.reason(),
                    Map.of("published", request.published(), "category", request.category()));
            return created;
        });
        return Map.of("id", id, "status", request.published() ? "PUBLISHED" : "DRAFT");
    }

    @PatchMapping("/{id}/publication")
    public Map<String, Object> setPublication(@PathVariable long id, @Valid @RequestBody PublicationChange request,
                                               @RequestHeader("X-Admin-Actor") @Size(min = 2, max = 64) String actor) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid announcement id");
        Boolean updated = transaction.execute(status -> {
            int changed = jdbc.update("UPDATE public_announcements SET is_published = ?, published_at = CASE WHEN ? THEN COALESCE(published_at, now()) ELSE published_at END WHERE id = ?",
                    request.published(), request.published(), id);
            if (changed == 0) return false;
            audit(actor, request.published() ? "ANNOUNCEMENT_PUBLISHED" : "ANNOUNCEMENT_UNPUBLISHED",
                    id, request.reason(), Map.of("published", request.published()));
            return true;
        });
        if (!Boolean.TRUE.equals(updated)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found");
        return Map.of("id", id, "published", request.published());
    }

    private void audit(String actor, String action, long entityId, String reason, Map<String, Object> details) {
        jdbc.update("""
                INSERT INTO public_admin_audit(actor, action, entity_type, entity_id, reason, details)
                VALUES (?, ?, 'ANNOUNCEMENT', ?, ?, CAST(? AS jsonb))
                """, actor, action, entityId, reason.trim(), toJson(details));
    }

    private String toJson(Map<String, Object> values) {
        return values.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
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

    public record PublicationChange(boolean published, @NotBlank @Size(max = 500) String reason) {}
}
