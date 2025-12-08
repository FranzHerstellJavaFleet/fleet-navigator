package io.javafleet.fleetnavigator.exception;

/**
 * Exception wenn ein angefordertes Modell nicht gefunden wird.
 */
public class ModelNotFoundException extends FleetNavigatorException {

    public static final String ERROR_CODE = "MODEL_NOT_FOUND";

    public ModelNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    public ModelNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    public static ModelNotFoundException byName(String modelName) {
        return new ModelNotFoundException(
            String.format("Modell '%s' wurde nicht gefunden.", modelName)
        );
    }

    public static ModelNotFoundException notInstalled(String modelName) {
        return new ModelNotFoundException(
            String.format("Modell '%s' ist nicht installiert. Bitte laden Sie es zuerst herunter.", modelName)
        );
    }

    public static ModelNotFoundException fileNotFound(String path) {
        return new ModelNotFoundException(
            String.format("Modell-Datei nicht gefunden: %s", path)
        );
    }

    public static ModelNotFoundException noModelsAvailable() {
        return new ModelNotFoundException(
            "Keine Modelle verfügbar. Bitte laden Sie zuerst ein Modell herunter."
        );
    }
}
