package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.LogAnalysisRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for AI-powered log analysis using LLMProviderService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogAnalysisService {

    private final LLMProviderService llmProviderService;
    private final SettingsService settingsService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Store active analysis sessions
    private final Map<String, AnalysisSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Create pending session (before log is read) so SSE can connect
     */
    public void createPendingSession(String sessionId, String mateId, LogAnalysisRequest request) {
        // Get model from settings
        String modelName;
        try {
            modelName = settingsService.getSelectedModel();
            if (modelName == null || modelName.isEmpty()) {
                modelName = "qwen2.5-coder-3b-instruct-q4_k_m.gguf";
            }
        } catch (Exception e) {
            log.warn("Could not load selected model from settings, using default", e);
            modelName = "qwen2.5-coder-3b-instruct-q4_k_m.gguf";
        }

        AnalysisSession session = new AnalysisSession();
        session.sessionId = sessionId;
        session.mateId = mateId;
        session.model = modelName;
        session.prompt = request.getPrompt();
        session.logContent = null; // Will be set later when log is read
        session.emitter = null; // Will be set when SSE connects

        activeSessions.put(sessionId, session);

        log.info("Created pending analysis session: {} for mate: {} with model: {}", sessionId, mateId, modelName);
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
                    .data(Map.of(
                        "progress", progress,
                        "phase", progress < 50 ? "reading" : "analyzing"
                    )));
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
                // Wait for log content to be available (max 60 seconds for large logs)
                int maxWaitSeconds = 60;
                int waited = 0;
                while (session.logContent == null && waited < maxWaitSeconds) {
                    Thread.sleep(1000);
                    waited++;

                    // Log progress every 10 seconds
                    if (waited % 10 == 0) {
                        log.info("Still waiting for log content for session {}, waited {} seconds", sessionId, waited);
                    }
                }

                if (session.logContent == null) {
                    log.error("Timeout waiting for log content for session {} after {} seconds", sessionId, waited);
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Timeout: Log-Inhalt wurde nicht empfangen. Möglicherweise ist die WebSocket-Verbindung abgebrochen."));
                    emitter.completeWithError(new Exception("Log content not received after " + waited + " seconds"));
                    activeSessions.remove(sessionId);
                    return;
                }

                // Limit log content to prevent context overflow (max ~50k chars ≈ 12k tokens)
                String logContent = session.logContent;
                if (logContent.length() > 50000) {
                    log.warn("Log content too large ({} chars), truncating to 50k", logContent.length());
                    logContent = logContent.substring(0, 50000) + "\n\n[... Log gekürzt, zu groß für Kontext ...]";
                }

                String analysisPrompt = buildAnalysisPrompt(logContent, session.prompt);

                // Send start event
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data(Map.of("model", session.model, "timestamp", System.currentTimeMillis())));

                // Signal analysis phase starting (50%)
                sendProgress(sessionId, 50.0);

                // Use LLMProviderService for streaming analysis
                String systemPrompt = "Du bist ein erfahrener Linux System-Administrator und Experte für Log-Analyse. " +
                    "Analysiere das Log präzise, strukturiert und auf Deutsch.";

                // Track tokens for progress estimation
                final int[] tokenCount = {0};
                final int estimatedMaxTokens = 4096;

                llmProviderService.chatStream(
                    session.model,
                    analysisPrompt,
                    systemPrompt,
                    sessionId,
                    chunk -> {
                        try {
                            // Send chunk immediately (don't batch)
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(Map.of("chunk", chunk, "done", false)));

                            // Update analysis progress (50-100% based on token generation)
                            tokenCount[0] += chunk.length() / 4; // Rough estimate: 4 chars = 1 token
                            double analysisProgress = 50.0 + (50.0 * Math.min(1.0, (double) tokenCount[0] / estimatedMaxTokens));
                            sendProgress(sessionId, analysisProgress);

                            // Small delay to prevent overwhelming the connection
                            Thread.sleep(10);
                        } catch (IOException e) {
                            log.error("Error sending chunk to SSE emitter: {}", e.getMessage());
                            throw new RuntimeException("SSE connection broken", e);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Stream interrupted", e);
                        }
                    },
                    4096,  // maxTokens - limit output length
                    0.7,   // temperature
                    null,  // topP
                    null,  // topK
                    null,  // repeatPenalty
                    null   // numCtx
                );

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
     * Build analysis prompt for LLM
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
     * Internal class to track analysis sessions
     */
    private static class AnalysisSession {
        String sessionId;
        String mateId;
        String model;
        String logContent;
        String prompt;
        SseEmitter emitter;
    }
}
