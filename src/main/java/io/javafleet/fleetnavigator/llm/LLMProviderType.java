package io.javafleet.fleetnavigator.llm;

/**
 * LLM Provider Typen
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.7
 */
public enum LLMProviderType {

    /**
     * llama.cpp - Embedded Server
     * - Embedded in Fleet Navigator
     * - GGUF Models (39.600+ verfügbar)
     * - Zero Installation
     */
    LLAMA_CPP("llamacpp", "llama.cpp", "http://localhost:8080"),

    /**
     * Java llama.cpp - JNI-based Provider (Preferred)
     * - Native Java Integration
     * - GGUF Models (39.600+ verfügbar)
     * - Zero Installation, Maximum Performance
     */
    JAVA_LLAMA_CPP("java-llama-cpp", "Java llama.cpp", "embedded");

    private final String id;
    private final String displayName;
    private final String defaultUrl;

    LLMProviderType(String id, String displayName, String defaultUrl) {
        this.id = id;
        this.displayName = displayName;
        this.defaultUrl = defaultUrl;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    /**
     * Findet Provider-Type anhand ID
     */
    public static LLMProviderType fromId(String id) {
        for (LLMProviderType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + id);
    }
}
