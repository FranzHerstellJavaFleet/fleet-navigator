package io.javafleet.fleetnavigator.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Zentrale Utility-Klasse für gemeinsame Funktionen.
 *
 * Diese Klasse konsolidiert wiederkehrende Funktionen aus verschiedenen Services:
 * - HTML Escaping (vorher in ChatService + DocumentGeneratorService dupliziert)
 * - Dateinamen-Bereinigung (vorher nur in DocumentGeneratorService)
 * - DateTimeFormatter (vorher in 6+ Services verstreut)
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.0
 */
public final class FleetUtils {

    private FleetUtils() {
        // Utility class - keine Instanziierung
    }

    // ============================================================
    // DATE/TIME FORMATTERS - Zentral definiert
    // ============================================================

    /**
     * Deutsches Datumsformat mit ausgeschriebenem Monat: "30. November 2025"
     * Verwendung: Briefe, formelle Dokumente
     */
    public static final DateTimeFormatter DATE_GERMAN_LONG =
        DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN);

    /**
     * Kurzes deutsches Datumsformat: "30.11.2025"
     * Verwendung: Tabellen, kompakte Anzeigen
     */
    public static final DateTimeFormatter DATE_GERMAN_SHORT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

    /**
     * Deutsches Datum mit Uhrzeit: "30.11.2025 18:30"
     * Verwendung: Logs, Timestamps
     */
    public static final DateTimeFormatter DATETIME_GERMAN =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN);

    /**
     * Vollständiges deutsches Datum mit Sekunden: "30.11.2025 18:30:45"
     * Verwendung: Präzise Zeitstempel
     */
    public static final DateTimeFormatter DATETIME_GERMAN_FULL =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.GERMAN);

    /**
     * Nur Uhrzeit: "18:30"
     * Verwendung: Zeitanzeigen
     */
    public static final DateTimeFormatter TIME_GERMAN =
        DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);

    /**
     * ISO-Format für Dateinamen: "2025-11-30_18-30"
     * Verwendung: Dateinamen mit Timestamp (sortierbar)
     */
    public static final DateTimeFormatter FILENAME_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    /**
     * Kompaktes ISO-Format für Dateinamen: "20251130_183045"
     * Verwendung: Kurze Dateinamen-Timestamps
     */
    public static final DateTimeFormatter FILENAME_TIMESTAMP_COMPACT =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * ISO-Datum: "2025-11-30"
     * Verwendung: API-Responses, internationale Formate
     */
    public static final DateTimeFormatter DATE_ISO =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Timezone für alle Operationen: Europe/Berlin
     */
    public static final ZoneId TIMEZONE_BERLIN = ZoneId.of("Europe/Berlin");

    // ============================================================
    // HTML ESCAPING
    // ============================================================

    /**
     * Escaped HTML-Sonderzeichen um XSS zu verhindern.
     *
     * Ersetzt:
     * - & → &amp;
     * - < → &lt;
     * - > → &gt;
     * - " → &quot;
     * - ' → &#39;
     *
     * @param text Der zu escapende Text
     * @return Escaped Text, oder leerer String wenn null
     */
    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Prüft ob ein String null oder leer/whitespace ist.
     *
     * @param text Der zu prüfende String
     * @return true wenn null, leer oder nur Whitespace
     */
    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * Prüft ob ein String nicht null und nicht leer ist.
     *
     * @param text Der zu prüfende String
     * @return true wenn nicht null und nicht leer
     */
    public static boolean isNotBlank(String text) {
        return text != null && !text.isBlank();
    }

    // ============================================================
    // FILENAME SANITIZATION
    // ============================================================

    /**
     * Bereinigt einen String für die Verwendung als Dateiname.
     *
     * - Entfernt ungültige Zeichen: / \ : * ? " < > |
     * - Ersetzt Leerzeichen durch Unterstriche
     * - Reduziert mehrfache Unterstriche
     * - Entfernt führende/nachfolgende Unterstriche
     * - Kürzt auf maximal 50 Zeichen
     *
     * @param input Der Eingabe-String
     * @return Bereinigter Dateiname, oder "Dokument" wenn leer
     */
    public static String sanitizeFilename(String input) {
        if (input == null || input.isBlank()) {
            return "Dokument";
        }

        String sanitized = input
                .replaceAll("[/\\\\:*?\"<>|]", "")    // Ungültige Zeichen entfernen
                .replaceAll("\\s+", "_")              // Leerzeichen zu Unterstrichen
                .replaceAll("_+", "_")                // Mehrfache Unterstriche reduzieren
                .replaceAll("^_|_$", "");             // Führende/nachfolgende Unterstriche

        // Kürzen auf max. 50 Zeichen
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized.isBlank() ? "Dokument" : sanitized;
    }

    /**
     * Bereinigt einen String für die Verwendung als Dateiname mit custom Länge.
     *
     * @param input Der Eingabe-String
     * @param maxLength Maximale Länge
     * @return Bereinigter Dateiname
     */
    public static String sanitizeFilename(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "Dokument";
        }

        String sanitized = input
                .replaceAll("[/\\\\:*?\"<>|]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }

        return sanitized.isBlank() ? "Dokument" : sanitized;
    }

    // ============================================================
    // STRING UTILITIES
    // ============================================================

    /**
     * Gibt den ersten nicht-leeren String zurück, oder den Fallback.
     *
     * @param value Primärer Wert
     * @param fallback Fallback-Wert
     * @return value wenn nicht leer, sonst fallback
     */
    public static String coalesce(String value, String fallback) {
        return isNotBlank(value) ? value : fallback;
    }

    /**
     * Kürzt einen String auf maxLength und fügt "..." an wenn gekürzt.
     *
     * @param text Der zu kürzende Text
     * @param maxLength Maximale Länge (inkl. "...")
     * @return Gekürzter Text oder Original wenn kürzer
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
