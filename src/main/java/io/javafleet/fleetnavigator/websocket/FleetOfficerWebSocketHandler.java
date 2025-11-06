package io.javafleet.fleetnavigator.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.service.CommandExecutionService;
import io.javafleet.fleetnavigator.service.FleetOfficerService;
import io.javafleet.fleetnavigator.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for Fleet Officer communication
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FleetOfficerWebSocketHandler extends TextWebSocketHandler {

    private final FleetOfficerService fleetOfficerService;
    private final LogAnalysisService logAnalysisService;
    private final CommandExecutionService commandExecutionService;
    private final ObjectMapper objectMapper;

    // Map of officerId -> WebSocketSession
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String officerId = extractOfficerId(session);
        log.info("Fleet Officer connected: {} (session: {})", officerId, session.getId());

        if (officerId != null) {
            activeSessions.put(officerId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {} bytes", session.getId(), payload.length());

        try {
            OfficerMessage officerMessage = objectMapper.readValue(payload, OfficerMessage.class);
            handleOfficerMessage(session, officerMessage);
        } catch (Exception e) {
            log.error("Failed to parse officer message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String officerId = extractOfficerId(session);
        log.info("Fleet Officer disconnected: {} (session: {}, status: {})",
                 officerId, session.getId(), status);

        if (officerId != null) {
            activeSessions.remove(officerId);
            fleetOfficerService.markOffline(officerId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String officerId = extractOfficerId(session);
        log.error("WebSocket error for officer {}: {}", officerId, exception.getMessage(), exception);
    }

    /**
     * Handle incoming message from Fleet Officer
     */
    private void handleOfficerMessage(WebSocketSession session, OfficerMessage message) {
        String type = message.getType();
        String officerId = message.getOfficerId();

        // Only log important messages at INFO level
        if (!type.equals("stats") && !type.equals("heartbeat")) {
            log.info("Received {} message from officer: {}", type, officerId);
        } else {
            log.debug("Received {} message from officer: {}", type, officerId);
        }

        switch (type) {
            case "register":
                handleRegistration(session, message);
                break;
            case "stats":
                handleStats(message);
                break;
            case "heartbeat":
                handleHeartbeat(officerId);
                break;
            case "pong":
                log.debug("Received pong from {}", officerId);
                break;
            case "log_data":
                handleLogData(message);
                break;
            case "log_complete":
                handleLogComplete(message);
                break;
            case "command_output":
                handleCommandOutput(message);
                break;
            case "command_error":
                handleCommandError(message);
                break;
            case "command_complete":
                handleCommandComplete(message);
                break;
            default:
                log.warn("Unknown message type from {}: {}", officerId, type);
        }
    }

    /**
     * Handle officer registration
     */
    private void handleRegistration(WebSocketSession session, OfficerMessage message) {
        String officerId = message.getOfficerId();
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String name = (String) data.get("name");
        String description = (String) data.get("description");

        log.info("Registering officer: {} (name: {}, description: {})",
                 officerId, name, description);

        fleetOfficerService.registerOfficer(officerId, name, description, session);
        activeSessions.put(officerId, session);
    }

    /**
     * Handle hardware stats
     */
    private void handleStats(OfficerMessage message) {
        try {
            // Convert data object to HardwareStats
            String dataJson = objectMapper.writeValueAsString(message.getData());
            HardwareStats stats = objectMapper.readValue(dataJson, HardwareStats.class);

            // Silent update - no logging for stats (too noisy)
            fleetOfficerService.updateStats(stats);
        } catch (Exception e) {
            log.error("Failed to process hardware stats: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle heartbeat
     */
    private void handleHeartbeat(String officerId) {
        // Silent update - no logging for heartbeat (too noisy)
        fleetOfficerService.updateHeartbeat(officerId);
    }

    /**
     * Handle log data chunk from officer
     */
    private void handleLogData(OfficerMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        String chunk = (String) data.get("chunk");
        Object progressObj = data.get("progress");

        if (sessionId != null && chunk != null) {
            fleetOfficerService.appendLogData(sessionId, chunk);
            log.debug("Received log chunk for session: {}, size: {} bytes", sessionId, chunk.length());

            // Send progress update if available
            if (progressObj != null) {
                double progress = progressObj instanceof Integer
                    ? ((Integer) progressObj).doubleValue()
                    : (Double) progressObj;
                logAnalysisService.sendProgress(sessionId, progress);
                log.debug("Forwarded progress update for session {}: {}%", sessionId, progress);
            }
        } else {
            log.warn("Invalid log_data message: missing sessionId or chunk");
        }
    }

    /**
     * Handle log complete notification from officer
     */
    private void handleLogComplete(OfficerMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        Integer totalSize = (Integer) data.get("totalSize");

        if (sessionId == null) {
            log.warn("Invalid log_complete message: missing sessionId");
            return;
        }

        // Get complete log data
        String logContent = fleetOfficerService.getLogData(sessionId);

        if (logContent != null) {
            log.info("Log reading completed for session: {}, total size: {} bytes", sessionId, totalSize);

            // Update existing analysis session with log content
            logAnalysisService.updateSessionWithLogContent(sessionId, logContent);

            log.info("Updated analysis session {} with log content", sessionId);
        } else {
            log.warn("Cannot find log content for session {}", sessionId);
        }
    }

    /**
     * Send command to specific officer
     */
    public void sendCommand(String officerId, OfficerCommand command) {
        WebSocketSession session = activeSessions.get(officerId);
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send command to {}: session not active", officerId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(command);
            session.sendMessage(new TextMessage(json));
            log.info("Sent {} command to officer: {}", command.getType(), officerId);
        } catch (IOException e) {
            log.error("Failed to send command to {}: {}", officerId, e.getMessage(), e);
        }
    }

    /**
     * Send ping to all active officers
     */
    public void pingAllOfficers() {
        OfficerCommand ping = new OfficerCommand("ping");
        activeSessions.keySet().forEach(officerId -> sendCommand(officerId, ping));
    }

    /**
     * Extract officer ID from WebSocket session URI
     */
    private String extractOfficerId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // Path format: /api/fleet-officer/ws/{officerId}
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    /**
     * Check if officer is connected
     */
    public boolean isOfficerConnected(String officerId) {
        WebSocketSession session = activeSessions.get(officerId);
        return session != null && session.isOpen();
    }

    /**
     * Get all active officer IDs
     */
    public java.util.Set<String> getActiveOfficerIds() {
        return activeSessions.keySet();
    }

    // ========== Command Execution Handlers ==========

    /**
     * Handle command stdout output
     */
    private void handleCommandOutput(OfficerMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        String content = (String) data.get("content");

        log.debug("Received command output for session {}: {} bytes", sessionId, content.length());
        commandExecutionService.appendOutput(sessionId, "stdout", content);
    }

    /**
     * Handle command stderr output
     */
    private void handleCommandError(OfficerMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        String content = (String) data.get("content");

        log.debug("Received command error for session {}: {} bytes", sessionId, content.length());
        commandExecutionService.appendOutput(sessionId, "stderr", content);
    }

    /**
     * Handle command completion
     */
    private void handleCommandComplete(OfficerMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        Integer exitCode = (Integer) data.get("exitCode");

        log.info("Command completed for session {}: exitCode={}", sessionId, exitCode);
        commandExecutionService.completeExecution(sessionId, exitCode != null ? exitCode : -1);
    }
}
