package io.javafleet.fleetnavigator.experts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity für einen Modus (Blickwinkel) eines Experten
 *
 * Ein Modus definiert eine spezifische Arbeitsweise/Perspektive des Experten.
 * z.B. Roland kann Modi haben für: Verwaltungsrecht, Sozialrecht, Strafrecht
 */
@Entity
@Table(name = "expert_modes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referenz zum Experten
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    @JsonIgnore
    private Expert expert;

    /**
     * Name des Modus (z.B. "Verwaltungsrecht", "Sozialrecht", "Strafrecht")
     */
    @Column(nullable = false)
    private String name;

    /**
     * Beschreibung was dieser Modus macht
     */
    private String description;

    /**
     * Zusätzlicher Prompt-Text für diesen Modus
     * Wird an den Base-Prompt des Experten angehängt
     */
    @Column(columnDefinition = "TEXT")
    private String promptAddition;

    /**
     * Keywords für automatische Modus-Erkennung
     * Komma-separiert (z.B. "behörde,antrag,verwaltung,bescheid")
     */
    @Column(columnDefinition = "TEXT")
    private String keywords;

    /**
     * Temperature für diesen Modus (überschreibt Expert-Default)
     */
    private Double temperature;

    /**
     * TopP für diesen Modus (überschreibt Expert-Default)
     */
    private Double topP;

    /**
     * TopK für diesen Modus
     */
    private Integer topK;

    /**
     * Repeat Penalty für diesen Modus
     */
    private Double repeatPenalty;

    /**
     * num_ctx für diesen Modus (überschreibt Expert-Default)
     */
    private Integer numCtx;

    /**
     * Max Tokens für Antworten in diesem Modus
     */
    private Integer maxTokens;

    /**
     * Priorität für Keyword-Matching (höher = wird zuerst geprüft)
     */
    private Integer priority = 0;

    /**
     * Ob dieser Modus aktiv ist
     */
    private Boolean active = true;

    /**
     * Erstellungszeitpunkt
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Letzter Update-Zeitpunkt
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

    /**
     * Hilfsmethode: Keywords als Array
     */
    public String[] getKeywordsArray() {
        if (keywords == null || keywords.isBlank()) {
            return new String[0];
        }
        return keywords.split(",");
    }

    /**
     * Hilfsmethode: Expert-ID ohne volle Referenz
     */
    public Long getExpertId() {
        return expert != null ? expert.getId() : null;
    }
}
