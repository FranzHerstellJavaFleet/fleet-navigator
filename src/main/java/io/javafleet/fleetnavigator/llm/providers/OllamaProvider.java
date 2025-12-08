package io.javafleet.fleetnavigator.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.service.OllamaService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Ollama Provider Implementation
 * Wraps OllamaService and implements LLMProvider interface
 *
 * @author JavaFleet Systems Consulting
 * @since 0.3.2
 */
@Component
@Slf4j
public class OllamaProvider implements LLMProvider {

    private final OllamaService ollamaService;
    private final boolean enabled;
    private final String ollamaBaseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaProvider(OllamaService ollamaService, LLMConfigProperties config) {
        this.ollamaService = ollamaService;
        this.enabled = config.getOllama() != null && config.getOllama().isEnabled();
        this.ollamaBaseUrl = config.getOllama() != null ? config.getOllama().getBaseUrl() : "http://localhost:11434";
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();

        if (enabled) {
            log.info("🔮 OllamaProvider initialized and enabled");
        } else {
            log.info("🔮 OllamaProvider initialized but disabled in config");
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        return ollamaService.isOllamaAvailable();
    }

    @Override
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        if (!enabled) {
            throw new UnsupportedOperationException("Ollama provider is disabled in configuration");
        }
        return ollamaService.chat(model, prompt, systemPrompt, requestId);
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt, String requestId,
                          Consumer<String> chunkConsumer, Integer maxTokens, Double temperature,
                          Double topP, Integer topK, Double repeatPenalty, Integer numCtx) throws IOException {
        // Delegate to full method with cpuOnly=false (default)
        chatStream(model, prompt, systemPrompt, requestId, chunkConsumer,
                maxTokens, temperature, topP, topK, repeatPenalty, numCtx, false);
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt, String requestId,
                          Consumer<String> chunkConsumer, Integer maxTokens, Double temperature,
                          Double topP, Integer topK, Double repeatPenalty, Integer numCtx,
                          Boolean cpuOnly) throws IOException {
        if (!enabled) {
            throw new UnsupportedOperationException("Ollama provider is disabled in configuration");
        }
        ollamaService.chatStream(model, prompt, systemPrompt, requestId, chunkConsumer,
                maxTokens, temperature, topP, topK, repeatPenalty, numCtx, cpuOnly);
    }

    @Override
    public String chatWithVision(String model, String prompt, List<String> images,
                                 String systemPrompt, String requestId) throws IOException {
        if (!enabled) {
            throw new UnsupportedOperationException("Ollama provider is disabled in configuration");
        }
        return ollamaService.chatWithVision(model, prompt, images, systemPrompt, requestId);
    }

    @Override
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                     String systemPrompt, String requestId,
                                     Consumer<String> chunkConsumer) throws IOException {
        if (!enabled) {
            throw new UnsupportedOperationException("Ollama provider is disabled in configuration");
        }
        ollamaService.chatStreamWithVision(model, prompt, images, systemPrompt, requestId, chunkConsumer);
    }

    @Override
    public List<ModelInfo> getAvailableModels() throws IOException {
        if (!enabled) {
            return Collections.emptyList();
        }

        String url = ollamaBaseUrl + "/api/tags";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Ollama models: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode modelsArray = jsonNode.get("models");

            List<ModelInfo> models = new ArrayList<>();

            if (modelsArray != null && modelsArray.isArray()) {
                for (JsonNode modelNode : modelsArray) {
                    String modelName = modelNode.get("name").asText();
                    long sizeBytes = modelNode.has("size") ? modelNode.get("size").asLong() : 0;
                    String modifiedAt = modelNode.has("modified_at") ? modelNode.get("modified_at").asText() : null;
                    String digest = modelNode.has("digest") ? modelNode.get("digest").asText() : null;

                    // Extract details
                    JsonNode details = modelNode.get("details");
                    String parentModel = null;
                    String format = null;
                    String family = null;
                    String parameterSize = null;
                    String quantization = null;

                    if (details != null) {
                        parentModel = details.has("parent_model") ? details.get("parent_model").asText() : null;
                        format = details.has("format") ? details.get("format").asText() : null;
                        family = details.has("family") ? details.get("family").asText() : null;
                        parameterSize = details.has("parameter_size") ? details.get("parameter_size").asText() : null;
                        quantization = details.has("quantization_level") ? details.get("quantization_level").asText() : null;
                    }

                    // Check if this is a custom model
                    // Detection Strategy:
                    // 1. Has non-empty parent_model (new custom models)
                    // 2. Has custom naming pattern (doesn't match standard Ollama library patterns)
                    boolean hasParentModel = (parentModel != null && !parentModel.isEmpty());
                    boolean hasCustomName = isCustomModelName(modelName);
                    boolean isCustom = hasParentModel || hasCustomName;

                    // Build description from available info
                    StringBuilder description = new StringBuilder();
                    if (family != null) {
                        description.append("Familie: ").append(family);
                    }
                    if (parameterSize != null) {
                        if (description.length() > 0) description.append(" • ");
                        description.append("Parameter: ").append(parameterSize);
                    }
                    if (quantization != null) {
                        if (description.length() > 0) description.append(" • ");
                        description.append("Quantisierung: ").append(quantization);
                    }
                    if (format != null) {
                        if (description.length() > 0) description.append(" • ");
                        description.append("Format: ").append(format);
                    }

                    ModelInfo modelInfo = ModelInfo.builder()
                        .name(modelName)
                        .displayName(modelName)
                        .provider("ollama")
                        .size(sizeBytes)
                        .sizeHuman(formatBytes(sizeBytes))
                        .architecture(family)
                        .quantization(quantization)
                        .description(description.length() > 0 ? description.toString() : null)
                        .modifiedAt(modifiedAt)
                        .digest(digest)
                        .custom(isCustom)
                        .installed(true)
                        .build();

                    models.add(modelInfo);

                    if (isCustom) {
                        log.debug("Detected custom model: {} (parent: {})", modelName, parentModel);
                    }
                }
            }

            log.info("Loaded {} Ollama models ({} custom)",
                models.size(),
                models.stream().filter(ModelInfo::isCustom).count());

            return models;
        }
    }

    /**
     * Format bytes to human readable string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Detects if a model name indicates a custom/user-created model
     *
     * Custom models typically have:
     * - Single word names (e.g., "katharina", "cassian", "mymodel")
     * - Unusual naming patterns not matching Ollama library
     *
     * Standard Ollama models follow patterns like:
     * - llama3.2:3b, qwen2.5:32b, mistral:7b-instruct
     * - modelname:tag or modelname:version
     */
    private boolean isCustomModelName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Known Ollama library model prefixes (expanded list)
        String[] knownModels = {
            "llama", "mistral", "qwen", "gemma", "phi", "codellama",
            "deepseek", "llava", "vicuna", "orca", "falcon", "wizardlm",
            "neural-chat", "starling", "openchat", "dolphin", "solar",
            "yi", "mixtral", "nous-hermes", "zephyr", "tinyllama",
            "command-r", "granite", "gemma2", "llama2", "llama3",
            "nomic-embed", "all-minilm", "mxbai-embed", "snowflake-arctic",
            "bakllava", "codegemma", "mathstral", "nemotron", "wizard-vicuna"
        };

        String lowerName = name.toLowerCase();

        // Check if it starts with any known model prefix
        for (String known : knownModels) {
            if (lowerName.startsWith(known)) {
                return false; // It's a standard model
            }
        }

        // Extract base name (before colon) for analysis
        // e.g., "cassian:latest" -> "cassian", "nous-hermes:7b" -> "nous-hermes"
        String baseName = name.contains(":") ? name.substring(0, name.indexOf(":")) : name;
        String baseNameLower = baseName.toLowerCase();

        // Check if base name is in known models
        for (String known : knownModels) {
            if (baseNameLower.startsWith(known)) {
                return false; // It's a standard model
            }
        }

        // Mark as custom if it's a simple name without dashes or numbers
        // This catches: "katharina:latest", "cassian:latest", "mymodel", "nova:latest"
        // But not: "nous-hermes:latest", "llama3.2:7b", "qwen2.5-coder:14b"
        if (!baseName.contains("-") && !baseName.matches(".*\\d+.*")) {
            return true; // Likely custom
        }

        // Default: NOT custom (conservative approach - avoid false positives)
        return false;
    }

    @Override
    public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
        if (!enabled) {
            throw new UnsupportedOperationException("Ollama provider is disabled in configuration");
        }
        ollamaService.pullModel(modelName, progressConsumer);
    }

    @Override
    public boolean deleteModel(String modelName) throws IOException {
        if (!enabled) {
            throw new UnsupportedOperationException("Ollama provider is disabled in configuration");
        }
        return ollamaService.deleteModel(modelName);
    }

    @Override
    public Map<String, Object> getModelDetails(String modelName) throws IOException {
        if (!enabled) {
            return Collections.emptyMap();
        }
        return ollamaService.getModelDetails(modelName);
    }

    @Override
    public void createModel(String modelName, String baseModel, String systemPrompt,
                           Double temperature, Double topP, Integer topK,
                           Double repeatPenalty, Consumer<String> progressConsumer) throws IOException {
        throw new UnsupportedOperationException("Custom model creation not yet implemented for Ollama");
    }

    @Override
    public boolean cancelRequest(String requestId) {
        if (!enabled) {
            return false;
        }
        return ollamaService.cancelRequest(requestId);
    }

    @Override
    public int estimateTokens(String text) {
        return ollamaService.estimateTokens(text);
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        if (!enabled) {
            return Collections.emptySet();
        }

        return Set.of(
            ProviderFeature.STREAMING,
            ProviderFeature.VISION,
            ProviderFeature.LIST_MODELS,
            ProviderFeature.PULL_MODEL,
            ProviderFeature.DELETE_MODEL,
            ProviderFeature.MODEL_DETAILS
        );
    }
}
