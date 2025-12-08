package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ContextFileDTO> contextFiles;
    private List<Long> chatIds; // IDs of chats in this project
    private long totalContextSize; // Total size in bytes
    private int estimatedTokens; // Estimated tokens in all context files
}
