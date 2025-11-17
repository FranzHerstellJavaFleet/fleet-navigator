package io.javafleet.fleetnavigator.llm.providers;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.model.GgufModelConfig;
import io.javafleet.fleetnavigator.repository.GgufModelConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LLM Provider using java-llama.cpp JNI binding.
 * Runs llama.cpp models directly in-process via native code.
 *
 * Advantages over server-based approach:
 * - No external process management
 * - Direct memory access (faster)
 * - Automatic native memory cleanup
 * - Simple API
 */
@Slf4j
@Component
public class JavaLlamaCppProvider implements LLMProvider {

    private static final Set<ProviderFeature> SUPPORTED_FEATURES = EnumSet.of(
        ProviderFeature.STREAMING,
        ProviderFeature.BLOCKING,
        ProviderFeature.LIST_MODELS,
        ProviderFeature.DYNAMIC_CONTEXT_SIZE,
        ProviderFeature.GPU_ACCELERATION
    );

    private final LLMConfigProperties config;
    private final GgufModelConfigRepository ggufModelConfigRepository;
    private final Map<String, LlamaModel> loadedModels = new HashMap<>();
    private final Set<String> activeRequests = Collections.synchronizedSet(new HashSet<>());

    public JavaLlamaCppProvider(LLMConfigProperties config, GgufModelConfigRepository ggufModelConfigRepository) {
        this.config = config;
        this.ggufModelConfigRepository = ggufModelConfigRepository;
        log.info("🦙 JavaLlamaCppProvider initialized (JNI-based)");
    }

    @Override
    public boolean supportsFeature(ProviderFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        return EnumSet.copyOf(SUPPORTED_FEATURES);
    }

    @Override
    public String getProviderName() {
        return "java-llama-cpp";
    }

    @Override
    public boolean isAvailable() {
        // Check if models directory exists
        Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());
        return Files.exists(modelsDir) && Files.isDirectory(modelsDir);
    }

    @Override
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        log.info("Starting non-streaming generation with model: {}", model);

        StringBuilder fullResponse = new StringBuilder();
        chatStream(model, prompt, systemPrompt, requestId,
                   fullResponse::append, null, null, null, null, null);

        return fullResponse.toString();
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature,
                           Double topP, Integer topK, Double repeatPenalty) throws IOException {

        log.info("Starting streaming generation with model: {}", model);

        // Check for custom configuration - use its system prompt if present
        Optional<GgufModelConfig> customConfig = ggufModelConfigRepository.findByName(model);
        String effectiveSystemPrompt = systemPrompt;

        if (customConfig.isPresent() && customConfig.get().getSystemPrompt() != null && !customConfig.get().getSystemPrompt().isEmpty()) {
            effectiveSystemPrompt = customConfig.get().getSystemPrompt();
            log.info("🎨 Using custom system prompt from config for: {}", model);
        }

        log.info("System prompt: {}", effectiveSystemPrompt != null ? effectiveSystemPrompt.substring(0, Math.min(100, effectiveSystemPrompt.length())) + "..." : "null");
        log.info("User prompt: {}", prompt != null ? prompt.substring(0, Math.min(100, prompt.length())) + "..." : "null");
        activeRequests.add(requestId);

        try {
            // Get or load model
            LlamaModel llamaModel = getOrLoadModel(model);

            // Build full prompt with system message
            String fullPrompt = buildPrompt(effectiveSystemPrompt, prompt);
            log.debug("Full prompt length: {} characters", fullPrompt.length());

            // Configure inference parameters
            // Priority: method params > custom config > defaults
            InferenceParameters params = new InferenceParameters(fullPrompt);

            // Temperature
            if (temperature != null) {
                params.setTemperature(temperature.floatValue());
            } else if (customConfig.isPresent() && customConfig.get().getTemperature() != null) {
                params.setTemperature(customConfig.get().getTemperature().floatValue());
            } else {
                params.setTemperature(0.7f);
            }

            params.setPenalizeNl(true);

            // Max tokens
            if (maxTokens != null && maxTokens > 0) {
                params.setNPredict(maxTokens);
            } else if (customConfig.isPresent() && customConfig.get().getMaxTokens() != null) {
                params.setNPredict(customConfig.get().getMaxTokens());
            }

            // Top-P
            if (topP != null) {
                params.setTopP(topP.floatValue());
            } else if (customConfig.isPresent() && customConfig.get().getTopP() != null) {
                params.setTopP(customConfig.get().getTopP().floatValue());
            }

            // Top-K
            if (topK != null) {
                params.setTopK(topK);
            } else if (customConfig.isPresent() && customConfig.get().getTopK() != null) {
                params.setTopK(customConfig.get().getTopK());
            }

            // Repeat penalty
            if (repeatPenalty != null) {
                params.setRepeatPenalty(repeatPenalty.floatValue());
            } else if (customConfig.isPresent() && customConfig.get().getRepeatPenalty() != null) {
                params.setRepeatPenalty(customConfig.get().getRepeatPenalty().floatValue());
            }

            // ========== Additional Advanced Parameters from Custom Config ==========

            if (customConfig.isPresent()) {
                GgufModelConfig cfg = customConfig.get();

                // Mirostat Sampling (not yet supported by java-llama-cpp)
                // TODO: Add when library supports these methods
                // if (cfg.getMirostat() != null) {
                //     params.setMirostat(cfg.getMirostat());
                // }
                // if (cfg.getMirostatTau() != null) {
                //     params.setMirostatTau(cfg.getMirostatTau().floatValue());
                // }
                // if (cfg.getMirostatEta() != null) {
                //     params.setMirostatEta(cfg.getMirostatEta().floatValue());
                // }

                // TFS (Tail Free Sampling)
                if (cfg.getTfsZ() != null) {
                    params.setTfsZ(cfg.getTfsZ().floatValue());
                }

                // Typical P
                if (cfg.getTypicalP() != null) {
                    params.setTypicalP(cfg.getTypicalP().floatValue());
                }

                // Presence & Frequency Penalty
                if (cfg.getPresencePenalty() != null) {
                    params.setPresencePenalty(cfg.getPresencePenalty().floatValue());
                }
                if (cfg.getFrequencyPenalty() != null) {
                    params.setFrequencyPenalty(cfg.getFrequencyPenalty().floatValue());
                }

                // Min-P Sampling
                if (cfg.getMinP() != null) {
                    params.setMinP(cfg.getMinP().floatValue());
                }

                // Seed for reproducibility
                if (cfg.getSeed() != null && cfg.getSeed() >= 0) {
                    params.setSeed(cfg.getSeed().intValue());
                }

                // Stop sequences (if stored as comma-separated or newline-separated)
                if (cfg.getStopSequences() != null && !cfg.getStopSequences().isEmpty()) {
                    String[] stopSeqs = cfg.getStopSequences().split("[,\\n]");
                    for (String seq : stopSeqs) {
                        String trimmed = seq.trim();
                        if (!trimmed.isEmpty()) {
                            params.setStopStrings(trimmed);
                        }
                    }
                }

                log.debug("Applied advanced parameters from custom config for model: {}", model);
            }

            // Stream generation
            for (LlamaOutput output : llamaModel.generate(params)) {
                // Check if request was cancelled
                if (!activeRequests.contains(requestId)) {
                    log.info("Request {} was cancelled", requestId);
                    break;
                }

                String chunk = output.toString();
                chunkConsumer.accept(chunk);
            }
        } catch (Exception e) {
            log.error("Error during streaming generation", e);
            throw new IOException("Streaming generation failed: " + e.getMessage(), e);
        } finally {
            activeRequests.remove(requestId);
        }
    }

    @Override
    public String chatWithVision(String model, String prompt, List<String> images,
                                  String systemPrompt, String requestId) throws IOException {
        throw new UnsupportedOperationException("Vision models not yet supported by java-llama-cpp provider");
    }

    @Override
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                      String systemPrompt, String requestId,
                                      Consumer<String> chunkConsumer) throws IOException {
        throw new UnsupportedOperationException("Vision models not yet supported by java-llama-cpp provider");
    }

    @Override
    public List<ModelInfo> getAvailableModels() throws IOException {
        List<ModelInfo> models = new ArrayList<>();
        Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());

        if (!Files.exists(modelsDir)) {
            return models;
        }

        // Search in library/ and custom/ subdirectories
        List<Path> searchPaths = Arrays.asList(
            modelsDir.resolve("library"),
            modelsDir.resolve("custom"),
            modelsDir
        );

        for (Path searchPath : searchPaths) {
            if (Files.exists(searchPath) && Files.isDirectory(searchPath)) {
                try (Stream<Path> files = Files.walk(searchPath, 1)) {
                    files.filter(p -> p.toString().endsWith(".gguf"))
                         .forEach(p -> {
                             String filename = p.getFileName().toString();
                             ModelInfo info = new ModelInfo();
                             info.setName(filename);
                             info.setProvider("java-llama-cpp");
                             info.setSize(getFileSize(p));
                             info.setModifiedAt(getModifiedDate(p));
                             models.add(info);
                         });
                } catch (IOException e) {
                    log.warn("Failed to scan directory: {}", searchPath, e);
                }
            }
        }

        return models;
    }

    @Override
    public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
        throw new UnsupportedOperationException("Model pulling not supported by java-llama-cpp. Please copy GGUF files manually to " + config.getLlamacpp().getModelsDir());
    }

    @Override
    public boolean deleteModel(String modelName) throws IOException {
        Path modelPath = resolveModelPath(modelName);

        if (Files.exists(modelPath)) {
            // Close model if loaded
            if (loadedModels.containsKey(modelName)) {
                try {
                    loadedModels.get(modelName).close();
                    loadedModels.remove(modelName);
                } catch (Exception e) {
                    log.warn("Failed to close model before deletion: {}", modelName, e);
                }
            }

            Files.delete(modelPath);
            log.info("Deleted model: {}", modelName);
            return true;
        }

        return false;
    }

    @Override
    public Map<String, Object> getModelDetails(String modelName) throws IOException {
        Map<String, Object> details = new HashMap<>();
        Path modelPath = resolveModelPath(modelName);

        if (!Files.exists(modelPath)) {
            throw new IOException("Model not found: " + modelName);
        }

        details.put("name", modelName);
        details.put("path", modelPath.toString());
        details.put("size", Files.size(modelPath));
        details.put("provider", "java-llama-cpp");
        details.put("format", "GGUF");

        return details;
    }

    @Override
    public boolean cancelRequest(String requestId) {
        if (activeRequests.contains(requestId)) {
            activeRequests.remove(requestId);
            log.info("Cancelled request: {}", requestId);
            return true;
        }
        return false;
    }

    /**
     * Get or load a model (with caching)
     * Checks for custom GGUF model configuration first, falls back to default config
     */
    private LlamaModel getOrLoadModel(String modelName) throws IOException {
        // Check cache
        if (loadedModels.containsKey(modelName)) {
            log.debug("Using cached model: {}", modelName);
            return loadedModels.get(modelName);
        }

        // Check if there's a custom configuration for this model
        Optional<GgufModelConfig> customConfig = ggufModelConfigRepository.findByName(modelName);

        Path modelPath;
        int contextSize;
        int gpuLayers;

        if (customConfig.isPresent()) {
            GgufModelConfig cfg = customConfig.get();
            log.info("🎨 Found custom config for model: {} (context: {}K)", modelName, cfg.getContextSize() / 1024);

            // Use base model file from config
            modelPath = resolveModelPath(cfg.getBaseModel());
            contextSize = cfg.getContextSize();
            gpuLayers = cfg.getGpuLayers();
        } else {
            // Use default config
            modelPath = resolveModelPath(modelName);
            contextSize = config.getLlamacpp().getContextSize();
            gpuLayers = config.getLlamacpp().getGpuLayers();
        }

        if (!Files.exists(modelPath)) {
            log.error("❌ Model file not found at resolved path: {}", modelPath.toAbsolutePath());
            log.error("❌ Model name requested: {}", modelName);
            log.error("❌ Models directory: {}", config.getLlamacpp().getModelsDir());
            throw new IOException("Model file not found: " + modelPath.toAbsolutePath());
        }

        log.info("✅ Loading model: {} from path: {} (context: {}K, GPU layers: {})",
                 modelName, modelPath.toAbsolutePath(), contextSize / 1024, gpuLayers);

        // Configure model parameters using fluent API
        ModelParameters modelParams = new ModelParameters()
                .setModel(modelPath.toString())
                .setGpuLayers(gpuLayers)
                .setCtxSize(contextSize);

        // Apply threads configuration
        if (customConfig.isPresent() && customConfig.get().getThreads() != null && customConfig.get().getThreads() > 0) {
            modelParams.setThreads(customConfig.get().getThreads());
            log.debug("Using custom threads: {}", customConfig.get().getThreads());
        } else if (config.getLlamacpp().getThreads() > 0) {
            modelParams.setThreads(config.getLlamacpp().getThreads());
        }

        // Apply batch size configuration
        if (customConfig.isPresent() && customConfig.get().getBatchSize() != null && customConfig.get().getBatchSize() > 0) {
            modelParams.setBatchSize(customConfig.get().getBatchSize());
            log.debug("Using custom batch size: {}", customConfig.get().getBatchSize());
        }

        // Apply RoPE scaling configuration
        if (customConfig.isPresent()) {
            GgufModelConfig cfg = customConfig.get();

            if (cfg.getRopeFreqBase() != null && cfg.getRopeFreqBase() > 0) {
                modelParams.setRopeFreqBase(cfg.getRopeFreqBase().floatValue());
                log.debug("Using custom RoPE freq base: {}", cfg.getRopeFreqBase());
            }

            if (cfg.getRopeFreqScale() != null && cfg.getRopeFreqScale() > 0) {
                modelParams.setRopeFreqScale(cfg.getRopeFreqScale().floatValue());
                log.debug("Using custom RoPE freq scale: {}", cfg.getRopeFreqScale());
            }

            // Apply flash attention if enabled (not yet supported)
            // TODO: Add when library supports these methods
            // if (cfg.getFlashAttention() != null && cfg.getFlashAttention()) {
            //     modelParams.setFlashAttention(true);
            //     log.debug("Flash Attention enabled");
            // }

            // Apply performance hints (not yet supported by java-llama-cpp)
            // TODO: Add when library supports these methods
            // if (cfg.getLowVram() != null && cfg.getLowVram()) {
            //     modelParams.setLowVram(true);
            //     log.debug("Low VRAM mode enabled");
            // }

            // if (cfg.getMmapEnabled() != null) {
            //     modelParams.setMmap(cfg.getMmapEnabled());
            //     log.debug("Memory mapping: {}", cfg.getMmapEnabled());
            // }

            // if (cfg.getMlockEnabled() != null && cfg.getMlockEnabled()) {
            //     modelParams.setMlock(true);
            //     log.debug("Memory lock (mlock) enabled");
            // }
        }

        // Load model
        try {
            LlamaModel model = new LlamaModel(modelParams);
            loadedModels.put(modelName, model);
            log.info("✅ Model loaded successfully: {} (context: {}K)", modelName, contextSize / 1024);
            return model;
        } catch (Exception e) {
            log.error("Failed to load model: {}", modelName, e);
            throw new IOException("Failed to load model: " + e.getMessage(), e);
        }
    }

    /**
     * Resolve model file path from model name
     */
    private Path resolveModelPath(String modelName) {
        Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());

        // Try different locations
        Path[] candidates = {
                modelsDir.resolve(modelName),
                modelsDir.resolve("library").resolve(modelName),
                modelsDir.resolve("custom").resolve(modelName)
        };

        log.debug("🔍 Resolving model path for: {}", modelName);
        log.debug("🔍 Models directory: {}", modelsDir.toAbsolutePath());

        for (Path candidate : candidates) {
            log.debug("🔍 Trying: {}", candidate.toAbsolutePath());
            if (Files.exists(candidate)) {
                log.info("✅ Found model at: {}", candidate.toAbsolutePath());
                return candidate;
            }
        }

        // Default: assume it's in library/
        Path defaultPath = modelsDir.resolve("library").resolve(modelName);
        log.warn("⚠️ Model not found in any candidate location, using default: {}", defaultPath.toAbsolutePath());
        return defaultPath;
    }

    /**
     * Build prompt with system message
     */
    private String buildPrompt(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return userPrompt;
        }

        // ChatML format for most instruct models
        return String.format(
                "<|im_start|>system\n%s<|im_end|>\n<|im_start|>user\n%s<|im_end|>\n<|im_start|>assistant\n",
                systemPrompt,
                userPrompt
        );
    }

    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private String getModifiedDate(Path path) {
        try {
            return Files.getLastModifiedTime(path).toString();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Clean up loaded models
     */
    public void shutdown() {
        log.info("Shutting down JavaLlamaCppProvider, closing {} loaded models", loadedModels.size());
        for (Map.Entry<String, LlamaModel> entry : loadedModels.entrySet()) {
            try {
                entry.getValue().close();
                log.info("Closed model: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Error closing model: {}", entry.getKey(), e);
            }
        }
        loadedModels.clear();
    }
}
