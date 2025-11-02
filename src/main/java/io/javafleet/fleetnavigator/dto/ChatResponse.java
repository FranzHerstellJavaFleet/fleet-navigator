package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for chat messages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long chatId;
    private String response;
    private Integer tokens;
    private String model;
    private String requestId;  // For request tracking/cancellation
}
