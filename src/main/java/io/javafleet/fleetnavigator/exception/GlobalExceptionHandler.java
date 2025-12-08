package io.javafleet.fleetnavigator.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Globaler Exception Handler für alle REST API Fehler.
 * Wandelt Exceptions in benutzerfreundliche deutsche Fehlermeldungen um.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== Fleet Navigator Spezifische Exceptions ====================

    @ExceptionHandler(OllamaConnectionException.class)
    public ResponseEntity<ApiErrorResponse> handleOllamaConnectionException(
            OllamaConnectionException ex, HttpServletRequest request) {
        log.warn("Ollama-Verbindungsfehler: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .details("Der Ollama-Server konnte nicht erreicht werden.")
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Prüfen Sie, ob Ollama installiert ist",
                        "Starten Sie Ollama mit 'ollama serve'",
                        "Prüfen Sie die Firewall-Einstellungen"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleModelNotFoundException(
            ModelNotFoundException ex, HttpServletRequest request) {
        log.warn("Modell nicht gefunden: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Laden Sie das Modell über die Modellverwaltung herunter",
                        "Prüfen Sie den Modell-Namen auf Tippfehler"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ModelLoadException.class)
    public ResponseEntity<ApiErrorResponse> handleModelLoadException(
            ModelLoadException ex, HttpServletRequest request) {
        log.error("Fehler beim Laden des Modells: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Versuchen Sie ein kleineres Modell",
                        "Schließen Sie andere Anwendungen um Speicher freizugeben",
                        "Laden Sie das Modell erneut herunter"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ExpertNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleExpertNotFoundException(
            ExpertNotFoundException ex, HttpServletRequest request) {
        log.warn("Experte nicht gefunden: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotFoundException(
            ChatNotFoundException ex, HttpServletRequest request) {
        log.warn("Chat nicht gefunden: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ProviderNotAvailableException.class)
    public ResponseEntity<ApiErrorResponse> handleProviderNotAvailableException(
            ProviderNotAvailableException ex, HttpServletRequest request) {
        log.warn("Provider nicht verfügbar: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .details("Der gewählte KI-Provider ist momentan nicht erreichbar.")
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Wählen Sie einen anderen Provider in den Einstellungen",
                        "Prüfen Sie, ob der Server läuft",
                        "Starten Sie den llama-server auf Port 2026"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(HuggingFaceException.class)
    public ResponseEntity<ApiErrorResponse> handleHuggingFaceException(
            HuggingFaceException ex, HttpServletRequest request) {
        log.warn("HuggingFace-Fehler: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.BAD_GATEWAY.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Prüfen Sie Ihre Internetverbindung",
                        "Versuchen Sie es in einigen Minuten erneut"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleFileUploadException(
            FileUploadException ex, HttpServletRequest request) {
        log.warn("Datei-Upload-Fehler: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Allgemeine Fleet Navigator Exception ====================

    @ExceptionHandler(FleetNavigatorException.class)
    public ResponseEntity<ApiErrorResponse> handleFleetNavigatorException(
            FleetNavigatorException ex, HttpServletRequest request) {
        log.error("Fleet Navigator Fehler: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ==================== Spring Framework Exceptions ====================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Datei zu groß: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message("Die hochgeladene Datei ist zu groß.")
                .details("Maximale Dateigröße überschritten.")
                .errorCode("FILE_TOO_LARGE")
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Wählen Sie eine kleinere Datei",
                        "Komprimieren Sie die Datei vor dem Upload"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validierungsfehler: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Ungültige Eingabe");

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message("Ungültige Eingabe: " + errorMessage)
                .errorCode("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Ungültiges Argument: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode("INVALID_ARGUMENT")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Netzwerk-Exceptions ====================

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ApiErrorResponse> handleConnectException(
            ConnectException ex, HttpServletRequest request) {
        log.error("Verbindungsfehler: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message("Verbindung zum Server fehlgeschlagen.")
                .details(ex.getMessage())
                .errorCode("CONNECTION_FAILED")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Prüfen Sie, ob der Dienst läuft",
                        "Prüfen Sie die Netzwerkverbindung"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // ==================== Fallback für alle anderen Exceptions ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String path = request.getRequestURI();

        // Bei statischen Ressourcen (JS, CSS, etc.) nicht mit JSON antworten
        // da der Browser text/javascript oder text/css erwartet
        if (isStaticResourceRequest(path)) {
            log.debug("Fehler bei statischer Ressource ignoriert: {} - {}", path, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Bei Client-Abbruch (broken pipe) nicht als Fehler loggen
        if (isClientAbortException(ex)) {
            log.debug("Client-Verbindung abgebrochen für: {}", path);
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        log.error("Unerwarteter Fehler: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .message("Ein unerwarteter Fehler ist aufgetreten.")
                .details(ex.getClass().getSimpleName() + ": " + ex.getMessage())
                .errorCode("INTERNAL_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .suggestions(List.of(
                        "Versuchen Sie es erneut",
                        "Starten Sie die Anwendung neu",
                        "Kontaktieren Sie den Support wenn das Problem bestehen bleibt"
                ))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Prüft ob der Request eine statische Ressource anfordert.
     */
    private boolean isStaticResourceRequest(String path) {
        if (path == null) return false;
        return path.endsWith(".js")
            || path.endsWith(".css")
            || path.endsWith(".map")
            || path.endsWith(".ico")
            || path.endsWith(".png")
            || path.endsWith(".jpg")
            || path.endsWith(".jpeg")
            || path.endsWith(".gif")
            || path.endsWith(".svg")
            || path.endsWith(".woff")
            || path.endsWith(".woff2")
            || path.endsWith(".ttf")
            || path.endsWith(".eot")
            || path.startsWith("/assets/");
    }

    /**
     * Prüft ob die Exception durch Client-Abbruch verursacht wurde (broken pipe).
     */
    private boolean isClientAbortException(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            String className = cause.getClass().getName();
            String message = cause.getMessage();
            if (className.contains("ClientAbortException")
                || (message != null && message.contains("broken pipe"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
