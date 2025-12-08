package io.javafleet.fleetnavigator.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * JUnit-Tests für FileUploadException.
 *
 * Testet alle Factory-Methoden und stellt sicher, dass die
 * Fehlermeldungen korrekt auf Deutsch sind.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.6.0
 */
@DisplayName("FileUploadException Tests")
class FileUploadExceptionTest {

    @Nested
    @DisplayName("Factory-Methoden")
    class FactoryMethods {

        @Test
        @DisplayName("emptyFile() erzeugt korrekte Fehlermeldung")
        void emptyFile_CreatesCorrectMessage() {
            FileUploadException ex = FileUploadException.emptyFile();

            assertThat(ex.getMessage()).contains("leer");
            assertThat(ex.getErrorCode()).isEqualTo("FILE_UPLOAD_ERROR");
        }

        @Test
        @DisplayName("fileTooLarge() enthält maximale Größe")
        void fileTooLarge_ContainsMaxSize() {
            FileUploadException ex = FileUploadException.fileTooLarge(50);

            assertThat(ex.getMessage()).contains("50 MB");
            assertThat(ex.getMessage()).contains("zu groß");
            assertThat(ex.getErrorCode()).isEqualTo("FILE_UPLOAD_ERROR");
        }

        @Test
        @DisplayName("invalidFileType() enthält erlaubte Typen")
        void invalidFileType_ContainsAllowedTypes() {
            String allowedTypes = "pdf, doc, docx";
            FileUploadException ex = FileUploadException.invalidFileType(allowedTypes);

            assertThat(ex.getMessage()).contains(allowedTypes);
            assertThat(ex.getMessage()).contains("Ungültiger Dateityp");
        }

        @Test
        @DisplayName("invalidFileName() erzeugt korrekte Meldung")
        void invalidFileName_CreatesCorrectMessage() {
            FileUploadException ex = FileUploadException.invalidFileName();

            assertThat(ex.getMessage()).contains("Dateiname");
            assertThat(ex.getMessage()).contains("nicht erlaubte Zeichen");
        }

        @Test
        @DisplayName("suspiciousFile() enthält Grund")
        void suspiciousFile_ContainsReason() {
            String reason = "Path Traversal Versuch";
            FileUploadException ex = FileUploadException.suspiciousFile(reason);

            assertThat(ex.getMessage()).contains(reason);
            assertThat(ex.getMessage()).contains("Sicherheitsgründen");
        }

        @Test
        @DisplayName("storageFailed() erzeugt korrekte Meldung")
        void storageFailed_CreatesCorrectMessage() {
            FileUploadException ex = FileUploadException.storageFailed();

            assertThat(ex.getMessage()).contains("nicht gespeichert");
        }

        @Test
        @DisplayName("processingFailed() enthält Details")
        void processingFailed_ContainsDetails() {
            String details = "PDF konnte nicht gelesen werden";
            FileUploadException ex = FileUploadException.processingFailed(details);

            assertThat(ex.getMessage()).contains(details);
            assertThat(ex.getMessage()).contains("nicht verarbeitet");
        }
    }

    @Nested
    @DisplayName("Exception-Eigenschaften")
    class ExceptionProperties {

        @Test
        @DisplayName("Alle Exceptions haben ERROR_CODE")
        void allExceptions_HaveErrorCode() {
            assertThat(FileUploadException.ERROR_CODE).isEqualTo("FILE_UPLOAD_ERROR");
        }

        @Test
        @DisplayName("Exception mit Cause behält Cause")
        void exceptionWithCause_RetainsCause() {
            Exception cause = new RuntimeException("Original error");
            FileUploadException ex = new FileUploadException("Wrapper message", cause);

            assertThat(ex.getCause()).isEqualTo(cause);
            assertThat(ex.getMessage()).isEqualTo("Wrapper message");
        }

        @Test
        @DisplayName("Exception erbt von FleetNavigatorException")
        void exception_ExtendsFleetNavigatorException() {
            FileUploadException ex = FileUploadException.emptyFile();

            assertThat(ex).isInstanceOf(FleetNavigatorException.class);
        }
    }
}
