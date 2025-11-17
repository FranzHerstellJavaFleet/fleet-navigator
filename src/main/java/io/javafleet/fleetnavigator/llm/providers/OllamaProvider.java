package io.javafleet.fleetnavigator.llm.providers;

import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * DEPRECATED: Ollama Provider wurde entfernt.
 * Diese Klasse existiert nur noch als Stub um Build-Fehler zu vermeiden.
 * Wird in zukünftigen Versionen komplett gelöscht.
 *
 * @deprecated Ollama Support wurde entfernt. Verwende java-llama-cpp stattdessen.
 */
@Deprecated
@Component
@Slf4j
public class OllamaProvider implements LLMProvider {

    public OllamaProvider(LLMConfigProperties config) {
        log.warn("⚠️ OllamaProvider is DEPRECATED and will be removed. Use java-llama-cpp instead.");
    }

    @Override
    public String getProviderName() {
        return "ollama-deprecated";
    }

    @Override
    public boolean isAvailable() {
        return false; // Always unavailable
    }

    @Override
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        throw new UnsupportedOperationException("Ollama provider has been removed. Use java-llama-cpp.");
    }

    @Override
    public void chatStream(String model, String prompt, String systemPrompt, String requestId,
                          Consumer<String> chunkConsumer, Integer maxTokens, Double temperature,
                          Double topP, Integer topK, Double repeatPenalty) throws IOException {
        throw new UnsupportedOperationException("Ollama provider has been removed. Use java-llama-cpp.");
    }

    @Override
    public String chatWithVision(String model, String prompt, List<String> images,
                                 String systemPrompt, String requestId) throws IOException {
        throw new UnsupportedOperationException("Ollama provider has been removed. Use java-llama-cpp.");
    }

    @Override
    public void chatStreamWithVision(String model, String prompt, List<String> images,
                                     String systemPrompt, String requestId,
                                     Consumer<String> chunkConsumer) throws IOException {
        throw new UnsupportedOperationException("Ollama provider has been removed. Use java-llama-cpp.");
    }

    @Override
    public List<ModelInfo> getAvailableModels() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
        throw new UnsupportedOperationException("Ollama provider has been removed. Use java-llama-cpp.");
    }

    @Override
    public boolean deleteModel(String modelName) throws IOException {
        return false;
    }

    @Override
    public Map<String, Object> getModelDetails(String modelName) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public void createModel(String modelName, String baseModel, String systemPrompt,
                           Double temperature, Double topP, Integer topK,
                           Double repeatPenalty, Consumer<String> progressConsumer) throws IOException {
        throw new UnsupportedOperationException("Ollama provider has been removed. Use java-llama-cpp.");
    }

    @Override
    public boolean cancelRequest(String requestId) {
        return false;
    }

    @Override
    public int estimateTokens(String text) {
        return text.length() / 4;
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        return Collections.emptySet();
    }
}
