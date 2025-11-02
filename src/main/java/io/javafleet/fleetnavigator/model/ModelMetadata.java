package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model metadata stored in database
 * Contains additional information about Ollama models
 */
@Entity
@Table(name = "model_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Model name (e.g., "qwen2.5-coder:7b")
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Model size in bytes
     */
    private Long size;

    /**
     * Short description
     */
    @Column(length = 500)
    private String description;

    /**
     * Model specialties/capabilities (comma-separated or JSON)
     */
    @Column(length = 1000)
    private String specialties;

    /**
     * Publisher/Creator (e.g., "Alibaba Cloud", "Meta")
     */
    private String publisher;

    /**
     * Release date
     */
    private LocalDateTime releaseDate;

    /**
     * Training data cutoff date (bis wann trainiert wurde)
     */
    private String trainedUntil;

    /**
     * License information
     */
    private String license;

    /**
     * Is this the default model?
     */
    @Column(nullable = false)
    private Boolean isDefault = false;

    /**
     * Custom user notes
     */
    @Column(length = 1000)
    private String notes;

    /**
     * When was this metadata created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When was this metadata last updated
     */
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
