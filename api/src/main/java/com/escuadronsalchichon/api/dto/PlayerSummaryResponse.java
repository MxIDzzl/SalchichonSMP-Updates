package com.escuadronsalchichon.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PlayerSummaryResponse(
        UUID uuid,
        String username,
        LocalDate lastDeathDate,
        long totemUses,
        long officialDeaths,
        long playtimeTicks,
        String playerValue
) {}
