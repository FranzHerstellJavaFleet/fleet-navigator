package io.javafleet.fleetnavigator.experts.runtime;

import io.javafleet.fleetnavigator.config.FleetPathsConfiguration;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import io.javafleet.fleetnavigator.experts.repository.ExpertRepository;
import io.javafleet.fleetnavigator.llm.providers.ExternalLlamaServerProvider;
import io.javafleet.fleetnavigator.llm.providers.JavaLlamaCppProvider;
import io.javafleet.fleetnavigator.llm.providers.OllamaProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit Tests für ExpertRuntimeFactory.
 *
 * Testet die Factory für ExpertRuntime-Instanzen:
 * - Provider-Auswahl (java-llama-cpp vs ollama)
 * - Model-Pfad-Auflösung
 * - Caching von ExpertRuntime-Instanzen
 * - Mode-Auflösung
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExpertRuntimeFactory Tests")
class ExpertRuntimeFactoryTest {

    @Mock
    private ExpertRepository expertRepository;

    @Mock
    private FleetPathsConfiguration pathsConfig;

    @Mock
    private LLMConfigProperties llmConfig;

    @Mock
    private JavaLlamaCppProvider javaLlamaCppProvider;

    @Mock
    private OllamaProvider ollamaProvider;

    @Mock
    private ExternalLlamaServerProvider llamaServerProvider;

    @TempDir
    Path tempDir;

    private ExpertRuntimeFactory factory;
    private Expert testExpert;
    private ExpertMode testMode;

    @BeforeEach
    void setUp() {
        factory = new ExpertRuntimeFactory(
            expertRepository,
            pathsConfig,
            llmConfig,
            javaLlamaCppProvider,
            ollamaProvider,
            llamaServerProvider
        );

        // Test-Expert mit Modus erstellen
        testExpert = new Expert();
        testExpert.setId(1L);
        testExpert.setName("TestExperte");
        testExpert.setRole("Berater");
        testExpert.setBaseModel("mistral:latest");
        testExpert.setGgufModel("Mistral-7B.Q4_K_M.gguf");
        testExpert.setBasePrompt("Du bist ein Testexperte.");
        testExpert.setDefaultTemperature(0.7);
        testExpert.setDefaultTopP(0.9);

        testMode = new ExpertMode();
        testMode.setId(10L);
        testMode.setName("Analytisch");
        testMode.setPromptAddition("Analysiere gründlich.");
        testMode.setTemperature(0.3);

        List<ExpertMode> modes = new ArrayList<>();
        modes.add(testMode);
        testExpert.setModes(modes);
    }

    @Nested
    @DisplayName("getRuntime()")
    class GetRuntimeTests {

        @Test
        @DisplayName("Gibt empty zurück bei null expertId")
        void shouldReturnEmptyForNullExpertId() {
            Optional<ExpertRuntime> result = factory.getRuntime(null, null, false);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Gibt empty zurück wenn Expert nicht gefunden")
        void shouldReturnEmptyIfExpertNotFound() {
            when(expertRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<ExpertRuntime> result = factory.getRuntime(999L, null, false);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Erstellt ExpertRuntime für gültigen Expert")
        void shouldCreateRuntimeForValidExpert() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("TestExperte");
            assertThat(result.get().getRole()).isEqualTo("Berater");
        }

        @Test
        @DisplayName("Löst aktiven Modus korrekt auf")
        void shouldResolveActiveMode() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, 10L, false);

            assertThat(result).isPresent();
            assertThat(result.get().getActiveModeName()).isEqualTo("Analytisch");
            assertThat(result.get().getTemperature()).isEqualTo(0.3); // Modus-Temperatur
        }

        @Test
        @DisplayName("Ignoriert ungültige Modus-ID")
        void shouldIgnoreInvalidModeId() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, 999L, false);

            assertThat(result).isPresent();
            assertThat(result.get().getActiveModeName()).isNull();
        }
    }

    @Nested
    @DisplayName("Provider-Auswahl")
    class ProviderSelectionTests {

        @Test
        @DisplayName("Wählt java-llama-cpp wenn GGUF-Modell explizit gesetzt")
        void shouldSelectJavaLlamaCppForExplicitGguf() {
            testExpert.setGgufModel("Mistral-7B.Q4_K_M.gguf");
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderName()).isEqualTo("java-llama-cpp");
        }

        @Test
        @DisplayName("Wählt java-llama-cpp wenn baseModel auf .gguf endet")
        void shouldSelectJavaLlamaCppForGgufBaseModel() {
            testExpert.setGgufModel(null);
            testExpert.setBaseModel("custom-model.gguf");
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderName()).isEqualTo("java-llama-cpp");
        }

        @Test
        @DisplayName("Wählt Default-Provider aus Config")
        void shouldSelectDefaultProviderFromConfig() {
            testExpert.setGgufModel(null);
            testExpert.setBaseModel("mistral:latest");
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderName()).isEqualTo("ollama");
        }

        @Test
        @DisplayName("Fallback auf java-llama-cpp wenn verfügbar")
        void shouldFallbackToJavaLlamaCpp() {
            testExpert.setGgufModel(null);
            testExpert.setBaseModel("mistral:latest");
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(llmConfig.getDefaultProvider()).thenReturn("unknown");
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderName()).isEqualTo("java-llama-cpp");
        }

        @Test
        @DisplayName("Fallback auf Ollama wenn java-llama-cpp nicht verfügbar")
        void shouldFallbackToOllama() {
            testExpert.setGgufModel(null);
            testExpert.setBaseModel("mistral:latest");
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(llmConfig.getDefaultProvider()).thenReturn("unknown");
            when(javaLlamaCppProvider.isAvailable()).thenReturn(false);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderName()).isEqualTo("ollama");
        }
    }

    @Nested
    @DisplayName("Modell-Pfad-Auflösung")
    class ModelPathResolutionTests {

        @Test
        @DisplayName("Findet Modell im Hauptverzeichnis")
        void shouldFindModelInMainDir() throws IOException {
            Path modelFile = tempDir.resolve("Mistral-7B.Q4_K_M.gguf");
            Files.createFile(modelFile);

            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getResolvedModelPath()).isEqualTo(modelFile);
        }

        @Test
        @DisplayName("Findet Modell im library-Unterverzeichnis")
        void shouldFindModelInLibrarySubdir() throws IOException {
            Path libraryDir = tempDir.resolve("library");
            Files.createDirectory(libraryDir);
            Path modelFile = libraryDir.resolve("Mistral-7B.Q4_K_M.gguf");
            Files.createFile(modelFile);

            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getResolvedModelPath()).isEqualTo(modelFile);
        }

        @Test
        @DisplayName("Findet Modell im custom-Unterverzeichnis")
        void shouldFindModelInCustomSubdir() throws IOException {
            Path customDir = tempDir.resolve("custom");
            Files.createDirectory(customDir);
            Path modelFile = customDir.resolve("Mistral-7B.Q4_K_M.gguf");
            Files.createFile(modelFile);

            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getResolvedModelPath()).isEqualTo(modelFile);
        }

        @Test
        @DisplayName("Fuzzy-Match findet ähnliche Modellnamen")
        void shouldFindModelWithFuzzyMatch() throws IOException {
            Path libraryDir = tempDir.resolve("library");
            Files.createDirectory(libraryDir);
            // Leicht anderer Name
            Path modelFile = libraryDir.resolve("mistral-7b.Q4_K_M.gguf");
            Files.createFile(modelFile);

            testExpert.setGgufModel("Mistral-7B.Q4_K_M.gguf");

            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getResolvedModelPath()).isEqualTo(modelFile);
        }

        @Test
        @DisplayName("Gibt null für Modell-Pfad bei Ollama-Provider")
        void shouldReturnNullPathForOllama() {
            testExpert.setGgufModel(null);
            testExpert.setBaseModel("mistral:latest");

            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getResolvedModelPath()).isNull();
        }

        @Test
        @DisplayName("Gibt null wenn Modell nicht gefunden")
        void shouldReturnNullIfModelNotFound() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
            when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.getRuntime(1L, null, false);

            assertThat(result).isPresent();
            assertThat(result.get().getResolvedModelPath()).isNull();
        }
    }

    @Nested
    @DisplayName("Caching")
    class CachingTests {

        @Test
        @DisplayName("Cached ExpertRuntime wird wiederverwendet")
        void shouldReuseFromCache() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            // Erster Aufruf
            Optional<ExpertRuntime> first = factory.getRuntime(1L, null, false);

            // Repository-Mock zurücksetzen um zu prüfen ob Cache greift
            org.mockito.Mockito.reset(expertRepository);

            // Zweiter Aufruf - sollte aus Cache kommen
            Optional<ExpertRuntime> second = factory.getRuntime(1L, null, false);

            assertThat(first).isPresent();
            assertThat(second).isPresent();
            assertThat(first.get()).isSameAs(second.get());

            // Repository sollte NICHT erneut aufgerufen worden sein
            org.mockito.Mockito.verifyNoInteractions(expertRepository);
        }

        @Test
        @DisplayName("Unterschiedliche Modi haben unterschiedliche Cache-Keys")
        void shouldHaveDifferentCacheKeysForModes() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> withoutMode = factory.getRuntime(1L, null, false);
            Optional<ExpertRuntime> withMode = factory.getRuntime(1L, 10L, false);

            assertThat(withoutMode).isPresent();
            assertThat(withMode).isPresent();
            assertThat(withoutMode.get()).isNotSameAs(withMode.get());
        }

        @Test
        @DisplayName("CPU-Only hat eigenen Cache-Key")
        void shouldHaveDifferentCacheKeyForCpuOnly() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> gpuMode = factory.getRuntime(1L, null, false);
            Optional<ExpertRuntime> cpuMode = factory.getRuntime(1L, null, true);

            assertThat(gpuMode).isPresent();
            assertThat(cpuMode).isPresent();
            assertThat(gpuMode.get()).isNotSameAs(cpuMode.get());
            assertThat(gpuMode.get().getCpuOnly()).isFalse();
            assertThat(cpuMode.get().getCpuOnly()).isTrue();
        }

        @Test
        @DisplayName("clearCache() leert den Cache")
        void shouldClearCache() {
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> first = factory.getRuntime(1L, null, false);

            factory.clearCache();

            // Nach clearCache muss Repository erneut abgefragt werden
            Optional<ExpertRuntime> second = factory.getRuntime(1L, null, false);

            assertThat(first).isPresent();
            assertThat(second).isPresent();
            assertThat(first.get()).isNotSameAs(second.get());
        }

        @Test
        @DisplayName("clearCacheForExpert() leert nur Cache für bestimmten Expert")
        void shouldClearCacheForSpecificExpert() {
            Expert secondExpert = new Expert();
            secondExpert.setId(2L);
            secondExpert.setName("ZweiterExperte");
            secondExpert.setRole("Tester");
            secondExpert.setBaseModel("llama:latest");

            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));
            when(expertRepository.findById(2L)).thenReturn(Optional.of(secondExpert));
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            // Beide cachen
            Optional<ExpertRuntime> expert1First = factory.getRuntime(1L, null, false);
            Optional<ExpertRuntime> expert2First = factory.getRuntime(2L, null, false);

            // Nur Expert 1 aus Cache löschen
            factory.clearCacheForExpert(1L);

            // Repository-Mock zurücksetzen
            org.mockito.Mockito.reset(expertRepository);
            when(expertRepository.findById(1L)).thenReturn(Optional.of(testExpert));

            // Expert 2 sollte noch im Cache sein
            Optional<ExpertRuntime> expert2Second = factory.getRuntime(2L, null, false);
            assertThat(expert2First.get()).isSameAs(expert2Second.get());

            // Expert 1 muss neu geladen werden
            Optional<ExpertRuntime> expert1Second = factory.getRuntime(1L, null, false);
            assertThat(expert1First.get()).isNotSameAs(expert1Second.get());
        }
    }

    @Nested
    @DisplayName("createRuntime() ohne Caching")
    class CreateRuntimeTests {

        @Test
        @DisplayName("Erstellt Runtime ohne Caching")
        void shouldCreateRuntimeWithoutCaching() {
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> first = factory.createRuntime(testExpert, null, false);
            Optional<ExpertRuntime> second = factory.createRuntime(testExpert, null, false);

            assertThat(first).isPresent();
            assertThat(second).isPresent();
            // Sollten unterschiedliche Instanzen sein
            assertThat(first.get()).isNotSameAs(second.get());
        }

        @Test
        @DisplayName("Gibt empty für null Expert")
        void shouldReturnEmptyForNullExpert() {
            Optional<ExpertRuntime> result = factory.createRuntime(null, null, false);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Erstellt Runtime mit aktivem Modus")
        void shouldCreateRuntimeWithActiveMode() {
            when(ollamaProvider.isAvailable()).thenReturn(true);
            when(ollamaProvider.getProviderName()).thenReturn("ollama");
            when(llmConfig.getDefaultProvider()).thenReturn("ollama");
            when(pathsConfig.getResolvedModelsDir()).thenReturn(tempDir);

            Optional<ExpertRuntime> result = factory.createRuntime(testExpert, testMode, false);

            assertThat(result).isPresent();
            assertThat(result.get().getActiveModeName()).isEqualTo("Analytisch");
            assertThat(result.get().getTemperature()).isEqualTo(0.3);
        }
    }
}
