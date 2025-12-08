package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.exception.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

/**
 * JUnit-Tests für FileUploadValidator.
 *
 * Testet alle Validierungsregeln für Datei-Uploads:
 * - Dateigröße
 * - Dateiendung
 * - MIME-Type
 * - Dateiname (gefährliche Zeichen, Path Traversal)
 * - Verdächtige Inhalte
 *
 * @author JavaFleet Systems Consulting
 * @since 0.6.0
 */
@DisplayName("FileUploadValidator Tests")
class FileUploadValidatorTest {

    private FileUploadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileUploadValidator();
    }

    @Nested
    @DisplayName("Leere Dateien")
    class EmptyFileValidation {

        @Test
        @DisplayName("Null-Datei wird abgelehnt")
        void nullFile_IsRejected() {
            assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("leer");
        }

        @Test
        @DisplayName("Leere Datei wird abgelehnt")
        void emptyFile_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[0]
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("leer");
        }
    }

    @Nested
    @DisplayName("Dateigröße")
    class FileSizeValidation {

        @Test
        @DisplayName("Kleine Datei wird akzeptiert")
        void smallFile_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Zu große Datei wird abgelehnt (>50MB)")
        void tooLargeFile_IsRejected() {
            // 51 MB Datei simulieren
            byte[] largeContent = new byte[51 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                "file", "large.txt", "text/plain", largeContent
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("zu groß")
                .hasMessageContaining("50 MB");
        }
    }

    @Nested
    @DisplayName("Dateiendungen")
    class FileExtensionValidation {

        @Test
        @DisplayName("PDF wird akzeptiert")
        void pdfFile_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "PDF content".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Java-Datei wird akzeptiert")
        void javaFile_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "Main.java", "text/x-java-source", "class Main {}".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Bild wird akzeptiert")
        void imageFile_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("EXE-Datei wird abgelehnt")
        void exeFile_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "MZ".getBytes()
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("Ungültiger Dateityp");
        }

        @Test
        @DisplayName("Datei ohne Endung wird abgelehnt")
        void fileWithoutExtension_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "application/octet-stream", "content".getBytes()
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("Ungültiger Dateityp");
        }
    }

    @Nested
    @DisplayName("Dateinamen-Validierung")
    class FileNameValidation {

        @Test
        @DisplayName("Normaler Dateiname wird akzeptiert")
        void normalFileName_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "mein-dokument_2024.pdf", "application/pdf", "content".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Dateiname mit .. wird abgelehnt (Path Traversal)")
        void pathTraversal_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "../../../etc/passwd", "text/plain", "content".getBytes()
            );

            // Wird durch INVALID_FILENAME_PATTERN abgelehnt (enthält verbotene Zeichen)
            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("nicht erlaubte Zeichen");
        }

        @Test
        @DisplayName("Dateiname mit führendem / wird abgelehnt")
        void absolutePath_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "/etc/passwd", "text/plain", "content".getBytes()
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("Doppelte gefährliche Extension wird abgelehnt")
        void doubleExtension_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "harmlos.pdf.exe", "application/octet-stream", "content".getBytes()
            );

            // Wird durch Dateierweiterungs-Validierung abgelehnt (.exe nicht erlaubt)
            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("Ungültiger Dateityp");
        }

        @Test
        @DisplayName("Null-Dateiname wird abgelehnt")
        void nullFileName_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", null, "text/plain", "content".getBytes()
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class);
        }
    }

    @Nested
    @DisplayName("MIME-Type Validierung")
    class MimeTypeValidation {

        @Test
        @DisplayName("Korrekter MIME-Type wird akzeptiert")
        void validMimeType_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Fehlender MIME-Type wird toleriert")
        void missingMimeType_IsTolerated() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "code.java", null, "class Test {}".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("application/octet-stream für Textdateien wird akzeptiert")
        void octetStreamForText_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "script.py", "application/octet-stream", "print('hello')".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Verdächtige Inhalte")
    class SuspiciousContentValidation {

        @Test
        @DisplayName("Script-Tag in PDF-Datei wird erkannt")
        void scriptInPdf_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf",
                "<script>alert('xss')</script>".getBytes()
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("schädlichen Code");
        }

        @Test
        @DisplayName("JavaScript-URL in Dokument wird erkannt")
        void javascriptUrl_IsRejected() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "javascript:alert(1)".getBytes()
            );

            assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("schädlichen Code");
        }

        @Test
        @DisplayName("Normales HTML-Dokument wird akzeptiert (kein Script-Tag)")
        void normalHtml_IsAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "page.html", "text/html",
                "<html><body><h1>Hello</h1></body></html>".getBytes()
            );

            assertThatCode(() -> validator.validate(file))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Valide Dateien")
    class ValidFiles {

        @Test
        @DisplayName("Alle unterstützten Dokumenttypen werden akzeptiert")
        void allDocumentTypes_AreAccepted() {
            String[] extensions = {"pdf", "doc", "docx", "odt", "rtf", "txt", "md"};

            for (String ext : extensions) {
                MockMultipartFile file = new MockMultipartFile(
                    "file", "document." + ext, "application/octet-stream", "content".getBytes()
                );

                assertThatCode(() -> validator.validate(file))
                    .as("Extension ." + ext + " sollte akzeptiert werden")
                    .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Alle unterstützten Code-Typen werden akzeptiert")
        void allCodeTypes_AreAccepted() {
            String[] extensions = {"java", "py", "js", "ts", "go", "rs", "c", "cpp", "sql"};

            for (String ext : extensions) {
                MockMultipartFile file = new MockMultipartFile(
                    "file", "code." + ext, "text/plain", "code content".getBytes()
                );

                assertThatCode(() -> validator.validate(file))
                    .as("Extension ." + ext + " sollte akzeptiert werden")
                    .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Alle unterstützten Bildtypen werden akzeptiert")
        void allImageTypes_AreAccepted() {
            // Map von Extension zu korrektem MIME-Type
            String[][] imageTypes = {
                {"jpg", "image/jpeg"},
                {"jpeg", "image/jpeg"},
                {"png", "image/png"},
                {"gif", "image/gif"},
                {"webp", "image/webp"},
                {"bmp", "image/bmp"}
            };

            for (String[] typeInfo : imageTypes) {
                String ext = typeInfo[0];
                String mimeType = typeInfo[1];
                MockMultipartFile file = new MockMultipartFile(
                    "file", "image." + ext, mimeType, new byte[]{1, 2, 3}
                );

                assertThatCode(() -> validator.validate(file))
                    .as("Extension ." + ext + " sollte akzeptiert werden")
                    .doesNotThrowAnyException();
            }
        }
    }
}
