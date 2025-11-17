package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.ModelInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * DEPRECATED: OllamaService wurde entfernt.
 * Diese Klasse existiert nur noch als Stub um Build-Fehler zu vermeiden.
 * Wird in zukünftigen Versionen komplett gelöscht.
 *
 * @deprecated Ollama Support wurde entfernt. Verwende LLMProviderService mit java-llama-cpp stattdessen.
 */
@Deprecated
@Service
@Slf4j
public class OllamaService {

    public OllamaService() {
        log.warn("⚠️ OllamaService is DEPRECATED and will be removed. Use LLMProviderService instead.");
    }

    /**
     * @deprecated Use LLMProviderService.chat() instead
     */
    @Deprecated
    public String chat(String model, String prompt, String systemPrompt, String requestId) throws IOException {
        throw new UnsupportedOperationException("OllamaService has been removed. Use LLMProviderService with java-llama-cpp.");
    }

    /**
     * @deprecated Use LLMProviderService.getAvailableModels() instead
     */
    @Deprecated
    public List<ModelInfo> getAvailableModels() throws IOException {
        log.warn("OllamaService.getAvailableModels() is deprecated. Returning empty list.");
        return Collections.emptyList();
    }

    /**
     * @deprecated Use LLMProviderService.isAvailable() instead
     */
    @Deprecated
    public boolean isOllamaAvailable() {
        log.warn("OllamaService.isOllamaAvailable() is deprecated. Returning false.");
        return false;
    }

    /**
     * @deprecated Use LLMProviderService.createModel() instead
     */
    @Deprecated
    public void createModel(String modelName, String baseModel, String systemPrompt,
                           Double temperature, Double topP, Integer topK,
                           Double repeatPenalty, Consumer<String> progressConsumer) throws IOException {
        throw new UnsupportedOperationException("OllamaService has been removed. Use LLMProviderService with java-llama-cpp.");
    }

    /**
     * @deprecated Use LLMProviderService.deleteModel() instead
     */
    @Deprecated
    public boolean deleteModel(String modelName) throws IOException {
        log.warn("OllamaService.deleteModel() is deprecated. Returning false.");
        return false;
    }
}
