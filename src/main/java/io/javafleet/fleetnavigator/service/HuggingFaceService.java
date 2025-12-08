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

            // GGUF metadata (includes file size!)
            if (node.has("gguf")) {
                JsonNode ggufNode = node.get("gguf");
                // Total size in bytes
                if (ggufNode.has("total")) {
                    builder.modelSize(ggufNode.get("total").asLong());
                }
                // Also get context length if available
                if (ggufNode.has("context_length")) {
                    // Could be used for context window info
                    log.debug("GGUF context_length: {}", ggufNode.get("context_length").asLong());
                }
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

            // Estimate model size from name if not available from API
            HuggingFaceModelInfo model = builder.build();
            if (model.getModelSize() == null && model.getName() != null) {
                Long estimatedSize = estimateModelSize(model.getName());
                if (estimatedSize != null) {
                    model.setModelSize(estimatedSize);
                }
            }

            return model;
        } catch (Exception e) {
            log.error("Failed to parse model info", e);
            return null;
        }
    }

    /**
     * Estimate model size based on model name (parameter count + quantization)
     *
     * Formula: ~0.5-1.0 GB per 1B parameters for Q4_K_M
     * Q8_0 is about 2x Q4_K_M
     * Q2_K is about 0.5x Q4_K_M
     * Q5_K_M is about 1.25x Q4_K_M
     */
    private Long estimateModelSize(String modelName) {
        String nameLower = modelName.toLowerCase();

        // Extract parameter count (e.g., "7B", "3B", "1.5B", "70B")
        // Order matters - check larger sizes first to avoid partial matches
        Double paramBillions = null;

        // Named models (Large, Small, etc.) - check these first
        if (nameLower.contains("mistral-large") || nameLower.contains("mistral_large")) {
            // Mistral Large 2411 is 123B
            paramBillions = 123.0;
        } else if (nameLower.contains("mistral-small") || nameLower.contains("mistral_small")) {
            // Mistral Small is 22B (or 24B in newer versions)
            paramBillions = 22.0;
        } else if (nameLower.contains("mistral-medium") || nameLower.contains("mistral_medium")) {
            paramBillions = 70.0;
        }
        // Explicit parameter counts - order from largest to smallest
        else if (nameLower.contains("123b") || nameLower.contains("-123b")) {
            paramBillions = 123.0;
        } else if (nameLower.contains("110b") || nameLower.contains("-110b")) {
            paramBillions = 110.0;
        } else if (nameLower.contains("72b") || nameLower.contains("-72b")) {
            paramBillions = 72.0;
        } else if (nameLower.contains("70b") || nameLower.contains("-70b")) {
            paramBillions = 70.0;
        } else if (nameLower.contains("65b") || nameLower.contains("-65b")) {
            paramBillions = 65.0;
        } else if (nameLower.contains("34b") || nameLower.contains("-34b")) {
            paramBillions = 34.0;
        } else if (nameLower.contains("32b") || nameLower.contains("-32b")) {
            paramBillions = 32.0;
        } else if (nameLower.contains("27b") || nameLower.contains("-27b")) {
            paramBillions = 27.0;
        } else if (nameLower.contains("24b") || nameLower.contains("-24b")) {
            paramBillions = 24.0;
        } else if (nameLower.contains("22b") || nameLower.contains("-22b")) {
            paramBillions = 22.0;
        } else if (nameLower.contains("20b") || nameLower.contains("-20b")) {
            paramBillions = 20.0;
        } else if (nameLower.contains("14b") || nameLower.contains("-14b")) {
            paramBillions = 14.0;
        } else if (nameLower.contains("13b") || nameLower.contains("-13b")) {
            paramBillions = 13.0;
        } else if (nameLower.contains("12b") || nameLower.contains("-12b") || nameLower.contains("nemo")) {
            // Mistral Nemo is 12B
            paramBillions = 12.0;
        } else if (nameLower.contains("11b") || nameLower.contains("-11b")) {
            paramBillions = 11.0;
        } else if (nameLower.contains("9b") || nameLower.contains("-9b")) {
            paramBillions = 9.0;
        } else if (nameLower.contains("8b") || nameLower.contains("-8b")) {
            paramBillions = 8.0;
        } else if (nameLower.contains("7b") || nameLower.contains("-7b")) {
            paramBillions = 7.0;
        } else if (nameLower.contains("6.7b") || nameLower.contains("-6.7b")) {
            paramBillions = 6.7;
        } else if (nameLower.contains("4b") || nameLower.contains("-4b")) {
            paramBillions = 4.0;
        } else if (nameLower.contains("3.8b") || nameLower.contains("-3.8b") || nameLower.contains("phi-3.5-mini") || nameLower.contains("phi3.5-mini")) {
            // Phi-3.5-mini is 3.8B
            paramBillions = 3.8;
        } else if (nameLower.contains("3b") || nameLower.contains("-3b")) {
            paramBillions = 3.0;
        } else if (nameLower.contains("2.7b") || nameLower.contains("-2.7b")) {
            paramBillions = 2.7;
        } else if (nameLower.contains("2b") || nameLower.contains("-2b")) {
            paramBillions = 2.0;
        } else if (nameLower.contains("1.5b") || nameLower.contains("-1.5b")) {
            paramBillions = 1.5;
        } else if (nameLower.contains("1b") || nameLower.contains("-1b")) {
            paramBillions = 1.0;
        } else if (nameLower.contains("0.5b") || nameLower.contains("-0.5b") || nameLower.contains("500m")) {
            paramBillions = 0.5;
        }

        if (paramBillions == null) {
            return null;
        }

        // Base size: ~0.6 GB per 1B params for Q4_K_M
        double baseSizeGB = paramBillions * 0.6;

        // Adjust for quantization
        double quantMultiplier = 1.0;
        if (nameLower.contains("q8_0") || nameLower.contains("q8-0") || nameLower.contains("_q8")) {
            quantMultiplier = 1.8;  // Q8 is larger
        } else if (nameLower.contains("q5_k") || nameLower.contains("q5-k")) {
            quantMultiplier = 1.25;
        } else if (nameLower.contains("q6_k") || nameLower.contains("q6-k")) {
            quantMultiplier = 1.5;
        } else if (nameLower.contains("q2_k") || nameLower.contains("q2-k")) {
            quantMultiplier = 0.5;
        } else if (nameLower.contains("q3_k") || nameLower.contains("q3-k")) {
            quantMultiplier = 0.65;
        } else if (nameLower.contains("iq4_xs") || nameLower.contains("iq4-xs")) {
            quantMultiplier = 0.85;
        } else if (nameLower.contains("iq2") || nameLower.contains("iq1")) {
            quantMultiplier = 0.4;
        }
        // Q4_K_M is the baseline (multiplier = 1.0)

        double estimatedGB = baseSizeGB * quantMultiplier;
        long estimatedBytes = (long) (estimatedGB * 1024 * 1024 * 1024);

        log.debug("Estimated size for {}: {:.2f} GB", modelName, estimatedGB);
        return estimatedBytes;
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
