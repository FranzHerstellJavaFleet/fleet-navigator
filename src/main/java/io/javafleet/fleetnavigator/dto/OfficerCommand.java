package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Command from Navigator to Fleet Officer
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfficerCommand {
    private String type;              // ping, collect_stats, shutdown
    private Map<String, Object> payload;
    private LocalDateTime timestamp;

    public OfficerCommand(String type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
}
