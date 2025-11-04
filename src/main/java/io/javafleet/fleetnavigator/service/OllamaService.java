package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.dto.ModelInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    public OllamaService(
            @Value("${ollama.base-url}") String ollamaBaseUrl,
            @Value("${ollama.timeout-seconds:300}") int timeoutSeconds,
            @Value("${ollama.default-model}") String defaultModel) {

        this.ollamaBaseUrl = ollamaBaseUrl;
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        log.info("OllamaService initialized with base URL: {}", ollamaBaseUrl);
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
        chatStream(model, prompt, systemPrompt, requestId, chunkConsumer, null, null, null, null, null);
    }

    /**
     * Send a chat message to Ollama with STREAMING enabled and custom parameters
     */
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature, Double topP, Integer topK, Double repeatPenalty) throws IOException {
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

                    // Send progress update to consumer
                    String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "";
                    String progressMsg = status;

                    // Include completion percentage if available
                    if (jsonNode.has("completed") && jsonNode.has("total")) {
                        long completed = jsonNode.get("completed").asLong();
                        long total = jsonNode.get("total").asLong();
                        if (total > 0) {
                            int percent = (int) ((completed * 100) / total);
                            progressMsg = status + " (" + percent + "%)";
                        }
                    }

                    progressConsumer.accept(progressMsg);

                    // Check if done
                    if (jsonNode.has("status") && jsonNode.get("status").asText().equals("success")) {
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
     * Format bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Create a custom model using Ollama's create API with streaming progress
     * @param modelName Name for the new model (e.g. "nova:latest")
     * @param modelfile Complete Modelfile content
     * @param progressConsumer Consumer for progress updates
     */
    public void createModel(String modelName, String baseModel, String systemPrompt,
                            Double temperature, Double topP, Integer topK, Double repeatPenalty,
                            Consumer<String> progressConsumer) throws IOException {
        String url = ollamaBaseUrl + "/api/create";

        // NEW Ollama API format (since v0.5.5): separate fields instead of modelfile string
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", modelName);
        requestBody.put("from", baseModel);  // REQUIRED: base model
        requestBody.put("stream", true);

        // Add optional system prompt
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            requestBody.put("system", systemPrompt);
        }

        // Add optional parameters as nested map
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
        if (!parameters.isEmpty()) {
            requestBody.put("parameter", parameters);
        }

        String json = objectMapper.writeValueAsString(requestBody);

        log.info("Creating model '{}' from base '{}'", modelName, baseModel);
        log.debug("Request body: {}", json);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama create model error: " + response);
            }

            // Read streaming response
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    JsonNode jsonNode = objectMapper.readTree(line);

                    // Check for errors first!
                    if (jsonNode.has("error")) {
                        String error = jsonNode.get("error").asText();
                        log.error("Ollama model creation failed: {}", error);
                        throw new IOException("Ollama model creation failed: " + error);
                    }

                    // Extract status message
                    if (jsonNode.has("status")) {
                        String status = jsonNode.get("status").asText();
                        progressConsumer.accept(status);
                        log.debug("Create model progress: {}", status);
                    }

                    // Check if done
                    if (jsonNode.has("done") && jsonNode.get("done").asBoolean()) {
                        log.info("Model creation completed: {}", modelName);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error creating model: {}", modelName, e);
            throw e;
        }
    }
}
