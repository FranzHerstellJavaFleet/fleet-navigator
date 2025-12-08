package io.javafleet.fleetnavigator.experts.runtime;

import io.javafleet.fleetnavigator.config.FleetPathsConfiguration;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import io.javafleet.fleetnavigator.experts.repository.ExpertRepository;
import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.providers.ExternalLlamaServerProvider;
import io.javafleet.fleetnavigator.llm.providers.JavaLlamaCppProvider;
import io.javafleet.fleetnavigator.llm.providers.OllamaProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory für ExpertRuntime-Instanzen.
 *
 * Verantwortlich für:
 * - Auflösung des richtigen LLM-Providers
 * - Auflösung des Modell-Pfades (GGUF)
 * - Caching von ExpertRuntime-Instanzen
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExpertRuntimeFactory {

    private final ExpertRepository expertRepository;
    private final FleetPathsConfiguration pathsConfig;
    private final LLMConfigProperties llmConfig;
    private final JavaLlamaCppProvider javaLlamaCppProvider;
    private final OllamaProvider ollamaProvider;
    private final ExternalLlamaServerProvider llamaServerProvider;

    // Cache für ExpertRuntime (Key: expertId_modeId_cpuOnly)
    private final Map<String, ExpertRuntime> runtimeCache = new ConcurrentHashMap<>();

    /**
     * Erstellt oder holt gecachte ExpertRuntime für einen Experten.
     *
     * @param expertId ID des Experten
     * @param modeId ID des aktiven Modus (kann null sein)
     * @param cpuOnly CPU-Only Modus
     * @return ExpertRuntime oder empty wenn Experte nicht gefunden
     */
    public Optional<ExpertRuntime> getRuntime(Long expertId, Long modeId, Boolean cpuOnly) {
        if (expertId == null) {
            return Optional.empty();
        }

        String cacheKey = buildCacheKey(expertId, modeId, cpuOnly);

        // Cache prüfen
        ExpertRuntime cached = runtimeCache.get(cacheKey);
        if (cached != null) {
            log.debug("🎓 ExpertRuntime aus Cache: {}", cached.getName());
            return Optional.of(cached);
        }

        // Expert laden
        Optional<Expert> expertOpt = expertRepository.findById(expertId);
        if (expertOpt.isEmpty()) {
            log.warn("Expert mit ID {} nicht gefunden", expertId);
            return Optional.empty();
        }

        Expert expert = expertOpt.get();

        // Aktiven Modus finden
        ExpertMode activeMode = null;
        if (modeId != null) {
            activeMode = expert.getModes().stream()
                .filter(m -> modeId.equals(m.getId()))
                .findFirst()
                .orElse(null);
        }

        // Provider auswählen
        LLMProvider provider = selectProvider(expert);

        // Modell-Pfad auflösen
        Path resolvedPath = resolveModelPath(expert, provider);

        // ExpertRuntime erstellen
        ExpertRuntime runtime = new ExpertRuntime(
            expert, activeMode, provider, resolvedPath, cpuOnly
        );

        // In Cache speichern
        runtimeCache.put(cacheKey, runtime);

        return Optional.of(runtime);
    }

    /**
     * Erstellt ExpertRuntime ohne Caching (für einmalige Verwendung)
     */
    public Optional<ExpertRuntime> createRuntime(Expert expert, ExpertMode activeMode, Boolean cpuOnly) {
        if (expert == null) {
            return Optional.empty();
        }

        LLMProvider provider = selectProvider(expert);
        Path resolvedPath = resolveModelPath(expert, provider);

        return Optional.of(new ExpertRuntime(expert, activeMode, provider, resolvedPath, cpuOnly));
    }

    /**
     * Cache leeren (z.B. nach Expert-Update)
     */
    public void clearCache() {
        runtimeCache.clear();
        log.info("ExpertRuntime Cache geleert");
    }

    /**
     * Cache für bestimmten Experten leeren
     */
    public void clearCacheForExpert(Long expertId) {
        runtimeCache.keySet().removeIf(key -> key.startsWith(expertId + "_"));
        log.debug("Cache für Expert {} geleert", expertId);
    }

    // ===== Private Hilfsmethoden =====

    private String buildCacheKey(Long expertId, Long modeId, Boolean cpuOnly) {
        return String.format("%d_%s_%s",
            expertId,
            modeId != null ? modeId : "default",
            cpuOnly != null && cpuOnly ? "cpu" : "gpu"
        );
    }

    /**
     * Wählt den passenden Provider basierend auf Expert-Konfiguration
     *
     * Priorität:
     * 1. Explizit im Expert gespeicherter providerType
     * 2. GGUF-Modell gesetzt → java-llama-cpp oder llama-server
     * 3. baseModel endet auf .gguf → java-llama-cpp oder llama-server
     * 4. Default Provider aus Config
     * 5. Fallback auf ersten verfügbaren
     */
    private LLMProvider selectProvider(Expert expert) {
        // 1. PRIORITÄT: Expliziter Provider im Expert
        String providerType = expert.getProviderType();
        if (providerType != null && !providerType.isBlank()) {
            LLMProvider explicit = getProviderByType(providerType);
            if (explicit != null && explicit.isAvailable()) {
                log.debug("🎓 Provider für {}: {} (explizit gespeichert)", expert.getName(), providerType);
                return explicit;
            }
            log.warn("🎓 Expliziter Provider '{}' für {} nicht verfügbar, verwende Fallback",
                providerType, expert.getName());
        }

        // 2. Hat der Expert ein explizites GGUF-Modell? → llama-server (bevorzugt) oder java-llama-cpp
        if (expert.getGgufModel() != null && !expert.getGgufModel().isBlank()) {
            if (llamaServerProvider.isAvailable()) {
                log.debug("🎓 Provider für {}: llama-server (GGUF explizit)", expert.getName());
                return llamaServerProvider;
            }
            if (javaLlamaCppProvider.isAvailable()) {
                log.debug("🎓 Provider für {}: java-llama-cpp (GGUF explizit)", expert.getName());
                return javaLlamaCppProvider;
            }
        }

        // 3. Prüfe ob das baseModel ein GGUF-Dateiname ist
        String baseModel = expert.getBaseModel();
        if (baseModel != null && baseModel.toLowerCase().endsWith(".gguf")) {
            if (llamaServerProvider.isAvailable()) {
                log.debug("🎓 Provider für {}: llama-server (baseModel ist GGUF)", expert.getName());
                return llamaServerProvider;
            }
            if (javaLlamaCppProvider.isAvailable()) {
                log.debug("🎓 Provider für {}: java-llama-cpp (baseModel ist GGUF)", expert.getName());
                return javaLlamaCppProvider;
            }
        }

        // 4. Default Provider aus Config
        String defaultProvider = llmConfig.getDefaultProvider();
        LLMProvider defaultProv = getProviderByType(defaultProvider);
        if (defaultProv != null && defaultProv.isAvailable()) {
            log.debug("🎓 Provider für {}: {} (Default)", expert.getName(), defaultProvider);
            return defaultProv;
        }

        // 5. Fallback: Ersten verfügbaren Provider nehmen
        if (llamaServerProvider.isAvailable()) {
            log.debug("🎓 Provider für {}: llama-server (Fallback)", expert.getName());
            return llamaServerProvider;
        }
        if (javaLlamaCppProvider.isAvailable()) {
            log.debug("🎓 Provider für {}: java-llama-cpp (Fallback)", expert.getName());
            return javaLlamaCppProvider;
        }

        log.debug("🎓 Provider für {}: ollama (Fallback)", expert.getName());
        return ollamaProvider;
    }

    /**
     * Gibt Provider anhand des Typ-Strings zurück
     */
    private LLMProvider getProviderByType(String providerType) {
        if (providerType == null) return null;

        return switch (providerType.toLowerCase()) {
            case "llama-server" -> llamaServerProvider;
            case "java-llama-cpp" -> javaLlamaCppProvider;
            case "ollama" -> ollamaProvider;
            default -> null;
        };
    }

    /**
     * Löst den Modell-Pfad auf (für GGUF-Modelle)
     */
    private Path resolveModelPath(Expert expert, LLMProvider provider) {
        // Für java-llama-cpp und llama-server relevant
        String providerName = provider.getProviderName();
        if (!"java-llama-cpp".equals(providerName) && !"llama-server".equals(providerName)) {
            return null;
        }

        Path modelsDir = pathsConfig.getResolvedModelsDir();
        String modelName = expert.getGgufModel();

        // Fallback auf baseModel wenn kein ggufModel gesetzt
        if (modelName == null || modelName.isBlank()) {
            modelName = expert.getBaseModel();
        }

        if (modelName == null || modelName.isBlank()) {
            log.warn("Kein Modell für Expert {} konfiguriert", expert.getName());
            return null;
        }

        // Verschiedene Pfade versuchen
        Path[] candidates = {
            modelsDir.resolve(modelName),
            modelsDir.resolve("library").resolve(modelName),
            modelsDir.resolve("custom").resolve(modelName)
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                try {
                    // Symlinks auflösen für native Bibliothek (llama.cpp)
                    Path realPath = candidate.toRealPath();
                    log.info("✅ Modell gefunden für {}: {} → {}", expert.getName(), candidate, realPath);
                    return realPath;
                } catch (IOException e) {
                    log.warn("⚠️ Konnte Symlink nicht auflösen für {}: {}", candidate, e.getMessage());
                    return candidate;
                }
            }
        }

        // Letzte Chance: Suche mit Teil-Match
        Path found = searchModelFile(modelsDir, modelName);
        if (found != null) {
            try {
                Path realPath = found.toRealPath();
                log.info("✅ Modell gefunden (Fuzzy) für {}: {} → {}", expert.getName(), found, realPath);
                return realPath;
            } catch (IOException e) {
                log.info("✅ Modell gefunden (Fuzzy) für {}: {}", expert.getName(), found);
                return found;
            }
        }

        log.warn("⚠️ Modell nicht gefunden für {}: {} in {}", expert.getName(), modelName, modelsDir);
        return null;
    }

    /**
     * Sucht nach einem Modell mit Teil-Match
     */
    private Path searchModelFile(Path modelsDir, String modelName) {
        try {
            // Extrahiere Basis-Namen ohne Pfad
            String baseName = modelName;
            if (baseName.contains("/")) {
                baseName = baseName.substring(baseName.lastIndexOf("/") + 1);
            }

            String searchName = baseName.toLowerCase();

            // Suche in library und custom
            for (String subdir : new String[]{"library", "custom", ""}) {
                Path searchDir = subdir.isEmpty() ? modelsDir : modelsDir.resolve(subdir);
                if (!Files.exists(searchDir)) continue;

                var found = Files.list(searchDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".gguf"))
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(
                        searchName.replace(".gguf", "").toLowerCase()))
                    .findFirst();

                if (found.isPresent()) {
                    return found.get();
                }
            }
        } catch (Exception e) {
            log.warn("Fehler bei Modell-Suche: {}", e.getMessage());
        }

        return null;
    }
}
