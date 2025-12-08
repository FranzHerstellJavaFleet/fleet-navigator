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
    private Integer projectTotalChatTokens; // Total tokens from ALL chats in the project
    private Integer projectChatCount; // Number of chats in the project

    // Expert für diesen Chat
    private Long expertId;  // ID des ausgewählten Experten (null = kein Experte)

    // Expert Mode für diesen Chat
    private Long activeExpertModeId;   // ID des aktiven Modus (null = Allgemein)
    private String activeExpertModeName; // Name des aktiven Modus (z.B. "Mietrecht")
}
