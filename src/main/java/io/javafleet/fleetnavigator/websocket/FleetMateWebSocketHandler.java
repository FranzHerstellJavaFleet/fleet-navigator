package io.javafleet.fleetnavigator.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.service.*;
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
 * WebSocket handler for Fleet Mate communication
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FleetMateWebSocketHandler extends TextWebSocketHandler {

    private final FleetMateService fleetMateService;
    private final LogAnalysisService logAnalysisService;
    private final CommandExecutionService commandExecutionService;
    private final EmailClassificationService emailClassificationService;
    private final EmailReplyService emailReplyService;
    private final ObjectMapper objectMapper;

    // Map of mateId -> WebSocketSession
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String mateId = extractMateId(session);
        log.info("Fleet Mate connected: {} (session: {})", mateId, session.getId());

        if (mateId != null) {
            activeSessions.put(mateId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {} bytes", session.getId(), payload.length());

        try {
            MateMessage mateMessage = objectMapper.readValue(payload, MateMessage.class);
            handleMateMessage(session, mateMessage);
        } catch (Exception e) {
            log.error("Failed to parse mate message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String mateId = extractMateId(session);
        log.info("Fleet Mate disconnected: {} (session: {}, status: {})",
                 mateId, session.getId(), status);

        if (mateId != null) {
            activeSessions.remove(mateId);
            fleetMateService.markOffline(mateId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String mateId = extractMateId(session);
        log.error("WebSocket error for mate {}: {}", mateId, exception.getMessage(), exception);
    }

    /**
     * Handle incoming message from Fleet Mate
     */
    private void handleMateMessage(WebSocketSession session, MateMessage message) {
        String type = message.getType();
        String mateId = message.getMateId();

        // For Office Mate: Extract mateId from session if not in message
        if (mateId == null || mateId.isEmpty()) {
            mateId = extractMateId(session);
        }

        // Only log important messages at INFO level
        if (!type.equals("stats") && !type.equals("heartbeat")) {
            log.info("Received {} message from mate: {}", type, mateId);
        } else {
            log.debug("Received {} message from mate: {}", type, mateId);
        }

        switch (type) {
            case "register":
                handleRegistration(session, message);
                break;
            case "stats":
                handleStats(message);
                break;
            case "heartbeat":
                if (mateId != null) {
                    handleHeartbeat(mateId);
                }
                break;
            case "pong":
                log.debug("Received pong from {}", mateId);
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
            case "classify_email":
                handleEmailClassification(session, message);
                break;
            case "generate_reply":
                handleReplyGeneration(session, message);
                break;
            case "get_models":
                handleGetModels(session, message);
                break;
            case "generate_document":
                handleDocumentGeneration(session, message);
                break;
            default:
                log.warn("Unknown message type from {}: {}", mateId, type);
        }
    }

    /**
     * Handle mate registration
     */
    private void handleRegistration(WebSocketSession session, MateMessage message) {
        // Extract mateId from session URI (Office Mates don't send it in message)
        String mateId = extractMateId(session);
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String name = (String) data.get("name");
        String description = (String) data.get("description");
        String preferredModel = (String) data.get("preferredModel");

        log.info("Registering mate: {} (name: {}, description: {}, model: {})",
                 mateId, name, description, preferredModel);

        fleetMateService.registerMate(mateId, name, description, preferredModel, session);
        activeSessions.put(mateId, session);
    }

    /**
     * Handle hardware stats
     */
    private void handleStats(MateMessage message) {
        try {
            // Convert data object to HardwareStats
            String dataJson = objectMapper.writeValueAsString(message.getData());
            HardwareStats stats = objectMapper.readValue(dataJson, HardwareStats.class);

            // Silent update - no logging for stats (too noisy)
            fleetMateService.updateStats(stats);
        } catch (Exception e) {
            log.error("Failed to process hardware stats: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle heartbeat
     */
    private void handleHeartbeat(String mateId) {
        // Silent update - no logging for heartbeat (too noisy)
        fleetMateService.updateHeartbeat(mateId);
    }

    /**
     * Handle log data chunk from mate with enhanced progress metadata
     */
    private void handleLogData(MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        String chunk = (String) data.get("chunk");
        Object progressObj = data.get("progress");
        Object currentLineObj = data.get("currentLine");
        Object totalLinesObj = data.get("totalLines");
        Object chunkNumberObj = data.get("chunkNumber");
        Object totalChunksObj = data.get("totalChunks");

        if (sessionId != null && chunk != null) {
            fleetMateService.appendLogData(sessionId, chunk);

            // Extract metadata
            int currentLine = currentLineObj instanceof Integer ? (Integer) currentLineObj : 0;
            int totalLines = totalLinesObj instanceof Integer ? (Integer) totalLinesObj : 0;
            int chunkNumber = chunkNumberObj instanceof Integer ? (Integer) chunkNumberObj : 0;
            int totalChunks = totalChunksObj instanceof Integer ? (Integer) totalChunksObj : 0;

            log.debug("Received log chunk {}/{} for session {}: lines {}/{} ({} bytes)",
                     chunkNumber, totalChunks, sessionId, currentLine, totalLines, chunk.length());

            // Send progress update if available (for reading phase)
            if (progressObj != null) {
                double progress = progressObj instanceof Integer
                    ? ((Integer) progressObj).doubleValue()
                    : (Double) progressObj;
                logAnalysisService.sendProgress(sessionId, progress / 2.0); // Reading is 0-50%
                log.debug("Log reading progress for session {}: {}% (chunk {}/{})",
                         sessionId, progress, chunkNumber, totalChunks);
            }
        } else {
            log.warn("Invalid log_data message: missing sessionId or chunk");
        }
    }

    /**
     * Handle log complete notification from mate
     */
    private void handleLogComplete(MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        Integer totalSize = (Integer) data.get("totalSize");

        if (sessionId == null) {
            log.warn("Invalid log_complete message: missing sessionId");
            return;
        }

        // Get complete log data
        String logContent = fleetMateService.getLogData(sessionId);

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
     * Send command to specific mate
     */
    public void sendCommand(String mateId, MateCommand command) {
        WebSocketSession session = activeSessions.get(mateId);
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send command to {}: session not active", mateId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(command);
            session.sendMessage(new TextMessage(json));
            log.info("Sent {} command to mate: {}", command.getType(), mateId);
        } catch (IOException e) {
            log.error("Failed to send command to {}: {}", mateId, e.getMessage(), e);
        }
    }

    /**
     * Send ping to all active mates
     */
    public void pingAllMates() {
        MateCommand ping = new MateCommand("ping");
        activeSessions.keySet().forEach(mateId -> sendCommand(mateId, ping));
    }

    /**
     * Extract mate ID from WebSocket session URI
     */
    private String extractMateId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // Path format: /api/fleet-mate/ws/{mateId}
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    /**
     * Check if mate is connected
     */
    public boolean isMateConnected(String mateId) {
        WebSocketSession session = activeSessions.get(mateId);
        return session != null && session.isOpen();
    }

    /**
     * Get all active mate IDs
     */
    public java.util.Set<String> getActiveMateIds() {
        return activeSessions.keySet();
    }

    // ========== Command Execution Handlers ==========

    /**
     * Handle command stdout output
     */
    private void handleCommandOutput(MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        String content = (String) data.get("content");

        log.debug("Received command output for session {}: {} bytes", sessionId, content.length());
        commandExecutionService.appendOutput(sessionId, "stdout", content);
    }

    /**
     * Handle command stderr output
     */
    private void handleCommandError(MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        String content = (String) data.get("content");

        log.debug("Received command error for session {}: {} bytes", sessionId, content.length());
        commandExecutionService.appendOutput(sessionId, "stderr", content);
    }

    /**
     * Handle command completion
     */
    private void handleCommandComplete(MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");
        Integer exitCode = (Integer) data.get("exitCode");

        log.info("Command completed for session {}: exitCode={}", sessionId, exitCode);
        commandExecutionService.completeExecution(sessionId, exitCode != null ? exitCode : -1);
    }

    // ========== Email Handlers ==========

    /**
     * Handle email classification request
     */
    private void handleEmailClassification(WebSocketSession session, MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        // messageId can be Integer or String, handle both
        Object messageIdObj = data.get("messageId");
        String messageId = messageIdObj != null ? messageIdObj.toString() : "unknown";
        String mateId = extractMateId(session); // Get from session, not message

        log.info("Classifying email: messageId={}, mate={}", messageId, mateId);

        try {
            // Get mate's preferred model
            String model = mateId != null ? getMateModel(mateId) : null;

            // Add model to data if mate has preference
            if (model != null && !model.isEmpty()) {
                data.put("preferredModel", model);
            }

            // Classify email
            EmailClassificationService.EmailClassification classification =
                    emailClassificationService.classifyEmail(data);

            // Send response back to mate
            Map<String, Object> response = Map.of(
                    "type", "classify_response",
                    "data", Map.of(  // Changed from "payload" to "data"
                            "messageId", messageId,
                            "category", classification.category,
                            "confidence", classification.confidence,
                            "reasoning", classification.reasoning,
                            "accountEmail", data.get("accountEmail")
                    )
            );

            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));

            log.info("Sent classification response for {}: category={} (model: {})",
                    messageId, classification.category, model);

        } catch (Exception e) {
            log.error("Failed to classify email", e);
            sendError(session, "Failed to classify email: " + e.getMessage());
        }
    }

    /**
     * Handle email reply generation request
     */
    private void handleReplyGeneration(WebSocketSession session, MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        // messageId can be Integer or String, handle both
        Object messageIdObj = data.get("messageId");
        String messageId = messageIdObj != null ? messageIdObj.toString() : "unknown";
        String from = (String) data.get("from");
        String subject = (String) data.get("subject");
        String body = (String) data.get("body");
        String mateId = extractMateId(session); // Get from session, not message

        log.info("Generating reply for email: messageId={}, from={}, mate={}",
                 messageId, from, mateId);

        try {
            // Get mate's preferred model
            String model = mateId != null ? getMateModel(mateId) : null;

            // Generate reply with mate's preferred model
            String suggestedReply = emailReplyService.generateReply(
                    from, subject, body, model);

            // Send response back to mate
            Map<String, Object> response = Map.of(
                    "type", "reply_generated",
                    "data", Map.of(  // Changed from "payload" to "data"
                            "messageId", messageId,
                            "suggestedReply", suggestedReply
                    )
            );

            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));

            log.info("Sent reply suggestion for {}: {} characters (model: {})",
                    messageId, suggestedReply.length(), model);

        } catch (Exception e) {
            log.error("Failed to generate reply", e);
            sendError(session, "Failed to generate reply: " + e.getMessage());
        }
    }

    /**
     * Get mate's preferred model
     */
    private String getMateModel(String mateId) {
        FleetMateService.MateInfo info = fleetMateService.getMateInfo(mateId);
        return info != null ? info.getPreferredModel() : null;
    }

    /**
     * Send error message to mate
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                    "type", "error",
                    "payload", Map.of("message", errorMessage)
            );

            String json = objectMapper.writeValueAsString(error);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send error message", e);
        }
    }

    // ========== Office Mate Handlers ==========

    /**
     * Handle get models request from Office Mate
     */
    private void handleGetModels(WebSocketSession session, MateMessage message) {
        String mateId = extractMateId(session);
        log.info("Getting available models for Office Mate: {}", mateId);

        try {
            // Get all available Ollama models
            java.util.List<String> models = emailClassificationService.getAvailableModels();

            // Send response back to mate
            Map<String, Object> response = Map.of(
                    "type", "models_list",
                    "data", Map.of(
                            "models", models
                    )
            );

            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));

            log.info("Sent {} models to Office Mate: {}", models.size(), mateId);

        } catch (Exception e) {
            log.error("Failed to get models", e);
            sendError(session, "Failed to get models: " + e.getMessage());
        }
    }

    /**
     * Handle document generation request from Office Mate
     */
    private void handleDocumentGeneration(WebSocketSession session, MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String prompt = (String) data.get("prompt");
        String model = (String) data.get("model");
        String mateId = extractMateId(session);

        log.info("Generating document for Office Mate: {}, model={}, promptLength={}",
                mateId, model, prompt != null ? prompt.length() : 0);

        try {
            // Get mate's preferred model if not specified
            if (model == null || model.isEmpty()) {
                model = getMateModel(mateId);
            }
            if (model == null || model.isEmpty()) {
                model = "llama3.2"; // Fallback to default
            }

            // Generate document - use email reply service with simple context
            String generatedText = emailReplyService.generateSimpleCompletion(prompt, model);

            // Send response back to mate
            Map<String, Object> response = Map.of(
                    "type", "document_generated",
                    "data", Map.of(
                            "content", generatedText,
                            "model", model
                    )
            );

            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));

            log.info("Sent generated document to Office Mate: {}, length={} chars (model: {})",
                    mateId, generatedText.length(), model);

        } catch (Exception e) {
            log.error("Failed to generate document", e);
            sendError(session, "Failed to generate document: " + e.getMessage());
        }
    }
}
