package io.javafleet.fleetnavigator.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * JUnit-Tests für GlobalExceptionHandler.
 *
 * Testet die zentrale Fehlerbehandlung und stellt sicher, dass
 * alle Exceptions korrekt in ApiErrorResponse umgewandelt werden.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.6.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Nested
    @DisplayName("FileUploadException Handling")
    class FileUploadExceptionHandling {

        @Test
        @DisplayName("Gibt BAD_REQUEST zurück")
        void returns_BadRequest() {
            FileUploadException ex = FileUploadException.emptyFile();

            ResponseEntity<ApiErrorResponse> response = handler.handleFileUploadException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Enthält korrekten Error-Code")
        void contains_CorrectErrorCode() {
            FileUploadException ex = FileUploadException.fileTooLarge(50);

            ResponseEntity<ApiErrorResponse> response = handler.handleFileUploadException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("FILE_UPLOAD_ERROR");
        }

        @Test
        @DisplayName("Enthält Fehlermeldung")
        void contains_ErrorMessage() {
            FileUploadException ex = FileUploadException.invalidFileType("pdf, doc");

            ResponseEntity<ApiErrorResponse> response = handler.handleFileUploadException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("pdf, doc");
        }

        @Test
        @DisplayName("Enthält Request-Pfad")
        void contains_RequestPath() {
            FileUploadException ex = FileUploadException.emptyFile();

            ResponseEntity<ApiErrorResponse> response = handler.handleFileUploadException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }
    }

    @Nested
    @DisplayName("ModelNotFoundException Handling")
    class ModelNotFoundExceptionHandling {

        @Test
        @DisplayName("Gibt NOT_FOUND zurück")
        void returns_NotFound() {
            ModelNotFoundException ex = ModelNotFoundException.byName("test-model");

            ResponseEntity<ApiErrorResponse> response = handler.handleModelNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Enthält Modellnamen in Nachricht")
        void contains_ModelName() {
            ModelNotFoundException ex = ModelNotFoundException.byName("llama-7b");

            ResponseEntity<ApiErrorResponse> response = handler.handleModelNotFoundException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("llama-7b");
        }

        @Test
        @DisplayName("Enthält Vorschläge zur Fehlerbehebung")
        void contains_Suggestions() {
            ModelNotFoundException ex = ModelNotFoundException.byName("test-model");

            ResponseEntity<ApiErrorResponse> response = handler.handleModelNotFoundException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSuggestions()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("OllamaConnectionException Handling")
    class OllamaConnectionExceptionHandling {

        @Test
        @DisplayName("Gibt SERVICE_UNAVAILABLE zurück")
        void returns_ServiceUnavailable() {
            OllamaConnectionException ex = OllamaConnectionException.connectionFailed("http://localhost:11434");

            ResponseEntity<ApiErrorResponse> response = handler.handleOllamaConnectionException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("Enthält URL in Nachricht")
        void contains_Url() {
            OllamaConnectionException ex = OllamaConnectionException.connectionFailed("http://localhost:11434");

            ResponseEntity<ApiErrorResponse> response = handler.handleOllamaConnectionException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("localhost:11434");
        }

        @Test
        @DisplayName("Enthält Vorschläge zur Fehlerbehebung")
        void contains_Suggestions() {
            OllamaConnectionException ex = OllamaConnectionException.serverNotRunning();

            ResponseEntity<ApiErrorResponse> response = handler.handleOllamaConnectionException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSuggestions()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ChatNotFoundException Handling")
    class ChatNotFoundExceptionHandling {

        @Test
        @DisplayName("Gibt NOT_FOUND zurück")
        void returns_NotFound() {
            ChatNotFoundException ex = ChatNotFoundException.byId(123L);

            ResponseEntity<ApiErrorResponse> response = handler.handleChatNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Enthält Chat-ID in Nachricht")
        void contains_ChatId() {
            ChatNotFoundException ex = ChatNotFoundException.byId(456L);

            ResponseEntity<ApiErrorResponse> response = handler.handleChatNotFoundException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("456");
        }
    }

    @Nested
    @DisplayName("ExpertNotFoundException Handling")
    class ExpertNotFoundExceptionHandling {

        @Test
        @DisplayName("Gibt NOT_FOUND zurück")
        void returns_NotFound() {
            ExpertNotFoundException ex = ExpertNotFoundException.byId(99L);

            ResponseEntity<ApiErrorResponse> response = handler.handleExpertNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Enthält Experten-ID in Nachricht")
        void contains_ExpertId() {
            ExpertNotFoundException ex = ExpertNotFoundException.byId(42L);

            ResponseEntity<ApiErrorResponse> response = handler.handleExpertNotFoundException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("42");
        }
    }

    @Nested
    @DisplayName("ProviderNotAvailableException Handling")
    class ProviderNotAvailableExceptionHandling {

        @Test
        @DisplayName("Gibt SERVICE_UNAVAILABLE zurück")
        void returns_ServiceUnavailable() {
            ProviderNotAvailableException ex = ProviderNotAvailableException.notConfigured("ollama");

            ResponseEntity<ApiErrorResponse> response = handler.handleProviderNotAvailableException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("Enthält Provider-Namen")
        void contains_ProviderName() {
            ProviderNotAvailableException ex = ProviderNotAvailableException.notConfigured("java-llama-cpp");

            ResponseEntity<ApiErrorResponse> response = handler.handleProviderNotAvailableException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("java-llama-cpp");
        }

        @Test
        @DisplayName("noProvidersAvailable enthält korrekte Meldung")
        void noProvidersAvailable_ContainsCorrectMessage() {
            ProviderNotAvailableException ex = ProviderNotAvailableException.noProvidersAvailable();

            ResponseEntity<ApiErrorResponse> response = handler.handleProviderNotAvailableException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Kein LLM-Provider");
        }
    }

    @Nested
    @DisplayName("HuggingFaceException Handling")
    class HuggingFaceExceptionHandling {

        @Test
        @DisplayName("Gibt BAD_GATEWAY zurück")
        void returns_BadGateway() {
            HuggingFaceException ex = HuggingFaceException.connectionFailed();

            ResponseEntity<ApiErrorResponse> response = handler.handleHuggingFaceException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        @DisplayName("apiError enthält Statuscode")
        void apiError_ContainsStatusCode() {
            HuggingFaceException ex = HuggingFaceException.apiError(404);

            ResponseEntity<ApiErrorResponse> response = handler.handleHuggingFaceException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("404");
        }

        @Test
        @DisplayName("downloadFailed enthält Dateinamen")
        void downloadFailed_ContainsFilename() {
            HuggingFaceException ex = HuggingFaceException.downloadFailed("model.gguf");

            ResponseEntity<ApiErrorResponse> response = handler.handleHuggingFaceException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("model.gguf");
        }
    }

    @Nested
    @DisplayName("ModelLoadException Handling")
    class ModelLoadExceptionHandling {

        @Test
        @DisplayName("Gibt INTERNAL_SERVER_ERROR zurück")
        void returns_InternalServerError() {
            ModelLoadException ex = ModelLoadException.outOfMemory("big-model");

            ResponseEntity<ApiErrorResponse> response = handler.handleModelLoadException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("loadFailed enthält Modellnamen und Grund")
        void loadFailed_ContainsModelAndReason() {
            ModelLoadException ex = ModelLoadException.loadFailed("llama-7b", "Speicherfehler");

            ResponseEntity<ApiErrorResponse> response = handler.handleModelLoadException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("llama-7b");
            assertThat(response.getBody().getMessage()).contains("Speicherfehler");
        }
    }

    @Nested
    @DisplayName("ApiErrorResponse Struktur")
    class ApiErrorResponseStructure {

        @Test
        @DisplayName("Enthält Timestamp")
        void contains_Timestamp() {
            FileUploadException ex = FileUploadException.emptyFile();

            ResponseEntity<ApiErrorResponse> response = handler.handleFileUploadException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Enthält Status-Code als Zahl")
        void contains_StatusCode() {
            FileUploadException ex = FileUploadException.emptyFile();

            ResponseEntity<ApiErrorResponse> response = handler.handleFileUploadException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Enthält Request-Pfad")
        void contains_Path() {
            FileUploadException ex = FileUploadException.emptyFile();

            ResponseEntity<ApiErrorResponse> response = handler.handleFileUploadException(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        }
    }
}
