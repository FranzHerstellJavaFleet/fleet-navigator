package io.javafleet.fleetnavigator.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * llama.cpp Provider Implementation
 *
 * Embedded llama-server mit GGUF-Modellen
 *
 * Native Image Critical:
 * - ProcessBuilder ist Native Image kompatibel
 * - Keine Runtime.exec() nutzen
 * - File I/O ist OK
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.7
 */
@Component
@Slf4j
public class LlamaCppProvider implements LLMProvider {

    private static final Set<ProviderFeature> SUPPORTED_FEATURES = EnumSet.of(
        ProviderFeature.STREAMING,
        ProviderFeature.BLOCKING,
        ProviderFeature.MODEL_DETAILS,
        ProviderFeature.EMBEDDINGS,
        ProviderFeature.VISION
    );

    private final LLMConfigProperties config;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private Process llamaServerProcess;
    private Path currentModel;

    // Track active requests for cancellation
    private final Map<String, Call> activeRequests = new ConcurrentHashMap<>();

    public LlamaCppProvider(LLMConfigProperties config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        // Build OkHttp client with timeout
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        log.info("🦙 LlamaCppProvider initialized");
    }

    @Override
    public boolean supportsFeature(ProviderFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        return EnumSet.copyOf(SUPPORTED_FEATURES);
    }

    /**
     * Startet llama-server beim Startup (wenn auto-start aktiviert)
     */
    @PostConstruct
    public void init() {
        if (!config.getLlamacpp().isEnabled()) {
            log.info("llama.cpp provider is disabled");
            return;
        }

        if (!config.getLlamacpp().isAutoStart()) {
            log.info("llama.cpp auto-start is disabled");
            return;
        }

        // Auto-start wird beim ersten Modell-Load durchgeführt
        log.info("llama.cpp ready for on-demand model loading");
    }

    /**
     * Stoppt llama-server beim Shutdown
     */
    @PreDestroy
    public void shutdown() {
        stopLlamaServer();
    }

    @Override
    public String getProviderName() {
        return "llamacpp";
    }

    @Override
    public boolean isAvailable() {
        if (!config.getLlamacpp().isEnabled()) {
            return false;
        }

        // Check if binary exists
        Path binaryPath = Paths.get(config.getLlamacpp().getBinaryPath());
        if (!Files.exists(binaryPath)) {
            log.debug("llama-server binary not found at: {}", binaryPath);
            return false;
        }

        // Check if models directory exists and has at least one GGUF file
        Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());
        if (!Files.exists(modelsDir)) {
            log.debug("Models directory not found: {}", modelsDir);
            return false;
        }

        try {
            long ggufCount = 0;

            // Check root models directory
            ggufCount += Files.list(modelsDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".gguf"))
                    .count();

            // Check library subdirectory
            Path libraryDir = modelsDir.resolve("library");
            if (Files.exists(libraryDir)) {
                ggufCount += Files.list(libraryDir)
                        .filter(path -> path.toString().toLowerCase().endsWith(".gguf"))
                        .count();
            }

            // Check custom subdirectory
            Path customDir = modelsDir.resolve("custom");
            if (Files.exists(customDir)) {
                ggufCount += Files.list(customDir)
                        .filter(path -> path.toString().toLowerCase().endsWith(".gguf"))
                        .count();
            }

            if (ggufCount == 0) {
                log.debug("No GGUF models found in: {}", modelsDir);
                return false;
            }

            log.debug("llama.cpp is available with {} GGUF models", ggufCount);
            return true;
        } catch (IOException e) {
            log.error("Error checking models directory", e);
            return false;
        }
    }

    @Override
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        throw new UnsupportedOperationException("llama.cpp chat() not yet implemented - use chatStream()");
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature,
                           Double topP, Integer topK, Double repeatPenalty,
                           Integer numCtx) throws IOException {
        // Note: numCtx for llama-server is configured at server startup via -c flag
        if (numCtx != null) {
            log.info("📏 numCtx={} requested (llama-server uses -c at startup)", numCtx);
        }

        // Ensure llama-server is running with the correct model
        ensureLlamaServerRunning(model);

        // Build messages array for OpenAI-compatible format
        List<Map<String, String>> messages = new ArrayList<>();

        // Add system message if provided
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // Add user message
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        // Build request body for llama.cpp server (OpenAI-compatible format)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        // Add parameters (OpenAI format)
        if (maxTokens != null && maxTokens > 0) {
            requestBody.put("max_tokens", maxTokens);
        }
        if (temperature != null) {
            requestBody.put("temperature", temperature);
        }
        if (topP != null) {
            requestBody.put("top_p", topP);
        }
        // Note: OpenAI format doesn't support top_k and repeat_penalty directly
        // llama-server might support them anyway

        String json = objectMapper.writeValueAsString(requestBody);

        // llama-server uses /v1/chat/completions endpoint (OpenAI-compatible)
        String url = "http://localhost:" + config.getLlamacpp().getPort() + "/v1/chat/completions";

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);

        // Track this request for potential cancellation
        if (requestId != null) {
            activeRequests.put(requestId, call);
            log.debug("Tracking llama.cpp request: {}", requestId);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("llama-server returned {}: {}", response.code(), errorBody);
                log.error("Request JSON length: {} chars", json.length());
                throw new IOException("llama-server API error: " + response.code() + " - " + errorBody);
            }

            // Read the streaming response line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (call.isCanceled()) {
                        log.info("llama.cpp request {} was cancelled", requestId);
                        break;
                    }

                    // Skip "data: " prefix if present (SSE format)
                    if (line.startsWith("data: ")) {
                        line = line.substring(6);
                    }

                    // Skip empty lines
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Check for [DONE] marker (OpenAI format)
                    if (line.trim().equals("[DONE]")) {
                        log.debug("llama.cpp streaming completed for request: {}", requestId);
                        break;
                    }

                    // Parse each JSON line
                    JsonNode jsonNode = objectMapper.readTree(line);

                    // Extract content from OpenAI format: choices[0].delta.content
                    if (jsonNode.has("choices") && jsonNode.get("choices").isArray() &&
                        jsonNode.get("choices").size() > 0) {
                        JsonNode choice = jsonNode.get("choices").get(0);
                        if (choice.has("delta")) {
                            JsonNode delta = choice.get("delta");
                            if (delta.has("content")) {
                                String chunk = delta.get("content").asText();
                                chunkConsumer.accept(chunk);
                            }
                        }
                    }

                    // Fallback: Legacy format (old llama.cpp)
                    if (jsonNode.has("content")) {
                        String chunk = jsonNode.get("content").asText();
                        chunkConsumer.accept(chunk);
                    }
                }
            }
        } catch (IOException e) {
            if (call.isCanceled()) {
                log.info("llama.cpp request {} was cancelled", requestId);
                throw new IOException("Request was cancelled by user");
            }
            throw e;
        } finally {
            // Remove from active requests
            if (requestId != null) {
                activeRequests.remove(requestId);
            }
        }
    }

    @Override
    public String chatWithVision(String model, String prompt, List<String> images,
                                  String systemPrompt, String requestId) throws IOException {

        StringBuilder response = new StringBuilder();

        // Use streaming internally to collect full response
        chatStreamWithVision(model, prompt, images, systemPrompt, requestId, chunk -> {
            response.append(chunk);
        });

        return response.toString();
    }

    @Override
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                      String systemPrompt, String requestId,
                                      Consumer<String> chunkConsumer) throws IOException {
        // Call with default parameters
        chatStreamWithVision(model, prompt, images, systemPrompt, requestId, chunkConsumer,
            io.javafleet.fleetnavigator.dto.SamplingParameters.balanced());
    }

    /**
     * Vision chat with full parameter control
     */
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                      String systemPrompt, String requestId,
                                      Consumer<String> chunkConsumer,
                                      io.javafleet.fleetnavigator.dto.SamplingParameters params) throws IOException {

        // Use default params if none provided
        if (params == null) {
            params = io.javafleet.fleetnavigator.dto.SamplingParameters.balanced();
        }

        // Ensure llama-server is running with the correct model
        ensureLlamaServerRunning(model);

        // Build messages array for OpenAI-compatible vision format
        List<Map<String, Object>> messages = new ArrayList<>();

        // Add system message - use custom from params, request, or default
        String effectiveSystemPrompt = params.getCustomSystemPrompt();
        if (effectiveSystemPrompt == null || effectiveSystemPrompt.isEmpty()) {
            effectiveSystemPrompt = systemPrompt;
        }
        if (effectiveSystemPrompt == null || effectiveSystemPrompt.isEmpty()) {
            effectiveSystemPrompt = "You are a precise image analysis assistant. Describe only what you actually see in the image. Do not invent or assume details that are not clearly visible. Be factual and concise.";
        }

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", effectiveSystemPrompt);
        messages.add(systemMsg);

        // Add user message with images (OpenAI vision format)
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");

        // Build content array with text and images
        List<Map<String, Object>> contentArray = new ArrayList<>();

        // Add text content
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        contentArray.add(textContent);

        // Add image content (Base64 format)
        if (images != null && !images.isEmpty()) {
            for (String imageData : images) {
                Map<String, Object> imageContent = new HashMap<>();
                imageContent.put("type", "image_url");

                Map<String, String> imageUrl = new HashMap<>();
                // If imageData is already a data URL, use it directly
                // Otherwise, assume it's base64 and add the prefix
                if (imageData.startsWith("data:image/")) {
                    imageUrl.put("url", imageData);
                } else {
                    imageUrl.put("url", "data:image/jpeg;base64," + imageData);
                }

                imageContent.put("image_url", imageUrl);
                contentArray.add(imageContent);
            }
        }

        userMsg.put("content", contentArray);
        messages.add(userMsg);

        // Build request body with generation parameters from SamplingParameters
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        // === GENERATION CONTROL ===
        if (params.getMaxTokens() != null) {
            requestBody.put("max_tokens", params.getMaxTokens());
        }

        // === SAMPLING PARAMETERS ===
        if (params.getTemperature() != null) {
            requestBody.put("temperature", params.getTemperature());
        }
        if (params.getTopP() != null) {
            requestBody.put("top_p", params.getTopP());
        }
        if (params.getTopK() != null && params.getTopK() > 0) {
            requestBody.put("top_k", params.getTopK());
        }
        if (params.getMinP() != null && params.getMinP() > 0.0) {
            requestBody.put("min_p", params.getMinP());
        }

        // === REPETITION CONTROL ===
        if (params.getRepeatPenalty() != null) {
            requestBody.put("repeat_penalty", params.getRepeatPenalty());
        }
        if (params.getRepeatLastN() != null && params.getRepeatLastN() > 0) {
            requestBody.put("repeat_last_n", params.getRepeatLastN());
        }
        if (params.getPresencePenalty() != null && params.getPresencePenalty() != 0.0) {
            requestBody.put("presence_penalty", params.getPresencePenalty());
        }
        if (params.getFrequencyPenalty() != null && params.getFrequencyPenalty() != 0.0) {
            requestBody.put("frequency_penalty", params.getFrequencyPenalty());
        }

        // === MIROSTAT (Advanced Sampling) ===
        if (params.getMirostatMode() != null && params.getMirostatMode() > 0) {
            requestBody.put("mirostat", params.getMirostatMode());
            if (params.getMirostatTau() != null) {
                requestBody.put("mirostat_tau", params.getMirostatTau());
            }
            if (params.getMirostatEta() != null) {
                requestBody.put("mirostat_eta", params.getMirostatEta());
            }
        }

        // === STOP SEQUENCES ===
        if (params.getStopSequences() != null && !params.getStopSequences().isEmpty()) {
            requestBody.put("stop", params.getStopSequences());
        }

        String json = objectMapper.writeValueAsString(requestBody);
        log.info("🎛️ Vision Request Parameters: {}", json);

        // Use same endpoint as regular chat
        String url = "http://localhost:" + config.getLlamacpp().getPort() + "/v1/chat/completions";

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);

        // Track this request for potential cancellation
        if (requestId != null) {
            activeRequests.put(requestId, call);
            log.debug("Tracking llama.cpp vision request: {}", requestId);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("llama-server vision returned {}: {}", response.code(), errorBody);
                throw new IOException("llama-server vision API error: " + response.code() + " - " + errorBody);
            }

            // Read the streaming response line by line (same as regular chat)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (call.isCanceled()) {
                        log.info("llama.cpp vision request {} was cancelled", requestId);
                        break;
                    }

                    // Skip "data: " prefix if present (SSE format)
                    if (line.startsWith("data: ")) {
                        line = line.substring(6);
                    }

                    // Skip empty lines
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Check for [DONE] marker
                    if (line.trim().equals("[DONE]")) {
                        log.debug("llama.cpp vision streaming completed for request: {}", requestId);
                        break;
                    }

                    // Parse each JSON line
                    JsonNode jsonNode = objectMapper.readTree(line);

                    // Extract content from OpenAI format
                    if (jsonNode.has("choices") && jsonNode.get("choices").isArray() &&
                        jsonNode.get("choices").size() > 0) {
                        JsonNode choice = jsonNode.get("choices").get(0);
                        if (choice.has("delta")) {
                            JsonNode delta = choice.get("delta");
                            if (delta.has("content")) {
                                String chunk = delta.get("content").asText();
                                chunkConsumer.accept(chunk);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (call.isCanceled()) {
                log.info("llama.cpp vision request {} was cancelled", requestId);
                throw new IOException("Request was cancelled by user");
            }
            throw e;
        } finally {
            // Remove from active requests
            if (requestId != null) {
                activeRequests.remove(requestId);
            }
        }
    }

    @Override
    public List<ModelInfo> getAvailableModels() throws IOException {
        List<ModelInfo> models = new ArrayList<>();
        Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());

        if (!Files.exists(modelsDir)) {
            log.warn("Models directory does not exist: {}", modelsDir);
            return models;
        }

        // Scan library directory (downloaded models)
        Path libraryDir = modelsDir.resolve("library");
        if (Files.exists(libraryDir)) {
            try (Stream<Path> paths = Files.list(libraryDir)) {
                paths.filter(path -> path.toString().toLowerCase().endsWith(".gguf"))
                        .forEach(path -> {
                            String filename = path.getFileName().toString();
                            String displayName = extractDisplayName(filename);

                            ModelInfo modelInfo = ModelInfo.builder()
                                    .name(filename)
                                    .displayName(displayName)
                                    .provider("llamacpp")
                                    .size(getFileSize(path))
                                    .sizeHuman(formatBytes(getFileSize(path)))
                                    .architecture(extractArchitecture(filename))
                                    .quantization(extractQuantization(filename))
                                    .installed(true)
                                    .custom(false)
                                    .build();

                            models.add(modelInfo);
                        });
            }
        }

        // Scan custom directory (user uploaded models)
        Path customDir = modelsDir.resolve("custom");
        if (Files.exists(customDir)) {
            try (Stream<Path> paths = Files.list(customDir)) {
                paths.filter(path -> path.toString().toLowerCase().endsWith(".gguf"))
                        .forEach(path -> {
                            String filename = path.getFileName().toString();
                            String displayName = extractDisplayName(filename);

                            ModelInfo modelInfo = ModelInfo.builder()
                                    .name(filename)
                                    .displayName(displayName)
                                    .provider("llamacpp")
                                    .size(getFileSize(path))
                                    .sizeHuman(formatBytes(getFileSize(path)))
                                    .architecture(extractArchitecture(filename))
                                    .quantization(extractQuantization(filename))
                                    .installed(true)
                                    .custom(true)
                                    .build();

                            models.add(modelInfo);
                        });
            }
        }

        // Also scan root models directory for backwards compatibility
        try (Stream<Path> paths = Files.list(modelsDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".gguf"))
                    .forEach(path -> {
                        String filename = path.getFileName().toString();
                        String displayName = extractDisplayName(filename);

                        ModelInfo modelInfo = ModelInfo.builder()
                                .name(filename)
                                .displayName(displayName)
                                .provider("llamacpp")
                                .size(getFileSize(path))
                                .sizeHuman(formatBytes(getFileSize(path)))
                                .architecture(extractArchitecture(filename))
                                .quantization(extractQuantization(filename))
                                .installed(true)
                                .custom(false)
                                .build();

                        models.add(modelInfo);
                    });
        }

        log.info("Found {} GGUF models", models.size());
        return models;
    }

    @Override
    public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
        // Note: This method is now implemented via ModelDownloadService
        // The modelName is expected to be a model ID from ModelRegistry
        log.info("Pull model request for: {}", modelName);
        progressConsumer.accept("ℹ️ Modell-Download wird über ModelDownloadService durchgeführt");
    }

    @Override
    public boolean deleteModel(String modelName) throws IOException {
        Path modelPath = Paths.get(config.getLlamacpp().getModelsDir(), modelName);

        if (!Files.exists(modelPath)) {
            log.warn("Model not found: {}", modelPath);
            return false;
        }

        try {
            Files.delete(modelPath);
            log.info("✅ Deleted model: {}", modelName);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete model: {}", modelName, e);
            throw e;
        }
    }

    @Override
    public Map<String, Object> getModelDetails(String modelName) throws IOException {
        Path modelPath = Paths.get(config.getLlamacpp().getModelsDir(), modelName);

        if (!Files.exists(modelPath)) {
            throw new IOException("Model not found: " + modelName);
        }

        return Map.of(
                "name", modelName,
                "path", modelPath.toString(),
                "size", Files.size(modelPath),
                "sizeHuman", formatBytes(Files.size(modelPath)),
                "provider", "llamacpp",
                "type", "GGUF"
        );
    }

    @Override
    public boolean cancelRequest(String requestId) {
        Call call = activeRequests.get(requestId);
        if (call != null && !call.isCanceled()) {
            call.cancel();
            activeRequests.remove(requestId);
            log.info("Cancelled llama.cpp request: {}", requestId);
            return true;
        }
        log.warn("llama.cpp request {} not found or already completed", requestId);
        return false;
    }

    // ===== HELPER METHODS =====

    /**
     * Ensures llama-server is running with the specified model
     */
    private void ensureLlamaServerRunning(String modelName) throws IOException {
        Path modelPath = resolveModelPath(modelName);

        // If already running with this model, do nothing
        if (llamaServerProcess != null && llamaServerProcess.isAlive() && modelPath.equals(currentModel)) {
            log.debug("llama-server already running with model: {}", modelName);
            return;
        }

        // Stop existing server if running
        stopLlamaServer();

        // Start new server with requested model
        startLlamaServer(modelPath);
        currentModel = modelPath;
    }

    /**
     * Finds MMPROJ file for vision models
     * Searches for mmproj*.gguf files in the same directory as the model
     */
    private Optional<Path> findMmprojFile(Path modelPath) {
        try {
            Path modelDir = modelPath.getParent();
            if (modelDir == null) {
                return Optional.empty();
            }

            // Search for mmproj files in the same directory
            try (Stream<Path> paths = Files.list(modelDir)) {
                return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString().toLowerCase();
                        return filename.startsWith("mmproj") && filename.endsWith(".gguf");
                    })
                    .findFirst();
            }
        } catch (IOException e) {
            log.error("Error searching for MMPROJ file", e);
            return Optional.empty();
        }
    }

    /**
     * Starts llama-server with specified model
     */
    private void startLlamaServer(Path modelPath) throws IOException {
        Path binaryPath = Paths.get(config.getLlamacpp().getBinaryPath());

        if (!Files.exists(binaryPath)) {
            throw new IOException("llama-server binary not found: " + binaryPath);
        }

        if (!Files.exists(modelPath)) {
            throw new IOException("Model file not found: " + modelPath);
        }

        log.info("🚀 Starting llama-server with model: {}", modelPath.getFileName());

        List<String> command = new ArrayList<>();
        command.add(binaryPath.toString());
        command.add("-m");
        command.add(modelPath.toString());
        command.add("--port");
        command.add(String.valueOf(config.getLlamacpp().getPort()));
        command.add("--host");
        command.add("0.0.0.0");
        command.add("-ngl");
        command.add(String.valueOf(config.getLlamacpp().getGpuLayers()));
        command.add("--ctx-size");
        command.add(String.valueOf(config.getLlamacpp().getContextSize()));

        if (config.getLlamacpp().getThreads() > 0) {
            command.add("-t");
            command.add(String.valueOf(config.getLlamacpp().getThreads()));
        }

        // Performance optimizations for GPU inference
        command.add("-b");  // batch-size
        command.add("512");
        command.add("-ub"); // ubatch-size (micro-batch for prompt processing)
        command.add("256");
        command.add("-np");  // parallel slots (multiple requests simultaneously)
        command.add("4");
        command.add("--flash-attn");  // Flash Attention (if supported)

        // Add MMPROJ file for vision models (LLaVA, etc.)
        Optional<Path> mmprojFile = findMmprojFile(modelPath);
        if (mmprojFile.isPresent()) {
            log.info("🖼️ Vision model detected - adding MMPROJ file: {}", mmprojFile.get().getFileName());
            command.add("--mmproj");
            command.add(mmprojFile.get().toString());
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        // Set LD_LIBRARY_PATH to include bin/ directory (for shared libraries)
        Path binDir = binaryPath.getParent();
        if (binDir != null) {
            Map<String, String> env = pb.environment();
            String ldLibraryPath = binDir.toString();

            // Append to existing LD_LIBRARY_PATH if present
            String existing = env.get("LD_LIBRARY_PATH");
            if (existing != null && !existing.isEmpty()) {
                ldLibraryPath = ldLibraryPath + ":" + existing;
            }

            env.put("LD_LIBRARY_PATH", ldLibraryPath);
            log.info("Setting LD_LIBRARY_PATH={}", ldLibraryPath);
        }

        llamaServerProcess = pb.start();

        // Use CountDownLatch to wait for server startup AND model loading
        java.util.concurrent.CountDownLatch serverReadyLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean serverStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean modelLoaded = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Start a thread to consume output and detect when server is ready
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(llamaServerProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("llama-server: {}", line);

                    // Check if server started listening
                    if (line.contains("HTTP server is listening")) {
                        serverStarted.set(true);
                        log.info("✅ llama-server HTTP endpoint is now listening");
                    }

                    // IMPORTANT: Wait until model is fully loaded (not just server started)
                    // llama-server outputs "main: model loaded" - check lowercase to be flexible
                    if (line.toLowerCase().contains("model loaded")) {
                        modelLoaded.set(true);
                        serverReadyLatch.countDown();
                        log.info("✅ Model fully loaded and ready for inference");
                    }
                }
            } catch (IOException e) {
                log.error("Error reading llama-server output", e);
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();

        // Wait for server AND model to be ready (max 5 minutes for large Vision models with MMPROJ)
        log.info("⏳ Waiting for llama-server to start and load model...");
        try {
            boolean ready = serverReadyLatch.await(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!ready) {
                throw new IOException("llama-server did not load model within 300 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for llama-server", e);
        }

        log.info("✅ llama-server started on port {}", config.getLlamacpp().getPort());
    }

    /**
     * Stops llama-server
     */
    private void stopLlamaServer() {
        if (llamaServerProcess != null && llamaServerProcess.isAlive()) {
            log.info("🛑 Stopping llama-server...");
            llamaServerProcess.destroy();

            try {
                llamaServerProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                llamaServerProcess.destroyForcibly();
            }

            llamaServerProcess = null;
            currentModel = null;
            log.info("✅ llama-server stopped");
        }
    }

    /**
     * Resolves model name to full path
     * Searches in root, library/, and custom/ directories
     */
    private Path resolveModelPath(String modelName) throws IOException {
        Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());

        // If modelName is already a filename with .gguf extension
        if (modelName.toLowerCase().endsWith(".gguf")) {
            // Check root directory
            Path modelPath = modelsDir.resolve(modelName);
            if (Files.exists(modelPath)) {
                return modelPath;
            }

            // Check library directory
            Path libraryPath = modelsDir.resolve("library").resolve(modelName);
            if (Files.exists(libraryPath)) {
                return libraryPath;
            }

            // Check custom directory
            Path customPath = modelsDir.resolve("custom").resolve(modelName);
            if (Files.exists(customPath)) {
                return customPath;
            }
        }

        // Try to find model by partial name in all directories
        List<Path> dirsToSearch = new ArrayList<>();
        dirsToSearch.add(modelsDir);

        Path libraryDir = modelsDir.resolve("library");
        if (Files.exists(libraryDir)) {
            dirsToSearch.add(libraryDir);
        }

        Path customDir = modelsDir.resolve("custom");
        if (Files.exists(customDir)) {
            dirsToSearch.add(customDir);
        }

        // Search all directories for .gguf files matching the name
        for (Path dir : dirsToSearch) {
            try (Stream<Path> paths = Files.list(dir)) {
                Optional<Path> found = paths
                        .filter(path -> path.toString().toLowerCase().endsWith(".gguf"))
                        .filter(path -> path.getFileName().toString().toLowerCase().contains(modelName.toLowerCase()))
                        .findFirst();

                if (found.isPresent()) {
                    return found.get();
                }
            }
        }

        throw new IOException("Model not found: " + modelName);
    }

    /**
     * Extracts display name from filename
     */
    private String extractDisplayName(Path modelPath) {
        return extractDisplayName(modelPath.getFileName().toString());
    }

    private String extractDisplayName(String filename) {
        return filename
                .replace(".gguf", "")
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("(?i)\\bQ[0-9]_[KM](_[SM])?\\b", ""); // Remove quantization
    }

    /**
     * Extracts architecture from filename
     */
    private String extractArchitecture(String filename) {
        String lower = filename.toLowerCase();
        if (lower.contains("llama")) return "llama";
        if (lower.contains("qwen")) return "qwen";
        if (lower.contains("mistral")) return "mistral";
        if (lower.contains("phi")) return "phi";
        if (lower.contains("gemma")) return "gemma";
        return "unknown";
    }

    /**
     * Extracts quantization from filename
     */
    private String extractQuantization(String filename) {
        // Match patterns like Q4_K_M, Q8_0, etc.
        if (filename.matches(".*Q[0-9]_[KM](_[SM])?.*")) {
            int start = filename.indexOf('Q');
            int end = start + 6; // Q4_K_M is 6 chars
            if (end <= filename.length()) {
                return filename.substring(start, Math.min(end, filename.length()));
            }
        }
        return "unknown";
    }

    /**
     * Gets file size
     */
    private Long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            log.error("Error getting file size for: {}", path, e);
            return 0L;
        }
    }

    /**
     * Formats bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
