package io.javafleet.fleetnavigator.exception;

/**
 * Exception wenn ein Modell nicht geladen werden kann.
 */
public class ModelLoadException extends FleetNavigatorException {

    public static final String ERROR_CODE = "MODEL_LOAD_ERROR";

    public ModelLoadException(String message) {
        super(message, ERROR_CODE);
    }

    public ModelLoadException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    public static ModelLoadException loadFailed(String modelName, String reason) {
        return new ModelLoadException(
            String.format("Modell '%s' konnte nicht geladen werden: %s", modelName, reason)
        );
    }

    public static ModelLoadException outOfMemory(String modelName) {
        return new ModelLoadException(
            String.format("Nicht genügend Speicher für Modell '%s'. Versuchen Sie ein kleineres Modell.", modelName)
        );
    }

    public static ModelLoadException insufficientMemory(String modelName, long requiredMb) {
        return new ModelLoadException(
            String.format(
                "Nicht genügend Speicher für Modell '%s'. Benötigt: %d MB. " +
                "Versuchen Sie ein kleineres Modell oder schließen Sie andere Anwendungen.",
                modelName, requiredMb
            )
        );
    }

    public static ModelLoadException invalidFormat(String modelName) {
        return new ModelLoadException(
            String.format("Modell '%s' hat ein ungültiges Format.", modelName)
        );
    }

    public static ModelLoadException corruptFile(String modelName) {
        return new ModelLoadException(
            String.format("Modell-Datei '%s' ist beschädigt. Bitte laden Sie das Modell erneut herunter.", modelName)
        );
    }

    public static ModelLoadException unsupportedFormat(String modelName, String format) {
        return new ModelLoadException(
            String.format("Modell '%s' hat ein nicht unterstütztes Format: %s", modelName, format)
        );
    }
}
