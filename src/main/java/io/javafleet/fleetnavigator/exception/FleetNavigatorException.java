package io.javafleet.fleetnavigator.exception;

/**
 * Basis-Exception für alle Fleet Navigator spezifischen Fehler.
 * Enthält einen errorCode für eindeutige Identifikation.
 */
public class FleetNavigatorException extends RuntimeException {

    private final String errorCode;

    public FleetNavigatorException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public FleetNavigatorException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
