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
}
