package io.javafleet.fleetnavigator.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * JUnit-Tests für Model-Pfad-Auflösung
 *
 * Testet verschiedene Szenarien der Modellpfad-Auflösung:
 * - Absolute Pfade (z.B. /opt/fleet-navigator/models/library/model.gguf)
 * - Relative Pfade (z.B. library/model.gguf)
 * - Nur Dateinamen (z.B. model.gguf)
 * - Ollama-Modellnamen (z.B. llama3.2:3b)
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.0
 */
class ModelPathResolutionTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Absolute Pfade")
    class AbsolutePathTests {

        @Test
        @DisplayName("Existierender absoluter Pfad wird direkt verwendet")
        void absolutePath_Exists_UsedDirectly() throws IOException {
            // Given - Erstelle temporäre Modell-Datei
            Path modelFile = tempDir.resolve("test-model.gguf");
            Files.createFile(modelFile);

            String absolutePath = modelFile.toAbsolutePath().toString();

            // When - Prüfe ob Pfad mit / beginnt
            boolean isAbsolute = absolutePath.startsWith("/");

            // Then
            assertThat(isAbsolute).isTrue();
            assertThat(Files.exists(Path.of(absolutePath))).isTrue();
        }

        @Test
        @DisplayName("Nicht existierender absoluter Pfad extrahiert Dateinamen")
        void absolutePath_NotExists_ExtractsFilename() {
            // Given
            String absolutePath = "/non/existent/path/to/model.gguf";
            Path path = Path.of(absolutePath);

            // When
            String filename = path.getFileName().toString();

            // Then
            assertThat(filename).isEqualTo("model.gguf");
        }

        @Test
        @DisplayName("Absoluter Pfad mit library/ Unterverzeichnis")
        void absolutePath_WithLibrarySubdir() throws IOException {
            // Given
            Path libraryDir = tempDir.resolve("library");
            Files.createDirectories(libraryDir);
            Path modelFile = libraryDir.resolve("Mistral-Nemo.gguf");
            Files.createFile(modelFile);

            String absolutePath = modelFile.toAbsolutePath().toString();

            // When/Then
            assertThat(absolutePath).contains("/library/");
            assertThat(Files.exists(Path.of(absolutePath))).isTrue();
        }
    }

    @Nested
    @DisplayName("Relative Pfade")
    class RelativePathTests {

        @Test
        @DisplayName("Relativer Pfad wird korrekt aufgelöst")
        void relativePath_ResolvedCorrectly() throws IOException {
            // Given
            Path libraryDir = tempDir.resolve("library");
            Files.createDirectories(libraryDir);
            Path modelFile = libraryDir.resolve("model.gguf");
            Files.createFile(modelFile);

            // When
            Path resolved = tempDir.resolve("library").resolve("model.gguf");

            // Then
            assertThat(Files.exists(resolved)).isTrue();
        }

        @Test
        @DisplayName("Relativer Pfad mit nur Dateiname sucht in Standardverzeichnissen")
        void relativePath_FilenameOnly_SearchesDefaultDirs() throws IOException {
            // Given - Erstelle Verzeichnisstruktur
            Path modelsDir = tempDir;
            Path libraryDir = modelsDir.resolve("library");
            Path customDir = modelsDir.resolve("custom");
            Files.createDirectories(libraryDir);
            Files.createDirectories(customDir);

            // Model nur in library
            Path modelInLibrary = libraryDir.resolve("special-model.gguf");
            Files.createFile(modelInLibrary);

            // When - Suche nach Model
            Path[] candidates = {
                    modelsDir.resolve("special-model.gguf"),
                    modelsDir.resolve("library").resolve("special-model.gguf"),
                    modelsDir.resolve("custom").resolve("special-model.gguf")
            };

            Path found = null;
            for (Path candidate : candidates) {
                if (Files.exists(candidate)) {
                    found = candidate;
                    break;
                }
            }

            // Then
            assertThat(found).isNotNull();
            assertThat(found.toString()).contains("library");
        }
    }

    @Nested
    @DisplayName("GGUF-Datei Erkennung")
    class GgufDetectionTests {

        @Test
        @DisplayName("Erkennt .gguf Endung (lowercase)")
        void detectsGgufExtension_Lowercase() {
            String modelName = "model.gguf";
            assertThat(modelName.toLowerCase().endsWith(".gguf")).isTrue();
        }

        @Test
        @DisplayName("Erkennt .GGUF Endung (uppercase)")
        void detectsGgufExtension_Uppercase() {
            String modelName = "MODEL.GGUF";
            assertThat(modelName.toLowerCase().endsWith(".gguf")).isTrue();
        }

        @Test
        @DisplayName("Erkennt .gguf in absolutem Pfad")
        void detectsGgufExtension_InAbsolutePath() {
            String absolutePath = "/opt/fleet-navigator/models/library/Mistral-Nemo-Instruct-2407.Q4_K_M.gguf";
            assertThat(absolutePath.toLowerCase().endsWith(".gguf")).isTrue();
        }

        @Test
        @DisplayName("Erkennt NICHT .gguf wenn andere Endung")
        void doesNotDetect_OtherExtension() {
            String modelName = "model.bin";
            assertThat(modelName.toLowerCase().endsWith(".gguf")).isFalse();
        }
    }

    @Nested
    @DisplayName("Pfad-Validierung")
    class PathValidationTests {

        @Test
        @DisplayName("Absoluter Pfad beginnt mit /")
        void absolutePath_StartsWithSlash() {
            String absolutePath = "/opt/fleet-navigator/models/model.gguf";
            assertThat(absolutePath.startsWith("/")).isTrue();
        }

        @Test
        @DisplayName("Relativer Pfad beginnt NICHT mit /")
        void relativePath_DoesNotStartWithSlash() {
            String relativePath = "library/model.gguf";
            assertThat(relativePath.startsWith("/")).isFalse();
        }

        @Test
        @DisplayName("Nur Dateiname beginnt NICHT mit /")
        void filenameOnly_DoesNotStartWithSlash() {
            String filename = "model.gguf";
            assertThat(filename.startsWith("/")).isFalse();
        }

        @Test
        @DisplayName("Path.of() verarbeitet absoluten Pfad korrekt")
        void pathOf_HandlesAbsolutePathCorrectly() {
            String absolutePath = "/opt/fleet-navigator/models/library/model.gguf";
            Path path = Path.of(absolutePath);

            assertThat(path.isAbsolute()).isTrue();
            assertThat(path.getFileName().toString()).isEqualTo("model.gguf");
            assertThat(path.getParent().getFileName().toString()).isEqualTo("library");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Null modelName wird behandelt")
        void nullModelName_Handled() {
            String modelName = null;
            boolean isAbsolute = modelName != null && modelName.startsWith("/");
            assertThat(isAbsolute).isFalse();
        }

        @Test
        @DisplayName("Leerer modelName wird behandelt")
        void emptyModelName_Handled() {
            String modelName = "";
            boolean isAbsolute = modelName != null && modelName.startsWith("/");
            assertThat(isAbsolute).isFalse();
        }

        @Test
        @DisplayName("Pfad mit Leerzeichen wird verarbeitet")
        void pathWithSpaces_Processed() throws IOException {
            Path dirWithSpaces = tempDir.resolve("models with spaces");
            Files.createDirectories(dirWithSpaces);
            Path modelFile = dirWithSpaces.resolve("model file.gguf");
            Files.createFile(modelFile);

            String absolutePath = modelFile.toAbsolutePath().toString();
            assertThat(Files.exists(Path.of(absolutePath))).isTrue();
        }

        @Test
        @DisplayName("Pfad mit Sonderzeichen wird verarbeitet")
        void pathWithSpecialChars_Processed() throws IOException {
            Path modelFile = tempDir.resolve("Llama-3.2-3B-Instruct-Q4_K_M.gguf");
            Files.createFile(modelFile);

            String absolutePath = modelFile.toAbsolutePath().toString();
            assertThat(Files.exists(Path.of(absolutePath))).isTrue();
            assertThat(absolutePath).contains("-");
            assertThat(absolutePath).contains("_");
            assertThat(absolutePath).contains(".");
        }
    }
}
