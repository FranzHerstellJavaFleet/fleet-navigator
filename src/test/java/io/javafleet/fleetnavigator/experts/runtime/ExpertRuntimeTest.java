package io.javafleet.fleetnavigator.experts.runtime;

import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests für ExpertRuntime.
 *
 * Testet die Runtime-Kapselung eines Experten mit:
 * - Korrekter Initialisierung aus Expert-Entity
 * - System-Prompt-Generierung mit/ohne Modus
 * - Parameter-Auflösung (Modus > Expert > Default)
 * - Chat-Delegation an Provider
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpertRuntime Tests")
class ExpertRuntimeTest {

    @Mock
    private LLMProvider mockProvider;

    private Expert testExpert;
    private ExpertMode testMode;
    private Path testModelPath;

    @BeforeEach
    void setUp() {
        // Test-Expert erstellen
        testExpert = new Expert();
        testExpert.setId(1L);
        testExpert.setName("TestExperte");
        testExpert.setRole("Testberater");
        testExpert.setAvatarUrl("/avatars/test.png");
        testExpert.setBaseModel("test-model:latest");
        testExpert.setGgufModel("test-model.Q4_K_M.gguf");
        testExpert.setBasePrompt("Du bist ein hilfreicher Test-Assistent.");
        testExpert.setPersonalityPrompt("Freundlich und präzise.");
        testExpert.setDefaultTemperature(0.8);
        testExpert.setDefaultTopP(0.95);
        testExpert.setDefaultMaxTokens(2048);
        testExpert.setDefaultNumCtx(4096);
        testExpert.setAutoWebSearch(true);
        testExpert.setSearchDomains("example.com,test.de");
        testExpert.setMaxSearchResults(5);
        testExpert.setAutoFileSearch(false);

        // Test-Modus erstellen
        testMode = new ExpertMode();
        testMode.setId(10L);
        testMode.setName("Kritisch");
        testMode.setPromptAddition("Analysiere kritisch und hinterfrage Annahmen.");
        testMode.setTemperature(0.5);
        testMode.setTopP(0.8);

        // Mock Provider-Name
        when(mockProvider.getProviderName()).thenReturn("java-llama-cpp");

        testModelPath = Path.of("/opt/models/test-model.Q4_K_M.gguf");
    }

    @Nested
    @DisplayName("Initialisierung")
    class InitializationTests {

        @Test
        @DisplayName("Expert-Attribute werden korrekt übernommen")
        void shouldCopyExpertAttributes() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            assertThat(runtime.getExpertId()).isEqualTo(1L);
            assertThat(runtime.getName()).isEqualTo("TestExperte");
            assertThat(runtime.getRole()).isEqualTo("Testberater");
            assertThat(runtime.getAvatarUrl()).isEqualTo("/avatars/test.png");
        }

        @Test
        @DisplayName("Provider und Modell werden korrekt gesetzt")
        void shouldSetProviderAndModel() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            assertThat(runtime.getProvider()).isEqualTo(mockProvider);
            assertThat(runtime.getProviderName()).isEqualTo("java-llama-cpp");
            assertThat(runtime.getModelName()).isEqualTo("test-model.Q4_K_M.gguf");
            assertThat(runtime.getResolvedModelPath()).isEqualTo(testModelPath);
        }

        @Test
        @DisplayName("CPU-Only Modus wird übernommen")
        void shouldSetCpuOnlyMode() {
            ExpertRuntime runtimeWithGpu = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);
            ExpertRuntime runtimeCpuOnly = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, true);

            assertThat(runtimeWithGpu.getCpuOnly()).isFalse();
            assertThat(runtimeCpuOnly.getCpuOnly()).isTrue();
        }

        @Test
        @DisplayName("Web-Search-Einstellungen werden übernommen")
        void shouldCopyWebSearchSettings() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            assertThat(runtime.getAutoWebSearch()).isTrue();
            assertThat(runtime.getSearchDomains()).containsExactly("example.com", "test.de");
            assertThat(runtime.getMaxSearchResults()).isEqualTo(5);
            assertThat(runtime.getAutoFileSearch()).isFalse();
        }
    }

    @Nested
    @DisplayName("System-Prompt-Generierung")
    class SystemPromptTests {

        @Test
        @DisplayName("System-Prompt enthält Basis-Prompt und Personality")
        void shouldCombineBasePromptAndPersonality() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            assertThat(runtime.getSystemPrompt())
                .contains("Du bist ein hilfreicher Test-Assistent")
                .contains("Freundlich und präzise");
        }

        @Test
        @DisplayName("Aktiver Modus wird im System-Prompt ergänzt")
        void shouldAppendModeToSystemPrompt() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, testMode, mockProvider, testModelPath, false);

            assertThat(runtime.getSystemPrompt())
                .contains("Kritisch")
                .contains("Analysiere kritisch und hinterfrage Annahmen");
            assertThat(runtime.getActiveModeName()).isEqualTo("Kritisch");
        }

        @Test
        @DisplayName("Ohne Modus bleibt activeModeName null")
        void shouldHaveNullModeNameWithoutMode() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            assertThat(runtime.getActiveModeName()).isNull();
        }
    }

    @Nested
    @DisplayName("Parameter-Auflösung")
    class ParameterResolutionTests {

        @Test
        @DisplayName("Expert-Parameter werden als Default verwendet")
        void shouldUseExpertParametersAsDefault() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            assertThat(runtime.getTemperature()).isEqualTo(0.8);
            assertThat(runtime.getTopP()).isEqualTo(0.95);
            assertThat(runtime.getMaxTokens()).isEqualTo(2048);
            assertThat(runtime.getContextSize()).isEqualTo(4096);
        }

        @Test
        @DisplayName("Modus-Parameter überschreiben Expert-Defaults")
        void shouldOverrideWithModeParameters() {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, testMode, mockProvider, testModelPath, false);

            // Modus-Werte überschreiben Expert-Defaults
            assertThat(runtime.getTemperature()).isEqualTo(0.5);
            assertThat(runtime.getTopP()).isEqualTo(0.8);

            // Expert-Werte bleiben wenn Modus keine hat
            assertThat(runtime.getMaxTokens()).isEqualTo(2048);
            assertThat(runtime.getContextSize()).isEqualTo(4096);
        }

        @Test
        @DisplayName("Fallback-Werte werden verwendet wenn Expert keine hat")
        void shouldUseFallbackValues() {
            Expert emptyExpert = new Expert();
            emptyExpert.setId(2L);
            emptyExpert.setName("EmptyExpert");
            emptyExpert.setRole("Test");
            emptyExpert.setBaseModel("model");

            ExpertRuntime runtime = new ExpertRuntime(emptyExpert, null, mockProvider, null, false);

            assertThat(runtime.getTemperature()).isEqualTo(0.7);  // Fallback
            assertThat(runtime.getTopP()).isEqualTo(0.9);         // Fallback
            assertThat(runtime.getMaxTokens()).isEqualTo(4096);   // Fallback
            assertThat(runtime.getContextSize()).isEqualTo(8192); // Fallback
        }
    }

    @Nested
    @DisplayName("Chat-Methoden")
    class ChatMethodTests {

        @Test
        @DisplayName("chatStream delegiert an Provider mit korrekten Parametern")
        void shouldDelegateChatStreamToProvider() throws IOException {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);
            Consumer<String> chunkConsumer = chunk -> {};

            String requestId = runtime.chatStream("Hallo", new ArrayList<>(), chunkConsumer);

            assertThat(requestId).isNotNull().isNotEmpty();
            verify(mockProvider).chatStream(
                eq(testModelPath.toString()),
                anyString(),
                anyString(),
                anyString(),
                eq(chunkConsumer),
                eq(2048),      // maxTokens
                eq(0.8),       // temperature
                eq(0.95),      // topP
                eq(40),        // topK
                eq(1.18),      // repeatPenalty
                eq(4096),      // contextSize
                eq(false)      // cpuOnly
            );
        }

        @Test
        @DisplayName("chat delegiert an Provider für blockierende Anfrage")
        void shouldDelegateChatToProvider() throws IOException {
            when(mockProvider.chat(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Test-Antwort");

            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            String response = runtime.chat("Test-Frage", new ArrayList<>());

            assertThat(response).isEqualTo("Test-Antwort");
            verify(mockProvider).chat(
                eq(testModelPath.toString()),
                anyString(),
                anyString(),
                anyString()
            );
        }

        @Test
        @DisplayName("cancelRequest wird an Provider weitergeleitet")
        void shouldDelegateCancelToProvider() {
            when(mockProvider.cancelRequest("req-123")).thenReturn(true);

            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            boolean result = runtime.cancelRequest("req-123");

            assertThat(result).isTrue();
            verify(mockProvider).cancelRequest("req-123");
        }
    }

    @Nested
    @DisplayName("Historie-Verarbeitung")
    class HistoryProcessingTests {

        @Test
        @DisplayName("Leere Historie wird korrekt behandelt")
        void shouldHandleEmptyHistory() throws IOException {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);
            Consumer<String> chunkConsumer = chunk -> {};

            runtime.chatStream("Neue Nachricht", null, chunkConsumer);
            runtime.chatStream("Neue Nachricht", new ArrayList<>(), chunkConsumer);

            // Keine Exception = Test bestanden
        }

        @Test
        @DisplayName("Historie-Nachrichten werden in Prompt integriert")
        void shouldIntegrateHistoryIntoPrompt() throws IOException {
            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);
            Consumer<String> chunkConsumer = chunk -> {};

            List<Message> history = new ArrayList<>();
            Message userMsg = new Message();
            userMsg.setRole(Message.MessageRole.USER);
            userMsg.setContent("Erste Frage");
            history.add(userMsg);

            Message assistantMsg = new Message();
            assistantMsg.setRole(Message.MessageRole.ASSISTANT);
            assistantMsg.setContent("Erste Antwort");
            history.add(assistantMsg);

            runtime.chatStream("Folgefrage", history, chunkConsumer);

            // Verifiziere dass Provider mit Prompt aufgerufen wurde der Historie enthält
            verify(mockProvider).chatStream(
                anyString(),
                org.mockito.ArgumentMatchers.contains("Erste Frage"),
                anyString(),
                anyString(),
                any(),
                anyInt(),
                anyDouble(),
                anyDouble(),
                anyInt(),
                anyDouble(),
                anyInt(),
                any()
            );
        }
    }

    @Nested
    @DisplayName("Modell-Name-Auflösung")
    class ModelNameResolutionTests {

        @Test
        @DisplayName("GGUF-Modell wird für java-llama-cpp bevorzugt")
        void shouldPreferGgufModelForJavaLlamaCpp() {
            when(mockProvider.getProviderName()).thenReturn("java-llama-cpp");

            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

            assertThat(runtime.getModelName()).isEqualTo("test-model.Q4_K_M.gguf");
        }

        @Test
        @DisplayName("BaseModel wird für Ollama verwendet")
        void shouldUseBaseModelForOllama() {
            when(mockProvider.getProviderName()).thenReturn("ollama");

            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, null, false);

            assertThat(runtime.getModelName()).isEqualTo("test-model:latest");
        }

        @Test
        @DisplayName("Fallback auf BaseModel wenn kein GGUF definiert")
        void shouldFallbackToBaseModelIfNoGguf() {
            testExpert.setGgufModel(null);
            when(mockProvider.getProviderName()).thenReturn("java-llama-cpp");

            ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, null, false);

            assertThat(runtime.getModelName()).isEqualTo("test-model:latest");
        }
    }

    @Test
    @DisplayName("toString() gibt formatierte Zusammenfassung")
    void shouldProvideFormattedToString() {
        ExpertRuntime runtime = new ExpertRuntime(testExpert, null, mockProvider, testModelPath, false);

        String result = runtime.toString();

        assertThat(result)
            .contains("TestExperte")
            .contains("Testberater")
            .contains("java-llama-cpp");
    }
}
