package io.javafleet.fleetnavigator.dto;

import java.util.Arrays;
import java.util.List;

/**
 * DTO for Document Agent Settings
 * Coming Soon - Placeholder for future implementation
 */
public class DocumentAgentSettingsDTO {

    // Model Configuration
    private String model;
    private String visionModel;

    // Document-specific Settings
    private String uploadDirectory;
    private String textEditor; // libreoffice, onlyoffice, msword
    private Boolean autoOcrEnabled;
    private List<String> supportedFormats;
    private Integer maxFileSizeMb;
    private Boolean autoIndexing;
    private String indexingStrategy; // "full_text", "semantic", "hybrid"

    // Agent Status
    private String status; // "coming_soon", "active", "disabled"

    public DocumentAgentSettingsDTO() {
        // Defaults
        this.model = "llama3.1:8b"; // Text-generative model for document creation
        this.visionModel = "llava:13b";
        this.uploadDirectory = "~/FleetNavigator/Documents";
        this.textEditor = "libreoffice"; // Default text editor
        this.autoOcrEnabled = true;
        this.supportedFormats = Arrays.asList("PDF", "DOCX", "TXT", "MD", "ODT");
        this.maxFileSizeMb = 50;
        this.autoIndexing = true;
        this.indexingStrategy = "hybrid";
        this.status = "active"; // Document Agent is now active
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVisionModel() {
        return visionModel;
    }

    public void setVisionModel(String visionModel) {
        this.visionModel = visionModel;
    }

    public String getUploadDirectory() {
        return uploadDirectory;
    }

    public void setUploadDirectory(String uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    public Boolean getAutoOcrEnabled() {
        return autoOcrEnabled;
    }

    public void setAutoOcrEnabled(Boolean autoOcrEnabled) {
        this.autoOcrEnabled = autoOcrEnabled;
    }

    public List<String> getSupportedFormats() {
        return supportedFormats;
    }

    public void setSupportedFormats(List<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    public Integer getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(Integer maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public Boolean getAutoIndexing() {
        return autoIndexing;
    }

    public void setAutoIndexing(Boolean autoIndexing) {
        this.autoIndexing = autoIndexing;
    }

    public String getIndexingStrategy() {
        return indexingStrategy;
    }

    public void setIndexingStrategy(String indexingStrategy) {
        this.indexingStrategy = indexingStrategy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTextEditor() {
        return textEditor;
    }

    public void setTextEditor(String textEditor) {
        this.textEditor = textEditor;
    }
}
