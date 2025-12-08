package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.dto.ModelInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for Ollama API integration
 *
 * @author JavaFleet Systems Consulting
 * @since 0.3.2
 */
@Service
@Slf4j
public class OllamaService {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String ollamaBaseUrl;
    private final String defaultModel;

    // Track active requests for cancellation
    private final Map<String, Call> activeRequests = new ConcurrentHashMap<>();

    public OllamaService(LLMConfigProperties config) {
        this.ollamaBaseUrl = config.getOllama().getBaseUrl();
        this.defaultModel = config.getOllama().getDefaultModel();
        this.objectMapper = new ObjectMapper();

        int timeoutSeconds = config.getOllama().getTimeoutSeconds();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        log.info("🔮 OllamaService initialized with base URL: {}", ollamaBaseUrl);
    }

    /**
     * Send a chat message to Ollama and get response
     */
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        String url = ollamaBaseUrl + "/api/generate";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model != null ? model : defaultModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.put("system", systemPrompt);
        }

        String json = objectMapper.writeValueAsString(requestBody);

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
            log.debug("Tracking request: {}", requestId);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama API error: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            return jsonNode.get("response").asText();
        } catch (IOException e) {
            if (call.isCanceled()) {
                log.info("Request {} was cancelled", requestId);
                throw new IOException("Request was cancelled by user");
            }
            throw e;
        } finally {
            // Remove from active requests
            if (requestId != null) {
                activeRequests.remove(requestId);
                log.debug("Removed request: {}", requestId);
            }
        }
    }

    /**
     * Send a chat message to Ollama with STREAMING enabled
     * Calls the consumer for each chunk received
     */
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer) throws IOException {
        chatStream(model, prompt, systemPrompt, requestId, chunkConsumer, null, null, null, null, null, null);
    }

    /**
     * Send a chat message to Ollama with STREAMING enabled and custom parameters
     */
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature, Double topP, Integer topK,
                           Double repeatPenalty, Integer numCtx) throws IOException {
        // Delegate to full method with cpuOnly=false (default)
        chatStream(model, prompt, systemPrompt, requestId, chunkConsumer,
                maxTokens, temperature, topP, topK, repeatPenalty, numCtx, false);
    }

    /**
     * Send a chat message to Ollama with STREAMING enabled, custom parameters, and CPU-only option
     * @param cpuOnly wenn true, wird num_gpu=0 gesetzt (deaktiviert CUDA/GPU)
     */
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature, Double topP, Integer topK,
                           Double repeatPenalty, Integer numCtx, Boolean cpuOnly) throws IOException {
        // DEBUG: Log cpuOnly value
        log.info("🔍 DEBUG chatStream called - cpuOnly parameter value: {}", cpuOnly);

        String url = ollamaBaseUrl + "/api/generate";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model != null ? model : defaultModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", true);  // Enable streaming!

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.put("system", systemPrompt);
        }

        // Add model parameters (options)
        Map<String, Object> options = new HashMap<>();
        if (maxTokens != null && maxTokens > 0) {
            options.put("num_predict", maxTokens);  // CRITICAL: Controls max output length
        }
        if (temperature != null) {
            options.put("temperature", temperature);
        }
        if (topP != null) {
            options.put("top_p", topP);
        }
        if (topK != null) {
            options.put("top_k", topK);
        }
        if (repeatPenalty != null) {
            options.put("repeat_penalty", repeatPenalty);
        }
        if (numCtx != null && numCtx > 0) {
            options.put("num_ctx", numCtx);  // Context window size for expert
            log.info("📏 Setting num_ctx={} for model {}", numCtx, model);
        }

        // CPU-Only Mode: Deaktiviert CUDA/GPU für Demos auf Laptops ohne NVIDIA
        if (Boolean.TRUE.equals(cpuOnly)) {
            options.put("num_gpu", 0);
            log.info("🖥️ CPU-Only Mode aktiviert (num_gpu=0) - GPU/CUDA deaktiviert");
        }

        if (!options.isEmpty()) {
            requestBody.put("options", options);
            log.info("🔧 Ollama options: {}", options);  // INFO level for visibility
        } else {
            log.warn("⚠️  No Ollama options set - using defaults!");
        }

        String json = objectMapper.writeValueAsString(requestBody);

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
            log.debug("Tracking streaming request: {}", requestId);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama API error: " + response);
            }

            // Read the streaming response line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (call.isCanceled()) {
                        log.info("Streaming request {} was cancelled", requestId);
                        break;
                    }

                    // Parse each JSON line
                    JsonNode jsonNode = objectMapper.readTree(line);
                    String chunk = jsonNode.get("response").asText();

                    // Send chunk to consumer
                    chunkConsumer.accept(chunk);

                    // Check if done
                    if (jsonNode.has("done") && jsonNode.get("done").asBoolean()) {
                        log.debug("Streaming completed for request: {}", requestId);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            if (call.isCanceled()) {
                log.info("Streaming request {} was cancelled", requestId);
                throw new IOException("Request was cancelled by user");
            }
            throw e;
        } finally {
            // Remove from active requests
            if (requestId != null) {
                activeRequests.remove(requestId);
                log.debug("Removed streaming request: {}", requestId);
            }
        }
    }

    /**
     * Cancel an active request
     */
    public boolean cancelRequest(String requestId) {
        Call call = activeRequests.get(requestId);
        if (call != null && !call.isCanceled()) {
            call.cancel();
            activeRequests.remove(requestId);
            log.info("Cancelled request: {}", requestId);
            return true;
        }
        log.warn("Request {} not found or already completed", requestId);
        return false;
    }

    /**
     * Get list of available models from Ollama
     */
    public List<ModelInfo> getAvailableModels() throws IOException {
        String url = ollamaBaseUrl + "/api/tags";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch models: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            List<ModelInfo> models = new ArrayList<>();
            JsonNode modelsArray = jsonNode.get("models");

            if (modelsArray != null && modelsArray.isArray()) {
                for (JsonNode modelNode : modelsArray) {
                    ModelInfo info = new ModelInfo();
                    info.setName(modelNode.get("name").asText());
                    info.setSize(modelNode.has("size") ?
                        formatBytes(modelNode.get("size").asLong()) : "Unknown");
                    info.setModifiedAt(modelNode.has("modified_at") ?
                        modelNode.get("modified_at").asText() : null);
                    info.setDigest(modelNode.has("digest") ?
                        modelNode.get("digest").asText() : null);

                    models.add(info);
                }
            }

            return models;
        }
    }

    /**
     * Check if Ollama is available
     */
    public boolean isOllamaAvailable() {
        try {
            String url = ollamaBaseUrl + "/api/tags";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.error("Ollama not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Estimate token count (simple estimation: ~4 characters per token)
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    /**
     * Delete a model from Ollama
     */
    public boolean deleteModel(String modelName) throws IOException {
        String url = ollamaBaseUrl + "/api/delete";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", modelName);

        String json = objectMapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to delete model: " + errorBody);
            }
            log.info("Successfully deleted model: {}", modelName);
            return true;
        }
    }

    /**
     * Pull (download) a model from Ollama with streaming progress
     * Calls the consumer with progress updates
     * Calculates total progress across all layers
     */
    public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
        String url = ollamaBaseUrl + "/api/pull";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", modelName);
        requestBody.put("stream", true);  // Enable streaming for progress

        String json = objectMapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to pull model: " + errorBody);
            }

            // Track progress across all layers for total progress calculation
            Map<String, Long> layerTotals = new HashMap<>();
            Map<String, Long> layerCompleted = new HashMap<>();

            // Read the streaming response line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (call.isCanceled()) {
                        log.info("Model pull was cancelled for: {}", modelName);
                        break;
                    }

                    // Parse each JSON line for progress
                    JsonNode jsonNode = objectMapper.readTree(line);

                    // Get status and digest
                    String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "";
                    String digest = jsonNode.has("digest") ? jsonNode.get("digest").asText() : "";
                    String progressMsg = status;

                    // Track layer progress for downloading phases
                    if (jsonNode.has("completed") && jsonNode.has("total") && !digest.isEmpty()) {
                        long completed = jsonNode.get("completed").asLong();
                        long total = jsonNode.get("total").asLong();

                        // Update layer tracking
                        layerTotals.put(digest, total);
                        layerCompleted.put(digest, completed);

                        // Calculate total progress across all known layers
                        long totalBytes = layerTotals.values().stream().mapToLong(Long::longValue).sum();
                        long completedBytes = layerCompleted.values().stream().mapToLong(Long::longValue).sum();

                        if (totalBytes > 0) {
                            int totalPercent = (int) ((completedBytes * 100) / totalBytes);
                            // Format with human-readable sizes
                            String completedStr = formatBytes(completedBytes);
                            String totalStr = formatBytes(totalBytes);
                            progressMsg = String.format("downloading %s / %s (%d%%)", completedStr, totalStr, totalPercent);
                        }
                    } else if (status.equals("pulling manifest")) {
                        progressMsg = "pulling manifest (0%)";
                    } else if (status.contains("verifying")) {
                        progressMsg = "verifying... (95%)";
                    } else if (status.equals("writing manifest")) {
                        progressMsg = "writing manifest (98%)";
                    } else if (status.equals("success")) {
                        progressMsg = "success (100%)";
                    }

                    progressConsumer.accept(progressMsg);

                    // Check if done
                    if (status.equals("success")) {
                        log.info("Model pull completed for: {}", modelName);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get detailed information about a specific model
     */
    public Map<String, Object> getModelDetails(String modelName) throws IOException {
        String url = ollamaBaseUrl + "/api/show";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", modelName);

        String json = objectMapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get model details: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            Map<String, Object> details = new HashMap<>();
            details.put("name", modelName);

            if (jsonNode.has("modelfile")) {
                details.put("modelfile", jsonNode.get("modelfile").asText());
            }
            if (jsonNode.has("parameters")) {
                details.put("parameters", jsonNode.get("parameters").asText());
            }
            if (jsonNode.has("template")) {
                details.put("template", jsonNode.get("template").asText());
            }
            if (jsonNode.has("details")) {
                JsonNode detailsNode = jsonNode.get("details");
                details.put("format", detailsNode.has("format") ? detailsNode.get("format").asText() : null);
                details.put("family", detailsNode.has("family") ? detailsNode.get("family").asText() : null);
                details.put("parameter_size", detailsNode.has("parameter_size") ? detailsNode.get("parameter_size").asText() : null);
            }

            // Extract context_length from model_info
            if (jsonNode.has("model_info")) {
                JsonNode modelInfo = jsonNode.get("model_info");
                // Try different possible field names for context length
                if (modelInfo.has("context_length")) {
                    details.put("context_length", modelInfo.get("context_length").asInt());
                } else if (modelInfo.has("llama.context_length")) {
                    details.put("context_length", modelInfo.get("llama.context_length").asInt());
                } else if (modelInfo.has("gemma.context_length")) {
                    details.put("context_length", modelInfo.get("gemma.context_length").asInt());
                } else if (modelInfo.has("qwen2.context_length")) {
                    details.put("context_length", modelInfo.get("qwen2.context_length").asInt());
                }
            }

            return details;
        }
    }

    /**
     * Send a chat message to Ollama with Vision support (images)
     * Uses the /api/chat endpoint which supports multi-modal inputs
     */
    public String chatWithVision(String model, String prompt, List<String> images,
                                  String systemPrompt, String requestId) throws IOException {
        String url = ollamaBaseUrl + "/api/chat";

        // Build messages array for chat API
        List<Map<String, Object>> messages = new ArrayList<>();

        // Add system message if provided
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // Add user message with optional images
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        if (images != null && !images.isEmpty()) {
            userMsg.put("images", images);
            log.info("Sending {} images to vision model", images.size());
        }

        messages.add(userMsg);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model != null ? model : defaultModel);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        String json = objectMapper.writeValueAsString(requestBody);

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
            log.debug("Tracking vision request: {}", requestId);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama API error: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // Extract message content from chat response
            JsonNode messageNode = jsonNode.get("message");
            if (messageNode != null && messageNode.has("content")) {
                return messageNode.get("content").asText();
            }

            throw new IOException("Invalid response format from Ollama chat API");
        } catch (IOException e) {
            if (call.isCanceled()) {
                log.info("Vision request {} was cancelled", requestId);
                throw new IOException("Request was cancelled by user");
            }
            throw e;
        } finally {
            // Remove from active requests
            if (requestId != null) {
                activeRequests.remove(requestId);
                log.debug("Removed vision request: {}", requestId);
            }
        }
    }

    /**
     * Send a chat message to Ollama with Vision support (images) and STREAMING enabled
     */
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                      String systemPrompt, String requestId,
                                      Consumer<String> chunkConsumer) throws IOException {
        String url = ollamaBaseUrl + "/api/chat";

        // Build messages array for chat API
        List<Map<String, Object>> messages = new ArrayList<>();

        // Add system message if provided
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // Add user message with optional images
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        if (images != null && !images.isEmpty()) {
            userMsg.put("images", images);
            log.info("Streaming with {} images to vision model", images.size());
        }

        messages.add(userMsg);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model != null ? model : defaultModel);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);  // Enable streaming!

        String json = objectMapper.writeValueAsString(requestBody);

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
            log.debug("Tracking streaming vision request: {}", requestId);
        }

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama API error: " + response);
            }

            // Read the streaming response line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (call.isCanceled()) {
                        log.info("Streaming vision request {} was cancelled", requestId);
                        break;
                    }

                    // Parse each JSON line
                    JsonNode jsonNode = objectMapper.readTree(line);

                    // Extract content from message
                    JsonNode messageNode = jsonNode.get("message");
                    if (messageNode != null && messageNode.has("content")) {
                        String chunk = messageNode.get("content").asText();
                        chunkConsumer.accept(chunk);
                    }

                    // Check if done
                    if (jsonNode.has("done") && jsonNode.get("done").asBoolean()) {
                        log.debug("Streaming vision completed for request: {}", requestId);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            if (call.isCanceled()) {
                log.info("Streaming vision request {} was cancelled", requestId);
                throw new IOException("Request was cancelled by user");
            }
            throw e;
        } finally {
            // Remove from active requests
            if (requestId != null) {
                activeRequests.remove(requestId);
                log.debug("Removed streaming vision request: {}", requestId);
            }
        }
    }

    /**
     * Vision-Chaining: Verwende Vision Model für Bildbeschreibung, dann Haupt-Model für finale Antwort
     * Step 1: Vision Model analysiert Bild
     * Step 2: Haupt-Model erhält Vision-Output als Kontext
     */
    public String chatWithVisionChaining(String visionModel, String mainModel, String prompt,
                                          List<String> images, String systemPrompt,
                                          String requestId) throws IOException {
        log.info("Vision-Chaining: Vision={}, Main={}", visionModel, mainModel);

        // Step 1: Vision Model analysiert Bild - Interne Kommunikation (kann Englisch sein)
        String visionPrompt = "Describe in detail what you see in this image. " +
                "Be precise and structured. Provide a comprehensive technical description.\n\n" +
                "User question context: " + prompt;

        String visionOutput = chatWithVision(visionModel, visionPrompt, images, null, requestId + "-vision");
        log.info("Vision Model Output (first 100 chars): {}", visionOutput.substring(0, Math.min(100, visionOutput.length())));

        // Step 2: Haupt-Model erhält Vision-Output + Original-Prompt (AUSGABE AUF DEUTSCH!)
        String chainedPrompt = "WICHTIG: Deine Antwort MUSS auf Deutsch sein!\n\n" +
                "Bildinhalt:\n" + visionOutput + "\n\n" +
                "Frage des Nutzers: " + prompt + "\n\n" +
                "Antworte jetzt auf Deutsch:";

        // Kombiniere User-System-Prompt mit deutscher Ausgabe-Instruktion
        String finalSystemPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                ? "Du antwortest IMMER auf Deutsch.\n\n" + systemPrompt
                : "Du bist ein hilfreicher Assistent und antwortest IMMER auf Deutsch.";

        return chat(mainModel, chainedPrompt, finalSystemPrompt, requestId + "-main");
    }

    /**
     * Vision-Chaining mit STREAMING
     */
    public void chatStreamWithVisionChaining(String visionModel, String mainModel, String prompt,
                                              List<String> images, String systemPrompt,
                                              String requestId, Consumer<String> chunkConsumer) throws IOException {
        log.info("Vision-Chaining (Stream): Vision={}, Main={}", visionModel, mainModel);

        // Step 1: Vision Model analysiert Bild - Interne Kommunikation (kann Englisch sein)
        String visionPrompt = "Describe in detail what you see in this image. " +
                "Be precise and structured. Provide a comprehensive technical description.\n\n" +
                "User question context: " + prompt;

        String visionOutput = chatWithVision(visionModel, visionPrompt, images, null, requestId + "-vision");
        log.info("Vision Model Output (first 100 chars): {}", visionOutput.substring(0, Math.min(100, visionOutput.length())));

        // Optional: Sende Info an Frontend dass Vision-Step abgeschlossen ist
        chunkConsumer.accept("🔍 [Vision-Analyse abgeschlossen]\n\n");

        // Step 2: Haupt-Model erhält Vision-Output + Original-Prompt (AUSGABE AUF DEUTSCH!)
        String chainedPrompt = "WICHTIG: Deine Antwort MUSS auf Deutsch sein!\n\n" +
                "Bildinhalt:\n" + visionOutput + "\n\n" +
                "Frage des Nutzers: " + prompt + "\n\n" +
                "Antworte jetzt auf Deutsch:";

        // Kombiniere User-System-Prompt mit deutscher Ausgabe-Instruktion
        String finalSystemPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                ? "Du antwortest IMMER auf Deutsch.\n\n" + systemPrompt
                : "Du bist ein hilfreicher Assistent und antwortest IMMER auf Deutsch.";

        chatStream(mainModel, chainedPrompt, finalSystemPrompt, requestId + "-main", chunkConsumer);
    }

    /**
     * Get all available models from Ollama Library
     */
    public List<Map<String, Object>> getOllamaLibraryModels() throws IOException {
        String url = "https://ollama.com/api/tags";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Ollama Library: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            List<Map<String, Object>> models = new ArrayList<>();
            JsonNode modelsArray = jsonNode.get("models");

            if (modelsArray != null && modelsArray.isArray()) {
                for (JsonNode modelNode : modelsArray) {
                    Map<String, Object> model = new HashMap<>();
                    model.put("name", modelNode.get("name").asText());
                    model.put("model", modelNode.get("model").asText());

                    // Format size
                    if (modelNode.has("size")) {
                        long sizeBytes = modelNode.get("size").asLong();
                        model.put("size", formatBytes(sizeBytes));
                        model.put("sizeBytes", sizeBytes);
                    }

                    // Add modified date
                    if (modelNode.has("modified_at")) {
                        model.put("modifiedAt", modelNode.get("modified_at").asText());
                    }

                    // Add digest
                    if (modelNode.has("digest")) {
                        model.put("digest", modelNode.get("digest").asText());
                    }

                    models.add(model);
                }
            }

            log.info("Loaded {} models from Ollama Library", models.size());
            return models;
        }
    }

    /**
     * Create a custom model with parameters
     * Uses new Ollama 0.12+ API format with 'from', 'system', and 'parameters' fields
     */
    public void createModel(String modelName, String baseModel, String systemPrompt,
                           Double temperature, Double topP, Integer topK,
                           Double repeatPenalty, Integer numPredict, Integer numCtx,
                           Consumer<String> progressConsumer) throws IOException {
        String url = ollamaBaseUrl + "/api/create";

        // Build request body using new Ollama 0.12+ API format
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", modelName);
        requestBody.put("from", baseModel);

        // Add system prompt directly (not as Modelfile string)
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.put("system", systemPrompt);
        }

        // Add parameters as object (not as Modelfile PARAMETER lines)
        Map<String, Object> parameters = new HashMap<>();
        if (temperature != null) {
            parameters.put("temperature", temperature);
        }
        if (topP != null) {
            parameters.put("top_p", topP);
        }
        if (topK != null) {
            parameters.put("top_k", topK);
        }
        if (repeatPenalty != null) {
            parameters.put("repeat_penalty", repeatPenalty);
        }
        if (numPredict != null && numPredict > 0) {
            parameters.put("num_predict", numPredict);
        }
        if (numCtx != null && numCtx > 0) {
            parameters.put("num_ctx", numCtx);
        }

        if (!parameters.isEmpty()) {
            requestBody.put("parameters", parameters);
        }

        requestBody.put("stream", true);

        log.info("Creating model '{}' from base '{}' with {} parameters",
                modelName, baseModel, parameters.size());

        String json = objectMapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to create model: " + errorBody);
            }

            // Read the streaming response line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (call.isCanceled()) {
                        log.info("Model creation was cancelled for: {}", modelName);
                        break;
                    }

                    // Parse each JSON line for progress
                    JsonNode jsonNode = objectMapper.readTree(line);
                    String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "";

                    progressConsumer.accept(status);

                    // Check if done
                    if (jsonNode.has("status") && jsonNode.get("status").asText().equals("success")) {
                        log.info("Model creation completed for: {}", modelName);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Format bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
