package com.escuadronsalchichon.api.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminPlayerStatsControllerTest {
    @Test
    void onlyOfficialDeathsAndPlaytimeAreCorrectable() {
        assertEquals("official_deaths", AdminPlayerStatsController.columnFor("deaths"));
        assertEquals("official_deaths", AdminPlayerStatsController.columnFor("OFFICIAL_DEATHS"));
        assertEquals("playtime_ticks", AdminPlayerStatsController.columnFor("playtime"));
        assertEquals("playtime_ticks", AdminPlayerStatsController.columnFor("playtime_ticks"));
    }

    @Test
    void playerValueAndUnknownColumnsAreNeverAccepted() {
        assertNull(AdminPlayerStatsController.columnFor("value"));
        assertNull(AdminPlayerStatsController.columnFor("player_value"));
        assertNull(AdminPlayerStatsController.columnFor("username"));
        assertNull(AdminPlayerStatsController.columnFor(null));
    }
}
