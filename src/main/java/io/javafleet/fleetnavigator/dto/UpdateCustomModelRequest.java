package io.javafleet.fleetnavigator.dto;

import lombok.Data;

/**
 * Request DTO for updating a custom model (creates new version)
 */
@Data
public class UpdateCustomModelRequest {

    /**
     * New name for the updated version (optional, will be auto-generated if not provided)
     */
    private String name;

    /**
     * Updated system prompt
     */
    private String systemPrompt;

    /**
     * Updated description
     */
    private String description;

    /**
     * Updated temperature parameter
     */
    private Double temperature;

    /**
     * Updated Top P parameter
     */
    private Double topP;

    /**
     * Updated Top K parameter
     */
    private Integer topK;

    /**
     * Updated repeat penalty parameter
     */
    private Double repeatPenalty;

    /**
     * Updated maximum tokens to generate (num_predict)
     */
    private Integer numPredict;

    /**
     * Updated context window size (num_ctx) - max 131072 (128K)
     */
    private Integer numCtx;
}
