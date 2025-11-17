package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.SamplingParameters;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Sampling Parameter management
 * Handles defaults, presets, and auto-optimization for all model types
 */
@RestController
@RequestMapping("/api/sampling")
public class SamplingParametersController {

    /**
     * Get all available presets
     */
    @GetMapping("/presets")
    public ResponseEntity<Map<String, SamplingParameters>> getPresets() {
        Map<String, SamplingParameters> presets = new HashMap<>();
        presets.put("ultra-precise", SamplingParameters.ultraPrecise());
        presets.put("balanced", SamplingParameters.balanced());
        presets.put("detailed", SamplingParameters.detailed());
        presets.put("creative", SamplingParameters.creative());
        presets.put("mirostat", SamplingParameters.mirostat());
        return ResponseEntity.ok(presets);
    }

    /**
     * Get a specific preset by name
     */
    @GetMapping("/presets/{name}")
    public ResponseEntity<SamplingParameters> getPreset(@PathVariable String name) {
        SamplingParameters params = switch (name.toLowerCase()) {
            case "ultra-precise" -> SamplingParameters.ultraPrecise();
            case "balanced" -> SamplingParameters.balanced();
            case "detailed" -> SamplingParameters.detailed();
            case "creative" -> SamplingParameters.creative();
            case "mirostat" -> SamplingParameters.mirostat();
            default -> null;
        };

        if (params == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(params);
    }

    /**
     * Get universal defaults (for "Reset to Defaults" button)
     */
    @GetMapping("/defaults")
    public ResponseEntity<Map<String, Object>> getDefaults() {
        SamplingParameters defaults = SamplingParameters.getDefaults();

        Map<String, Object> response = new HashMap<>();
        response.put("parameters", defaults);
        response.put("description", "Universal balanced defaults that work for most models");

        // Add parameter descriptions
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("maxTokens", "Maximum tokens (50-2048). Lower=brief, Higher=detailed");
        descriptions.put("temperature", "Randomness (0.0-2.0). Low=factual, High=creative. For vision: 0.05-0.3 recommended");
        descriptions.put("topP", "Nucleus sampling (0.0-1.0). Controls diversity");
        descriptions.put("topK", "Top-K sampling (0-100). 0=disabled. Limits token choices");
        descriptions.put("minP", "Min probability (0.0-1.0). Filters unlikely tokens");
        descriptions.put("repeatPenalty", "Repeat penalty (1.0-1.5). >1.0 reduces repetition");
        descriptions.put("repeatLastN", "Tokens to check for repetition (0-512)");
        descriptions.put("presencePenalty", "Presence penalty (-2.0 to 2.0). Encourages new topics");
        descriptions.put("frequencyPenalty", "Frequency penalty (-2.0 to 2.0). Reduces word repetition");
        descriptions.put("mirostatMode", "Mirostat (0=off, 1=v1, 2=v2). Alternative sampling");
        descriptions.put("mirostatTau", "Mirostat target entropy (0-10). 2-3=focused, 7-10=diverse");
        descriptions.put("mirostatEta", "Mirostat learning rate (0.01-1.0)");
        descriptions.put("stopSequences", "Strings that stop generation");
        descriptions.put("customSystemPrompt", "Override default anti-hallucination prompt");

        response.put("descriptions", descriptions);

        // Add recommendations
        Map<String, String> recommendations = new HashMap<>();
        recommendations.put("minimal-hallucination", "Use ultra-precise preset or temp=0.05-0.1");
        recommendations.put("balanced-quality", "Use balanced preset (default)");
        recommendations.put("detailed-analysis", "Use detailed preset with maxTokens=800-1200");
        recommendations.put("creative-narrative", "Use creative preset but expect more hallucination");
        recommendations.put("prevent-loops", "Increase repeatPenalty to 1.2-1.3");
        recommendations.put("mirostat-mode", "Disable top_p/top_k when using mirostat");

        response.put("recommendations", recommendations);

        return ResponseEntity.ok(response);
    }

    /**
     * Validate custom parameters
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateParameters(@RequestBody SamplingParameters params) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> warnings = new HashMap<>();

        // Validate ranges
        if (params.getTemperature() != null && params.getTemperature() > 0.5) {
            warnings.put("temperature", "High temperature (" + params.getTemperature() + ") may cause hallucination. Recommended: 0.05-0.3 for vision");
        }

        if (params.getMaxTokens() != null && params.getMaxTokens() > 1000) {
            warnings.put("maxTokens", "Large maxTokens (" + params.getMaxTokens() + ") may lead to repetitive or hallucinated content");
        }

        if (params.getRepeatPenalty() != null && params.getRepeatPenalty() < 1.05) {
            warnings.put("repeatPenalty", "Low repeat penalty (" + params.getRepeatPenalty() + ") may allow repetition. Recommended: 1.1-1.2");
        }

        if (params.getMirostatMode() != null && params.getMirostatMode() > 0) {
            if (params.getTopP() != null && params.getTopP() < 1.0) {
                warnings.put("mirostat", "Mirostat is enabled but top_p=" + params.getTopP() + ". Set top_p=1.0 when using Mirostat");
            }
            if (params.getTopK() != null && params.getTopK() > 0) {
                warnings.put("mirostat", "Mirostat is enabled but top_k=" + params.getTopK() + ". Set top_k=0 when using Mirostat");
            }
        }

        response.put("valid", warnings.isEmpty());
        response.put("warnings", warnings);
        response.put("parameters", params);

        return ResponseEntity.ok(response);
    }

    /**
     * Get defaults optimized for specific model (auto-detect from name)
     */
    @GetMapping("/defaults/auto/{modelName}")
    public ResponseEntity<Map<String, Object>> getAutoDefaults(@PathVariable String modelName) {
        SamplingParameters params = SamplingParameters.autoDefaults(modelName);

        Map<String, Object> response = new HashMap<>();
        response.put("parameters", params);
        response.put("modelName", modelName);

        // Detect model type
        String detectedType = "TEXT";
        String lower = modelName.toLowerCase();
        if (lower.contains("llava") || lower.contains("vision") || lower.contains("qwen-vl")) {
            detectedType = "VISION";
        } else if (lower.contains("coder") || lower.contains("code")) {
            detectedType = "CODE";
        } else if (lower.contains("chat") || lower.contains("instruct")) {
            detectedType = "CHAT";
        }

        response.put("detectedType", detectedType);
        response.put("description", "Auto-optimized defaults for " + detectedType + " model");

        return ResponseEntity.ok(response);
    }

    /**
     * Get defaults for specific model type
     */
    @GetMapping("/defaults/{type}")
    public ResponseEntity<Map<String, Object>> getDefaultsForType(@PathVariable String type) {
        SamplingParameters.ModelType modelType;
        try {
            modelType = SamplingParameters.ModelType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid model type. Valid types: VISION, TEXT, CODE, CHAT"
            ));
        }

        SamplingParameters params = SamplingParameters.getDefaultsForModelType(modelType);

        Map<String, Object> response = new HashMap<>();
        response.put("parameters", params);
        response.put("modelType", modelType);
        response.put("description", "Optimized defaults for " + modelType + " models");

        return ResponseEntity.ok(response);
    }

    /**
     * Get parameter help/documentation in German
     */
    @GetMapping("/help/de")
    public ResponseEntity<Map<String, Object>> getHelpDE() {
        Map<String, Object> help = new HashMap<>();

        help.put("title", "Sampling Parameter Hilfe");
        help.put("language", "de");

        Map<String, Map<String, String>> parameters = new HashMap<>();

        // maxTokens
        Map<String, String> maxTokens = new HashMap<>();
        maxTokens.put("name", "Maximale Tokens");
        maxTokens.put("description", "Begrenzt die Länge der generierten Antwort");
        maxTokens.put("range", "50-2048");
        maxTokens.put("default", "512");
        maxTokens.put("example", "200 = Kurze Antwort, 1000 = Ausführliche Beschreibung");
        maxTokens.put("tip", "Für Vision: 200-500. Für Code: 1024-2048. Für Chat: 512-1024");
        parameters.put("maxTokens", maxTokens);

        // temperature
        Map<String, String> temperature = new HashMap<>();
        temperature.put("name", "Temperatur");
        temperature.put("description", "Steuert Zufälligkeit und Kreativität");
        temperature.put("range", "0.0-2.0");
        temperature.put("default", "0.7");
        temperature.put("example", "0.1 = Sehr faktisch (Vision!), 0.7 = Ausgewogen, 1.2 = Sehr kreativ");
        temperature.put("tip", "WICHTIG: Für Vision-Modelle 0.05-0.2 verwenden, sonst Halluzinationen!");
        temperature.put("warning", "Werte über 1.0 können zu unsinnigen Antworten führen");
        parameters.put("temperature", temperature);

        // topP
        Map<String, String> topP = new HashMap<>();
        topP.put("name", "Top-P (Nucleus Sampling)");
        topP.put("description", "Begrenzt Auswahl auf wahrscheinlichste Tokens");
        topP.put("range", "0.0-1.0");
        topP.put("default", "0.9");
        topP.put("example", "0.5 = Sehr fokussiert, 0.9 = Standard, 0.95 = Mehr Vielfalt");
        topP.put("tip", "Mit Temperature kombinieren für beste Ergebnisse");
        parameters.put("topP", topP);

        // topK
        Map<String, String> topK = new HashMap<>();
        topK.put("name", "Top-K Sampling");
        topK.put("description", "Begrenzt Auswahl auf K wahrscheinlichste Tokens");
        topK.put("range", "0-100 (0=deaktiviert)");
        topK.put("default", "40");
        topK.put("example", "10 = Sehr eingeschränkt, 40 = Standard, 80 = Viel Vielfalt");
        topK.put("tip", "Alternative zu Top-P. Meist nur eins verwenden");
        parameters.put("topK", topK);

        // repeatPenalty
        Map<String, String> repeatPenalty = new HashMap<>();
        repeatPenalty.put("name", "Wiederholungs-Strafe");
        repeatPenalty.put("description", "Bestraft wiederholte Wörter/Phrasen");
        repeatPenalty.put("range", "1.0-1.5");
        repeatPenalty.put("default", "1.1");
        repeatPenalty.put("example", "1.0 = Keine Strafe, 1.15 = Leicht, 1.3 = Stark");
        repeatPenalty.put("tip", "Für Vision: 1.15-1.2. Bei Loops: auf 1.3 erhöhen");
        repeatPenalty.put("warning", "Zu hoch (>1.4) kann Kohärenz verschlechtern");
        parameters.put("repeatPenalty", repeatPenalty);

        // mirostat
        Map<String, String> mirostat = new HashMap<>();
        mirostat.put("name", "Mirostat-Modus");
        mirostat.put("description", "Alternative Sampling-Strategie (experimentell)");
        mirostat.put("range", "0, 1, 2 (0=aus)");
        mirostat.put("default", "0");
        mirostat.put("example", "0 = Standard-Sampling, 2 = Mirostat 2.0 (empfohlen wenn aktiviert)");
        mirostat.put("tip", "Wenn aktiviert: top_p=1.0 und top_k=0 setzen!");
        mirostat.put("warning", "Experimentell - nicht für alle Modelle geeignet");
        parameters.put("mirostat", mirostat);

        help.put("parameters", parameters);

        // Quick tips
        Map<String, String> quickTips = new HashMap<>();
        quickTips.put("vision", "Vision-Modelle: temperature=0.05-0.2, maxTokens=200-500, repeatPenalty=1.15");
        quickTips.put("code", "Code-Modelle: temperature=0.2-0.4, maxTokens=1024-2048, repeatPenalty=1.1");
        quickTips.put("chat", "Chat-Modelle: temperature=0.7-0.9, maxTokens=512-1024, repeatPenalty=1.1");
        quickTips.put("factual", "Faktische Antworten: temperature=0.1-0.3, topP=0.8, topK=20");
        quickTips.put("creative", "Kreative Antworten: temperature=0.8-1.0, topP=0.95, topK=60");
        help.put("quickTips", quickTips);

        // Common issues
        Map<String, String> commonIssues = new HashMap<>();
        commonIssues.put("hallucination", "Halluzinationen? → temperature auf 0.1 senken");
        commonIssues.put("repetition", "Wiederholungen? → repeatPenalty auf 1.2-1.3 erhöhen");
        commonIssues.put("tooShort", "Zu kurz? → maxTokens erhöhen");
        commonIssues.put("nonsense", "Unsinn? → temperature senken, topK verringern");
        commonIssues.put("boring", "Langweilig? → temperature etwas erhöhen (0.7-0.8)");
        help.put("commonIssues", commonIssues);

        return ResponseEntity.ok(help);
    }

    /**
     * Get parameter help/documentation in English
     */
    @GetMapping("/help/en")
    public ResponseEntity<Map<String, Object>> getHelpEN() {
        Map<String, Object> help = new HashMap<>();

        help.put("title", "Sampling Parameters Help");
        help.put("language", "en");

        Map<String, Map<String, String>> parameters = new HashMap<>();

        // maxTokens
        Map<String, String> maxTokens = new HashMap<>();
        maxTokens.put("name", "Maximum Tokens");
        maxTokens.put("description", "Limits the length of generated response");
        maxTokens.put("range", "50-2048");
        maxTokens.put("default", "512");
        maxTokens.put("example", "200 = Brief answer, 1000 = Detailed description");
        maxTokens.put("tip", "For vision: 200-500. For code: 1024-2048. For chat: 512-1024");
        parameters.put("maxTokens", maxTokens);

        // temperature
        Map<String, String> temperature = new HashMap<>();
        temperature.put("name", "Temperature");
        temperature.put("description", "Controls randomness and creativity");
        temperature.put("range", "0.0-2.0");
        temperature.put("default", "0.7");
        temperature.put("example", "0.1 = Very factual (Vision!), 0.7 = Balanced, 1.2 = Very creative");
        temperature.put("tip", "IMPORTANT: Use 0.05-0.2 for vision models to prevent hallucination!");
        temperature.put("warning", "Values above 1.0 may produce nonsensical output");
        parameters.put("temperature", temperature);

        // topP
        Map<String, String> topP = new HashMap<>();
        topP.put("name", "Top-P (Nucleus Sampling)");
        topP.put("description", "Limits selection to most probable tokens");
        topP.put("range", "0.0-1.0");
        topP.put("default", "0.9");
        topP.put("example", "0.5 = Very focused, 0.9 = Standard, 0.95 = More diversity");
        topP.put("tip", "Combine with temperature for best results");
        parameters.put("topP", topP);

        // topK
        Map<String, String> topK = new HashMap<>();
        topK.put("name", "Top-K Sampling");
        topK.put("description", "Limits selection to K most probable tokens");
        topK.put("range", "0-100 (0=disabled)");
        topK.put("default", "40");
        topK.put("example", "10 = Very restricted, 40 = Standard, 80 = High diversity");
        topK.put("tip", "Alternative to Top-P. Usually use only one");
        parameters.put("topK", topK);

        // repeatPenalty
        Map<String, String> repeatPenalty = new HashMap<>();
        repeatPenalty.put("name", "Repeat Penalty");
        repeatPenalty.put("description", "Penalizes repeated words/phrases");
        repeatPenalty.put("range", "1.0-1.5");
        repeatPenalty.put("default", "1.1");
        repeatPenalty.put("example", "1.0 = No penalty, 1.15 = Light, 1.3 = Strong");
        repeatPenalty.put("tip", "For vision: 1.15-1.2. If loops occur: increase to 1.3");
        repeatPenalty.put("warning", "Too high (>1.4) may reduce coherence");
        parameters.put("repeatPenalty", repeatPenalty);

        // mirostat
        Map<String, String> mirostat = new HashMap<>();
        mirostat.put("name", "Mirostat Mode");
        mirostat.put("description", "Alternative sampling strategy (experimental)");
        mirostat.put("range", "0, 1, 2 (0=off)");
        mirostat.put("default", "0");
        mirostat.put("example", "0 = Standard sampling, 2 = Mirostat 2.0 (recommended if enabled)");
        mirostat.put("tip", "If enabled: set top_p=1.0 and top_k=0!");
        mirostat.put("warning", "Experimental - not suitable for all models");
        parameters.put("mirostat", mirostat);

        help.put("parameters", parameters);

        // Quick tips
        Map<String, String> quickTips = new HashMap<>();
        quickTips.put("vision", "Vision models: temperature=0.05-0.2, maxTokens=200-500, repeatPenalty=1.15");
        quickTips.put("code", "Code models: temperature=0.2-0.4, maxTokens=1024-2048, repeatPenalty=1.1");
        quickTips.put("chat", "Chat models: temperature=0.7-0.9, maxTokens=512-1024, repeatPenalty=1.1");
        quickTips.put("factual", "Factual answers: temperature=0.1-0.3, topP=0.8, topK=20");
        quickTips.put("creative", "Creative answers: temperature=0.8-1.0, topP=0.95, topK=60");
        help.put("quickTips", quickTips);

        // Common issues
        Map<String, String> commonIssues = new HashMap<>();
        commonIssues.put("hallucination", "Hallucinations? → Lower temperature to 0.1");
        commonIssues.put("repetition", "Repetitions? → Increase repeatPenalty to 1.2-1.3");
        commonIssues.put("tooShort", "Too short? → Increase maxTokens");
        commonIssues.put("nonsense", "Nonsense? → Lower temperature, reduce topK");
        commonIssues.put("boring", "Boring? → Slightly increase temperature (0.7-0.8)");
        help.put("commonIssues", commonIssues);

        return ResponseEntity.ok(help);
    }
}
