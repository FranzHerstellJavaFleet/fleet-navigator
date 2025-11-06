package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.service.CommandExecutionService;
import io.javafleet.fleetnavigator.service.FleetOfficerService;
import io.javafleet.fleetnavigator.service.FleetOfficerService.OfficerInfo;
import io.javafleet.fleetnavigator.service.LogAnalysisService;
import io.javafleet.fleetnavigator.service.OllamaService;
import io.javafleet.fleetnavigator.websocket.FleetOfficerWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST API for Fleet Officer management
 */
@Slf4j
@RestController
@RequestMapping("/api/fleet-officer")
@RequiredArgsConstructor
public class FleetOfficerController {

    private final FleetOfficerService fleetOfficerService;
    private final FleetOfficerWebSocketHandler webSocketHandler;
    private final LogAnalysisService logAnalysisService;
    private final CommandExecutionService commandExecutionService;
    private final OllamaService ollamaService;

    /**
     * Get all registered officers
     */
    @GetMapping("/officers")
    public ResponseEntity<List<OfficerInfo>> getAllOfficers() {
        return ResponseEntity.ok(fleetOfficerService.getAllOfficers());
    }

    /**
     * Get online officers
     */
    @GetMapping("/officers/online")
    public ResponseEntity<List<OfficerInfo>> getOnlineOfficers() {
        return ResponseEntity.ok(fleetOfficerService.getOnlineOfficers());
    }

    /**
     * Get specific officer info
     */
    @GetMapping("/officers/{officerId}")
    public ResponseEntity<OfficerInfo> getOfficer(@PathVariable String officerId) {
        OfficerInfo info = fleetOfficerService.getOfficerInfo(officerId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    /**
     * Get latest hardware stats for officer
     */
    @GetMapping("/officers/{officerId}/stats")
    public ResponseEntity<HardwareStats> getOfficerStats(@PathVariable String officerId) {
        HardwareStats stats = fleetOfficerService.getLatestStats(officerId);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * Send ping command to officer
     */
    @PostMapping("/officers/{officerId}/ping")
    public ResponseEntity<String> pingOfficer(@PathVariable String officerId) {
        if (!webSocketHandler.isOfficerConnected(officerId)) {
            return ResponseEntity.badRequest().body("Officer not connected");
        }

        webSocketHandler.sendCommand(officerId, new OfficerCommand("ping"));
        return ResponseEntity.ok("Ping sent");
    }

    /**
     * Request stats collection from officer
     */
    @PostMapping("/officers/{officerId}/collect-stats")
    public ResponseEntity<String> collectStats(@PathVariable String officerId) {
        if (!webSocketHandler.isOfficerConnected(officerId)) {
            return ResponseEntity.badRequest().body("Officer not connected");
        }

        webSocketHandler.sendCommand(officerId, new OfficerCommand("collect_stats"));
        return ResponseEntity.ok("Collection requested");
    }

    /**
     * Shutdown officer
     */
    @PostMapping("/officers/{officerId}/shutdown")
    public ResponseEntity<String> shutdownOfficer(@PathVariable String officerId) {
        if (!webSocketHandler.isOfficerConnected(officerId)) {
            return ResponseEntity.badRequest().body("Officer not connected");
        }

        webSocketHandler.sendCommand(officerId, new OfficerCommand("shutdown"));
        return ResponseEntity.ok("Shutdown command sent");
    }

    /**
     * Remove officer from registry
     */
    @DeleteMapping("/officers/{officerId}")
    public ResponseEntity<String> removeOfficer(@PathVariable String officerId) {
        fleetOfficerService.removeOfficer(officerId);
        return ResponseEntity.ok("Officer removed");
    }

    /**
     * Ping all online officers
     */
    @PostMapping("/officers/ping-all")
    public ResponseEntity<String> pingAllOfficers() {
        webSocketHandler.pingAllOfficers();
        return ResponseEntity.ok("Ping sent to all officers");
    }

    /**
     * Get summary of all officers
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<OfficerInfo> all = fleetOfficerService.getAllOfficers();
        List<OfficerInfo> online = fleetOfficerService.getOnlineOfficers();

        return ResponseEntity.ok(Map.of(
            "total", all.size(),
            "online", online.size(),
            "offline", all.size() - online.size(),
            "active_connections", webSocketHandler.getActiveOfficerIds().size()
        ));
    }

    /**
     * Start AI-powered log analysis
     * Officer will read log file and send to Navigator, which then analyzes with Ollama
     */
    @PostMapping("/officers/{officerId}/analyze-log")
    public ResponseEntity<Map<String, String>> analyzeLog(
            @PathVariable String officerId,
            @RequestBody LogAnalysisRequest request) {

        if (!webSocketHandler.isOfficerConnected(officerId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Officer not connected"));
        }

        request.setOfficerId(officerId);

        // Create session ID first
        String sessionId = officerId + "-" + System.currentTimeMillis();

        // Store request IMMEDIATELY so SSE stream can find it
        fleetOfficerService.storePendingAnalysis(sessionId, request);

        // Create analysis session NOW (before log is read) so SSE stream finds it
        logAnalysisService.createPendingSession(sessionId, officerId, request);

        // Send command to officer to read log file (include sessionId!)
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);  // Send sessionId to officer
        payload.put("path", request.getLogPath());
        payload.put("mode", request.getMode() != null ? request.getMode() : "smart");
        payload.put("lines", request.getLines() != null ? request.getLines() : 1000);

        OfficerCommand command = new OfficerCommand();
        command.setType("read_log");
        command.setPayload(payload);

        webSocketHandler.sendCommand(officerId, command);

        log.info("Started log analysis session: {} for officer: {}", sessionId, officerId);

        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "status", "reading_log",
            "message", "Officer is reading log file..."
        ));
    }

    /**
     * SSE endpoint for streaming AI analysis results
     */
    @GetMapping("/stream/{sessionId}")
    public SseEmitter streamAnalysis(@PathVariable String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout

        // Register emitter for progress updates
        logAnalysisService.registerEmitter(sessionId, emitter);

        // Start async analysis
        logAnalysisService.analyzeLogWithStreaming(sessionId, emitter);

        return emitter;
    }

    // ========== Command Execution Endpoints ==========

    /**
     * Execute a remote command on an officer
     */
    @PostMapping("/officers/{officerId}/execute")
    public ResponseEntity<?> executeCommand(
            @PathVariable String officerId,
            @RequestBody CommandExecutionRequest request) {

        if (!webSocketHandler.isOfficerConnected(officerId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Officer not connected"));
        }

        try {
            // Create execution session
            request.setOfficerId(officerId);
            String sessionId = commandExecutionService.createSession(request);

            // Send execute command to officer via WebSocket
            Map<String, Object> payload = new HashMap<>();
            payload.put("sessionId", sessionId);
            payload.put("command", request.getCommand());
            payload.put("args", request.getArgs() != null ? request.getArgs() : List.of());
            payload.put("workingDir", request.getWorkingDirectory() != null ? request.getWorkingDirectory() : "/tmp");
            payload.put("timeout", request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 300);

            OfficerCommand command = new OfficerCommand();
            command.setType("execute_command");
            command.setPayload(payload);

            webSocketHandler.sendCommand(officerId, command);

            log.info("Started command execution: session={}, officer={}, command={}",
                    sessionId, officerId, request.getCommand());

            return ResponseEntity.ok(CommandExecutionResponse.builder()
                    .sessionId(sessionId)
                    .officerId(officerId)
                    .command(request.getCommand())
                    .status("executing")
                    .message("Command sent to officer")
                    .build());

        } catch (SecurityException e) {
            log.warn("Command rejected for security reasons: {}", e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to execute command", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * SSE endpoint for streaming command output
     */
    @GetMapping("/exec-stream/{sessionId}")
    public SseEmitter streamCommandOutput(@PathVariable String sessionId) {
        return commandExecutionService.registerEmitter(sessionId);
    }

    /**
     * Get command execution history for an officer
     */
    @GetMapping("/officers/{officerId}/command-history")
    public ResponseEntity<List<CommandHistoryEntry>> getCommandHistory(@PathVariable String officerId) {
        List<CommandHistoryEntry> history = commandExecutionService.getHistory(officerId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get list of whitelisted commands
     */
    @GetMapping("/whitelisted-commands")
    public ResponseEntity<Map<String, Object>> getWhitelistedCommands() {
        // Quick Actions - predefined useful commands
        List<Map<String, String>> quickActions = List.of(
            Map.of("label", "Disk Space", "command", "df", "args", "-h"),
            Map.of("label", "Memory Usage", "command", "free", "args", "-h"),
            Map.of("label", "System Uptime", "command", "uptime", "args", ""),
            Map.of("label", "Top CPU Processes", "command", "ps", "args", "aux --sort=-cpu"),
            Map.of("label", "Top Memory Processes", "command", "ps", "args", "aux --sort=-mem"),
            Map.of("label", "System Log (last 50)", "command", "journalctl", "args", "-n 50"),
            Map.of("label", "Failed Services", "command", "systemctl", "args", "list-units --state=failed"),
            Map.of("label", "Open Ports", "command", "ss", "args", "-tuln"),
            Map.of("label", "Network Interfaces", "command", "ip", "args", "addr show"),
            Map.of("label", "Kernel Messages", "command", "dmesg", "args", "| tail -50")
        );

        return ResponseEntity.ok(Map.of(
            "quickActions", quickActions,
            "message", "Predefined quick actions for common tasks"
        ));
    }

    /**
     * Get available Ollama models
     */
    @GetMapping("/ollama-models")
    public ResponseEntity<?> getOllamaModels() {
        try {
            var models = ollamaService.getAvailableModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Failed to fetch Ollama models: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Register a remote officer connection request
     * Note: This doesn't actively connect TO the officer.
     * Instead, it saves the address so the officer can connect to us.
     */
    @PostMapping("/connect-remote")
    public ResponseEntity<?> connectRemoteOfficer(@RequestBody Map<String, Object> request) {
        try {
            String host = (String) request.get("host");
            Integer port = request.get("port") != null ? (Integer) request.get("port") : 2025;

            if (host == null || host.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Host ist erforderlich"));
            }

            log.info("Remote officer connection request: host={}, port={}", host, port);

            // Store the remote officer address
            // The officer itself needs to connect via WebSocket to: ws://THIS_SERVER:2025/api/fleet-officer/ws/{officerId}
            // For now, we just acknowledge the request

            return ResponseEntity.ok(Map.of(
                "message", "Remote Officer Adresse gespeichert. Der Officer muss sich nun mit dem Navigator verbinden.",
                "info", "Starte den Fleet Officer auf dem Remote-System mit: NAVIGATOR_URL=ws://<NAVIGATOR_IP>:2025 ./fleet-officer",
                "host", host,
                "port", port
            ));

        } catch (Exception e) {
            log.error("Failed to process remote officer connection", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
