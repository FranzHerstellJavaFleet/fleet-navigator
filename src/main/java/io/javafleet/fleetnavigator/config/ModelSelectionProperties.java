package io.javafleet.fleetnavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for smart model selection.
 * Inspired by local-llm-demo-full project.
 */
@Component
@ConfigurationProperties(prefix = "fleet-navigator.model-selection")
@Data
public class ModelSelectionProperties {

    /**
     * Enable or disable smart model selection.
     * If disabled, uses user-specified or chat default model.
     */
    private boolean enabled = true;

    /**
     * Model to use for code-related tasks (coding, debugging, technical questions).
     */
    private String codeModel = "qwen2.5-coder:7b";

    /**
     * Model to use for simple Q&A and quick responses.
     */
    private String fastModel = "llama3.2:3b";

    /**
     * Model to use for vision tasks (image analysis).
     */
    private String visionModel = "llava:13b";
}
