package io.javafleet.fleetnavigator.exception;

/**
 * Exception wenn Ollama-Server nicht erreichbar ist.
 */
public class OllamaConnectionException extends FleetNavigatorException {

    public static final String ERROR_CODE = "OLLAMA_CONNECTION_ERROR";

    public OllamaConnectionException(String message) {
        super(message, ERROR_CODE);
    }

    public OllamaConnectionException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    public static OllamaConnectionException serverNotRunning() {
        return new OllamaConnectionException(
            "Ollama-Server ist nicht erreichbar. Bitte starten Sie Ollama mit 'ollama serve'."
        );
    }

    public static OllamaConnectionException connectionFailed(String url) {
        return new OllamaConnectionException(
            String.format("Ollama-Server unter %s ist nicht erreichbar.", url)
        );
    }

    public static OllamaConnectionException connectionRefused(String host, int port) {
        return new OllamaConnectionException(
            String.format("Verbindung zu Ollama auf %s:%d abgelehnt. Läuft der Server?", host, port)
        );
    }

    public static OllamaConnectionException timeout(int timeoutSeconds) {
        return new OllamaConnectionException(
            String.format("Zeitüberschreitung nach %d Sekunden bei der Verbindung zu Ollama.", timeoutSeconds)
        );
    }

    public static OllamaConnectionException modelNotAvailable(String modelName) {
        return new OllamaConnectionException(
            String.format("Modell '%s' ist in Ollama nicht verfügbar. Bitte mit 'ollama pull %s' herunterladen.",
                modelName, modelName)
        );
    }
}
