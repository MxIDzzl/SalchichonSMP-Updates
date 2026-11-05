package com.escuadronsalchichon.api.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicDataRepositoryTest {
    @Test
    void lifeAvailabilityUsesTheConfiguredServerDate() {
        var today = java.time.LocalDate.of(2026, 11, 4);
        assertEquals(true, PublicDataRepository.hasLifeOn(null, today));
        assertEquals(true, PublicDataRepository.hasLifeOn(today.minusDays(1), today));
        assertEquals(false, PublicDataRepository.hasLifeOn(today, today));
        assertEquals(false, PublicDataRepository.hasLifeOn(today.plusDays(1), today));
    }

    @Test
    void usesOnlyAllowlistedSortExpressions() {
        assertEquals("player_value DESC, pd.uuid ASC", PublicDataRepository.orderBy("value"));
        assertEquals("official_deaths DESC, pd.uuid ASC", PublicDataRepository.orderBy("deaths"));
        assertEquals("playtime_ticks DESC, pd.uuid ASC", PublicDataRepository.orderBy("playtime"));
        assertEquals("pd.totem_uses DESC, pd.uuid ASC", PublicDataRepository.orderBy("totems"));
    }

    @Test
    void unknownSortFallsBackToPlayerValueWithoutInterpolatingInput() {
        assertEquals("player_value DESC, pd.uuid ASC",
                PublicDataRepository.orderBy("value DESC; DROP TABLE player_data"));
    }
}
