package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.ModelSelectionSettingsDTO;
import io.javafleet.fleetnavigator.dto.ResetSelectionDTO;
import io.javafleet.fleetnavigator.service.SettingsService;
import io.javafleet.fleetnavigator.service.LLMProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for application settings.
 */
@RestController
@RequestMapping("/api/settings")
@Slf4j
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final LLMProviderService llmProviderService;
    private final CacheManager cacheManager;

    /**
     * Get model selection settings.
     */
    @GetMapping("/model-selection")
    public ResponseEntity<ModelSelectionSettingsDTO> getModelSelectionSettings() {
        log.info("GET /api/settings/model-selection");
        return ResponseEntity.ok(settingsService.getModelSelectionSettings());
    }

    /**
     * Update model selection settings.
     */
    @PutMapping("/model-selection")
    public ResponseEntity<ModelSelectionSettingsDTO> updateModelSelectionSettings(
            @RequestBody ModelSelectionSettingsDTO settings) {
        log.info("PUT /api/settings/model-selection: {}", settings);

        // Update settings
        ModelSelectionSettingsDTO updated = settingsService.updateModelSelectionSettings(settings);

        // Clear model selection cache to apply new settings immediately
        var cache = cacheManager.getCache("modelSelection");
        if (cache != null) {
            cache.clear();
            log.info("Cleared modelSelection cache to apply new settings");
        }

        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/settings/selected-model - Get user's last selected model
     */
    @GetMapping("/selected-model")
    public ResponseEntity<String> getSelectedModel() {
        log.info("GET /api/settings/selected-model");
        String model = settingsService.getSelectedModel();
        if (model == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(model);
    }

    /**
     * POST /api/settings/selected-model - Save user's selected model
     */
    @PostMapping("/selected-model")
    public ResponseEntity<Void> saveSelectedModel(@RequestBody String modelName) {
        log.info("POST /api/settings/selected-model: {}", modelName);
        settingsService.saveSelectedModel(modelName);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/settings/email-model - Get email classification model
     */
    @GetMapping("/email-model")
    public ResponseEntity<String> getEmailModel() {
        log.info("GET /api/settings/email-model");
        String model = settingsService.getEmailModel();
        return ResponseEntity.ok(model != null ? model : "");
    }

    /**
     * POST /api/settings/email-model - Save email classification model
     */
    @PostMapping("/email-model")
    public ResponseEntity<Void> saveEmailModel(@RequestBody String modelName) {
        log.info("POST /api/settings/email-model: {}", modelName);
        settingsService.saveEmailModel(modelName);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/settings/log-analysis-model - Get log analysis model
     */
    @GetMapping("/log-analysis-model")
    public ResponseEntity<String> getLogAnalysisModel() {
        log.info("GET /api/settings/log-analysis-model");
        String model = settingsService.getLogAnalysisModel();
        return ResponseEntity.ok(model != null ? model : "");
    }

    /**
     * POST /api/settings/log-analysis-model - Save log analysis model
     */
    @PostMapping("/log-analysis-model")
    public ResponseEntity<Void> saveLogAnalysisModel(@RequestBody String modelName) {
        log.info("POST /api/settings/log-analysis-model: {}", modelName);
        settingsService.saveLogAnalysisModel(modelName);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/settings/document-model - Get document generation model
     */
    @GetMapping("/document-model")
    public ResponseEntity<String> getDocumentModel() {
        log.info("GET /api/settings/document-model");
        String model = settingsService.getDocumentModel();
        return ResponseEntity.ok(model != null ? model : "");
    }

    /**
     * POST /api/settings/document-model - Save document generation model
     */
    @PostMapping("/document-model")
    public ResponseEntity<Void> saveDocumentModel(@RequestBody String modelName) {
        log.info("POST /api/settings/document-model: {}", modelName);
        settingsService.saveDocumentModel(modelName);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/settings/llm-provider - Get active LLM provider
     */
    @GetMapping("/llm-provider")
    public ResponseEntity<Map<String, String>> getActiveProvider() {
        log.info("GET /api/settings/llm-provider");
        String activeProvider = llmProviderService.getActiveProviderName();
        return ResponseEntity.ok(Map.of("provider", activeProvider));
    }

    /**
     * POST /api/settings/llm-provider - Switch LLM provider (persists to DB)
     */
    @PostMapping("/llm-provider")
    public ResponseEntity<Map<String, String>> switchProvider(@RequestBody Map<String, String> request) {
        String providerName = request.get("provider");
        log.info("POST /api/settings/llm-provider: {}", providerName);

        try {
            llmProviderService.switchProvider(providerName);
            return ResponseEntity.ok(Map.of(
                "message", "Provider switched successfully",
                "provider", providerName
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Failed to switch provider: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/settings/llm-providers/status - Get status of all providers
     */
    @GetMapping("/llm-providers/status")
    public ResponseEntity<Map<String, Object>> getProvidersStatus() {
        log.info("GET /api/settings/llm-providers/status");
        Map<String, Boolean> status = llmProviderService.getProviderStatus();
        String activeProvider = llmProviderService.getActiveProviderName();

        return ResponseEntity.ok(Map.of(
            "providers", status,
            "active", activeProvider
        ));
    }

    /**
     * POST /api/settings/reset-selective - DANGER ZONE: Reset selected application data
     * This endpoint deletes only the selected categories of user data.
     * Use only for testing/seeding purposes!
     */
    @PostMapping("/reset-selective")
    public ResponseEntity<Void> resetSelectedData(@RequestBody ResetSelectionDTO selection) {
        log.warn("⚠️ POST /api/settings/reset-selective - Resetting selected data!");
        settingsService.resetSelectedData(
            selection.isChats(),
            selection.isProjects(),
            selection.isCustomModels(),
            selection.isSettings(),
            selection.isPersonalInfo(),
            selection.isTemplates(),
            selection.isStats()
        );
        return ResponseEntity.ok().build();
    }
}
