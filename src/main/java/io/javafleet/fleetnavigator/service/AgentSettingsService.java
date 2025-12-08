package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.EmailAgentSettingsDTO;
import io.javafleet.fleetnavigator.dto.DocumentAgentSettingsDTO;
import io.javafleet.fleetnavigator.dto.OSAgentSettingsDTO;
import io.javafleet.fleetnavigator.model.AppSettings;
import io.javafleet.fleetnavigator.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Distributed Agent settings.
 * Coming Soon - Placeholder for future agent implementations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentSettingsService {

    private final AppSettingsRepository settingsRepository;

    // ============================================================================
    // EMAIL AGENT
    // ============================================================================

    /**
     * Get Email Agent settings.
     */
    @Transactional(readOnly = true)
    public EmailAgentSettingsDTO getEmailAgentSettings() {
        EmailAgentSettingsDTO settings = new EmailAgentSettingsDTO();

        settings.setModel(getStringSetting("agent.email.model", "qwen2.5:7b"));
        settings.setVisionModel(getStringSetting("agent.email.vision.model", "llava:13b"));
        settings.setImapServer(getStringSetting("agent.email.imap.server", "imap.gmail.com"));
        settings.setSmtpServer(getStringSetting("agent.email.smtp.server", "smtp.gmail.com"));
        settings.setImapPort(getIntegerSetting("agent.email.imap.port", 993));
        settings.setSmtpPort(getIntegerSetting("agent.email.smtp.port", 587));
        settings.setAutoCategorizationEnabled(getBooleanSetting("agent.email.auto.categorize", true));
        settings.setUseSsl(getBooleanSetting("agent.email.use.ssl", true));
        settings.setStatus(getStringSetting("agent.email.status", "coming_soon"));

        return settings;
    }

    /**
     * Update Email Agent settings.
     */
    @Transactional
    public EmailAgentSettingsDTO updateEmailAgentSettings(EmailAgentSettingsDTO settings) {
        log.info("Updating Email Agent settings");

        saveSetting("agent.email.model", settings.getModel(), "Email Agent model");
        saveSetting("agent.email.vision.model", settings.getVisionModel(), "Email Agent vision model");
        saveSetting("agent.email.imap.server", settings.getImapServer(), "IMAP server");
        saveSetting("agent.email.smtp.server", settings.getSmtpServer(), "SMTP server");
        saveSetting("agent.email.imap.port", String.valueOf(settings.getImapPort()), "IMAP port");
        saveSetting("agent.email.smtp.port", String.valueOf(settings.getSmtpPort()), "SMTP port");
        saveSetting("agent.email.auto.categorize", String.valueOf(settings.getAutoCategorizationEnabled()), "Auto categorization");
        saveSetting("agent.email.use.ssl", String.valueOf(settings.getUseSsl()), "Use SSL");
        saveSetting("agent.email.status", settings.getStatus(), "Email Agent status");

        log.info("Email Agent settings updated successfully");
        return getEmailAgentSettings();
    }

    // ============================================================================
    // DOCUMENT AGENT
    // ============================================================================

    /**
     * Get Document Agent settings.
     */
    @Transactional(readOnly = true)
    public DocumentAgentSettingsDTO getDocumentAgentSettings() {
        DocumentAgentSettingsDTO settings = new DocumentAgentSettingsDTO();

        settings.setModel(getStringSetting("agent.document.model", "llama3.1:8b"));
        settings.setVisionModel(getStringSetting("agent.document.vision.model", "llava:13b"));
        settings.setUploadDirectory(getStringSetting("agent.document.upload.dir", "~/FleetNavigator/Documents"));
        settings.setTextEditor(getStringSetting("agent.document.text.editor", "libreoffice"));
        settings.setAutoOcrEnabled(getBooleanSetting("agent.document.auto.ocr", true));
        settings.setSupportedFormats(getListSetting("agent.document.formats", Arrays.asList("PDF", "DOCX", "TXT", "MD", "ODT")));
        settings.setMaxFileSizeMb(getIntegerSetting("agent.document.max.size.mb", 50));
        settings.setAutoIndexing(getBooleanSetting("agent.document.auto.indexing", true));
        settings.setIndexingStrategy(getStringSetting("agent.document.indexing.strategy", "hybrid"));
        settings.setStatus(getStringSetting("agent.document.status", "active"));

        return settings;
    }

    /**
     * Update Document Agent settings.
     */
    @Transactional
    public DocumentAgentSettingsDTO updateDocumentAgentSettings(DocumentAgentSettingsDTO settings) {
        log.info("Updating Document Agent settings");

        saveSetting("agent.document.model", settings.getModel(), "Document Agent model");
        saveSetting("agent.document.vision.model", settings.getVisionModel(), "Document Agent vision model");
        saveSetting("agent.document.upload.dir", settings.getUploadDirectory(), "Upload directory");
        saveSetting("agent.document.text.editor", settings.getTextEditor(), "Text editor");
        saveSetting("agent.document.auto.ocr", String.valueOf(settings.getAutoOcrEnabled()), "Auto OCR");
        saveSetting("agent.document.formats", String.join(",", settings.getSupportedFormats()), "Supported formats");
        saveSetting("agent.document.max.size.mb", String.valueOf(settings.getMaxFileSizeMb()), "Max file size");
        saveSetting("agent.document.auto.indexing", String.valueOf(settings.getAutoIndexing()), "Auto indexing");
        saveSetting("agent.document.indexing.strategy", settings.getIndexingStrategy(), "Indexing strategy");
        saveSetting("agent.document.status", settings.getStatus(), "Document Agent status");

        log.info("Document Agent settings updated successfully");
        return getDocumentAgentSettings();
    }

    // ============================================================================
    // OS AGENT
    // ============================================================================

    /**
     * Get OS Agent settings.
     */
    @Transactional(readOnly = true)
    public OSAgentSettingsDTO getOSAgentSettings() {
        OSAgentSettingsDTO settings = new OSAgentSettingsDTO();

        settings.setModel(getStringSetting("agent.os.model", "llama3.2:3b"));
        settings.setVisionModel(getStringSetting("agent.os.vision.model", "llava:13b"));
        settings.setAllowedCommands(getListSetting("agent.os.allowed.commands",
            Arrays.asList("ls", "cd", "pwd", "cat", "grep", "find", "echo")));
        settings.setSandboxEnabled(getBooleanSetting("agent.os.sandbox.enabled", true));
        settings.setMaxExecutionTimeSeconds(getIntegerSetting("agent.os.max.exec.time", 30));
        settings.setWorkingDirectory(getStringSetting("agent.os.working.dir", "~/fleet-workspace"));
        settings.setFileSystemAccessEnabled(getBooleanSetting("agent.os.filesystem.access", true));
        settings.setNetworkAccessEnabled(getBooleanSetting("agent.os.network.access", false));
        settings.setMaxOutputLines(getIntegerSetting("agent.os.max.output.lines", 1000));
        settings.setStatus(getStringSetting("agent.os.status", "coming_soon"));

        return settings;
    }

    /**
     * Update OS Agent settings.
     */
    @Transactional
    public OSAgentSettingsDTO updateOSAgentSettings(OSAgentSettingsDTO settings) {
        log.info("Updating OS Agent settings");

        saveSetting("agent.os.model", settings.getModel(), "OS Agent model");
        saveSetting("agent.os.vision.model", settings.getVisionModel(), "OS Agent vision model");
        saveSetting("agent.os.allowed.commands", String.join(",", settings.getAllowedCommands()), "Allowed commands");
        saveSetting("agent.os.sandbox.enabled", String.valueOf(settings.getSandboxEnabled()), "Sandbox enabled");
        saveSetting("agent.os.max.exec.time", String.valueOf(settings.getMaxExecutionTimeSeconds()), "Max execution time");
        saveSetting("agent.os.working.dir", settings.getWorkingDirectory(), "Working directory");
        saveSetting("agent.os.filesystem.access", String.valueOf(settings.getFileSystemAccessEnabled()), "Filesystem access");
        saveSetting("agent.os.network.access", String.valueOf(settings.getNetworkAccessEnabled()), "Network access");
        saveSetting("agent.os.max.output.lines", String.valueOf(settings.getMaxOutputLines()), "Max output lines");
        saveSetting("agent.os.status", settings.getStatus(), "OS Agent status");

        log.info("OS Agent settings updated successfully");
        return getOSAgentSettings();
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private String getStringSetting(String key, String defaultValue) {
        return settingsRepository.findByKey(key)
            .map(AppSettings::getValue)
            .orElse(defaultValue);
    }

    private Boolean getBooleanSetting(String key, Boolean defaultValue) {
        return settingsRepository.findByKey(key)
            .map(s -> Boolean.parseBoolean(s.getValue()))
            .orElse(defaultValue);
    }

    private Integer getIntegerSetting(String key, Integer defaultValue) {
        return settingsRepository.findByKey(key)
            .map(s -> Integer.parseInt(s.getValue()))
            .orElse(defaultValue);
    }

    private List<String> getListSetting(String key, List<String> defaultValue) {
        return settingsRepository.findByKey(key)
            .map(s -> Arrays.asList(s.getValue().split(",")))
            .orElse(defaultValue);
    }

    private void saveSetting(String key, String value, String description) {
        AppSettings setting = settingsRepository.findByKey(key)
            .orElse(new AppSettings());
        setting.setKey(key);
        setting.setValue(value);
        setting.setDescription(description);
        settingsRepository.save(setting);
    }
}
