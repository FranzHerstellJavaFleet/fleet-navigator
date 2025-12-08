package io.javafleet.fleetnavigator.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider-agnostische Modell-Informationen
 *
 * Native Image: Simple POJO, keine Reflection-Probleme
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    /**
     * Modell-Name (z.B. "llama3.2:3b", "qwen2.5:7b")
     */
    private String name;

    /**
     * Display-Name für UI (z.B. "Llama 3.2 (3B)")
     */
    private String displayName;

    /**
     * Provider-Name ("ollama", "llamacpp", "openai")
     */
    private String provider;

    /**
     * Größe in Bytes
     */
    private Long size;

    /**
     * Größe als Human-Readable String (z.B. "2.0 GB")
     */
    private String sizeHuman;

    /**
     * Architektur (z.B. "llama", "mistral", "qwen")
     */
    private String architecture;

    /**
     * Quantisierung (z.B. "Q4_K_M", "Q8_0") - nur für GGUF
     */
    private String quantization;

    /**
     * Beschreibung
     */
    private String description;

    /**
     * Context Window (Anzahl Tokens)
     */
    private Long contextWindow;

    /**
     * Modified-At Timestamp
     */
    private String modifiedAt;

    /**
     * Digest/Hash für Versionierung
     */
    private String digest;

    /**
     * Ist das ein User-uploaded Custom Model?
     */
    private boolean custom;

    /**
     * Ist das Modell bereits heruntergeladen/installiert?
     */
    private boolean installed;

    /**
     * Herausgeber/Publisher (z.B. "Meta", "Alibaba Cloud", "OpenAI")
     */
    private String publisher;

    /**
     * Release-Datum (z.B. "2024-09", "2024-11")
     */
    private String releaseDate;

    /**
     * Trainingsdaten bis (z.B. "2024-09", "2023-04")
     */
    private String trainedUntil;

    /**
     * Lizenz (z.B. "Apache 2.0", "MIT", "Llama 3.2 Community License")
     */
    private String license;

    /**
     * Unterstützte Sprachen
     */
    private String languages;

    /**
     * Primäre Aufgaben/Use-Cases
     */
    private String primaryTasks;

    /**
     * Stärken des Modells
     */
    private String strengths;

    /**
     * Bekannte Schwächen/Limitierungen
     */
    private String limitations;

    /**
     * Benchmark-Ergebnisse als Map (z.B. "MMLU": "72.3%")
     */
    private java.util.Map<String, String> benchmarks;
}
