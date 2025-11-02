package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.FileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for file upload handling
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileProcessingService fileProcessingService;

    /**
     * Upload and process a file
     *
     * @param file The uploaded file
     * @return Processed file information
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File is empty"));
            }

            // Check file size (max 50MB)
            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File size exceeds maximum of 50MB"));
            }

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

            log.info("File uploaded successfully: {} ({})", processedFile.getFilename(), processedFile.getType());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (IOException e) {
            log.error("Failed to process file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to process file: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
