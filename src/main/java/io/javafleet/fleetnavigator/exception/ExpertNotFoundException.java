package io.javafleet.fleetnavigator.exception;

/**
 * Exception wenn ein Experte nicht gefunden wird.
 */
public class ExpertNotFoundException extends FleetNavigatorException {

    public static final String ERROR_CODE = "EXPERT_NOT_FOUND";

    public ExpertNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    public static ExpertNotFoundException byId(Long expertId) {
        return new ExpertNotFoundException(
            String.format("Experte mit ID %d wurde nicht gefunden.", expertId)
        );
    }

    public static ExpertNotFoundException byName(String expertName) {
        return new ExpertNotFoundException(
            String.format("Experte '%s' wurde nicht gefunden.", expertName)
        );
    }
}
