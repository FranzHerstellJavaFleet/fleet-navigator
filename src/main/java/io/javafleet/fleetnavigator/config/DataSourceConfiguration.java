package io.javafleet.fleetnavigator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Konfiguriert die H2 DataSource mit plattformspezifischem Pfad.
 *
 * Diese Klasse ermittelt den korrekten Datenpfad basierend auf dem
 * Betriebssystem BEVOR Spring Boot die DataSource initialisiert.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@Configuration
@Slf4j
public class DataSourceConfiguration {

    /**
     * Erstellt die H2 DataSource mit plattformspezifischem Pfad
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        Path dataDir = resolveDataDir();
        String dbPath = dataDir.resolve("fleetnavdb").toString();

        log.info("H2 Datenbank-Pfad: {}", dbPath);

        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:file:" + dbPath + ";DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE")
                .username("sa")
                .password("")
                .build();
    }

    /**
     * Ermittelt das Daten-Verzeichnis basierend auf dem OS
     */
    private Path resolveDataDir() {
        // Prüfe ob Override via System Property gesetzt ist
        String override = System.getProperty("fleet-navigator.paths.data-dir");
        if (override != null && !override.isBlank()) {
            Path path = Paths.get(override);
            ensureDirectoryExists(path);
            return path;
        }

        // Prüfe Environment Variable
        override = System.getenv("FLEET_NAVIGATOR_DATA_DIR");
        if (override != null && !override.isBlank()) {
            Path path = Paths.get(override);
            ensureDirectoryExists(path);
            return path;
        }

        // Plattformspezifischer Default
        Path baseDir = resolveBaseDir();
        Path dataDir = baseDir.resolve("data");
        ensureDirectoryExists(dataDir);
        return dataDir;
    }

    /**
     * Ermittelt das Basis-Verzeichnis basierend auf dem OS
     */
    private Path resolveBaseDir() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows: %LOCALAPPDATA%\JavaFleet
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Paths.get(localAppData, "JavaFleet");
            }
            return Paths.get(userHome, "AppData", "Local", "JavaFleet");
        } else {
            // Linux/macOS: ~/.java-fleet
            return Paths.get(userHome, ".java-fleet");
        }
    }

    /**
     * Stellt sicher, dass das Verzeichnis existiert
     */
    private void ensureDirectoryExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Verzeichnis erstellt: {}", path);
            }
        } catch (IOException e) {
            log.error("Fehler beim Erstellen des Verzeichnisses {}: {}", path, e.getMessage());
        }
    }
}
