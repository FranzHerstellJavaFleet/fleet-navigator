package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for GGUF models used with java-llama-cpp provider.
 * Allows per-model customization of context size, system prompts, and parameters.
 */
@Entity
@Table(name = "gguf_model_config")
@Data
public class GgufModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name for this configured model (e.g., "Karla Coding Assistant")
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Base GGUF model filename (e.g., "qwen2.5-7b-instruct-q4_K_M.gguf")
     */
    @Column(nullable = false)
    private String baseModel;

    /**
     * System prompt that will be prepended to every conversation with this model.
     * Can be loaded from uploaded text file or entered directly.
     */
    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    /**
     * Context window size in tokens (4096, 8192, 16384, 32768, etc.)
     */
    @Column(nullable = false)
    private Integer contextSize = 4096;

    /**
     * Number of GPU layers to offload (0 = CPU only, 999 = all layers)
     */
    @Column(nullable = false)
    private Integer gpuLayers = 999;

    /**
     * Sampling temperature (0.0 to 2.0)
     */
    private Double temperature;

    /**
     * Top-P sampling (nucleus sampling)
     */
    private Double topP;

    /**
     * Top-K sampling
     */
    private Integer topK;

    /**
     * Repetition penalty
     */
    private Double repeatPenalty;

    /**
     * Maximum tokens to generate
     */
    private Integer maxTokens;

    // ========== CPU/Threading Configuration ==========

    /**
     * Number of CPU threads to use (0 = auto)
     */
    private Integer threads;

    /**
     * Batch size for prompt processing (default: 512)
     */
    private Integer batchSize;

    // ========== RoPE Scaling Configuration ==========

    /**
     * RoPE frequency base (default: 10000.0)
     * Used for extending context windows
     */
    private Double ropeFreqBase;

    /**
     * RoPE frequency scale (default: 1.0)
     * Values < 1.0 extend context, > 1.0 compress
     */
    private Double ropeFreqScale;

    // ========== Mirostat Sampling ==========

    /**
     * Mirostat sampling mode (0=disabled, 1=Mirostat, 2=Mirostat 2.0)
     */
    private Integer mirostat;

    /**
     * Mirostat target entropy (tau parameter, default: 5.0)
     */
    private Double mirostatTau;

    /**
     * Mirostat learning rate (eta parameter, default: 0.1)
     */
    private Double mirostatEta;

    // ========== Advanced Sampling Parameters ==========

    /**
     * Tail Free Sampling parameter (default: 1.0, disabled)
     */
    private Double tfsZ;

    /**
     * Typical P sampling (default: 1.0, disabled)
     */
    private Double typicalP;

    /**
     * Presence penalty (0.0 to 2.0, default: 0.0)
     */
    private Double presencePenalty;

    /**
     * Frequency penalty (0.0 to 2.0, default: 0.0)
     */
    private Double frequencyPenalty;

    /**
     * Min-P sampling (0.0 to 1.0, default: 0.05)
     * Modern alternative to Top-P
     */
    private Double minP;

    // ========== Reproducibility ==========

    /**
     * Random seed for reproducibility (-1 = random)
     */
    private Long seed;

    /**
     * Stop sequences (one per line, stored as JSON array)
     */
    @Column(columnDefinition = "TEXT")
    private String stopSequences;

    // ========== Performance Optimization ==========

    /**
     * Enable Flash Attention for faster processing
     */
    private Boolean flashAttention;

    /**
     * Low VRAM mode (reduces GPU memory usage)
     */
    private Boolean lowVram;

    /**
     * Enable memory mapping (faster loading, more RAM)
     */
    private Boolean mmapEnabled;

    /**
     * Lock model in RAM (prevents swapping)
     */
    private Boolean mlockEnabled;

    // ========== Organization/Metadata ==========

    /**
     * Model category (e.g., "Coding", "Chat", "German", "Vision")
     */
    private String category;

    /**
     * Tags for filtering/searching (comma-separated)
     */
    private String tags;

    /**
     * Description/notes about this model configuration
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this is the default model
     */
    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
