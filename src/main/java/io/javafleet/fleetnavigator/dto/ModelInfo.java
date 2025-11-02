package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Ollama model information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    private String name;
    private String size;
    private String modifiedAt;
    private Long contextWindow;
    private String digest;  // Unique identifier for model version (for update detection)
}
