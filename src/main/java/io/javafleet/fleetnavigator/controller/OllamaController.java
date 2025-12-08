package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.llm.providers.OllamaProvider;
import io.javafleet.fleetnavigator.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller für Ollama-spezifische Operationen
 *
 * @author JavaFleet Systems Consulting
 * @since 0.3.2
 */
@RestController
@RequestMapping("/api/ollama")
@RequiredArgsConstructor
@Slf4j
public class OllamaController {

    private final OllamaProvider ollamaProvider;
    private final OllamaService ollamaService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Gibt alle installierten Ollama-Modelle zurück
     */
    @GetMapping("/models")
    public ResponseEntity<List<ModelInfo>> getModels() {
        try {
            if (!ollamaProvider.isAvailable()) {
                log.warn("Ollama is not available");
                return ResponseEntity.ok(List.of());
            }

            List<ModelInfo> models = ollamaProvider.getAvailableModels();
            log.info("Retrieved {} Ollama models", models.size());
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Failed to get Ollama models: {}", e.getMessage());
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    /**
     * Pull (download) a model from Ollama with SSE streaming progress
     * @param modelName The model name to pull (e.g., "llama3.2:3b")
     * @return SSE stream with progress updates
     */
    @GetMapping(value = "/pull/{modelName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter pullModel(@PathVariable String modelName) {
        log.info("Starting model pull for: {}", modelName);

        SseEmitter emitter = new SseEmitter(600000L); // 10 minute timeout for large models

        executorService.submit(() -> {
            try {
                // Check if Ollama is available first
                if (!ollamaProvider.isAvailable()) {
                    emitter.send(SseEmitter.event()
                        .data("{\"status\":\"error\",\"error\":\"Ollama ist nicht verfügbar. Bitte starten Sie Ollama mit 'ollama serve'.\"}"));
                    emitter.complete();
                    return;
                }

                ollamaService.pullModel(modelName, progress -> {
                    try {
                        // Parse progress message and send as JSON
                        String jsonData = createProgressJson(progress);
                        emitter.send(SseEmitter.event().data(jsonData));
                    } catch (IOException e) {
                        log.warn("Failed to send SSE event: {}", e.getMessage());
                    }
                });

                // Send completion
                emitter.send(SseEmitter.event().data("{\"status\":\"success\"}"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Model pull failed for {}: {}", modelName, e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                        .data("{\"status\":\"error\",\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        emitter.onCompletion(() -> log.info("SSE connection completed for model pull: {}", modelName));
        emitter.onTimeout(() -> log.warn("SSE connection timed out for model pull: {}", modelName));
        emitter.onError(e -> log.error("SSE error for model pull {}: {}", modelName, e.getMessage()));

        return emitter;
    }

    /**
     * Create JSON progress object from progress string
     */
    private String createProgressJson(String progress) {
        // Parse progress like "downloading 1.5 GB / 2.0 GB (75%)"
        if (progress.contains("downloading") && progress.contains("%")) {
            try {
                int percentStart = progress.lastIndexOf("(");
                int percentEnd = progress.lastIndexOf("%");
                if (percentStart > 0 && percentEnd > percentStart) {
                    String percentStr = progress.substring(percentStart + 1, percentEnd);
                    int percent = Integer.parseInt(percentStr.trim());

                    // Extract sizes
                    String sizesPart = progress.replace("downloading", "").trim();
                    int slashIdx = sizesPart.indexOf("/");
                    String completed = slashIdx > 0 ? sizesPart.substring(0, slashIdx).trim() : "0";
                    String total = slashIdx > 0 ? sizesPart.substring(slashIdx + 1, percentStart).trim() : "0";

                    return String.format("{\"status\":\"downloading\",\"completed\":%d,\"total\":100,\"percent\":%d,\"completedSize\":\"%s\",\"totalSize\":\"%s\"}",
                        percent, percent, completed, total);
                }
            } catch (Exception e) {
                // Fall through to default
            }
        }

        // Default: just return status
        return "{\"status\":\"" + progress.replace("\"", "'") + "\"}";
    }

    /**
     * Prüft ob Ollama verfügbar ist
     */
    @GetMapping("/status")
    public ResponseEntity<OllamaStatus> getStatus() {
        boolean available = ollamaProvider.isAvailable();
        OllamaStatus status = new OllamaStatus();
        status.setAvailable(available);
        status.setMessage(available ? "Ollama is running" : "Ollama is not available");
        return ResponseEntity.ok(status);
    }

    /**
     * DTO für Ollama Status
     */
    public static class OllamaStatus {
        private boolean available;
        private String message;

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
