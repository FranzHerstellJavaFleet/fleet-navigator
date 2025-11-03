package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.ModelInfo;
import io.javafleet.fleetnavigator.model.ModelMetadata;
import io.javafleet.fleetnavigator.service.ModelMetadataEnrichmentService;
import io.javafleet.fleetnavigator.service.ModelMetadataService;
import io.javafleet.fleetnavigator.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller for model operations
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Slf4j
public class ModelController {

    private final OllamaService ollamaService;
    private final ModelMetadataService metadataService;
    private final ModelMetadataEnrichmentService enrichmentService;

    // Thread pool for handling model pull operations
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * GET /api/models - Get available Ollama models with metadata
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAvailableModels() {
        try {
            log.info("Fetching available models");
            List<ModelInfo> models = ollamaService.getAvailableModels();

            // Enrich with metadata from database and curated metadata
            List<Map<String, Object>> enrichedModels = new ArrayList<>();
            for (ModelInfo model : models) {
                Map<String, Object> enriched = new HashMap<>();
                enriched.put("name", model.getName());
                enriched.put("size", model.getSize());
                enriched.put("modifiedAt", model.getModifiedAt());
                enriched.put("digest", model.getDigest());  // Include digest for update detection

                // Add curated metadata (release date, training cutoff, etc.)
                enrichmentService.enrichModelInfo(enriched);

                // Add custom metadata from database if available (overrides curated)
                Optional<ModelMetadata> metadata = metadataService.getMetadataByName(model.getName());
                metadata.ifPresent(meta -> {
                    if (meta.getDescription() != null) enriched.put("description", meta.getDescription());
                    if (meta.getSpecialties() != null) enriched.put("specialties", meta.getSpecialties());
                    if (meta.getPublisher() != null) enriched.put("publisher", meta.getPublisher());
                    if (meta.getReleaseDate() != null) enriched.put("releaseDate", meta.getReleaseDate());
                    if (meta.getTrainedUntil() != null) enriched.put("trainedUntil", meta.getTrainedUntil());
                    if (meta.getLicense() != null) enriched.put("license", meta.getLicense());
                    enriched.put("isDefault", meta.getIsDefault());
                    if (meta.getNotes() != null) enriched.put("notes", meta.getNotes());
                });

                enrichedModels.add(enriched);
            }

            return ResponseEntity.ok(enrichedModels);
        } catch (IOException e) {
            log.error("Error fetching models from Ollama", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DELETE /api/models/{name} - Delete a model
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> deleteModel(@PathVariable String name) {
        try {
            log.info("Deleting model: {}", name);
            ollamaService.deleteModel(name);

            // Also delete metadata if exists
            metadataService.deleteMetadata(name);

            return ResponseEntity.ok(Map.of("message", "Model deleted successfully", "model", name));
        } catch (IOException e) {
            log.error("Error deleting model: {}", name, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete model: " + e.getMessage()));
        }
    }

    /**
     * POST /api/models/pull - Download a model with streaming progress
     */
    @PostMapping("/pull")
    public SseEmitter pullModel(@RequestBody Map<String, String> request) {
        String modelName = request.get("name");
        log.info("Starting model pull for: {}", modelName);

        SseEmitter emitter = new SseEmitter(600_000L); // 10 minute timeout

        executorService.execute(() -> {
            try {
                // Send initial event
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"status\":\"Starting download\"}"));

                // Pull model with progress updates
                ollamaService.pullModel(modelName, progress -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("progress")
                                .data("{\"status\":\"" + progress + "\"}"));
                    } catch (IOException e) {
                        log.error("Error sending progress update", e);
                        emitter.completeWithError(e);
                    }
                });

                // Send completion event
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"status\":\"Download complete\"}"));

                emitter.complete();
                log.info("Model pull completed for: {}", modelName);

            } catch (Exception e) {
                log.error("Error during model pull", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\":\"" + e.getMessage() + "\"}"));
                } catch (IOException ex) {
                    log.error("Error sending error event", ex);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * GET /api/models/{name}/details - Get detailed information about a model
     */
    @GetMapping("/{name}/details")
    public ResponseEntity<Map<String, Object>> getModelDetails(@PathVariable String name) {
        try {
            log.info("Fetching details for model: {}", name);

            // Get details from Ollama
            Map<String, Object> details = ollamaService.getModelDetails(name);

            // Enrich with metadata from database
            Optional<ModelMetadata> metadata = metadataService.getMetadataByName(name);
            metadata.ifPresent(meta -> {
                details.put("description", meta.getDescription());
                details.put("specialties", meta.getSpecialties());
                details.put("publisher", meta.getPublisher());
                details.put("releaseDate", meta.getReleaseDate());
                details.put("trainedUntil", meta.getTrainedUntil());
                details.put("license", meta.getLicense());
                details.put("isDefault", meta.getIsDefault());
                details.put("notes", meta.getNotes());
            });

            return ResponseEntity.ok(details);
        } catch (IOException e) {
            log.error("Error fetching model details: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/models/{name}/default - Set a model as default
     */
    @PostMapping("/{name}/default")
    public ResponseEntity<Map<String, String>> setDefaultModel(@PathVariable String name) {
        try {
            log.info("Setting default model to: {}", name);
            metadataService.setDefaultModel(name);
            return ResponseEntity.ok(Map.of("message", "Default model set successfully", "model", name));
        } catch (Exception e) {
            log.error("Error setting default model: {}", name, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to set default model: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/models/{name}/metadata - Update metadata for a model
     */
    @PutMapping("/{name}/metadata")
    public ResponseEntity<ModelMetadata> updateMetadata(
            @PathVariable String name,
            @RequestBody ModelMetadata metadata) {
        try {
            log.info("Updating metadata for model: {}", name);
            metadata.setName(name); // Ensure name matches path variable
            ModelMetadata updated = metadataService.saveMetadata(metadata);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating metadata for model: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/models/default - Get the default model
     * Priority: 1. User-selected default from DB, 2. Fallback to phi:latest (Microsoft Phi - small and efficient)
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, String>> getDefaultModel() {
        Optional<ModelMetadata> defaultModel = metadataService.getDefaultModel();
        if (defaultModel.isPresent()) {
            return ResponseEntity.ok(Map.of("model", defaultModel.get().getName()));
        } else {
            return ResponseEntity.ok(Map.of("model", "phi:latest")); // Fallback to Microsoft Phi
        }
    }

    /**
     * GET /api/models/library - Get all available models from Ollama Library
     */
    @GetMapping("/library")
    public ResponseEntity<List<Map<String, Object>>> getLibraryModels() {
        try {
            log.info("Fetching all models from Ollama Library");
            List<Map<String, Object>> libraryModels = ollamaService.getOllamaLibraryModels();
            return ResponseEntity.ok(libraryModels);
        } catch (IOException e) {
            log.error("Error fetching Ollama Library models", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
