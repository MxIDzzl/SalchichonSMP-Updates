package com.escuadronsalchichon.api.dto;

import java.time.Instant;

public record OverviewResponse(
        long players,
        long officialDeaths,
        long playtimeTicks,
        String playerValue,
        String seasonName,
        String seasonDescription,
        String difficulty,
        String seasonStartDate,
        Instant generatedAt
) {}
