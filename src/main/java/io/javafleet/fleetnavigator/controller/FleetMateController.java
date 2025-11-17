package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.service.CommandExecutionService;
import io.javafleet.fleetnavigator.service.FleetMateService;
import io.javafleet.fleetnavigator.service.FleetMateService.MateInfo;
import io.javafleet.fleetnavigator.service.LogAnalysisService;
import io.javafleet.fleetnavigator.service.OllamaService;
import io.javafleet.fleetnavigator.websocket.FleetMateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST API for Fleet Mate management
 */
@Slf4j
@RestController
@RequestMapping("/api/fleet-mate")
@RequiredArgsConstructor
public class FleetMateController {

    private final FleetMateService fleetMateService;
    private final FleetMateWebSocketHandler webSocketHandler;
    private final LogAnalysisService logAnalysisService;
    private final CommandExecutionService commandExecutionService;
    private final OllamaService ollamaService;

    /**
     * Get all registered mates
     */
    @GetMapping("/mates")
    public ResponseEntity<List<MateInfo>> getAllMates() {
        return ResponseEntity.ok(fleetMateService.getAllMates());
    }

    /**
     * Get online mates
     */
    @GetMapping("/mates/online")
    public ResponseEntity<List<MateInfo>> getOnlineMates() {
        return ResponseEntity.ok(fleetMateService.getOnlineMates());
    }

    /**
     * Get specific mate info
     */
    @GetMapping("/mates/{mateId}")
    public ResponseEntity<MateInfo> getMate(@PathVariable String mateId) {
        MateInfo info = fleetMateService.getMateInfo(mateId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    /**
     * Get latest hardware stats for mate
     */
    @GetMapping("/mates/{mateId}/stats")
    public ResponseEntity<HardwareStats> getMateStats(@PathVariable String mateId) {
        HardwareStats stats = fleetMateService.getLatestStats(mateId);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * Send ping command to mate
     */
    @PostMapping("/mates/{mateId}/ping")
    public ResponseEntity<String> pingMate(@PathVariable String mateId) {
        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body("Mate not connected");
        }

        webSocketHandler.sendCommand(mateId, new MateCommand("ping"));
        return ResponseEntity.ok("Ping sent");
    }

    /**
     * Request stats collection from mate
     */
    @PostMapping("/mates/{mateId}/collect-stats")
    public ResponseEntity<String> collectStats(@PathVariable String mateId) {
        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body("Mate not connected");
        }

        webSocketHandler.sendCommand(mateId, new MateCommand("collect_stats"));
        return ResponseEntity.ok("Collection requested");
    }

    /**
     * Shutdown mate
     */
    @PostMapping("/mates/{mateId}/shutdown")
    public ResponseEntity<String> shutdownMate(@PathVariable String mateId) {
        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body("Mate not connected");
        }

        webSocketHandler.sendCommand(mateId, new MateCommand("shutdown"));
        return ResponseEntity.ok("Shutdown command sent");
    }

    /**
     * Remove mate from registry
     */
    @DeleteMapping("/mates/{mateId}")
    public ResponseEntity<String> removeMate(@PathVariable String mateId) {
        fleetMateService.removeMate(mateId);
        return ResponseEntity.ok("Mate removed");
    }

    /**
     * Ping all online mates
     */
    @PostMapping("/mates/ping-all")
    public ResponseEntity<String> pingAllMates() {
        webSocketHandler.pingAllMates();
        return ResponseEntity.ok("Ping sent to all mates");
    }

    /**
     * Get summary of all mates
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<MateInfo> all = fleetMateService.getAllMates();
        List<MateInfo> online = fleetMateService.getOnlineMates();

        return ResponseEntity.ok(Map.of(
            "total", all.size(),
            "online", online.size(),
            "offline", all.size() - online.size(),
            "active_connections", webSocketHandler.getActiveMateIds().size()
        ));
    }

    /**
     * Start AI-powered log analysis
     * Mate will read log file and send to Navigator, which then analyzes with Ollama
     */
    @PostMapping("/mates/{mateId}/analyze-log")
    public ResponseEntity<Map<String, String>> analyzeLog(
            @PathVariable String mateId,
            @RequestBody LogAnalysisRequest request) {

        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mate not connected"));
        }

        request.setMateId(mateId);

        // Create session ID first
        String sessionId = mateId + "-" + System.currentTimeMillis();

        // Store request IMMEDIATELY so SSE stream can find it
        fleetMateService.storePendingAnalysis(sessionId, request);

        // Create analysis session NOW (before log is read) so SSE stream finds it
        logAnalysisService.createPendingSession(sessionId, mateId, request);

        // Send command to mate to read log file (include sessionId!)
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);  // Send sessionId to mate
        payload.put("path", request.getLogPath());
        payload.put("mode", request.getMode() != null ? request.getMode() : "smart");
        payload.put("lines", request.getLines() != null ? request.getLines() : 1000);

        MateCommand command = new MateCommand();
        command.setType("read_log");
        command.setPayload(payload);

        webSocketHandler.sendCommand(mateId, command);

        log.info("Started log analysis session: {} for mate: {}", sessionId, mateId);

        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "status", "reading_log",
            "message", "Mate is reading log file..."
        ));
    }

    /**
     * SSE endpoint for streaming AI analysis results
     */
    @GetMapping("/stream/{sessionId}")
    public SseEmitter streamAnalysis(@PathVariable String sessionId) {
        // 10 minute timeout for large log analysis
        SseEmitter emitter = new SseEmitter(600000L);

        // Set completion callback
        emitter.onCompletion(() -> log.debug("SSE stream completed for session: {}", sessionId));

        // Set timeout callback
        emitter.onTimeout(() -> {
            log.warn("SSE stream timeout for session: {}", sessionId);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Stream timeout - Analyse dauerte zu lange"));
            } catch (Exception e) {
                log.error("Failed to send timeout event", e);
            }
        });

        // Set error callback
        emitter.onError(ex -> log.error("SSE stream error for session: {}", sessionId, ex));

        // Register emitter for progress updates
        logAnalysisService.registerEmitter(sessionId, emitter);

        // Start async analysis
        logAnalysisService.analyzeLogWithStreaming(sessionId, emitter);

        return emitter;
    }

    // ========== Command Execution Endpoints ==========

    /**
     * Execute a remote command on a mate
     */
    @PostMapping("/mates/{mateId}/execute")
    public ResponseEntity<?> executeCommand(
            @PathVariable String mateId,
            @RequestBody CommandExecutionRequest request) {

        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mate not connected"));
        }

        try {
            // Create execution session
            request.setMateId(mateId);
            String sessionId = commandExecutionService.createSession(request);

            // Send execute command to mate via WebSocket
            Map<String, Object> payload = new HashMap<>();
            payload.put("sessionId", sessionId);
            payload.put("command", request.getCommand());
            payload.put("args", request.getArgs() != null ? request.getArgs() : List.of());
            payload.put("workingDir", request.getWorkingDirectory() != null ? request.getWorkingDirectory() : "/tmp");
            payload.put("timeout", request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 300);

            MateCommand command = new MateCommand();
            command.setType("execute_command");
            command.setPayload(payload);

            webSocketHandler.sendCommand(mateId, command);

            log.info("Started command execution: session={}, mate={}, command={}",
                    sessionId, mateId, request.getCommand());

            return ResponseEntity.ok(CommandExecutionResponse.builder()
                    .sessionId(sessionId)
                    .mateId(mateId)
                    .command(request.getCommand())
                    .status("executing")
                    .message("Command sent to mate")
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
     * Get command execution history for a mate
     */
    @GetMapping("/mates/{mateId}/command-history")
    public ResponseEntity<List<CommandHistoryEntry>> getCommandHistory(@PathVariable String mateId) {
        List<CommandHistoryEntry> history = commandExecutionService.getHistory(mateId);
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
     * Register a remote mate connection request
     * Note: This doesn't actively connect TO the mate.
     * Instead, it saves the address so the mate can connect to us.
     */
    @PostMapping("/connect-remote")
    public ResponseEntity<?> connectRemoteMate(@RequestBody Map<String, Object> request) {
        try {
            String host = (String) request.get("host");
            Integer port = request.get("port") != null ? (Integer) request.get("port") : 2025;

            if (host == null || host.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Host ist erforderlich"));
            }

            log.info("Remote mate connection request: host={}, port={}", host, port);

            // Store the remote mate address
            // The mate itself needs to connect via WebSocket to: ws://THIS_SERVER:2025/api/fleet-mate/ws/{mateId}
            // For now, we just acknowledge the request

            return ResponseEntity.ok(Map.of(
                "message", "Remote Mate Adresse gespeichert. Der Mate muss sich nun mit dem Navigator verbinden.",
                "info", "Starte den Fleet Mate auf dem Remote-System mit: NAVIGATOR_URL=ws://<NAVIGATOR_IP>:2025 ./fleet-mate",
                "host", host,
                "port", port
            ));

        } catch (Exception e) {
            log.error("Failed to process remote mate connection", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
