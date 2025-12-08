package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Command from Navigator to Fleet Mate
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MateCommand {
    private String type;              // ping, collect_stats, shutdown
    private Map<String, Object> payload;
    private LocalDateTime timestamp;

    public MateCommand(String type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public MateCommand(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }
}
