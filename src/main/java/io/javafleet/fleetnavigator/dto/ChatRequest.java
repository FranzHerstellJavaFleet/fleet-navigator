package io.javafleet.fleetnavigator.dto;

import lombok.Data;
import java.util.List;

/**
 * Request DTO for sending a chat message
 */
@Data
public class ChatRequest {
    private Long chatId;
    private String message;
    private String model;
    private String systemPrompt;
    private Boolean stream = false;

    // Expert System
    private Long expertId;             // ID des aktiven Experten (für searchDomains etc.)
    private Long activeExpertModeId;   // ID des aktiven Modus (null = Allgemein)

    // For Vision models: Base64 encoded images
    private List<String> images;

    // For documents: Extracted text content
    private String documentContext;

    // File metadata for tracking uploaded files (name, type, size)
    private List<FileMetadata> fileMetadata;

    // Vision-Chaining settings
    private Boolean visionChainEnabled = false;
    private String visionModel;  // Vision Model für Chaining (z.B. llava:13b)
    private Boolean showIntermediateOutput = false;  // Zeige LLaVA-Beschreibung im Chat

    // Language setting for output
    private String language = "de";  // "de", "es", "tr", "fr"

    // Web Search RAG settings
    private Boolean webSearchEnabled = false;      // Aktiviert Web-Suche vor LLM-Anfrage (RAG-Modus)
    private Boolean includeSourceUrls = false;     // Quellen-URLs am Ende der Antwort hinzufügen
    private List<String> searchDomains;            // Domain-Filter (z.B. "gesetze-im-internet.de")
    private Integer maxSearchResults = 3;          // Max Suchergebnisse (3 = weniger Token-Verbrauch)

    // Hardware/Performance settings
    private Boolean cpuOnly = false;               // Deaktiviert CUDA/GPU (num_gpu=0) für Demos auf Laptops ohne NVIDIA

    // Model parameters for generation control (DEPRECATED - use samplingParameters instead)
    @Deprecated
    private Integer maxTokens;      // Maximum tokens to generate (num_predict in Ollama)
    @Deprecated
    private Double temperature;     // Randomness (0.0-2.0)
    @Deprecated
    private Double topP;           // Nucleus sampling
    @Deprecated
    private Integer topK;          // Top-K sampling
    @Deprecated
    private Double repeatPenalty;  // Penalize repetitions

    // Advanced Sampling Parameters (for ALL models - text and vision)
    // If provided, overrides deprecated individual parameters above
    private SamplingParameters samplingParameters;

    // Legacy: Vision-specific parameters (DEPRECATED - use samplingParameters instead)
    @Deprecated
    private SamplingParameters visionParameters;
}
