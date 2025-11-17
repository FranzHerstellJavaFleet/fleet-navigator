package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for command execution history
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandHistoryEntry {
    private String sessionId;
    private String mateId;
    private String command;
    private String fullCommand;       // Command with args
    private Integer exitCode;
    private String output;            // Truncated output (first 1000 chars)
    private Long durationMs;          // Execution duration in milliseconds
    private LocalDateTime executedAt;
    private String status;            // "success", "failed", "timeout"
}
