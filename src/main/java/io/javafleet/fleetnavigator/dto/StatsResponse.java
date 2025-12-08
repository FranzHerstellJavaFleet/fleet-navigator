package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private Long totalTokens;
    private Integer totalMessages;
    private Integer chatCount;
}
