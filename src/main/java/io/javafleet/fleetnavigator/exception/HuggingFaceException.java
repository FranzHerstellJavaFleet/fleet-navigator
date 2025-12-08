package io.javafleet.fleetnavigator.exception;

/**
 * Exception für HuggingFace API Fehler.
 */
public class HuggingFaceException extends FleetNavigatorException {

    public static final String ERROR_CODE = "HUGGINGFACE_ERROR";

    public HuggingFaceException(String message) {
        super(message, ERROR_CODE);
    }

    public HuggingFaceException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    public static HuggingFaceException connectionFailed() {
        return new HuggingFaceException(
            "Verbindung zu HuggingFace fehlgeschlagen. Bitte prüfen Sie Ihre Internetverbindung."
        );
    }

    public static HuggingFaceException apiError(int statusCode) {
        return new HuggingFaceException(
            String.format("HuggingFace API-Fehler: HTTP %d", statusCode)
        );
    }

    public static HuggingFaceException downloadFailed(String filename) {
        return new HuggingFaceException(
            String.format("Download von '%s' fehlgeschlagen.", filename)
        );
    }

    public static HuggingFaceException modelNotFound(String modelId) {
        return new HuggingFaceException(
            String.format("Modell '%s' wurde auf HuggingFace nicht gefunden.", modelId)
        );
    }

    public static HuggingFaceException rateLimited() {
        return new HuggingFaceException(
            "Rate-Limit erreicht. Bitte warten Sie einen Moment und versuchen Sie es erneut."
        );
    }

    public static HuggingFaceException invalidToken() {
        return new HuggingFaceException(
            "Ungültiger oder fehlender HuggingFace API-Token. Bitte konfigurieren Sie einen gültigen Token."
        );
    }

    public static HuggingFaceException timeout(int timeoutSeconds) {
        return new HuggingFaceException(
            String.format("Zeitüberschreitung nach %d Sekunden beim Zugriff auf HuggingFace.", timeoutSeconds)
        );
    }
}
