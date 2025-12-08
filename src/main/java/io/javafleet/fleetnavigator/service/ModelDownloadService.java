package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.config.FleetPathsConfiguration;
import io.javafleet.fleetnavigator.llm.ModelRegistry;
import io.javafleet.fleetnavigator.llm.ModelRegistryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service für den Download von GGUF-Modellen von HuggingFace
 *
 * WICHTIG: Verwendet FleetPathsConfiguration für plattformspezifische Pfade!
 * - Linux: ~/.java-fleet/models/library/
 * - Windows: %LOCALAPPDATA%\JavaFleet\models\library\
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.9
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelDownloadService {

    private final ModelRegistry modelRegistry;
    private final FleetPathsConfiguration pathsConfig;

    // Track active downloads for progress updates
    private final Map<String, DownloadProgress> activeDownloads = new ConcurrentHashMap<>();
    private final Map<String, Call> activeDownloadCalls = new ConcurrentHashMap<>();

    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // Kein Read-Timeout für große Downloads
        .writeTimeout(120, TimeUnit.SECONDS)
        .build();

    /**
     * Startet Download eines Modells von HuggingFace
     *
     * @param modelId ID aus der ModelRegistry
     * @param progressConsumer Consumer für Progress-Updates
     */
    public void downloadModel(String modelId, Consumer<String> progressConsumer) throws IOException {
        // 1. Modell aus Registry holen
        ModelRegistryEntry model = modelRegistry.findById(modelId)
            .orElseThrow(() -> new IllegalArgumentException("Unbekanntes Modell: " + modelId));

        // 2. Zielpfad erstellen
        Path libraryDir = getLibraryDir();
        Files.createDirectories(libraryDir);

        Path targetPath = libraryDir.resolve(model.getFilename());

        // 3. Prüfe ob bereits vorhanden
        if (Files.exists(targetPath)) {
            long existingSize = Files.size(targetPath);
            if (existingSize == model.getSizeBytes()) {
                progressConsumer.accept("✅ Modell bereits vorhanden!");
                log.info("Model already exists: {}", model.getFilename());
                return;
            } else {
                log.warn("Existing model has wrong size, re-downloading: {}", model.getFilename());
                Files.delete(targetPath);
            }
        }

        // 4. Erstelle Download-URL
        String downloadUrl = buildHuggingFaceDownloadUrl(
            model.getHuggingFaceRepo(),
            model.getFilename()
        );

        log.info("📥 Starting download: {} from {}", model.getDisplayName(), downloadUrl);
        progressConsumer.accept("📥 Starte Download: " + model.getDisplayName());

        // 5. Download mit Progress
        downloadWithProgress(modelId, model, downloadUrl, targetPath, progressConsumer);
    }

    /**
     * Startet Download eines Modells direkt von HuggingFace
     *
     * @param modelId HuggingFace Model ID (e.g., "Qwen/Qwen2.5-3B-Instruct-GGUF")
     * @param filename GGUF filename to download
     * @param progressConsumer Callback für Progress-Updates
     * @throws IOException bei Download-Fehler
     */
    public void downloadHuggingFaceModel(
        String modelId,
        String filename,
        Consumer<String> progressConsumer
    ) throws IOException {
        downloadHuggingFaceModel(modelId, filename, null, progressConsumer);
    }

    /**
     * Download HuggingFace model with optional MMPROJ file for vision models
     *
     * @param modelId HuggingFace Model ID
     * @param filename GGUF filename to download
     * @param mmprojFilename Optional MMPROJ filename for vision models (can be null)
     * @param progressConsumer Callback for progress updates
     * @throws IOException on download error
     */
    public void downloadHuggingFaceModel(
        String modelId,
        String filename,
        String mmprojFilename,
        Consumer<String> progressConsumer
    ) throws IOException {
        // 1. Construct HuggingFace URL
        String downloadUrl = String.format(
            "https://huggingface.co/%s/resolve/main/%s",
            modelId,
            filename
        );

        log.info("📥 Starting HuggingFace download: {} from {}", filename, downloadUrl);
        progressConsumer.accept("📥 Starte HuggingFace Download: " + filename);

        // 2. Ensure library directory exists
        Path libraryDir = getLibraryDir();
        if (!Files.exists(libraryDir)) {
            Files.createDirectories(libraryDir);
        }

        // 3. Target path
        Path targetPath = libraryDir.resolve(filename);

        // 4. Check if file already exists
        if (Files.exists(targetPath)) {
            log.warn("File already exists: {}", targetPath);
            progressConsumer.accept("⚠️ Datei existiert bereits: " + filename);
            throw new IOException("Datei existiert bereits: " + filename);
        }

        // 5. Download main model with progress
        downloadFromUrlWithProgress(modelId, filename, downloadUrl, targetPath, progressConsumer);

        // 6. Download MMPROJ file for vision models
        if (mmprojFilename != null && !mmprojFilename.isEmpty()) {
            String mmprojUrl = String.format(
                "https://huggingface.co/%s/resolve/main/%s",
                modelId,
                mmprojFilename
            );
            Path mmprojPath = libraryDir.resolve(mmprojFilename);

            if (!Files.exists(mmprojPath)) {
                log.info("📥 Downloading MMPROJ file: {}", mmprojFilename);
                progressConsumer.accept("📥 Lade MMPROJ-Datei: " + mmprojFilename);
                try {
                    downloadFromUrlWithProgress(modelId + "-mmproj", mmprojFilename, mmprojUrl, mmprojPath, progressConsumer);
                    progressConsumer.accept("✅ MMPROJ-Datei heruntergeladen!");
                } catch (IOException e) {
                    log.error("Failed to download MMPROJ file: {}", e.getMessage());
                    progressConsumer.accept("⚠️ MMPROJ-Download fehlgeschlagen (optional)");
                    // Don't fail the whole download if MMPROJ fails
                }
            } else {
                progressConsumer.accept("✅ MMPROJ-Datei bereits vorhanden");
            }
        }
    }

    /**
     * Download mit Progress-Tracking
     */
    private void downloadWithProgress(
        String modelId,
        ModelRegistryEntry model,
        String url,
        Path targetPath,
        Consumer<String> progressConsumer
    ) throws IOException {
        // Initialize Progress
        DownloadProgress progress = new DownloadProgress(
            modelId,
            model.getDisplayName(),
            model.getSizeBytes()
        );
        activeDownloads.put(modelId, progress);

        Request request = new Request.Builder()
            .url(url)
            .build();

        Call call = client.newCall(request);
        activeDownloadCalls.put(modelId, call);

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download fehlgeschlagen: HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Leere Response von Server");
            }

            long totalBytes = model.getSizeBytes();
            long downloadedBytes = 0;

            try (InputStream input = body.byteStream();
                 FileOutputStream output = new FileOutputStream(targetPath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long lastProgressUpdate = System.currentTimeMillis();

                while ((bytesRead = input.read(buffer)) != -1) {
                    // Check if cancelled
                    if (call.isCanceled()) {
                        log.info("Download cancelled: {}", modelId);
                        Files.deleteIfExists(targetPath);
                        progressConsumer.accept("❌ Download abgebrochen");
                        return;
                    }

                    output.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    // Update progress every 500ms
                    long now = System.currentTimeMillis();
                    if (now - lastProgressUpdate > 500) {
                        progress.setDownloadedBytes(downloadedBytes);
                        progress.updateProgress();

                        int percent = progress.getPercentComplete();
                        String speedMB = String.format("%.1f", progress.getSpeedMBps());
                        String downloaded = formatBytes(downloadedBytes);
                        String total = model.getSizeHuman();

                        String progressMsg = String.format(
                            "⬇️ %d%% - %s / %s - %s MB/s",
                            percent,
                            downloaded,
                            total,
                            speedMB
                        );

                        progressConsumer.accept(progressMsg);
                        lastProgressUpdate = now;

                        log.debug("Download progress {}: {}%", modelId, percent);
                    }
                }

                // Verify size
                if (downloadedBytes != totalBytes) {
                    double percentComplete = (double) downloadedBytes / totalBytes * 100.0;

                    // Bei > 95% Download behalten wir die Datei (könnte trotzdem funktionieren)
                    if (percentComplete < 95.0) {
                        log.warn("Download zu unvollständig ({} %), lösche Datei", String.format("%.1f", percentComplete));
                        Files.deleteIfExists(targetPath);
                        throw new IOException(String.format(
                            "Download unvollständig: %d von %d Bytes (%.1f%%)",
                            downloadedBytes, totalBytes, percentComplete
                        ));
                    } else {
                        // Fast vollständig - behalten und als abgeschlossen markieren
                        log.warn("⚠️ Download fast vollständig ({} %), Datei wird behalten: {}",
                            String.format("%.1f%%", percentComplete), targetPath);
                        progressConsumer.accept(String.format(
                            "⚠️ Download fast vollständig (%.1f%%) - Datei gespeichert", percentComplete));
                    }
                }

                progress.setStatus(DownloadStatus.COMPLETED);
                progressConsumer.accept("✅ Download abgeschlossen: " + model.getDisplayName());
                log.info("✅ Download completed: {}", model.getDisplayName());

            } catch (IOException e) {
                // Cleanup nur bei echten Fehlern (< 95% heruntergeladen)
                long currentSize = Files.exists(targetPath) ? Files.size(targetPath) : 0;
                if (currentSize < model.getSizeBytes() * 0.95) {
                    log.error("Download fehlgeschlagen bei {} Bytes, lösche Datei", currentSize);
                    Files.deleteIfExists(targetPath);
                } else {
                    log.warn("Download fehlgeschlagen, aber > 95% vorhanden, behalte Datei");
                }
                throw e;
            }

        } finally {
            activeDownloads.remove(modelId);
            activeDownloadCalls.remove(modelId);
        }
    }

    /**
     * Bricht einen aktiven Download ab
     */
    public boolean cancelDownload(String modelId) {
        Call call = activeDownloadCalls.get(modelId);
        if (call != null && !call.isCanceled()) {
            call.cancel();
            activeDownloads.remove(modelId);
            activeDownloadCalls.remove(modelId);
            log.info("Cancelled download: {}", modelId);
            return true;
        }
        return false;
    }

    /**
     * Gibt Progress eines aktiven Downloads zurück
     */
    public DownloadProgress getProgress(String modelId) {
        return activeDownloads.get(modelId);
    }

    /**
     * Alle aktiven Downloads
     */
    public Map<String, DownloadProgress> getActiveDownloads() {
        return new ConcurrentHashMap<>(activeDownloads);
    }

    /**
     * Prüft ob ein Modell bereits heruntergeladen ist
     */
    public boolean isModelDownloaded(String filename) {
        Path libraryPath = getLibraryDir().resolve(filename);
        return Files.exists(libraryPath);
    }

    /**
     * Löscht ein heruntergeladenes Modell
     */
    public boolean deleteModel(String filename) throws IOException {
        Path libraryPath = getLibraryDir().resolve(filename);
        if (Files.exists(libraryPath)) {
            Files.delete(libraryPath);
            log.info("Deleted model: {}", filename);
            return true;
        }
        return false;
    }

    // ===== HELPER METHODS =====

    /**
     * Download from URL with automatic size detection from Content-Length header
     */
    private void downloadFromUrlWithProgress(
        String modelId,
        String displayName,
        String url,
        Path targetPath,
        Consumer<String> progressConsumer
    ) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .build();

        Call call = client.newCall(request);
        activeDownloadCalls.put(modelId, call);

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download fehlgeschlagen: HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Leere Response von Server");
            }

            // Get Content-Length from header (this is the REAL file size!)
            long totalBytes = body.contentLength();
            if (totalBytes <= 0) {
                log.warn("Content-Length not available, progress will be estimated");
                totalBytes = 0; // Unknown size
            }

            log.info("File size from Content-Length: {} bytes ({})", totalBytes, formatBytes(totalBytes));

            // Initialize Progress
            DownloadProgress progress = new DownloadProgress(
                modelId,
                displayName,
                totalBytes > 0 ? totalBytes : null // null if size unknown
            );
            activeDownloads.put(modelId, progress);

            long downloadedBytes = 0;

            try (InputStream input = body.byteStream();
                 FileOutputStream output = new FileOutputStream(targetPath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long lastProgressUpdate = System.currentTimeMillis();

                while ((bytesRead = input.read(buffer)) != -1) {
                    // Check if cancelled
                    if (call.isCanceled()) {
                        log.info("Download cancelled: {}", modelId);
                        Files.deleteIfExists(targetPath);
                        progressConsumer.accept("❌ Download abgebrochen");
                        return;
                    }

                    output.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    // Update progress every 500ms
                    long now = System.currentTimeMillis();
                    if (now - lastProgressUpdate > 500) {
                        progress.setDownloadedBytes(downloadedBytes);
                        progress.updateProgress();

                        String downloaded = formatBytes(downloadedBytes);
                        String progressMsg;

                        if (totalBytes > 0) {
                            // Size known - show percentage
                            int percent = progress.getPercentComplete();
                            String speedMB = String.format("%.1f", progress.getSpeedMBps());
                            String total = formatBytes(totalBytes);

                            progressMsg = String.format(
                                "⬇️ %d%% - %s / %s - %s MB/s",
                                percent,
                                downloaded,
                                total,
                                speedMB
                            );
                        } else {
                            // Size unknown - show only downloaded and speed
                            String speedMB = String.format("%.1f", progress.getSpeedMBps());
                            progressMsg = String.format(
                                "⬇️ %s heruntergeladen - %s MB/s",
                                downloaded,
                                speedMB
                            );
                        }

                        progressConsumer.accept(progressMsg);
                        lastProgressUpdate = now;
                    }
                }

                log.info("Download completed: {} ({} bytes)", displayName, downloadedBytes);
                progressConsumer.accept("✅ Download abgeschlossen: " + formatBytes(downloadedBytes));

            } catch (IOException e) {
                Files.deleteIfExists(targetPath);
                throw e;
            } finally {
                activeDownloads.remove(modelId);
                activeDownloadCalls.remove(modelId);
            }
        }
    }

    /**
     * Erstellt HuggingFace Download-URL
     */
    private String buildHuggingFaceDownloadUrl(String repo, String filename) {
        return String.format(
            "https://huggingface.co/%s/resolve/main/%s",
            repo,
            filename
        );
    }

    /**
     * Library-Verzeichnis (für offizielle Modelle)
     * Verwendet FleetPathsConfiguration für plattformspezifische Pfade:
     * - Linux: ~/.java-fleet/models/library/
     * - Windows: %LOCALAPPDATA%\JavaFleet\models\library\
     */
    private Path getLibraryDir() {
        Path libraryDir = pathsConfig.getResolvedModelsDir().resolve("library");
        log.debug("Library-Verzeichnis: {}", libraryDir);
        return libraryDir;
    }

    /**
     * Custom-Verzeichnis (für user-uploads)
     */
    public Path getCustomDir() {
        return pathsConfig.getResolvedModelsDir().resolve("custom");
    }

    /**
     * Formatiert Bytes zu menschenlesbarer Größe
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ===== INNER CLASSES =====

    /**
     * Download Progress Tracker
     */
    public static class DownloadProgress {
        private final String modelId;
        private final String displayName;
        private final Long totalBytes;
        private Long downloadedBytes;
        private Integer percentComplete;
        private DownloadStatus status;
        private long startTime;
        private double speedMBps;

        public DownloadProgress(String modelId, String displayName, Long totalBytes) {
            this.modelId = modelId;
            this.displayName = displayName;
            this.totalBytes = totalBytes;
            this.downloadedBytes = 0L;
            this.percentComplete = 0;
            this.status = DownloadStatus.DOWNLOADING;
            this.startTime = System.currentTimeMillis();
            this.speedMBps = 0.0;
        }

        public void setDownloadedBytes(Long bytes) {
            this.downloadedBytes = bytes;
        }

        public void updateProgress() {
            if (totalBytes != null && totalBytes > 0) {
                this.percentComplete = (int) ((downloadedBytes * 100) / totalBytes);

                // Calculate speed
                long elapsedMs = System.currentTimeMillis() - startTime;
                if (elapsedMs > 0) {
                    double elapsedSec = elapsedMs / 1000.0;
                    double downloadedMB = downloadedBytes / (1024.0 * 1024.0);
                    this.speedMBps = downloadedMB / elapsedSec;
                }
            }
        }

        public void setStatus(DownloadStatus status) {
            this.status = status;
        }

        // Getters
        public String getModelId() { return modelId; }
        public String getDisplayName() { return displayName; }
        public Long getTotalBytes() { return totalBytes; }
        public Long getDownloadedBytes() { return downloadedBytes; }
        public Integer getPercentComplete() { return percentComplete; }
        public DownloadStatus getStatus() { return status; }
        public double getSpeedMBps() { return speedMBps; }
    }

    /**
     * Download Status Enum
     */
    public enum DownloadStatus {
        QUEUED,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
