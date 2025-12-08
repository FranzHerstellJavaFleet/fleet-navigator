package io.javafleet.fleetnavigator.model;

/**
 * Aufgabentypen für intelligente Modellauswahl
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.8
 */
public enum TaskType {
    /**
     * Bild- und PDF-Analyse (Vision)
     */
    VISION,

    /**
     * Email-Klassifizierung und Sortierung
     */
    EMAIL_CLASSIFICATION,

    /**
     * Code-Generierung und -Review
     */
    CODE_GENERATION,

    /**
     * Log-Datei-Analyse
     */
    LOG_ANALYSIS,

    /**
     * Brief- und Dokumenten-Generierung
     */
    DOCUMENT_GENERATION,

    /**
     * Allgemeiner Chat
     */
    GENERAL_CHAT,

    /**
     * Schnelle Antworten (Definitionen, kurze Fragen)
     */
    FAST_RESPONSE
}
