package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.config.FleetPathsConfiguration;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service für automatische Updates von GitHub Releases.
 *
 * Funktionsweise:
 * 1. Prüft beim Start und alle 6 Stunden auf neue Releases
 * 2. Vergleicht Versionen (Semantic Versioning)
 * 3. Lädt bei neuerer Version das passende Archiv herunter
 * 4. Entpackt und ersetzt das JAR
 * 5. Signalisiert Neustart-Bedarf
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoUpdateService {

    private final FleetPathsConfiguration pathsConfig;
    private final ObjectMapper objectMapper;

    @Value("${fleet-navigator.version:0.5.0}")
    private String currentVersion;

    @Value("${fleet-navigator.update.enabled:true}")
    private boolean updateEnabled;

    @Value("${fleet-navigator.update.github-repo:Franz-Martin/Fleet-Navigator}")
    private String githubRepo;

    @Value("${fleet-navigator.update.check-interval-hours:6}")
    private int checkIntervalHours;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private final AtomicReference<UpdateInfo> latestUpdate = new AtomicReference<>();
    private final AtomicReference<String> downloadProgress = new AtomicReference<>("");
    private final AtomicReference<LocalDateTime> lastCheckTime = new AtomicReference<>();

    @PostConstruct
    public void init() {
        if (updateEnabled) {
            log.info("Auto-Update Service initialisiert für Repository: {}", githubRepo);
            log.info("Aktuelle Version: {}", currentVersion);
            // Verzögerter erster Check (30 Sekunden nach Start)
            new Thread(() -> {
                try {
                    Thread.sleep(30000);
                    checkForUpdates();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            log.info("Auto-Update Service ist deaktiviert");
        }
    }

    /**
     * Prüft alle 6 Stunden auf Updates (konfigurierbar)
     */
    @Scheduled(fixedRateString = "${fleet-navigator.update.check-interval-ms:21600000}")
    public void scheduledUpdateCheck() {
        if (updateEnabled) {
            checkForUpdates();
        }
    }

    /**
     * Prüft GitHub auf neue Releases
     */
    public UpdateCheckResult checkForUpdates() {
        log.info("Prüfe auf Updates...");
        lastCheckTime.set(LocalDateTime.now());

        try {
            String apiUrl = String.format("https://api.github.com/repos/%s/releases/latest", githubRepo);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Fleet-Navigator-AutoUpdate")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                log.info("Noch keine Releases verfügbar");
                return new UpdateCheckResult(false, currentVersion, null, "Keine Releases gefunden");
            }

            if (response.statusCode() != 200) {
                log.warn("GitHub API Fehler: {} - {}", response.statusCode(), response.body());
                return new UpdateCheckResult(false, currentVersion, null,
                        "GitHub API Fehler: " + response.statusCode());
            }

            GitHubRelease release = objectMapper.readValue(response.body(), GitHubRelease.class);
            String latestVersion = release.getTagName().replaceFirst("^v", "");

            log.info("Neueste Version auf GitHub: {}", latestVersion);

            if (isNewerVersion(latestVersion, currentVersion)) {
                log.info("Neue Version verfügbar: {} -> {}", currentVersion, latestVersion);

                // Passendes Asset finden
                String osName = System.getProperty("os.name").toLowerCase();
                String arch = System.getProperty("os.arch").toLowerCase();
                String assetPattern = determineAssetPattern(osName, arch);

                Optional<GitHubAsset> matchingAsset = release.getAssets().stream()
                        .filter(a -> a.getName().contains(assetPattern))
                        .findFirst();

                if (matchingAsset.isPresent()) {
                    UpdateInfo info = new UpdateInfo(
                            latestVersion,
                            release.getTagName(),
                            release.getName(),
                            release.getBody(),
                            release.getHtmlUrl(),
                            matchingAsset.get().getName(),
                            matchingAsset.get().getBrowserDownloadUrl(),
                            matchingAsset.get().getSize()
                    );
                    latestUpdate.set(info);
                    updateAvailable.set(true);

                    return new UpdateCheckResult(true, currentVersion, info, "Update verfügbar");
                } else {
                    log.warn("Kein passendes Asset für {} {} gefunden", osName, arch);
                    return new UpdateCheckResult(false, currentVersion, null,
                            "Kein passendes Download-Paket für dieses System gefunden");
                }
            } else {
                log.info("Keine neue Version verfügbar");
                updateAvailable.set(false);
                return new UpdateCheckResult(false, currentVersion, null, "Bereits auf dem neuesten Stand");
            }

        } catch (Exception e) {
            log.error("Fehler beim Update-Check: {}", e.getMessage(), e);
            return new UpdateCheckResult(false, currentVersion, null, "Fehler: " + e.getMessage());
        }
    }

    /**
     * Lädt das Update herunter und bereitet es vor
     */
    public DownloadResult downloadUpdate() {
        UpdateInfo info = latestUpdate.get();
        if (info == null) {
            return new DownloadResult(false, "Kein Update verfügbar", null);
        }

        if (downloadInProgress.get()) {
            return new DownloadResult(false, "Download bereits im Gange", null);
        }

        downloadInProgress.set(true);
        downloadProgress.set("Starte Download...");

        try {
            Path updateDir = pathsConfig.getResolvedDataDir().resolve("updates");
            Files.createDirectories(updateDir);

            Path downloadPath = updateDir.resolve(info.getAssetName());

            log.info("Lade Update herunter: {} -> {}", info.getDownloadUrl(), downloadPath);
            downloadProgress.set("Lade " + info.getAssetName() + " herunter...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(info.getDownloadUrl()))
                    .header("User-Agent", "Fleet-Navigator-AutoUpdate")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("Download fehlgeschlagen: HTTP " + response.statusCode());
            }

            // Download mit Progress-Tracking
            long totalSize = info.getAssetSize();
            long downloaded = 0;
            byte[] buffer = new byte[8192];
            int bytesRead;

            try (InputStream in = response.body();
                 var out = Files.newOutputStream(downloadPath)) {
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    int percent = (int) ((downloaded * 100) / totalSize);
                    downloadProgress.set(String.format("Download: %d%% (%s / %s)",
                            percent, formatSize(downloaded), formatSize(totalSize)));
                }
            }

            downloadProgress.set("Download abgeschlossen. Entpacke...");
            log.info("Download abgeschlossen: {}", downloadPath);

            // Entpacken
            Path extractDir = updateDir.resolve("extracted");
            if (Files.exists(extractDir)) {
                deleteDirectory(extractDir);
            }
            Files.createDirectories(extractDir);

            if (info.getAssetName().endsWith(".tar.gz")) {
                extractTarGz(downloadPath, extractDir);
            } else if (info.getAssetName().endsWith(".zip")) {
                extractZip(downloadPath, extractDir);
            }

            downloadProgress.set("Update bereit zur Installation");
            log.info("Update entpackt nach: {}", extractDir);

            return new DownloadResult(true, "Update bereit zur Installation", extractDir);

        } catch (Exception e) {
            log.error("Download-Fehler: {}", e.getMessage(), e);
            downloadProgress.set("Fehler: " + e.getMessage());
            return new DownloadResult(false, "Download-Fehler: " + e.getMessage(), null);
        } finally {
            downloadInProgress.set(false);
        }
    }

    /**
     * Installiert das heruntergeladene Update (JAR austauschen)
     */
    public InstallResult installUpdate() {
        try {
            Path updateDir = pathsConfig.getResolvedDataDir().resolve("updates").resolve("extracted");

            if (!Files.exists(updateDir)) {
                return new InstallResult(false, "Kein heruntergeladenes Update gefunden", false);
            }

            // JAR finden
            Optional<Path> newJar = Files.walk(updateDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> p.getFileName().toString().startsWith("fleet-navigator"))
                    .findFirst();

            if (newJar.isEmpty()) {
                return new InstallResult(false, "Kein JAR im Update-Paket gefunden", false);
            }

            // Aktuelles JAR finden
            Path currentJar = getCurrentJarPath();
            if (currentJar == null) {
                return new InstallResult(false, "Kann aktuelles JAR nicht ermitteln", false);
            }

            // Backup erstellen
            Path backupDir = pathsConfig.getResolvedDataDir().resolve("backups");
            Files.createDirectories(backupDir);
            Path backupJar = backupDir.resolve("fleet-navigator-" + currentVersion + ".jar.backup");
            Files.copy(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
            log.info("Backup erstellt: {}", backupJar);

            // Neues JAR kopieren (wird beim Neustart wirksam)
            Path pendingUpdate = currentJar.getParent().resolve("fleet-navigator.jar.update");
            Files.copy(newJar.get(), pendingUpdate, StandardCopyOption.REPLACE_EXISTING);

            // Update-Marker schreiben
            Path markerFile = pathsConfig.getResolvedDataDir().resolve("pending-update.marker");
            Files.writeString(markerFile, latestUpdate.get().getVersion());

            log.info("Update vorbereitet. Neustart erforderlich.");

            return new InstallResult(true,
                    "Update auf Version " + latestUpdate.get().getVersion() + " vorbereitet. Bitte Anwendung neu starten.",
                    true);

        } catch (Exception e) {
            log.error("Installations-Fehler: {}", e.getMessage(), e);
            return new InstallResult(false, "Installations-Fehler: " + e.getMessage(), false);
        }
    }

    /**
     * Vergleicht zwei Versionen (Semantic Versioning)
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        try {
            String[] newParts = newVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");

            for (int i = 0; i < Math.max(newParts.length, currentParts.length); i++) {
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i].replaceAll("[^0-9]", "")) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;

                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            return false;
        } catch (Exception e) {
            log.warn("Versionsvergleich fehlgeschlagen: {} vs {}", newVersion, currentVersion);
            return false;
        }
    }

    /**
     * Ermittelt das passende Asset-Pattern für das aktuelle OS
     */
    private String determineAssetPattern(String osName, String arch) {
        if (osName.contains("linux")) {
            return "linux-x64";
        } else if (osName.contains("windows")) {
            return "windows-x64";
        } else if (osName.contains("mac")) {
            return arch.contains("aarch64") || arch.contains("arm") ? "macos-arm64" : "macos-x64";
        }
        return "linux-x64"; // Default
    }

    /**
     * Ermittelt den Pfad zum aktuell laufenden JAR
     */
    private Path getCurrentJarPath() {
        try {
            String jarPath = getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            if (jarPath.endsWith(".jar")) {
                return Path.of(jarPath);
            }

            // Development mode - suche im target/
            Path targetJar = Path.of("target").toAbsolutePath();
            if (Files.exists(targetJar)) {
                return Files.list(targetJar)
                        .filter(p -> p.toString().endsWith(".jar"))
                        .filter(p -> p.getFileName().toString().startsWith("fleet-navigator"))
                        .filter(p -> !p.toString().contains("sources"))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Kann JAR-Pfad nicht ermitteln: {}", e.getMessage());
        }
        return null;
    }

    private void extractTarGz(Path archive, Path destDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", archive.toString(), "-C", destDir.toString());
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("tar Extraktion fehlgeschlagen mit Exit-Code: " + exitCode);
        }
    }

    private void extractZip(Path archive, Path destDir) throws IOException {
        try (var zipFs = java.nio.file.FileSystems.newFileSystem(archive)) {
            for (var root : zipFs.getRootDirectories()) {
                Files.walk(root).forEach(source -> {
                    try {
                        Path dest = destDir.resolve(root.relativize(source).toString());
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(dest);
                        } else {
                            Files.createDirectories(dest.getParent());
                            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Konnte nicht löschen: {}", path);
                        }
                    });
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // Getter für Status-Abfragen
    public boolean isUpdateAvailable() {
        return updateAvailable.get();
    }

    public boolean isDownloadInProgress() {
        return downloadInProgress.get();
    }

    public String getDownloadProgress() {
        return downloadProgress.get();
    }

    public UpdateInfo getLatestUpdateInfo() {
        return latestUpdate.get();
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime.get();
    }

    // DTOs
    @Data
    public static class UpdateCheckResult {
        private final boolean updateAvailable;
        private final String currentVersion;
        private final UpdateInfo updateInfo;
        private final String message;
    }

    @Data
    public static class DownloadResult {
        private final boolean success;
        private final String message;
        private final Path extractedPath;
    }

    @Data
    public static class InstallResult {
        private final boolean success;
        private final String message;
        private final boolean restartRequired;
    }

    @Data
    public static class UpdateInfo {
        private final String version;
        private final String tagName;
        private final String releaseName;
        private final String releaseNotes;
        private final String releaseUrl;
        private final String assetName;
        private final String downloadUrl;
        private final long assetSize;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitHubRelease {
        private String tag_name;
        private String name;
        private String body;
        private String html_url;
        private List<GitHubAsset> assets;

        public String getTagName() { return tag_name; }
        public String getHtmlUrl() { return html_url; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitHubAsset {
        private String name;
        private String browser_download_url;
        private long size;

        public String getBrowserDownloadUrl() { return browser_download_url; }
    }
}
