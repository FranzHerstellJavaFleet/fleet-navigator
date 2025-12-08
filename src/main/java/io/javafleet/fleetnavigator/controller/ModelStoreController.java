package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.HuggingFaceModelInfo;
import io.javafleet.fleetnavigator.dto.SystemStatus;
import io.javafleet.fleetnavigator.llm.ModelRegistry;
import io.javafleet.fleetnavigator.llm.ModelRegistryEntry;
import io.javafleet.fleetnavigator.service.HuggingFaceService;
import io.javafleet.fleetnavigator.service.LlamaServerProcessManager;
import io.javafleet.fleetnavigator.service.ModelDownloadService;
import io.javafleet.fleetnavigator.service.ModelMetadataService;
import io.javafleet.fleetnavigator.service.SystemService;
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
    private final SystemService systemService;
    private final ModelMetadataService modelMetadataService;
    private final LlamaServerProcessManager llamaServerManager;
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
     * IDIOTENSICHERE GPU-PRÜFUNG VOR DOWNLOAD
     *
     * Prüft ob das Modell auf die GPU des Benutzers passt.
     * Schützt vor frustrierenden "out of memory" Fehlern beim Laden.
     *
     * @param modelId Die Modell-ID aus der Registry
     * @return GpuCheckResponse mit Empfehlung und Warnung
     */
    @GetMapping("/{modelId}/check-gpu")
    public ResponseEntity<GpuCheckResponse> checkGpuCompatibility(@PathVariable String modelId) {
        log.info("Checking GPU compatibility for model: {}", modelId);

        return modelRegistry.findById(modelId)
            .map(model -> {
                SystemStatus status = systemService.getSystemStatus();

                // GPU-Informationen
                Long gpuMemoryTotalBytes = status.getGpuMemoryTotal();
                Long gpuMemoryUsedBytes = status.getGpuMemoryUsed();
                String gpuName = status.getGpuName();

                // Model-Größe in Bytes
                long modelSizeBytes = model.getSizeBytes();

                // Konvertiere in GB für bessere Lesbarkeit
                double gpuTotalGB = gpuMemoryTotalBytes != null ? gpuMemoryTotalBytes / (1024.0 * 1024.0 * 1024.0) : 0;
                double gpuUsedGB = gpuMemoryUsedBytes != null ? gpuMemoryUsedBytes / (1024.0 * 1024.0 * 1024.0) : 0;
                double gpuFreeGB = gpuTotalGB - gpuUsedGB;
                double modelSizeGB = modelSizeBytes / (1024.0 * 1024.0 * 1024.0);

                // VRAM-Schätzung: Modell braucht ca. 1.2x seine Dateigröße im VRAM
                // (für Q4_K_M ist das ziemlich genau, für andere Quantisierungen variiert es)
                double estimatedVramGB = modelSizeGB * 1.2;

                // Mindestens 1 GB Puffer für System/CUDA Overhead
                double safetyBufferGB = 1.0;
                double requiredVramGB = estimatedVramGB + safetyBufferGB;

                // Prüfung
                boolean canRun = gpuTotalGB >= requiredVramGB;
                boolean hasWarning = !canRun || gpuFreeGB < requiredVramGB;

                String warning = null;
                String recommendation = null;

                if (gpuTotalGB == 0) {
                    // Keine GPU erkannt
                    canRun = false;
                    warning = "Keine GPU erkannt! Dieses Modell benötigt eine NVIDIA GPU mit mindestens " +
                             String.format("%.1f GB", requiredVramGB) + " VRAM.";
                    recommendation = "CPU_ONLY";
                } else if (!canRun) {
                    // GPU zu klein für das Modell
                    warning = String.format(
                        "WARNUNG: Deine GPU (%s) hat nur %.1f GB VRAM. " +
                        "Das Modell '%s' benötigt ca. %.1f GB VRAM. " +
                        "Das Modell wird NICHT auf deine GPU passen!",
                        gpuName, gpuTotalGB, model.getDisplayName(), requiredVramGB
                    );
                    recommendation = "CHOOSE_SMALLER_MODEL";
                } else if (gpuFreeGB < requiredVramGB) {
                    // GPU groß genug, aber aktuell belegt
                    warning = String.format(
                        "Hinweis: Deine GPU hat genug VRAM (%.1f GB), aber aktuell sind nur %.1f GB frei. " +
                        "Schließe andere Programme oder starte einen neuen Chat, um VRAM freizugeben.",
                        gpuTotalGB, gpuFreeGB
                    );
                    recommendation = "FREE_VRAM";
                }

                log.info("GPU Check Result: model={}, gpuTotal={}GB, gpuFree={}GB, modelSize={}GB, estimatedVram={}GB, canRun={}",
                    modelId, String.format("%.1f", gpuTotalGB), String.format("%.1f", gpuFreeGB),
                    String.format("%.1f", modelSizeGB), String.format("%.1f", estimatedVramGB), canRun);

                return ResponseEntity.ok(new GpuCheckResponse(
                    modelId,
                    model.getDisplayName(),
                    canRun,
                    hasWarning,
                    warning,
                    recommendation,
                    gpuName != null ? gpuName : "Keine GPU",
                    gpuTotalGB,
                    gpuFreeGB,
                    modelSizeGB,
                    estimatedVramGB,
                    model.getMinRamGB(),
                    model.getRecommendedRamGB()
                ));
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

    /**
     * Response für GPU-Kompatibilitätsprüfung
     * Enthält alle Informationen die der User braucht um zu entscheiden
     */
    @Data
    public static class GpuCheckResponse {
        private String modelId;
        private String modelName;
        private boolean canRun;           // Passt das Modell auf die GPU?
        private boolean hasWarning;       // Gibt es eine Warnung?
        private String warning;           // Warnungstext (null wenn keine)
        private String recommendation;    // "CPU_ONLY", "CHOOSE_SMALLER_MODEL", "FREE_VRAM", null
        private String gpuName;           // Name der GPU
        private double gpuTotalGB;        // Gesamter VRAM
        private double gpuFreeGB;         // Freier VRAM
        private double modelSizeGB;       // Modell-Dateigröße
        private double estimatedVramGB;   // Geschätzter VRAM-Bedarf
        private int minRamGB;             // Minimum RAM laut Registry
        private int recommendedRamGB;     // Empfohlener RAM laut Registry

        public GpuCheckResponse(String modelId, String modelName, boolean canRun, boolean hasWarning,
                               String warning, String recommendation, String gpuName,
                               double gpuTotalGB, double gpuFreeGB, double modelSizeGB,
                               double estimatedVramGB, int minRamGB, int recommendedRamGB) {
            this.modelId = modelId;
            this.modelName = modelName;
            this.canRun = canRun;
            this.hasWarning = hasWarning;
            this.warning = warning;
            this.recommendation = recommendation;
            this.gpuName = gpuName;
            this.gpuTotalGB = gpuTotalGB;
            this.gpuFreeGB = gpuFreeGB;
            this.modelSizeGB = modelSizeGB;
            this.estimatedVramGB = estimatedVramGB;
            this.minRamGB = minRamGB;
            this.recommendedRamGB = recommendedRamGB;
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

                // Download completed - set as default and start llama-server
                try {
                    // Check if there's already a default model
                    var currentDefault = modelMetadataService.getDefaultModel();
                    if (currentDefault.isEmpty() || "phi:latest".equals(currentDefault.get().getName())) {
                        // No default or placeholder default - set this model as default
                        modelMetadataService.setDefaultModel(filename);
                        log.info("✅ Downloaded model {} set as default", filename);
                    }

                    // Start llama-server with the downloaded model if not already running
                    LlamaServerProcessManager.ServerStatus status = llamaServerManager.getStatus();
                    if (!status.isOnline()) {
                        log.info("🚀 Starting llama-server with newly downloaded model: {}", filename);
                        LlamaServerProcessManager.StartResult result = llamaServerManager.startServer(
                            filename,
                            2026,   // Standard-Port
                            8192,   // Context Size
                            99      // GPU Layers (alle)
                        );
                        if (result.isSuccess()) {
                            log.info("✅ llama-server gestartet auf Port {}", result.getPort());
                        } else {
                            log.warn("⚠️ llama-server konnte nicht gestartet werden: {}", result.getMessage());
                        }
                    } else {
                        log.info("ℹ️ llama-server läuft bereits auf Port {}", status.getPort());
                    }
                } catch (Exception e) {
                    log.warn("Could not set downloaded model as default or start llama-server: {}", e.getMessage());
                }

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
