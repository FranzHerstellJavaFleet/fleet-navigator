package io.javafleet.fleetnavigator.dto;

import lombok.Data;
import java.util.List;

/**
 * Request DTO for sending a chat message
 */
@Data
public class ChatRequest {
    private Long chatId;
    private String message;
    private String model;
    private String systemPrompt;
    private Boolean stream = false;

    // For Vision models: Base64 encoded images
    private List<String> images;

    // For documents: Extracted text content
    private String documentContext;

    // Vision-Chaining settings
    private Boolean visionChainEnabled = false;
    private String visionModel;  // Vision Model für Chaining (z.B. llava:13b)

    // Language setting for output
    private String language = "de";  // "de", "es", "tr", "fr"

    // Model parameters for generation control
    private Integer maxTokens;      // Maximum tokens to generate (num_predict in Ollama)
    private Double temperature;     // Randomness (0.0-2.0)
    private Double topP;           // Nucleus sampling
    private Integer topK;          // Top-K sampling
    private Double repeatPenalty;  // Penalize repetitions
}
