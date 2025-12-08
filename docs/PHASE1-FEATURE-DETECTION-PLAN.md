# Phase 1: Feature Detection Implementation Plan

**Zeitaufwand:** 4-6 Stunden
**Erfolgswahrscheinlichkeit:** 80% 🟢
**Schwierigkeit:** Mittel

---

## Ziel

Provider-Features explizit machen, damit das Frontend weiß, welche Funktionen verfügbar sind und Fehler gracefully handhaben kann.

---

## Step 1: Feature Enum erstellen (15 Minuten)

**Datei:** `src/main/java/io/javafleet/fleetnavigator/llm/ProviderFeature.java`

```java
package io.javafleet.fleetnavigator.llm;

/**
 * Features that LLM providers may or may not support
 */
public enum ProviderFeature {
    /**
     * Can pull/download models from remote registry
     */
    PULL_MODEL,

    /**
     * Can delete models from local storage
     */
    DELETE_MODEL,

    /**
     * Can get detailed model information (size, params, etc.)
     */
    MODEL_DETAILS,

    /**
     * Can create custom models from Modelfile
     */
    CREATE_CUSTOM_MODEL,

    /**
     * Supports streaming responses
     */
    STREAMING,

    /**
     * Supports non-streaming (blocking) responses
     */
    BLOCKING,

    /**
     * Can list available models
     */
    LIST_MODELS,

    /**
     * Supports embeddings generation
     */
    EMBEDDINGS,

    /**
     * Supports vision/image inputs
     */
    VISION,

    /**
     * Supports function calling/tools
     */
    FUNCTION_CALLING,

    /**
     * Can configure context size per request
     */
    DYNAMIC_CONTEXT_SIZE,

    /**
     * Supports GPU acceleration
     */
    GPU_ACCELERATION
}
```

**Test:**
```bash
# Kompiliere und prüfe, dass Enum existiert
mvn compile -DskipTests
```

---

## Step 2: LLMProvider Interface erweitern (30 Minuten)

**Datei:** `src/main/java/io/javafleet/fleetnavigator/llm/LLMProvider.java`

**WICHTIG:** Füge am Ende des Interface hinzu (nicht mittendrin!):

```java
public interface LLMProvider {
    // ... existierende Methoden bleiben unverändert ...

    /**
     * Check if this provider supports a specific feature.
     *
     * @param feature The feature to check
     * @return true if supported, false otherwise
     */
    default boolean supportsFeature(ProviderFeature feature) {
        // Default: Assume all features are supported (backwards compatibility)
        return true;
    }

    /**
     * Get all features supported by this provider.
     *
     * @return Set of supported features
     */
    default Set<ProviderFeature> getSupportedFeatures() {
        // Default: Return all features (backwards compatibility)
        return EnumSet.allOf(ProviderFeature.class);
    }

    /**
     * Get human-readable name of this provider.
     *
     * @return Provider display name
     */
    default String getProviderName() {
        return getClass().getSimpleName();
    }
}
```

**Wichtig:** Vergiss nicht die Imports:
```java
import java.util.Set;
import java.util.EnumSet;
```

**Test:**
```bash
# Sollte kompilieren ohne Fehler
mvn compile -DskipTests
```

---

## Step 3: JavaLlamaCppProvider Feature Detection (45 Minuten)

**Datei:** `src/main/java/io/javafleet/fleetnavigator/llm/JavaLlamaCppProvider.java`

Füge NACH der Klassen-Deklaration hinzu:

```java
@Service
@Qualifier("java-llama-cpp")
public class JavaLlamaCppProvider implements LLMProvider {

    private static final Set<ProviderFeature> SUPPORTED_FEATURES = EnumSet.of(
        ProviderFeature.STREAMING,
        ProviderFeature.BLOCKING,
        ProviderFeature.LIST_MODELS,
        ProviderFeature.DYNAMIC_CONTEXT_SIZE,
        ProviderFeature.GPU_ACCELERATION
    );

    @Override
    public boolean supportsFeature(ProviderFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        return EnumSet.copyOf(SUPPORTED_FEATURES);
    }

    @Override
    public String getProviderName() {
        return "Java Llama.cpp (JNI)";
    }

    // ... rest der Klasse bleibt gleich ...
}
```

**Dann ERSETZE die UnsupportedOperation Methoden:**

```java
@Override
public void pullModel(String modelName) {
    if (!supportsFeature(ProviderFeature.PULL_MODEL)) {
        throw new UnsupportedOperationException(
            "java-llama.cpp does not support pulling models. " +
            "Please download GGUF models manually to: " + modelDirectory
        );
    }
}

@Override
public void deleteModel(String modelName) {
    if (!supportsFeature(ProviderFeature.DELETE_MODEL)) {
        log.warn("Model deletion not fully implemented for java-llama.cpp");
        // Optional: Delete .gguf file manually
        Path modelPath = Paths.get(modelDirectory, modelName + ".gguf");
        try {
            Files.deleteIfExists(modelPath);
            log.info("Deleted model file: {}", modelPath);
        } catch (IOException e) {
            log.error("Failed to delete model file: {}", modelPath, e);
            throw new RuntimeException("Failed to delete model: " + e.getMessage());
        }
    }
}

@Override
public Map<String, Object> getModelDetails(String modelName) {
    if (!supportsFeature(ProviderFeature.MODEL_DETAILS)) {
        // Fallback: Return basic info
        Map<String, Object> details = new HashMap<>();
        details.put("name", modelName);
        details.put("provider", "java-llama-cpp");
        details.put("type", "gguf");

        // Try to get file size
        Path modelPath = Paths.get(modelDirectory, modelName);
        if (!modelPath.toString().endsWith(".gguf")) {
            modelPath = Paths.get(modelDirectory, modelName + ".gguf");
        }

        try {
            if (Files.exists(modelPath)) {
                long size = Files.size(modelPath);
                details.put("size", size);
                details.put("size_human", formatBytes(size));
            }
        } catch (IOException e) {
            log.warn("Could not get model file size: {}", e.getMessage());
        }

        return details;
    }
    throw new UnsupportedOperationException("Full model details not implemented");
}

private String formatBytes(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp-1) + "i";
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
}
```

**Test:**
```bash
mvn compile -DskipTests
```

---

## Step 4: OllamaProvider Feature Detection (30 Minuten)

**Datei:** `src/main/java/io/javafleet/fleetnavigator/llm/OllamaProvider.java`

```java
@Service
@Qualifier("ollama")
public class OllamaProvider implements LLMProvider {

    private static final Set<ProviderFeature> SUPPORTED_FEATURES = EnumSet.of(
        ProviderFeature.PULL_MODEL,
        ProviderFeature.DELETE_MODEL,
        ProviderFeature.MODEL_DETAILS,
        ProviderFeature.CREATE_CUSTOM_MODEL,
        ProviderFeature.STREAMING,
        ProviderFeature.BLOCKING,
        ProviderFeature.LIST_MODELS,
        ProviderFeature.EMBEDDINGS,
        ProviderFeature.VISION,
        ProviderFeature.FUNCTION_CALLING
        // Ollama unterstützt fast alles!
    );

    @Override
    public boolean supportsFeature(ProviderFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        return EnumSet.copyOf(SUPPORTED_FEATURES);
    }

    @Override
    public String getProviderName() {
        return "Ollama";
    }

    // ... rest bleibt gleich ...
}
```

**Test:**
```bash
mvn compile -DskipTests
```

---

## Step 5: LlamaCppProvider Feature Detection (20 Minuten)

**Datei:** `src/main/java/io/javafleet/fleetnavigator/llm/LlamaCppProvider.java`

```java
@Service
@Qualifier("llama-cpp")
public class LlamaCppProvider implements LLMProvider {

    private static final Set<ProviderFeature> SUPPORTED_FEATURES = EnumSet.of(
        ProviderFeature.STREAMING,
        ProviderFeature.BLOCKING,
        ProviderFeature.MODEL_DETAILS,
        ProviderFeature.EMBEDDINGS
        // llama.cpp server hat begrenzte Features
    );

    @Override
    public boolean supportsFeature(ProviderFeature feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public Set<ProviderFeature> getSupportedFeatures() {
        return EnumSet.copyOf(SUPPORTED_FEATURES);
    }

    @Override
    public String getProviderName() {
        return "Llama.cpp Server";
    }

    // ... rest bleibt gleich ...
}
```

**Test:**
```bash
mvn compile -DskipTests
```

---

## Step 6: REST API Endpoint hinzufügen (45 Minuten)

**Datei:** `src/main/java/io/javafleet/fleetnavigator/controller/LLMProviderController.java`

Füge neue Endpoint-Methode hinzu:

```java
@RestController
@RequestMapping("/api/llm")
public class LLMProviderController {

    // ... existierende Felder ...

    /**
     * Get supported features for a specific provider
     */
    @GetMapping("/providers/{type}/features")
    public ResponseEntity<Map<String, Object>> getProviderFeatures(@PathVariable String type) {
        try {
            LLMProvider provider = providerService.getProvider(type);

            Map<String, Object> response = new HashMap<>();
            response.put("provider", type);
            response.put("name", provider.getProviderName());

            // Convert Set<ProviderFeature> to List<String> for JSON
            List<String> features = provider.getSupportedFeatures().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList());
            response.put("features", features);

            // Add feature-specific details
            Map<String, Boolean> featureMap = new HashMap<>();
            for (ProviderFeature feature : ProviderFeature.values()) {
                featureMap.put(feature.name(), provider.supportsFeature(feature));
            }
            response.put("featureMap", featureMap);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting provider features for {}: {}", type, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get features for all providers
     */
    @GetMapping("/providers/features")
    public ResponseEntity<Map<String, Object>> getAllProviderFeatures() {
        Map<String, Object> allFeatures = new HashMap<>();

        for (String providerType : List.of("ollama", "llama-cpp", "java-llama-cpp")) {
            try {
                LLMProvider provider = providerService.getProvider(providerType);

                Map<String, Object> providerInfo = new HashMap<>();
                providerInfo.put("name", provider.getProviderName());
                providerInfo.put("features", provider.getSupportedFeatures().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.toList()));

                allFeatures.put(providerType, providerInfo);
            } catch (Exception e) {
                log.warn("Could not get features for provider {}: {}", providerType, e.getMessage());
            }
        }

        return ResponseEntity.ok(allFeatures);
    }
}
```

Vergiss nicht die Imports:
```java
import java.util.stream.Collectors;
import io.javafleet.fleetnavigator.llm.ProviderFeature;
```

**Test:**
```bash
mvn compile -DskipTests
```

---

## Step 7: Build & Integration Test (1 Stunde)

### 7.1 Kompletter Build
```bash
mvn clean package -DskipTests
```

**Erwartung:** ✅ BUILD SUCCESS

### 7.2 Anwendung starten
```bash
java -jar target/fleet-navigator-0.2.7.jar
```

**Warte bis:** `Tomcat started on port 2025`

### 7.3 API Tests

```bash
# Test 1: Alle Provider Features
curl http://localhost:2025/api/llm/providers/features | jq

# Erwartung:
# {
#   "ollama": { "name": "Ollama", "features": [...] },
#   "java-llama-cpp": { "name": "Java Llama.cpp (JNI)", "features": [...] }
# }

# Test 2: Einzelner Provider
curl http://localhost:2025/api/llm/providers/java-llama-cpp/features | jq

# Erwartung:
# {
#   "provider": "java-llama-cpp",
#   "name": "Java Llama.cpp (JNI)",
#   "features": ["BLOCKING", "DYNAMIC_CONTEXT_SIZE", ...],
#   "featureMap": {
#     "PULL_MODEL": false,
#     "DELETE_MODEL": false,
#     "STREAMING": true,
#     ...
#   }
# }

# Test 3: Prüfe dass PULL_MODEL false ist
curl http://localhost:2025/api/llm/providers/java-llama-cpp/features | jq '.featureMap.PULL_MODEL'
# Erwartung: false

# Test 4: Prüfe dass Ollama PULL_MODEL true hat
curl http://localhost:2025/api/llm/providers/ollama/features | jq '.featureMap.PULL_MODEL'
# Erwartung: true
```

---

## Step 8: Frontend Integration (Optional - 1 Stunde)

**Datei:** `frontend/src/services/api.js`

Füge neue Funktion hinzu:

```javascript
// Get provider features
export const getProviderFeatures = async (providerType) => {
  const response = await axios.get(`/api/llm/providers/${providerType}/features`)
  return response.data
}

// Get all provider features
export const getAllProviderFeatures = async () => {
  const response = await axios.get('/api/llm/providers/features')
  return response.data
}
```

**Datei:** `frontend/src/components/ModelManager.vue`

In der `<script setup>` Section:

```javascript
import { getProviderFeatures } from '@/services/api'

const providerFeatures = ref({})

// Lade Features beim Provider-Wechsel
watch(currentProvider, async (newProvider) => {
  try {
    providerFeatures.value = await getProviderFeatures(newProvider)
  } catch (error) {
    console.error('Failed to load provider features:', error)
  }
})

// Prüfe ob Feature unterstützt wird
const supportsFeature = (feature) => {
  return providerFeatures.value?.featureMap?.[feature] === true
}
```

Im Template - bedingte Buttons:

```vue
<!-- Pull Model Button nur wenn unterstützt -->
<button
  v-if="supportsFeature('PULL_MODEL')"
  @click="pullModel(model.name)"
  class="btn btn-primary"
>
  Download
</button>

<!-- Sonst: Info-Message -->
<div v-else class="text-muted">
  <small>⚠️ Provider unterstützt kein Model-Download</small>
</div>

<!-- Delete Button nur wenn unterstützt -->
<button
  v-if="supportsFeature('DELETE_MODEL')"
  @click="deleteModel(model.name)"
  class="btn btn-danger"
>
  Löschen
</button>
```

---

## Erfolgs-Kriterien

✅ **Phase 1 ist erfolgreich wenn:**

1. Alle Provider kompilieren ohne Fehler
2. API-Endpoint `/api/llm/providers/features` liefert Daten
3. JavaLlamaCpp zeigt `PULL_MODEL: false`
4. Ollama zeigt `PULL_MODEL: true`
5. Frontend kann Features abfragen (optional)

---

## Troubleshooting

### Problem: "Symbol not found: ProviderFeature"
**Lösung:** Import fehlt:
```java
import io.javafleet.fleetnavigator.llm.ProviderFeature;
```

### Problem: "EnumSet cannot be resolved"
**Lösung:** Import fehlt:
```java
import java.util.EnumSet;
import java.util.Set;
```

### Problem: API gibt 404
**Lösung:** Controller nicht gescannt. Prüfe `@RestController` Annotation.

### Problem: Features sind immer true
**Lösung:** Provider überschreibt `supportsFeature()` nicht. Prüfe Implementation.

---

## Nächste Schritte nach Phase 1

Nach erfolgreichem Abschluss:
- ✅ Du hast Feature Detection
- ✅ Frontend kann Features abfragen
- ✅ Keine UnsupportedOperationException mehr im Frontend

**Dann bist du bereit für Phase 2:** Orphaned Data Cleanup (Problem #2)

---

**Geschätzte Gesamtzeit:** 4-6 Stunden
**Schwierigkeitsgrad:** Mittel
**Erfolgswahrscheinlichkeit:** 80% 🟢

Viel Erfolg! 🚀
