package io.javafleet.fleetnavigator.exception;

/**
 * Exception für Datei-Upload Fehler.
 */
public class FileUploadException extends FleetNavigatorException {

    public static final String ERROR_CODE = "FILE_UPLOAD_ERROR";

    public FileUploadException(String message) {
        super(message, ERROR_CODE);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    public static FileUploadException emptyFile() {
        return new FileUploadException("Datei ist leer. Bitte wählen Sie eine gültige Datei aus.");
    }

    public static FileUploadException fileTooLarge(long maxSizeMb) {
        return new FileUploadException(
            String.format("Datei ist zu groß. Maximale Größe: %d MB", maxSizeMb)
        );
    }

    public static FileUploadException invalidFileType(String allowedTypes) {
        return new FileUploadException(
            String.format("Ungültiger Dateityp. Erlaubt sind: %s", allowedTypes)
        );
    }

    public static FileUploadException invalidFileName() {
        return new FileUploadException(
            "Ungültiger Dateiname. Der Dateiname enthält nicht erlaubte Zeichen."
        );
    }

    public static FileUploadException suspiciousFile(String reason) {
        return new FileUploadException(
            String.format("Datei wurde aus Sicherheitsgründen abgelehnt: %s", reason)
        );
    }

    public static FileUploadException storageFailed() {
        return new FileUploadException(
            "Datei konnte nicht gespeichert werden. Bitte versuchen Sie es erneut."
        );
    }

    public static FileUploadException processingFailed(String details) {
        return new FileUploadException(
            String.format("Datei konnte nicht verarbeitet werden: %s", details)
        );
    }
}
