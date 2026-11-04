package com.escuadronsalchichon.api.dto;

import java.time.Instant;

public record OverviewResponse(
        long players,
        long officialDeaths,
        long playtimeTicks,
        long playerValue,
        String seasonName,
        String difficulty,
        String seasonStartDate,
        Instant generatedAt
) {}
