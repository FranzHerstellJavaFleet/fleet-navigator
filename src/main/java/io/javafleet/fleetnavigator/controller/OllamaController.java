package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.llm.providers.OllamaProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
