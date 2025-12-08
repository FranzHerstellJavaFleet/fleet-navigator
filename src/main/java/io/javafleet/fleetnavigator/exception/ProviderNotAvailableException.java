package io.javafleet.fleetnavigator.exception;

/**
 * Exception wenn kein LLM-Provider verfügbar ist.
 */
public class ProviderNotAvailableException extends FleetNavigatorException {

    public static final String ERROR_CODE = "PROVIDER_NOT_AVAILABLE";

    public ProviderNotAvailableException(String message) {
        super(message, ERROR_CODE);
    }

    public ProviderNotAvailableException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    public static ProviderNotAvailableException notConfigured(String providerName) {
        return new ProviderNotAvailableException(
            String.format("LLM-Provider '%s' ist nicht konfiguriert. Bitte konfigurieren Sie den Provider in den Einstellungen.", providerName)
        );
    }

    public static ProviderNotAvailableException notRunning(String providerName) {
        return new ProviderNotAvailableException(
            String.format("LLM-Provider '%s' ist nicht gestartet. Bitte starten Sie den Provider.", providerName)
        );
    }

    public static ProviderNotAvailableException noProvidersAvailable() {
        return new ProviderNotAvailableException(
            "Kein LLM-Provider verfügbar. Bitte installieren und starten Sie mindestens einen Provider."
        );
    }

    public static ProviderNotAvailableException noProviderConfigured() {
        return new ProviderNotAvailableException(
            "Kein LLM-Provider konfiguriert. Bitte wählen Sie in den Einstellungen einen Provider aus."
        );
    }

    public static ProviderNotAvailableException llamaServerNotRunning() {
        return new ProviderNotAvailableException(
            "llama-server ist nicht gestartet auf Port 2026. Bitte starten Sie den Server."
        );
    }

    public static ProviderNotAvailableException connectionFailed(String providerName, String url) {
        return new ProviderNotAvailableException(
            String.format("Verbindung zu LLM-Provider '%s' unter %s fehlgeschlagen.", providerName, url)
        );
    }
}
