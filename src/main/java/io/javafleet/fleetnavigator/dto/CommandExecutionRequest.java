package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for remote command execution on Fleet Mates
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandExecutionRequest {
    private String mateId;
    private String command;          // Base command (e.g., "ls", "ps", "systemctl status")
    private List<String> args;       // Command arguments (e.g., ["-la", "/var/log"])
    private String workingDirectory; // Optional working directory (default: /tmp)
    private Integer timeoutSeconds;  // Execution timeout (default: 300 = 5 minutes)
    private boolean captureStderr;   // Whether to capture stderr separately (default: true)
}
