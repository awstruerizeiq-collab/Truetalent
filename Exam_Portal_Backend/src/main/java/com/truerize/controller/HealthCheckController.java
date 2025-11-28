package com.truerize.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
            "status", "running",
            "app", "Truerize Exam Portal Backend",
            "message", "Backend server is active and ready to accept requests",
            "server", "http://localhost:8080"
        );
    }

    @GetMapping("/api/health")
    public Map<String, Object> apiHealth() {
        return Map.of(
            "success", true,
            "message", "Truerize Exam Portal backend is healthy ✅"
        );
    }
}
