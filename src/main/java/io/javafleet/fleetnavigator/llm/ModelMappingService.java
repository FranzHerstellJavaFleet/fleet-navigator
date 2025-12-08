package io.javafleet.fleetnavigator.llm;

import io.javafleet.fleetnavigator.config.FleetPathsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Service für Model-Mapping zwischen Ollama-Modellnamen und GGUF-Dateien.
 *
 * Ermöglicht automatische Übersetzung von z.B. "qwen2.5:7b" zu "qwen2.5-7b-instruct-q4_k_m.gguf"
 * wenn java-llama-cpp Provider aktiv ist.
 */
@Service
@Slf4j
public class ModelMappingService {

    @Autowired
    private FleetPathsConfiguration pathsConfig;

    @Value("${llm.llamacpp.models-dir:}")
    private String modelsDirOverride;

    private String modelsDir;

    // Mapping: Ollama Name → GGUF Dateiname (ohne Pfad)
    private final Map<String, String> ollamaToGgufMapping = new ConcurrentHashMap<>();

    // Verfügbare GGUF-Dateien
    private final List<String> availableGgufModels = new ArrayList<>();

    // Standard-GGUF-Modell falls kein Mapping gefunden
    private String defaultGgufModel = null;

    @PostConstruct
    public void init() {
        // Ermittle models directory: Override hat Vorrang vor FleetPathsConfiguration
        if (modelsDirOverride != null && !modelsDirOverride.isBlank()) {
            modelsDir = modelsDirOverride;
            log.info("Models-Verzeichnis (Override): {}", modelsDir);
        } else {
            modelsDir = pathsConfig.getResolvedModelsDir().toString();
            log.info("Models-Verzeichnis (Auto): {}", modelsDir);
        }

        scanAvailableModels();
        initializeDefaultMappings();
        log.info("ModelMappingService initialisiert mit {} GGUF-Modellen und {} Mappings",
                availableGgufModels.size(), ollamaToGgufMapping.size());
    }

    /**
     * Scannt das Models-Verzeichnis nach GGUF-Dateien
     */
    public void scanAvailableModels() {
        availableGgufModels.clear();

        Path basePath = Paths.get(modelsDir);
        if (!Files.exists(basePath)) {
            log.warn("Models-Verzeichnis existiert nicht: {}", modelsDir);
            return;
        }

        try (Stream<Path> paths = Files.walk(basePath, 2)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().toLowerCase().endsWith(".gguf"))
                 .forEach(p -> {
                     String relativePath = basePath.relativize(p).toString();
                     availableGgufModels.add(relativePath);
                     log.debug("GGUF-Modell gefunden: {}", relativePath);
                 });
        } catch (Exception e) {
            log.error("Fehler beim Scannen des Models-Verzeichnisses", e);
        }

        // Setze Standard-Modell auf erstes gefundenes
        if (!availableGgufModels.isEmpty() && defaultGgufModel == null) {
            defaultGgufModel = availableGgufModels.get(0);
            log.info("Standard-GGUF-Modell gesetzt: {}", defaultGgufModel);
        }
    }

    /**
     * Initialisiert Standard-Mappings für häufige Ollama-Modelle
     *
     * WICHTIG: Für schwächere Hardware werden kleinere Modelle (1B-3B) als Standard verwendet!
     * Die 3 Basis-Experten (Roland, Ayşe, Luca) nutzen alle das gleiche leichte Modell.
     */
    private void initializeDefaultMappings() {
        // ========== STANDARD-MODELL FÜR ALLE EXPERTEN ==========
        // Llama 3.2 1B ist das Standard-Modell - läuft auch auf schwacher Hardware!
        // Alle Experten teilen sich dieses Modell, die Persönlichkeit kommt vom System-Prompt

        // Roland (Rechtsanwalt) - nutzt qwen2.5:7b in Ollama
        addMapping("qwen2.5:7b", "llama-3.2-1b-instruct", "llama");

        // Ayşe (Marketing) - nutzt auch qwen2.5:7b in Ollama
        // Mapping bereits oben

        // Luca (IT-Support) - nutzt deepseek-coder-v2:16b in Ollama
        addMapping("deepseek-coder-v2:16b", "llama-3.2-1b-instruct", "llama");

        // ========== KLEINERE MODELLE (1B-3B) - Für schwache Hardware ==========
        // Diese werden bevorzugt verwendet wenn keine größeren verfügbar sind

        // Llama 3.2 Familie (empfohlen für schwache Hardware)
        addMapping("llama3.2:1b", "llama-3.2-1b-instruct", "llama");
        addMapping("llama3.2:3b", "llama-3.2-3b-instruct", "llama");

        // Qwen kleine Modelle
        addMapping("qwen2.5:0.5b", "qwen2.5-0.5b-instruct", "qwen");
        addMapping("qwen2.5:1.5b", "qwen2.5-1.5b-instruct", "qwen");
        addMapping("qwen2.5:3b", "qwen2.5-3b-instruct", "qwen");

        // Phi-3 Mini (klein und schnell)
        addMapping("phi3:mini", "phi-3-mini", "phi");

        // Gemma 2B
        addMapping("gemma:2b", "gemma-2b-instruct", "gemma");

        // ========== GRÖSSERE MODELLE (7B+) - Für leistungsstarke Hardware ==========
        // Diese werden nur verwendet wenn explizit konfiguriert

        addMapping("qwen2.5:14b", "qwen2.5-14b-instruct", "qwen");
        addMapping("llama3.1:8b", "llama-3.1-8b-instruct", "llama");
        addMapping("llama3:8b", "llama-3-8b-instruct", "llama");
        addMapping("mistral:latest", "mistral-7b-instruct", "mistral");
        addMapping("mistral:7b", "mistral-7b-instruct", "mistral");
        addMapping("mistral:7b-instruct", "mistral-7b-instruct", "mistral");
        addMapping("deepseek-coder:6.7b", "deepseek-coder", "deepseek");
        addMapping("phi3:medium", "phi-3-medium", "phi");
        addMapping("gemma2:9b", "gemma-2-9b-instruct", "gemma");
        addMapping("gemma:7b", "gemma-7b-instruct", "gemma");

        log.info("Standard-Mappings initialisiert - Experten nutzen leichte Modelle (1B-3B) für breite Hardware-Kompatibilität");
    }

    /**
     * Fügt ein Mapping hinzu und sucht nach passender GGUF-Datei
     */
    private void addMapping(String ollamaName, String ggufBaseName, String familyPrefix) {
        // Suche nach passender GGUF-Datei
        String foundGguf = findMatchingGguf(ggufBaseName, familyPrefix);
        if (foundGguf != null) {
            ollamaToGgufMapping.put(ollamaName.toLowerCase(), foundGguf);
            log.debug("Mapping hinzugefügt: {} → {}", ollamaName, foundGguf);
        }
    }

    /**
     * Sucht nach einer GGUF-Datei die zum Basisnamen passt
     */
    private String findMatchingGguf(String baseName, String familyPrefix) {
        String lowerBase = baseName.toLowerCase().replace("-", "").replace(".", "");

        for (String gguf : availableGgufModels) {
            String lowerGguf = gguf.toLowerCase().replace("-", "").replace("_", "").replace(".", "");

            // Exakte Übereinstimmung des Basisnamens
            if (lowerGguf.contains(lowerBase)) {
                return gguf;
            }

            // Familie passt
            if (lowerGguf.contains(familyPrefix.toLowerCase())) {
                return gguf;
            }
        }
        return null;
    }

    /**
     * Konvertiert Ollama-Modellname zu GGUF-Dateipfad
     *
     * @param ollamaModel Ollama-Modellname (z.B. "qwen2.5:7b")
     * @return Voller Pfad zur GGUF-Datei oder null
     */
    public String resolveToGgufPath(String ollamaModel) {
        if (ollamaModel == null || ollamaModel.isBlank()) {
            return getDefaultGgufPath();
        }

        // Wenn bereits ein GGUF-Pfad, direkt zurückgeben
        if (ollamaModel.toLowerCase().endsWith(".gguf")) {
            if (ollamaModel.startsWith("/")) {
                return ollamaModel; // Absoluter Pfad
            }

            // Suche in allen bekannten Unterordnern nach dem GGUF-Modell
            String modelFileName = ollamaModel;
            // Falls es einen Pfad enthält (z.B. "library/model.gguf"), extrahiere nur den Dateinamen
            if (modelFileName.contains("/")) {
                modelFileName = modelFileName.substring(modelFileName.lastIndexOf("/") + 1);
            }

            // Suche in verfügbaren Modellen
            for (String availableModel : availableGgufModels) {
                if (availableModel.endsWith(modelFileName) ||
                    availableModel.toLowerCase().endsWith(modelFileName.toLowerCase())) {
                    String fullPath = modelsDir + "/" + availableModel;
                    log.debug("GGUF-Modell gefunden: {} → {}", ollamaModel, fullPath);
                    return fullPath;
                }
            }

            // Fallback: Versuche direkten Pfad (für abwärtskompatibilität)
            String directPath = modelsDir + "/" + ollamaModel;
            if (new File(directPath).exists()) {
                return directPath;
            }

            // Modell nicht gefunden, logge Warnung
            log.warn("GGUF-Modell '{}' nicht in {} gefunden. Verfügbare Modelle: {}",
                    ollamaModel, modelsDir, availableGgufModels);
            return null;
        }

        // Mapping nachschlagen
        String ggufFile = ollamaToGgufMapping.get(ollamaModel.toLowerCase());
        if (ggufFile != null) {
            return modelsDir + "/" + ggufFile;
        }

        // Fuzzy-Match versuchen (Modellname ohne Tag)
        String modelBase = ollamaModel.split(":")[0].toLowerCase();
        for (Map.Entry<String, String> entry : ollamaToGgufMapping.entrySet()) {
            if (entry.getKey().startsWith(modelBase)) {
                log.info("Fuzzy-Match für '{}': {} → {}", ollamaModel, entry.getKey(), entry.getValue());
                return modelsDir + "/" + entry.getValue();
            }
        }

        // Fallback: Suche nach ähnlichem GGUF
        String fuzzyMatch = findMatchingGguf(modelBase, modelBase);
        if (fuzzyMatch != null) {
            log.info("Fuzzy-GGUF gefunden für '{}': {}", ollamaModel, fuzzyMatch);
            return modelsDir + "/" + fuzzyMatch;
        }

        // Letzter Fallback: Standard-Modell
        log.warn("Kein GGUF-Mapping für '{}' gefunden, verwende Standard", ollamaModel);
        return getDefaultGgufPath();
    }

    /**
     * Gibt den Pfad zum Standard-GGUF-Modell zurück
     */
    public String getDefaultGgufPath() {
        if (defaultGgufModel != null) {
            return modelsDir + "/" + defaultGgufModel;
        }
        return null;
    }

    /**
     * Setzt das Standard-GGUF-Modell
     */
    public void setDefaultGgufModel(String modelFile) {
        this.defaultGgufModel = modelFile;
        log.info("Standard-GGUF-Modell geändert: {}", modelFile);
    }

    /**
     * Fügt ein manuelles Mapping hinzu
     */
    public void addCustomMapping(String ollamaName, String ggufFile) {
        ollamaToGgufMapping.put(ollamaName.toLowerCase(), ggufFile);
        log.info("Custom-Mapping hinzugefügt: {} → {}", ollamaName, ggufFile);
    }

    /**
     * Gibt alle verfügbaren GGUF-Modelle zurück
     */
    public List<String> getAvailableGgufModels() {
        return new ArrayList<>(availableGgufModels);
    }

    /**
     * Gibt alle aktuellen Mappings zurück
     */
    public Map<String, String> getAllMappings() {
        return new HashMap<>(ollamaToGgufMapping);
    }

    /**
     * Prüft ob ein Mapping für das Ollama-Modell existiert
     */
    public boolean hasMapping(String ollamaModel) {
        if (ollamaModel == null) return false;
        return ollamaToGgufMapping.containsKey(ollamaModel.toLowerCase());
    }

    /**
     * Aktualisiert die Modell-Liste (z.B. nach Download neuer Modelle)
     */
    public void refresh() {
        scanAvailableModels();
        initializeDefaultMappings();
        log.info("ModelMappingService aktualisiert: {} GGUF-Modelle, {} Mappings",
                availableGgufModels.size(), ollamaToGgufMapping.size());
    }
}
