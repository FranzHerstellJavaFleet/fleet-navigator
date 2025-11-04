package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.ModelSelectionSettingsDTO;
import io.javafleet.fleetnavigator.dto.ResetSelectionDTO;
import io.javafleet.fleetnavigator.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for application settings.
 */
@RestController
@RequestMapping("/api/settings")
@Slf4j
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
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
