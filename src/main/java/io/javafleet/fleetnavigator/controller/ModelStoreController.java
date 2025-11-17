package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.HuggingFaceModelInfo;
import io.javafleet.fleetnavigator.llm.ModelRegistry;
import io.javafleet.fleetnavigator.llm.ModelRegistryEntry;
import io.javafleet.fleetnavigator.service.HuggingFaceService;
import io.javafleet.fleetnavigator.service.ModelDownloadService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller für den Model Store
 *
 * Bietet Zugriff auf verfügbare Modelle und Download-Funktionalität
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.9
 */
@RestController
@RequestMapping("/api/model-store")
@RequiredArgsConstructor
@Slf4j
public class ModelStoreController {

    private final ModelRegistry modelRegistry;
    private final ModelDownloadService downloadService;
    private final HuggingFaceService huggingFaceService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Alle verfügbaren Modelle aus der Registry
     */
    @GetMapping("/all")
    public ResponseEntity<List<ModelRegistryEntry>> getAllModels() {
        List<ModelRegistryEntry> models = modelRegistry.getAllModels();

        // Mark which ones are already downloaded
        models.forEach(model -> {
            boolean downloaded = downloadService.isModelDownloaded(model.getFilename());
            // Note: We can't modify the builder, so this info needs to be added in frontend
            log.debug("Model {} downloaded: {}", model.getId(), downloaded);
        });

        return ResponseEntity.ok(models);
    }

    /**
     * Featured Modelle (Empfehlungen)
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ModelRegistryEntry>> getFeaturedModels() {
        return ResponseEntity.ok(modelRegistry.getFeaturedModels());
    }

    /**
     * Trending Modelle
     */
    @GetMapping("/trending")
    public ResponseEntity<List<ModelRegistryEntry>> getTrendingModels() {
        return ResponseEntity.ok(modelRegistry.getTrendingModels());
    }

    /**
     * Nach Kategorie filtern
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ModelRegistryEntry>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(modelRegistry.getByCategory(category));
    }

    /**
     * Nach max RAM filtern
     */
    @GetMapping("/by-ram/{maxRamGB}")
    public ResponseEntity<List<ModelRegistryEntry>> getByMaxRam(@PathVariable int maxRamGB) {
        return ResponseEntity.ok(modelRegistry.getByMaxRam(maxRamGB));
    }

    /**
     * Modell-Details
     */
    @GetMapping("/{modelId}")
    public ResponseEntity<ModelRegistryEntry> getModelDetails(@PathVariable String modelId) {
        return modelRegistry.findById(modelId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Prüft ob Modell bereits heruntergeladen ist
     */
    @GetMapping("/{modelId}/downloaded")
    public ResponseEntity<DownloadedStatusResponse> isModelDownloaded(@PathVariable String modelId) {
        return modelRegistry.findById(modelId)
            .map(model -> {
                boolean downloaded = downloadService.isModelDownloaded(model.getFilename());
                return ResponseEntity.ok(new DownloadedStatusResponse(modelId, downloaded));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Startet Model-Download mit Server-Sent Events (SSE) für Progress
     */
    @GetMapping(value = "/download/{modelId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter downloadModel(@PathVariable String modelId) {
        log.info("📥 Starting model download request: {}", modelId);

        SseEmitter emitter = new SseEmitter(600_000L); // 10 Minuten Timeout

        executorService.execute(() -> {
            try {
                downloadService.downloadModel(modelId, progressMessage -> {
                    try {
                        log.debug("Progress: {}", progressMessage);
                        emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(progressMessage));
                    } catch (IOException e) {
                        log.error("Error sending progress update", e);
                        emitter.completeWithError(e);
                    }
                });

                // Download abgeschlossen
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("✅ Download abgeschlossen!"));
                emitter.complete();

            } catch (IOException e) {
                log.error("Download failed: {}", modelId, e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("❌ Download fehlgeschlagen: " + e.getMessage()));
                } catch (IOException ex) {
                    log.error("Error sending error event", ex);
                }
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("Unexpected error during download", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Bricht einen aktiven Download ab
     */
    @PostMapping("/download/{modelId}/cancel")
    public ResponseEntity<CancelResponse> cancelDownload(@PathVariable String modelId) {
        boolean cancelled = downloadService.cancelDownload(modelId);
        return ResponseEntity.ok(new CancelResponse(cancelled,
            cancelled ? "Download abgebrochen" : "Kein aktiver Download gefunden"));
    }

    /**
     * Aktive Downloads
     */
    @GetMapping("/downloads/active")
    public ResponseEntity<Map<String, ModelDownloadService.DownloadProgress>> getActiveDownloads() {
        return ResponseEntity.ok(downloadService.getActiveDownloads());
    }

    /**
     * Löscht ein heruntergeladenes Modell
     */
    @DeleteMapping("/{modelId}")
    public ResponseEntity<DeleteResponse> deleteModel(@PathVariable String modelId) {
        return modelRegistry.findById(modelId)
            .map(model -> {
                try {
                    boolean deleted = downloadService.deleteModel(model.getFilename());
                    if (deleted) {
                        return ResponseEntity.ok(new DeleteResponse(true, "Modell gelöscht"));
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new DeleteResponse(false, "Modell nicht gefunden"));
                    }
                } catch (IOException e) {
                    log.error("Error deleting model: {}", modelId, e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new DeleteResponse(false, "Fehler beim Löschen: " + e.getMessage()));
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ===== DTOs =====

    @Data
    public static class DownloadedStatusResponse {
        private String modelId;
        private boolean downloaded;

        public DownloadedStatusResponse(String modelId, boolean downloaded) {
            this.modelId = modelId;
            this.downloaded = downloaded;
        }
    }

    @Data
    public static class CancelResponse {
        private boolean success;
        private String message;

        public CancelResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    @Data
    public static class DeleteResponse {
        private boolean success;
        private String message;

        public DeleteResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    // ============================================================================
    // HUGGINGFACE SEARCH & DISCOVERY
    // ============================================================================

    /**
     * Suche nach GGUF-Modellen auf HuggingFace
     *
     * @param query Suchbegriff (z.B. "qwen", "llama", "german")
     * @param limit Maximale Anzahl der Ergebnisse (default: 50)
     * @return Liste gefundener Modelle
     */
    @GetMapping("/huggingface/search")
    public ResponseEntity<List<HuggingFaceModelInfo>> searchHuggingFace(
            @RequestParam String query,
            @RequestParam(defaultValue = "50") int limit
    ) {
        log.info("Searching HuggingFace for: {} (limit: {})", query, limit);
        List<HuggingFaceModelInfo> results = huggingFaceService.searchModels(query, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Detaillierte Informationen zu einem HuggingFace-Modell
     *
     * @param modelId Vollständige Modell-ID (z.B. "Qwen/Qwen2.5-3B-Instruct-GGUF")
     * @return Detaillierte Modell-Informationen
     */
    @GetMapping("/huggingface/details")
    public ResponseEntity<HuggingFaceModelInfo> getHuggingFaceModelDetails(
            @RequestParam String modelId
    ) {
        log.info("Fetching HuggingFace model details: {}", modelId);
        HuggingFaceModelInfo model = huggingFaceService.getModelDetails(modelId);

        if (model != null) {
            // Fetch README for full description
            String readme = huggingFaceService.getModelReadme(modelId);
            if (readme != null) {
                model.setReadme(readme);
                // Extract short description from README (first paragraph)
                String[] lines = readme.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty() && !line.startsWith("#")) {
                        model.setShortDescription(line.trim());
                        break;
                    }
                }
            }
            return ResponseEntity.ok(model);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Beliebte GGUF-Modelle
     */
    @GetMapping("/huggingface/popular")
    public ResponseEntity<List<HuggingFaceModelInfo>> getPopularModels(
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("Fetching popular GGUF models (limit: {})", limit);
        List<HuggingFaceModelInfo> models = huggingFaceService.getPopularGGUFModels(limit);
        return ResponseEntity.ok(models);
    }

    /**
     * Deutsche GGUF-Modelle
     */
    @GetMapping("/huggingface/german")
    public ResponseEntity<List<HuggingFaceModelInfo>> getGermanModels(
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("Fetching German GGUF models (limit: {})", limit);
        List<HuggingFaceModelInfo> models = huggingFaceService.searchGermanModels(limit);
        return ResponseEntity.ok(models);
    }

    /**
     * Instruct/Chat GGUF-Modelle
     */
    @GetMapping("/huggingface/instruct")
    public ResponseEntity<List<HuggingFaceModelInfo>> getInstructModels(
            @RequestParam(defaultValue = "30") int limit
    ) {
        log.info("Fetching Instruct/Chat GGUF models (limit: {})", limit);
        List<HuggingFaceModelInfo> models = huggingFaceService.searchInstructModels(limit);
        return ResponseEntity.ok(models);
    }

    /**
     * Code-Generation GGUF-Modelle
     */
    @GetMapping("/huggingface/code")
    public ResponseEntity<List<HuggingFaceModelInfo>> getCodeModels(
            @RequestParam(defaultValue = "30") int limit
    ) {
        log.info("Fetching Code GGUF models (limit: {})", limit);
        List<HuggingFaceModelInfo> models = huggingFaceService.searchCodeModels(limit);
        return ResponseEntity.ok(models);
    }

    /**
     * Vision GGUF-Modelle (experimentell - nur mit Ollama)
     */
    @GetMapping("/huggingface/vision")
    public ResponseEntity<List<HuggingFaceModelInfo>> getVisionModels(
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("Fetching Vision GGUF models (limit: {})", limit);
        List<HuggingFaceModelInfo> models = huggingFaceService.searchVisionModels(limit);
        return ResponseEntity.ok(models);
    }

    /**
     * Download GGUF model from HuggingFace with Server-Sent Events (SSE) for Progress
     *
     * @param modelId HuggingFace model ID (e.g., "Qwen/Qwen2.5-3B-Instruct-GGUF")
     * @param filename GGUF filename to download (e.g., "qwen2.5-3b-instruct-q4_k_m.gguf")
     * @return SseEmitter with progress updates
     */
    @GetMapping(value = "/huggingface/download", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter downloadHuggingFaceModel(
            @RequestParam String modelId,
            @RequestParam String filename,
            @RequestParam(required = false) String mmprojFilename
    ) {
        log.info("📥 Starting HuggingFace model download: {} / {} (mmproj: {})",
            modelId, filename, mmprojFilename != null ? mmprojFilename : "none");

        SseEmitter emitter = new SseEmitter(1_800_000L); // 30 minutes timeout for large models

        executorService.execute(() -> {
            try {
                downloadService.downloadHuggingFaceModel(modelId, filename, mmprojFilename, progressMessage -> {
                    try {
                        log.info("📊 Progress: {}", progressMessage);
                        SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .name("progress")
                            .data(progressMessage);
                        emitter.send(event);
                        log.info("✅ SSE event sent successfully");
                    } catch (IOException e) {
                        log.error("❌ Error sending progress update: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                    }
                });

                // Download completed
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("✅ Download abgeschlossen!"));
                emitter.complete();

            } catch (IOException e) {
                log.error("HuggingFace download failed: {} / {}", modelId, filename, e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("❌ Download fehlgeschlagen: " + e.getMessage()));
                } catch (IOException ex) {
                    log.error("Error sending error event", ex);
                }
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("Unexpected error during HuggingFace download", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
