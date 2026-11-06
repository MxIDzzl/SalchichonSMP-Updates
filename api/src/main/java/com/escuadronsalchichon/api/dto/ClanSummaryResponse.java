package com.escuadronsalchichon.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ClanSummaryResponse(
        UUID uuid,
        String name,
        String tag,
        String description,
        UUID leaderUuid,
        Instant createdAt,
        int memberCount,
        int maxMembers,
        long points,
        int level,
        long kills,
        long deaths,
        double kdr
) {}
