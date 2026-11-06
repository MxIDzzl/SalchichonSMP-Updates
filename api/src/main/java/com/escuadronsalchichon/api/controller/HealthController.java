package com.escuadronsalchichon.api.controller;

import com.escuadronsalchichon.api.repository.PublicDataRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final PublicDataRepository repository;

    public HealthController(PublicDataRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/live")
    public Map<String, Object> live() {
        return Map.of("status", "UP", "timestamp", Instant.now());
    }

    @GetMapping("/ready")
    public Map<String, Object> ready() {
        try {
            if (repository.databaseReady()) {
                return Map.of("status", "READY", "database", "UP", "timestamp", Instant.now());
            }
        } catch (RuntimeException ignored) {
            // Do not leak database connection details in the public health response.
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service not ready");
    }
}
