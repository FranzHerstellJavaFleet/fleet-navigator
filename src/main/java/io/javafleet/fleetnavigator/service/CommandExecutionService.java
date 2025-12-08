package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.CommandExecutionRequest;
import io.javafleet.fleetnavigator.dto.CommandHistoryEntry;
import io.javafleet.fleetnavigator.dto.CommandOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing remote command execution on Fleet Mates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommandExecutionService {

    // Session management
    private final Map<String, CommandExecutionRequest> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> outputBuffers = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionStartTimes = new ConcurrentHashMap<>();

    // Command history (last 100 commands per mate)
    private final Map<String, List<CommandHistoryEntry>> commandHistory = new ConcurrentHashMap<>();

    // Whitelisted commands (base commands only)
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        // System info
        "df", "free", "uptime", "uname", "hostname", "whoami", "date",

        // File operations (read-only)
        "ls", "cat", "head", "tail", "grep", "find", "du", "pwd",

        // Process monitoring
        "ps", "top", "htop", "pgrep", "pidof",

        // System services
        "systemctl", "journalctl", "service",

        // Network
        "ping", "curl", "wget", "netstat", "ss", "ip", "ifconfig",

        // Package info (read-only)
        "dpkg", "apt", "yum", "rpm",

        // Other utilities
        "which", "whereis", "file", "stat", "wc", "sort", "uniq",
        "dmesg", "lsblk", "lsusb", "lspci", "env"
    );

    /**
     * Create a new command execution session
     */
    public String createSession(CommandExecutionRequest request) {
        String sessionId = generateSessionId(request.getMateId());

        // Validate command
        validateCommand(request.getCommand());

        // Store session
        activeSessions.put(sessionId, request);
        outputBuffers.put(sessionId, new StringBuilder());
        sessionStartTimes.put(sessionId, LocalDateTime.now());

        log.info("Created command execution session: {} for mate: {}, command: {}",
                sessionId, request.getMateId(), request.getCommand());

        return sessionId;
    }

    /**
     * Register SSE emitter for a session
     */
    public SseEmitter registerEmitter(String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout

        emitter.onCompletion(() -> {
            sseEmitters.remove(sessionId);
            log.debug("SSE emitter completed for session: {}", sessionId);
        });

        emitter.onTimeout(() -> {
            sseEmitters.remove(sessionId);
            log.warn("SSE emitter timeout for session: {}", sessionId);
        });

        emitter.onError((ex) -> {
            sseEmitters.remove(sessionId);
            log.error("SSE emitter error for session: {}", sessionId, ex);
        });

        sseEmitters.put(sessionId, emitter);

        // Send initial start event
        sendEvent(sessionId, "start", Map.of(
            "sessionId", sessionId,
            "message", "Command execution started"
        ));

        return emitter;
    }

    /**
     * Append command output chunk
     */
    public void appendOutput(String sessionId, String type, String content) {
        StringBuilder buffer = outputBuffers.get(sessionId);
        if (buffer != null) {
            buffer.append(content);
        }

        // Stream to SSE if connected
        sendEvent(sessionId, "chunk", Map.of(
            "type", type,
            "content", content,
            "done", false
        ));
    }

    /**
     * Mark command as completed
     */
    public void completeExecution(String sessionId, int exitCode) {
        CommandExecutionRequest request = activeSessions.get(sessionId);
        if (request == null) {
            log.warn("Session not found: {}", sessionId);
            return;
        }

        LocalDateTime startTime = sessionStartTimes.get(sessionId);
        long durationMs = startTime != null
            ? java.time.Duration.between(startTime, LocalDateTime.now()).toMillis()
            : 0;

        // Get output
        StringBuilder buffer = outputBuffers.get(sessionId);
        String fullOutput = buffer != null ? buffer.toString() : "";

        // Create history entry
        CommandHistoryEntry historyEntry = CommandHistoryEntry.builder()
            .sessionId(sessionId)
            .mateId(request.getMateId())
            .command(request.getCommand())
            .fullCommand(buildFullCommand(request))
            .exitCode(exitCode)
            .output(truncateOutput(fullOutput, 5000))
            .durationMs(durationMs)
            .executedAt(startTime)
            .status(exitCode == 0 ? "success" : "failed")
            .build();

        // Add to history
        addToHistory(request.getMateId(), historyEntry);

        // Send completion event
        sendEvent(sessionId, "done", Map.of(
            "exitCode", exitCode,
            "durationMs", durationMs,
            "output", fullOutput
        ));

        // Cleanup
        cleanup(sessionId);

        log.info("Command execution completed: session={}, exitCode={}, duration={}ms",
                sessionId, exitCode, durationMs);
    }

    /**
     * Get command history for a mate
     */
    public List<CommandHistoryEntry> getHistory(String mateId) {
        return commandHistory.getOrDefault(mateId, Collections.emptyList());
    }

    /**
     * Get active session details
     */
    public CommandExecutionRequest getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    // === Private Helper Methods ===

    private String generateSessionId(String mateId) {
        return String.format("%s-cmd-%d", mateId, System.currentTimeMillis());
    }

    private void validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }

        String baseCommand = command.trim().split("\\s+")[0];

        if (!ALLOWED_COMMANDS.contains(baseCommand)) {
            throw new SecurityException("Command not whitelisted: " + baseCommand);
        }
    }

    private String buildFullCommand(CommandExecutionRequest request) {
        StringBuilder cmd = new StringBuilder(request.getCommand());
        if (request.getArgs() != null && !request.getArgs().isEmpty()) {
            cmd.append(" ").append(String.join(" ", request.getArgs()));
        }
        return cmd.toString();
    }

    private void sendEvent(String sessionId, String eventName, Map<String, Object> data) {
        SseEmitter emitter = sseEmitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            } catch (Exception e) {
                log.error("Failed to send SSE event: {}", eventName, e);
                sseEmitters.remove(sessionId);
            }
        }
    }

    private void addToHistory(String mateId, CommandHistoryEntry entry) {
        commandHistory.computeIfAbsent(mateId, k -> new CopyOnWriteArrayList<>())
            .add(0, entry); // Add to beginning

        // Keep only last 100 entries
        List<CommandHistoryEntry> history = commandHistory.get(mateId);
        if (history.size() > 100) {
            history.subList(100, history.size()).clear();
        }
    }

    private String truncateOutput(String output, int maxLength) {
        if (output.length() <= maxLength) {
            return output;
        }
        return output.substring(0, maxLength) + "\n... (truncated)";
    }

    private void cleanup(String sessionId) {
        activeSessions.remove(sessionId);
        outputBuffers.remove(sessionId);
        sessionStartTimes.remove(sessionId);

        SseEmitter emitter = sseEmitters.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
