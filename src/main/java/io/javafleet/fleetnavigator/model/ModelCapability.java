package io.javafleet.fleetnavigator.model;

import java.util.Set;

/**
 * Model-Fähigkeiten für task-basiertes Routing
 *
 * @author JavaFleet Systems Consulting
 * @since 0.2.8
 */
public enum ModelCapability {
    /**
     * Vision-Fähigkeiten (Bilder, PDFs)
     */
    VISION,

    /**
     * Code-Generierung und -Verständnis
     */
    CODE,

    /**
     * Deutsche Sprache
     */
    GERMAN,

    /**
     * Schnelle Inferenz (kleine Modelle)
     */
    FAST,

    /**
     * Lange Context-Fenster
     */
    LONG_CONTEXT,

    /**
     * Allzweck Chat
     */
    CHAT,

    /**
     * Instruktions-fähig (Instruct/Chat-Modelle)
     */
    INSTRUCT;

    /**
     * Prüft ob ein Modellname bestimmte Capabilities hat
     *
     * @param modelName Name des Modells
     * @return Set von erkannten Capabilities
     */
    public static Set<ModelCapability> detectCapabilities(String modelName) {
        String lowerName = modelName.toLowerCase();
        Set<ModelCapability> capabilities = new java.util.HashSet<>();

        // Vision
        if (lowerName.contains("llava") ||
            lowerName.contains("bakllava") ||
            lowerName.contains("minicpm-v") ||
            lowerName.contains("vision") ||
            lowerName.contains("clip")) {
            capabilities.add(VISION);
        }

        // Code
        if (lowerName.contains("coder") ||
            lowerName.contains("code") ||
            lowerName.contains("deepseek") ||
            lowerName.contains("starcoder")) {
            capabilities.add(CODE);
        }

        // German
        if (lowerName.contains("qwen") ||
            lowerName.contains("german") ||
            lowerName.contains("deutsch") ||
            lowerName.contains("leo")) {
            capabilities.add(GERMAN);
        }

        // Fast (kleine Modelle)
        if (lowerName.contains("1b") ||
            lowerName.contains("3b") ||
            lowerName.contains("tiny") ||
            lowerName.contains("mini")) {
            capabilities.add(FAST);
        }

        // Long Context
        if (lowerName.contains("32k") ||
            lowerName.contains("64k") ||
            lowerName.contains("128k") ||
            lowerName.contains("long")) {
            capabilities.add(LONG_CONTEXT);
        }

        // Instruct
        if (lowerName.contains("instruct") ||
            lowerName.contains("chat")) {
            capabilities.add(INSTRUCT);
            capabilities.add(CHAT);
        }

        // Fallback: Allzweck-Chat für alle nicht-speziellen Modelle
        if (!capabilities.contains(VISION) && !capabilities.contains(CODE)) {
            capabilities.add(CHAT);
        }

        return capabilities;
    }

    /**
     * Gibt passende Capabilities für einen TaskType zurück
     *
     * @param taskType Der Aufgabentyp
     * @return Set von benötigten Capabilities
     */
    public static Set<ModelCapability> getRequiredCapabilities(TaskType taskType) {
        return switch (taskType) {
            case VISION -> Set.of(VISION);
            case CODE_GENERATION -> Set.of(CODE, INSTRUCT);
            case LOG_ANALYSIS -> Set.of(CODE, INSTRUCT);
            case EMAIL_CLASSIFICATION -> Set.of(FAST, INSTRUCT);
            case DOCUMENT_GENERATION -> Set.of(GERMAN, INSTRUCT);
            case FAST_RESPONSE -> Set.of(FAST, INSTRUCT);
            case GENERAL_CHAT -> Set.of(CHAT, INSTRUCT);
        };
    }
}
