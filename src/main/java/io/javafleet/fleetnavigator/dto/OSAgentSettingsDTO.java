package io.javafleet.fleetnavigator.dto;

import java.util.Arrays;
import java.util.List;

/**
 * DTO for OS Agent Settings
 * Coming Soon - Placeholder for future implementation
 */
public class OSAgentSettingsDTO {

    // Model Configuration
    private String model;
    private String visionModel;

    // OS-specific Settings
    private List<String> allowedCommands;
    private Boolean sandboxEnabled;
    private Integer maxExecutionTimeSeconds;
    private String workingDirectory;
    private Boolean fileSystemAccessEnabled;
    private Boolean networkAccessEnabled;
    private Integer maxOutputLines;

    // Agent Status
    private String status; // "coming_soon", "active", "disabled"

    public OSAgentSettingsDTO() {
        // Defaults - Security-focused
        this.model = "llama3.2:3b";
        this.visionModel = "llava:13b";
        this.allowedCommands = Arrays.asList("ls", "cd", "pwd", "cat", "grep", "find", "echo");
        this.sandboxEnabled = true;
        this.maxExecutionTimeSeconds = 30;
        this.workingDirectory = "~/fleet-workspace";
        this.fileSystemAccessEnabled = true;
        this.networkAccessEnabled = false; // Disabled by default for security
        this.maxOutputLines = 1000;
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

    public List<String> getAllowedCommands() {
        return allowedCommands;
    }

    public void setAllowedCommands(List<String> allowedCommands) {
        this.allowedCommands = allowedCommands;
    }

    public Boolean getSandboxEnabled() {
        return sandboxEnabled;
    }

    public void setSandboxEnabled(Boolean sandboxEnabled) {
        this.sandboxEnabled = sandboxEnabled;
    }

    public Integer getMaxExecutionTimeSeconds() {
        return maxExecutionTimeSeconds;
    }

    public void setMaxExecutionTimeSeconds(Integer maxExecutionTimeSeconds) {
        this.maxExecutionTimeSeconds = maxExecutionTimeSeconds;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Boolean getFileSystemAccessEnabled() {
        return fileSystemAccessEnabled;
    }

    public void setFileSystemAccessEnabled(Boolean fileSystemAccessEnabled) {
        this.fileSystemAccessEnabled = fileSystemAccessEnabled;
    }

    public Boolean getNetworkAccessEnabled() {
        return networkAccessEnabled;
    }

    public void setNetworkAccessEnabled(Boolean networkAccessEnabled) {
        this.networkAccessEnabled = networkAccessEnabled;
    }

    public Integer getMaxOutputLines() {
        return maxOutputLines;
    }

    public void setMaxOutputLines(Integer maxOutputLines) {
        this.maxOutputLines = maxOutputLines;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
