package io.javafleet.fleetnavigator.dto;

import io.javafleet.fleetnavigator.model.Message.MessageRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Message
 */
@Data
public class MessageDTO {
    private Long id;
    private MessageRole role;
    private String content;
    private Integer tokens;
    private String modelName;
    private LocalDateTime createdAt;

    /**
     * JSON array of file attachment metadata.
     * Format: [{"name":"file.pdf","type":"pdf","size":12345}, ...]
     */
    private String attachments;

    /**
     * Download URL for generated documents.
     * Can be:
     * - "/api/downloads/doc/{id}" for server-generated documents
     * - "fleet-mate://{sessionId}" for locally saved documents via Fleet-Mate
     */
    private String downloadUrl;
}
