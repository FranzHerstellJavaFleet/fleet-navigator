package io.javafleet.fleetnavigator.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.security.CryptoService;
import io.javafleet.fleetnavigator.security.MatePairingService;
import io.javafleet.fleetnavigator.service.*;
import io.javafleet.fleetnavigator.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for Fleet Mate communication with authentication.
 * Supports both legacy mode (no auth) and secure mode (pairing + auth).
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
    private final MatePairingService pairingService;
    private final CryptoService cryptoService;

    // RAG Service injected via setter to avoid circular dependency
    private RAGService ragService;

    // ChatService injected via setter to avoid circular dependency
    private io.javafleet.fleetnavigator.service.ChatService chatService;

    // FleetCode Service injected via setter to avoid circular dependency
    private io.javafleet.fleetnavigator.service.FleetCodeService fleetCodeService;

    // Map of mateId -> WebSocketSession
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    // Authenticated sessions (mateId set)
    private final Set<String> authenticatedSessions = ConcurrentHashMap.newKeySet();

    // Session to mateId mapping for authenticated mates
    private final Map<String, String> sessionToMateId = new ConcurrentHashMap<>();

    // Pending pairing sessions (requestId -> WebSocketSession)
    // Used to send pairing_approved back to the mate
    private final Map<String, WebSocketSession> pendingPairingSessions = new ConcurrentHashMap<>();

    /**
     * Set RAG service (used to avoid circular dependency)
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setRagService(@org.springframework.context.annotation.Lazy RAGService ragService) {
        this.ragService = ragService;
    }

    /**
     * Set ChatService (used to avoid circular dependency)
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setChatService(@org.springframework.context.annotation.Lazy io.javafleet.fleetnavigator.service.ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Set FleetCodeService (used to avoid circular dependency)
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setFleetCodeService(@org.springframework.context.annotation.Lazy io.javafleet.fleetnavigator.service.FleetCodeService fleetCodeService) {
        this.fleetCodeService = fleetCodeService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String mateIdFromPath = extractMateId(session);
        log.info("Fleet Mate connection attempt: {} (session: {})", mateIdFromPath, session.getId());

        // Don't add to activeSessions yet - wait for authentication or registration
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
        String sessionId = session.getId();
        String mateId = sessionToMateId.get(sessionId);

        // Fall back to path-based mateId if not in session map
        if (mateId == null) {
            mateId = extractMateId(session);
        }

        log.info("Fleet Mate disconnected: {} (session: {}, status: {})",
                 mateId, sessionId, status);

        if (mateId != null) {
            activeSessions.remove(mateId);
            authenticatedSessions.remove(mateId);
            fleetMateService.markOffline(mateId);
            pairingService.endSession(mateId);
        }
        sessionToMateId.remove(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String mateId = extractMateId(session);
        log.error("WebSocket error for mate {}: {}", mateId, exception.getMessage(), exception);
    }

    /**
     * Disconnect a mate by closing their WebSocket session
     * Called when a mate is removed from trusted mates
     */
    public void disconnectMate(String mateId) {
        WebSocketSession session = activeSessions.get(mateId);
        if (session != null && session.isOpen()) {
            try {
                log.info("Forcefully disconnecting mate: {}", mateId);
                session.close(CloseStatus.NORMAL.withReason("Mate removed from trusted list"));
            } catch (Exception e) {
                log.error("Failed to close session for mate {}: {}", mateId, e.getMessage());
            }
        }
        // Clean up completely - remove from all tracking
        activeSessions.remove(mateId);
        authenticatedSessions.remove(mateId);
        fleetMateService.removeMate(mateId);  // Completely remove, not just mark offline
        pairingService.endSession(mateId);
        log.info("Mate {} completely removed from all sessions", mateId);
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
            // ========== Security Messages ==========
            case "pairing_request":
                handlePairingRequest(session, message);
                break;
            case "auth":
                handleAuthentication(session, message);
                break;
            case "auth_challenge_request":
                handleAuthChallengeRequest(session, message);
                break;
            case "encrypted":
                handleEncryptedMessage(session, message);
                break;

            // ========== Legacy Registration (backward compatible) ==========
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

            // ========== RAG Messages ==========
            case "file_search_results":
                handleFileSearchResults(message);
                break;
            case "file_content":
                handleFileContent(message);
                break;
            case "context_saved":
                handleContextSaved(message);
                break;
            case "context_loaded":
                handleContextLoaded(message);
                break;
            case "contexts_list":
                handleContextsList(message);
                break;
            case "context_deleted":
                handleContextDeleted(message);
                break;

            // ========== Document Generation Response ==========
            case "document_generated":
                handleDocumentGeneratedResponse(mateId, message);
                break;

            // ========== FleetCode AI Coding Agent ==========
            case "fleetcode_step":
                handleFleetCodeStep(message);
                break;
            case "fleetcode_result":
                handleFleetCodeResult(message);
                break;

            default:
                log.warn("Unknown message type from {}: {}", mateId, type);
        }
    }

    /**
     * Handle mate registration (LEGACY - now disabled for security)
     * All mates must use pairing flow instead.
     */
    private void handleRegistration(WebSocketSession session, MateMessage message) {
        String mateId = extractMateId(session);
        log.warn("Rejected legacy registration from {} - pairing required!", mateId);
        sendError(session, "Legacy registration disabled. Please use pairing flow.");

        // Close the connection after a short delay
        try {
            session.close();
        } catch (Exception e) {
            log.debug("Error closing session: {}", e.getMessage());
        }
    }

    /**
     * Handle hardware stats
     */
    private void handleStats(MateMessage message) {
        try {
            // Convert data object to HardwareStats
            String dataJson = objectMapper.writeValueAsString(message.getData());
            HardwareStats stats = objectMapper.readValue(dataJson, HardwareStats.class);

            // Always use mateId from message (outer envelope)
            // The Go client may send wrong mateId in stats data (from config)
            // but the message.mateId is correct (set after auth_success)
            if (message.getMateId() != null) {
                stats.setMateId(message.getMateId());
            }

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

            // Send response back to mate (encrypted)
            Map<String, Object> response = new HashMap<>();
            response.put("type", "classify_response");
            Map<String, Object> payload = new HashMap<>();
            payload.put("messageId", messageId);
            payload.put("category", classification.category);
            payload.put("confidence", classification.confidence);
            payload.put("reasoning", classification.reasoning);
            payload.put("accountEmail", data.get("accountEmail"));
            response.put("payload", payload);

            sendEncryptedResponse(session, mateId, response);

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

            // Send response back to mate (encrypted)
            Map<String, Object> response = new HashMap<>();
            response.put("type", "reply_generated");
            Map<String, Object> payload = new HashMap<>();
            payload.put("messageId", messageId);
            payload.put("suggestedReply", suggestedReply);
            response.put("payload", payload);

            sendEncryptedResponse(session, mateId, response);

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

            // Send response back to mate (encrypted)
            Map<String, Object> response = new HashMap<>();
            response.put("type", "models_list");
            Map<String, Object> payload = new HashMap<>();
            payload.put("models", models);
            response.put("payload", payload);

            sendEncryptedResponse(session, mateId, response);

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

            // Send response back to mate (encrypted)
            Map<String, Object> response = new HashMap<>();
            response.put("type", "document_generated");
            Map<String, Object> payload = new HashMap<>();
            payload.put("content", generatedText);
            payload.put("model", model);
            response.put("payload", payload);

            sendEncryptedResponse(session, mateId, response);

            log.info("Sent generated document to Office Mate: {}, length={} chars (model: {})",
                    mateId, generatedText.length(), model);

        } catch (Exception e) {
            log.error("Failed to generate document", e);
            sendError(session, "Failed to generate document: " + e.getMessage());
        }
    }

    // ========== RAG Handlers ==========

    /**
     * Handle file search results from Mate
     */
    @SuppressWarnings("unchecked")
    private void handleFileSearchResults(MateMessage message) {
        if (ragService == null) {
            log.warn("RAG service not available");
            return;
        }
        Map<String, Object> data = (Map<String, Object>) message.getData();
        ragService.handleSearchResults(data);
    }

    /**
     * Handle file content from Mate
     */
    @SuppressWarnings("unchecked")
    private void handleFileContent(MateMessage message) {
        if (ragService == null) {
            log.warn("RAG service not available");
            return;
        }
        Map<String, Object> data = (Map<String, Object>) message.getData();
        ragService.handleFileContent(data);
    }

    /**
     * Handle context saved confirmation from Mate
     */
    @SuppressWarnings("unchecked")
    private void handleContextSaved(MateMessage message) {
        if (ragService == null) {
            log.warn("RAG service not available");
            return;
        }
        Map<String, Object> data = (Map<String, Object>) message.getData();
        ragService.handleContextSaved(data);
    }

    /**
     * Handle context loaded from Mate
     */
    @SuppressWarnings("unchecked")
    private void handleContextLoaded(MateMessage message) {
        if (ragService == null) {
            log.warn("RAG service not available");
            return;
        }
        Map<String, Object> data = (Map<String, Object>) message.getData();
        ragService.handleContextLoaded(data);
    }

    /**
     * Handle contexts list from Mate
     */
    @SuppressWarnings("unchecked")
    private void handleContextsList(MateMessage message) {
        if (ragService == null) {
            log.warn("RAG service not available");
            return;
        }
        Map<String, Object> data = (Map<String, Object>) message.getData();
        ragService.handleContextsList(data);
    }

    /**
     * Handle context deleted confirmation from Mate
     */
    @SuppressWarnings("unchecked")
    private void handleContextDeleted(MateMessage message) {
        if (ragService == null) {
            log.warn("RAG service not available");
            return;
        }
        Map<String, Object> data = (Map<String, Object>) message.getData();
        ragService.handleContextDeleted(data);
    }

    // ========== Security Handlers ==========

    /**
     * Handle pairing request from a new Mate
     */
    @SuppressWarnings("unchecked")
    private void handlePairingRequest(WebSocketSession session, MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        // Support both Java naming (name, type) and Go naming (mateName, mateType)
        String mateName = getStringWithFallback(data, "name", "mateName");
        String mateType = getStringWithFallback(data, "type", "mateType");
        String matePublicKey = getStringWithFallback(data, "publicKey", "matePublicKey");
        String mateExchangeKey = getStringWithFallback(data, "exchangeKey", "mateExchangeKey");

        // Get IP addresses - prefer those sent by Mate over WebSocket session IP
        String ipv4 = getStringWithFallback(data, "ipv4", "IPv4");
        String ipv6 = getStringWithFallback(data, "ipv6", "IPv6");

        // Fallback to WebSocket session IP if Mate didn't send any
        if ((ipv4 == null || ipv4.isEmpty()) && (ipv6 == null || ipv6.isEmpty())) {
            try {
                java.net.InetSocketAddress remoteAddr = session.getRemoteAddress();
                if (remoteAddr != null) {
                    java.net.InetAddress addr = remoteAddr.getAddress();
                    String hostAddr = addr.getHostAddress();
                    if (addr instanceof java.net.Inet4Address) {
                        ipv4 = hostAddr;
                    } else if (addr instanceof java.net.Inet6Address) {
                        ipv6 = hostAddr;
                        // Remove scope ID if present (e.g., fe80::1%eth0 -> fe80::1)
                        int scopeIdx = ipv6.indexOf('%');
                        if (scopeIdx > 0) {
                            ipv6 = ipv6.substring(0, scopeIdx);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not extract IP address from session: {}", e.getMessage());
            }
        }

        log.info("Received pairing request from: {} (type: {}, IPv4: {}, IPv6: {})",
                mateName, mateType, ipv4, ipv6);

        try {
            MatePairingService.PairingResponse response = pairingService.createPairingRequest(
                    mateName, mateType, matePublicKey, mateExchangeKey, ipv4, ipv6);

            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("type", "pairing_response");
            // Use "payload" for Go client compatibility
            responseData.put("payload", Map.of(
                    "requestId", response.getRequestId() != null ? response.getRequestId() : "",
                    "mateId", response.getMateId() != null ? response.getMateId() : "",
                    "pairingCode", response.getPairingCode() != null ? response.getPairingCode() : "",
                    "navigatorPublicKey", response.getNavigatorPublicKey(),
                    "navigatorExchangeKey", response.getNavigatorExchangeKey(),
                    "status", response.getStatus().name()
            ));

            String json = objectMapper.writeValueAsString(responseData);
            session.sendMessage(new TextMessage(json));

            if (response.getStatus() == MatePairingService.PairingStatus.ALREADY_PAIRED) {
                log.info("Mate already paired: {}", response.getMateId());
            } else {
                // Store session for later pairing_approved message
                if (response.getRequestId() != null) {
                    pendingPairingSessions.put(response.getRequestId(), session);
                    log.info("Stored session for pairing request: {}", response.getRequestId());
                }
                log.info("Pairing request created, code: {}", response.getPairingCode());
            }

        } catch (Exception e) {
            log.error("Failed to process pairing request", e);
            sendError(session, "Pairing request failed: " + e.getMessage());
        }
    }

    /**
     * Handle authentication challenge request
     */
    @SuppressWarnings("unchecked")
    private void handleAuthChallengeRequest(WebSocketSession session, MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String mateId = (String) data.get("mateId");

        log.info("Auth challenge requested for: {}", mateId);

        try {
            String nonce = pairingService.generateAuthChallenge(mateId);

            Map<String, Object> response = Map.of(
                    "type", "auth_challenge",
                    "payload", Map.of(
                            "mateId", mateId,
                            "nonce", nonce,
                            "navigatorPublicKey", cryptoService.getPublicKeyBase64()
                    )
            );

            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));

            log.debug("Sent auth challenge to: {}", mateId);

        } catch (Exception e) {
            log.error("Failed to generate auth challenge", e);
            sendError(session, "Auth challenge failed: " + e.getMessage());
        }
    }

    /**
     * Handle authentication attempt from Mate
     */
    @SuppressWarnings("unchecked")
    private void handleAuthentication(WebSocketSession session, MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String mateId = (String) data.get("mateId");
        String publicKey = (String) data.get("publicKey");
        String signature = (String) data.get("signature");
        String nonce = (String) data.get("nonce");

        log.info("Authentication attempt from: {}", mateId);

        try {
            MatePairingService.AuthenticationResult result =
                    pairingService.authenticate(mateId, publicKey, signature, nonce);

            if (result.isSuccess()) {
                // Mark session as authenticated
                authenticatedSessions.add(mateId);
                sessionToMateId.put(session.getId(), mateId);
                activeSessions.put(mateId, session);

                // Register mate in FleetMateService for API visibility
                fleetMateService.registerMate(mateId, result.getMateName(),
                        "Authenticated Fleet Mate", session);

                Map<String, Object> response = Map.of(
                        "type", "auth_success",
                        "payload", Map.of(
                                "mateId", result.getMateId(),
                                "mateName", result.getMateName()
                        )
                );

                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));

                log.info("Mate {} authenticated and registered successfully", mateId);

            } else {
                Map<String, Object> response = Map.of(
                        "type", "auth_failed",
                        "payload", Map.of(
                                "error", result.getError()
                        )
                );

                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));

                log.warn("Authentication failed for {}: {}", mateId, result.getError());
            }

        } catch (Exception e) {
            log.error("Authentication error", e);
            sendError(session, "Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Check if a mate is authenticated
     */
    public boolean isMateAuthenticated(String mateId) {
        return authenticatedSessions.contains(mateId);
    }

    /**
     * Get the mateId for a session
     */
    public String getMateIdForSession(String sessionId) {
        return sessionToMateId.get(sessionId);
    }

    /**
     * Send pairing_approved message to a mate that requested pairing.
     * Called by MatePairingController after user approves the pairing.
     * Also registers the mate in FleetMateService so it appears on the dashboard.
     */
    public boolean sendPairingApproved(String requestId, String mateId, String mateName, String navigatorExchangeKey) {
        WebSocketSession session = pendingPairingSessions.remove(requestId);
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send pairing_approved: session not found or closed for requestId {}", requestId);
            return false;
        }

        try {
            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("type", "pairing_approved");
            responseData.put("payload", Map.of(
                    "mateId", mateId,
                    "navigatorExchangeKey", navigatorExchangeKey,
                    "status", "APPROVED"
            ));

            String json = objectMapper.writeValueAsString(responseData);
            session.sendMessage(new TextMessage(json));
            log.info("Sent pairing_approved to mate: {} (requestId: {})", mateId, requestId);

            // Register mate in FleetMateService so it appears on dashboard immediately
            authenticatedSessions.add(mateId);
            sessionToMateId.put(session.getId(), mateId);
            activeSessions.put(mateId, session);
            fleetMateService.registerMate(mateId, mateName, "Newly paired Fleet Mate", session);
            log.info("Mate {} registered in FleetMateService after pairing approval", mateId);

            return true;

        } catch (Exception e) {
            log.error("Failed to send pairing_approved to {}: {}", mateId, e.getMessage());
            return false;
        }
    }

    /**
     * Send pairing_rejected message to a mate.
     */
    public boolean sendPairingRejected(String requestId) {
        WebSocketSession session = pendingPairingSessions.remove(requestId);
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send pairing_rejected: session not found or closed for requestId {}", requestId);
            return false;
        }

        try {
            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("type", "pairing_rejected");
            responseData.put("payload", Map.of(
                    "status", "REJECTED"
            ));

            String json = objectMapper.writeValueAsString(responseData);
            session.sendMessage(new TextMessage(json));
            log.info("Sent pairing_rejected for requestId: {}", requestId);
            return true;

        } catch (Exception e) {
            log.error("Failed to send pairing_rejected: {}", e.getMessage());
            return false;
        }
    }

    // ========== Encrypted Message Handling ==========

    /**
     * Handle an encrypted message from an authenticated mate.
     * Go client sends: {"type": "encrypted", "mateId": "...", "payload": "..."}
     * The payload field is aliased to data in MateMessage.
     */
    @SuppressWarnings("unchecked")
    private void handleEncryptedMessage(WebSocketSession session, MateMessage message) {
        // mateId can be at top level (from Go) or in nested data
        String mateId = message.getMateId();
        String encryptedPayload;

        // payload comes through as data (via @JsonAlias) and can be String or nested Map
        Object dataObj = message.getData();
        if (dataObj instanceof String) {
            // Go client: payload is directly a string
            encryptedPayload = (String) dataObj;
        } else if (dataObj instanceof Map) {
            // Nested format: {"data": {"mateId": "...", "payload": "..."}}
            Map<String, Object> dataMap = (Map<String, Object>) dataObj;
            if (mateId == null) {
                mateId = (String) dataMap.get("mateId");
            }
            encryptedPayload = (String) dataMap.get("payload");
        } else {
            log.error("Invalid encrypted message format");
            return;
        }

        // Verify mate is authenticated
        if (!authenticatedSessions.contains(mateId)) {
            log.warn("Received encrypted message from unauthenticated mate: {}", mateId);
            sendError(session, "Not authenticated");
            return;
        }

        try {
            // Get session secret
            byte[] secret = pairingService.getSessionSecret(mateId);
            if (secret == null) {
                log.error("No session secret for mate: {}", mateId);
                sendError(session, "Session not established");
                return;
            }

            // Decrypt the payload
            String decryptedJson = cryptoService.decrypt(encryptedPayload, secret);
            log.debug("Decrypted message from {}: {} chars", mateId, decryptedJson.length());

            // Parse the decrypted message
            MateMessage innerMessage = objectMapper.readValue(decryptedJson, MateMessage.class);

            // Process the decrypted message (recursive call to normal handler)
            handleMateMessage(session, innerMessage);

        } catch (Exception e) {
            log.error("Failed to decrypt message from {}: {}", mateId, e.getMessage());
            sendError(session, "Decryption failed");
        }
    }

    /**
     * Send an encrypted command to an authenticated mate
     */
    public void sendEncryptedCommand(String mateId, MateCommand command) {
        WebSocketSession session = activeSessions.get(mateId);
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send encrypted command to {}: session not active", mateId);
            return;
        }

        // Verify mate is authenticated
        if (!authenticatedSessions.contains(mateId)) {
            log.warn("Cannot send encrypted command to unauthenticated mate: {}", mateId);
            // Fall back to unencrypted for backwards compatibility
            sendCommand(mateId, command);
            return;
        }

        try {
            // Get session secret
            byte[] secret = pairingService.getSessionSecret(mateId);
            if (secret == null) {
                log.warn("No session secret for {}, sending unencrypted", mateId);
                sendCommand(mateId, command);
                return;
            }

            // Serialize the command
            String commandJson = objectMapper.writeValueAsString(command);

            // Encrypt the command
            String encryptedPayload = cryptoService.encrypt(commandJson, secret);

            // Create encrypted wrapper
            Map<String, Object> encryptedMessage = Map.of(
                    "type", "encrypted",
                    "payload", Map.of(
                            "mateId", mateId,
                            "payload", encryptedPayload
                    )
            );

            String json = objectMapper.writeValueAsString(encryptedMessage);
            session.sendMessage(new TextMessage(json));

            log.debug("Sent encrypted {} command to mate: {}", command.getType(), mateId);

        } catch (Exception e) {
            log.error("Failed to send encrypted command to {}: {}", mateId, e.getMessage(), e);
        }
    }

    /**
     * Send a command - automatically uses encryption if mate is authenticated
     */
    public void sendCommandAuto(String mateId, MateCommand command) {
        if (authenticatedSessions.contains(mateId) && pairingService.getSessionSecret(mateId) != null) {
            sendEncryptedCommand(mateId, command);
        } else {
            sendCommand(mateId, command);
        }
    }

    /**
     * Send encrypted response to mate
     */
    public void sendEncryptedResponse(WebSocketSession session, String mateId, Map<String, Object> response) {
        if (!authenticatedSessions.contains(mateId)) {
            // Fall back to unencrypted
            try {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Failed to send response", e);
            }
            return;
        }

        try {
            byte[] secret = pairingService.getSessionSecret(mateId);
            if (secret == null) {
                String json = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(json));
                return;
            }

            // Serialize and encrypt
            String responseJson = objectMapper.writeValueAsString(response);
            String encryptedPayload = cryptoService.encrypt(responseJson, secret);

            // Create encrypted wrapper
            Map<String, Object> encryptedMessage = Map.of(
                    "type", "encrypted",
                    "payload", Map.of(
                            "mateId", mateId,
                            "payload", encryptedPayload
                    )
            );

            String json = objectMapper.writeValueAsString(encryptedMessage);
            session.sendMessage(new TextMessage(json));

        } catch (Exception e) {
            log.error("Failed to send encrypted response to {}: {}", mateId, e.getMessage());
        }
    }

    /**
     * Get string value from map with fallback key (for Go/Java naming compatibility)
     */
    private String getStringWithFallback(Map<String, Object> data, String primaryKey, String fallbackKey) {
        if (data == null) return null;
        Object value = data.get(primaryKey);
        if (value == null) {
            value = data.get(fallbackKey);
        }
        return value != null ? value.toString() : null;
    }

    // ========== Document Generation Response Handler ==========

    /**
     * Handle document_generated response from Fleet-Mate
     * This is called when Fleet-Mate confirms that a document has been created locally
     */
    @SuppressWarnings("unchecked")
    private void handleDocumentGeneratedResponse(String mateId, MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();

        // Extract response data (using correct field names from Go struct)
        String sessionId = (String) data.get("sessionId");
        String filePath = (String) data.get("path");          // Full path
        String displayPath = (String) data.get("displayPath"); // Path with ~ for home
        String fileName = (String) data.get("fileName");
        String format = (String) data.get("format");
        Boolean success = data.get("success") != null ? (Boolean) data.get("success") : true;
        String error = (String) data.get("error");

        log.info("📄 Document generated from {}: session={}, file={}, path={}, success={}",
                mateId, sessionId, fileName, displayPath, success);

        // Forward to ChatService to handle the response
        if (chatService != null) {
            chatService.handleDocumentGenerated(sessionId, filePath, success, error);
        } else {
            log.warn("ChatService not available - cannot process document_generated response");
        }
    }

    // ========== FleetCode AI Coding Agent Handlers ==========

    /**
     * Handle FleetCode step update from mate
     */
    @SuppressWarnings("unchecked")
    private void handleFleetCodeStep(MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");

        if (sessionId == null) {
            log.warn("FleetCode step without sessionId");
            return;
        }

        log.debug("FleetCode step received for session {}: tool={}", sessionId, data.get("tool"));

        if (fleetCodeService != null) {
            fleetCodeService.sendStep(sessionId, data);
        }
    }

    /**
     * Handle FleetCode final result from mate
     */
    @SuppressWarnings("unchecked")
    private void handleFleetCodeResult(MateMessage message) {
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String sessionId = (String) data.get("sessionId");

        if (sessionId == null) {
            log.warn("FleetCode result without sessionId");
            return;
        }

        Boolean success = data.get("success") != null ? (Boolean) data.get("success") : false;
        log.info("FleetCode result for session {}: success={}", sessionId, success);

        if (fleetCodeService != null) {
            fleetCodeService.sendResult(sessionId, data);
        }
    }
}
