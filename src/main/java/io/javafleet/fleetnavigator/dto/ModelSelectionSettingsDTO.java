package io.javafleet.fleetnavigator.dto;

import lombok.Data;

/**
 * DTO for model selection settings.
 */
@Data
public class ModelSelectionSettingsDTO {

    /**
     * Enable or disable smart model selection.
     */
    private boolean enabled;

    /**
     * Model to use for code-related tasks.
     */
    private String codeModel;

    /**
     * Model to use for simple Q&A.
     */
    private String fastModel;

    /**
     * Model to use for vision tasks.
     */
    private String visionModel;

    /**
     * Default model for new chats.
     */
    private String defaultModel;

    /**
     * Enable vision chaining (Vision Model → Main Model).
     */
    private boolean visionChainingEnabled;

    /**
     * Use smart model selection for the main model in vision chaining.
     * If true: Vision Model → Smart-Selected Main Model (based on prompt)
     * If false: Vision Model → User-Selected Main Model
     */
    private boolean visionChainingSmartSelection;
}
