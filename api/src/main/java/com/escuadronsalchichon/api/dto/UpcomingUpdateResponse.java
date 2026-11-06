package com.escuadronsalchichon.api.dto;

import java.time.OffsetDateTime;

public record UpcomingUpdateResponse(
        long id,
        String title,
        String body,
        String category,
        OffsetDateTime scheduledAt,
        boolean teaser
) {}
