package io.javafleet.fleetnavigator.llm;

import io.javafleet.fleetnavigator.llm.dto.ModelInfo;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Abstraktes Interface für alle LLM Provider
 *
 * Implementiert von:
 * - OllamaProvider (HTTP zu Ollama Server)
 * - LlamaCppProvider (Embedded llama-server)
 * - OpenAIProvider (OpenAI REST API) - optional
 *
 * CRITICAL für Native Image:
 * - Keine Reflection in Implementierungen
 * - Keine dynamischen Proxies
 * - Explizite Bean-Registrierung
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.7
 */
public interface LLMProvider {

    /**
     * Provider-Name (ollama, llamacpp, openai)
     *
     * @return Provider identifier
     */
    String getProviderName();

    /**
     * Prüft ob Provider verfügbar ist
     *
     * @return true wenn Provider einsatzbereit
     */
    boolean isAvailable();

    // ===== CHAT-GENERIERUNG =====

    /**
     * Generiert Text-Response (synchron)
     *
     * @param model Modell-Name
     * @param prompt User-Prompt
     * @param systemPrompt Optional: System-Prompt
     * @param requestId Request-ID für Tracking/Cancellation
     * @return Generierte Antwort
     * @throws IOException bei Netzwerk-/Provider-Fehlern
     */
    String chat(String model, String prompt, String systemPrompt,
                String requestId) throws IOException;

    /**
     * Generiert Text-Response (streaming)
     *
     * @param model Modell-Name
     * @param prompt User-Prompt
     * @param systemPrompt Optional: System-Prompt
     * @param requestId Request-ID für Tracking/Cancellation
     * @param chunkConsumer Consumer für Text-Chunks
     * @param maxTokens Max. Anzahl Tokens (null = unlimited)
     * @param temperature Temperatur (0.0 - 2.0, null = default)
     * @param topP Top-P Sampling (null = default)
     * @param topK Top-K Sampling (null = default)
     * @param repeatPenalty Repeat Penalty (null = default)
     * @param numCtx Context-Window Size (null = model default)
     * @throws IOException bei Netzwerk-/Provider-Fehlern
     */
    void chatStream(String model, String prompt, String systemPrompt,
                    String requestId, Consumer<String> chunkConsumer,
                    Integer maxTokens, Double temperature,
                    Double topP, Integer topK, Double repeatPenalty,
                    Integer numCtx) throws IOException;

    /**
     * Generiert Text-Response (streaming) mit CPU-Only Option
     *
     * @param model Modell-Name
     * @param prompt User-Prompt
     * @param systemPrompt Optional: System-Prompt
     * @param requestId Request-ID für Tracking/Cancellation
     * @param chunkConsumer Consumer für Text-Chunks
     * @param maxTokens Max. Anzahl Tokens (null = unlimited)
     * @param temperature Temperatur (0.0 - 2.0, null = default)
     * @param topP Top-P Sampling (null = default)
     * @param topK Top-K Sampling (null = default)
     * @param repeatPenalty Repeat Penalty (null = default)
     * @param numCtx Context-Window Size (null = model default)
     * @param cpuOnly Wenn true, wird GPU/CUDA deaktiviert (num_gpu=0)
     * @throws IOException bei Netzwerk-/Provider-Fehlern
     */
    default void chatStream(String model, String prompt, String systemPrompt,
                    String requestId, Consumer<String> chunkConsumer,
                    Integer maxTokens, Double temperature,
                    Double topP, Integer topK, Double repeatPenalty,
                    Integer numCtx, Boolean cpuOnly) throws IOException {
        // Default: Ignore cpuOnly flag and delegate to standard method
        chatStream(model, prompt, systemPrompt, requestId, chunkConsumer,
                maxTokens, temperature, topP, topK, repeatPenalty, numCtx);
    }

    // ===== VISION-SUPPORT =====

    /**
     * Vision Chat (Non-Streaming)
     *
     * @param model Modell-Name (muss Vision-Support haben)
     * @param prompt User-Prompt
     * @param images Base64-encoded Bilder
     * @param systemPrompt Optional: System-Prompt
     * @param requestId Request-ID für Tracking
     * @return Generierte Antwort
     * @throws IOException bei Fehlern
     */
    String chatWithVision(String model, String prompt, List<String> images,
                          String systemPrompt, String requestId) throws IOException;

    /**
     * Vision Chat (Streaming)
     *
     * @param model Modell-Name (muss Vision-Support haben)
     * @param prompt User-Prompt
     * @param images Base64-encoded Bilder
     * @param systemPrompt Optional: System-Prompt
     * @param requestId Request-ID
     * @param chunkConsumer Consumer für Text-Chunks
     * @throws IOException bei Fehlern
     */
    void chatStreamWithVision(String model, String prompt, List<String> images,
                              String systemPrompt, String requestId,
                              Consumer<String> chunkConsumer) throws IOException;

    // ===== MODELL-MANAGEMENT =====

    /**
     * Liste verfügbarer Modelle
     *
     * @return Liste von ModelInfo-Objekten
     * @throws IOException bei Fehlern
     */
    List<ModelInfo> getAvailableModels() throws IOException;

    /**
     * Modell herunterladen mit Progress
     *
     * @param modelName Name des Modells
     * @param progressConsumer Consumer für Progress-Updates (JSON-Strings)
     * @throws IOException bei Fehlern
     */
    void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException;

    /**
     * Modell löschen
     *
     * @param modelName Name des Modells
     * @return true wenn erfolgreich gelöscht
     * @throws IOException bei Fehlern
     */
    boolean deleteModel(String modelName) throws IOException;

    /**
     * Modell-Details abrufen
     *
     * @param modelName Name des Modells
     * @return Map mit Modell-Details (modelfile, parameters, etc.)
     * @throws IOException bei Fehlern
     */
    Map<String, Object> getModelDetails(String modelName) throws IOException;

    // ===== CUSTOM MODELS (OPTIONAL) =====

    /**
     * Custom Model erstellen
     *
     * Hinweis: Diese Funktion ist optional und wird nicht von allen Providern unterstützt.
     * Z.B. Ollama unterstützt Custom Models, llama.cpp nicht (dort einfach GGUF kopieren).
     *
     * @param modelName Name des neuen Modells
     * @param baseModel Basis-Modell
     * @param systemPrompt System-Prompt für Custom Model
     * @param temperature Optional: Temperature Override
     * @param topP Optional: Top-P Override
     * @param topK Optional: Top-K Override
     * @param repeatPenalty Optional: Repeat Penalty Override
     * @param progressConsumer Consumer für Progress-Updates
     * @throws IOException bei Fehlern
     * @throws UnsupportedOperationException wenn Provider keine Custom Models unterstützt
     */
    default void createModel(String modelName, String baseModel, String systemPrompt,
                             Double temperature, Double topP, Integer topK,
                             Double repeatPenalty, Consumer<String> progressConsumer) throws IOException {
        throw new UnsupportedOperationException(
            "Custom model creation not supported by " + getProviderName()
        );
    }

    // ===== REQUEST-MANAGEMENT =====

    /**
     * Request abbrechen
     *
     * @param requestId Request-ID die gecancelt werden soll
     * @return true wenn Request gefunden und gecancelt
     */
    boolean cancelRequest(String requestId);

    // ===== HILFSFUNKTIONEN =====

    /**
     * Token-Schätzung
     *
     * Einfache Heuristik: ~4 Zeichen pro Token
     *
     * @param text Text für den Tokens geschätzt werden sollen
     * @return Geschätzte Anzahl Tokens
     */
    default int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }

    // ===== FEATURE DETECTION =====

    /**
     * Check if this provider supports a specific feature.
     *
     * @param feature The feature to check
     * @return true if supported, false otherwise
     */
    default boolean supportsFeature(ProviderFeature feature) {
        // Default: Assume all features are supported (backwards compatibility)
        return true;
    }

    /**
     * Get all features supported by this provider.
     *
     * @return Set of supported features
     */
    default Set<ProviderFeature> getSupportedFeatures() {
        // Default: Return all features (backwards compatibility)
        return EnumSet.allOf(ProviderFeature.class);
    }
}
