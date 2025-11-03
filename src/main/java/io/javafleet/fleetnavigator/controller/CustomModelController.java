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

        SseEmitter emitter = new SseEmitter(600_000L); // 10 minute timeout

        executorService.execute(() -> {
            try {
                // 1. Create model entry in database
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"status\":\"Erstelle Modell-Eintrag in Datenbank...\"}"));

                CustomModel customModel = customModelService.createCustomModel(request);

                // 2. Create model in Ollama with progress updates
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"status\":\"Erstelle Modell in Ollama...\"}"));

                ollamaService.createModel(
                        customModel.getName(),
                        customModel.getModelfile(),
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

                // 3. Send completion event
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"status\":\"Modell erfolgreich erstellt!\",\"modelId\":" + customModel.getId() + "}"));

                emitter.complete();
                log.info("Custom model creation completed: {}", customModel.getName());

            } catch (Exception e) {
                log.error("Error during custom model creation", e);
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
            try {
                // 1. Create new version in database
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"status\":\"Erstelle neue Version in Datenbank...\"}"));

                CustomModel newVersion = customModelService.updateCustomModel(id, request);

                // 2. Create new version in Ollama
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"status\":\"Erstelle neue Version in Ollama...\"}"));

                ollamaService.createModel(
                        newVersion.getName(),
                        newVersion.getModelfile(),
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

                // 3. Send completion
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"status\":\"Neue Version erstellt!\",\"modelId\":" + newVersion.getId() + ",\"version\":" + newVersion.getVersion() + "}"));

                emitter.complete();
                log.info("Custom model update completed: {}", newVersion.getName());

            } catch (Exception e) {
                log.error("Error during custom model update", e);
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
