package io.javafleet.fleetnavigator.llm;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Registry Entry für ein herunterladbares Modell
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.9
 */
@Data
@Builder
public class ModelRegistryEntry {

    // Identifikation
    private String id;                      // "qwen2.5-3b-instruct"
    private String displayName;             // "Qwen 2.5 (3B) - Instruct"
    private String provider;                // "Alibaba Cloud"
    private String architecture;            // "qwen2"
    private String version;                 // "2.5"
    private String parameterSize;           // "3B"
    private String quantization;            // "Q4_K_M"

    // Download-Informationen
    private String huggingFaceRepo;         // "Qwen/Qwen2.5-3B-Instruct-GGUF"
    private String filename;                // "qwen2.5-3b-instruct-q4_k_m.gguf"
    private Long sizeBytes;                 // 2147483648
    private String sizeHuman;               // "2.0 GB"

    // Beschreibungen
    private String description;             // "Exzellentes mehrsprachiges Modell..."
    private List<String> languages;         // ["Deutsch", "Englisch", ...]
    private List<String> useCases;          // ["Chat", "Briefe", ...]
    private String license;                 // "Apache 2.0"

    // Bewertungen
    private Float rating;                   // 4.8
    private Integer downloads;              // 125000

    // Hardware-Anforderungen
    private Integer minRamGB;               // 4
    private Integer recommendedRamGB;       // 8
    private Boolean gpuAccelSupported;      // true

    // UI-Flags
    private boolean featured;               // Auf Startseite zeigen?
    private boolean trending;               // Im Trending-Bereich?
    private String category;                // "chat", "code", "compact"

    // Zusätzliche Metadaten
    private String releaseDate;             // "2024-11"
    private String trainedUntil;            // "2024-09"
    private String contextWindow;           // "128K tokens"
    private String primaryTasks;            // "Chat, Briefe, Übersetzungen"
    private String strengths;               // "Exzellentes Deutsch, schnell, ..."
    private String limitations;             // "Komplexe Mathematik"

    // Vision/Multimodal Support
    private boolean isVisionModel;          // true for LLaVA, MiniCPM-V, etc.
    private String mmprojFilename;          // "mmproj-model-f16.gguf" for vision models
    private String mmprojUrl;               // Alternative URL for mmproj file (optional)
}
