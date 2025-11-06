package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for command output chunks (streamed via SSE)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommandOutput {
    private String sessionId;
    private String type;              // "stdout", "stderr", "exit"
    private String content;           // Output content
    private Integer exitCode;         // Only for type="exit"
    private LocalDateTime timestamp;
    private boolean done;             // true when command finished
}
