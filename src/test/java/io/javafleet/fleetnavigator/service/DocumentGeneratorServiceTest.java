package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.model.PersonalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests für DocumentGeneratorService.
 *
 * WICHTIG: Diese Tests sichern den funktionierenden Zustand ab!
 * Stand: 2025-11-30
 *
 * Getestete Funktionen:
 * - ODT-Generierung mit Liberation Sans Schriftart
 * - DIN 5008 Briefformat
 * - Download-Button Funktionalität
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentGeneratorService Tests - Stand 2025-11-30")
class DocumentGeneratorServiceTest {

    @Mock
    private PersonalInfoService personalInfoService;

    private DocumentGeneratorService documentGeneratorService;

    @BeforeEach
    void setUp() {
        documentGeneratorService = new DocumentGeneratorService(personalInfoService);
    }

    @Nested
    @DisplayName("Dokument-Anfrage-Erkennung")
    class DocumentRequestDetection {

        @Test
        @DisplayName("Brief-Anfrage wird als ODT erkannt")
        void briefRequestShouldBeDetectedAsOdt() {
            var request = documentGeneratorService.detectDocumentRequest("Erstelle einen Brief als Download");

            assertNotNull(request, "Brief-Anfrage sollte erkannt werden");
            assertEquals(DocumentGeneratorService.DocumentType.ODT, request.type(),
                "Brief sollte als ODT generiert werden (Download-Button!)");
        }

        @Test
        @DisplayName("Schreiben-Anfrage wird als ODT erkannt")
        void schreibenRequestShouldBeDetectedAsOdt() {
            var request = documentGeneratorService.detectDocumentRequest("Formuliere ein Schreiben zum Download");

            assertNotNull(request, "Schreiben-Anfrage sollte erkannt werden");
            assertEquals(DocumentGeneratorService.DocumentType.ODT, request.type());
        }

        @Test
        @DisplayName("Explizite DOCX-Anfrage wird als DOCX erkannt")
        void docxRequestShouldBeDetectedAsDocx() {
            var request = documentGeneratorService.detectDocumentRequest("Erstelle einen Brief als Word Dokument");

            assertNotNull(request, "DOCX-Anfrage sollte erkannt werden");
            assertEquals(DocumentGeneratorService.DocumentType.DOCX, request.type());
        }

        @Test
        @DisplayName("PDF-Anfrage wird als PDF erkannt")
        void pdfRequestShouldBeDetectedAsPdf() {
            var request = documentGeneratorService.detectDocumentRequest("Erstelle eine PDF Zusammenfassung zum Download");

            assertNotNull(request, "PDF-Anfrage sollte erkannt werden");
            assertEquals(DocumentGeneratorService.DocumentType.PDF, request.type());
        }

        @Test
        @DisplayName("Normale Chat-Nachricht erzeugt kein Dokument")
        void normalMessageShouldNotGenerateDocument() {
            var request = documentGeneratorService.detectDocumentRequest("Was ist die Hauptstadt von Deutschland?");

            assertNull(request, "Normale Nachrichten sollten keine Dokumente erzeugen");
        }
    }

    @Nested
    @DisplayName("ODT-Brief-Generierung")
    class OdtLetterGeneration {

        @Test
        @DisplayName("ODT-Brief wird erfolgreich generiert")
        void odtLetterShouldBeGenerated() throws IOException {
            // Mock PersonalInfo
            PersonalInfo personalInfo = new PersonalInfo();
            personalInfo.setFirstName("Max");
            personalInfo.setLastName("Mustermann");
            personalInfo.setStreet("Musterstraße 123");
            personalInfo.setPostalCode("12345");
            personalInfo.setCity("Musterstadt");
            when(personalInfoService.getPersonalInfo()).thenReturn(Optional.of(personalInfo));

            // Create test expert
            Expert expert = new Expert();
            expert.setName("Roland Navarro");
            expert.setRole("Rechtsanwalt");

            // Generate letter
            var document = documentGeneratorService.generateOdtLetter(
                expert,
                "Dies ist der Briefinhalt.\n\nMit mehreren Absätzen.",
                "Empfänger GmbH\nMusterweg 456\n67890 Beispielstadt",
                "Betreff des Briefes"
            );

            assertNotNull(document, "Dokument sollte generiert werden");
            assertNotNull(document.id(), "Dokument-ID sollte vorhanden sein");
            assertTrue(document.filename().endsWith(".odt"), "Dateiname sollte .odt enden");
            assertEquals("application/vnd.oasis.opendocument.text", document.contentType());
            assertTrue(Files.exists(document.path()), "Datei sollte existieren");
            assertTrue(Files.size(document.path()) > 0, "Datei sollte nicht leer sein");

            // Cleanup
            Files.deleteIfExists(document.path());
        }

        @Test
        @DisplayName("ODT-Brief enthält Liberation Sans Schriftart")
        void odtLetterShouldContainLiberationSansFont() throws IOException {
            when(personalInfoService.getPersonalInfo()).thenReturn(Optional.empty());

            Expert expert = new Expert();
            expert.setName("Test Experte");
            expert.setRole("Tester");

            var document = documentGeneratorService.generateOdtLetter(
                expert,
                "Testinhalt",
                null,
                "Testbetreff"
            );

            // ODT is a ZIP file - read content.xml and check for Liberation Sans
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(document.path()))) {
                ZipEntry entry;
                boolean foundLiberationSans = false;

                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("content.xml") || entry.getName().equals("styles.xml")) {
                        byte[] content = zis.readAllBytes();
                        String xml = new String(content);
                        if (xml.contains("Liberation Sans")) {
                            foundLiberationSans = true;
                            break;
                        }
                    }
                }

                assertTrue(foundLiberationSans,
                    "ODT sollte Liberation Sans Schriftart verwenden (moderne Sans-Serif, nicht Monospace!)");
            }

            // Cleanup
            Files.deleteIfExists(document.path());
        }

        @Test
        @DisplayName("ODT-Brief funktioniert ohne PersonalInfo")
        void odtLetterShouldWorkWithoutPersonalInfo() throws IOException {
            when(personalInfoService.getPersonalInfo()).thenReturn(Optional.empty());

            Expert expert = new Expert();
            expert.setName("Test");
            expert.setRole("Test");

            var document = documentGeneratorService.generateOdtLetter(
                expert,
                "Inhalt",
                null,
                "Betreff"
            );

            assertNotNull(document);
            assertTrue(Files.exists(document.path()));

            // Cleanup
            Files.deleteIfExists(document.path());
        }
    }

    @Nested
    @DisplayName("Download-Funktionalität")
    class DownloadFunctionality {

        @Test
        @DisplayName("Dokument kann über ID abgerufen werden")
        void documentShouldBeRetrievableById() throws IOException {
            when(personalInfoService.getPersonalInfo()).thenReturn(Optional.empty());

            Expert expert = new Expert();
            expert.setName("Test");
            expert.setRole("Test");

            // Generate document
            var generated = documentGeneratorService.generateOdtLetter(
                expert, "Test", null, "Test"
            );

            // Retrieve by ID
            var retrieved = documentGeneratorService.getDocument(generated.id());

            assertNotNull(retrieved, "Dokument sollte über ID abrufbar sein");
            assertEquals(generated.id(), retrieved.id());

            // Get bytes for download
            byte[] bytes = documentGeneratorService.getDocumentBytes(generated.id());
            assertNotNull(bytes, "Dokument-Bytes sollten abrufbar sein");
            assertTrue(bytes.length > 0, "Dokument sollte nicht leer sein");

            // Cleanup
            Files.deleteIfExists(generated.path());
        }

        @Test
        @DisplayName("Nicht existierendes Dokument gibt null zurück")
        void nonExistentDocumentShouldReturnNull() {
            var document = documentGeneratorService.getDocument("nicht-vorhanden-12345");
            assertNull(document, "Nicht existierendes Dokument sollte null zurückgeben");
        }
    }

    @Nested
    @DisplayName("DIN 5008 Format")
    class Din5008Format {

        @Test
        @DisplayName("Betreff enthält kein 'Betreff:' Präfix (nach DIN 5008)")
        void subjectShouldNotHavePrefix() throws IOException {
            when(personalInfoService.getPersonalInfo()).thenReturn(Optional.empty());

            Expert expert = new Expert();
            expert.setName("Test");
            expert.setRole("Test");

            var document = documentGeneratorService.generateOdtLetter(
                expert,
                "Inhalt",
                null,
                "Kündigung meines Vertrages"
            );

            // Check content.xml for subject without "Betreff:" prefix
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(document.path()))) {
                ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("content.xml")) {
                        byte[] content = zis.readAllBytes();
                        String xml = new String(content);

                        // Nach DIN 5008: Betreff ohne "Betreff:" Präfix
                        assertTrue(xml.contains("Kündigung meines Vertrages"),
                            "Betreffzeile sollte vorhanden sein");
                        assertFalse(xml.contains("Betreff: Kündigung"),
                            "Betreff sollte KEIN 'Betreff:' Präfix haben (DIN 5008)");
                        break;
                    }
                }
            }

            // Cleanup
            Files.deleteIfExists(document.path());
        }
    }
}
