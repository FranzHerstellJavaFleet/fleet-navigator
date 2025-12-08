package io.javafleet.fleetnavigator.dto;

import lombok.Data;

/**
 * Request DTO for creating a custom Ollama model
 */
@Data
public class CreateCustomModelRequest {

    /**
     * Name for the custom model (e.g. "nova:latest")
     */
    private String name;

    /**
     * Base model to derive from (e.g. "llama3.2:3b")
     */
    private String baseModel;

    /**
     * System prompt defining character/personality
     */
    private String systemPrompt;

    /**
     * Short description
     */
    private String description;

    /**
     * Temperature parameter (0.0 - 2.0)
     */
    private Double temperature;

    /**
     * Top P parameter (0.0 - 1.0)
     */
    private Double topP;

    /**
     * Top K parameter (0 - 100)
     */
    private Integer topK;

    /**
     * Repeat penalty parameter
     */
    private Double repeatPenalty;

    /**
     * Maximum tokens to generate (num_predict)
     */
    private Integer numPredict;

    /**
     * Context window size (num_ctx) - max 131072 (128K)
     */
    private Integer numCtx;
}
