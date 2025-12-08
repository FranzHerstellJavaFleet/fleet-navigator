package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.MateCommand;
import io.javafleet.fleetnavigator.service.FleetCodeService;
import io.javafleet.fleetnavigator.service.FleetMateService;
import io.javafleet.fleetnavigator.websocket.FleetMateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for FleetCode AI coding agent operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/fleetcode")
@RequiredArgsConstructor
public class FleetCodeController {

    private final FleetCodeService fleetCodeService;
    private final FleetMateService fleetMateService;
    private final FleetMateWebSocketHandler webSocketHandler;

    /**
     * Start a FleetCode execution on a mate.
     * Returns a session ID that can be used to stream results.
     */
    @PostMapping("/execute/{mateId}")
    public ResponseEntity<?> executeFleetCode(
            @PathVariable String mateId,
            @RequestBody FleetCodeRequest request) {

        log.info("FleetCode execute request for mate: {}, task: {}", mateId, request.getTask());

        // Check if mate is online
        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Mate ist nicht verbunden",
                "mateId", mateId
            ));
        }

        // Create session
        String sessionId = fleetCodeService.createSession(
            mateId,
            request.getTask(),
            request.getWorkingDir()
        );

        // Send command to mate
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("task", request.getTask());
        payload.put("workingDir", request.getWorkingDir() != null ? request.getWorkingDir() : "/tmp");

        MateCommand command = new MateCommand("fleetcode_execute", payload);
        webSocketHandler.sendCommand(mateId, command);

        log.info("Sent FleetCode command to mate: {}, session: {}", mateId, sessionId);

        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "mateId", mateId,
            "task", request.getTask()
        ));
    }

    /**
     * Stream FleetCode execution results via SSE.
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResults(@PathVariable String sessionId) {
        log.info("SSE stream requested for FleetCode session: {}", sessionId);

        // 10 minute timeout for long-running tasks
        SseEmitter emitter = new SseEmitter(600000L);

        if (!fleetCodeService.hasSession(sessionId)) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", "Session nicht gefunden")));
                emitter.complete();
            } catch (Exception e) {
                log.error("Failed to send error for unknown session: {}", sessionId);
            }
            return emitter;
        }

        fleetCodeService.registerEmitter(sessionId, emitter);

        // Send initial connected event
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("sessionId", sessionId)));
        } catch (Exception e) {
            log.error("Failed to send connected event: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * Get session status
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        FleetCodeService.FleetCodeSession session = fleetCodeService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("mateId", session.getMateId());
        response.put("task", session.getTask());
        response.put("workingDir", session.getWorkingDir());
        response.put("completed", session.isCompleted());
        response.put("steps", session.getSteps());
        response.put("result", session.getResult());

        return ResponseEntity.ok(response);
    }

    /**
     * Request DTO
     */
    public static class FleetCodeRequest {
        private String task;
        private String workingDir;

        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
        public String getWorkingDir() { return workingDir; }
        public void setWorkingDir(String workingDir) { this.workingDir = workingDir; }
    }
}
