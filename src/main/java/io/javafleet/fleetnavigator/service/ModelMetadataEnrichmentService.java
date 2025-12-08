package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to enrich model information with metadata from curated database
 */
@Service
@Slf4j
public class ModelMetadataEnrichmentService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Map<String, Object>> metadataCache = new HashMap<>();

    @PostConstruct
    public void loadMetadata() {
        try {
            ClassPathResource resource = new ClassPathResource("model-metadata.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            JsonNode models = root.get("models");

            if (models != null && models.isArray()) {
                for (JsonNode model : models) {
                    String name = model.get("name").asText();
                    Map<String, Object> metadata = new HashMap<>();

                    // Basic info
                    metadata.put("publisher", getStringOrNull(model, "publisher"));
                    metadata.put("releaseDate", getStringOrNull(model, "releaseDate"));
                    metadata.put("trainedUntil", getStringOrNull(model, "trainedUntil"));
                    metadata.put("contextWindow", getIntOrNull(model, "contextWindow"));
                    metadata.put("license", getStringOrNull(model, "license"));
                    metadata.put("description", getStringOrNull(model, "description"));

                    // Tasks and capabilities
                    metadata.put("primaryTasks", getArrayAsString(model, "primaryTasks"));
                    metadata.put("strengths", getArrayAsString(model, "strengths"));
                    metadata.put("languages", getArrayAsString(model, "languages"));

                    // Benchmarks
                    if (model.has("benchmarks")) {
                        JsonNode benchmarks = model.get("benchmarks");
                        Map<String, String> benchmarkMap = new HashMap<>();
                        benchmarks.fields().forEachRemaining(entry ->
                            benchmarkMap.put(entry.getKey(), entry.getValue().asText())
                        );
                        metadata.put("benchmarks", benchmarkMap);
                    }

                    metadataCache.put(name, metadata);
                    log.debug("Loaded metadata for model: {}", name);
                }
            }

            log.info("Loaded metadata for {} models", metadataCache.size());
        } catch (IOException e) {
            log.error("Failed to load model metadata", e);
        }
    }

    /**
     * Enrich model info map with metadata
     */
    public void enrichModelInfo(Map<String, Object> modelInfo) {
        String modelName = (String) modelInfo.get("name");
        if (modelName == null) return;

        // Extract base model name (e.g., "qwen2.5-coder:7b" -> "qwen2.5-coder")
        String baseName = extractBaseName(modelName);

        Map<String, Object> metadata = metadataCache.get(baseName);
        if (metadata != null) {
            modelInfo.putAll(metadata);
            log.debug("Enriched model {} with metadata", modelName);
        } else {
            log.debug("No metadata found for model: {} (base: {})", modelName, baseName);
        }
    }

    /**
     * Extract base model name without version/variant
     */
    private String extractBaseName(String fullName) {
        // Remove tag (e.g., ":7b", ":latest")
        String baseName = fullName.split(":")[0];

        // Handle special cases - order matters (more specific first)
        if (baseName.startsWith("qwen2.5-coder") || baseName.startsWith("qwen2-coder")) {
            return "qwen2.5-coder";
        }
        if (baseName.startsWith("codeqwen")) {
            return "codeqwen";
        }
        if (baseName.startsWith("llama3.3")) {
            return "llama3.3";
        }
        if (baseName.startsWith("llama3.1")) {
            return "llama3.1";
        }
        if (baseName.startsWith("llama2")) {
            return "llama2";
        }
        if (baseName.startsWith("codellama")) {
            return "codellama";
        }
        if (baseName.startsWith("deepseek-coder-v2")) {
            return "deepseek-coder-v2";
        }
        if (baseName.startsWith("deepseek-coder")) {
            return "deepseek-coder";
        }
        // New code models
        if (baseName.startsWith("magicoder")) {
            return "magicoder";
        }
        if (baseName.startsWith("starcoder2")) {
            return "starcoder2";
        }
        if (baseName.startsWith("starcoder")) {
            return "starcoder";
        }
        if (baseName.startsWith("wizardcoder")) {
            return "wizardcoder";
        }
        if (baseName.startsWith("codestral")) {
            return "codestral";
        }
        if (baseName.startsWith("codegemma")) {
            return "codegemma";
        }
        if (baseName.startsWith("granite-code")) {
            return "granite-code";
        }
        if (baseName.startsWith("stable-code")) {
            return "stable-code";
        }
        if (baseName.startsWith("yi-coder")) {
            return "yi-coder";
        }

        return baseName;
    }

    private String getStringOrNull(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : null;
    }

    private Integer getIntOrNull(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asInt() : null;
    }

    private String getArrayAsString(JsonNode node, String field) {
        if (!node.has(field)) return null;
        JsonNode array = node.get(field);
        if (!array.isArray()) return null;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) result.append(", ");
            result.append(array.get(i).asText());
        }
        return result.toString();
    }
}
