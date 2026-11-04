package com.escuadronsalchichon.api.dto;

import java.time.OffsetDateTime;

public record AnnouncementResponse(
        long id,
        String title,
        String body,
        String category,
        OffsetDateTime publishedAt,
        OffsetDateTime expiresAt
) {}
