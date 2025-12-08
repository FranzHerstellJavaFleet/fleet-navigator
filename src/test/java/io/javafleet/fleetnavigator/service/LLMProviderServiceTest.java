package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.llm.providers.JavaLlamaCppProvider;
import io.javafleet.fleetnavigator.llm.providers.LlamaCppProvider;
import io.javafleet.fleetnavigator.llm.providers.OllamaProvider;
import io.javafleet.fleetnavigator.llm.providers.ExternalLlamaServerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit-Tests für LLMProviderService
 *
 * Testet:
 * - Provider-Umschaltung (java-llama-cpp, llamacpp, ollama)
 * - Provider-Erkennung mit Prioritäten
 * - Provider-Isolation (Anfragen gehen an korrekten Provider)
 * - Fehlerbehandlung
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LLMProviderServiceTest {

    @Mock
    private LlamaCppProvider llamaCppProvider;

    @Mock
    private JavaLlamaCppProvider javaLlamaCppProvider;

    @Mock
    private OllamaProvider ollamaProvider;

    @Mock
    private ExternalLlamaServerProvider externalLlamaServerProvider;

    @Mock
    private LLMConfigProperties config;

    @Mock
    private SettingsService settingsService;

    private LLMProviderService service;

    @BeforeEach
    void setUp() {
        // Default setup: alle Provider verfügbar
        when(javaLlamaCppProvider.getProviderName()).thenReturn("java-llama-cpp");
        when(llamaCppProvider.getProviderName()).thenReturn("llamacpp");
        when(ollamaProvider.getProviderName()).thenReturn("ollama");
        when(externalLlamaServerProvider.getProviderName()).thenReturn("llama-server");

        when(javaLlamaCppProvider.isAvailable()).thenReturn(true);
        when(llamaCppProvider.isAvailable()).thenReturn(true);
        when(ollamaProvider.isAvailable()).thenReturn(true);
        when(externalLlamaServerProvider.isAvailable()).thenReturn(false); // Not available by default

        when(config.getDefaultProvider()).thenReturn("auto");
        when(settingsService.getActiveProvider()).thenReturn(null);
    }

    private void createService() {
        service = new LLMProviderService(
            llamaCppProvider,
            javaLlamaCppProvider,
            ollamaProvider,
            externalLlamaServerProvider,
            config,
            settingsService
        );
    }

    // ===== Provider Detection Tests =====

    @Nested
    @DisplayName("Provider Detection (detectActiveProvider)")
    class ProviderDetectionTests {

        @Test
        @DisplayName("Priorität 1: Gespeicherter Provider aus DB wird bevorzugt")
        void detectActiveProvider_savedProviderHasHighestPriority() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn("ollama");

            // When
            createService();

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("ollama");
        }

        @Test
        @DisplayName("Priorität 2: Config Default wenn kein gespeicherter Provider")
        void detectActiveProvider_configDefaultUsedWhenNoSavedProvider() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn(null);
            when(config.getDefaultProvider()).thenReturn("llamacpp");

            // When
            createService();

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("llamacpp");
        }

        @Test
        @DisplayName("Priorität 3: java-llama-cpp als Standard (auto mode)")
        void detectActiveProvider_javaLlamaCppDefaultInAutoMode() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn(null);
            when(config.getDefaultProvider()).thenReturn("auto");

            // When
            createService();

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("java-llama-cpp");
        }

        @Test
        @DisplayName("Priorität 4: llamacpp Fallback wenn java-llama-cpp nicht verfügbar")
        void detectActiveProvider_llamacppFallbackWhenJavaLlamaCppUnavailable() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn(null);
            when(config.getDefaultProvider()).thenReturn("auto");
            when(javaLlamaCppProvider.isAvailable()).thenReturn(false);

            // When
            createService();

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("llamacpp");
        }

        @Test
        @DisplayName("Priorität 5: Erster verfügbarer Provider als letzter Fallback")
        void detectActiveProvider_fallbackToFirstAvailable() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn(null);
            when(config.getDefaultProvider()).thenReturn("auto");
            when(javaLlamaCppProvider.isAvailable()).thenReturn(false);
            when(llamaCppProvider.isAvailable()).thenReturn(false);
            // ollama ist noch verfügbar

            // When
            createService();

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("ollama");
        }

        @Test
        @DisplayName("Exception wenn kein Provider verfügbar")
        void detectActiveProvider_throwsExceptionWhenNoProviderAvailable() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn(null);
            when(config.getDefaultProvider()).thenReturn("auto");
            when(javaLlamaCppProvider.isAvailable()).thenReturn(false);
            when(llamaCppProvider.isAvailable()).thenReturn(false);
            when(ollamaProvider.isAvailable()).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> createService())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No LLM provider available");
        }

        @Test
        @DisplayName("Gespeicherter Provider nicht verfügbar → Fallback zu Auto-Detection")
        void detectActiveProvider_savedProviderUnavailableFallsBack() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn("ollama");
            when(ollamaProvider.isAvailable()).thenReturn(false);
            when(config.getDefaultProvider()).thenReturn("auto");

            // When
            createService();

            // Then - sollte auf java-llama-cpp fallen (nächste Priorität)
            assertThat(service.getActiveProviderName()).isEqualTo("java-llama-cpp");
        }

        @Test
        @DisplayName("Config Provider nicht verfügbar → Fallback zu java-llama-cpp")
        void detectActiveProvider_configProviderUnavailableFallsBack() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn(null);
            when(config.getDefaultProvider()).thenReturn("ollama");
            when(ollamaProvider.isAvailable()).thenReturn(false);

            // When
            createService();

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("java-llama-cpp");
        }
    }

    // ===== Provider Switching Tests =====

    @Nested
    @DisplayName("Provider Switching (switchProvider)")
    class ProviderSwitchingTests {

        @BeforeEach
        void setUpService() {
            createService();
        }

        @Test
        @DisplayName("Wechsel von java-llama-cpp zu ollama")
        void switchProvider_fromJavaLlamaCppToOllama() {
            // Given - Default ist java-llama-cpp
            assertThat(service.getActiveProviderName()).isEqualTo("java-llama-cpp");

            // When
            service.switchProvider("ollama");

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("ollama");
            verify(settingsService).saveActiveProvider("ollama");
        }

        @Test
        @DisplayName("Wechsel von java-llama-cpp zu llamacpp (Server)")
        void switchProvider_fromJavaLlamaCppToLlamaCppServer() {
            // When
            service.switchProvider("llamacpp");

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("llamacpp");
            verify(settingsService).saveActiveProvider("llamacpp");
        }

        @Test
        @DisplayName("Wechsel zu java-llama-cpp")
        void switchProvider_toJavaLlamaCpp() {
            // Given - erst zu ollama wechseln
            service.switchProvider("ollama");

            // When - zurück zu java-llama-cpp
            service.switchProvider("java-llama-cpp");

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("java-llama-cpp");
            verify(settingsService).saveActiveProvider("java-llama-cpp");
        }

        @Test
        @DisplayName("Case-insensitive Provider-Name")
        void switchProvider_caseInsensitive() {
            // When
            service.switchProvider("OLLAMA");

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("ollama");
        }

        @Test
        @DisplayName("IllegalArgumentException bei unbekanntem Provider")
        void switchProvider_unknownProviderThrowsException() {
            assertThatThrownBy(() -> service.switchProvider("unknown-provider"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown provider");
        }

        @Test
        @DisplayName("IllegalStateException wenn Provider nicht verfügbar")
        void switchProvider_unavailableProviderThrowsException() {
            // Given
            when(ollamaProvider.isAvailable()).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> service.switchProvider("ollama"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Provider not available");
        }

        @Test
        @DisplayName("Persistierung in Datenbank bei jedem Wechsel")
        void switchProvider_persistsToDatabaseOnEverySwitch() {
            // When - mehrere Wechsel
            service.switchProvider("ollama");
            service.switchProvider("llamacpp");
            service.switchProvider("java-llama-cpp");

            // Then
            verify(settingsService).saveActiveProvider("ollama");
            verify(settingsService).saveActiveProvider("llamacpp");
            verify(settingsService).saveActiveProvider("java-llama-cpp");
        }
    }

    // ===== Provider Isolation Tests =====

    @Nested
    @DisplayName("Provider Isolation (Chat-Anfragen)")
    class ProviderIsolationTests {

        @BeforeEach
        void setUpService() {
            createService();
        }

        @Test
        @DisplayName("Chat-Anfrage geht an aktiven Provider (java-llama-cpp)")
        void chat_goesToActiveProvider_javaLlamaCpp() throws IOException {
            // Given
            when(javaLlamaCppProvider.chat(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Response from java-llama-cpp");

            // When
            String response = service.chat("model", "prompt", "system", "req-1");

            // Then
            verify(javaLlamaCppProvider).chat("model", "prompt", "system", "req-1");
            verify(llamaCppProvider, never()).chat(any(), any(), any(), any());
            verify(ollamaProvider, never()).chat(any(), any(), any(), any());
            assertThat(response).isEqualTo("Response from java-llama-cpp");
        }

        @Test
        @DisplayName("Chat-Anfrage geht an ollama nach Wechsel")
        void chat_goesToOllamaAfterSwitch() throws IOException {
            // Given
            service.switchProvider("ollama");
            when(ollamaProvider.chat(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Response from ollama");

            // When
            String response = service.chat("model", "prompt", "system", "req-1");

            // Then
            verify(ollamaProvider).chat("model", "prompt", "system", "req-1");
            verify(javaLlamaCppProvider, never()).chat(any(), any(), any(), any());
            verify(llamaCppProvider, never()).chat(any(), any(), any(), any());
            assertThat(response).isEqualTo("Response from ollama");
        }

        @Test
        @DisplayName("Chat-Anfrage geht an llamacpp (Server) nach Wechsel")
        void chat_goesToLlamaCppServerAfterSwitch() throws IOException {
            // Given
            service.switchProvider("llamacpp");
            when(llamaCppProvider.chat(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Response from llamacpp server");

            // When
            String response = service.chat("model", "prompt", "system", "req-1");

            // Then
            verify(llamaCppProvider).chat("model", "prompt", "system", "req-1");
            verify(javaLlamaCppProvider, never()).chat(any(), any(), any(), any());
            verify(ollamaProvider, never()).chat(any(), any(), any(), any());
            assertThat(response).isEqualTo("Response from llamacpp server");
        }

        @Test
        @DisplayName("Streaming geht an aktiven Provider")
        void chatStream_goesToActiveProvider() throws IOException {
            // Given
            Consumer<String> consumer = chunk -> {};

            // When
            service.chatStream("model", "prompt", "system", "req-1", consumer,
                100, 0.7, 0.9, 40, 1.1, 4096);

            // Then
            verify(javaLlamaCppProvider).chatStream(
                eq("model"), eq("prompt"), eq("system"), eq("req-1"),
                eq(consumer), eq(100), eq(0.7), eq(0.9), eq(40), eq(1.1), eq(4096), eq(false)
            );
        }

        @Test
        @DisplayName("Streaming mit CPU-Only geht an aktiven Provider")
        void chatStream_cpuOnlyGoesToActiveProvider() throws IOException {
            // Given
            Consumer<String> consumer = chunk -> {};

            // When
            service.chatStream("model", "prompt", "system", "req-1", consumer,
                100, 0.7, 0.9, 40, 1.1, 4096, true);

            // Then
            verify(javaLlamaCppProvider).chatStream(
                eq("model"), eq("prompt"), eq("system"), eq("req-1"),
                eq(consumer), eq(100), eq(0.7), eq(0.9), eq(40), eq(1.1), eq(4096), eq(true)
            );
        }

        @Test
        @DisplayName("Cancel Request geht an aktiven Provider")
        void cancelRequest_goesToActiveProvider() {
            // Given
            when(javaLlamaCppProvider.cancelRequest("req-1")).thenReturn(true);

            // When
            boolean result = service.cancelRequest("req-1");

            // Then
            verify(javaLlamaCppProvider).cancelRequest("req-1");
            assertThat(result).isTrue();
        }
    }

    // ===== Provider Status Tests =====

    @Nested
    @DisplayName("Provider Status")
    class ProviderStatusTests {

        @BeforeEach
        void setUpService() {
            createService();
        }

        @Test
        @DisplayName("getProviderStatus gibt alle Provider mit Status zurück")
        void getProviderStatus_returnsAllProviders() {
            // When
            Map<String, Boolean> status = service.getProviderStatus();

            // Then - 4 Provider: java-llama-cpp, llamacpp, ollama, llama-server
            assertThat(status).hasSize(4);
            assertThat(status.get("java-llama-cpp")).isTrue();
            assertThat(status.get("llamacpp")).isTrue();
            assertThat(status.get("ollama")).isTrue();
            assertThat(status).containsKey("llama-server");
        }

        @Test
        @DisplayName("getProviderStatus zeigt nicht verfügbare Provider")
        void getProviderStatus_showsUnavailableProviders() {
            // Given
            when(ollamaProvider.isAvailable()).thenReturn(false);

            // When
            Map<String, Boolean> status = service.getProviderStatus();

            // Then
            assertThat(status.get("java-llama-cpp")).isTrue();
            assertThat(status.get("llamacpp")).isTrue();
            assertThat(status.get("ollama")).isFalse();
        }

        @Test
        @DisplayName("getAvailableProviders gibt nur verfügbare zurück")
        void getAvailableProviders_returnsOnlyAvailable() {
            // Given
            when(llamaCppProvider.isAvailable()).thenReturn(false);

            // When
            List<String> available = service.getAvailableProviders();

            // Then
            assertThat(available).containsExactlyInAnyOrder("java-llama-cpp", "ollama");
            assertThat(available).doesNotContain("llamacpp");
        }

        @Test
        @DisplayName("isAnyProviderAvailable true wenn mindestens einer verfügbar")
        void isAnyProviderAvailable_trueIfOneAvailable() {
            // Given
            when(javaLlamaCppProvider.isAvailable()).thenReturn(false);
            when(llamaCppProvider.isAvailable()).thenReturn(false);
            // ollama noch verfügbar

            // When
            boolean result = service.isAnyProviderAvailable();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAnyProviderAvailable false wenn keiner verfügbar")
        void isAnyProviderAvailable_falseIfNoneAvailable() {
            // Given - muss Service mit verfügbaren Providern erstellen, dann deaktivieren
            // (da Constructor Exception wirft wenn keiner verfügbar)
            createService();

            // Jetzt alle deaktivieren
            when(javaLlamaCppProvider.isAvailable()).thenReturn(false);
            when(llamaCppProvider.isAvailable()).thenReturn(false);
            when(ollamaProvider.isAvailable()).thenReturn(false);

            // When
            boolean result = service.isAnyProviderAvailable();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("getProvider gibt korrekten Provider zurück")
        void getProvider_returnsCorrectProvider() {
            // When/Then
            assertThat(service.getProvider("java-llama-cpp")).isEqualTo(javaLlamaCppProvider);
            assertThat(service.getProvider("llamacpp")).isEqualTo(llamaCppProvider);
            assertThat(service.getProvider("ollama")).isEqualTo(ollamaProvider);
        }

        @Test
        @DisplayName("getProvider case-insensitive")
        void getProvider_caseInsensitive() {
            assertThat(service.getProvider("JAVA-LLAMA-CPP")).isEqualTo(javaLlamaCppProvider);
            assertThat(service.getProvider("Ollama")).isEqualTo(ollamaProvider);
        }

        @Test
        @DisplayName("getProvider gibt null bei unbekanntem Provider")
        void getProvider_nullForUnknown() {
            assertThat(service.getProvider("unknown")).isNull();
        }
    }

    // ===== Model Management Tests =====

    @Nested
    @DisplayName("Model Management")
    class ModelManagementTests {

        @BeforeEach
        void setUpService() {
            createService();
        }

        @Test
        @DisplayName("getAvailableModels vom aktiven Provider")
        void getAvailableModels_fromActiveProvider() throws IOException {
            // Given
            List<ModelInfo> models = List.of(
                ModelInfo.builder().name("model1").size(4096L).displayName("Model 1").build(),
                ModelInfo.builder().name("model2").size(8192L).displayName("Model 2").build()
            );
            when(javaLlamaCppProvider.getAvailableModels()).thenReturn(models);

            // When
            List<ModelInfo> result = service.getAvailableModels();

            // Then
            assertThat(result).hasSize(2);
            verify(javaLlamaCppProvider).getAvailableModels();
        }

        @Test
        @DisplayName("getAllModels von allen verfügbaren Providern")
        void getAllModels_fromAllProviders() throws IOException {
            // Given
            when(javaLlamaCppProvider.getAvailableModels()).thenReturn(
                List.of(ModelInfo.builder().name("jlc-model").size(4096L).displayName("JLC Model").build())
            );
            when(llamaCppProvider.getAvailableModels()).thenReturn(
                List.of(ModelInfo.builder().name("lc-model").size(4096L).displayName("LC Model").build())
            );
            when(ollamaProvider.getAvailableModels()).thenReturn(
                List.of(ModelInfo.builder().name("ollama-model").size(4096L).displayName("Ollama Model").build())
            );

            // When
            List<ModelInfo> result = service.getAllModels();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting("name")
                .containsExactlyInAnyOrder("jlc-model", "lc-model", "ollama-model");
        }

        @Test
        @DisplayName("getAllModels ignoriert nicht verfügbare Provider")
        void getAllModels_ignoresUnavailableProviders() throws IOException {
            // Given
            when(llamaCppProvider.isAvailable()).thenReturn(false);
            when(javaLlamaCppProvider.getAvailableModels()).thenReturn(
                List.of(ModelInfo.builder().name("jlc-model").size(4096L).displayName("JLC Model").build())
            );
            when(ollamaProvider.getAvailableModels()).thenReturn(
                List.of(ModelInfo.builder().name("ollama-model").size(4096L).displayName("Ollama Model").build())
            );

            // When
            List<ModelInfo> result = service.getAllModels();

            // Then
            assertThat(result).hasSize(2);
            verify(llamaCppProvider, never()).getAvailableModels();
        }

        @Test
        @DisplayName("getAllModels fängt IOException von einzelnen Providern ab")
        void getAllModels_handlesProviderExceptions() throws IOException {
            // Given
            when(javaLlamaCppProvider.getAvailableModels()).thenReturn(
                List.of(ModelInfo.builder().name("jlc-model").size(4096L).displayName("JLC Model").build())
            );
            when(llamaCppProvider.getAvailableModels()).thenThrow(new IOException("Connection failed"));
            when(ollamaProvider.getAvailableModels()).thenReturn(
                List.of(ModelInfo.builder().name("ollama-model").size(4096L).displayName("Ollama Model").build())
            );

            // When
            List<ModelInfo> result = service.getAllModels();

            // Then - sollte 2 Modelle zurückgeben, nicht abstürzen
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("getDefaultModelWithFallback bevorzugt angegebenes Modell")
        void getDefaultModelWithFallback_prefersGivenModel() throws IOException {
            // Given
            List<ModelInfo> models = List.of(
                ModelInfo.builder().name("model1").size(4096L).displayName("Model 1").build(),
                ModelInfo.builder().name("preferred-model").size(8192L).displayName("Preferred").build()
            );
            when(javaLlamaCppProvider.getAvailableModels()).thenReturn(models);

            // When
            String result = service.getDefaultModelWithFallback("preferred-model");

            // Then
            assertThat(result).isEqualTo("preferred-model");
        }

        @Test
        @DisplayName("getDefaultModelWithFallback Fallback auf erstes Modell")
        void getDefaultModelWithFallback_fallsBackToFirst() throws IOException {
            // Given
            List<ModelInfo> models = List.of(
                ModelInfo.builder().name("first-model").size(4096L).displayName("First").build(),
                ModelInfo.builder().name("second-model").size(8192L).displayName("Second").build()
            );
            when(javaLlamaCppProvider.getAvailableModels()).thenReturn(models);

            // When
            String result = service.getDefaultModelWithFallback("non-existent");

            // Then
            assertThat(result).isEqualTo("first-model");
        }

        @Test
        @DisplayName("getDefaultModelWithFallback null wenn keine Modelle")
        void getDefaultModelWithFallback_nullWhenNoModels() throws IOException {
            // Given
            when(javaLlamaCppProvider.getAvailableModels()).thenReturn(List.of());

            // When
            String result = service.getDefaultModelWithFallback("any");

            // Then
            assertThat(result).isNull();
        }
    }

    // ===== Provider-spezifische Delegation Tests =====

    @Nested
    @DisplayName("Provider-spezifische Operationen")
    class ProviderSpecificOperationsTests {

        @BeforeEach
        void setUpService() {
            createService();
        }

        @Test
        @DisplayName("pullModel delegiert an aktiven Provider")
        void pullModel_delegatesToActiveProvider() throws IOException {
            // Given
            Consumer<String> progressConsumer = progress -> {};

            // When
            service.pullModel("new-model", progressConsumer);

            // Then
            verify(javaLlamaCppProvider).pullModel("new-model", progressConsumer);
        }

        @Test
        @DisplayName("deleteModel delegiert an aktiven Provider")
        void deleteModel_delegatesToActiveProvider() throws IOException {
            // Given
            when(javaLlamaCppProvider.deleteModel("old-model")).thenReturn(true);

            // When
            boolean result = service.deleteModel("old-model");

            // Then
            assertThat(result).isTrue();
            verify(javaLlamaCppProvider).deleteModel("old-model");
        }

        @Test
        @DisplayName("getModelDetails delegiert an aktiven Provider")
        void getModelDetails_delegatesToActiveProvider() throws IOException {
            // Given
            Map<String, Object> details = Map.of("size", 4096, "format", "gguf");
            when(javaLlamaCppProvider.getModelDetails("model")).thenReturn(details);

            // When
            Map<String, Object> result = service.getModelDetails("model");

            // Then
            assertThat(result).isEqualTo(details);
            verify(javaLlamaCppProvider).getModelDetails("model");
        }

        @Test
        @DisplayName("createModel delegiert an aktiven Provider")
        void createModel_delegatesToActiveProvider() throws IOException {
            // Given
            Consumer<String> progressConsumer = progress -> {};

            // When
            service.createModel("custom-model", "base-model", "system prompt",
                0.7, 0.9, 40, 1.1, progressConsumer);

            // Then
            verify(javaLlamaCppProvider).createModel(
                "custom-model", "base-model", "system prompt",
                0.7, 0.9, 40, 1.1, progressConsumer
            );
        }

        @Test
        @DisplayName("estimateTokens delegiert an aktiven Provider")
        void estimateTokens_delegatesToActiveProvider() {
            // Given
            when(javaLlamaCppProvider.estimateTokens("Test text")).thenReturn(2);

            // When
            int result = service.estimateTokens("Test text");

            // Then
            assertThat(result).isEqualTo(2);
            verify(javaLlamaCppProvider).estimateTokens("Test text");
        }
    }

    // ===== Vision Tests =====

    @Nested
    @DisplayName("Vision Support")
    class VisionSupportTests {

        @BeforeEach
        void setUpService() {
            createService();
        }

        @Test
        @DisplayName("chatWithVision delegiert an aktiven Provider")
        void chatWithVision_delegatesToActiveProvider() throws IOException {
            // Given
            List<String> images = List.of("base64image1", "base64image2");
            when(javaLlamaCppProvider.chatWithVision(any(), any(), any(), any(), any()))
                .thenReturn("Vision response");

            // When
            String result = service.chatWithVision("llava", "Describe this", images, "system", "req-1");

            // Then
            assertThat(result).isEqualTo("Vision response");
            verify(javaLlamaCppProvider).chatWithVision("llava", "Describe this", images, "system", "req-1");
        }

        @Test
        @DisplayName("chatStreamWithVision delegiert an aktiven Provider")
        void chatStreamWithVision_delegatesToActiveProvider() throws IOException {
            // Given
            List<String> images = List.of("base64image");
            Consumer<String> consumer = chunk -> {};

            // When
            service.chatStreamWithVision("llava", "Describe", images, "system", "req-1", consumer);

            // Then
            verify(javaLlamaCppProvider).chatStreamWithVision("llava", "Describe", images, "system", "req-1", consumer);
        }
    }

    // ===== llama-server Provider Tests =====

    @Nested
    @DisplayName("llama-server Provider (für FleetCode)")
    class LlamaServerProviderTests {

        @BeforeEach
        void setUpWithLlamaServer() {
            // llama-server verfügbar machen
            when(externalLlamaServerProvider.isAvailable()).thenReturn(true);
            createService();
        }

        @Test
        @DisplayName("llama-server ist in Provider-Status enthalten")
        void llamaServerInProviderStatus() {
            // When
            Map<String, Boolean> status = service.getProviderStatus();

            // Then
            assertThat(status).containsKey("llama-server");
            assertThat(status.get("llama-server")).isTrue();
        }

        @Test
        @DisplayName("Wechsel zu llama-server Provider")
        void switchToLlamaServer() {
            // When
            service.switchProvider("llama-server");

            // Then
            assertThat(service.getActiveProviderName()).isEqualTo("llama-server");
            verify(settingsService).saveActiveProvider("llama-server");
        }

        @Test
        @DisplayName("Chat-Anfrage geht an llama-server nach Wechsel")
        void chatGoesToLlamaServerAfterSwitch() throws IOException {
            // Given
            service.switchProvider("llama-server");
            when(externalLlamaServerProvider.chat(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Response from llama-server");

            // When
            String response = service.chat("model", "prompt", "system", "req-1");

            // Then
            verify(externalLlamaServerProvider).chat("model", "prompt", "system", "req-1");
            verify(javaLlamaCppProvider, never()).chat(any(), any(), any(), any());
            assertThat(response).isEqualTo("Response from llama-server");
        }

        @Test
        @DisplayName("llama-server erscheint in getAvailableProviders wenn verfügbar")
        void llamaServerInAvailableProviders() {
            // When
            List<String> available = service.getAvailableProviders();

            // Then
            assertThat(available).contains("llama-server");
        }

        @Test
        @DisplayName("llama-server erscheint NICHT in getAvailableProviders wenn nicht verfügbar")
        void llamaServerNotInAvailableProvidersWhenUnavailable() {
            // Given
            when(externalLlamaServerProvider.isAvailable()).thenReturn(false);

            // When
            List<String> available = service.getAvailableProviders();

            // Then
            assertThat(available).doesNotContain("llama-server");
        }

        @Test
        @DisplayName("Gespeicherter llama-server Provider wird beim Start geladen")
        void savedLlamaServerProviderIsLoadedOnStartup() {
            // Given - Setup mit gespeichertem llama-server
            when(settingsService.getActiveProvider()).thenReturn("llama-server");
            when(externalLlamaServerProvider.isAvailable()).thenReturn(true);

            // When
            LLMProviderService newService = new LLMProviderService(
                llamaCppProvider,
                javaLlamaCppProvider,
                ollamaProvider,
                externalLlamaServerProvider,
                config,
                settingsService
            );

            // Then
            assertThat(newService.getActiveProviderName()).isEqualTo("llama-server");
        }

        @Test
        @DisplayName("Fallback zu java-llama-cpp wenn gespeicherter llama-server nicht verfügbar")
        void fallbackFromSavedLlamaServerWhenUnavailable() {
            // Given
            when(settingsService.getActiveProvider()).thenReturn("llama-server");
            when(externalLlamaServerProvider.isAvailable()).thenReturn(false);
            when(config.getDefaultProvider()).thenReturn("auto");

            // When
            LLMProviderService newService = new LLMProviderService(
                llamaCppProvider,
                javaLlamaCppProvider,
                ollamaProvider,
                externalLlamaServerProvider,
                config,
                settingsService
            );

            // Then - Fallback zu java-llama-cpp
            assertThat(newService.getActiveProviderName()).isEqualTo("java-llama-cpp");
        }

        @Test
        @DisplayName("getProvider gibt llama-server Provider zurück")
        void getProviderReturnsLlamaServer() {
            // When/Then
            assertThat(service.getProvider("llama-server")).isEqualTo(externalLlamaServerProvider);
        }

        @Test
        @DisplayName("Streaming-Anfrage geht an llama-server")
        void streamingGoesToLlamaServer() throws IOException {
            // Given
            service.switchProvider("llama-server");
            Consumer<String> consumer = chunk -> {};

            // When
            service.chatStream("model", "prompt", "system", "req-1", consumer,
                100, 0.7, 0.9, 40, 1.1, 4096);

            // Then
            verify(externalLlamaServerProvider).chatStream(
                eq("model"), eq("prompt"), eq("system"), eq("req-1"),
                eq(consumer), eq(100), eq(0.7), eq(0.9), eq(40), eq(1.1), eq(4096), eq(false)
            );
        }
    }

    // ===== Sequentielle Provider-Wechsel Tests =====

    @Nested
    @DisplayName("Sequentielle Provider-Wechsel")
    class SequentialProviderSwitchTests {

        @BeforeEach
        void setUpService() {
            createService();
        }

        @Test
        @DisplayName("Mehrfache Wechsel zwischen allen Providern")
        void multipleSwitchesBetweenAllProviders() throws IOException {
            // Given
            when(javaLlamaCppProvider.chat(any(), any(), any(), any())).thenReturn("JLC response");
            when(ollamaProvider.chat(any(), any(), any(), any())).thenReturn("Ollama response");
            when(llamaCppProvider.chat(any(), any(), any(), any())).thenReturn("LC response");

            // When - Round-trip durch alle Provider
            assertThat(service.chat("m", "p", "s", "1")).isEqualTo("JLC response");

            service.switchProvider("ollama");
            assertThat(service.chat("m", "p", "s", "2")).isEqualTo("Ollama response");

            service.switchProvider("llamacpp");
            assertThat(service.chat("m", "p", "s", "3")).isEqualTo("LC response");

            service.switchProvider("java-llama-cpp");
            assertThat(service.chat("m", "p", "s", "4")).isEqualTo("JLC response");

            // Then - alle Provider wurden korrekt aufgerufen
            verify(javaLlamaCppProvider, times(2)).chat(any(), any(), any(), any());
            verify(ollamaProvider, times(1)).chat(any(), any(), any(), any());
            verify(llamaCppProvider, times(1)).chat(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Wechsel wird korrekt in DB persistiert")
        void switchesPersistCorrectlyToDatabase() {
            // When
            service.switchProvider("ollama");
            service.switchProvider("llamacpp");
            service.switchProvider("java-llama-cpp");
            service.switchProvider("ollama");

            // Then
            verify(settingsService, times(2)).saveActiveProvider("ollama");
            verify(settingsService, times(1)).saveActiveProvider("llamacpp");
            verify(settingsService, times(1)).saveActiveProvider("java-llama-cpp");
        }
    }
}
