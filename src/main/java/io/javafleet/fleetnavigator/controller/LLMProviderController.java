package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.service.LLMProviderService;
import io.javafleet.fleetnavigator.service.SettingsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller für LLM Provider Management
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.7
 */
@RestController
@RequestMapping("/api/llm/providers")
@RequiredArgsConstructor
@Slf4j
public class LLMProviderController {

    private final LLMProviderService llmProviderService;
    private final LLMConfigProperties config;
    private final SettingsService settingsService;

    /**
     * Gibt alle verfügbaren Provider mit Status zurück
     */
    @GetMapping
    public ResponseEntity<ProvidersResponse> getProviders() {
        Map<String, Boolean> providerStatus = llmProviderService.getProviderStatus();
        String activeProvider = llmProviderService.getActiveProviderName();

        ProvidersResponse response = new ProvidersResponse();
        response.setActiveProvider(activeProvider);
        response.setAvailableProviders(llmProviderService.getAvailableProviders());
        response.setProviderStatus(providerStatus);

        return ResponseEntity.ok(response);
    }

    /**
     * Gibt aktiven Provider zurück
     */
    @GetMapping("/active")
    public ResponseEntity<ActiveProviderResponse> getActiveProvider() {
        String activeProvider = llmProviderService.getActiveProviderName();
        boolean available = llmProviderService.getActiveProvider().isAvailable();

        ActiveProviderResponse response = new ActiveProviderResponse();
        response.setProvider(activeProvider);
        response.setAvailable(available);

        return ResponseEntity.ok(response);
    }

    /**
     * Wechselt den aktiven Provider
     */
    @PostMapping("/switch")
    public ResponseEntity<SwitchResponse> switchProvider(@RequestBody SwitchRequest request) {
        try {
            String oldProvider = llmProviderService.getActiveProviderName();
            String oldModel = settingsService.getSelectedModel();

            llmProviderService.switchProvider(request.getProvider());
            String newProvider = llmProviderService.getActiveProviderName();

            // Intelligente Modellauswahl beim Provider-Wechsel
            String newModel = selectModelForNewProvider(oldModel, newProvider);
            if (newModel != null) {
                settingsService.saveSelectedModel(newModel);
                log.info("Auto-selected model '{}' for provider '{}'", newModel, newProvider);
            }

            log.info("Provider switched from {} to {}", oldProvider, newProvider);

            SwitchResponse response = new SwitchResponse();
            response.setSuccess(true);
            response.setOldProvider(oldProvider);
            response.setNewProvider(newProvider);
            response.setSelectedModel(newModel);
            response.setMessage("Provider erfolgreich gewechselt zu: " + newProvider);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Failed to switch provider: {}", e.getMessage());

            SwitchResponse response = new SwitchResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Wählt automatisch ein passendes Modell für den neuen Provider
     *
     * Strategie:
     * 1. Suche ähnliches Modell basierend auf Name/Größe
     * 2. Fallback: Erstes verfügbares Modell
     */
    private String selectModelForNewProvider(String oldModel, String newProvider) {
        try {
            List<ModelInfo> availableModels = llmProviderService.getAvailableModels();

            if (availableModels.isEmpty()) {
                log.warn("No models available for provider '{}'", newProvider);
                return null;
            }

            // 1. Versuche ähnliches Modell zu finden
            if (oldModel != null && !oldModel.isEmpty()) {
                Optional<ModelInfo> similarModel = findSimilarModel(oldModel, availableModels);
                if (similarModel.isPresent()) {
                    log.info("Found similar model: {} -> {}", oldModel, similarModel.get().getName());
                    return similarModel.get().getName();
                }
            }

            // 2. Fallback: Erstes verfügbares Modell
            String firstModel = availableModels.get(0).getName();
            log.info("Using first available model: {}", firstModel);
            return firstModel;

        } catch (Exception e) {
            log.error("Failed to select model for new provider: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Findet ein ähnliches Modell basierend auf Name und Größe
     *
     * Beispiele:
     * - "Mistral-7B-Instruct-v0.3.IQ4_XS.gguf" -> "mistral:7b" oder "mistral:latest"
     * - "llama3.1-8b.gguf" -> "llama3.1:8b"
     */
    private Optional<ModelInfo> findSimilarModel(String oldModelName, List<ModelInfo> availableModels) {
        String normalized = normalizeModelName(oldModelName);

        // Extrahiere Basis-Namen und Größe
        String baseName = extractBaseName(normalized);
        String size = extractSize(normalized);

        log.debug("Looking for similar model - baseName: '{}', size: '{}'", baseName, size);

        // 1. Exakte Übereinstimmung (Basis-Name + Größe)
        if (size != null) {
            Optional<ModelInfo> exactMatch = availableModels.stream()
                .filter(m -> {
                    String mNorm = normalizeModelName(m.getName());
                    return mNorm.contains(baseName) && mNorm.contains(size);
                })
                .findFirst();

            if (exactMatch.isPresent()) {
                return exactMatch;
            }
        }

        // 2. Basis-Name Übereinstimmung (ohne Größe)
        Optional<ModelInfo> nameMatch = availableModels.stream()
            .filter(m -> normalizeModelName(m.getName()).contains(baseName))
            .findFirst();

        return nameMatch;
    }

    /**
     * Normalisiert Modellnamen für Vergleich
     */
    private String normalizeModelName(String name) {
        return name.toLowerCase()
            .replace(".gguf", "")
            .replace("-instruct", "")
            .replace("_", "-")
            .replace(":", "-")
            .replaceAll("\\.(iq\\d+_xs|q\\d+_\\w+)", ""); // Remove quantization
    }

    /**
     * Extrahiert Basis-Namen (z.B. "mistral", "llama3.1")
     */
    private String extractBaseName(String normalized) {
        // Entferne Größen-Angaben und Versionen
        String baseName = normalized
            .replaceAll("-?(\\d+b|\\d+\\.\\d+b)", "")
            .replaceAll("-v\\d+(\\.\\d+)?", "")
            .replaceAll("-latest", "")
            .split("-")[0];

        return baseName;
    }

    /**
     * Extrahiert Modell-Größe (z.B. "7b", "13b", "8b")
     */
    private String extractSize(String normalized) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)b");
        java.util.regex.Matcher matcher = pattern.matcher(normalized);

        if (matcher.find()) {
            return matcher.group(1) + "b";
        }

        return null;
    }

    /**
     * Gibt vollständige Provider-Konfiguration zurück (llama.cpp + Ollama)
     */
    @GetMapping("/config")
    public ResponseEntity<ProviderConfigResponse> getProviderConfig() {
        ProviderConfigResponse response = new ProviderConfigResponse();

        // General
        response.setDefaultProvider(config.getDefaultProvider());

        // llama.cpp Config
        LlamaCppConfigDto llamacpp = new LlamaCppConfigDto();
        llamacpp.setBinaryPath(config.getLlamacpp().getBinaryPath());
        llamacpp.setPort(config.getLlamacpp().getPort());
        llamacpp.setModelsDir(config.getLlamacpp().getModelsDir());
        llamacpp.setAutoStart(config.getLlamacpp().isAutoStart());
        llamacpp.setContextSize(config.getLlamacpp().getContextSize());
        llamacpp.setGpuLayers(config.getLlamacpp().getGpuLayers());
        llamacpp.setThreads(config.getLlamacpp().getThreads());
        llamacpp.setEnabled(config.getLlamacpp().isEnabled());
        response.setLlamacpp(llamacpp);

        // Ollama Config
        OllamaConfigDto ollama = new OllamaConfigDto();
        ollama.setBaseUrl(config.getOllama().getBaseUrl());
        ollama.setDefaultModel(config.getOllama().getDefaultModel());
        ollama.setTimeoutSeconds(config.getOllama().getTimeoutSeconds());
        ollama.setEnabled(config.getOllama().isEnabled());
        response.setOllama(ollama);

        return ResponseEntity.ok(response);
    }

    /**
     * Aktualisiert Provider-Konfiguration
     * HINWEIS: Requires restart for most settings
     */
    @PutMapping("/config")
    public ResponseEntity<ConfigUpdateResponse> updateProviderConfig(@RequestBody ProviderConfigUpdateRequest request) {
        ConfigUpdateResponse response = new ConfigUpdateResponse();
        response.setSuccess(true);
        response.setMessage("Konfiguration gespeichert. Neustart erforderlich für die meisten Änderungen.");
        response.setRestartRequired(true);

        // TODO: Implement config persistence to application.properties or separate config file
        log.warn("Config update not yet implemented - changes will be lost on restart!");

        return ResponseEntity.ok(response);
    }

    // ===== DTOs =====

    @Data
    public static class ProvidersResponse {
        private String activeProvider;
        private List<String> availableProviders;
        private Map<String, Boolean> providerStatus;
    }

    @Data
    public static class ActiveProviderResponse {
        private String provider;
        private boolean available;
    }

    @Data
    public static class SwitchRequest {
        private String provider;
    }

    @Data
    public static class SwitchResponse {
        private boolean success;
        private String oldProvider;
        private String newProvider;
        private String selectedModel;
        private String message;
    }

    @Data
    public static class ProviderConfigResponse {
        private String defaultProvider;
        private LlamaCppConfigDto llamacpp;
        private OllamaConfigDto ollama;
    }

    @Data
    public static class LlamaCppConfigDto {
        private String binaryPath;
        private int port;
        private String modelsDir;
        private boolean autoStart;
        private int contextSize;
        private int gpuLayers;
        private int threads;
        private boolean enabled;
    }

    @Data
    public static class OllamaConfigDto {
        private String baseUrl;
        private String defaultModel;
        private int timeoutSeconds;
        private boolean enabled;
    }

    @Data
    public static class ProviderConfigUpdateRequest {
        private String defaultProvider;
        private LlamaCppConfigDto llamacpp;
        private OllamaConfigDto ollama;
    }

    @Data
    public static class ConfigUpdateResponse {
        private boolean success;
        private String message;
        private boolean restartRequired;
    }

    /**
     * Get supported features for a specific provider
     */
    @GetMapping("/{type}/features")
    public ResponseEntity<Map<String, Object>> getProviderFeatures(@PathVariable String type) {
        try {
            LLMProvider provider = llmProviderService.getProvider(type);

            Map<String, Object> response = new HashMap<>();
            response.put("provider", type);
            response.put("name", provider.getProviderName());

            // Convert Set<ProviderFeature> to List<String> for JSON
            List<String> features = provider.getSupportedFeatures().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList());
            response.put("features", features);

            // Add feature-specific details
            Map<String, Boolean> featureMap = new HashMap<>();
            for (ProviderFeature feature : ProviderFeature.values()) {
                featureMap.put(feature.name(), provider.supportsFeature(feature));
            }
            response.put("featureMap", featureMap);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting provider features for {}: {}", type, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get features for all providers (only llama.cpp variants)
     */
    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> getAllProviderFeatures() {
        Map<String, Object> allFeatures = new HashMap<>();

        // Only llama.cpp variants supported
        for (String providerType : List.of("java-llama-cpp", "llamacpp")) {
            try {
                LLMProvider provider = llmProviderService.getProvider(providerType);

                if (provider != null) {
                    Map<String, Object> providerInfo = new HashMap<>();
                    providerInfo.put("name", provider.getProviderName());
                    providerInfo.put("features", provider.getSupportedFeatures().stream()
                        .map(Enum::name)
                        .sorted()
                        .collect(Collectors.toList()));

                    allFeatures.put(providerType, providerInfo);
                }
            } catch (Exception e) {
                log.warn("Could not get features for provider {}: {}", providerType, e.getMessage());
            }
        }

        return ResponseEntity.ok(allFeatures);
    }
}
