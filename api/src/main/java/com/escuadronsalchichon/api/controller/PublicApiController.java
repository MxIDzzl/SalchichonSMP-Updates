package com.escuadronsalchichon.api.controller;

import com.escuadronsalchichon.api.dto.AnnouncementResponse;
import com.escuadronsalchichon.api.dto.OverviewResponse;
import com.escuadronsalchichon.api.dto.PlayerSummaryResponse;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PublicApiController {
    private final PublicDataRepository repository;
    private final String seasonName;
    private final String difficulty;
    private final String seasonStartDate;

    public PublicApiController(
            PublicDataRepository repository,
            @Value("${salchichon.season.name:UHC SEASON 1}") String seasonName,
            @Value("${salchichon.season.difficulty:HARD}") String difficulty,
            @Value("${salchichon.season.start-date:2026-10-19}") String seasonStartDate
    ) {
        this.repository = repository;
        this.seasonName = seasonName;
        this.difficulty = difficulty;
        this.seasonStartDate = seasonStartDate;
    }

    @GetMapping("/public/overview")
    public OverviewResponse overview() {
        PublicDataRepository.OverviewTotals totals = repository.overviewTotals();
        return new OverviewResponse(totals.players(), totals.officialDeaths(), totals.playtimeTicks(),
                totals.playerValue(), seasonName, difficulty, seasonStartDate, Instant.now());
    }

    @GetMapping("/public/players")
    public List<PlayerSummaryResponse> players(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "value") String sort
    ) {
        validatePage(limit, offset);
        return repository.findPlayers(limit, offset, sort);
    }

    @GetMapping("/public/players/{uuid}")
    public PlayerSummaryResponse player(@PathVariable UUID uuid) {
        return repository.findPlayer(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    }

    @GetMapping("/public/announcements")
    public List<AnnouncementResponse> announcements(@RequestParam(defaultValue = "10") int limit) {
        validatePage(limit, 0);
        return repository.findAnnouncements(limit);
    }

    private void validatePage(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0 || offset > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
    }
}
