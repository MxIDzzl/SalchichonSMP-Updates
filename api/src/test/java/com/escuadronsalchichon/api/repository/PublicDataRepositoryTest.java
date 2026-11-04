package com.escuadronsalchichon.api.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicDataRepositoryTest {
    @Test
    void usesOnlyAllowlistedSortExpressions() {
        assertEquals("player_value DESC, pd.username ASC", PublicDataRepository.orderBy("value"));
        assertEquals("official_deaths DESC, pd.username ASC", PublicDataRepository.orderBy("deaths"));
        assertEquals("playtime_ticks DESC, pd.username ASC", PublicDataRepository.orderBy("playtime"));
        assertEquals("pd.totem_uses DESC, pd.username ASC", PublicDataRepository.orderBy("totems"));
    }

    @Test
    void unknownSortFallsBackToPlayerValueWithoutInterpolatingInput() {
        assertEquals("player_value DESC, pd.username ASC",
                PublicDataRepository.orderBy("value DESC; DROP TABLE player_data"));
    }
}
