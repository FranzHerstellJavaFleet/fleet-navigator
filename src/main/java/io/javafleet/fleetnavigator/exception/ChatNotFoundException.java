package io.javafleet.fleetnavigator.exception;

/**
 * Exception wenn ein Chat nicht gefunden wird.
 */
public class ChatNotFoundException extends FleetNavigatorException {

    public static final String ERROR_CODE = "CHAT_NOT_FOUND";

    public ChatNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    public static ChatNotFoundException byId(Long chatId) {
        return new ChatNotFoundException(
            String.format("Chat mit ID %d wurde nicht gefunden.", chatId)
        );
    }
}
