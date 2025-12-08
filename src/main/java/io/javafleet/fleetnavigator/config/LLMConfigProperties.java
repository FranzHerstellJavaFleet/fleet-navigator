package io.javafleet.fleetnavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM Provider Konfiguration
 *
 * Unterstützt llama.cpp (java-llama-cpp und llamacpp) und Ollama
 * Native Image: @ConfigurationProperties ist kompatibel
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.7
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMConfigProperties {

    /**
     * Default Provider (java-llama-cpp, llamacpp, oder ollama)
     * "auto" = Auto-Detection mit Fallback-Logik
     */
    private String defaultProvider = "java-llama-cpp";

    /**
     * llama.cpp-spezifische Konfiguration
     */
    private LlamaCppConfig llamacpp = new LlamaCppConfig();

    /**
     * Ollama-spezifische Konfiguration
     */
    private OllamaConfig ollama = new OllamaConfig();

    /**
     * llama.cpp Provider Config
     */
    @Data
    public static class LlamaCppConfig {
        /**
         * Pfad zum llama-server Binary
         */
        private String binaryPath = "./bin/llama-server";

        /**
         * Port für llama-server
         */
        private int port = 8080;

        /**
         * Verzeichnis für GGUF-Modelle
         */
        private String modelsDir = "./models";

        /**
         * Auto-Start llama-server beim Application-Start
         */
        private boolean autoStart = true;

        /**
         * Context Size (Anzahl Tokens)
         */
        private int contextSize = 4096;

        /**
         * GPU Layers (-1 = auto, 0 = CPU only, 999 = all layers)
         */
        private int gpuLayers = 999;

        /**
         * Threads für CPU Inference
         */
        private int threads = 0; // 0 = auto-detect

        /**
         * Aktiviert/Deaktiviert llama.cpp Provider
         */
        private boolean enabled = true;
    }

    /**
     * Ollama Provider Config
     */
    @Data
    public static class OllamaConfig {
        /**
         * Ollama Server URL
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * Default Model für Ollama
         */
        private String defaultModel = "mistral:latest";

        /**
         * Timeout in Sekunden
         */
        private int timeoutSeconds = 300;

        /**
         * Aktiviert/Deaktiviert Ollama Provider
         */
        private boolean enabled = false; // Default: disabled
    }
}
