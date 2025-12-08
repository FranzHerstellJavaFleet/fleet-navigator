package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.LLMProviderType;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.llm.providers.LlamaCppProvider;
import io.javafleet.fleetnavigator.llm.providers.JavaLlamaCppProvider;
import io.javafleet.fleetnavigator.llm.providers.OllamaProvider;
import io.javafleet.fleetnavigator.llm.providers.ExternalLlamaServerProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Zentrale Facade für alle LLM-Provider
 *
 * Verantwortlich für:
 * - Provider Auto-Detection
 * - Fallback-Logik
 * - Einheitliche API für alle Provider
 *
 * Native Image Considerations:
 * - Keine dynamische Provider-Suche via Reflection
 * - Explizite Constructor Injection
 * - Keine @Autowired auf Collections
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.7
 */
@Service
@Slf4j
public class LLMProviderService {

    private final Map<String, LLMProvider> providers;
    private final LLMConfigProperties config;
    private final SettingsService settingsService;
    private LLMProvider activeProvider;

    /**
     * Explizite Constructor Injection für Native Image
     *
     * WICHTIG: Keine @Autowired List<LLMProvider> - das nutzt Reflection!
     */
    public LLMProviderService(
            LlamaCppProvider llamaCppProvider,
            JavaLlamaCppProvider javaLlamaCppProvider,
            OllamaProvider ollamaProvider,
            ExternalLlamaServerProvider externalLlamaServerProvider,
            LLMConfigProperties config,
            SettingsService settingsService
    ) {
        // Manuelle Map-Erstellung statt Reflection
        // LinkedHashMap für definierte Reihenfolge: llama-server zuerst (Default für FleetCode)
        this.providers = new java.util.LinkedHashMap<>();
        providers.put("llama-server", externalLlamaServerProvider);  // External llama-server (FleetCode)
        providers.put("java-llama-cpp", javaLlamaCppProvider);  // JNI-based provider
        providers.put("llamacpp", llamaCppProvider);  // Server-based provider (legacy)
        providers.put("ollama", ollamaProvider);     // Ollama provider

        this.config = config;
        this.settingsService = settingsService;
        this.activeProvider = detectActiveProvider();
    }

    /**
     * Auto-Detection mit Fallback-Logik
     *
     * Priorität:
     * 1. Gespeicherter Provider aus Datenbank (persistiert über Neustarts)
     * 2. User-Präferenz aus Config (wenn verfügbar)
     * 3. java-llama-cpp (JNI-based, preferred)
     * 4. llamacpp (Server-based, legacy)
     * 5. Erster verfügbarer Provider
     */
    private LLMProvider detectActiveProvider() {
        // 1. Gespeicherter Provider aus Datenbank (höchste Priorität)
        String savedProvider = settingsService.getActiveProvider();
        if (savedProvider != null && !savedProvider.isEmpty()) {
            LLMProvider provider = providers.get(savedProvider.toLowerCase());
            if (provider != null && provider.isAvailable()) {
                log.info("✅ Using saved provider from database: {}", savedProvider);
                return provider;
            } else {
                log.warn("⚠️  Saved provider '{}' not available, falling back to auto-detection", savedProvider);
            }
        }

        // 2. User-Präferenz aus Config
        String defaultProviderConfig = config.getDefaultProvider();
        if (!"auto".equalsIgnoreCase(defaultProviderConfig)) {
            LLMProvider preferred = providers.get(defaultProviderConfig.toLowerCase());
            if (preferred != null && preferred.isAvailable()) {
                log.info("✅ Using configured provider: {}", defaultProviderConfig);
                return preferred;
            } else {
                log.warn("⚠️  Configured provider '{}' not available, falling back to auto-detection",
                        defaultProviderConfig);
            }
        }

        // 3. java-llama-cpp (JNI-based) als bevorzugter Provider
        LLMProvider javaLlamacpp = providers.get("java-llama-cpp");
        if (javaLlamacpp != null && javaLlamacpp.isAvailable()) {
            log.info("✅ Using java-llama-cpp provider (default)");
            return javaLlamacpp;
        }

        // 4. llama.cpp (server-based) als Fallback
        LLMProvider llamacpp = providers.get("llamacpp");
        if (llamacpp != null && llamacpp.isAvailable()) {
            log.info("✅ Using llama.cpp server provider (fallback)");
            return llamacpp;
        }

        // 5. Fallback auf ersten verfügbaren Provider
        LLMProvider fallback = providers.values().stream()
                .filter(LLMProvider::isAvailable)
                .findFirst()
                .orElse(null);

        if (fallback != null) {
            log.info("✅ Using fallback provider: {}", fallback.getProviderName());
            return fallback;
        }

        log.error("❌ No LLM provider available!");
        throw new IllegalStateException("No LLM provider available. Please configure llama.cpp.");
    }

    // ===== CHAT-GENERIERUNG =====

    /**
     * Generiert Text mit aktivem Provider (non-streaming)
     */
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        return activeProvider.chat(model, prompt, systemPrompt, requestId);
    }

    /**
     * Streaming-Generierung mit aktivem Provider
     */
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature,
                           Double topP, Integer topK, Double repeatPenalty,
                           Integer numCtx) throws IOException {
        chatStream(model, prompt, systemPrompt, requestId, chunkConsumer,
                maxTokens, temperature, topP, topK, repeatPenalty, numCtx, false);
    }

    /**
     * Streaming-Generierung mit aktivem Provider und CPU-Only Option
     * @param cpuOnly wenn true, wird GPU/CUDA deaktiviert (für Demos auf Laptops ohne NVIDIA)
     */
    public void chatStream(String model, String prompt, String systemPrompt,
                           String requestId, Consumer<String> chunkConsumer,
                           Integer maxTokens, Double temperature,
                           Double topP, Integer topK, Double repeatPenalty,
                           Integer numCtx, Boolean cpuOnly) throws IOException {
        activeProvider.chatStream(model, prompt, systemPrompt, requestId, chunkConsumer,
                maxTokens, temperature, topP, topK, repeatPenalty, numCtx, cpuOnly);
    }

    // ===== VISION-SUPPORT =====

    /**
     * Vision Chat (non-streaming)
     */
    public String chatWithVision(String model, String prompt, List<String> images,
                                  String systemPrompt, String requestId) throws IOException {
        return activeProvider.chatWithVision(model, prompt, images, systemPrompt, requestId);
    }

    /**
     * Result-Objekt für Vision-Chaining - enthält beide Ergebnisse
     */
    public record VisionChainResult(String response, String visionOutput) {}

    /**
     * Vision-Chaining (non-streaming): 2-Step Process
     * Step 1: Vision Model analysiert Bild
     * Step 2: Haupt-Model erhält Vision-Output als Kontext
     *
     * @return VisionChainResult mit finaler Antwort UND Vision/OCR-Ergebnis
     */
    public VisionChainResult chatWithVisionChainingFull(String visionModel, String mainModel, String prompt,
                                          List<String> images, String systemPrompt,
                                          String requestId) throws IOException {
        log.info("Vision-Chaining (Non-Stream): Vision={}, Main={}", visionModel, mainModel);

        // Step 1: Vision Model analysiert Bild - OCR-optimierter Prompt für Dokumente
        String visionPrompt = "This image contains a document. Your task is to:\n" +
                "1. READ and TRANSCRIBE all visible text exactly as written\n" +
                "2. Preserve the document structure (headings, paragraphs, lists)\n" +
                "3. Include ALL text, numbers, dates, and names you can see\n" +
                "4. If text is unclear, indicate with [unclear]\n\n" +
                "IMPORTANT: Extract the COMPLETE text content, not just a description.\n\n" +
                "User's question about this document: " + prompt;

        String visionOutput = chatWithVision(visionModel, visionPrompt, images, null, requestId + "-vision");
        log.info("Vision Model Output (first 100 chars): {}",
                visionOutput.substring(0, Math.min(100, visionOutput.length())));

        // Step 2: Haupt-Model erhält Vision-Output + Original-Prompt (STRIKTE DEUTSCHE AUSGABE!)
        String chainedPrompt = "STRIKT: Antworte NUR auf Deutsch! NIEMALS auf Chinesisch oder Englisch!\n\n" +
                "=== DOKUMENTINHALT (aus Bildanalyse) ===\n" + visionOutput + "\n" +
                "=== ENDE DOKUMENTINHALT ===\n\n" +
                "Frage des Nutzers: " + prompt + "\n\n" +
                "Deine Antwort (auf Deutsch):";

        // Kombiniere User-System-Prompt mit STRIKTER deutscher Ausgabe-Instruktion
        String germanEnforcement = "WICHTIG: Du antwortest AUSSCHLIESSLICH auf Deutsch. " +
                "Keine anderen Sprachen erlaubt - kein Chinesisch, kein Englisch!\n\n";
        String finalSystemPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                ? germanEnforcement + systemPrompt
                : germanEnforcement + "Du bist ein hilfreicher Assistent.";

        String response = chat(mainModel, chainedPrompt, finalSystemPrompt, requestId + "-main");
        return new VisionChainResult(response, visionOutput);
    }

    /**
     * Vision-Chaining (non-streaming): 2-Step Process (Legacy - ohne VisionOutput Rückgabe)
     * Step 1: Vision Model analysiert Bild
     * Step 2: Haupt-Model erhält Vision-Output als Kontext
     */
    public String chatWithVisionChaining(String visionModel, String mainModel, String prompt,
                                          List<String> images, String systemPrompt,
                                          String requestId) throws IOException {
        log.info("Vision-Chaining (Non-Stream): Vision={}, Main={}", visionModel, mainModel);

        // Step 1: Vision Model analysiert Bild - OCR-optimierter Prompt für Dokumente
        String visionPrompt = "This image contains a document. Your task is to:\n" +
                "1. READ and TRANSCRIBE all visible text exactly as written\n" +
                "2. Preserve the document structure (headings, paragraphs, lists)\n" +
                "3. Include ALL text, numbers, dates, and names you can see\n" +
                "4. If text is unclear, indicate with [unclear]\n\n" +
                "IMPORTANT: Extract the COMPLETE text content, not just a description.\n\n" +
                "User's question about this document: " + prompt;

        String visionOutput = chatWithVision(visionModel, visionPrompt, images, null, requestId + "-vision");
        log.info("Vision Model Output (first 100 chars): {}",
                visionOutput.substring(0, Math.min(100, visionOutput.length())));

        // Step 2: Haupt-Model erhält Vision-Output + Original-Prompt (STRIKTE DEUTSCHE AUSGABE!)
        String chainedPrompt = "STRIKT: Antworte NUR auf Deutsch! NIEMALS auf Chinesisch oder Englisch!\n\n" +
                "=== DOKUMENTINHALT (aus Bildanalyse) ===\n" + visionOutput + "\n" +
                "=== ENDE DOKUMENTINHALT ===\n\n" +
                "Frage des Nutzers: " + prompt + "\n\n" +
                "Deine Antwort (auf Deutsch):";

        // Kombiniere User-System-Prompt mit STRIKTER deutscher Ausgabe-Instruktion
        String germanEnforcement = "WICHTIG: Du antwortest AUSSCHLIESSLICH auf Deutsch. " +
                "Keine anderen Sprachen erlaubt - kein Chinesisch, kein Englisch!\n\n";
        String finalSystemPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                ? germanEnforcement + systemPrompt
                : germanEnforcement + "Du bist ein hilfreicher Assistent.";

        return chat(mainModel, chainedPrompt, finalSystemPrompt, requestId + "-main");
    }

    /**
     * Vision Chat (streaming)
     */
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                      String systemPrompt, String requestId,
                                      Consumer<String> chunkConsumer) throws IOException {
        activeProvider.chatStreamWithVision(model, prompt, images, systemPrompt, requestId, chunkConsumer);
    }

    /**
     * Vision-Chaining: 2-Step Process
     * Step 1: Vision Model analysiert Bild
     * Step 2: Haupt-Model erhält Vision-Output als Kontext
     *
     * @param showIntermediateOutput If true, send vision output to frontend
     * @param visionOutputConsumer Optional callback to receive the vision/OCR output for storage
     */
    public void chatStreamWithVisionChaining(String visionModel, String mainModel, String prompt,
                                              List<String> images, String systemPrompt,
                                              String requestId, Consumer<String> chunkConsumer,
                                              boolean showIntermediateOutput,
                                              Consumer<String> visionOutputConsumer) throws IOException {
        log.info("Vision-Chaining (Stream): Vision={}, Main={}, ShowIntermediate={}", visionModel, mainModel, showIntermediateOutput);

        // Step 1: Vision Model analysiert Bild - OCR-optimierter Prompt für Dokumente
        String visionPrompt = "This image contains a document. Your task is to:\n" +
                "1. READ and TRANSCRIBE all visible text exactly as written\n" +
                "2. Preserve the document structure (headings, paragraphs, lists)\n" +
                "3. Include ALL text, numbers, dates, and names you can see\n" +
                "4. If text is unclear, indicate with [unclear]\n\n" +
                "IMPORTANT: Extract the COMPLETE text content, not just a description.\n\n" +
                "User's question about this document: " + prompt;

        String visionOutput = chatWithVision(visionModel, visionPrompt, images, null, requestId + "-vision");
        log.info("Vision Model Output (first 100 chars): {}",
                visionOutput.substring(0, Math.min(100, visionOutput.length())));

        // Callback für Vision/OCR-Ergebnis (zur Speicherung in DB)
        if (visionOutputConsumer != null) {
            visionOutputConsumer.accept(visionOutput);
        }

        // Sende Vision-Output an Frontend (wenn aktiviert)
        if (showIntermediateOutput) {
            chunkConsumer.accept("🔍 **Vision-Modell (" + visionModel + ") Analyse:**\n\n");
            chunkConsumer.accept(visionOutput + "\n\n");
            chunkConsumer.accept("---\n\n");
            chunkConsumer.accept("💬 **Antwort des Haupt-Modells:**\n\n");
        }

        // Step 2: Haupt-Model erhält Vision-Output + Original-Prompt (STRIKTE DEUTSCHE AUSGABE!)
        String chainedPrompt = "STRIKT: Antworte NUR auf Deutsch! NIEMALS auf Chinesisch oder Englisch!\n\n" +
                "=== DOKUMENTINHALT (aus Bildanalyse) ===\n" + visionOutput + "\n" +
                "=== ENDE DOKUMENTINHALT ===\n\n" +
                "Frage des Nutzers: " + prompt + "\n\n" +
                "Deine Antwort (auf Deutsch):";

        // Kombiniere User-System-Prompt mit STRIKTER deutscher Ausgabe-Instruktion
        String germanEnforcement = "WICHTIG: Du antwortest AUSSCHLIESSLICH auf Deutsch. " +
                "Keine anderen Sprachen erlaubt - kein Chinesisch, kein Englisch!\n\n";
        String finalSystemPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                ? germanEnforcement + systemPrompt
                : germanEnforcement + "Du bist ein hilfreicher Assistent.";

        chatStream(mainModel, chainedPrompt, finalSystemPrompt, requestId + "-main", chunkConsumer,
                null, null, null, null, null, null);
    }

    /**
     * Vision-Chaining: 2-Step Process (Legacy ohne visionOutputConsumer)
     */
    public void chatStreamWithVisionChaining(String visionModel, String mainModel, String prompt,
                                              List<String> images, String systemPrompt,
                                              String requestId, Consumer<String> chunkConsumer,
                                              boolean showIntermediateOutput) throws IOException {
        chatStreamWithVisionChaining(visionModel, mainModel, prompt, images, systemPrompt,
                requestId, chunkConsumer, showIntermediateOutput, null);
    }

    // ===== MODELL-MANAGEMENT =====

    /**
     * Alle verfügbaren Modelle (vom aktiven Provider)
     */
    public List<ModelInfo> getAvailableModels() throws IOException {
        return activeProvider.getAvailableModels();
    }

    /**
     * Alle verfügbaren Modelle (alle Provider)
     */
    public List<ModelInfo> getAllModels() throws IOException {
        List<ModelInfo> allModels = new ArrayList<>();

        for (LLMProvider provider : providers.values()) {
            if (provider.isAvailable()) {
                try {
                    allModels.addAll(provider.getAvailableModels());
                } catch (IOException e) {
                    log.warn("Failed to get models from {}: {}", provider.getProviderName(), e.getMessage());
                }
            }
        }

        return allModels;
    }

    /**
     * Modell herunterladen (aktiver Provider)
     */
    public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
        activeProvider.pullModel(modelName, progressConsumer);
    }

    /**
     * Modell löschen (aktiver Provider)
     */
    public boolean deleteModel(String modelName) throws IOException {
        return activeProvider.deleteModel(modelName);
    }

    /**
     * Modell-Details (aktiver Provider)
     */
    public Map<String, Object> getModelDetails(String modelName) throws IOException {
        return activeProvider.getModelDetails(modelName);
    }

    /**
     * Custom Model erstellen (aktiver Provider)
     */
    public void createModel(String modelName, String baseModel, String systemPrompt,
                            Double temperature, Double topP, Integer topK,
                            Double repeatPenalty, Consumer<String> progressConsumer) throws IOException {
        activeProvider.createModel(modelName, baseModel, systemPrompt,
                temperature, topP, topK, repeatPenalty, progressConsumer);
    }

    // ===== REQUEST-MANAGEMENT =====

    /**
     * Request abbrechen (aktiver Provider)
     */
    public boolean cancelRequest(String requestId) {
        return activeProvider.cancelRequest(requestId);
    }

    // ===== HILFSFUNKTIONEN =====

    /**
     * Token-Schätzung
     */
    public int estimateTokens(String text) {
        return activeProvider.estimateTokens(text);
    }

    // ===== PROVIDER-MANAGEMENT =====

    /**
     * Provider manuell wechseln und in Datenbank speichern
     *
     * @param providerName Name des Providers (java-llama-cpp, llamacpp)
     * @throws IllegalArgumentException wenn Provider nicht existiert
     * @throws IllegalStateException wenn Provider nicht verfügbar
     */
    public void switchProvider(String providerName) {
        LLMProvider newProvider = providers.get(providerName.toLowerCase());

        if (newProvider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }

        if (!newProvider.isAvailable()) {
            throw new IllegalStateException("Provider not available: " + providerName);
        }

        this.activeProvider = newProvider;

        // Persistiere die Wahl in der Datenbank
        settingsService.saveActiveProvider(providerName.toLowerCase());

        log.info("🔄 Switched to provider: {} (saved to database)", providerName);
    }

    /**
     * Aktiver Provider
     */
    public LLMProvider getActiveProvider() {
        return activeProvider;
    }

    /**
     * Aktiver Provider Name
     */
    public String getActiveProviderName() {
        return activeProvider.getProviderName();
    }

    /**
     * Gibt Default-Modell mit Fallback zurück
     *
     * Option A: Wenn Default-Modell nicht verfügbar ist, nimm erstes verfügbares Modell
     *
     * @param preferredModel Das bevorzugte Modell (kann null sein)
     * @return Verfügbares Modell oder null wenn keine Modelle verfügbar
     */
    public String getDefaultModelWithFallback(String preferredModel) {
        try {
            List<ModelInfo> availableModels = activeProvider.getAvailableModels();

            if (availableModels.isEmpty()) {
                log.warn("⚠️  No models available for provider: {}", activeProvider.getProviderName());
                return null;
            }

            // 1. Versuche das bevorzugte Modell
            if (preferredModel != null && !preferredModel.isEmpty()) {
                boolean modelExists = availableModels.stream()
                    .anyMatch(m -> m.getName().equals(preferredModel));

                if (modelExists) {
                    log.debug("✅ Using preferred model: {}", preferredModel);
                    return preferredModel;
                } else {
                    log.warn("⚠️  Preferred model '{}' not found, using first available model", preferredModel);
                }
            }

            // 2. Fallback: Nimm erstes verfügbares Modell
            String fallbackModel = availableModels.get(0).getName();
            log.info("🔄 Using fallback model: {} (from {} available models)",
                fallbackModel, availableModels.size());
            return fallbackModel;

        } catch (IOException e) {
            log.error("❌ Failed to get available models: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Status aller Provider
     *
     * @return Map mit Provider-Namen und Verfügbarkeit
     */
    public Map<String, Boolean> getProviderStatus() {
        Map<String, Boolean> status = new HashMap<>();

        for (Map.Entry<String, LLMProvider> entry : providers.entrySet()) {
            status.put(entry.getKey(), entry.getValue().isAvailable());
        }

        return status;
    }

    /**
     * Liste aller Provider
     */
    public List<String> getAvailableProviders() {
        return providers.values().stream()
                .filter(LLMProvider::isAvailable)
                .map(LLMProvider::getProviderName)
                .collect(Collectors.toList());
    }

    /**
     * Provider availability check für System Health
     */
    public boolean isAnyProviderAvailable() {
        return providers.values().stream()
                .anyMatch(LLMProvider::isAvailable);
    }

    /**
     * Provider nach Name holen
     */
    public LLMProvider getProvider(String providerName) {
        return providers.get(providerName.toLowerCase());
    }
}
