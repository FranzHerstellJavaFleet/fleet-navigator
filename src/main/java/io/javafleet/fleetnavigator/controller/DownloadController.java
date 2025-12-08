package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.GeneratedDownload;
import io.javafleet.fleetnavigator.service.CodeGeneratorService;
import io.javafleet.fleetnavigator.service.DocumentGeneratorService;
import io.javafleet.fleetnavigator.service.ZipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST Controller for file downloads
 */
@Slf4j
@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
public class DownloadController {

    private final CodeGeneratorService codeGeneratorService;
    private final ZipService zipService;
    private final DocumentGeneratorService documentGeneratorService;

    // In-memory store for download metadata (in production, use Redis or database)
    private final Map<String, GeneratedDownload> downloadRegistry = new ConcurrentHashMap<>();

    /**
     * Generate project and create download
     *
     * @param request Contains AI response with code/project structure
     * @return Download metadata with URL
     */
    @PostMapping("/generate")
    public ResponseEntity<GeneratedDownload> generateDownload(@RequestBody Map<String, String> request) {
        try {
            String aiResponse = request.get("content");
            String filename = request.getOrDefault("filename", "generated-project");

            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            log.info("Generating download from AI response (length: {})", aiResponse.length());

            // Generate project files
            Path projectPath = codeGeneratorService.generateProject(aiResponse);

            // Create ZIP
            String downloadId = UUID.randomUUID().toString();
            Path zipPath = Paths.get(codeGeneratorService.getTempDirectory(), downloadId + ".zip");
            File zipFile = zipService.createZip(projectPath, zipPath);

            // Register download
            GeneratedDownload download = GeneratedDownload.builder()
                    .downloadId(downloadId)
                    .filename(filename + ".zip")
                    .downloadUrl("/api/downloads/" + downloadId)
                    .sizeBytes(zipFile.length())
                    .sizeHumanReadable(zipService.getHumanReadableSize(zipFile.length()))
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            downloadRegistry.put(downloadId, download);

            log.info("Download ready: {} ({} bytes)", downloadId, zipFile.length());

            return ResponseEntity.ok(download);

        } catch (Exception e) {
            log.error("Failed to generate download", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download the generated ZIP file
     *
     * @param downloadId The download ID
     * @return ZIP file as downloadable resource
     */
    @GetMapping("/{downloadId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String downloadId) {
        try {
            GeneratedDownload download = downloadRegistry.get(downloadId);

            if (download == null) {
                log.warn("Download not found: {}", downloadId);
                return ResponseEntity.notFound().build();
            }

            // Check if expired
            if (download.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Download expired: {}", downloadId);
                downloadRegistry.remove(downloadId);
                return ResponseEntity.status(410).build(); // Gone
            }

            // Get ZIP file
            Path zipPath = Paths.get(codeGeneratorService.getTempDirectory(), downloadId + ".zip");
            File zipFile = zipPath.toFile();

            if (!zipFile.exists()) {
                log.warn("Download file not found: {}", zipPath);
                downloadRegistry.remove(downloadId);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(zipFile);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipFile.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to serve download: {}", downloadId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get download metadata
     */
    @GetMapping("/{downloadId}/info")
    public ResponseEntity<GeneratedDownload> getDownloadInfo(@PathVariable String downloadId) {
        GeneratedDownload download = downloadRegistry.get(downloadId);

        if (download == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(download);
    }

    /**
     * Delete a download (cleanup)
     */
    @DeleteMapping("/{downloadId}")
    public ResponseEntity<Void> deleteDownload(@PathVariable String downloadId) {
        try {
            downloadRegistry.remove(downloadId);

            // Delete ZIP file
            Path zipPath = Paths.get(codeGeneratorService.getTempDirectory(), downloadId + ".zip");
            File zipFile = zipPath.toFile();

            if (zipFile.exists()) {
                zipFile.delete();
                log.info("Deleted download: {}", downloadId);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to delete download: {}", downloadId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download a generated document (DOCX, PDF)
     * @param docId The document ID
     * @return Document file as downloadable resource
     */
    @GetMapping("/doc/{docId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String docId) {
        try {
            log.info("Document download requested: {}", docId);

            byte[] bytes = documentGeneratorService.getDocumentBytes(docId);
            if (bytes == null) {
                log.warn("Document not found: {}", docId);
                return ResponseEntity.notFound().build();
            }

            DocumentGeneratorService.GeneratedDocument doc = documentGeneratorService.getDocument(docId);
            if (doc == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new ByteArrayResource(bytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                    .contentType(MediaType.parseMediaType(doc.contentType()))
                    .contentLength(bytes.length)
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to serve document: {}", docId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
