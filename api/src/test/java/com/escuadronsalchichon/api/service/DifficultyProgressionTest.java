package com.escuadronsalchichon.api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DifficultyProgressionTest {
    @Test
    void advancesOnTheConfiguredSeasonDay() {
        String stages = "1:HARD,14:HARD+,28:EXTREME";
        assertEquals("HARD", DifficultyProgression.resolve(
                "BASE", "2026-11-01", stages, LocalDate.of(2026, 11, 13)));
        assertEquals("HARD+", DifficultyProgression.resolve(
                "BASE", "2026-11-01", stages, LocalDate.of(2026, 11, 14)));
        assertEquals("EXTREME", DifficultyProgression.resolve(
                "BASE", "2026-11-01", stages, LocalDate.of(2026, 11, 28)));
    }

    @Test
    void usesBaseDifficultyWhenScheduleOrStartDateIsUnavailable() {
        LocalDate today = LocalDate.of(2026, 11, 13);
        assertEquals("BASE", DifficultyProgression.resolve("BASE", "2026-11-01", "", today));
        assertEquals("BASE", DifficultyProgression.resolve("BASE", "—", "1:HARD", today));
        assertEquals("BASE", DifficultyProgression.resolve("BASE", "2026-11-01", "invalid", today));
    }

    @Test
    void doesNotActivateAStageBeforeItsThreshold() {
        assertEquals("BASE", DifficultyProgression.resolve(
                "BASE", "2026-11-01", "14:HARD+,28:EXTREME", LocalDate.of(2026, 11, 10)));
    }
}
