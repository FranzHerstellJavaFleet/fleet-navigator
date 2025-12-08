package io.javafleet.fleetnavigator.dto;

/**
 * DTO for Email Agent Settings
 * Coming Soon - Placeholder for future implementation
 */
public class EmailAgentSettingsDTO {

    // Model Configuration
    private String model;
    private String visionModel;

    // Email-specific Settings
    private String imapServer;
    private String smtpServer;
    private Integer imapPort;
    private Integer smtpPort;
    private Boolean autoCategorizationEnabled;
    private Boolean useSsl;

    // Agent Status
    private String status; // "coming_soon", "active", "disabled"

    public EmailAgentSettingsDTO() {
        // Defaults
        this.model = "qwen2.5:7b";
        this.visionModel = "llava:13b";
        this.imapServer = "imap.gmail.com";
        this.smtpServer = "smtp.gmail.com";
        this.imapPort = 993;
        this.smtpPort = 587;
        this.autoCategorizationEnabled = true;
        this.useSsl = true;
        this.status = "coming_soon";
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

    public String getImapServer() {
        return imapServer;
    }

    public void setImapServer(String imapServer) {
        this.imapServer = imapServer;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public Integer getImapPort() {
        return imapPort;
    }

    public void setImapPort(Integer imapPort) {
        this.imapPort = imapPort;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public Boolean getAutoCategorizationEnabled() {
        return autoCategorizationEnabled;
    }

    public void setAutoCategorizationEnabled(Boolean autoCategorizationEnabled) {
        this.autoCategorizationEnabled = autoCategorizationEnabled;
    }

    public Boolean getUseSsl() {
        return useSsl;
    }

    public void setUseSsl(Boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
