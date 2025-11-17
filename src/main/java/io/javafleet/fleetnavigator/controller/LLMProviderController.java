package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.service.LLMProviderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            llmProviderService.switchProvider(request.getProvider());
            String newProvider = llmProviderService.getActiveProviderName();

            log.info("Provider switched from {} to {}", oldProvider, newProvider);

            SwitchResponse response = new SwitchResponse();
            response.setSuccess(true);
            response.setOldProvider(oldProvider);
            response.setNewProvider(newProvider);
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
     * Gibt vollständige Provider-Konfiguration zurück (nur llama.cpp)
     */
    @GetMapping("/config")
    public ResponseEntity<ProviderConfigResponse> getProviderConfig() {
        ProviderConfigResponse response = new ProviderConfigResponse();

        // General
        response.setDefaultProvider(config.getDefaultProvider());

        // llama.cpp Config (einziger Provider)
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
        private String message;
    }

    @Data
    public static class ProviderConfigResponse {
        private String defaultProvider;
        private LlamaCppConfigDto llamacpp;
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
    public static class ProviderConfigUpdateRequest {
        private String defaultProvider;
        private LlamaCppConfigDto llamacpp;
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
