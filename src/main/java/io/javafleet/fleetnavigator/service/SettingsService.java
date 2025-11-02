package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.ModelSelectionSettingsDTO;
import io.javafleet.fleetnavigator.model.AppSettings;
import io.javafleet.fleetnavigator.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing application settings.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SettingsService {

    private final AppSettingsRepository settingsRepository;

    /**
     * Get model selection settings.
     */
    @Transactional(readOnly = true)
    public ModelSelectionSettingsDTO getModelSelectionSettings() {
        ModelSelectionSettingsDTO settings = new ModelSelectionSettingsDTO();

        // Load settings from database, fall back to defaults
        settings.setEnabled(getBooleanSetting(
            AppSettings.KEY_MODEL_SELECTION_ENABLED, true));
        settings.setCodeModel(getStringSetting(
            AppSettings.KEY_CODE_MODEL, "qwen2.5-coder:7b"));
        settings.setFastModel(getStringSetting(
            AppSettings.KEY_FAST_MODEL, "llama3.2:3b"));
        settings.setVisionModel(getStringSetting(
            AppSettings.KEY_VISION_MODEL, "llava:13b"));
        settings.setDefaultModel(getStringSetting(
            AppSettings.KEY_DEFAULT_MODEL, "qwen2.5-coder:7b"));
        settings.setVisionChainingEnabled(getBooleanSetting(
            AppSettings.KEY_VISION_CHAINING_ENABLED, true));
        settings.setVisionChainingSmartSelection(getBooleanSetting(
            AppSettings.KEY_VISION_CHAINING_SMART_SELECTION, true));

        return settings;
    }

    /**
     * Update model selection settings.
     */
    @Transactional
    public ModelSelectionSettingsDTO updateModelSelectionSettings(ModelSelectionSettingsDTO settings) {
        log.info("Updating model selection settings");

        saveSetting(AppSettings.KEY_MODEL_SELECTION_ENABLED,
            String.valueOf(settings.isEnabled()),
            "Enable/disable smart model selection");

        saveSetting(AppSettings.KEY_CODE_MODEL,
            settings.getCodeModel(),
            "Model for code-related tasks");

        saveSetting(AppSettings.KEY_FAST_MODEL,
            settings.getFastModel(),
            "Model for simple Q&A");

        saveSetting(AppSettings.KEY_VISION_MODEL,
            settings.getVisionModel(),
            "Model for vision tasks");

        saveSetting(AppSettings.KEY_DEFAULT_MODEL,
            settings.getDefaultModel(),
            "Default model for new chats");

        saveSetting(AppSettings.KEY_VISION_CHAINING_ENABLED,
            String.valueOf(settings.isVisionChainingEnabled()),
            "Enable vision chaining (Vision Model → Main Model)");

        saveSetting(AppSettings.KEY_VISION_CHAINING_SMART_SELECTION,
            String.valueOf(settings.isVisionChainingSmartSelection()),
            "Use smart model selection for main model in vision chaining");

        log.info("Model selection settings updated successfully");
        return getModelSelectionSettings();
    }

    /**
     * Get a string setting value.
     */
    private String getStringSetting(String key, String defaultValue) {
        return settingsRepository.findByKey(key)
            .map(AppSettings::getValue)
            .orElse(defaultValue);
    }

    /**
     * Get a boolean setting value.
     */
    private boolean getBooleanSetting(String key, boolean defaultValue) {
        return settingsRepository.findByKey(key)
            .map(s -> Boolean.parseBoolean(s.getValue()))
            .orElse(defaultValue);
    }

    /**
     * Save a setting.
     */
    private void saveSetting(String key, String value, String description) {
        AppSettings setting = settingsRepository.findByKey(key)
            .orElse(new AppSettings());

        setting.setKey(key);
        setting.setValue(value);
        setting.setDescription(description);

        settingsRepository.save(setting);
    }
}
