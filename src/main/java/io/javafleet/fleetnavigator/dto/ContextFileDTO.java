package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContextFileDTO {
    private Long id;
    private String filename;
    private String fileType;
    private Long size; // Size in bytes
    private int estimatedTokens;
    private LocalDateTime uploadedAt;
    // Content is not included in DTO to avoid sending large text in lists
}
