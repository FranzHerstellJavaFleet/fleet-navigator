package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for renaming a chat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RenameChatRequest {
    private String newTitle;
}
