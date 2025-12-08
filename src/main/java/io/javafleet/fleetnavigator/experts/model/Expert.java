package io.javafleet.fleetnavigator.experts.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity für einen konfigurierbaren AI-Experten
 *
 * Ein Experte hat eine Basis-Persönlichkeit und kann mehrere Modi (Blickwinkel) haben,
 * die zur Laufzeit hinzugefügt, bearbeitet oder entfernt werden können.
 */
@Entity
@Table(name = "experts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name des Experten (z.B. "Roland", "Ayşe")
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Rolle/Beruf des Experten (z.B. "Rechtsanwalt", "Steuerberater", "Marketing-Experte")
     */
    @Column(nullable = false)
    private String role;

    /**
     * Kurze Beschreibung des Experten
     */
    private String description;

    /**
     * Basis-System-Prompt der die Expertise/Rolle definiert
     * z.B. "Du bist ein erfahrener Rechtsanwalt mit Schwerpunkt..."
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String basePrompt;

    /**
     * Personality-Prompt der den Kommunikationsstil definiert
     * z.B. Freundlichkeit, Duzen/Siezen, Humor, Witzigkeit
     * Vom Benutzer anpassbar für individuelles Erlebnis
     */
    @Column(columnDefinition = "TEXT")
    private String personalityPrompt;

    /**
     * Avatar-URL für das Experten-Bild
     * Wird in der TopBar angezeigt wenn der Experte ausgewählt ist
     * Kann eine lokale URL (/images/experts/roland.png) oder externe URL sein
     */
    @Column(length = 500)
    private String avatarUrl;

    /**
     * Basis-Modell das verwendet wird (z.B. "llama3.1:8b-instruct-q8_0")
     * Wird für Ollama-Provider verwendet
     */
    @Column(nullable = false)
    private String baseModel;

    /**
     * GGUF-Modell für java-llama-cpp Provider
     * z.B. "qwen2.5-7b-instruct-q4_k_m.gguf"
     * Wenn null/leer, wird automatisches Mapping von baseModel verwendet
     */
    @Column(length = 255)
    private String ggufModel;

    /**
     * Provider-Typ für diesen Experten
     * Bestimmt welcher LLM-Provider verwendet wird:
     * - "ollama" → Ollama Server
     * - "java-llama-cpp" → Eingebetteter GGUF-Server
     * - "llama-server" → Externer llama-server (Port 2026)
     * - null → Verwendet den aktiven System-Provider
     */
    @Column(length = 50)
    private String providerType;

    /**
     * Standard-Temperature (kann pro Modus überschrieben werden)
     */
    private Double defaultTemperature = 0.7;

    /**
     * Standard-TopP (kann pro Modus überschrieben werden)
     */
    private Double defaultTopP = 0.9;

    /**
     * Standard num_ctx für diesen Experten (Kontext-Fenster)
     */
    private Integer defaultNumCtx = 8192;

    /**
     * Standard max_tokens für diesen Experten (maximale Antwort-Länge)
     */
    private Integer defaultMaxTokens = 4096;

    /**
     * Ob der Experte aktiv/verfügbar ist
     */
    private Boolean active = true;

    /**
     * Web-Suche automatisch aktivieren für diesen Experten
     * Wenn true, wird bei jeder Anfrage automatisch im Web gesucht
     */
    private Boolean autoWebSearch = false;

    /**
     * Such-Domains für Web-Recherche (komma-getrennt)
     * z.B. "gesetze-im-internet.de,dejure.org,bundesgerichtshof.de"
     */
    @Column(columnDefinition = "TEXT")
    private String searchDomains;

    /**
     * Maximale Anzahl Suchergebnisse
     */
    private Integer maxSearchResults = 5;

    /**
     * Dateisuche automatisch aktivieren für diesen Experten
     * Wenn true, kann der Experte in hochgeladenen Dokumenten suchen
     */
    private Boolean autoFileSearch = false;

    /**
     * Dokumenten-Verzeichnis für diesen Experten
     * z.B. "Roland" → ~/Dokumente/Fleet-Navigator/Roland/
     * Der Experte speichert generierte Dokumente in diesem Verzeichnis
     */
    @Column(length = 100)
    private String documentDirectory;

    /**
     * Bevorzugter Fleet-Mate für Dokumentenerstellung
     * z.B. "os-ubuntu-desktop-trainer"
     * Wenn null/leer und mehrere OS-Mates verfügbar → Auswahl im Frontend
     */
    @Column(length = 100)
    private String preferredMateId;

    /**
     * Ob eine Benachrichtigung angezeigt werden soll wenn zu diesem Experten gewechselt wird
     * Standard: true (Benachrichtigung wird angezeigt)
     */
    private Boolean showSwitchNotification = true;

    /**
     * Die Modi (Blickwinkel) dieses Experten
     */
    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ExpertMode> modes = new ArrayList<>();

    /**
     * ID des Standard-Modus
     */
    private Long defaultModeId;

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
     * Hilfsmethode: Modus hinzufügen
     */
    public void addMode(ExpertMode mode) {
        modes.add(mode);
        mode.setExpert(this);
    }

    /**
     * Hilfsmethode: Modus entfernen
     */
    public void removeMode(ExpertMode mode) {
        modes.remove(mode);
        mode.setExpert(null);
    }

    /**
     * Hilfsmethode: Such-Domains als Liste zurückgeben
     */
    public List<String> getSearchDomainsAsList() {
        if (searchDomains == null || searchDomains.isBlank()) {
            return new ArrayList<>();
        }
        return java.util.Arrays.stream(searchDomains.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Hilfsmethode: Such-Domains aus Liste setzen
     */
    public void setSearchDomainsFromList(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            this.searchDomains = null;
        } else {
            this.searchDomains = String.join(",", domains);
        }
    }

    /**
     * Hilfsmethode: Gibt den kombinierten System Prompt zurück (basePrompt + personalityPrompt)
     * Der personalityPrompt wird nur angehängt wenn er nicht leer ist
     */
    public String getCombinedSystemPrompt() {
        if (personalityPrompt == null || personalityPrompt.isBlank()) {
            return basePrompt;
        }
        return basePrompt + "\n\n## Kommunikationsstil:\n" + personalityPrompt;
    }
}
