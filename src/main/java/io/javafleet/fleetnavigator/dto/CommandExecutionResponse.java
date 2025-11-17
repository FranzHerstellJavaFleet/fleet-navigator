package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for command execution requests
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandExecutionResponse {
    private String sessionId;         // Unique session identifier
    private String mateId;            // Target mate ID
    private String command;           // Executed command
    private String status;            // "pending", "executing", "completed", "failed", "timeout"
    private String message;           // Status message
}
