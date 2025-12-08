package io.javafleet.fleetnavigator.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Externer llama-server Provider für FleetCode
 *
 * Verbindet sich mit einem separat gestarteten llama-server
 * auf einem konfigurierbaren Port (Standard: 2026).
 *
 * FleetCode benötigt diesen Provider für AI Coding Agent Funktionalität.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.0
 */
@Component
@Slf4j
public class ExternalLlamaServerProvider implements LLMProvider {

    private static final Set<ProviderFeature> SUPPORTED_FEATURES = EnumSet.of(
        ProviderFeature.STREAMING,
        ProviderFeature.BLOCKING,
        ProviderFeature.MODEL_DETAILS,
        ProviderFeature.EMBEDDINGS,
        ProviderFeature.VISION
    );

    private static final String PROVIDER_NAME = "llama-server";

    private final LLMConfigProperties config;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private int serverPort = 2026;

    // Track active requests for cancellation
    private final Map<String, Call> activeRequests = new ConcurrentHashMap<>();

    public ExternalLlamaServerProvider(LLMConfigProperties config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        // Build OkHttp client with timeout
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // FleetCode standard port
        this.serverPort = 2026;

        log.info("🖥️ ExternalLlamaServerProvider initialized (port: {})", serverPort);
    }

    @Override
    public boolean supportsFeature(ProviderFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        // llama-server Provider ist IMMER als Option verfügbar
        // Der Server muss nicht laufen um den Provider auszuwählen
        // Verwende isRunning() um den tatsächlichen Server-Status zu prüfen
        return true;
    }

    /**
     * Prüft ob der llama-server tatsächlich läuft und erreichbar ist
     * Separiert von isAvailable() damit Provider-Wechsel immer möglich ist
     *
     * @return true wenn Server auf dem konfigurierten Port antwortet
     */
    public boolean isRunning() {
        try {
            String healthUrl = String.format("http://localhost:%d/health", serverPort);
            Request request = new Request.Builder()
                    .url(healthUrl)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                boolean running = response.isSuccessful();
                log.debug("External llama-server running check: {}", running ? "online" : "offline");
                return running;
            }
        } catch (IOException e) {
            log.debug("External llama-server not running on port {}: {}", serverPort, e.getMessage());
            return false;
        }
    }

    @Override
    public List<ModelInfo> getAvailableModels() throws IOException {
        // Scan the models directory for all GGUF files (same as JavaLlamaCppProvider)
        // This allows users to see and select all downloaded models
        List<ModelInfo> models = new ArrayList<>();
        Path modelsDir = getModelsDir();

        if (!Files.exists(modelsDir)) {
            log.warn("Models directory does not exist: {}", modelsDir.toAbsolutePath());
            return models;
        }

        // Track which model is currently loaded on the server
        String loadedModelName = null;
        try {
            String propsUrl = String.format("http://localhost:%d/props", serverPort);
            Request request = new Request.Builder()
                    .url(propsUrl)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode props = objectMapper.readTree(response.body().string());
                    if (props.has("model")) {
                        loadedModelName = props.get("model").asText();
                    } else if (props.has("model_path")) {
                        String path = props.get("model_path").asText();
                        loadedModelName = path.substring(path.lastIndexOf('/') + 1);
                    }
                }
            }
        } catch (IOException e) {
            log.debug("Could not get loaded model info from llama-server: {}", e.getMessage());
        }

        final String finalLoadedModelName = loadedModelName;

        // Search in library/ and custom/ subdirectories
        List<Path> searchPaths = Arrays.asList(
            modelsDir.resolve("library"),
            modelsDir.resolve("custom"),
            modelsDir
        );

        for (Path searchPath : searchPaths) {
            if (Files.exists(searchPath) && Files.isDirectory(searchPath)) {
                try (Stream<Path> files = Files.walk(searchPath, 1)) {
                    files.filter(p -> p.toString().toLowerCase().endsWith(".gguf"))
                         .forEach(p -> {
                             String filename = p.getFileName().toString();
                             ModelInfo info = new ModelInfo();
                             info.setName(filename);
                             info.setProvider(PROVIDER_NAME);
                             info.setSize(getFileSize(p));
                             info.setModifiedAt(getModifiedDate(p));

                             // Mark loaded model
                             if (filename.equals(finalLoadedModelName)) {
                                 info.setDescription("✓ Aktuell geladen auf Port " + serverPort);
                             }

                             models.add(info);
                         });
                } catch (IOException e) {
                    log.warn("Failed to scan directory: {}", searchPath, e);
                }
            }
        }

        log.info("Found {} GGUF models in {}", models.size(), modelsDir.toAbsolutePath());
        return models;
    }

    /**
     * Get the models directory path
     */
    private Path getModelsDir() {
        // Check for environment variable first
        String envModelsDir = System.getenv("FLEET_NAVIGATOR_MODELS_DIR");
        if (envModelsDir != null && !envModelsDir.isEmpty()) {
            return Paths.get(envModelsDir);
        }

        // Default: ~/.java-fleet/models
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".java-fleet", "models");
    }

    /**
     * Get file size in bytes
     */
    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Get file modification date as ISO string
     */
    private String getModifiedDate(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant().toString();
        } catch (IOException e) {
            return Instant.now().toString();
        }
    }

    @Override
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        String url = String.format("http://localhost:%d/completion", serverPort);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", buildPrompt(systemPrompt, prompt));
        requestBody.put("n_predict", 2048);
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.7);

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);
        if (requestId != null) {
            activeRequests.put(requestId, call);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response.code());
            }

            JsonNode responseNode = objectMapper.readTree(response.body().string());
            return responseNode.path("content").asText("");
        } finally {
            if (requestId != null) {
                activeRequests.remove(requestId);
            }
        }
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature,
                           Double topP, Integer topK, Double repeatPenalty,
                           Integer numCtx) throws IOException {

        String url = String.format("http://localhost:%d/completion", serverPort);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", buildPrompt(systemPrompt, prompt));
        requestBody.put("n_predict", maxTokens != null ? maxTokens : 2048);
        requestBody.put("stream", true);
        requestBody.put("temperature", temperature != null ? temperature : 0.7);

        if (topP != null) requestBody.put("top_p", topP);
        if (topK != null) requestBody.put("top_k", topK);
        if (repeatPenalty != null) requestBody.put("repeat_penalty", repeatPenalty);

        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);
        if (requestId != null) {
            activeRequests.put(requestId, call);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response.code());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        JsonNode chunk = objectMapper.readTree(data);
                        String content = chunk.path("content").asText("");
                        if (!content.isEmpty()) {
                            chunkConsumer.accept(content);
                        }

                        // Check if generation stopped
                        if (chunk.has("stop") && chunk.get("stop").asBoolean()) {
                            break;
                        }
                    }
                }
            }
        } finally {
            if (requestId != null) {
                activeRequests.remove(requestId);
            }
        }
    }

    @Override
    public String chatWithVision(String model, String prompt, List<String> images,
                                  String systemPrompt, String requestId) throws IOException {
        // Build prompt with image references (if server supports vision)
        String fullPrompt = buildPrompt(systemPrompt, prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", fullPrompt);
        requestBody.put("n_predict", 2048);
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.7);

        // Add images if server supports vision
        if (images != null && !images.isEmpty()) {
            requestBody.put("image_data", images.stream()
                .map(img -> Map.of("data", img, "id", 0))
                .toList());
        }

        String url = String.format("http://localhost:%d/completion", serverPort);
        String json = objectMapper.writeValueAsString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);
        if (requestId != null) {
            activeRequests.put(requestId, call);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response.code());
            }

            JsonNode responseNode = objectMapper.readTree(response.body().string());
            return responseNode.path("content").asText("");
        } finally {
            if (requestId != null) {
                activeRequests.remove(requestId);
            }
        }
    }

    @Override
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                      String systemPrompt, String requestId,
                                      Consumer<String> chunkConsumer) throws IOException {
        // For simplicity, use non-streaming for vision and emit result at once
        String result = chatWithVision(model, prompt, images, systemPrompt, requestId);
        chunkConsumer.accept(result);
    }

    @Override
    public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
        // External llama-server doesn't support model pulling
        progressConsumer.accept("{\"status\":\"error\",\"message\":\"llama-server unterstützt kein Model-Pulling. Laden Sie Modelle manuell beim Serverstart.\"}");
    }

    @Override
    public boolean deleteModel(String modelName) throws IOException {
        // External llama-server doesn't support model deletion
        log.warn("llama-server unterstützt keine Modell-Löschung");
        return false;
    }

    @Override
    public Map<String, Object> getModelDetails(String modelName) throws IOException {
        Map<String, Object> details = new HashMap<>();
        details.put("name", modelName);
        details.put("provider", PROVIDER_NAME);
        details.put("port", serverPort);
        details.put("description", "Modell auf Embedded llama-server");
        return details;
    }

    @Override
    public boolean cancelRequest(String requestId) {
        Call call = activeRequests.remove(requestId);
        if (call != null) {
            call.cancel();
            log.info("Cancelled request: {}", requestId);
            return true;
        }
        return false;
    }

    /**
     * Build prompt with system prompt support (ChatML format)
     */
    private String buildPrompt(String systemPrompt, String userPrompt) {
        StringBuilder sb = new StringBuilder();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append("<|im_start|>system\n");
            sb.append(systemPrompt);
            sb.append("<|im_end|>\n");
        }

        sb.append("<|im_start|>user\n");
        sb.append(userPrompt);
        sb.append("<|im_end|>\n");
        sb.append("<|im_start|>assistant\n");

        return sb.toString();
    }

    /**
     * Set the server port for the external llama-server
     */
    public void setServerPort(int port) {
        this.serverPort = port;
        log.info("External llama-server port changed to: {}", port);
    }

    /**
     * Get the current server port
     */
    public int getServerPort() {
        return serverPort;
    }
}
