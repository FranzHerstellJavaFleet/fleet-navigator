package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.CreateCustomModelRequest;
import io.javafleet.fleetnavigator.dto.UpdateCustomModelRequest;
import io.javafleet.fleetnavigator.model.CustomModel;
import io.javafleet.fleetnavigator.service.CustomModelService;
import io.javafleet.fleetnavigator.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller for custom model operations
 */
@RestController
@RequestMapping("/api/custom-models")
@RequiredArgsConstructor
@Slf4j
public class CustomModelController {

    private final CustomModelService customModelService;
    private final OllamaService ollamaService;

    // Thread pool for handling model creation operations
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * GET /api/custom-models - Get all custom models
     */
    @GetMapping
    public ResponseEntity<List<CustomModel>> getAllCustomModels() {
        try {
            log.info("Fetching all custom models");
            List<CustomModel> models = customModelService.getAllCustomModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Error fetching custom models", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/custom-models/{id} - Get custom model by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomModel> getCustomModelById(@PathVariable Long id) {
        try {
            log.info("Fetching custom model by ID: {}", id);
            return customModelService.getCustomModelById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching custom model: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/custom-models/{id}/ancestry - Get ancestry chain for a custom model
     */
    @GetMapping("/{id}/ancestry")
    public ResponseEntity<List<CustomModel>> getAncestry(@PathVariable Long id) {
        try {
            log.info("Fetching ancestry for custom model: {}", id);
            List<CustomModel> ancestry = customModelService.getAncestry(id);
            return ResponseEntity.ok(ancestry);
        } catch (Exception e) {
            log.error("Error fetching ancestry for model: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/custom-models - Create a new custom model with streaming progress
     */
    @PostMapping
    public SseEmitter createCustomModel(@RequestBody CreateCustomModelRequest request) {
        log.info("Creating custom model: {}", request.getName());
        log.info("DEBUG - Request baseModel: '{}'", request.getBaseModel());
        log.info("DEBUG - Request systemPrompt: '{}'", request.getSystemPrompt());
        log.info("DEBUG - Request temperature: {}", request.getTemperature());

        SseEmitter emitter = new SseEmitter(600_000L); // 10 minute timeout

        executorService.execute(() -> {
            CustomModel customModel = null;
            try {
                // 1. Prepare model creation
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"status\":\"Bereite Modell-Erstellung vor...\"}"));

                log.info("Creating custom model '{}' based on '{}'", request.getName(), request.getBaseModel());

                // 2. Create model in Ollama FIRST (prevents zombie models)
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"status\":\"Erstelle Modell in Ollama...\"}"));

                // Ensure model name has :latest tag if no tag specified
                String modelNameWithTag = request.getName().contains(":")
                    ? request.getName()
                    : request.getName() + ":latest";

                // Use new Ollama API format (since v0.5.5): separate fields instead of modelfile string
                ollamaService.createModel(
                        modelNameWithTag,
                        request.getBaseModel(),
                        request.getSystemPrompt(),
                        request.getTemperature(),
                        request.getTopP(),
                        request.getTopK(),
                        request.getRepeatPenalty(),
                        request.getNumPredict(),
                        request.getNumCtx(),
                        progress -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("progress")
                                        .data("{\"status\":\"" + progress + "\"}"));
                            } catch (IOException e) {
                                log.error("Error sending progress update", e);
                                emitter.completeWithError(e);
                            }
                        }
                );

                // 3. ONLY if Ollama succeeded: Save to database
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"status\":\"Speichere in Datenbank...\"}"));

                // Update request with tagged name before saving to database
                request.setName(modelNameWithTag);
                customModel = customModelService.createCustomModel(request);

                // 4. Send completion event
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"status\":\"Modell erfolgreich erstellt!\",\"modelId\":" + customModel.getId() + "}"));

                emitter.complete();
                log.info("Custom model creation completed: {}", customModel.getName());

            } catch (Exception e) {
                log.error("Error during custom model creation", e);

                // If we created Ollama model but DB failed, try to clean up Ollama
                if (customModel == null) {
                    try {
                        log.warn("Cleaning up Ollama model '{}' due to creation failure", request.getName());
                        ollamaService.deleteModel(request.getName());
                    } catch (Exception cleanupEx) {
                        log.error("Failed to cleanup Ollama model after error", cleanupEx);
                    }
                }

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
     * PUT /api/custom-models/{id} - Update custom model (creates new version) with streaming
     */
    @PutMapping("/{id}")
    public SseEmitter updateCustomModel(@PathVariable Long id, @RequestBody UpdateCustomModelRequest request) {
        log.info("Updating custom model: {}", id);

        SseEmitter emitter = new SseEmitter(600_000L);

        executorService.execute(() -> {
            CustomModel newVersion = null;
            String tempModelName = null;
            try {
                // 1. Get original model to generate new version name
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"status\":\"Vorbereite Update...\"}"));

                CustomModel originalModel = customModelService.getCustomModelById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Custom model not found: " + id));

                // Generate new version name (e.g., mymodel:v2)
                int nextVersion = originalModel.getVersion() + 1;
                tempModelName = originalModel.getName().split(":")[0] + ":v" + nextVersion;

                // 2. Create new version in Ollama FIRST (use original base model, updates don't change base)
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"status\":\"Erstelle neue Version in Ollama...\"}"));

                // Use new Ollama API format (since v0.5.5): separate fields instead of modelfile string
                ollamaService.createModel(
                        tempModelName,
                        originalModel.getBaseModel(),
                        request.getSystemPrompt() != null ? request.getSystemPrompt() : originalModel.getSystemPrompt(),
                        request.getTemperature() != null ? request.getTemperature() : originalModel.getTemperature(),
                        request.getTopP() != null ? request.getTopP() : originalModel.getTopP(),
                        request.getTopK() != null ? request.getTopK() : originalModel.getTopK(),
                        request.getRepeatPenalty() != null ? request.getRepeatPenalty() : originalModel.getRepeatPenalty(),
                        request.getNumPredict() != null ? request.getNumPredict() : originalModel.getNumPredict(),
                        request.getNumCtx() != null ? request.getNumCtx() : originalModel.getNumCtx(),
                        progress -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("progress")
                                        .data("{\"status\":\"" + progress + "\"}"));
                            } catch (IOException e) {
                                log.error("Error sending progress update", e);
                                emitter.completeWithError(e);
                            }
                        }
                );

                // 4. ONLY if Ollama succeeded: Save to database
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"status\":\"Speichere in Datenbank...\"}"));

                newVersion = customModelService.updateCustomModel(id, request);

                // 5. Send completion
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"status\":\"Neue Version erstellt!\",\"modelId\":" + newVersion.getId() + ",\"version\":" + newVersion.getVersion() + "}"));

                emitter.complete();
                log.info("Custom model update completed: {}", newVersion.getName());

            } catch (Exception e) {
                log.error("Error during custom model update", e);

                // If we created Ollama model but DB failed, try to clean up Ollama
                if (newVersion == null && tempModelName != null) {
                    try {
                        log.warn("Cleaning up Ollama model '{}' due to update failure", tempModelName);
                        ollamaService.deleteModel(tempModelName);
                    } catch (Exception cleanupEx) {
                        log.error("Failed to cleanup Ollama model after error", cleanupEx);
                    }
                }

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
     * DELETE /api/custom-models/{id} - Delete custom model (from database only, doesn't delete from Ollama)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCustomModel(@PathVariable Long id) {
        try {
            log.info("Deleting custom model: {}", id);

            // Get model name before deleting
            CustomModel model = customModelService.getCustomModelById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Custom model not found: " + id));

            String modelName = model.getName();

            customModelService.deleteCustomModel(id);

            return ResponseEntity.ok(Map.of(
                    "message", "Custom model deleted from database",
                    "model", modelName,
                    "note", "Model still exists in Ollama. Use 'ollama rm " + modelName + "' to remove it."
            ));
        } catch (Exception e) {
            log.error("Error deleting custom model: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete custom model: " + e.getMessage()));
        }
    }

    /**
     * POST /api/custom-models/generate-modelfile - Generate Modelfile preview (without creating)
     */
    @PostMapping("/generate-modelfile")
    public ResponseEntity<Map<String, String>> generateModelfilePreview(@RequestBody CreateCustomModelRequest request) {
        try {
            String modelfile = customModelService.generateModelfile(
                    request.getBaseModel(),
                    request.getSystemPrompt(),
                    request.getTemperature(),
                    request.getTopP(),
                    request.getTopK(),
                    request.getRepeatPenalty()
            );

            return ResponseEntity.ok(Map.of("modelfile", modelfile));
        } catch (Exception e) {
            log.error("Error generating Modelfile preview", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate Modelfile: " + e.getMessage()));
        }
    }
}
