package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.model.GgufModelConfig;
import io.javafleet.fleetnavigator.repository.GgufModelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for managing GGUF model configurations.
 * Allows creating custom model configs with specific context sizes and system prompts.
 */
@RestController
@RequestMapping("/api/gguf-models")
@RequiredArgsConstructor
@Slf4j
public class GgufModelConfigController {

    private final GgufModelConfigRepository ggufModelConfigRepository;

    /**
     * Get all GGUF model configurations
     */
    @GetMapping
    public ResponseEntity<List<GgufModelConfig>> getAllConfigs() {
        log.info("Getting all GGUF model configurations");
        return ResponseEntity.ok(ggufModelConfigRepository.findAll());
    }

    /**
     * Get a specific GGUF model configuration
     */
    @GetMapping("/{id}")
    public ResponseEntity<GgufModelConfig> getConfig(@PathVariable Long id) {
        return ggufModelConfigRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get configuration by model name
     */
    @GetMapping("/by-name/{name}")
    public ResponseEntity<GgufModelConfig> getConfigByName(@PathVariable String name) {
        return ggufModelConfigRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new GGUF model configuration
     */
    @PostMapping
    public ResponseEntity<?> createConfig(@RequestBody GgufModelConfig config) {
        log.info("Creating new GGUF model config: {}", config.getName());

        // Check if name already exists
        if (ggufModelConfigRepository.existsByName(config.getName())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Model configuration with name '" + config.getName() + "' already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // If this is marked as default, unset other defaults
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            ggufModelConfigRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setIsDefault(false);
                ggufModelConfigRepository.save(existing);
            });
        }

        GgufModelConfig saved = ggufModelConfigRepository.save(config);
        log.info("Created GGUF model config with ID: {}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Upload system prompt from text file
     */
    @PostMapping("/upload-prompt")
    public ResponseEntity<Map<String, String>> uploadPrompt(@RequestParam("file") MultipartFile file) {
        log.info("Uploading system prompt file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File is empty");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, String> response = new HashMap<>();
            response.put("content", content);
            response.put("filename", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update an existing GGUF model configuration
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfig(@PathVariable Long id, @RequestBody GgufModelConfig config) {
        return ggufModelConfigRepository.findById(id)
                .map(existing -> {
                    // Check if name changed and if new name already exists
                    if (!existing.getName().equals(config.getName()) &&
                        ggufModelConfigRepository.existsByName(config.getName())) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Model configuration with name '" + config.getName() + "' already exists");
                        return ResponseEntity.status(HttpStatus.CONFLICT).body((Object) error);
                    }

                    // Update fields
                    existing.setName(config.getName());
                    existing.setBaseModel(config.getBaseModel());
                    existing.setSystemPrompt(config.getSystemPrompt());
                    existing.setContextSize(config.getContextSize());
                    existing.setGpuLayers(config.getGpuLayers());
                    existing.setTemperature(config.getTemperature());
                    existing.setTopP(config.getTopP());
                    existing.setTopK(config.getTopK());
                    existing.setRepeatPenalty(config.getRepeatPenalty());
                    existing.setMaxTokens(config.getMaxTokens());
                    existing.setDescription(config.getDescription());

                    // Handle default flag
                    if (Boolean.TRUE.equals(config.getIsDefault()) && !existing.getIsDefault()) {
                        ggufModelConfigRepository.findByIsDefaultTrue().ifPresent(other -> {
                            other.setIsDefault(false);
                            ggufModelConfigRepository.save(other);
                        });
                    }
                    existing.setIsDefault(config.getIsDefault());

                    GgufModelConfig saved = ggufModelConfigRepository.save(existing);
                    log.info("Updated GGUF model config: {}", saved.getName());
                    return ResponseEntity.ok((Object) saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a GGUF model configuration
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        return ggufModelConfigRepository.findById(id)
                .map(config -> {
                    log.info("Deleting GGUF model config: {}", config.getName());
                    ggufModelConfigRepository.delete(config);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get the default GGUF model configuration
     */
    @GetMapping("/default")
    public ResponseEntity<GgufModelConfig> getDefaultConfig() {
        return ggufModelConfigRepository.findByIsDefaultTrue()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Set a configuration as default
     */
    @PatchMapping("/{id}/set-default")
    public ResponseEntity<GgufModelConfig> setDefault(@PathVariable Long id) {
        return ggufModelConfigRepository.findById(id)
                .map(config -> {
                    // Unset other defaults
                    ggufModelConfigRepository.findByIsDefaultTrue().ifPresent(existing -> {
                        existing.setIsDefault(false);
                        ggufModelConfigRepository.save(existing);
                    });

                    config.setIsDefault(true);
                    GgufModelConfig saved = ggufModelConfigRepository.save(config);
                    log.info("Set GGUF model config as default: {}", saved.getName());
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
