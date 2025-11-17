package io.javafleet.fleetnavigator.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for exporting log analysis reports as PDF
 * Note: Disabled by default in Native Image builds due to Flexmark compatibility issues
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "fleet.pdf.enabled", havingValue = "true", matchIfMissing = false)
public class PdfExportService {

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public PdfExportService() {
        // Configure Flexmark for GitHub-flavored markdown
        MutableDataSet options = new MutableDataSet();
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Convert markdown analysis report to PDF
     *
     * @param markdownContent The markdown content from log analysis
     * @param sessionId       Session ID for the report
     * @param mateId          Fleet Mate ID
     * @param logPath         Path to the analyzed log file
     * @return PDF as byte array
     */
    public byte[] generatePdfReport(String markdownContent, String sessionId, String mateId, String logPath) {
        try {
            log.info("Generating PDF report for session: {}", sessionId);

            // Convert markdown to HTML
            String html = convertMarkdownToHtml(markdownContent, sessionId, mateId, logPath);

            // Convert HTML to PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html, outputStream);

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("PDF report generated successfully: {} bytes", pdfBytes.length);

            return pdfBytes;

        } catch (Exception e) {
            log.error("Failed to generate PDF report for session: {}", sessionId, e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert markdown content to styled HTML
     */
    private String convertMarkdownToHtml(String markdownContent, String sessionId, String mateId, String logPath) {
        // Parse markdown to HTML
        String bodyContent = htmlRenderer.render(markdownParser.parse(markdownContent));

        // Current timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

        // Build complete HTML document with CSS styling
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "  <meta charset=\"UTF-8\">" +
            "  <title>Fleet Navigator - Log-Analyse Bericht</title>" +
            "  <style>" +
            "    body {" +
            "      font-family: 'DejaVu Sans', Arial, sans-serif;" +
            "      margin: 40px;" +
            "      color: #333;" +
            "      line-height: 1.6;" +
            "    }" +
            "    h1 {" +
            "      color: #FF6B35;" +
            "      border-bottom: 3px solid #FF6B35;" +
            "      padding-bottom: 10px;" +
            "    }" +
            "    h2 {" +
            "      color: #FF8C42;" +
            "      margin-top: 30px;" +
            "      border-left: 4px solid #FF8C42;" +
            "      padding-left: 10px;" +
            "    }" +
            "    h3 {" +
            "      color: #555;" +
            "    }" +
            "    code {" +
            "      background-color: #f4f4f4;" +
            "      padding: 2px 6px;" +
            "      border-radius: 3px;" +
            "      font-family: 'Courier New', monospace;" +
            "      font-size: 0.9em;" +
            "    }" +
            "    pre {" +
            "      background-color: #f8f8f8;" +
            "      border: 1px solid #ddd;" +
            "      border-left: 4px solid #FF6B35;" +
            "      padding: 15px;" +
            "      overflow-x: auto;" +
            "      border-radius: 4px;" +
            "    }" +
            "    pre code {" +
            "      background-color: transparent;" +
            "      padding: 0;" +
            "    }" +
            "    ul, ol {" +
            "      margin-left: 20px;" +
            "    }" +
            "    .header {" +
            "      background-color: #FF6B35;" +
            "      color: white;" +
            "      padding: 20px;" +
            "      border-radius: 8px;" +
            "      margin-bottom: 30px;" +
            "    }" +
            "    .header h1 {" +
            "      color: white;" +
            "      border: none;" +
            "      margin: 0;" +
            "      padding: 0;" +
            "    }" +
            "    .meta-info {" +
            "      background-color: #f9f9f9;" +
            "      border: 1px solid #ddd;" +
            "      padding: 15px;" +
            "      border-radius: 4px;" +
            "      margin-bottom: 20px;" +
            "      font-size: 0.9em;" +
            "    }" +
            "    .meta-info strong {" +
            "      color: #FF6B35;" +
            "    }" +
            "    blockquote {" +
            "      border-left: 4px solid #FF8C42;" +
            "      padding-left: 15px;" +
            "      color: #666;" +
            "      font-style: italic;" +
            "    }" +
            "    table {" +
            "      border-collapse: collapse;" +
            "      width: 100%;" +
            "      margin: 20px 0;" +
            "    }" +
            "    th, td {" +
            "      border: 1px solid #ddd;" +
            "      padding: 10px;" +
            "      text-align: left;" +
            "    }" +
            "    th {" +
            "      background-color: #FF6B35;" +
            "      color: white;" +
            "    }" +
            "    .footer {" +
            "      margin-top: 50px;" +
            "      padding-top: 20px;" +
            "      border-top: 2px solid #ddd;" +
            "      font-size: 0.85em;" +
            "      color: #888;" +
            "      text-align: center;" +
            "    }" +
            "  </style>" +
            "</head>" +
            "<body>" +
            "  <div class=\"header\">" +
            "    <h1>🚢 Fleet Navigator - Log-Analyse Bericht</h1>" +
            "  </div>" +
            "  <div class=\"meta-info\">" +
            "    <p><strong>Erstellt am:</strong> " + timestamp + "</p>" +
            "    <p><strong>Session ID:</strong> " + sessionId + "</p>" +
            "    <p><strong>Fleet Mate:</strong> " + mateId + "</p>" +
            "    <p><strong>Log-Datei:</strong> " + logPath + "</p>" +
            "    <p><strong>Analysiert mit:</strong> GPU-beschleunigter KI (llama.cpp mit CUDA)</p>" +
            "  </div>" +
            "  " + bodyContent +
            "  <div class=\"footer\">" +
            "    <p>Generiert von Fleet Navigator v0.2.7 | © 2025 JavaFleet Systems</p>" +
            "    <p>🤖 KI-Analyse powered by llama.cpp with NVIDIA GPU acceleration</p>" +
            "  </div>" +
            "</body>" +
            "</html>";
    }
}
