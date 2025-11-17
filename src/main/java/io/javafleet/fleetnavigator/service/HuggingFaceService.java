package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.dto.HuggingFaceModelInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for interacting with HuggingFace API
 */
@Slf4j
@Service
public class HuggingFaceService {

    private static final String HF_API_BASE = "https://huggingface.co/api";
    private static final String HF_MODELS_SEARCH = HF_API_BASE + "/models";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search for GGUF models on HuggingFace
     *
     * @param query Search query (e.g., "qwen", "llama", "german")
     * @param limit Maximum number of results
     * @return List of model information
     */
    public List<HuggingFaceModelInfo> searchModels(String query, int limit) {
        List<HuggingFaceModelInfo> models = new ArrayList<>();

        try {
            // Build search URL with filters for GGUF models
            String url = HF_MODELS_SEARCH +
                    "?search=" + query +
                    "&filter=gguf" +
                    "&sort=downloads" +
                    "&direction=-1" +
                    "&limit=" + limit;

            log.info("Searching HuggingFace: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonNode rootNode = objectMapper.readTree(json);

                    if (rootNode.isArray()) {
                        for (JsonNode node : rootNode) {
                            HuggingFaceModelInfo model = parseModelInfo(node);
                            if (model != null) {
                                models.add(model);
                            }
                        }
                    }
                } else {
                    log.error("HuggingFace search failed: {}", response.code());
                }
            }
        } catch (Exception e) {
            log.error("Failed to search HuggingFace models", e);
        }

        return models;
    }

    /**
     * Get detailed information about a specific model
     *
     * @param modelId Full model ID (e.g., "Qwen/Qwen2.5-3B-Instruct-GGUF")
     * @return Detailed model information
     */
    public HuggingFaceModelInfo getModelDetails(String modelId) {
        try {
            String url = HF_API_BASE + "/models/" + modelId;
            log.info("Fetching model details: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonNode node = objectMapper.readTree(json);
                    return parseModelInfo(node);
                } else {
                    log.error("Failed to fetch model details: {}", response.code());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get model details for: {}", modelId, e);
        }
        return null;
    }

    /**
     * Get model README/Card content
     *
     * @param modelId Full model ID
     * @return README markdown content
     */
    public String getModelReadme(String modelId) {
        try {
            String url = "https://huggingface.co/" + modelId + "/raw/main/README.md";
            log.info("Fetching README: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch README for: {}", modelId);
        }
        return null;
    }

    /**
     * Parse model info from JSON node
     */
    private HuggingFaceModelInfo parseModelInfo(JsonNode node) {
        try {
            HuggingFaceModelInfo.HuggingFaceModelInfoBuilder builder = HuggingFaceModelInfo.builder();

            // Basic info
            if (node.has("id")) {
                String id = node.get("id").asText();
                builder.id(id);
                builder.modelId(id);

                // Extract author and name
                String[] parts = id.split("/");
                if (parts.length == 2) {
                    builder.author(parts[0]);
                    builder.name(parts[1]);
                    builder.displayName(parts[1].replace("-GGUF", "").replace("-gguf", ""));
                }
            }

            // Dates
            if (node.has("createdAt")) {
                builder.createdAt(parseDateTime(node.get("createdAt").asText()));
            }
            if (node.has("lastModified")) {
                builder.lastModified(parseDateTime(node.get("lastModified").asText()));
            }

            // Tags and metadata
            if (node.has("tags")) {
                List<String> tags = new ArrayList<>();
                node.get("tags").forEach(tag -> tags.add(tag.asText()));
                builder.tags(tags);
            }

            if (node.has("pipeline_tag")) {
                builder.pipeline_tag(node.get("pipeline_tag").asText());
            }

            if (node.has("library_name")) {
                builder.library_name(node.get("library_name").asText());
            }

            // Stats
            if (node.has("downloads")) {
                builder.downloads(node.get("downloads").asLong());
            }
            if (node.has("likes")) {
                builder.likes(node.get("likes").asLong());
            }

            // License
            if (node.has("license")) {
                builder.license(node.get("license").asText());
            }

            // Privacy
            if (node.has("private")) {
                builder.private_model(node.get("private").asBoolean());
            }
            if (node.has("gated")) {
                builder.gated(node.get("gated").asBoolean());
            }

            // Siblings (files)
            if (node.has("siblings")) {
                List<String> siblings = new ArrayList<>();
                node.get("siblings").forEach(sibling -> {
                    if (sibling.has("rfilename")) {
                        siblings.add(sibling.get("rfilename").asText());
                    }
                });
                builder.siblings(siblings);
            }

            // Description (from cardData if available)
            if (node.has("cardData")) {
                JsonNode cardData = node.get("cardData");
                if (cardData.has("model_description")) {
                    builder.description(cardData.get("model_description").asText());
                }
                if (cardData.has("language")) {
                    List<String> languages = new ArrayList<>();
                    JsonNode langNode = cardData.get("language");
                    if (langNode.isArray()) {
                        langNode.forEach(lang -> languages.add(lang.asText()));
                    } else {
                        languages.add(langNode.asText());
                    }
                    builder.languages(languages);
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to parse model info", e);
            return null;
        }
    }

    /**
     * Parse ISO 8601 datetime string
     */
    private LocalDateTime parseDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    /**
     * Get popular GGUF models (pre-filtered for quality)
     */
    public List<HuggingFaceModelInfo> getPopularGGUFModels(int limit) {
        return searchModels("instruct", limit);
    }

    /**
     * Search specifically for German models
     */
    public List<HuggingFaceModelInfo> searchGermanModels(int limit) {
        List<HuggingFaceModelInfo> allResults = new ArrayList<>();
        allResults.addAll(searchModels("german", limit / 2));
        allResults.addAll(searchModels("deutsch", limit / 2));
        return allResults;
    }

    /**
     * Search for Instruct/Chat models
     */
    public List<HuggingFaceModelInfo> searchInstructModels(int limit) {
        List<HuggingFaceModelInfo> allResults = new ArrayList<>();
        // Search for different instruct variants
        allResults.addAll(searchModels("instruct", limit / 3));
        allResults.addAll(searchModels("chat", limit / 3));
        allResults.addAll(searchModels("assistant", limit / 3));

        // Deduplicate and sort by downloads
        return deduplicateAndSort(allResults, limit);
    }

    /**
     * Search for Code models
     */
    public List<HuggingFaceModelInfo> searchCodeModels(int limit) {
        List<HuggingFaceModelInfo> allResults = new ArrayList<>();
        allResults.addAll(searchModels("coder", limit / 2));
        allResults.addAll(searchModels("code", limit / 2));
        return deduplicateAndSort(allResults, limit);
    }

    /**
     * Search for Vision models (experimental)
     */
    public List<HuggingFaceModelInfo> searchVisionModels(int limit) {
        List<HuggingFaceModelInfo> allResults = new ArrayList<>();
        allResults.addAll(searchModels("llava", limit / 2));
        allResults.addAll(searchModels("vision", limit / 2));
        return deduplicateAndSort(allResults, limit);
    }

    /**
     * Deduplicate models by ID and sort by downloads
     */
    private List<HuggingFaceModelInfo> deduplicateAndSort(
            List<HuggingFaceModelInfo> models,
            int limit
    ) {
        // Deduplicate by modelId
        Map<String, HuggingFaceModelInfo> uniqueModels = new LinkedHashMap<>();
        for (HuggingFaceModelInfo model : models) {
            uniqueModels.putIfAbsent(model.getModelId(), model);
        }

        // Sort by downloads (descending)
        return uniqueModels.values().stream()
                .sorted(Comparator.comparingLong(HuggingFaceModelInfo::getDownloads).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
