package io.javafleet.fleetnavigator.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Universal sampling parameters for fine-grained control over ALL LLM models
 * (text-only and vision/multimodal models).
 *
 * These parameters allow users to tune the model's output characteristics:
 * - Randomness vs. Determinism
 * - Creativity vs. Factual Accuracy
 * - Length and Verbosity
 * - Repetition Avoidance
 *
 * Works with:
 * - Text models (Llama, Mistral, Qwen, etc.)
 * - Vision models (LLaVA, Qwen-VL, etc.)
 * - All providers (llama.cpp, Ollama, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamplingParameters {

    // ===== GENERATION CONTROL =====

    /**
     * Maximum number of tokens to generate
     * Default: 300 (concise descriptions)
     * Range: 50-2048
     *
     * Lower values (50-200): Very brief descriptions
     * Medium values (200-500): Standard descriptions
     * Higher values (500-2048): Detailed analysis
     */
    @Builder.Default
    private Integer maxTokens = 300;

    // ===== SAMPLING PARAMETERS =====

    /**
     * Temperature - Controls randomness/creativity
     * Default: 0.1 (very factual, low hallucination)
     * Range: 0.0-2.0
     *
     * 0.0-0.3: Highly deterministic, factual (recommended for vision)
     * 0.4-0.7: Balanced creativity and accuracy
     * 0.8-1.0: More creative, less predictable
     * 1.1-2.0: Very creative, high hallucination risk
     *
     * NOTE: For vision tasks, keep LOW (0.1-0.3) to prevent hallucination!
     */
    @Builder.Default
    private Double temperature = 0.1;

    /**
     * Top-P (Nucleus Sampling) - Cumulative probability threshold
     * Default: 0.9
     * Range: 0.0-1.0
     *
     * 0.1-0.5: Very focused, limited vocabulary
     * 0.6-0.9: Balanced diversity (recommended)
     * 0.95-1.0: Maximum diversity
     *
     * Works in conjunction with temperature
     */
    @Builder.Default
    private Double topP = 0.9;

    /**
     * Top-K - Limits to top K most likely tokens
     * Default: 40
     * Range: 0-100 (0 = disabled)
     *
     * 5-20: Very focused output
     * 20-50: Balanced (recommended)
     * 50-100: More diverse
     * 0: Disabled (use top_p instead)
     */
    @Builder.Default
    private Integer topK = 40;

    /**
     * Min-P - Minimum probability threshold
     * Default: 0.05
     * Range: 0.0-1.0
     *
     * Tokens with probability below (best_token_prob * min_p) are filtered out
     * 0.0: Disabled
     * 0.05-0.1: Recommended for quality
     * 0.2+: Very restrictive
     */
    @Builder.Default
    private Double minP = 0.05;

    // ===== REPETITION CONTROL =====

    /**
     * Repeat Penalty - Penalizes token repetition
     * Default: 1.15 (moderate anti-repetition)
     * Range: 1.0-1.5
     *
     * 1.0: No penalty (may repeat)
     * 1.05-1.15: Light penalty (recommended for vision)
     * 1.2-1.3: Strong penalty (for very repetitive models)
     * 1.4+: Very strong (may reduce coherence)
     */
    @Builder.Default
    private Double repeatPenalty = 1.15;

    /**
     * Repeat Last N - How many tokens to consider for repeat penalty
     * Default: 64
     * Range: 0-512 (0 = ctx_size/2)
     *
     * Larger values = longer-range repetition detection
     */
    @Builder.Default
    private Integer repeatLastN = 64;

    /**
     * Presence Penalty - Penalizes tokens based on whether they appear
     * Default: 0.0 (disabled)
     * Range: -2.0 to 2.0
     *
     * Positive values: Encourage new topics/vocabulary
     * Negative values: Encourage staying on topic
     */
    @Builder.Default
    private Double presencePenalty = 0.0;

    /**
     * Frequency Penalty - Penalizes based on how often tokens appear
     * Default: 0.0 (disabled)
     * Range: -2.0 to 2.0
     *
     * Positive values: Reduce repeated words
     * Negative values: Allow more repetition
     */
    @Builder.Default
    private Double frequencyPenalty = 0.0;

    // ===== MIROSTAT (Advanced Sampling) =====

    /**
     * Mirostat Mode - Perplexity-based sampling
     * Default: 0 (disabled)
     * Values: 0, 1, 2
     *
     * 0: Disabled (use temp/top_p/top_k)
     * 1: Mirostat 1.0
     * 2: Mirostat 2.0 (recommended if using Mirostat)
     *
     * NOTE: When enabled, disable top_p/top_k (set to 1.0/0)
     */
    @Builder.Default
    private Integer mirostatMode = 0;

    /**
     * Mirostat Tau - Target entropy/perplexity
     * Default: 5.0
     * Range: 0.0-10.0
     *
     * 2.0-3.0: Focused, coherent output
     * 4.0-6.0: Balanced (recommended)
     * 7.0-10.0: More diverse, creative
     *
     * Only used if mirostatMode > 0
     */
    @Builder.Default
    private Double mirostatTau = 5.0;

    /**
     * Mirostat Eta - Learning rate for Mirostat
     * Default: 0.1
     * Range: 0.01-1.0
     *
     * Lower values: Slower adaptation
     * Higher values: Faster adaptation
     *
     * Only used if mirostatMode > 0
     */
    @Builder.Default
    private Double mirostatEta = 0.1;

    // ===== STOP CONDITIONS =====

    /**
     * Stop Sequences - Strings that trigger generation stop
     * Default: Common patterns that indicate hallucination loops
     *
     * Example: ["\n\n\n", "USER:", "ASSISTANT:", "###"]
     */
    @Builder.Default
    private java.util.List<String> stopSequences = java.util.Arrays.asList(
        "\n\n\n",
        "**Technologische Konzepte**",
        "USER:",
        "ASSISTANT:",
        "###"
    );

    // ===== SYSTEM PROMPT =====

    /**
     * Custom System Prompt override
     * Default: null (uses anti-hallucination default)
     *
     * If null or empty, uses:
     * "You are a precise image analysis assistant. Describe only what
     *  you actually see. Do not invent or assume details."
     */
    private String customSystemPrompt;

    // ===== MODEL TYPE ENUM =====

    public enum ModelType {
        VISION,      // LLaVA, Qwen-VL, etc. - needs low temp
        TEXT,        // Llama, Mistral, Qwen - general purpose
        CODE,        // Qwen-Coder, CodeLlama - precise code gen
        CHAT         // Chat-optimized models
    }

    // ===== DEFAULT VALUES (The "Reset" target) =====

    /**
     * DEFAULT values - returned when user clicks "Reset to Defaults"
     * These are balanced, safe values that work well for most models
     */
    public static SamplingParameters getDefaults() {
        return SamplingParameters.builder()
            .maxTokens(512)           // Moderate length
            .temperature(0.7)         // Balanced creativity
            .topP(0.9)                // Standard nucleus sampling
            .topK(40)                 // Moderate diversity
            .minP(0.05)               // Small threshold
            .repeatPenalty(1.1)       // Light anti-repetition
            .repeatLastN(64)          // Standard context
            .presencePenalty(0.0)     // Disabled
            .frequencyPenalty(0.0)    // Disabled
            .mirostatMode(0)          // Disabled
            .mirostatTau(5.0)         // Default if enabled
            .mirostatEta(0.1)         // Default if enabled
            .stopSequences(java.util.Arrays.asList("\n\n\n", "USER:", "ASSISTANT:"))
            .customSystemPrompt(null) // Use provider default
            .build();
    }

    /**
     * Get optimal defaults for specific model type
     * This is smarter than getDefaults() - adapts to model characteristics
     */
    public static SamplingParameters getDefaultsForModelType(ModelType type) {
        return switch (type) {
            case VISION -> visionDefaults();
            case TEXT -> textDefaults();
            case CODE -> codeDefaults();
            case CHAT -> chatDefaults();
        };
    }

    /**
     * Auto-detect model type from name and return appropriate defaults
     */
    public static SamplingParameters autoDefaults(String modelName) {
        String lower = modelName.toLowerCase();

        // Vision models
        if (lower.contains("llava") || lower.contains("vision") ||
            lower.contains("qwen-vl") || lower.contains("cogvlm") ||
            lower.contains("moondream") || lower.contains("mmproj")) {
            return visionDefaults();
        }

        // Code models
        if (lower.contains("coder") || lower.contains("code") ||
            lower.contains("starcoder") || lower.contains("codellama")) {
            return codeDefaults();
        }

        // Chat models
        if (lower.contains("chat") || lower.contains("instruct")) {
            return chatDefaults();
        }

        // Default to general text
        return textDefaults();
    }

    // ===== MODEL-TYPE-SPECIFIC DEFAULTS =====

    /**
     * Defaults for Vision Models (LLaVA, Qwen-VL, etc.)
     * Low temperature to prevent hallucination!
     */
    private static SamplingParameters visionDefaults() {
        return SamplingParameters.builder()
            .maxTokens(300)
            .temperature(0.1)         // VERY LOW for vision!
            .topP(0.9)
            .topK(40)
            .minP(0.05)
            .repeatPenalty(1.15)
            .repeatLastN(64)
            .presencePenalty(0.0)
            .frequencyPenalty(0.0)
            .mirostatMode(0)
            .stopSequences(java.util.Arrays.asList("\n\n\n", "USER:", "ASSISTANT:"))
            .customSystemPrompt("You are a precise image analysis assistant. Describe only what you actually see in the image. Do not invent or assume details that are not clearly visible. Be factual and concise.")
            .build();
    }

    /**
     * Defaults for Text Models (Llama, Mistral, Qwen)
     * Balanced for general purpose use
     */
    private static SamplingParameters textDefaults() {
        return SamplingParameters.builder()
            .maxTokens(512)
            .temperature(0.7)         // Moderate creativity
            .topP(0.9)
            .topK(40)
            .minP(0.05)
            .repeatPenalty(1.1)
            .repeatLastN(64)
            .presencePenalty(0.0)
            .frequencyPenalty(0.0)
            .mirostatMode(0)
            .stopSequences(java.util.Arrays.asList("\n\n\n"))
            .customSystemPrompt(null)
            .build();
    }

    /**
     * Defaults for Code Models (Qwen-Coder, CodeLlama)
     * Lower temperature for precision
     */
    private static SamplingParameters codeDefaults() {
        return SamplingParameters.builder()
            .maxTokens(2048)          // Longer for code
            .temperature(0.2)         // Low for precision
            .topP(0.95)
            .topK(50)
            .minP(0.05)
            .repeatPenalty(1.1)
            .repeatLastN(64)
            .presencePenalty(0.0)
            .frequencyPenalty(0.0)
            .mirostatMode(0)
            .stopSequences(java.util.Arrays.asList("\n\n\n", "```"))
            .customSystemPrompt(null)
            .build();
    }

    /**
     * Defaults for Chat Models
     * Higher creativity, conversational
     */
    private static SamplingParameters chatDefaults() {
        return SamplingParameters.builder()
            .maxTokens(1024)
            .temperature(0.8)         // More creative
            .topP(0.9)
            .topK(40)
            .minP(0.05)
            .repeatPenalty(1.1)
            .repeatLastN(64)
            .presencePenalty(0.0)
            .frequencyPenalty(0.0)
            .mirostatMode(0)
            .stopSequences(java.util.Arrays.asList("\n\n\n"))
            .customSystemPrompt(null)
            .build();
    }

    // ===== PRESETS (Advanced - for power users) =====

    /**
     * Preset: Ultra-Precise (minimal hallucination)
     * Use for: Technical documentation, factual descriptions
     */
    public static SamplingParameters ultraPrecise() {
        return SamplingParameters.builder()
            .maxTokens(200)
            .temperature(0.05)
            .topP(0.8)
            .topK(20)
            .minP(0.1)
            .repeatPenalty(1.2)
            .build();
    }

    /**
     * Preset: Balanced (good for most use cases)
     * Use for: General purpose, balanced quality
     */
    public static SamplingParameters balanced() {
        return SamplingParameters.builder()
            .maxTokens(300)
            .temperature(0.1)
            .topP(0.9)
            .topK(40)
            .minP(0.05)
            .repeatPenalty(1.15)
            .build();
    }

    /**
     * Preset: Detailed Analysis (longer, more descriptive)
     */
    public static SamplingParameters detailed() {
        return SamplingParameters.builder()
            .maxTokens(800)
            .temperature(0.2)
            .topP(0.95)
            .topK(50)
            .minP(0.05)
            .repeatPenalty(1.1)
            .build();
    }

    /**
     * Preset: Creative Description (higher temperature, more narrative)
     */
    public static SamplingParameters creative() {
        return SamplingParameters.builder()
            .maxTokens(500)
            .temperature(0.5)
            .topP(0.95)
            .topK(60)
            .minP(0.03)
            .repeatPenalty(1.1)
            .build();
    }

    /**
     * Preset: Mirostat Mode (alternative sampling strategy)
     */
    public static SamplingParameters mirostat() {
        return SamplingParameters.builder()
            .maxTokens(300)
            .temperature(0.0)  // Ignored in Mirostat
            .topP(1.0)         // Disabled for Mirostat
            .topK(0)           // Disabled for Mirostat
            .mirostatMode(2)
            .mirostatTau(4.0)
            .mirostatEta(0.1)
            .repeatPenalty(1.15)
            .build();
    }
}
