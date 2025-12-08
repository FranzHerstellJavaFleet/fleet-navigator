package io.javafleet.fleetnavigator.experts.dto;

import lombok.Data;

/**
 * Request DTO zum Erstellen eines Modus für einen Experten
 */
@Data
public class CreateExpertModeRequest {

    /**
     * Name des Modus (z.B. "Verwaltungsrecht")
     */
    private String name;

    /**
     * Beschreibung
     */
    private String description;

    /**
     * Zusätzlicher Prompt-Text für diesen Modus
     */
    private String promptAddition;

    /**
     * Keywords für Auto-Detection (komma-separiert)
     */
    private String keywords;

    /**
     * Temperature für diesen Modus
     */
    private Double temperature;

    /**
     * TopP für diesen Modus
     */
    private Double topP;

    /**
     * TopK für diesen Modus
     */
    private Integer topK;

    /**
     * Repeat Penalty
     */
    private Double repeatPenalty;

    /**
     * num_ctx für diesen Modus
     */
    private Integer numCtx;

    /**
     * Max Tokens für Antworten
     */
    private Integer maxTokens;

    /**
     * Priorität für Keyword-Matching
     */
    private Integer priority;
}
