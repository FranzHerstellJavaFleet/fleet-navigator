package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.SystemStatus;
import io.javafleet.fleetnavigator.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for system monitoring
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
public class SystemController {

    private final SystemService systemService;

    /**
     * GET /api/system/status - Get system status
     */
    @GetMapping("/status")
    public ResponseEntity<SystemStatus> getSystemStatus() {
        log.debug("Fetching system status");
        SystemStatus status = systemService.getSystemStatus();
        return ResponseEntity.ok(status);
    }
}
