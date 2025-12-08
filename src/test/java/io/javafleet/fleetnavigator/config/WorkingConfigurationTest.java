package io.javafleet.fleetnavigator.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für die funktionierende Konfiguration.
 *
 * WICHTIG: Diese Tests dokumentieren den funktionierenden Zustand!
 * Stand: 2025-11-30
 *
 * Funktionierende Konfiguration:
 * - context-size: 4096 (NICHT 8192 - das führt zu GPU-Speicher-Problemen!)
 * - gpu-layers: 999
 * - default-provider: java-llama-cpp
 * - models-dir: auto-detect via FleetPathsConfiguration
 */
@DisplayName("Working Configuration Tests - Stand 2025-11-30")
class WorkingConfigurationTest {

    @Test
    @DisplayName("application.properties: context-size muss 4096 sein (nicht 8192!)")
    void contextSizeMustBe4096() throws IOException {
        Properties props = loadApplicationProperties();

        String contextSize = props.getProperty("llm.llamacpp.context-size");
        assertNotNull(contextSize, "context-size muss definiert sein");
        assertEquals("4096", contextSize,
            "context-size MUSS 4096 sein! 8192 führt zu GPU-Speicher-Problemen mit Mistral-Nemo (7.5GB)");
    }

    @Test
    @DisplayName("application.properties: gpu-layers muss 999 sein")
    void gpuLayersMustBe999() throws IOException {
        Properties props = loadApplicationProperties();

        String gpuLayers = props.getProperty("llm.llamacpp.gpu-layers");
        assertNotNull(gpuLayers, "gpu-layers muss definiert sein");
        assertEquals("999", gpuLayers,
            "gpu-layers sollte 999 sein für maximale GPU-Nutzung");
    }

    @Test
    @DisplayName("application.properties: default-provider muss java-llama-cpp sein")
    void defaultProviderMustBeJavaLlamaCpp() throws IOException {
        Properties props = loadApplicationProperties();

        String provider = props.getProperty("llm.default-provider");
        assertNotNull(provider, "default-provider muss definiert sein");
        assertEquals("java-llama-cpp", provider,
            "default-provider MUSS java-llama-cpp sein! Kein Fallback auf Ollama!");
    }

    @Test
    @DisplayName("application.properties: models-dir darf NICHT auf /opt/ zeigen")
    void modelsDirectoryMustNotBeOpt() throws IOException {
        Properties props = loadApplicationProperties();

        String modelsDir = props.getProperty("llm.llamacpp.models-dir");

        // Wenn gesetzt, darf es nicht auf /opt/ zeigen
        if (modelsDir != null && !modelsDir.isBlank()) {
            assertFalse(modelsDir.contains("/opt/"),
                "models-dir darf NICHT auf /opt/ zeigen! Muss ~/.java-fleet/models/ verwenden!");
        }
        // Wenn nicht gesetzt oder auskommentiert = OK (auto-detect wird verwendet)
    }

    @Test
    @DisplayName("application.properties: java-llama-cpp muss enabled sein")
    void javaLlamaCppMustBeEnabled() throws IOException {
        Properties props = loadApplicationProperties();

        String enabled = props.getProperty("llm.llamacpp.enabled");
        assertNotNull(enabled, "llm.llamacpp.enabled muss definiert sein");
        assertEquals("true", enabled,
            "java-llama-cpp MUSS enabled sein!");
    }

    @Test
    @DisplayName("FleetPathsConfiguration: Basis-Verzeichnis unter User-Home")
    void fleetPathsConfigurationUsesUserHome() {
        FleetPathsConfiguration config = new FleetPathsConfiguration();
        config.init(); // Manuell initialisieren (ohne Spring-Kontext)
        Path baseDir = config.getResolvedBaseDir();

        String userHome = System.getProperty("user.home");
        assertTrue(baseDir.toString().startsWith(userHome),
            "Basis-Verzeichnis muss unter User-Home liegen: " + baseDir);

        assertTrue(baseDir.toString().contains(".java-fleet"),
            "Basis-Verzeichnis muss .java-fleet enthalten: " + baseDir);
    }

    @Test
    @DisplayName("FleetPathsConfiguration: Models-Verzeichnis unter ~/.java-fleet/models/")
    void fleetPathsConfigurationModelsDir() {
        FleetPathsConfiguration config = new FleetPathsConfiguration();
        config.init(); // Manuell initialisieren (ohne Spring-Kontext)
        Path modelsDir = config.getResolvedModelsDir();

        String expected = System.getProperty("user.home") + "/.java-fleet/models";
        assertEquals(Paths.get(expected), modelsDir,
            "Models-Verzeichnis muss ~/.java-fleet/models/ sein");
    }

    @Test
    @DisplayName("Server-Port muss 2025 sein")
    void serverPortMustBe2025() throws IOException {
        Properties props = loadApplicationProperties();

        String port = props.getProperty("server.port");
        assertNotNull(port, "server.port muss definiert sein");
        assertEquals("2025", port, "Server-Port muss 2025 sein");
    }

    private Properties loadApplicationProperties() throws IOException {
        Properties props = new Properties();

        // Versuche aus dem Classpath zu laden
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
                return props;
            }
        }

        // Fallback: Direkt aus dem Dateisystem laden
        Path propsPath = Paths.get("src/main/resources/application.properties");
        if (Files.exists(propsPath)) {
            try (InputStream is = Files.newInputStream(propsPath)) {
                props.load(is);
            }
        }

        return props;
    }
}
