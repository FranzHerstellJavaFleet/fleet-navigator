package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.dto.LogAnalysisRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for AI-powered log analysis using Ollama
 */
@Service
@Slf4j
public class LogAnalysisService {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    // Store active analysis sessions
    private final Map<String, AnalysisSession> activeSessions = new ConcurrentHashMap<>();

    public LogAnalysisService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Create pending session (before log is read) so SSE can connect
     */
    public void createPendingSession(String sessionId, String officerId, LogAnalysisRequest request) {
        AnalysisSession session = new AnalysisSession();
        session.sessionId = sessionId;
        session.officerId = officerId;
        session.model = request.getModel() != null ? request.getModel() : "mistral:latest";
        session.prompt = request.getPrompt();
        session.logContent = null; // Will be set later when log is read
        session.emitter = null; // Will be set when SSE connects

        activeSessions.put(sessionId, session);

        log.info("Created pending analysis session: {} for officer: {}", sessionId, officerId);
    }

    /**
     * Register SSE emitter for a session (so we can send progress updates)
     */
    public void registerEmitter(String sessionId, SseEmitter emitter) {
        AnalysisSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.emitter = emitter;
        }
    }

    /**
     * Send progress update to SSE stream
     */
    public void sendProgress(String sessionId, double progress) {
        AnalysisSession session = activeSessions.get(sessionId);
        if (session != null && session.emitter != null) {
            try {
                session.emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(Map.of("progress", progress)));
            } catch (IOException e) {
                log.debug("Failed to send progress event: {}", e.getMessage());
            }
        }
    }

    /**
     * Update session with log content when reading is complete
     */
    public void updateSessionWithLogContent(String sessionId, String logContent) {
        AnalysisSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.logContent = logContent;
            log.info("Updated session {} with log content ({} bytes)", sessionId, logContent.length());
        }
    }

    /**
     * Analyze log with Ollama and stream results to SSE emitter
     */
    public void analyzeLogWithStreaming(String sessionId, SseEmitter emitter) {
        AnalysisSession session = activeSessions.get(sessionId);
        if (session == null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Session not found: " + sessionId));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send error event", e);
            }
            return;
        }

        executorService.execute(() -> {
            try {
                // Wait for log content to be available (max 30 seconds)
                int maxWaitSeconds = 30;
                int waited = 0;
                while (session.logContent == null && waited < maxWaitSeconds) {
                    Thread.sleep(1000);
                    waited++;
                }

                if (session.logContent == null) {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Timeout waiting for log content"));
                    emitter.completeWithError(new Exception("Log content not received"));
                    return;
                }

                String analysisPrompt = buildAnalysisPrompt(session.logContent, session.prompt);

                // Send start event
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data(Map.of("model", session.model, "timestamp", System.currentTimeMillis())));

                // Call Ollama API with streaming
                streamFromOllama(session.model, analysisPrompt, emitter);

                // Send completion event
                emitter.send(SseEmitter.event()
                    .name("done")
                    .data(Map.of("timestamp", System.currentTimeMillis())));

                emitter.complete();
                activeSessions.remove(sessionId);

                log.info("Completed log analysis session: {}", sessionId);

            } catch (Exception e) {
                log.error("Error during log analysis", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Analysis failed: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ioEx) {
                    log.error("Failed to send error event", ioEx);
                }
            }
        });
    }

    /**
     * Stream responses from Ollama API
     */
    private void streamFromOllama(String model, String prompt, SseEmitter emitter) throws Exception {
        String url = ollamaBaseUrl + "/api/generate";

        Map<String, Object> request = Map.of(
            "model", model,
            "prompt", prompt,
            "stream", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.execute(
            URI.create(url),
            HttpMethod.POST,
            req -> {
                req.getHeaders().addAll(headers);
                objectMapper.writeValue(req.getBody(), request);
            },
            response -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        JsonNode json = objectMapper.readTree(line);
                        String chunk = json.get("response").asText();
                        boolean done = json.get("done").asBoolean();

                        // Stream chunk to frontend
                        emitter.send(SseEmitter.event()
                            .name("chunk")
                            .data(Map.of("chunk", chunk, "done", done)));

                        if (done) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    log.error("Error reading Ollama stream", e);
                    throw new RuntimeException(e);
                }
                return null;
            }
        );
    }

    /**
     * Build analysis prompt for Ollama
     */
    private String buildAnalysisPrompt(String logContent, String customPrompt) {
        String defaultPrompt = """
            Du bist ein erfahrener Linux System-Administrator und Experte für Log-Analyse.

            Analysiere folgendes System-Log:

            ```
            %s
            ```

            %s

            Gib eine strukturierte Analyse mit:
            1. 🔴 Kritische Fehler (CRITICAL/ERROR) - mit Zeitstempel und Kontext
            2. ⚠️  Warnungen (WARNING) - potenzielle Probleme
            3. 📊 Auffälligkeiten - ungewöhnliche Muster oder Wiederholungen
            4. 💡 Empfehlungen - konkrete Handlungsvorschläge

            Antworte auf Deutsch, präzise und technisch fundiert.
            Verwende Markdown-Formatierung für bessere Lesbarkeit.
            """;

        String task = (customPrompt != null && !customPrompt.isEmpty())
            ? customPrompt
            : "Analysiere das Log nach Fehlern, Warnungen und Auffälligkeiten.";

        return String.format(defaultPrompt, logContent, task);
    }

    /**
     * Generate unique session ID
     */
    private String generateSessionId(String officerId) {
        return officerId + "-" + System.currentTimeMillis();
    }

    /**
     * Internal class to track analysis sessions
     */
    private static class AnalysisSession {
        String sessionId;
        String officerId;
        String model;
        String logContent;
        String prompt;
        SseEmitter emitter;
    }
}
