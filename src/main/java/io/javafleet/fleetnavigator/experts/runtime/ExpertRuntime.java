package io.javafleet.fleetnavigator.experts.runtime;

import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.model.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runtime-Repräsentation eines Experten.
 *
 * Kapselt alle Informationen die für die Chat-Generierung nötig sind:
 * - LLM Provider (java-llama-cpp, ollama)
 * - Modell (aufgelöster Pfad)
 * - Prompts (System + Personality + aktiver Modus)
 * - Inference-Parameter (Temperature, TopP, etc.)
 *
 * Der Expert ist jetzt ein vollständiges Objekt das direkt
 * für Chat-Anfragen verwendet werden kann.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@Slf4j
@Getter
public class ExpertRuntime {

    // ===== Identität =====
    private final Long expertId;
    private final String name;
    private final String role;
    private final String avatarUrl;

    // ===== LLM Konfiguration =====
    private final LLMProvider provider;
    private final String providerName;
    private final String modelName;           // Original-Modellname
    private final Path resolvedModelPath;     // Aufgelöster Pfad (für GGUF)

    // ===== Prompts =====
    private final String systemPrompt;        // Kombinierter System-Prompt
    private final String activeModeName;      // Name des aktiven Modus (oder null)

    // ===== Inference Parameter =====
    private final Double temperature;
    private final Double topP;
    private final Integer topK;
    private final Double repeatPenalty;
    private final Integer maxTokens;
    private final Integer contextSize;
    private final Boolean cpuOnly;

    // ===== Web/File Search =====
    private final Boolean autoWebSearch;
    private final List<String> searchDomains;
    private final Integer maxSearchResults;
    private final Boolean autoFileSearch;

    /**
     * Privater Konstruktor - Verwendung über ExpertRuntimeFactory
     */
    ExpertRuntime(Expert expert, ExpertMode activeMode, LLMProvider provider,
                  Path resolvedModelPath, Boolean cpuOnly) {
        // Identität
        this.expertId = expert.getId();
        this.name = expert.getName();
        this.role = expert.getRole();
        this.avatarUrl = expert.getAvatarUrl();

        // Provider & Modell
        this.provider = provider;
        this.providerName = provider.getProviderName();
        this.modelName = resolveModelName(expert, provider);
        this.resolvedModelPath = resolvedModelPath;

        // System-Prompt zusammenbauen
        this.systemPrompt = buildSystemPrompt(expert, activeMode);
        this.activeModeName = activeMode != null ? activeMode.getName() : null;

        // Inference Parameter (Modus überschreibt Expert-Defaults)
        this.temperature = resolveParameter(
            activeMode != null ? activeMode.getTemperature() : null,
            expert.getDefaultTemperature(),
            0.7
        );
        this.topP = resolveParameter(
            activeMode != null ? activeMode.getTopP() : null,
            expert.getDefaultTopP(),
            0.9
        );
        this.topK = 40; // Standard
        this.repeatPenalty = 1.18; // Standard
        this.maxTokens = expert.getDefaultMaxTokens() != null ? expert.getDefaultMaxTokens() : 4096;
        this.contextSize = expert.getDefaultNumCtx() != null ? expert.getDefaultNumCtx() : 8192;
        this.cpuOnly = cpuOnly != null ? cpuOnly : false;

        // Web/File Search
        this.autoWebSearch = expert.getAutoWebSearch();
        this.searchDomains = expert.getSearchDomainsAsList();
        this.maxSearchResults = expert.getMaxSearchResults();
        this.autoFileSearch = expert.getAutoFileSearch();

        log.info("🎓 ExpertRuntime erstellt: {} ({}) mit {} auf {}",
                name, role, providerName, modelName);
    }

    /**
     * Chat mit Streaming
     *
     * @param userMessage Benutzer-Nachricht
     * @param history Chat-Historie (für Kontext)
     * @param chunkConsumer Consumer für Text-Chunks
     * @return Request-ID für Tracking/Cancellation
     */
    public String chatStream(String userMessage, List<Message> history,
                              Consumer<String> chunkConsumer) throws IOException {
        String requestId = UUID.randomUUID().toString();

        // Prompt mit Historie aufbauen
        String fullPrompt = buildPromptWithHistory(userMessage, history);

        log.debug("🎓 {} chatStream: {} Zeichen Prompt, {} Historie-Nachrichten",
                name, fullPrompt.length(), history != null ? history.size() : 0);

        // Model-Name für Provider (GGUF-Pfad oder Ollama-Name)
        String modelForProvider = resolvedModelPath != null
            ? resolvedModelPath.toString()
            : modelName;

        // An Provider delegieren
        provider.chatStream(
            modelForProvider,
            fullPrompt,
            systemPrompt,
            requestId,
            chunkConsumer,
            maxTokens,
            temperature,
            topP,
            topK,
            repeatPenalty,
            contextSize,
            cpuOnly
        );

        return requestId;
    }

    /**
     * Chat ohne Streaming (blockierend)
     */
    public String chat(String userMessage, List<Message> history) throws IOException {
        String requestId = UUID.randomUUID().toString();
        String fullPrompt = buildPromptWithHistory(userMessage, history);

        String modelForProvider = resolvedModelPath != null
            ? resolvedModelPath.toString()
            : modelName;

        return provider.chat(modelForProvider, fullPrompt, systemPrompt, requestId);
    }

    /**
     * Bricht laufenden Request ab
     */
    public boolean cancelRequest(String requestId) {
        return provider.cancelRequest(requestId);
    }

    // ===== Private Hilfsmethoden =====

    private String resolveModelName(Expert expert, LLMProvider provider) {
        // Für java-llama-cpp: GGUF-Modell bevorzugen
        if ("java-llama-cpp".equals(provider.getProviderName())) {
            if (expert.getGgufModel() != null && !expert.getGgufModel().isBlank()) {
                return expert.getGgufModel();
            }
        }
        // Fallback: baseModel (für Ollama)
        return expert.getBaseModel();
    }

    private String buildSystemPrompt(Expert expert, ExpertMode activeMode) {
        StringBuilder sb = new StringBuilder();

        // Basis-Prompt
        sb.append(expert.getCombinedSystemPrompt());

        // Modus-Prompt anhängen wenn aktiv
        if (activeMode != null && activeMode.getPromptAddition() != null
            && !activeMode.getPromptAddition().isBlank()) {
            sb.append("\n\n## Aktueller Blickwinkel: ").append(activeMode.getName());
            sb.append("\n").append(activeMode.getPromptAddition());
        }

        return sb.toString();
    }

    private String buildPromptWithHistory(String userMessage, List<Message> history) {
        if (history == null || history.isEmpty()) {
            return userMessage;
        }

        StringBuilder sb = new StringBuilder();

        // Letzte N Nachrichten als Kontext
        int maxHistory = 10;
        int start = Math.max(0, history.size() - maxHistory);

        for (int i = start; i < history.size(); i++) {
            Message msg = history.get(i);
            String roleLabel = Message.MessageRole.USER.equals(msg.getRole()) ? "Benutzer" : name;
            sb.append(roleLabel).append(": ").append(msg.getContent()).append("\n\n");
        }

        sb.append("Benutzer: ").append(userMessage);

        return sb.toString();
    }

    private Double resolveParameter(Double modeValue, Double expertDefault, double fallback) {
        if (modeValue != null) return modeValue;
        if (expertDefault != null) return expertDefault;
        return fallback;
    }

    @Override
    public String toString() {
        return String.format("ExpertRuntime[%s (%s) via %s with %s]",
                name, role, providerName, modelName);
    }
}
