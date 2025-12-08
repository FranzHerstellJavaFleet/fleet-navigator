package io.javafleet.fleetnavigator.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardisierte API-Fehlerantwort für alle Endpunkte.
 *
 * Struktur:
 * - message: Benutzerfreundliche Fehlermeldung (deutsch)
 * - details: Technische Details oder Lösungsvorschläge
 * - errorCode: Eindeutiger Fehlercode für Debugging
 * - timestamp: Zeitpunkt des Fehlers
 * - path: Betroffener API-Endpunkt
 * - suggestions: Liste von Lösungsvorschlägen
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /**
     * Benutzerfreundliche Fehlermeldung auf Deutsch
     */
    private String message;

    /**
     * Technische Details oder erweiterte Erklärung
     */
    private String details;

    /**
     * Eindeutiger Fehlercode (z.B. "OLLAMA_CONNECTION_FAILED")
     */
    private String errorCode;

    /**
     * HTTP Status Code
     */
    private int status;

    /**
     * Zeitpunkt des Fehlers
     */
    private LocalDateTime timestamp;

    /**
     * Betroffener API-Pfad
     */
    private String path;

    /**
     * Liste von Lösungsvorschlägen für den Benutzer
     */
    private List<String> suggestions;

    /**
     * Schnelle Erstellung einer einfachen Fehlermeldung
     */
    public static ApiErrorResponse of(String message, String errorCode, int status) {
        return ApiErrorResponse.builder()
                .message(message)
                .errorCode(errorCode)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Schnelle Erstellung mit Details
     */
    public static ApiErrorResponse of(String message, String details, String errorCode, int status) {
        return ApiErrorResponse.builder()
                .message(message)
                .details(details)
                .errorCode(errorCode)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
