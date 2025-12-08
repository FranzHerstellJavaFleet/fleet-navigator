package io.javafleet.fleetnavigator.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Zentrale Pfad-Konfiguration für Fleet Navigator.
 *
 * Erkennt automatisch das Betriebssystem und verwendet
 * plattformspezifische Konventionen für Datenpfade:
 *
 * Linux/macOS: ~/.java-fleet/
 * Windows:     %LOCALAPPDATA%\JavaFleet\
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@Configuration
@ConfigurationProperties(prefix = "fleet-navigator.paths")
@Slf4j
@Getter
public class FleetPathsConfiguration {

    // Wird aus properties geladen (optional)
    private String baseDir;
    private String dataDir;
    private String modelsDir;
    private String logsDir;
    private String configDir;

    // Ermittelte Pfade
    private Path resolvedBaseDir;
    private Path resolvedDataDir;
    private Path resolvedModelsDir;
    private Path resolvedLogsDir;
    private Path resolvedConfigDir;

    /**
     * Betriebssystem-Typ
     */
    public enum OsType {
        WINDOWS, LINUX, MACOS, UNKNOWN
    }

    @PostConstruct
    public void init() {
        OsType osType = detectOs();
        log.info("Betriebssystem erkannt: {}", osType);

        // Base Directory ermitteln
        resolvedBaseDir = resolveBaseDir(osType);
        log.info("Fleet Navigator Basis-Verzeichnis: {}", resolvedBaseDir);

        // Unterverzeichnisse
        resolvedDataDir = resolveDir(dataDir, resolvedBaseDir.resolve("data"));
        resolvedModelsDir = resolveDir(modelsDir, resolvedBaseDir.resolve("models"));
        resolvedLogsDir = resolveDir(logsDir, resolvedBaseDir.resolve("logs"));
        resolvedConfigDir = resolveDir(configDir, resolvedBaseDir.resolve("config"));

        // Verzeichnisse erstellen
        createDirectories();

        // Zusammenfassung loggen
        logPaths();
    }

    /**
     * Ermittelt das Betriebssystem
     */
    private OsType detectOs() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OsType.WINDOWS;
        } else if (os.contains("mac")) {
            return OsType.MACOS;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return OsType.LINUX;
        }
        return OsType.UNKNOWN;
    }

    /**
     * Ermittelt das Basis-Verzeichnis basierend auf OS
     */
    private Path resolveBaseDir(OsType osType) {
        // Falls explizit konfiguriert, verwende das
        if (baseDir != null && !baseDir.isBlank()) {
            return Paths.get(baseDir);
        }

        // Plattformspezifische Defaults
        String userHome = System.getProperty("user.home");

        return switch (osType) {
            case WINDOWS -> {
                // Windows: %LOCALAPPDATA%\JavaFleet oder %USERPROFILE%\AppData\Local\JavaFleet
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null && !localAppData.isBlank()) {
                    yield Paths.get(localAppData, "JavaFleet");
                }
                yield Paths.get(userHome, "AppData", "Local", "JavaFleet");
            }
            case MACOS -> {
                // macOS: ~/Library/Application Support/JavaFleet (Apple Convention)
                // oder ~/.java-fleet für Konsistenz mit Linux
                yield Paths.get(userHome, ".java-fleet");
            }
            case LINUX, UNKNOWN -> {
                // Linux: ~/.java-fleet (Hidden folder in home)
                yield Paths.get(userHome, ".java-fleet");
            }
        };
    }

    /**
     * Löst einen Pfad auf - verwendet Override wenn gesetzt, sonst Default
     */
    private Path resolveDir(String override, Path defaultPath) {
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        return defaultPath;
    }

    /**
     * Erstellt alle notwendigen Verzeichnisse
     */
    private void createDirectories() {
        try {
            Files.createDirectories(resolvedDataDir);
            Files.createDirectories(resolvedModelsDir);
            Files.createDirectories(resolvedModelsDir.resolve("library"));
            Files.createDirectories(resolvedModelsDir.resolve("custom"));
            Files.createDirectories(resolvedLogsDir);
            Files.createDirectories(resolvedConfigDir);
            log.info("Alle Verzeichnisse erstellt/verifiziert");
        } catch (IOException e) {
            log.error("Fehler beim Erstellen der Verzeichnisse: {}", e.getMessage());
        }
    }

    /**
     * Loggt alle konfigurierten Pfade
     */
    private void logPaths() {
        OsType osType = detectOs();
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          Fleet Navigator Pfad-Konfiguration                ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║ OS:       {}  ", osType);
        log.info("║ Basis:    {}  ", resolvedBaseDir);
        log.info("║ Daten:    {}  ", resolvedDataDir);
        log.info("║ Modelle:  {}  ", resolvedModelsDir);
        log.info("║   └─ library/  (vorinstallierte Modelle)");
        log.info("║   └─ custom/   (benutzerdefinierte Modelle)");
        log.info("║ Logs:     {}  ", resolvedLogsDir);
        log.info("║ Config:   {}  ", resolvedConfigDir);
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║ Plattform-Pfade:                                           ║");
        log.info("║   Linux:   ~/.java-fleet/                                  ║");
        log.info("║   macOS:   ~/.java-fleet/                                  ║");
        log.info("║   Windows: %LOCALAPPDATA%\\JavaFleet\\                       ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
    }

    // ===== Convenience Methoden für häufig benötigte Pfade =====

    /**
     * Gibt den Pfad zur H2 Datenbank zurück (ohne .mv.db Endung)
     */
    public String getDatabasePath() {
        return resolvedDataDir.resolve("fleetnavdb").toString();
    }

    /**
     * Gibt den JDBC URL für H2 zurück
     */
    public String getDatabaseUrl() {
        return "jdbc:h2:file:" + getDatabasePath();
    }

    /**
     * Gibt den Pfad für generierte Dokumente zurück
     */
    public Path getGeneratedDocumentsDir() {
        Path dir = resolvedDataDir.resolve("generated-documents");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Konnte generated-documents Verzeichnis nicht erstellen: {}", e.getMessage());
        }
        return dir;
    }

    /**
     * Gibt den Pfad für hochgeladene Bilder zurück
     */
    public Path getImagesDir() {
        Path dir = resolvedDataDir.resolve("images");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Konnte images Verzeichnis nicht erstellen: {}", e.getMessage());
        }
        return dir;
    }

    /**
     * Gibt den Pfad für den Datei-Index zurück
     */
    public Path getFileIndexDir() {
        Path dir = resolvedDataDir.resolve("file-index");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Konnte file-index Verzeichnis nicht erstellen: {}", e.getMessage());
        }
        return dir;
    }

    /**
     * Gibt den Pfad für die Log-Datei zurück
     */
    public String getLogFilePath() {
        return resolvedLogsDir.resolve("fleet-navigator.log").toString();
    }

    /**
     * Setter für Properties-Injection
     */
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public void setModelsDir(String modelsDir) {
        this.modelsDir = modelsDir;
    }

    public void setLogsDir(String logsDir) {
        this.logsDir = logsDir;
    }

    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }
}
