package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for user-created custom Ollama models
 * Supports versioning and ancestry tracking
 */
@Entity
@Table(name = "custom_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Model name as registered in Ollama (e.g. "nova:latest", "coder:v2")
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Base model this custom model is derived from (e.g. "llama3.2:3b")
     */
    @Column(nullable = false)
    private String baseModel;

    /**
     * System prompt defining the model's character/personality
     */
    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    /**
     * Short description of the model
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

    /**
     * Creation timestamp
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Ollama model digest (for tracking updates)
     */
    private String ollamaDigest;

    /**
     * Parent custom model ID (for versioning/ancestry)
     */
    private Long parentModelId;

    /**
     * Version number (starts at 1, increments on updates)
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * Complete Modelfile content (stored for reference and recreation)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String modelfile;

    /**
     * Model size in bytes (from Ollama)
     */
    private Long size;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
