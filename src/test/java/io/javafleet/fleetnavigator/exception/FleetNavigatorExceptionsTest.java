package io.javafleet.fleetnavigator.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * JUnit-Tests für alle FleetNavigator Exception-Typen.
 *
 * Testet die Factory-Methoden und Fehlermeldungen aller Custom Exceptions.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.6.0
 */
@DisplayName("FleetNavigator Exception Tests")
class FleetNavigatorExceptionsTest {

    @Nested
    @DisplayName("OllamaConnectionException")
    class OllamaConnectionExceptionTests {

        @Test
        @DisplayName("connectionFailed() enthält URL")
        void connectionFailed_ContainsUrl() {
            String url = "http://localhost:11434";
            OllamaConnectionException ex = OllamaConnectionException.connectionFailed(url);

            assertThat(ex.getMessage()).contains(url);
            assertThat(ex.getMessage()).contains("nicht erreichbar");
            assertThat(ex.getErrorCode()).isEqualTo("OLLAMA_CONNECTION_ERROR");
        }

        @Test
        @DisplayName("timeout() enthält Timeout-Zeit")
        void timeout_ContainsTimeoutSeconds() {
            OllamaConnectionException ex = OllamaConnectionException.timeout(30);

            assertThat(ex.getMessage()).contains("30");
            assertThat(ex.getMessage()).contains("Zeitüberschreitung");
        }

        @Test
        @DisplayName("modelNotAvailable() enthält Modellnamen")
        void modelNotAvailable_ContainsModelName() {
            OllamaConnectionException ex = OllamaConnectionException.modelNotAvailable("llama2:7b");

            assertThat(ex.getMessage()).contains("llama2:7b");
            assertThat(ex.getMessage()).contains("nicht verfügbar");
        }
    }

    @Nested
    @DisplayName("ModelNotFoundException")
    class ModelNotFoundExceptionTests {

        @Test
        @DisplayName("byName() enthält Modellnamen")
        void byName_ContainsModelName() {
            ModelNotFoundException ex = ModelNotFoundException.byName("qwen2.5-coder");

            assertThat(ex.getMessage()).contains("qwen2.5-coder");
            assertThat(ex.getErrorCode()).isEqualTo("MODEL_NOT_FOUND");
        }

        @Test
        @DisplayName("noModelsAvailable() erzeugt korrekte Meldung")
        void noModelsAvailable_CreatesCorrectMessage() {
            ModelNotFoundException ex = ModelNotFoundException.noModelsAvailable();

            assertThat(ex.getMessage()).contains("Keine Modelle");
        }
    }

    @Nested
    @DisplayName("ModelLoadException")
    class ModelLoadExceptionTests {

        @Test
        @DisplayName("loadFailed() enthält Modellnamen und Grund")
        void loadFailed_ContainsModelAndReason() {
            ModelLoadException ex = ModelLoadException.loadFailed("llama-7b", "Nicht genug VRAM");

            assertThat(ex.getMessage()).contains("llama-7b");
            assertThat(ex.getMessage()).contains("Nicht genug VRAM");
            assertThat(ex.getErrorCode()).isEqualTo("MODEL_LOAD_ERROR");
        }

        @Test
        @DisplayName("outOfMemory() enthält Modellnamen")
        void outOfMemory_ContainsModelName() {
            ModelLoadException ex = ModelLoadException.outOfMemory("big-model");

            assertThat(ex.getMessage()).contains("big-model");
            assertThat(ex.getMessage()).contains("Speicher");
        }

        @Test
        @DisplayName("invalidFormat() enthält Modellnamen")
        void invalidFormat_ContainsModelName() {
            ModelLoadException ex = ModelLoadException.invalidFormat("corrupted.gguf");

            assertThat(ex.getMessage()).contains("corrupted.gguf");
            assertThat(ex.getMessage()).contains("ungültiges Format");
        }
    }

    @Nested
    @DisplayName("ExpertNotFoundException")
    class ExpertNotFoundExceptionTests {

        @Test
        @DisplayName("byId() enthält ID")
        void byId_ContainsId() {
            ExpertNotFoundException ex = ExpertNotFoundException.byId(42L);

            assertThat(ex.getMessage()).contains("42");
            assertThat(ex.getErrorCode()).isEqualTo("EXPERT_NOT_FOUND");
        }

        @Test
        @DisplayName("byName() enthält Namen")
        void byName_ContainsName() {
            ExpertNotFoundException ex = ExpertNotFoundException.byName("Rechtsanwalt Roland");

            assertThat(ex.getMessage()).contains("Rechtsanwalt Roland");
        }
    }

    @Nested
    @DisplayName("ChatNotFoundException")
    class ChatNotFoundExceptionTests {

        @Test
        @DisplayName("byId() enthält Chat-ID")
        void byId_ContainsChatId() {
            ChatNotFoundException ex = ChatNotFoundException.byId(123L);

            assertThat(ex.getMessage()).contains("123");
            assertThat(ex.getErrorCode()).isEqualTo("CHAT_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("ProviderNotAvailableException")
    class ProviderNotAvailableExceptionTests {

        @Test
        @DisplayName("notConfigured() enthält Provider-Namen")
        void notConfigured_ContainsProviderName() {
            ProviderNotAvailableException ex = ProviderNotAvailableException.notConfigured("ollama");

            assertThat(ex.getMessage()).contains("ollama");
            assertThat(ex.getMessage()).contains("nicht konfiguriert");
            assertThat(ex.getErrorCode()).isEqualTo("PROVIDER_NOT_AVAILABLE");
        }

        @Test
        @DisplayName("notRunning() enthält Provider-Namen")
        void notRunning_ContainsProviderName() {
            ProviderNotAvailableException ex = ProviderNotAvailableException.notRunning("llama-server");

            assertThat(ex.getMessage()).contains("llama-server");
            assertThat(ex.getMessage()).contains("nicht gestartet");
        }

        @Test
        @DisplayName("noProvidersAvailable() erzeugt korrekte Meldung")
        void noProvidersAvailable_CreatesCorrectMessage() {
            ProviderNotAvailableException ex = ProviderNotAvailableException.noProvidersAvailable();

            assertThat(ex.getMessage()).contains("Kein LLM-Provider");
        }
    }

    @Nested
    @DisplayName("HuggingFaceException")
    class HuggingFaceExceptionTests {

        @Test
        @DisplayName("apiError() enthält Statuscode")
        void apiError_ContainsStatusCode() {
            HuggingFaceException ex = HuggingFaceException.apiError(404);

            assertThat(ex.getMessage()).contains("404");
            assertThat(ex.getErrorCode()).isEqualTo("HUGGINGFACE_ERROR");
        }

        @Test
        @DisplayName("downloadFailed() enthält Dateinamen")
        void downloadFailed_ContainsFilename() {
            HuggingFaceException ex = HuggingFaceException.downloadFailed("model.gguf");

            assertThat(ex.getMessage()).contains("model.gguf");
        }

        @Test
        @DisplayName("modelNotFound() enthält Modell-ID")
        void modelNotFound_ContainsModelId() {
            HuggingFaceException ex = HuggingFaceException.modelNotFound("TheBloke/Llama-2-7B-GGUF");

            assertThat(ex.getMessage()).contains("TheBloke/Llama-2-7B-GGUF");
        }

        @Test
        @DisplayName("rateLimited() erzeugt korrekte Meldung")
        void rateLimited_CreatesCorrectMessage() {
            HuggingFaceException ex = HuggingFaceException.rateLimited();

            assertThat(ex.getMessage()).contains("Rate-Limit");
        }
    }

    @Nested
    @DisplayName("FleetNavigatorException (Basis)")
    class FleetNavigatorExceptionTests {

        @Test
        @DisplayName("Basis-Exception hat ErrorCode")
        void baseException_HasErrorCode() {
            FleetNavigatorException ex = new FleetNavigatorException("Test message", "TEST_CODE");

            assertThat(ex.getMessage()).isEqualTo("Test message");
            assertThat(ex.getErrorCode()).isEqualTo("TEST_CODE");
        }

        @Test
        @DisplayName("Exception mit Cause behält Cause")
        void exceptionWithCause_RetainsCause() {
            RuntimeException cause = new RuntimeException("Original");
            FleetNavigatorException ex = new FleetNavigatorException("Wrapper", "CODE", cause);

            assertThat(ex.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Alle Exceptions sind RuntimeExceptions")
    class RuntimeExceptionInheritance {

        @Test
        @DisplayName("Alle Exceptions erben von RuntimeException")
        void allExceptions_AreRuntimeExceptions() {
            assertThat(new FleetNavigatorException("msg", "code")).isInstanceOf(RuntimeException.class);
            assertThat(FileUploadException.emptyFile()).isInstanceOf(RuntimeException.class);
            assertThat(ModelNotFoundException.byName("x")).isInstanceOf(RuntimeException.class);
            assertThat(ChatNotFoundException.byId(1L)).isInstanceOf(RuntimeException.class);
            assertThat(ExpertNotFoundException.byId(1L)).isInstanceOf(RuntimeException.class);
            assertThat(OllamaConnectionException.connectionFailed("x")).isInstanceOf(RuntimeException.class);
            assertThat(ProviderNotAvailableException.noProvidersAvailable()).isInstanceOf(RuntimeException.class);
            assertThat(HuggingFaceException.rateLimited()).isInstanceOf(RuntimeException.class);
            assertThat(ModelLoadException.outOfMemory("x")).isInstanceOf(RuntimeException.class);
        }
    }
}
