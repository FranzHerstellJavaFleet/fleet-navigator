package io.javafleet.fleetnavigator.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Chat with messages
 */
@Data
public class ChatDTO {
    private Long id;
    private String title;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageDTO> messages;
    private Integer totalTokens;
    private Long projectId; // ID of associated project (if any)
    private String projectName; // Name of associated project (if any)
    private Integer projectTokens; // Total tokens in project context files (if any)
}
