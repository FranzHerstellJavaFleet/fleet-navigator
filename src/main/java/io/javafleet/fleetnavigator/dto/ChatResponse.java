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
    private String downloadUrl;  // Optional: URL for downloadable generated code
    private Boolean webSearchPerformed = false;  // Ob Web-Suche durchgeführt wurde

    // Constructor for backward compatibility (without downloadUrl)
    public ChatResponse(Long chatId, String response, Integer tokens, String model, String requestId) {
        this.chatId = chatId;
        this.response = response;
        this.tokens = tokens;
        this.model = model;
        this.requestId = requestId;
        this.downloadUrl = null;
        this.webSearchPerformed = false;
    }
}
