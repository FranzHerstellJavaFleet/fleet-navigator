package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.AutoUpdateService;
import io.javafleet.fleetnavigator.service.AutoUpdateService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller für Auto-Update Funktionalität.
 *
 * Endpunkte:
 * - GET  /api/update/check    - Prüft auf neue Version
 * - GET  /api/update/status   - Aktueller Update-Status
 * - POST /api/update/download - Lädt Update herunter
 * - POST /api/update/install  - Installiert heruntergeladenes Update
 */
@RestController
@RequestMapping("/api/update")
@RequiredArgsConstructor
@Slf4j
public class UpdateController {

    private final AutoUpdateService autoUpdateService;

    /**
     * GET /api/update/status - Aktueller Update-Status
     */
    @GetMapping("/status")
    public ResponseEntity<UpdateStatusResponse> getStatus() {
        UpdateInfo info = autoUpdateService.getLatestUpdateInfo();

        return ResponseEntity.ok(new UpdateStatusResponse(
                autoUpdateService.getCurrentVersion(),
                autoUpdateService.isUpdateAvailable(),
                autoUpdateService.isDownloadInProgress(),
                autoUpdateService.getDownloadProgress(),
                autoUpdateService.getLastCheckTime(),
                info != null ? info.getVersion() : null,
                info != null ? info.getReleaseName() : null,
                info != null ? info.getReleaseNotes() : null,
                info != null ? info.getReleaseUrl() : null,
                info != null ? info.getAssetSize() : 0
        ));
    }

    /**
     * GET /api/update/check - Prüft auf neue Version
     */
    @GetMapping("/check")
    public ResponseEntity<UpdateCheckResult> checkForUpdates() {
        log.info("Manueller Update-Check angefordert");
        UpdateCheckResult result = autoUpdateService.checkForUpdates();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/update/download - Lädt Update herunter
     */
    @PostMapping("/download")
    public ResponseEntity<DownloadResult> downloadUpdate() {
        if (!autoUpdateService.isUpdateAvailable()) {
            return ResponseEntity.badRequest()
                    .body(new DownloadResult(false, "Kein Update verfügbar", null));
        }

        if (autoUpdateService.isDownloadInProgress()) {
            return ResponseEntity.badRequest()
                    .body(new DownloadResult(false, "Download bereits im Gange", null));
        }

        log.info("Update-Download angefordert");
        DownloadResult result = autoUpdateService.downloadUpdate();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/update/install - Installiert heruntergeladenes Update
     */
    @PostMapping("/install")
    public ResponseEntity<InstallResult> installUpdate() {
        log.info("Update-Installation angefordert");
        InstallResult result = autoUpdateService.installUpdate();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/update/progress - Download-Fortschritt (für Polling)
     */
    @GetMapping("/progress")
    public ResponseEntity<ProgressResponse> getProgress() {
        return ResponseEntity.ok(new ProgressResponse(
                autoUpdateService.isDownloadInProgress(),
                autoUpdateService.getDownloadProgress()
        ));
    }

    // Response DTOs
    public record UpdateStatusResponse(
            String currentVersion,
            boolean updateAvailable,
            boolean downloadInProgress,
            String downloadProgress,
            LocalDateTime lastCheckTime,
            String latestVersion,
            String releaseName,
            String releaseNotes,
            String releaseUrl,
            long downloadSize
    ) {}

    public record ProgressResponse(
            boolean inProgress,
            String progress
    ) {}
}
