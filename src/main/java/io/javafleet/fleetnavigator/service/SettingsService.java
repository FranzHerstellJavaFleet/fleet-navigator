package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.ModelSelectionSettingsDTO;
import io.javafleet.fleetnavigator.model.AppSettings;
import io.javafleet.fleetnavigator.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ContextItemRepository contextItemRepository;
    private final ContextFileRepository contextFileRepository;
    private final ProjectRepository projectRepository;
    private final ModelMetadataRepository modelMetadataRepository;
    private final GlobalStatsRepository globalStatsRepository;
    private final SystemPromptTemplateRepository systemPromptTemplateRepository;
    private final PersonalInfoRepository personalInfoRepository;
    private final LetterTemplateRepository letterTemplateRepository;
    private final CustomModelRepository customModelRepository;
    private final ApplicationContext applicationContext;

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
            AppSettings.KEY_VISION_CHAINING_ENABLED, false));
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
     * Get the user's last selected model.
     */
    @Transactional(readOnly = true)
    public String getSelectedModel() {
        String model = getStringSetting(AppSettings.KEY_SELECTED_MODEL, null);
        log.debug("Retrieved selected model from database: {}", model);
        return model;
    }

    /**
     * Save the user's selected model.
     */
    @Transactional
    public void saveSelectedModel(String modelName) {
        log.info("Saving selected model to database: {}", modelName);
        saveSetting(AppSettings.KEY_SELECTED_MODEL, modelName, "User's last selected model");
    }

    /**
     * Get the user's last selected expert ID.
     */
    @Transactional(readOnly = true)
    public Long getSelectedExpertId() {
        String expertIdStr = getStringSetting(AppSettings.KEY_SELECTED_EXPERT, null);
        if (expertIdStr == null || expertIdStr.isBlank()) {
            log.debug("No selected expert in database");
            return null;
        }
        try {
            Long expertId = Long.parseLong(expertIdStr);
            log.debug("Retrieved selected expert from database: {}", expertId);
            return expertId;
        } catch (NumberFormatException e) {
            log.warn("Invalid expert ID in database: {}", expertIdStr);
            return null;
        }
    }

    /**
     * Save the user's selected expert ID.
     */
    @Transactional
    public void saveSelectedExpertId(Long expertId) {
        log.info("Saving selected expert to database: {}", expertId);
        String value = expertId != null ? String.valueOf(expertId) : null;
        saveSetting(AppSettings.KEY_SELECTED_EXPERT, value, "User's last selected expert ID");
    }

    /**
     * Get the email classification model.
     */
    @Transactional(readOnly = true)
    public String getEmailModel() {
        String model = getStringSetting(AppSettings.KEY_EMAIL_MODEL, null);
        log.debug("Retrieved email model from database: {}", model);
        return model;
    }

    /**
     * Save the email classification model.
     */
    @Transactional
    public void saveEmailModel(String modelName) {
        log.info("Saving email model to database: {}", modelName);
        saveSetting(AppSettings.KEY_EMAIL_MODEL, modelName, "Email classification and reply model");
    }

    /**
     * Get the log analysis model.
     */
    @Transactional(readOnly = true)
    public String getLogAnalysisModel() {
        String model = getStringSetting(AppSettings.KEY_LOG_ANALYSIS_MODEL, null);
        log.debug("Retrieved log analysis model from database: {}", model);
        return model;
    }

    /**
     * Save the log analysis model.
     */
    @Transactional
    public void saveLogAnalysisModel(String modelName) {
        log.info("Saving log analysis model to database: {}", modelName);
        saveSetting(AppSettings.KEY_LOG_ANALYSIS_MODEL, modelName, "Log analysis and debugging model");
    }

    /**
     * Get the document generation model.
     */
    @Transactional(readOnly = true)
    public String getDocumentModel() {
        String model = getStringSetting(AppSettings.KEY_DOCUMENT_MODEL, null);
        log.debug("Retrieved document model from database: {}", model);
        return model;
    }

    /**
     * Save the document generation model.
     */
    @Transactional
    public void saveDocumentModel(String modelName) {
        log.info("Saving document model to database: {}", modelName);
        saveSetting(AppSettings.KEY_DOCUMENT_MODEL, modelName, "Document and letter generation model");
    }

    /**
     * Get the active LLM provider.
     */
    @Transactional(readOnly = true)
    public String getActiveProvider() {
        String provider = getStringSetting(AppSettings.KEY_ACTIVE_PROVIDER, null);
        log.debug("Retrieved active provider from database: {}", provider);
        return provider;
    }

    /**
     * Save the active LLM provider.
     */
    @Transactional
    public void saveActiveProvider(String providerName) {
        log.info("Saving active provider to database: {}", providerName);
        saveSetting(AppSettings.KEY_ACTIVE_PROVIDER, providerName, "Active LLM Provider (ollama, llamacpp, openai)");
    }

    /**
     * Get the show welcome tiles setting.
     */
    @Transactional(readOnly = true)
    public boolean getShowWelcomeTiles() {
        String value = getStringSetting(AppSettings.KEY_SHOW_WELCOME_TILES, "true");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Save the show welcome tiles setting.
     */
    @Transactional
    public void saveShowWelcomeTiles(boolean show) {
        log.info("Saving showWelcomeTiles to database: {}", show);
        saveSetting(AppSettings.KEY_SHOW_WELCOME_TILES, String.valueOf(show), "Show welcome tiles on start");
    }

    // ==================== TopBar Settings ====================

    /**
     * Get the show top bar setting.
     * Default is true (TopBar visible).
     */
    @Transactional(readOnly = true)
    public boolean getShowTopBar() {
        String value = getStringSetting(AppSettings.KEY_SHOW_TOP_BAR, "true");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Save the show top bar setting.
     */
    @Transactional
    public void saveShowTopBar(boolean show) {
        log.info("Saving showTopBar to database: {}", show);
        saveSetting(AppSettings.KEY_SHOW_TOP_BAR, String.valueOf(show), "Show top navigation bar");
    }

    // ==================== UI Theme Settings ====================

    /**
     * Get the UI theme setting.
     * Default is "tech-dark" (Tech Dunkel).
     */
    @Transactional(readOnly = true)
    public String getUiTheme() {
        String theme = getStringSetting(AppSettings.KEY_UI_THEME, "tech-dark");
        log.debug("Retrieved UI theme from database: {}", theme);
        return theme;
    }

    /**
     * Save the UI theme setting.
     */
    @Transactional
    public void saveUiTheme(String theme) {
        log.info("Saving uiTheme to database: {}", theme);
        saveSetting(AppSettings.KEY_UI_THEME, theme, "UI Theme (tech-dark, tech-light, lawyer-dark, lawyer-light)");
    }

    // ==================== File Search Settings ====================

    /**
     * Get file search folders as JSON string.
     */
    @Transactional(readOnly = true)
    public String getFileSearchFolders() {
        return getStringSetting(AppSettings.KEY_FILESEARCH_FOLDERS, "[]");
    }

    /**
     * Save file search folders as JSON string.
     */
    @Transactional
    public void saveFileSearchFolders(String foldersJson) {
        log.info("Saving file search folders to database");
        saveSetting(AppSettings.KEY_FILESEARCH_FOLDERS, foldersJson, "File search folder configurations (JSON)");
    }

    /**
     * Check if file search is enabled.
     */
    @Transactional(readOnly = true)
    public boolean isFileSearchEnabled() {
        return getBooleanSetting(AppSettings.KEY_FILESEARCH_ENABLED, true);
    }

    /**
     * Enable or disable file search.
     */
    @Transactional
    public void setFileSearchEnabled(boolean enabled) {
        saveSetting(AppSettings.KEY_FILESEARCH_ENABLED, String.valueOf(enabled), "Enable/disable file search");
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

    /**
     * DANGER ZONE: Reset selected application data.
     * Deletes only the selected categories of data.
     * Use only for testing/seeding purposes!
     */
    @Transactional
    public void resetSelectedData(boolean chats, boolean projects, boolean customModels,
                                    boolean settings, boolean personalInfo, boolean templates, boolean stats) {
        log.warn("⚠️ RESET SELECTED DATA - Deleting selected application data!");

        // Chats & Messages
        if (chats) {
            log.info("Deleting context items...");
            contextItemRepository.deleteAll();

            log.info("Deleting messages...");
            messageRepository.deleteAll();

            log.info("Deleting chats...");
            chatRepository.deleteAll();
        }

        // Projects & Files
        if (projects) {
            log.info("Deleting context files...");
            contextFileRepository.deleteAll();

            log.info("Deleting projects...");
            projectRepository.deleteAll();
        }

        // Custom Models
        if (customModels) {
            log.info("Deleting custom models...");
            customModelRepository.deleteAll();

            log.info("Deleting model metadata...");
            modelMetadataRepository.deleteAll();
        }

        // Settings
        if (settings) {
            log.info("Deleting app settings...");
            settingsRepository.deleteAll();
        }

        // Personal Info
        if (personalInfo) {
            log.info("Deleting personal info...");
            personalInfoRepository.deleteAll();
        }

        // Templates
        if (templates) {
            log.info("Deleting system prompt templates...");
            systemPromptTemplateRepository.deleteAll();
            // Sofort nach dem Löschen neu erstellen!
            log.info("🌱 Re-seeding system prompts (Karla als Standard)...");
            applicationContext.getBean(SystemPromptsInitializer.class).initializeSystemPrompts();

            log.info("Deleting letter templates...");
            letterTemplateRepository.deleteAll();
            // Sofort nach dem Löschen neu erstellen!
            log.info("🌱 Re-seeding letter templates...");
            applicationContext.getBean(DefaultDataInitializer.class).initializeLetterTemplates();
        }

        // Statistics
        if (stats) {
            log.info("Deleting global stats...");
            globalStatsRepository.deleteAll();
        }

        log.warn("✅ SELECTED DATA DELETED - Application partially reset!");

        // Personal Info seeding (nur wenn leer)
        if (personalInfo && !applicationContext.getBean(PersonalInfoService.class).hasPersonalInfo()) {
            log.info("🌱 Re-seeding personal info (Max Mustermann)...");
            applicationContext.getBean(DefaultDataInitializer.class).initializePlaceholderPersonalInfo();
        }

        log.info("✅ Seeding completed!");
    }
}
