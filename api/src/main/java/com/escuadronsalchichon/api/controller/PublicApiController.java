package com.escuadronsalchichon.api.controller;

import com.escuadronsalchichon.api.dto.AnnouncementResponse;
import com.escuadronsalchichon.api.dto.ClanDetailResponse;
import com.escuadronsalchichon.api.dto.ClanSummaryResponse;
import com.escuadronsalchichon.api.dto.OverviewResponse;
import com.escuadronsalchichon.api.dto.PlayerSummaryResponse;
import com.escuadronsalchichon.api.dto.UpcomingUpdateResponse;
import com.escuadronsalchichon.api.repository.PublicDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PublicApiController {
    private final PublicDataRepository repository;
    private final String seasonName;
    private final String seasonDescription;
    private final String difficulty;
    private final String seasonStartDate;
    private final String difficultyStages;
    private final ZoneId serverZone;

    public PublicApiController(
            PublicDataRepository repository,
            @Value("${salchichon.season.name:TEMPORADA SIN SINCRONIZAR}") String seasonName,
            @Value("${salchichon.season.description:La información oficial de la temporada aún no está sincronizada.}") String seasonDescription,
            @Value("${salchichon.season.difficulty:—}") String difficulty,
            @Value("${salchichon.season.start-date:—}") String seasonStartDate,
            @Value("${salchichon.season.difficulty-stages:}") String difficultyStages,
            @Value("${salchichon.server.timezone:America/Mexico_City}") String serverTimezone
    ) {
        this.repository = repository;
        this.seasonName = seasonName;
        this.seasonDescription = seasonDescription;
        this.difficulty = difficulty;
        this.seasonStartDate = seasonStartDate;
        this.difficultyStages = difficultyStages;
        this.serverZone = ZoneId.of(serverTimezone);
    }

    @GetMapping("/public/overview")
    public OverviewResponse overview() {
        PublicDataRepository.OverviewTotals totals = repository.overviewTotals();
        return new OverviewResponse(totals.players(), totals.officialDeaths(), totals.playtimeTicks(),
                totals.playerValue(), seasonName, seasonDescription, resolveDifficulty(), seasonStartDate, Instant.now());
    }

    @GetMapping("/public/players")
    public List<PlayerSummaryResponse> players(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "value") String sort
    ) {
        validatePage(limit, offset);
        return repository.findPlayers(limit, offset, sort, LocalDate.now(serverZone));
    }

    @GetMapping("/public/players/{uuid}")
    public PlayerSummaryResponse player(@PathVariable UUID uuid) {
        return repository.findPlayer(uuid, LocalDate.now(serverZone))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    }

    @GetMapping("/public/clans")
    public List<ClanSummaryResponse> clans(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "points") String sort
    ) {
        validatePage(limit, offset);
        return repository.findClans(limit, offset, sort);
    }

    @GetMapping("/public/clans/{uuid}")
    public ClanDetailResponse clan(@PathVariable UUID uuid) {
        return repository.findClan(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clan not found"));
    }

    @GetMapping("/public/announcements")
    public List<AnnouncementResponse> announcements(@RequestParam(defaultValue = "10") int limit) {
        validatePage(limit, 0);
        return repository.findAnnouncements(limit);
    }

    @GetMapping("/public/upcoming-update")
    public UpcomingUpdateResponse upcomingUpdate() {
        return repository.findUpcomingUpdate()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No upcoming update"));
    }

    private String resolveDifficulty() {
        if (difficultyStages == null || difficultyStages.isBlank()) return difficulty;
        final LocalDate startDate;
        try {
            startDate = LocalDate.parse(seasonStartDate);
        } catch (RuntimeException invalidDate) {
            return difficulty;
        }
        long seasonDay = Math.max(1, ChronoUnit.DAYS.between(startDate, LocalDate.now(serverZone)) + 1);
        Map<Long, String> stages = new TreeMap<>();
        for (String entry : difficultyStages.split(",")) {
            String[] parts = entry.trim().split(":", 2);
            if (parts.length != 2 || parts[1].isBlank()) continue;
            try {
                long day = Long.parseLong(parts[0].trim());
                if (day > 0) stages.put(day, parts[1].trim());
            } catch (NumberFormatException ignored) {
                // Invalid optional stage entries are ignored; the configured base remains safe.
            }
        }
        Map.Entry<Long, String> active = ((TreeMap<Long, String>) stages).floorEntry(seasonDay);
        return active == null ? difficulty : active.getValue();
    }

    private void validatePage(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0 || offset > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
    }
}
