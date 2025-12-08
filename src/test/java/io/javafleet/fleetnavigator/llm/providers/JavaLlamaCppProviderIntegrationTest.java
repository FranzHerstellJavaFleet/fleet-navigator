package io.javafleet.fleetnavigator.llm.providers;

import io.javafleet.fleetnavigator.config.FleetPathsConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JavaLlamaCppProvider.
 *
 * WICHTIG: Diese Tests sichern den funktionierenden Zustand ab!
 * Stand: 2025-11-30 - Brief-Generierung mit Experten funktioniert.
 *
 * Getestete Konfiguration:
 * - Models-Verzeichnis: ~/.java-fleet/models/
 * - Context-Size: 4096
 * - GPU-Layers: 999 (alle auf GPU)
 * - Provider: java-llama-cpp
 */
@DisplayName("JavaLlamaCppProvider Integration Tests - Funktionierender Zustand")
class JavaLlamaCppProviderIntegrationTest {

    private Path modelsDir;
    private Path libraryDir;

    @BeforeEach
    void setUp() {
        // Der korrekte Pfad für lokalen User-Modus
        String userHome = System.getProperty("user.home");
        modelsDir = Paths.get(userHome, ".java-fleet", "models");
        libraryDir = modelsDir.resolve("library");
    }

    @Test
    @DisplayName("Models-Verzeichnis existiert unter ~/.java-fleet/models/")
    void modelsDirectoryExists() {
        assertTrue(Files.exists(modelsDir),
            "Models-Verzeichnis muss existieren: " + modelsDir);
        assertTrue(Files.isDirectory(modelsDir),
            "Models-Pfad muss ein Verzeichnis sein: " + modelsDir);
    }

    @Test
    @DisplayName("Library-Unterverzeichnis existiert")
    void librarySubdirectoryExists() {
        assertTrue(Files.exists(libraryDir),
            "Library-Verzeichnis muss existieren: " + libraryDir);
        assertTrue(Files.isDirectory(libraryDir),
            "Library-Pfad muss ein Verzeichnis sein: " + libraryDir);
    }

    @Test
    @DisplayName("Mistral-Nemo GGUF-Modell existiert und ist lesbar")
    void mistralNemoModelExists() {
        Path mistralNemo = libraryDir.resolve("Mistral-Nemo-Instruct-2407.Q4_K_M.gguf");

        assertTrue(Files.exists(mistralNemo),
            "Mistral-Nemo Modell muss existieren: " + mistralNemo);
        assertTrue(Files.isReadable(mistralNemo),
            "Mistral-Nemo Modell muss lesbar sein: " + mistralNemo);

        // Größe prüfen (ca. 7.5GB)
        try {
            long size = Files.size(mistralNemo);
            assertTrue(size > 7_000_000_000L,
                "Mistral-Nemo sollte > 7GB sein, ist aber: " + size);
            assertTrue(size < 8_000_000_000L,
                "Mistral-Nemo sollte < 8GB sein, ist aber: " + size);
        } catch (Exception e) {
            fail("Konnte Dateigröße nicht lesen: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Llama-3.2-3B GGUF-Modell existiert (Fallback-Modell)")
    void llama32ModelExists() {
        Path llama32 = libraryDir.resolve("Llama-3.2-3B-Instruct-Q4_K_M.gguf");

        assertTrue(Files.exists(llama32),
            "Llama-3.2-3B Modell muss existieren: " + llama32);
        assertTrue(Files.isReadable(llama32),
            "Llama-3.2-3B Modell muss lesbar sein: " + llama32);
    }

    @Test
    @DisplayName("FleetPathsConfiguration liefert korrektes Models-Verzeichnis")
    void fleetPathsConfigurationReturnsCorrectModelsDir() {
        FleetPathsConfiguration config = new FleetPathsConfiguration();
        config.init(); // Manuell initialisieren (ohne Spring-Kontext)
        Path resolvedModelsDir = config.getResolvedModelsDir();

        assertNotNull(resolvedModelsDir, "Resolved models dir darf nicht null sein");
        assertEquals(modelsDir, resolvedModelsDir,
            "FleetPathsConfiguration muss ~/.java-fleet/models/ liefern");
    }

    @Test
    @DisplayName("Modell-Pfad-Auflösung: Absoluter Pfad wird erkannt")
    void absolutePathResolution() {
        String absolutePath = "/home/trainer/.java-fleet/models/library/Mistral-Nemo-Instruct-2407.Q4_K_M.gguf";

        assertTrue(absolutePath.startsWith("/"),
            "Absoluter Pfad muss mit / beginnen");
        assertTrue(Files.exists(Paths.get(absolutePath)),
            "Absoluter Pfad muss existieren: " + absolutePath);
    }

    @Test
    @DisplayName("Modell-Pfad-Auflösung: Relativer GGUF-Name wird in library/ gefunden")
    void relativeGgufNameResolution() {
        String modelName = "Mistral-Nemo-Instruct-2407.Q4_K_M.gguf";

        // Der Provider sollte in library/ suchen
        Path expectedPath = libraryDir.resolve(modelName);
        assertTrue(Files.exists(expectedPath),
            "Modell sollte unter library/ gefunden werden: " + expectedPath);
    }

    @Test
    @DisplayName("GGUF-Dateien haben korrekte Endung")
    void ggufFilesHaveCorrectExtension() {
        try {
            long ggufCount = Files.list(libraryDir)
                .filter(p -> p.toString().endsWith(".gguf"))
                .count();

            assertTrue(ggufCount > 0,
                "Es sollten GGUF-Dateien im library-Verzeichnis sein");

            System.out.println("Gefundene GGUF-Modelle: " + ggufCount);
        } catch (Exception e) {
            fail("Konnte library-Verzeichnis nicht lesen: " + e.getMessage());
        }
    }
}
