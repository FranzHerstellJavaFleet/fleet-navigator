package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.model.PersonalInfo;
import io.javafleet.fleetnavigator.util.FleetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.dom.element.style.StyleParagraphPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Service für die Erstellung professioneller Dokumente (DOCX, PDF)
 * im Stil von Anwalts-/Beraterschreiben
 *
 * Unterstützt:
 * - Absender-Adresse aus PersonalInfo
 * - Empfänger-Adresse (oder Platzhalter)
 * - Professionelles DIN 5008 Briefformat
 */
@Service
@Slf4j
public class DocumentGeneratorService {

    private final PersonalInfoService personalInfoService;

    // Benutzerfreundlicher Dokumenten-Ordner im Home-Verzeichnis
    private static final String DOCS_DIR;
    private static final String DOCS_DIR_DISPLAY;  // Für Anzeige an Benutzer
    // DateTimeFormatters jetzt zentral in FleetUtils definiert

    static {
        // Verwende ~/Dokumente/Fleet-Navigator/ als benutzerfreundlichen Speicherort
        String userHome = System.getProperty("user.home");
        Path docsPath = Path.of(userHome, "Dokumente", "Fleet-Navigator");

        // Fallback wenn Dokumente-Ordner nicht existiert
        if (!Files.exists(Path.of(userHome, "Dokumente"))) {
            docsPath = Path.of(userHome, "Documents", "Fleet-Navigator");
        }
        if (!Files.exists(docsPath.getParent())) {
            docsPath = Path.of(userHome, "Fleet-Navigator-Dokumente");
        }

        DOCS_DIR = docsPath.toString();
        DOCS_DIR_DISPLAY = docsPath.toString().replace(userHome, "~");
    }

    public DocumentGeneratorService(PersonalInfoService personalInfoService) {
        this.personalInfoService = personalInfoService;

        // Erstelle Dokumente-Verzeichnis
        try {
            Files.createDirectories(Path.of(DOCS_DIR));
            log.info("📁 Dokumenten-Verzeichnis: {}", DOCS_DIR);
            log.info("📁 Anzeige-Pfad: {}", DOCS_DIR_DISPLAY);
        } catch (IOException e) {
            log.error("Konnte Dokumenten-Verzeichnis nicht erstellen: {}", e.getMessage());
        }
    }

    /**
     * Holt Absender-Informationen aus PersonalInfo oder gibt Platzhalter zurück
     */
    private String getSenderAddress() {
        Optional<PersonalInfo> infoOpt = personalInfoService.getPersonalInfo();
        if (infoOpt.isPresent()) {
            PersonalInfo info = infoOpt.get();
            StringBuilder sb = new StringBuilder();

            // Name
            String fullName = info.getFullName();
            if (fullName != null && !fullName.isBlank()) {
                sb.append(fullName).append("\n");
            }

            // Adresse
            String address = info.getFullAddress();
            if (address != null && !address.isBlank()) {
                sb.append(address).append("\n");
            }

            // Kontakt
            if (info.getPhone() != null && !info.getPhone().isBlank()) {
                sb.append("Tel: ").append(info.getPhone()).append("\n");
            }
            if (info.getEmail() != null && !info.getEmail().isBlank()) {
                sb.append("E-Mail: ").append(info.getEmail());
            }

            return sb.toString().trim();
        }

        // Platzhalter wenn keine PersonalInfo vorhanden
        return "[Ihr Name]\n[Ihre Straße und Hausnummer]\n[PLZ Ort]\n[Telefon]\n[E-Mail]";
    }

    /**
     * Gibt Empfänger-Adresse oder Platzhalter zurück
     */
    private String getRecipientAddress(String recipient) {
        if (recipient != null && !recipient.isBlank()) {
            return recipient;
        }
        return "[Empfänger-Name]\n[Firma/Organisation]\n[Straße Hausnummer]\n[PLZ Ort]";
    }

    /**
     * Gibt den benutzerfreundlichen Anzeige-Pfad zurück (mit ~)
     */
    public static String getDisplayPath() {
        return DOCS_DIR_DISPLAY;
    }

    /**
     * Gibt den vollständigen Pfad zurück
     */
    public static String getFullPath() {
        return DOCS_DIR;
    }

    /**
     * Erkennt ob eine Nachricht eine Dokumentanfrage enthält
     */
    public DocumentRequest detectDocumentRequest(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase();

        // Brief/Schreiben Anfragen
        boolean wantsBrief = lower.contains("brief") || lower.contains("schreiben") ||
                            lower.contains("anschreiben") || lower.contains("schriftsatz");

        // PDF Anfragen
        boolean wantsPdf = lower.contains("pdf");

        // Zusammenfassung (könnte Brief oder PDF sein) - auch Tippfehler berücksichtigen
        boolean wantsSummary = lower.contains("zusammenfassung") || lower.contains("zusammenfassen") ||
                              lower.contains("zusammenfasung") || lower.contains("zusammenfasen");

        // Download-Intention - flexiblere Erkennung
        boolean wantsDownload = lower.contains("download") || lower.contains("herunterladen") ||
                               lower.contains("erstellen") || lower.contains("generieren") ||
                               lower.contains("als datei") || lower.contains("speichern") ||
                               lower.contains("zur verfügung") || lower.contains("datei") ||
                               (lower.contains("als") && lower.contains("datei"));

        // Formulierungs-Anfragen
        boolean wantsFormulation = lower.contains("formulier") || lower.contains("verfass") ||
                                   lower.contains("schreib mir") || lower.contains("erstell mir");

        // Format-spezifische Anfragen
        boolean wantsDocx = lower.contains("docx") || lower.contains("word") || lower.contains("microsoft");
        boolean wantsOdt = lower.contains("odt") || lower.contains("libreoffice") || lower.contains("openoffice");

        // Brief-Anfrage mit explizitem DOCX/Word → DOCX
        if ((wantsBrief || wantsFormulation) && wantsDocx) {
            return new DocumentRequest(DocumentType.DOCX, "brief");
        }

        // Brief-Anfrage mit explizitem ODT/LibreOffice → ODT
        if ((wantsBrief || wantsFormulation) && wantsOdt) {
            return new DocumentRequest(DocumentType.ODT, "brief");
        }

        // Explizite DOCX/Word-Anfrage ohne Brief (z.B. "als Word Datei", "erstelle docx")
        if (wantsDocx && wantsDownload) {
            return new DocumentRequest(DocumentType.DOCX, "dokument");
        }

        // Explizite ODT-Anfrage ohne Brief (z.B. "als odt", "erstelle odt datei")
        if (wantsOdt && wantsDownload) {
            return new DocumentRequest(DocumentType.ODT, "dokument");
        }

        // Brief-Anfrage ohne Format-Spezifikation → ODT als Default (offenes Format)
        if ((wantsBrief || wantsFormulation) && (wantsDownload || wantsFormulation)) {
            return new DocumentRequest(DocumentType.ODT, "brief");
        }

        // Explizite PDF-Anfrage
        if (wantsPdf && (wantsDownload || wantsSummary)) {
            return new DocumentRequest(DocumentType.PDF, "zusammenfassung");
        }

        // Zusammenfassung ohne PDF-Spezifikation → ODT als Standard (offenes Format)
        if (wantsSummary && wantsDownload) {
            return new DocumentRequest(DocumentType.ODT, "zusammenfassung");
        }

        // Allgemeine Datei-Anfrage ohne Format → ODT als Standard
        if (wantsDownload) {
            return new DocumentRequest(DocumentType.ODT, "dokument");
        }

        return null;
    }

    /**
     * Generiert ein professionelles Schreiben im gewünschten Format
     */
    public GeneratedDocument generateLetter(Expert expert, String letterContent, String recipient, String subject, DocumentType format) throws IOException {
        if (format == DocumentType.DOCX) {
            return generateDocxLetter(expert, letterContent, recipient, subject);
        } else {
            return generateOdtLetter(expert, letterContent, recipient, subject);
        }
    }

    /**
     * Generiert ein professionelles ODT-Schreiben im DIN 5008 Briefformat (LibreOffice)
     * Enthält Absender-Adresse, Empfänger-Adresse, Datum, Betreff und Briefinhalt
     *
     * Schriftart: Liberation Sans (moderne Sans-Serif, kompatibel mit Arial)
     */
    public GeneratedDocument generateOdtLetter(Expert expert, String letterContent, String recipient, String subject) throws IOException {
        log.info("Generiere ODT-Brief für Experte: {}", expert.getName());

        try {
            OdfTextDocument document = OdfTextDocument.newTextDocument();

            // ===== STILE DEFINIEREN =====
            // Liberation Sans ist auf allen Systemen verfügbar und entspricht Arial
            OdfOfficeStyles styles = document.getOrCreateDocumentStyles();

            // Absender-Stil (klein, grau)
            OdfStyle senderStyle = styles.newStyle("SenderStyle", OdfStyleFamily.Paragraph);
            StyleTextPropertiesElement senderTextProps = senderStyle.newStyleTextPropertiesElement(null);
            senderTextProps.setFoFontFamilyAttribute("Liberation Sans");
            senderTextProps.setFoFontSizeAttribute("9pt");
            senderTextProps.setFoColorAttribute("#666666");

            // Normal-Stil (Standard-Text)
            OdfStyle normalStyle = styles.newStyle("NormalStyle", OdfStyleFamily.Paragraph);
            StyleTextPropertiesElement normalTextProps = normalStyle.newStyleTextPropertiesElement(null);
            normalTextProps.setFoFontFamilyAttribute("Liberation Sans");
            normalTextProps.setFoFontSizeAttribute("11pt");
            normalTextProps.setFoColorAttribute("#000000");

            // Betreff-Stil (fett)
            OdfStyle subjectStyle = styles.newStyle("SubjectStyle", OdfStyleFamily.Paragraph);
            StyleTextPropertiesElement subjectTextProps = subjectStyle.newStyleTextPropertiesElement(null);
            subjectTextProps.setFoFontFamilyAttribute("Liberation Sans");
            subjectTextProps.setFoFontSizeAttribute("11pt");
            subjectTextProps.setFoFontWeightAttribute("bold");
            subjectTextProps.setFoColorAttribute("#000000");

            // Datum-Stil (rechtsbündig)
            OdfStyle dateStyle = styles.newStyle("DateStyle", OdfStyleFamily.Paragraph);
            StyleTextPropertiesElement dateTextProps = dateStyle.newStyleTextPropertiesElement(null);
            dateTextProps.setFoFontFamilyAttribute("Liberation Sans");
            dateTextProps.setFoFontSizeAttribute("11pt");
            StyleParagraphPropertiesElement dateParaProps = dateStyle.newStyleParagraphPropertiesElement();
            dateParaProps.setFoTextAlignAttribute("end");

            // Inhalt-Stil (Blocksatz)
            OdfStyle contentStyle = styles.newStyle("ContentStyle", OdfStyleFamily.Paragraph);
            StyleTextPropertiesElement contentTextProps = contentStyle.newStyleTextPropertiesElement(null);
            contentTextProps.setFoFontFamilyAttribute("Liberation Sans");
            contentTextProps.setFoFontSizeAttribute("11pt");
            StyleParagraphPropertiesElement contentParaProps = contentStyle.newStyleParagraphPropertiesElement();
            contentParaProps.setFoTextAlignAttribute("justify");
            contentParaProps.setFoMarginBottomAttribute("0.3cm");

            // ===== ABSENDER (Rücksendeangabe nach DIN 5008) =====
            String senderAddress = getSenderAddress();
            String senderOneLine = senderAddress.replace("\n", " · ");
            OdfTextParagraph senderPara = document.newParagraph();
            senderPara.setStyleName("SenderStyle");
            senderPara.addContent(senderOneLine);

            // Leerzeilen nach Absender
            document.newParagraph();

            // ===== EMPFÄNGER (Anschriftfeld nach DIN 5008) =====
            String recipientAddress = getRecipientAddress(recipient);
            for (String line : recipientAddress.split("\n")) {
                OdfTextParagraph recipientPara = document.newParagraph();
                recipientPara.setStyleName("NormalStyle");
                recipientPara.addContent(line);
            }

            // Leerzeilen nach Empfänger
            document.newParagraph();
            document.newParagraph();
            document.newParagraph();

            // ===== DATUM (rechtsbündig nach DIN 5008) =====
            Optional<PersonalInfo> infoOpt = personalInfoService.getPersonalInfo();
            String city = infoOpt.map(PersonalInfo::getCity).orElse("");
            String dateLocation = (!city.isBlank() ? city + ", " : "") + LocalDate.now().format(FleetUtils.DATE_GERMAN_LONG);

            OdfTextParagraph datePara = document.newParagraph();
            datePara.setStyleName("DateStyle");
            datePara.addContent(dateLocation);

            // Leerzeilen nach Datum
            document.newParagraph();
            document.newParagraph();

            // ===== BETREFF (fett, ohne "Betreff:" nach DIN 5008) =====
            if (subject != null && !subject.isBlank()) {
                OdfTextParagraph subjectPara = document.newParagraph();
                subjectPara.setStyleName("SubjectStyle");
                subjectPara.addContent(subject);
                document.newParagraph();
            }

            // ===== ANREDE =====
            OdfTextParagraph greetingPara = document.newParagraph();
            greetingPara.setStyleName("NormalStyle");
            greetingPara.addContent("Sehr geehrte Damen und Herren,");
            document.newParagraph();

            // ===== INHALT (Absätze mit Blocksatz) =====
            addOdtContentWithStyle(document, letterContent, "ContentStyle");

            // ===== GRUßFORMEL =====
            document.newParagraph();
            OdfTextParagraph closingPara = document.newParagraph();
            closingPara.setStyleName("NormalStyle");
            closingPara.addContent("Mit freundlichen Grüßen");

            // Leerzeilen für Unterschrift (3 Zeilen nach DIN 5008)
            document.newParagraph();
            document.newParagraph();
            document.newParagraph();

            // ===== UNTERSCHRIFT =====
            String signerName = infoOpt.map(PersonalInfo::getFullName)
                    .filter(n -> !n.isBlank())
                    .orElse(expert.getName());

            OdfTextParagraph signaturePara = document.newParagraph();
            signaturePara.setStyleName("NormalStyle");
            signaturePara.addContent(signerName);

            // In Datei speichern
            String fileId = UUID.randomUUID().toString();
            String topic = FleetUtils.sanitizeFilename(subject != null && !subject.isBlank() ? subject : "Brief");
            String timestamp = LocalDateTime.now().format(FleetUtils.FILENAME_TIMESTAMP);
            String filename = topic + "_" + timestamp + ".odt";
            Path filePath = Path.of(DOCS_DIR, fileId + ".odt");

            document.save(filePath.toFile());
            document.close();

            log.info("ODT-Brief erstellt: {} ({} bytes)", filename, Files.size(filePath));

            return new GeneratedDocument(fileId, filename, "application/vnd.oasis.opendocument.text", filePath);

        } catch (Exception e) {
            log.error("Fehler beim Erstellen des ODT-Briefs", e);
            throw new IOException("ODT-Generierung fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /**
     * Fügt formatierten Inhalt zum ODT-Dokument hinzu (mit Stil)
     */
    private void addOdtContentWithStyle(OdfTextDocument document, String content, String styleName) throws Exception {
        if (content == null) return;

        String[] paragraphs = content.split("\n\n");
        for (String para : paragraphs) {
            if (para.isBlank()) continue;

            // Zeilenumbrüche innerhalb des Absatzes
            String[] lines = para.split("\n");
            for (String line : lines) {
                OdfTextParagraph p = document.newParagraph();
                p.setStyleName(styleName);
                p.addContent(line.trim());
            }

            // Leerzeile nach Absatz
            document.newParagraph();
        }
    }

    /**
     * Generiert ein professionelles DOCX-Schreiben im DIN 5008 Briefformat (Microsoft Word)
     * Enthält Absender-Adresse, Empfänger-Adresse, Datum, Betreff und Briefinhalt
     */
    public GeneratedDocument generateDocxLetter(Expert expert, String letterContent, String recipient, String subject) throws IOException {
        log.info("Generiere DOCX-Brief für Experte: {}", expert.getName());

        try (XWPFDocument document = new XWPFDocument()) {
            // Seitenränder setzen (2.5cm)
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            CTPageMar pageMar = sectPr.addNewPgMar();
            pageMar.setLeft(BigInteger.valueOf(1440));
            pageMar.setRight(BigInteger.valueOf(1440));
            pageMar.setTop(BigInteger.valueOf(1440));
            pageMar.setBottom(BigInteger.valueOf(1440));

            // ===== ABSENDER (klein, oben) =====
            String senderAddress = getSenderAddress();
            XWPFParagraph senderPara = document.createParagraph();
            XWPFRun senderRun = senderPara.createRun();
            senderRun.setFontFamily("Times New Roman");
            senderRun.setFontSize(9);
            senderRun.setColor("666666");
            for (String line : senderAddress.split("\n")) {
                senderRun.setText(line);
                senderRun.addBreak();
            }

            // Trennlinie
            XWPFParagraph linePara = document.createParagraph();
            linePara.setBorderBottom(Borders.SINGLE);

            // Leerzeile
            document.createParagraph();

            // ===== EMPFÄNGER =====
            String recipientAddress = getRecipientAddress(recipient);
            XWPFParagraph recipientPara = document.createParagraph();
            XWPFRun recipientRun = recipientPara.createRun();
            recipientRun.setFontFamily("Times New Roman");
            recipientRun.setFontSize(11);
            for (String line : recipientAddress.split("\n")) {
                recipientRun.setText(line);
                recipientRun.addBreak();
            }

            // Leerzeilen
            document.createParagraph();
            document.createParagraph();

            // ===== DATUM (rechtsbündig) =====
            Optional<PersonalInfo> infoOpt = personalInfoService.getPersonalInfo();
            String city = infoOpt.map(PersonalInfo::getCity).orElse("");
            String dateLocation = (!city.isBlank() ? city + ", " : "") + LocalDate.now().format(FleetUtils.DATE_GERMAN_LONG);

            XWPFParagraph datePara = document.createParagraph();
            datePara.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun dateRun = datePara.createRun();
            dateRun.setFontFamily("Times New Roman");
            dateRun.setFontSize(11);
            dateRun.setText(dateLocation);

            // Leerzeilen
            document.createParagraph();
            document.createParagraph();

            // ===== BETREFF (fett) =====
            if (subject != null && !subject.isBlank()) {
                XWPFParagraph subjectPara = document.createParagraph();
                XWPFRun subjectRun = subjectPara.createRun();
                subjectRun.setFontFamily("Times New Roman");
                subjectRun.setFontSize(11);
                subjectRun.setBold(true);
                subjectRun.setText("Betreff: " + subject);
                document.createParagraph();
            }

            // ===== ANREDE =====
            XWPFParagraph greetingPara = document.createParagraph();
            XWPFRun greetingRun = greetingPara.createRun();
            greetingRun.setFontFamily("Times New Roman");
            greetingRun.setFontSize(11);
            greetingRun.setText("Sehr geehrte Damen und Herren,");
            document.createParagraph();

            // ===== INHALT =====
            addDocxContent(document, letterContent);

            // ===== GRUßFORMEL =====
            document.createParagraph();
            XWPFParagraph closingPara = document.createParagraph();
            XWPFRun closingRun = closingPara.createRun();
            closingRun.setFontFamily("Times New Roman");
            closingRun.setFontSize(11);
            closingRun.setText("Mit freundlichen Grüßen");

            // Leerzeilen für Unterschrift
            document.createParagraph();
            document.createParagraph();

            // ===== UNTERSCHRIFT =====
            String signerName = infoOpt.map(PersonalInfo::getFullName)
                    .filter(n -> !n.isBlank())
                    .orElse(expert.getName());

            XWPFParagraph signaturePara = document.createParagraph();
            XWPFRun signatureRun = signaturePara.createRun();
            signatureRun.setFontFamily("Times New Roman");
            signatureRun.setFontSize(11);
            signatureRun.setText(signerName);

            // In Datei speichern - mit Thema und Zeitstempel
            String fileId = UUID.randomUUID().toString();
            String topic = FleetUtils.sanitizeFilename(subject != null && !subject.isBlank() ? subject : "Brief");
            String timestamp = LocalDateTime.now().format(FleetUtils.FILENAME_TIMESTAMP);
            String filename = topic + "_" + timestamp + ".docx";
            Path filePath = Path.of(DOCS_DIR, fileId + ".docx");

            try (var outputStream = Files.newOutputStream(filePath)) {
                document.write(outputStream);
            }

            log.info("DOCX-Brief erstellt: {} ({} bytes)", filename, Files.size(filePath));

            return new GeneratedDocument(fileId, filename,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", filePath);
        }
    }

    /**
     * Fügt formatierten Inhalt zum DOCX-Dokument hinzu
     */
    private void addDocxContent(XWPFDocument document, String content) {
        if (content == null) return;

        String[] paragraphs = content.split("\n\n");
        for (String para : paragraphs) {
            if (para.isBlank()) continue;

            XWPFParagraph p = document.createParagraph();
            p.setAlignment(ParagraphAlignment.BOTH); // Blocksatz
            p.setSpacingAfter(200);

            String[] lines = para.split("\n");
            XWPFRun run = p.createRun();
            run.setFontFamily("Times New Roman");
            run.setFontSize(11);

            for (int i = 0; i < lines.length; i++) {
                run.setText(lines[i].trim());
                if (i < lines.length - 1) {
                    run.addBreak();
                }
            }
        }
    }

    /**
     * Generiert eine PDF-Zusammenfassung
     */
    public GeneratedDocument generatePdfSummary(Expert expert, String summaryContent, String title) throws IOException {
        log.info("Generiere PDF-Zusammenfassung für Experte: {}", expert.getName());

        // HTML für PDF
        String html = buildPdfHtml(expert, summaryContent, title);

        // PDF generieren mit iText
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        com.itextpdf.html2pdf.HtmlConverter.convertToPdf(html, outputStream);

        // In Datei speichern - mit Titel/Thema und Zeitstempel
        String fileId = UUID.randomUUID().toString();
        String topic = FleetUtils.sanitizeFilename(title != null && !title.isBlank() ? title : "Zusammenfassung");
        String timestamp = LocalDateTime.now().format(FleetUtils.FILENAME_TIMESTAMP);
        String filename = topic + "_" + timestamp + ".pdf";
        Path filePath = Path.of(DOCS_DIR, fileId + ".pdf");

        Files.write(filePath, outputStream.toByteArray());

        log.info("PDF erstellt: {} ({} bytes)", filename, Files.size(filePath));

        return new GeneratedDocument(fileId, filename, "application/pdf", filePath);
    }

    /**
     * Baut HTML für PDF-Generierung
     */
    private String buildPdfHtml(Expert expert, String content, String title) {
        String date = LocalDate.now().format(FleetUtils.DATE_GERMAN_LONG);

        // Escape HTML und formatiere
        String formattedContent = FleetUtils.escapeHtml(content)
                .replace("\n\n", "</p><p>")
                .replace("\n", "<br>");

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    @page { margin: 2.5cm; size: A4; }
                    body {
                        font-family: 'Times New Roman', Times, serif;
                        font-size: 11pt;
                        line-height: 1.5;
                        color: #333;
                    }
                    .header {
                        border-bottom: 2px solid #333;
                        padding-bottom: 15px;
                        margin-bottom: 25px;
                    }
                    .expert-name {
                        font-size: 16pt;
                        font-weight: bold;
                        margin: 0;
                    }
                    .expert-role {
                        font-size: 11pt;
                        color: #666;
                        margin: 5px 0 0 0;
                    }
                    .date {
                        text-align: right;
                        margin-bottom: 20px;
                    }
                    .title {
                        font-size: 14pt;
                        font-weight: bold;
                        text-align: center;
                        margin: 30px 0;
                        text-decoration: underline;
                    }
                    .content {
                        text-align: justify;
                    }
                    .content p {
                        margin-bottom: 12px;
                    }
                    .footer {
                        margin-top: 40px;
                        padding-top: 15px;
                        border-top: 1px solid #ccc;
                        font-size: 9pt;
                        color: #888;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <p class="expert-name">%s</p>
                    <p class="expert-role">%s</p>
                </div>
                <div class="date">%s</div>
                <div class="title">%s</div>
                <div class="content">
                    <p>%s</p>
                </div>
                <div class="footer">
                    Erstellt von Fleet Navigator | %s, %s
                </div>
            </body>
            </html>
            """,
            FleetUtils.escapeHtml(expert.getName()),
            FleetUtils.escapeHtml(expert.getRole()),
            date,
            FleetUtils.escapeHtml(title != null ? title : "Zusammenfassung"),
            formattedContent,
            FleetUtils.escapeHtml(expert.getName()),
            FleetUtils.escapeHtml(expert.getRole())
        );
    }

    /**
     * Holt ein generiertes Dokument für Download
     */
    public GeneratedDocument getDocument(String fileId) {
        // Suche nach Datei mit dieser ID
        try {
            // ODT (LibreOffice)
            Path odtPath = Path.of(DOCS_DIR, fileId + ".odt");
            if (Files.exists(odtPath)) {
                String filename = "Brief_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".odt";
                return new GeneratedDocument(fileId, filename,
                    "application/vnd.oasis.opendocument.text", odtPath);
            }

            // PDF
            Path pdfPath = Path.of(DOCS_DIR, fileId + ".pdf");
            if (Files.exists(pdfPath)) {
                String filename = "Zusammenfassung_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".pdf";
                return new GeneratedDocument(fileId, filename, "application/pdf", pdfPath);
            }

            // Legacy DOCX support
            Path docxPath = Path.of(DOCS_DIR, fileId + ".docx");
            if (Files.exists(docxPath)) {
                String filename = "Brief_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".docx";
                return new GeneratedDocument(fileId, filename,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxPath);
            }
        } catch (Exception e) {
            log.error("Fehler beim Laden des Dokuments: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Liest Dokumentinhalt als Bytes
     */
    public byte[] getDocumentBytes(String fileId) throws IOException {
        GeneratedDocument doc = getDocument(fileId);
        if (doc != null && doc.path() != null) {
            return Files.readAllBytes(doc.path());
        }
        return null;
    }

    // escapeHtml und sanitizeFilename jetzt zentral in FleetUtils

    // DTOs
    public record DocumentRequest(DocumentType type, String purpose) {}
    public record GeneratedDocument(String id, String filename, String contentType, Path path) {}
    public enum DocumentType { DOCX, PDF, ODT }
}
