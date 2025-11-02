package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.ModelSelectionSettingsDTO;
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
}
