package io.javafleet.fleetnavigator.llm.providers;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ModelMappingService;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.model.GgufModelConfig;
import io.javafleet.fleetnavigator.repository.GgufModelConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
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
    private final ModelMappingService modelMappingService;
    private final io.javafleet.fleetnavigator.config.FleetPathsConfiguration pathsConfig;
    // Thread-safe model cache - fixes "stream aborted on second message" bug
    private final Map<String, LlamaModel> loadedModels = new ConcurrentHashMap<>();
    private final Set<String> activeRequests = Collections.synchronizedSet(new HashSet<>());
    // Lock to ensure only one generate() runs at a time per model (java-llama.cpp limitation)
    private final Map<String, ReentrantLock> modelLocks = new ConcurrentHashMap<>();

    public JavaLlamaCppProvider(LLMConfigProperties config,
                                 GgufModelConfigRepository ggufModelConfigRepository,
                                 ModelMappingService modelMappingService,
                                 io.javafleet.fleetnavigator.config.FleetPathsConfiguration pathsConfig) {
        this.config = config;
        this.ggufModelConfigRepository = ggufModelConfigRepository;
        this.modelMappingService = modelMappingService;
        this.pathsConfig = pathsConfig;
        log.info("🦙 JavaLlamaCppProvider initialized (JNI-based) with ModelMappingService");
    }

    /**
     * Clean up GPU memory at application startup.
     * This kills any orphaned processes from previous crashed/killed instances
     * that might still be holding GPU memory.
     */
    @PostConstruct
    public void cleanupGpuMemoryOnStartup() {
        log.info("🧹 GPU Memory Cleanup: Checking for orphaned GPU processes...");

        try {
            // Get current process ID (to avoid killing ourselves)
            long currentPid = ProcessHandle.current().pid();

            // Query nvidia-smi for processes using GPU
            ProcessBuilder pb = new ProcessBuilder(
                "nvidia-smi", "--query-compute-apps=pid", "--format=csv,noheader"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            List<Long> gpuPids = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.equals("[N/A]")) {
                        try {
                            long pid = Long.parseLong(line);
                            if (pid != currentPid) {
                                gpuPids.add(pid);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid PIDs
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("⚠️ nvidia-smi returned exit code {}. GPU may not be available.", exitCode);
                return;
            }

            if (gpuPids.isEmpty()) {
                log.info("✅ GPU memory is clean - no orphaned processes found");
                return;
            }

            log.warn("⚠️ Found {} orphaned GPU process(es): {}", gpuPids.size(), gpuPids);

            // Kill orphaned processes
            int killed = 0;
            for (Long pid : gpuPids) {
                try {
                    ProcessBuilder killPb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
                    Process killProcess = killPb.start();
                    int killExitCode = killProcess.waitFor();
                    if (killExitCode == 0) {
                        log.info("🔫 Killed orphaned GPU process: {}", pid);
                        killed++;
                    } else {
                        log.warn("⚠️ Could not kill process {} (may have already exited)", pid);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Error killing process {}: {}", pid, e.getMessage());
                }
            }

            if (killed > 0) {
                log.info("✅ GPU Memory Cleanup complete - killed {} orphaned process(es)", killed);
                // Give GPU a moment to release memory
                Thread.sleep(500);
            }

        } catch (IOException e) {
            log.debug("nvidia-smi not available (no NVIDIA GPU?): {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("GPU cleanup interrupted");
        }
    }

    /**
     * Get the models directory - uses FleetPathsConfiguration for auto-detection
     */
    private Path getModelsDir() {
        // If explicit override is set in config, use it
        String configModelsDir = config.getLlamacpp().getModelsDir();
        if (configModelsDir != null && !configModelsDir.equals("./models") && !configModelsDir.isBlank()) {
            return Paths.get(configModelsDir);
        }
        // Otherwise use auto-detected path from FleetPathsConfiguration
        return pathsConfig.getResolvedModelsDir();
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
        // Check if models directory exists (uses FleetPathsConfiguration)
        Path modelsDir = getModelsDir();
        log.debug("🔍 isAvailable() checking models directory: {}", modelsDir.toAbsolutePath());
        boolean available = Files.exists(modelsDir) && Files.isDirectory(modelsDir);
        log.debug("🔍 isAvailable() result: {} (exists: {}, isDir: {})",
                  available, Files.exists(modelsDir), Files.isDirectory(modelsDir));
        return available;
    }

    @Override
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        log.info("Starting non-streaming generation with model: {}", model);

        StringBuilder fullResponse = new StringBuilder();
        chatStream(model, prompt, systemPrompt, requestId,
                   fullResponse::append, null, null, null, null, null, null);

        return fullResponse.toString();
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature,
                           Double topP, Integer topK, Double repeatPenalty,
                           Integer numCtx) throws IOException {
        // Delegate to cpuOnly version with default false
        chatStream(model, prompt, systemPrompt, requestId, chunkConsumer,
                   maxTokens, temperature, topP, topK, repeatPenalty, numCtx, false);
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature,
                           Double topP, Integer topK, Double repeatPenalty,
                           Integer numCtx, Boolean cpuOnly) throws IOException {
        // Note: numCtx is configured at JNI level, not per-request for java-llama.cpp
        if (numCtx != null) {
            log.info("📏 numCtx={} requested (configured at startup for java-llama.cpp)", numCtx);
        }

        // CPU-Only Mode: Log for debugging
        log.info("🔍 DEBUG JavaLlamaCppProvider.chatStream - cpuOnly={}", cpuOnly);

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

        // Get model lock (java-llama.cpp models can only handle one generate() at a time)
        String cacheKey = Boolean.TRUE.equals(cpuOnly) ? model + "_CPU_ONLY" : model;
        ReentrantLock modelLock = modelLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());

        log.info("🔒 Acquiring lock for model: {} (waiting: {})", cacheKey, modelLock.hasQueuedThreads());
        modelLock.lock();
        log.info("🔓 Lock acquired for model: {}", cacheKey);

        try {
            // Get or load model (with CPU-Only support)
            LlamaModel llamaModel = getOrLoadModel(model, Boolean.TRUE.equals(cpuOnly));

            // Build full prompt with system message
            String fullPrompt = buildPrompt(effectiveSystemPrompt, prompt, model);
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

            // Default stop strings for ChatML format (stop generation at end tokens)
            params.setStopStrings("<|im_end|>");
            params.setStopStrings("<|eot_id|>");
            params.setStopStrings("<|im_start|>");

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

            // Stream generation with token buffering for proper end-token detection
            // Buffer to accumulate tokens (end tokens can be split across chunks)
            StringBuilder tokenBuffer = new StringBuilder();
            final String[] END_TOKENS = {"<|im_end|>", "<|eot_id|>", "<|im_start|>assistant", "<|end|>"};
            final int MAX_BUFFER_SIZE = 20; // Longest end token is ~15 chars

            for (LlamaOutput output : llamaModel.generate(params)) {
                // Check if request was cancelled
                if (!activeRequests.contains(requestId)) {
                    log.info("Request {} was cancelled", requestId);
                    break;
                }

                String chunk = output.toString();
                tokenBuffer.append(chunk);

                // Check if buffer contains any end token
                String bufferContent = tokenBuffer.toString();
                boolean endTokenFound = false;

                for (String endToken : END_TOKENS) {
                    if (bufferContent.contains(endToken)) {
                        // Remove end token and everything after it
                        int idx = bufferContent.indexOf(endToken);
                        String cleanContent = bufferContent.substring(0, idx);
                        if (!cleanContent.isEmpty()) {
                            chunkConsumer.accept(cleanContent);
                        }
                        endTokenFound = true;
                        log.debug("End token '{}' detected, stopping generation", endToken);
                        break;
                    }
                }

                if (endTokenFound) {
                    break; // Stop generation
                }

                // Check if buffer might contain partial end token
                boolean mightContainPartial = false;
                for (String endToken : END_TOKENS) {
                    // Check if any prefix of an end token matches end of buffer
                    for (int len = 1; len < endToken.length(); len++) {
                        String prefix = endToken.substring(0, len);
                        if (bufferContent.endsWith(prefix)) {
                            mightContainPartial = true;
                            break;
                        }
                    }
                    if (mightContainPartial) break;
                }

                if (!mightContainPartial) {
                    // Safe to flush buffer - no partial end token
                    chunkConsumer.accept(bufferContent);
                    tokenBuffer.setLength(0);
                } else if (tokenBuffer.length() > MAX_BUFFER_SIZE) {
                    // Buffer too large, flush partial content
                    String safeContent = bufferContent.substring(0, bufferContent.length() - MAX_BUFFER_SIZE);
                    chunkConsumer.accept(safeContent);
                    tokenBuffer.delete(0, safeContent.length());
                }
            }

            // Flush remaining buffer (filter any end tokens)
            if (tokenBuffer.length() > 0) {
                String remaining = tokenBuffer.toString();
                for (String endToken : END_TOKENS) {
                    remaining = remaining.replace(endToken, "");
                }
                if (!remaining.isEmpty()) {
                    chunkConsumer.accept(remaining);
                }
            }
        } catch (Exception e) {
            log.error("Error during streaming generation", e);
            throw new IOException("Streaming generation failed: " + e.getMessage(), e);
        } finally {
            activeRequests.remove(requestId);
            modelLock.unlock();
            log.info("🔓 Lock released for model: {}", cacheKey);
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
        Path modelsDir = getModelsDir();

        if (!Files.exists(modelsDir)) {
            log.warn("Models directory does not exist: {}", modelsDir.toAbsolutePath());
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
        throw new UnsupportedOperationException("Model pulling not supported by java-llama-cpp. Please copy GGUF files manually to " + getModelsDir().toAbsolutePath());
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
     * Unload ALL cached models and free GPU memory.
     * Can be called manually via API or automatically when switching models.
     *
     * @return Number of models unloaded
     */
    public int unloadAllModels() {
        if (loadedModels.isEmpty()) {
            log.info("🧹 No models loaded - nothing to unload");
            return 0;
        }

        int count = loadedModels.size();
        log.info("🧹 GPU Memory Cleanup: Unloading {} cached model(s)...", count);

        List<String> keys = new ArrayList<>(loadedModels.keySet());
        for (String cacheKey : keys) {
            try {
                LlamaModel model = loadedModels.remove(cacheKey);
                if (model != null) {
                    log.info("🧹 Unloading model: {}", cacheKey);
                    model.close();
                    log.info("✅ Model unloaded: {}", cacheKey);
                }
            } catch (Exception e) {
                log.warn("⚠️ Error unloading model {}: {}", cacheKey, e.getMessage());
            }
        }

        // Force garbage collection
        System.gc();

        log.info("✅ GPU memory cleanup complete. Unloaded {} model(s).", count);
        return count;
    }

    /**
     * Get number of currently loaded models
     */
    public int getLoadedModelCount() {
        return loadedModels.size();
    }

    /**
     * Get names of currently loaded models
     */
    public List<String> getLoadedModelNames() {
        return new ArrayList<>(loadedModels.keySet());
    }

    /**
     * Unload all models except the one we're about to load.
     * This frees GPU memory before loading a new model.
     *
     * GPU Memory Problem: Large models like Mistral-Nemo (7.5GB) need almost
     * all available VRAM. If another model is still cached, loading fails with
     * "unable to allocate CUDA buffer".
     *
     * Solution: Unload all other models before loading a new one.
     *
     * @param keepCacheKey The cache key of the model we're about to load (don't unload this one)
     */
    private void unloadOtherModels(String keepCacheKey) {
        if (loadedModels.isEmpty()) {
            return;
        }

        // Find models to unload (all except the one we want to keep)
        List<String> toUnload = loadedModels.keySet().stream()
                .filter(key -> !key.equals(keepCacheKey))
                .toList();

        if (toUnload.isEmpty()) {
            return;
        }

        log.info("🧹 GPU Memory Management: Unloading {} cached model(s) to free VRAM...", toUnload.size());

        for (String cacheKey : toUnload) {
            try {
                LlamaModel model = loadedModels.remove(cacheKey);
                if (model != null) {
                    log.info("🧹 Unloading model: {}", cacheKey);
                    model.close();
                    log.info("✅ Model unloaded: {}", cacheKey);
                }
            } catch (Exception e) {
                log.warn("⚠️ Error unloading model {}: {}", cacheKey, e.getMessage());
            }
        }

        // Force garbage collection to release native memory
        // CUDA needs time to actually free the memory after Java objects are GC'd
        log.info("🔄 Running garbage collection to release native CUDA memory...");
        System.gc();

        // Wait for GC to complete and CUDA driver to reclaim memory
        // 500ms was not enough - CUDA needs more time especially for large models
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Second GC pass to ensure all native resources are freed
        System.gc();

        // Additional delay for CUDA driver to fully reclaim memory
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("✅ GPU memory cleanup complete (waited 2.5s for CUDA). Ready to load new model.");
    }

    /**
     * Get or load a model (with caching) - delegates to cpuOnly version
     */
    private LlamaModel getOrLoadModel(String modelName) throws IOException {
        return getOrLoadModel(modelName, false);
    }

    /**
     * Get or load a model (with caching)
     * Checks for custom GGUF model configuration first, falls back to default config
     * @param cpuOnly if true, loads model with gpuLayers=0 (CPU-only mode for demos)
     */
    private LlamaModel getOrLoadModel(String modelName, boolean cpuOnly) throws IOException {
        // Use separate cache key for CPU-only models
        String cacheKey = cpuOnly ? modelName + "_CPU_ONLY" : modelName;

        // Check cache
        if (loadedModels.containsKey(cacheKey)) {
            log.debug("Using cached model: {} (CPU-Only: {})", modelName, cpuOnly);
            return loadedModels.get(cacheKey);
        }

        // ============================================================
        // GPU Memory Management: Unload other models before loading new one
        // This prevents "unable to allocate CUDA buffer" errors
        // ============================================================
        unloadOtherModels(cacheKey);

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

        // CPU-Only Mode: Override gpuLayers to 0 (disables CUDA/GPU)
        if (cpuOnly) {
            log.info("🖥️ CPU-Only Mode: Setting gpuLayers=0 (was: {})", gpuLayers);
            gpuLayers = 0;
        }

        if (!Files.exists(modelPath)) {
            log.error("❌ Model file not found at resolved path: {}", modelPath.toAbsolutePath());
            log.error("❌ Model name requested: {}", modelName);
            log.error("❌ Models directory (resolved): {}", getModelsDir().toAbsolutePath());
            throw new IOException("Model file not found: " + modelPath.toAbsolutePath());
        }

        log.info("✅ Loading model: {} from path: {} (context: {}K, GPU layers: {}, CPU-Only: {})",
                 modelName, modelPath.toAbsolutePath(), contextSize / 1024, gpuLayers, cpuOnly);

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
        String modelPathString = modelPath.toString();
        try {
            log.info("📂 Attempting to load model with path: {}", modelPathString);
            log.info("📂 File exists check: {}", Files.exists(modelPath));
            log.info("📂 File readable check: {}", Files.isReadable(modelPath));
            log.info("📂 File size: {} bytes", Files.size(modelPath));

            LlamaModel model = new LlamaModel(modelParams);
            loadedModels.put(cacheKey, model);  // Use cacheKey (includes _CPU_ONLY suffix when cpuOnly=true)
            log.info("✅ Model loaded successfully: {} (context: {}K, CPU-Only: {}, cacheKey: {})",
                     modelName, contextSize / 1024, cpuOnly, cacheKey);
            return model;
        } catch (Exception e) {
            log.error("❌ Failed to load model: {} (CPU-Only: {})", modelName, cpuOnly);
            log.error("❌ Model path was: {}", modelPathString);
            log.error("❌ Exception: {}", e.getMessage(), e);
            throw new IOException("Failed to load model: " + e.getMessage(), e);
        }
    }

    /**
     * Resolve model file path from model name.
     *
     * Unterstützt automatisches Mapping von Ollama-Modellnamen zu GGUF-Dateien.
     * z.B. "qwen2.5:7b" → "custom/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
     */
    private Path resolveModelPath(String modelName) {
        Path modelsDir = getModelsDir();

        log.info("🔍 Resolving model path for: {}", modelName);
        log.info("🔍 Models directory (resolved): {}", modelsDir.toAbsolutePath());

        // 0. Wenn bereits ein absoluter Pfad, direkt prüfen und verwenden
        if (modelName != null && modelName.startsWith("/")) {
            Path absolutePath = Paths.get(modelName);
            if (Files.exists(absolutePath)) {
                log.info("✅ Using absolute model path: {}", absolutePath);
                return absolutePath;
            }
            // Wenn absoluter Pfad nicht existiert, extrahiere nur den Dateinamen
            String fileName = absolutePath.getFileName().toString();
            log.debug("🔍 Absolute path not found, trying filename only: {}", fileName);
            modelName = fileName;
        }

        // 1. Wenn bereits ein GGUF-Pfad, direkt verarbeiten
        if (modelName != null && modelName.toLowerCase().endsWith(".gguf")) {
            // Schon eine GGUF-Datei
            Path[] candidates = {
                    modelsDir.resolve(modelName),
                    modelsDir.resolve("library").resolve(modelName),
                    modelsDir.resolve("custom").resolve(modelName)
            };
            for (Path candidate : candidates) {
                if (Files.exists(candidate)) {
                    log.info("✅ Found GGUF model at: {}", candidate.toAbsolutePath());
                    return candidate;
                }
            }
        }

        // 2. Versuche Ollama-Modellname über ModelMappingService aufzulösen
        if (modelMappingService != null) {
            String mappedPath = modelMappingService.resolveToGgufPath(modelName);
            if (mappedPath != null) {
                Path resolvedPath = Paths.get(mappedPath);
                if (Files.exists(resolvedPath)) {
                    log.info("✅ Mapped Ollama model '{}' → GGUF: {}", modelName, resolvedPath);
                    return resolvedPath;
                } else {
                    log.debug("Mapped path does not exist: {}", resolvedPath);
                }
            }
        }

        // 3. Fallback: Standard-Suchpfade
        Path[] candidates = {
                modelsDir.resolve(modelName),
                modelsDir.resolve("library").resolve(modelName),
                modelsDir.resolve("custom").resolve(modelName)
        };

        for (Path candidate : candidates) {
            log.debug("🔍 Trying: {}", candidate.toAbsolutePath());
            if (Files.exists(candidate)) {
                log.info("✅ Found model at: {}", candidate.toAbsolutePath());
                return candidate;
            }
        }

        // 4. Letzter Fallback: Standard-GGUF-Modell verwenden
        if (modelMappingService != null) {
            String defaultGguf = modelMappingService.getDefaultGgufPath();
            if (defaultGguf != null) {
                Path defaultPath = Paths.get(defaultGguf);
                if (Files.exists(defaultPath)) {
                    log.warn("⚠️ Model '{}' nicht gefunden, verwende Standard-Modell: {}", modelName, defaultPath);
                    return defaultPath;
                }
            }
        }

        // Absoluter Fallback
        Path defaultPath = modelsDir.resolve("custom").resolve("Llama-3.2-1B-Instruct-Q4_K_M.gguf");
        log.warn("⚠️ Model not found in any location, using hardcoded default: {}", defaultPath.toAbsolutePath());
        return defaultPath;
    }

    /**
     * Build prompt with system message - auto-detects format based on model name
     */
    private String buildPrompt(String systemPrompt, String userPrompt, String modelName) {
        String modelLower = modelName != null ? modelName.toLowerCase() : "";

        // Detect prompt format based on model name
        if (modelLower.contains("mistral") || modelLower.contains("mixtral")) {
            log.info("📝 Using Mistral prompt format for model: {}", modelName);
            return buildMistralPrompt(systemPrompt, userPrompt);
        } else if (modelLower.contains("llama-3") || modelLower.contains("llama3")) {
            log.info("📝 Using Llama 3 prompt format for model: {}", modelName);
            return buildLlama3Prompt(systemPrompt, userPrompt);
        } else if (modelLower.contains("gemma")) {
            log.info("📝 Using Gemma prompt format for model: {}", modelName);
            return buildGemmaPrompt(systemPrompt, userPrompt);
        } else {
            log.info("📝 Using ChatML prompt format for model: {}", modelName);
            return buildChatMLPrompt(systemPrompt, userPrompt);
        }
    }

    /**
     * Mistral/Mixtral format: [INST] ... [/INST]
     */
    private String buildMistralPrompt(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return String.format("[INST] %s [/INST]", userPrompt);
        }
        // Mistral: System prompt goes at the beginning of the first [INST] block
        return String.format("[INST] %s\n\n%s [/INST]", systemPrompt, userPrompt);
    }

    /**
     * Llama 3 format with special tokens
     */
    private String buildLlama3Prompt(String systemPrompt, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("<|begin_of_text|>");

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n");
            sb.append(systemPrompt);
            sb.append("<|eot_id|>");
        }

        sb.append("<|start_header_id|>user<|end_header_id|>\n\n");
        sb.append(userPrompt);
        sb.append("<|eot_id|>");
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n");

        return sb.toString();
    }

    /**
     * Gemma format
     */
    private String buildGemmaPrompt(String systemPrompt, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("<start_of_turn>user\n");

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append(systemPrompt).append("\n\n");
        }
        sb.append(userPrompt);
        sb.append("<end_of_turn>\n<start_of_turn>model\n");

        return sb.toString();
    }

    /**
     * ChatML format (Qwen, most instruct models)
     */
    private String buildChatMLPrompt(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return String.format("<|im_start|>user\n%s<|im_end|>\n<|im_start|>assistant\n", userPrompt);
        }

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
