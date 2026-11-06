package com.escuadronsalchichon.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ClanMemberResponse(
        UUID playerUuid,
        String username,
        String rankId,
        Instant joinedAt,
        long kills,
        long deaths,
        double kdr
) {}
