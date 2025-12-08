package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for storing letter templates/prompts
 * Allows users to save frequently used letter prompts for quick access
 */
@Entity
@Table(name = "letter_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LetterTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Template name (e.g., "Kündigung KFZ-Versicherung", "Bewerbung", etc.)
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * The prompt text that will be sent to the AI
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    /**
     * Optional description/notes about this template
     */
    @Column(length = 500)
    private String description;

    /**
     * Category for organizing templates (e.g., "Kündigungen", "Bewerbungen", "Anfragen")
     */
    @Column(length = 50)
    private String category;

    /**
     * Timestamp when template was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when template was last updated
     */
    @Column(name = "updated_at")
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
