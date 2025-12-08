package io.javafleet.fleetnavigator.dto;

import lombok.Data;

/**
 * Request DTO for creating a new chat
 */
@Data
public class NewChatRequest {
    private String title;
    private String model;
    private Long expertId;  // Optional: ID des ausgewählten Experten
}
