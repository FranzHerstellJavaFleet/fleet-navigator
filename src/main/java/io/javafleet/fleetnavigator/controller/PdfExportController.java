package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for exporting log analysis reports as PDF
 * Note: Disabled by default in Native Image builds due to Flexmark compatibility issues
 */
@RestController
@RequestMapping("/api/fleet-mate")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fleet.pdf.enabled", havingValue = "true", matchIfMissing = false)
public class PdfExportController {

    private final PdfExportService pdfExportService;

    /**
     * Generate and download PDF report from analysis content
     *
     * POST /api/fleet-mate/export-pdf
     * Body: {
     *   "content": "markdown content",
     *   "mateId": "ubuntu-desktop-01",
     *   "logPath": "/var/log/syslog",
     *   "sessionId": "ubuntu-desktop-01-1763130248642"
     * }
     */
    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            String mateId = request.getOrDefault("mateId", "unknown");
            String logPath = request.getOrDefault("logPath", "unknown");
            String sessionId = request.getOrDefault("sessionId", "unknown");

            if (content == null || content.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            log.info("Generating PDF export for session: {}", sessionId);

            // Generate PDF
            byte[] pdfBytes = pdfExportService.generatePdfReport(content, sessionId, mateId, logPath);

            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("log-analysis_%s_%s.pdf", mateId, timestamp);

            // Return PDF as download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("PDF exported successfully: {} ({} bytes)", filename, pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Failed to export PDF", e);
            return ResponseEntity.status(500).build();
        }
    }
}
