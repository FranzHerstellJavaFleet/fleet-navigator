package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.exception.FileUploadException;
import io.javafleet.fleetnavigator.service.FileProcessingService;
import io.javafleet.fleetnavigator.service.FileUploadValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for file upload handling.
 * Verwendet FileUploadValidator für umfassende Sicherheitsprüfungen.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileProcessingService fileProcessingService;
    private final FileUploadValidator fileUploadValidator;

    /**
     * Upload and process a file.
     * Die Datei wird vor der Verarbeitung validiert (Größe, Typ, Name, Inhalt).
     *
     * @param file The uploaded file
     * @return Processed file information
     * @throws FileUploadException wenn die Validierung fehlschlägt
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        // Validiere Datei (wirft FileUploadException bei Problemen)
        fileUploadValidator.validate(file);

        try {
            // Process file
            FileProcessingService.ProcessedFile processedFile = fileProcessingService.processFile(file);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filename", processedFile.getFilename());
            response.put("contentType", processedFile.getContentType());
            response.put("size", processedFile.getSize());
            response.put("type", processedFile.getType());

            if (processedFile.getTextContent() != null) {
                response.put("textContent", processedFile.getTextContent());
            }

            if (processedFile.getBase64Content() != null) {
                response.put("base64Content", processedFile.getBase64Content());
            }

            // For scanned PDFs: return page images for Vision analysis
            if (processedFile.getPageImages() != null && !processedFile.getPageImages().isEmpty()) {
                response.put("pageImages", processedFile.getPageImages());
                response.put("pageCount", processedFile.getPageImages().size());
                log.info("Scanned PDF with {} page images for Vision analysis", processedFile.getPageImages().size());
            }

            log.info("Datei erfolgreich hochgeladen: {} ({}, {} bytes)",
                processedFile.getFilename(), processedFile.getType(), processedFile.getSize());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Fehler beim Verarbeiten der Datei: {}", e.getMessage());
            throw FileUploadException.processingFailed(e.getMessage());
        }
    }
}
