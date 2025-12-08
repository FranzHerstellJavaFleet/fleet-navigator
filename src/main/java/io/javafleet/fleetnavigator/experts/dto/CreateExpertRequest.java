package io.javafleet.fleetnavigator.experts.dto;

import lombok.Data;

import java.util.List;

/**
 * Request DTO zum Erstellen eines neuen Experten
 */
@Data
public class CreateExpertRequest {

    /**
     * Name des Experten (z.B. "Roland")
     */
    private String name;

    /**
     * Rolle/Beruf (z.B. "Rechtsanwalt")
     */
    private String role;

    /**
     * Kurze Beschreibung
     */
    private String description;

    /**
     * Basis-System-Prompt (Expertise/Rolle)
     */
    private String basePrompt;

    /**
     * Personality-Prompt (Kommunikationsstil, Humor, Du/Sie, etc.)
     * Wird vom Benutzer anpassbar sein
     */
    private String personalityPrompt;

    /**
     * Avatar-URL für das Experten-Bild
     * z.B. "/images/experts/roland.png" oder externe URL
     */
    private String avatarUrl;

    /**
     * Zu verwendendes Basis-Modell
     */
    private String baseModel;

    /**
     * Standard-Temperature
     */
    private Double defaultTemperature;

    /**
     * Standard-TopP
     */
    private Double defaultTopP;

    /**
     * Standard num_ctx
     */
    private Integer defaultNumCtx;

    /**
     * Web-Suche automatisch aktivieren
     */
    private Boolean autoWebSearch;

    /**
     * Such-Domains (komma-getrennt oder als String)
     */
    private String searchDomains;

    /**
     * Maximale Suchergebnisse
     */
    private Integer maxSearchResults;

    /**
     * Dateisuche automatisch aktivieren
     */
    private Boolean autoFileSearch;

    /**
     * Dokumenten-Verzeichnis für den Experten (z.B. "Roland")
     * Dokumente werden gespeichert in ~/Dokumente/Fleet-Navigator/{documentDirectory}/
     */
    private String documentDirectory;

    /**
     * Bevorzugter Fleet-Mate für Dokumentenerstellung (z.B. "os-ubuntu-desktop-trainer")
     */
    private String preferredMateId;

    /**
     * Maximale Antwortlänge (max_tokens)
     */
    private Integer defaultMaxTokens;

    /**
     * Initiale Modi (optional)
     */
    private List<CreateExpertModeRequest> modes;
}
