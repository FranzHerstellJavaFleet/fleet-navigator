package io.javafleet.fleetnavigator.experts.controller;

import io.javafleet.fleetnavigator.experts.dto.*;
import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import io.javafleet.fleetnavigator.experts.service.ExpertSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST-Controller für das Experten-System
 *
 * Endpoints:
 * - GET    /api/experts              - Alle Experten
 * - GET    /api/experts/{id}         - Experte nach ID
 * - POST   /api/experts              - Experte erstellen
 * - PUT    /api/experts/{id}         - Experte aktualisieren
 * - DELETE /api/experts/{id}         - Experte löschen
 *
 * - GET    /api/experts/{id}/modes   - Modi eines Experten
 * - POST   /api/experts/{id}/modes   - Modus hinzufügen
 * - PUT    /api/experts/modes/{id}   - Modus aktualisieren
 * - DELETE /api/experts/modes/{id}   - Modus löschen
 *
 */
@RestController
@RequestMapping("/api/experts")
@RequiredArgsConstructor
@Slf4j
public class ExpertController {

    private final ExpertSystemService expertService;

    // Erlaubte Bildformate
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // Maximale Bildgröße: 5 MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Speicherort für Experten-Avatare
    private static final String AVATAR_STORAGE_DIR = "data/images/experts";

    // ==================== EXPERT ENDPOINTS ====================

    /**
     * GET /api/experts - Alle Experten abrufen
     */
    @GetMapping
    public ResponseEntity<List<Expert>> getAllExperts() {
        try {
            List<Expert> experts = expertService.getAllExperts();
            return ResponseEntity.ok(experts);
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Experten", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/experts/{id} - Experte nach ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Expert> getExpertById(@PathVariable Long id) {
        try {
            return expertService.getExpertById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Fehler beim Abrufen des Experten: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/experts - Experte erstellen
     */
    @PostMapping
    public ResponseEntity<?> createExpert(@RequestBody CreateExpertRequest request) {
        try {
            Expert expert = expertService.createExpert(request);
            return ResponseEntity.ok(expert);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Fehler beim Erstellen des Experten", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/experts/{id} - Experte aktualisieren
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpert(@PathVariable Long id, @RequestBody CreateExpertRequest request) {
        try {
            Expert expert = expertService.updateExpert(id, request);
            return ResponseEntity.ok(expert);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Fehler beim Aktualisieren des Experten: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/experts/{id} - Experte löschen
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpert(@PathVariable Long id) {
        try {
            expertService.deleteExpert(id);
            return ResponseEntity.ok(Map.of("message", "Experte gelöscht"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Fehler beim Löschen des Experten: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    // ==================== MODE ENDPOINTS ====================

    /**
     * GET /api/experts/{id}/modes - Modi eines Experten
     */
    @GetMapping("/{id}/modes")
    public ResponseEntity<List<ExpertMode>> getModesForExpert(@PathVariable Long id) {
        try {
            List<ExpertMode> modes = expertService.getModesForExpert(id);
            return ResponseEntity.ok(modes);
        } catch (Exception e) {
            log.error("Fehler beim Abrufen der Modi für Experte: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/experts/{id}/modes - Modus hinzufügen
     */
    @PostMapping("/{id}/modes")
    public ResponseEntity<?> addModeToExpert(@PathVariable Long id, @RequestBody CreateExpertModeRequest request) {
        try {
            ExpertMode mode = expertService.addModeToExpert(id, request);
            return ResponseEntity.ok(mode);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Fehler beim Hinzufügen des Modus zu Experte: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/experts/modes/{modeId} - Modus aktualisieren
     */
    @PutMapping("/modes/{modeId}")
    public ResponseEntity<?> updateMode(@PathVariable Long modeId, @RequestBody CreateExpertModeRequest request) {
        try {
            ExpertMode mode = expertService.updateMode(modeId, request);
            return ResponseEntity.ok(mode);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Fehler beim Aktualisieren des Modus: {}", modeId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/experts/modes/{modeId} - Modus löschen
     */
    @DeleteMapping("/modes/{modeId}")
    public ResponseEntity<?> deleteMode(@PathVariable Long modeId) {
        try {
            expertService.deleteMode(modeId);
            return ResponseEntity.ok(Map.of("message", "Modus gelöscht"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Fehler beim Löschen des Modus: {}", modeId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    // ==================== AVATAR UPLOAD ENDPOINT ====================

    /**
     * POST /api/experts/avatar/upload - Avatar-Bild hochladen
     *
     * Lädt ein Bild hoch und gibt die URL zurück.
     * Erlaubte Formate: JPEG, PNG, GIF, WebP
     * Max. Größe: 5 MB
     */
    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // Validierung: Datei leer?
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Keine Datei ausgewählt"));
            }

            // Validierung: Dateigröße
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Datei zu groß (max. 5 MB)"));
            }

            // Validierung: Content-Type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ungültiges Bildformat. Erlaubt: JPEG, PNG, GIF, WebP"));
            }

            // Speicherverzeichnis erstellen
            Path storageDir = Paths.get(AVATAR_STORAGE_DIR);
            Files.createDirectories(storageDir);

            // Eindeutigen Dateinamen generieren
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String newFilename = UUID.randomUUID().toString() + extension;
            Path targetPath = storageDir.resolve(newFilename);

            // Datei speichern
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // URL für das Bild generieren
            String avatarUrl = "/api/experts/avatar/" + newFilename;

            log.info("Avatar hochgeladen: {} -> {}", originalFilename, avatarUrl);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "avatarUrl", avatarUrl,
                    "filename", newFilename
            ));

        } catch (IOException e) {
            log.error("Fehler beim Speichern des Avatars", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Fehler beim Speichern: " + e.getMessage()));
        }
    }

    /**
     * GET /api/experts/avatar/{filename} - Avatar-Bild abrufen
     */
    @GetMapping("/avatar/{filename}")
    public ResponseEntity<?> getAvatar(@PathVariable String filename) {
        try {
            // Sicherheitscheck: Nur Dateinamen ohne Pfad-Komponenten
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ungültiger Dateiname"));
            }

            Path avatarPath = Paths.get(AVATAR_STORAGE_DIR, filename);

            if (!Files.exists(avatarPath)) {
                return ResponseEntity.notFound().build();
            }

            // Content-Type bestimmen
            String contentType = Files.probeContentType(avatarPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            byte[] imageBytes = Files.readAllBytes(avatarPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);

        } catch (IOException e) {
            log.error("Fehler beim Laden des Avatars: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hilfsmethode: Dateiendung extrahieren
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".png"; // Default
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
