package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.SystemHealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller für System Health Checks
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthCheckService healthCheckService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("healthy", healthCheckService.isHealthy());
        health.put("ollamaAvailable", healthCheckService.isOllamaAvailable());
        health.put("hasModels", healthCheckService.hasModels());
        health.put("sufficientMemory", healthCheckService.hasSufficientMemory());
        health.put("warnings", healthCheckService.getWarnings());
        health.put("errors", healthCheckService.getErrors());
        health.put("summary", healthCheckService.getHealthSummary());

        return ResponseEntity.ok(health);
    }
}
