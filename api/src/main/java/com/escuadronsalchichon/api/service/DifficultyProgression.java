package com.escuadronsalchichon.api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

/** Resolves the published difficulty stage from the configured season-day schedule. */
public final class DifficultyProgression {
    private DifficultyProgression() {}

    public static long seasonDay(String seasonStartDate, LocalDate today) {
        if (seasonStartDate == null || today == null) return 0;
        try {
            LocalDate startDate = LocalDate.parse(seasonStartDate);
            long daysSinceStart = ChronoUnit.DAYS.between(startDate, today);
            return daysSinceStart < 0 ? 0 : daysSinceStart + 1;
        } catch (RuntimeException invalidDate) {
            return 0;
        }
    }

    public static String resolve(String baseDifficulty, String seasonStartDate,
                                 String configuredStages, LocalDate today) {
        if (configuredStages == null || configuredStages.isBlank() || today == null) {
            return baseDifficulty;
        }
        long seasonDay = seasonDay(seasonStartDate, today);
        if (seasonDay == 0) return baseDifficulty;
        TreeMap<Long, String> stages = new TreeMap<>();
        for (String entry : configuredStages.split(",")) {
            String[] parts = entry.trim().split(":", 2);
            if (parts.length != 2 || parts[1].isBlank()) continue;
            try {
                long day = Long.parseLong(parts[0].trim());
                if (day > 0) stages.put(day, parts[1].trim());
            } catch (NumberFormatException ignored) {
                // Ignore malformed optional entries; keep the base difficulty.
            }
        }
        Map.Entry<Long, String> active = stages.floorEntry(seasonDay);
        return active == null ? baseDifficulty : active.getValue();
    }
}
