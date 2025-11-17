# Fleet Navigator Model Store - Integriertes Modell-Management

**Version:** 1.0  
**Datum:** 2025-11-09  
**Ergänzung zu:** LLM-PROVIDER-SYSTEM.md

---

## 🎯 Vision: "App Store für AI Modelle"

Statt User zu externen Websites zu schicken, bietet Fleet Navigator einen **integrierten Model Store** - ähnlich wie Apple App Store oder Google Play Store.

### User Experience:

```
❌ VORHER (kompliziert):
1. Zu Hugging Face gehen
2. Nach GGUF suchen
3. Richtiges Modell finden
4. Quantisierung wählen
5. 2GB herunterladen
6. In Fleet Navigator importieren

✅ NACHHER (einfach):
1. Settings → Model Store öffnen
2. Modell auswählen
3. "Herunterladen" klicken
4. Fertig! ☕
```

---

## 🏪 Model Store Architektur

### UI Konzept

```
┌────────────────────────────────────────────────────────────┐
│  Fleet Navigator - Model Store                   🔍 Suche  │
├────────────────────────────────────────────────────────────┤
│                                                              │
│  📦 Empfohlene Modelle                           [Filter ▼]│
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ Llama 3.2 3B    │  │ Qwen 2.5 7B     │               │
│  │ ⭐⭐⭐⭐⭐ (4.8)    │  │ ⭐⭐⭐⭐⭐ (4.9)    │               │
│  │ 2.0 GB          │  │ 4.4 GB          │               │
│  │ Schnell, Chat   │  │ Multilingual    │               │
│  │ [Herunterladen] │  │ [✓ Installiert] │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                              │
│  📚 Kategorien                                              │
│  • Chat & Assistenten (156)                                │
│  • Code-Generierung (87)                                   │
│  • Mehrsprachig (234)                                      │
│  • Spezialisiert (312)                                     │
│                                                              │
│  🔥 Trending                                                │
│  • DeepSeek R1 - Reasoning Model                          │
│  • Gemma 2 27B - Google's Latest                          │
│  • Mistral 7B v0.3 - Updated                              │
│                                                              │
└────────────────────────────────────────────────────────────┘
```

---

## 🌐 Hugging Face API Integration

### REST API Endpoints

Hugging Face bietet eine **vollständige REST API** die wir nutzen können:

```bash
# Basis-URL
https://huggingface.co/api

# Wichtige Endpoints:
GET /api/models                    # Liste aller Modelle
GET /api/models?search=llama       # Suche nach Modellen
GET /api/models?filter=gguf        # Nach GGUF filtern
GET /api/models/{model_id}         # Modell-Details
```

### Vorteile der API-Integration

✅ **Immer aktuell** - Neueste Modelle automatisch verfügbar
✅ **Keine manuelle Pflege** - API liefert Metadaten
✅ **Community-Ratings** - Downloads, Likes automatisch
✅ **Keine Website nötig** - Alles programmatisch

### Hybrid-Ansatz: API + Kuratierung

**Beste Lösung:** API für Daten + Kuratierte Auswahl

```
┌─────────────────────────────────────────────────┐
│  Hugging Face API (39.600+ Modelle)            │
│  - Aktuelle Metadaten                           │
│  - Download-Zahlen                              │
│  - Größen, Tags, etc.                           │
└─────────────────────────────────────────────────┘
                    ↓ Filter
┌─────────────────────────────────────────────────┐
│  Fleet Navigator Kuratierung                    │
│  - Nur geprüfte Modelle                         │
│  - Deutsche Beschreibungen                      │
│  - Hardware-Anforderungen                       │
│  - Use-Case Kategorien                          │
└─────────────────────────────────────────────────┘
                    ↓ Result
┌─────────────────────────────────────────────────┐
│  Model Store UI                                 │
│  15-20 Featured + Alle durchsuchbar             │
└─────────────────────────────────────────────────┘
```

---

## 🗄️ Model Registry (Backend)

### Dynamische Registry mit HF API

```java
package io.javafleet.fleetnavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service für Hugging Face API Integration
 * 
 * Native Image Safe:
 * - WebClient statt RestTemplate
 * - Keine Jackson Reflection Probleme
 * - Caching für Performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceApiService {
    
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;
    
    // Cache für API-Responses (1 Stunde)
    private final Map<String, CachedResponse> cache = new HashMap<>();
    
    private static final String HF_API_BASE = "https://huggingface.co/api";
    private static final long CACHE_TTL_MS = 3600000; // 1 Stunde
    
    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
            .baseUrl(HF_API_BASE)
            .build();
    }
    
    /**
     * Suche GGUF-Modelle auf Hugging Face
     */
    public List<HFModelInfo> searchGGUFModels(String query, int limit) {
        String cacheKey = "search:" + query + ":" + limit;
        
        // Check Cache
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.getData();
        }
        
        // API Call
        List<HFModelInfo> models = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/models")
                .queryParam("search", query)
                .queryParam("filter", "gguf")
                .queryParam("sort", "downloads")
                .queryParam("direction", "-1")
                .queryParam("limit", limit)
                .build())
            .retrieve()
            .bodyToFlux(HFModelInfo.class)
            .collectList()
            .block();
        
        // Cache Result
        cache.put(cacheKey, new CachedResponse(models));
        
        return models != null ? models : List.of();
    }
    
    /**
     * Modell-Details abrufen
     */
    public HFModelDetails getModelDetails(String modelId) {
        String cacheKey = "details:" + modelId;
        
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (HFModelDetails) cached.getData();
        }
        
        HFModelDetails details = webClient.get()
            .uri("/models/{modelId}", modelId)
            .retrieve()
            .bodyToMono(HFModelDetails.class)
            .block();
        
        cache.put(cacheKey, new CachedResponse(details));
        return details;
    }
    
    /**
     * Download-URL für GGUF-File erstellen
     */
    public String getDownloadUrl(String repoId, String filename) {
        return String.format(
            "https://huggingface.co/%s/resolve/main/%s",
            repoId,
            filename
        );
    }
    
    /**
     * Trending/Featured Modelle von HF
     */
    public List<HFModelInfo> getTrendingModels() {
        return searchGGUFModels("", 20).stream()
            .sorted(Comparator.comparing(HFModelInfo::getDownloads).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }
    
    // ===== DTOs =====
    
    @Data
    public static class HFModelInfo {
        private String id;              // "bartowski/Llama-3.2-3B-Instruct-GGUF"
        private String modelId;         // Same as id
        private String author;          // "bartowski"
        private Long downloads;         // 125000
        private Integer likes;          // 234
        private List<String> tags;      // ["gguf", "llama", "text-generation"]
        private String pipeline_tag;    // "text-generation"
        private Long lastModified;      // Unix timestamp
    }
    
    @Data
    public static class HFModelDetails {
        private String id;
        private String modelId;
        private Map<String, Object> cardData;  // README.md parsed data
        private List<String> tags;
        private Long downloads;
        private Integer likes;
        private List<HFModelFile> siblings;    // Dateien im Repo
    }
    
    @Data
    public static class HFModelFile {
        private String rfilename;       // Filename
        private Long size;              // Bytes
    }
    
    // Cache Helper
    private static class CachedResponse {
        private final Object data;
        private final long timestamp;
        
        CachedResponse(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
        
        @SuppressWarnings("unchecked")
        <T> T getData() {
            return (T) data;
        }
    }
}
```

### Kuratierte Modell-Liste

Statt alles von HF zu zeigen, definieren wir **welche Modelle wir featuren**:

```java
package io.javafleet.fleetnavigator.models;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Kuratierte Modell-Registry
 * 
 * WICHTIG für Native Image:
 * - Statische Initialisierung (keine Runtime-Reflection)
 * - Compile-Time bekannte Liste
 */
@Data
@Builder
public class ModelRegistryEntry {
    
    private String id;                    // "llama-3.2-3b-instruct"
    private String displayName;           // "Llama 3.2 (3B) - Instruct"
    private String provider;              // Meta AI
    private String architecture;          // llama
    private String version;               // 3.2
    private String parameterSize;         // 3B
    private String quantization;          // Q4_K_M
    
    // Download Info
    private String huggingFaceRepo;       // "bartowski/Llama-3.2-3B-Instruct-GGUF"
    private String filename;              // "Llama-3.2-3B-Instruct-Q4_K_M.gguf"
    private Long sizeBytes;               // 2147483648 (2GB)
    private String sizeHuman;             // "2.0 GB"
    
    // Metadata
    private String description;           // "Fast and efficient general-purpose..."
    private List<String> tags;            // ["chat", "instruct", "english"]
    private List<String> useCases;        // ["Chat", "Q&A", "Instructions"]
    private String license;               // "Llama 3.2 License"
    private Float rating;                 // 4.8
    private Integer downloads;            // 125000
    
    // Requirements
    private Integer minRamGB;             // 4
    private Integer recommendedRamGB;     // 8
    private Boolean gpuAccelSupported;    // true
    
    // Status
    private boolean featured;             // Auf Startseite zeigen?
    private boolean trending;             // Im Trending-Bereich?
    private String category;              // "chat", "code", "multilingual"
}
```

### Statische Model Registry

```java
package io.javafleet.fleetnavigator.models;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Zentrale Registry aller verfügbaren Modelle
 * 
 * Native Image Safe:
 * - Statische Initialisierung
 * - Keine Reflection
 * - Compile-Time bekannt
 */
@Component
public class ModelRegistry {
    
    private static final List<ModelRegistryEntry> MODELS = new ArrayList<>();
    
    static {
        // ===== CHAT & ASSISTENTEN =====
        
        MODELS.add(ModelRegistryEntry.builder()
            .id("llama-3.2-3b-instruct")
            .displayName("Llama 3.2 (3B) - Instruct")
            .provider("Meta AI")
            .architecture("llama")
            .version("3.2")
            .parameterSize("3B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("bartowski/Llama-3.2-3B-Instruct-GGUF")
            .filename("Llama-3.2-3B-Instruct-Q4_K_M.gguf")
            .sizeBytes(2_097_152_000L) // 2.0 GB
            .sizeHuman("2.0 GB")
            .description("Schnelles und effizientes Modell für alltägliche Chat-Aufgaben. " +
                        "Ideal für Briefe, E-Mails und allgemeine Fragen.")
            .tags(List.of("chat", "instruct", "english", "german"))
            .useCases(List.of("Chat", "Briefe schreiben", "Q&A", "Übersetzungen"))
            .license("Llama 3.2 Community License")
            .rating(4.8f)
            .downloads(125000)
            .minRamGB(4)
            .recommendedRamGB(8)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("chat")
            .build()
        );
        
        MODELS.add(ModelRegistryEntry.builder()
            .id("qwen2.5-7b-instruct")
            .displayName("Qwen 2.5 (7B) - Instruct")
            .provider("Alibaba Cloud")
            .architecture("qwen2")
            .version("2.5")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("Qwen/Qwen2.5-7B-Instruct-GGUF")
            .filename("qwen2.5-7b-instruct-q4_k_m.gguf")
            .sizeBytes(4_400_000_000L) // 4.4 GB
            .sizeHuman("4.4 GB")
            .description("Exzellentes mehrsprachiges Modell mit hervorragender Qualität. " +
                        "Besonders stark bei Code, Mathe und mehrsprachigen Aufgaben.")
            .tags(List.of("chat", "multilingual", "code", "math"))
            .useCases(List.of("Mehrsprachiger Chat", "Code", "Mathematik", "Analyse"))
            .license("Apache 2.0")
            .rating(4.9f)
            .downloads(89000)
            .minRamGB(6)
            .recommendedRamGB(16)
            .gpuAccelSupported(true)
            .featured(true)
            .trending(true)
            .category("chat")
            .build()
        );
        
        MODELS.add(ModelRegistryEntry.builder()
            .id("phi-3-mini-4k-instruct")
            .displayName("Phi-3 Mini (3.8B) - Instruct")
            .provider("Microsoft")
            .architecture("phi3")
            .version("3")
            .parameterSize("3.8B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("microsoft/Phi-3-mini-4k-instruct-gguf")
            .filename("Phi-3-mini-4k-instruct-q4.gguf")
            .sizeBytes(2_300_000_000L) // 2.3 GB
            .sizeHuman("2.3 GB")
            .description("Kompaktes und leistungsstarkes Modell von Microsoft. " +
                        "Trotz kleiner Größe beeindruckende Leistung.")
            .tags(List.of("chat", "efficient", "microsoft"))
            .useCases(List.of("Chat", "Q&A", "Zusammenfassungen"))
            .license("MIT")
            .rating(4.6f)
            .downloads(67000)
            .minRamGB(4)
            .recommendedRamGB(8)
            .gpuAccelSupported(true)
            .featured(true)
            .category("chat")
            .build()
        );
        
        // ===== CODE-GENERIERUNG =====
        
        MODELS.add(ModelRegistryEntry.builder()
            .id("codellama-7b-instruct")
            .displayName("Code Llama (7B) - Instruct")
            .provider("Meta AI")
            .architecture("llama")
            .version("1")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("TheBloke/CodeLlama-7B-Instruct-GGUF")
            .filename("codellama-7b-instruct.Q4_K_M.gguf")
            .sizeBytes(4_200_000_000L)
            .sizeHuman("4.2 GB")
            .description("Spezialisiert auf Code-Generierung und Programmier-Aufgaben. " +
                        "Unterstützt viele Programmiersprachen.")
            .tags(List.of("code", "programming", "python", "java"))
            .useCases(List.of("Code schreiben", "Debugging", "Code-Erklärung"))
            .license("Llama 2 License")
            .rating(4.7f)
            .downloads(54000)
            .minRamGB(6)
            .recommendedRamGB(16)
            .gpuAccelSupported(true)
            .featured(true)
            .category("code")
            .build()
        );
        
        MODELS.add(ModelRegistryEntry.builder()
            .id("deepseek-coder-6.7b-instruct")
            .displayName("DeepSeek Coder (6.7B) - Instruct")
            .provider("DeepSeek")
            .architecture("deepseek")
            .version("1")
            .parameterSize("6.7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("TheBloke/deepseek-coder-6.7B-instruct-GGUF")
            .filename("deepseek-coder-6.7b-instruct.Q4_K_M.gguf")
            .sizeBytes(4_000_000_000L)
            .sizeHuman("4.0 GB")
            .description("Hochspezialisiertes Coding-Modell mit exzellenter Code-Qualität. " +
                        "Besonders stark bei Python, Java, JavaScript.")
            .tags(List.of("code", "specialized", "python", "java", "javascript"))
            .useCases(List.of("Code-Generierung", "Refactoring", "Documentation"))
            .license("DeepSeek License")
            .rating(4.8f)
            .downloads(43000)
            .minRamGB(6)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .category("code")
            .build()
        );
        
        // ===== MEHRSPRACHIG =====
        
        MODELS.add(ModelRegistryEntry.builder()
            .id("aya-23-8b")
            .displayName("Aya 23 (8B)")
            .provider("Cohere For AI")
            .architecture("command-r")
            .version("23")
            .parameterSize("8B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("CohereForAI/aya-23-8B-GGUF")
            .filename("aya-23-8b-q4_k_m.gguf")
            .sizeBytes(4_800_000_000L)
            .sizeHuman("4.8 GB")
            .description("Mehrsprachiges Modell mit Unterstützung für 23 Sprachen. " +
                        "Besonders stark bei nicht-englischen Sprachen.")
            .tags(List.of("multilingual", "101languages", "translation"))
            .useCases(List.of("Übersetzung", "Mehrsprachiger Chat", "Lokalisierung"))
            .license("Apache 2.0")
            .rating(4.6f)
            .downloads(31000)
            .minRamGB(6)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .category("multilingual")
            .build()
        );
        
        // ===== SPEZIALISIERT =====
        
        MODELS.add(ModelRegistryEntry.builder()
            .id("mistral-7b-instruct-v0.3")
            .displayName("Mistral 7B v0.3 - Instruct")
            .provider("Mistral AI")
            .architecture("mistral")
            .version("0.3")
            .parameterSize("7B")
            .quantization("Q4_K_M")
            .huggingFaceRepo("TheBloke/Mistral-7B-Instruct-v0.3-GGUF")
            .filename("mistral-7b-instruct-v0.3.Q4_K_M.gguf")
            .sizeBytes(4_100_000_000L)
            .sizeHuman("4.1 GB")
            .description("Balanced und vielseitiges Modell. Gutes Preis-Leistungs-Verhältnis " +
                        "für verschiedene Aufgaben.")
            .tags(List.of("chat", "balanced", "efficient"))
            .useCases(List.of("Chat", "Analyse", "Zusammenfassungen"))
            .license("Apache 2.0")
            .rating(4.7f)
            .downloads(98000)
            .minRamGB(6)
            .recommendedRamGB(12)
            .gpuAccelSupported(true)
            .featured(true)
            .category("chat")
            .build()
        );
    }
    
    /**
     * Alle Modelle zurückgeben
     */
    public List<ModelRegistryEntry> getAllModels() {
        return new ArrayList<>(MODELS);
    }
    
    /**
     * Featured Modelle (Startseite)
     */
    public List<ModelRegistryEntry> getFeaturedModels() {
        return MODELS.stream()
            .filter(ModelRegistryEntry::isFeatured)
            .limit(6)
            .toList();
    }
    
    /**
     * Trending Modelle
     */
    public List<ModelRegistryEntry> getTrendingModels() {
        return MODELS.stream()
            .filter(ModelRegistryEntry::isTrending)
            .sorted(Comparator.comparing(ModelRegistryEntry::getDownloads).reversed())
            .limit(5)
            .toList();
    }
    
    /**
     * Nach Kategorie filtern
     */
    public List<ModelRegistryEntry> getByCategory(String category) {
        return MODELS.stream()
            .filter(m -> m.getCategory().equalsIgnoreCase(category))
            .toList();
    }
    
    /**
     * Nach RAM-Requirements filtern
     */
    public List<ModelRegistryEntry> getByMaxRam(int maxRamGB) {
        return MODELS.stream()
            .filter(m -> m.getMinRamGB() <= maxRamGB)
            .toList();
    }
    
    /**
     * Modell nach ID finden
     */
    public Optional<ModelRegistryEntry> findById(String id) {
        return MODELS.stream()
            .filter(m -> m.getId().equals(id))
            .findFirst();
    }
    
    /**
     * Suche
     */
    public List<ModelRegistryEntry> search(String query) {
        String lowerQuery = query.toLowerCase();
        return MODELS.stream()
            .filter(m -> 
                m.getDisplayName().toLowerCase().contains(lowerQuery) ||
                m.getDescription().toLowerCase().contains(lowerQuery) ||
                m.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))
            )
            .toList();
    }
}
```

---

## 📥 Model Download Service

### Download-Manager mit Progress

```java
package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.models.ModelRegistry;
import io.javafleet.fleetnavigator.models.ModelRegistryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service für Modell-Downloads von Hugging Face
 * 
 * Native Image Safe:
 * - WebClient statt RestTemplate
 * - Reactive Streams
 * - Keine Reflection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelDownloadService {
    
    private final ModelRegistry modelRegistry;
    private final LLMConfigProperties config;
    private final WebClient.Builder webClientBuilder;
    
    // Download-Progress Tracking
    private final Map<String, DownloadProgress> activeDownloads = new ConcurrentHashMap<>();
    
    /**
     * Startet Download eines Modells
     * 
     * @param modelId ID aus Registry
     * @return Reactive Stream mit Progress-Updates
     */
    public Flux<DownloadProgress> downloadModel(String modelId) {
        
        // 1. Modell aus Registry holen
        ModelRegistryEntry model = modelRegistry.findById(modelId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown model: " + modelId));
        
        // 2. Prüfe ob bereits heruntergeladen
        Path targetPath = getModelPath(model.getFilename());
        if (Files.exists(targetPath)) {
            return Flux.just(DownloadProgress.completed(modelId));
        }
        
        // 3. Erstelle Download-URL
        String downloadUrl = buildHuggingFaceUrl(
            model.getHuggingFaceRepo(),
            model.getFilename()
        );
        
        log.info("📥 Starting download: {} from {}", model.getDisplayName(), downloadUrl);
        
        // 4. Download mit Progress Tracking
        return downloadWithProgress(modelId, downloadUrl, targetPath, model.getSizeBytes());
    }
    
    /**
     * Download mit Progress-Tracking
     */
    private Flux<DownloadProgress> downloadWithProgress(
        String modelId,
        String url,
        Path targetPath,
        Long totalBytes
    ) {
        return Flux.create(sink -> {
            
            // Initialize Progress
            DownloadProgress progress = new DownloadProgress(modelId, totalBytes);
            activeDownloads.put(modelId, progress);
            
            WebClient client = webClientBuilder.build();
            
            client.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(byte[].class)
                .doOnNext(bytes -> {
                    try {
                        // Schreibe in Datei
                        Files.write(
                            targetPath,
                            bytes,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND
                        );
                        
                        // Update Progress
                        progress.addDownloadedBytes(bytes.length);
                        sink.next(progress);
                        
                    } catch (IOException e) {
                        sink.error(e);
                    }
                })
                .doOnComplete(() -> {
                    progress.setStatus(DownloadStatus.COMPLETED);
                    sink.next(progress);
                    sink.complete();
                    activeDownloads.remove(modelId);
                    
                    log.info("✅ Download completed: {}", modelId);
                })
                .doOnError(error -> {
                    progress.setStatus(DownloadStatus.FAILED);
                    progress.setError(error.getMessage());
                    sink.next(progress);
                    sink.error(error);
                    activeDownloads.remove(modelId);
                    
                    log.error("❌ Download failed: {}", modelId, error);
                })
                .subscribe();
        });
    }
    
    /**
     * Hugging Face Download-URL erstellen
     */
    private String buildHuggingFaceUrl(String repo, String filename) {
        return String.format(
            "https://huggingface.co/%s/resolve/main/%s",
            repo,
            filename
        );
    }
    
    /**
     * Zielpfad für Modell
     */
    private Path getModelPath(String filename) {
        return Paths.get(config.getLlamacpp().getModelsDir(), filename);
    }
    
    /**
     * Download-Progress abrufen
     */
    public DownloadProgress getProgress(String modelId) {
        return activeDownloads.get(modelId);
    }
    
    /**
     * Alle aktiven Downloads
     */
    public Map<String, DownloadProgress> getActiveDownloads() {
        return new ConcurrentHashMap<>(activeDownloads);
    }
    
    /**
     * Download abbrechen
     */
    public void cancelDownload(String modelId) {
        DownloadProgress progress = activeDownloads.get(modelId);
        if (progress != null) {
            progress.setStatus(DownloadStatus.CANCELLED);
            activeDownloads.remove(modelId);
        }
    }
}

/**
 * Download Progress DTO
 */
@Data
public class DownloadProgress {
    private String modelId;
    private DownloadStatus status;
    private Long totalBytes;
    private Long downloadedBytes;
    private Integer percentComplete;
    private Long downloadSpeedBytesPerSec;
    private Long estimatedSecondsRemaining;
    private String error;
    
    public DownloadProgress(String modelId, Long totalBytes) {
        this.modelId = modelId;
        this.totalBytes = totalBytes;
        this.downloadedBytes = 0L;
        this.percentComplete = 0;
        this.status = DownloadStatus.DOWNLOADING;
    }
    
    public void addDownloadedBytes(int bytes) {
        this.downloadedBytes += bytes;
        if (totalBytes != null && totalBytes > 0) {
            this.percentComplete = (int) ((downloadedBytes * 100) / totalBytes);
        }
    }
    
    public static DownloadProgress completed(String modelId) {
        DownloadProgress p = new DownloadProgress(modelId, 0L);
        p.setStatus(DownloadStatus.COMPLETED);
        p.setPercentComplete(100);
        return p;
    }
}

enum DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

---

## 🎨 Frontend: Model Store UI

### Model Store Component

```vue
<template>
  <div class="model-store">
    
    <!-- Header mit Suche -->
    <div class="store-header">
      <h2>🏪 Model Store</h2>
      <div class="search-bar">
        <input 
          v-model="searchQuery"
          @input="handleSearch"
          placeholder="Modelle durchsuchen..."
          class="search-input"
        />
        <select v-model="filterCategory" class="category-filter">
          <option value="">Alle Kategorien</option>
          <option value="chat">Chat & Assistenten</option>
          <option value="code">Code-Generierung</option>
          <option value="multilingual">Mehrsprachig</option>
        </select>
        
        <select v-model="filterRam" class="ram-filter">
          <option value="0">Alle RAM-Größen</option>
          <option value="4">Max 4 GB RAM</option>
          <option value="8">Max 8 GB RAM</option>
          <option value="16">Max 16 GB RAM</option>
        </select>
      </div>
    </div>
    
    <!-- Featured Models -->
    <section v-if="!searchQuery" class="featured-section">
      <h3>⭐ Empfohlene Modelle</h3>
      <div class="model-grid">
        <ModelCard 
          v-for="model in featuredModels"
          :key="model.id"
          :model="model"
          :installed="isInstalled(model.id)"
          :downloading="isDownloading(model.id)"
          :progress="getDownloadProgress(model.id)"
          @download="startDownload"
          @cancel="cancelDownload"
        />
      </div>
    </section>
    
    <!-- Search Results / All Models -->
    <section class="all-models-section">
      <h3 v-if="searchQuery">
        🔍 Suchergebnisse für "{{ searchQuery }}" ({{ filteredModels.length }})
      </h3>
      <h3 v-else>
        📚 Alle Modelle ({{ filteredModels.length }})
      </h3>
      
      <div class="model-grid">
        <ModelCard 
          v-for="model in filteredModels"
          :key="model.id"
          :model="model"
          :installed="isInstalled(model.id)"
          :downloading="isDownloading(model.id)"
          :progress="getDownloadProgress(model.id)"
          @download="startDownload"
          @cancel="cancelDownload"
        />
      </div>
    </section>
    
    <!-- Active Downloads Panel -->
    <div v-if="Object.keys(activeDownloads).length > 0" class="downloads-panel">
      <h4>📥 Aktive Downloads</h4>
      <DownloadProgress 
        v-for="(progress, modelId) in activeDownloads"
        :key="modelId"
        :progress="progress"
        @cancel="cancelDownload(modelId)"
      />
    </div>
    
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import ModelCard from './ModelCard.vue';
import DownloadProgress from './DownloadProgress.vue';

const searchQuery = ref('');
const filterCategory = ref('');
const filterRam = ref(0);

const allModels = ref([]);
const featuredModels = ref([]);
const installedModels = ref([]);
const activeDownloads = ref({});

let downloadEventSource = null;

onMounted(async () => {
  await loadModels();
  await loadInstalledModels();
});

onUnmounted(() => {
  if (downloadEventSource) {
    downloadEventSource.close();
  }
});

async function loadModels() {
  const response = await axios.get('/api/model-store/all');
  allModels.value = response.data;
  
  const featuredResponse = await axios.get('/api/model-store/featured');
  featuredModels.value = featuredResponse.data;
}

async function loadInstalledModels() {
  const response = await axios.get('/api/llm/models');
  installedModels.value = response.data.map(m => m.name);
}

const filteredModels = computed(() => {
  let models = allModels.value;
  
  // Category Filter
  if (filterCategory.value) {
    models = models.filter(m => m.category === filterCategory.value);
  }
  
  // RAM Filter
  if (filterRam.value > 0) {
    models = models.filter(m => m.minRamGB <= filterRam.value);
  }
  
  // Search
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    models = models.filter(m => 
      m.displayName.toLowerCase().includes(query) ||
      m.description.toLowerCase().includes(query) ||
      m.tags.some(tag => tag.toLowerCase().includes(query))
    );
  }
  
  return models;
});

async function startDownload(modelId) {
  // Start download via SSE
  downloadEventSource = new EventSource(`/api/model-store/download/${modelId}`);
  
  downloadEventSource.addEventListener('progress', (event) => {
    const progress = JSON.parse(event.data);
    activeDownloads.value[modelId] = progress;
  });
  
  downloadEventSource.addEventListener('complete', (event) => {
    delete activeDownloads.value[modelId];
    downloadEventSource.close();
    loadInstalledModels(); // Refresh
    
    // Success notification
    showNotification(`✅ ${modelId} erfolgreich heruntergeladen!`);
  });
  
  downloadEventSource.addEventListener('error', (event) => {
    delete activeDownloads.value[modelId];
    downloadEventSource.close();
    
    showNotification(`❌ Download fehlgeschlagen: ${modelId}`);
  });
}

async function cancelDownload(modelId) {
  await axios.post(`/api/model-store/download/${modelId}/cancel`);
  delete activeDownloads.value[modelId];
  if (downloadEventSource) {
    downloadEventSource.close();
  }
}

function isInstalled(modelId) {
  return installedModels.value.includes(modelId);
}

function isDownloading(modelId) {
  return modelId in activeDownloads.value;
}

function getDownloadProgress(modelId) {
  return activeDownloads.value[modelId];
}

function handleSearch() {
  // Debounced search
}

function showNotification(message) {
  // Toast notification
}
</script>

<style scoped>
.model-store {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.store-header {
  margin-bottom: 40px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.search-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
}

.category-filter, .ram-filter {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.downloads-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-width: 400px;
  z-index: 1000;
}
</style>
```

### ModelCard Component

```vue
<template>
  <div class="model-card">
    <div class="model-header">
      <h4>{{ model.displayName }}</h4>
      <span class="rating">⭐ {{ model.rating }}</span>
    </div>
    
    <div class="model-meta">
      <span class="size">📦 {{ model.sizeHuman }}</span>
      <span class="downloads">⬇️ {{ formatDownloads(model.downloads) }}</span>
    </div>
    
    <p class="model-description">{{ model.description }}</p>
    
    <div class="model-tags">
      <span v-for="tag in model.tags.slice(0, 3)" :key="tag" class="tag">
        {{ tag }}
      </span>
    </div>
    
    <div class="model-requirements">
      <span>💾 Min {{ model.minRamGB }} GB RAM</span>
      <span v-if="model.gpuAccelSupported">🎮 GPU Support</span>
    </div>
    
    <!-- Actions -->
    <div class="model-actions">
      <!-- Already Installed -->
      <button v-if="installed" class="btn-installed" disabled>
        ✓ Installiert
      </button>
      
      <!-- Downloading -->
      <div v-else-if="downloading" class="downloading">
        <div class="progress-bar">
          <div 
            class="progress-fill"
            :style="{ width: progress.percentComplete + '%' }"
          />
        </div>
        <div class="download-info">
          <span>{{ progress.percentComplete }}%</span>
          <button @click="$emit('cancel', model.id)" class="btn-cancel">
            ✕
          </button>
        </div>
      </div>
      
      <!-- Download Button -->
      <button 
        v-else
        @click="$emit('download', model.id)"
        class="btn-download"
      >
        ⬇️ Herunterladen
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  model: Object,
  installed: Boolean,
  downloading: Boolean,
  progress: Object
});

defineEmits(['download', 'cancel']);

function formatDownloads(count) {
  if (count > 1000) {
    return Math.floor(count / 1000) + 'k';
  }
  return count;
}
</script>

<style scoped>
.model-card {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 20px;
  background: white;
  transition: box-shadow 0.2s;
}

.model-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.model-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  margin-bottom: 12px;
}

.model-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.rating {
  font-size: 14px;
  color: #f59e0b;
}

.model-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.model-description {
  font-size: 14px;
  color: #444;
  line-height: 1.5;
  margin: 12px 0;
  min-height: 60px;
}

.model-tags {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.tag {
  background: #f3f4f6;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: #374151;
}

.model-requirements {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #666;
  margin: 12px 0;
}

.model-actions {
  margin-top: 16px;
}

.btn-download {
  width: 100%;
  padding: 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-download:hover {
  background: #2563eb;
}

.btn-installed {
  width: 100%;
  padding: 12px;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: not-allowed;
}

.downloading {
  width: 100%;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: #3b82f6;
  transition: width 0.3s ease;
}

.download-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.btn-cancel {
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 4px 12px;
  cursor: pointer;
}
</style>
```

---

## 🎯 Backend Controller

```java
@RestController
@RequestMapping("/api/model-store")
@RequiredArgsConstructor
public class ModelStoreController {
    
    private final ModelRegistry modelRegistry;
    private final ModelDownloadService downloadService;
    
    /**
     * Alle verfügbaren Modelle
     */
    @GetMapping("/all")
    public List<ModelRegistryEntry> getAllModels() {
        return modelRegistry.getAllModels();
    }
    
    /**
     * Featured Modelle
     */
    @GetMapping("/featured")
    public List<ModelRegistryEntry> getFeaturedModels() {
        return modelRegistry.getFeaturedModels();
    }
    
    /**
     * Nach Kategorie filtern
     */
    @GetMapping("/category/{category}")
    public List<ModelRegistryEntry> getByCategory(@PathVariable String category) {
        return modelRegistry.getByCategory(category);
    }
    
    /**
     * Suche
     */
    @GetMapping("/search")
    public List<ModelRegistryEntry> search(@RequestParam String query) {
        return modelRegistry.search(query);
    }
    
    /**
     * Modell-Download mit SSE Progress
     */
    @GetMapping(value = "/download/{modelId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<DownloadProgress>> downloadModel(
        @PathVariable String modelId
    ) {
        return downloadService.downloadModel(modelId)
            .map(progress -> ServerSentEvent.<DownloadProgress>builder()
                .event(progress.getStatus() == DownloadStatus.COMPLETED ? "complete" : "progress")
                .data(progress)
                .build()
            );
    }
    
    /**
     * Download abbrechen
     */
    @PostMapping("/download/{modelId}/cancel")
    public ResponseEntity<Void> cancelDownload(@PathVariable String modelId) {
        downloadService.cancelDownload(modelId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Aktive Downloads
     */
    @GetMapping("/downloads/active")
    public Map<String, DownloadProgress> getActiveDownloads() {
        return downloadService.getActiveDownloads();
    }
}
```

---

## 🎯 Vorteile dieser Lösung

### 1. User Experience

```
✅ Alles in einer App
✅ Kein Browser-Wechsel zu Hugging Face
✅ Kuratierte, getestete Modelle
✅ Klare Beschreibungen auf Deutsch
✅ RAM-Requirements klar ersichtlich
✅ Echtzeit Download-Progress
```

### 2. Qualitätssicherung

```
✅ Nur geprüfte Modelle im Store
✅ Ratings & Download-Zahlen
✅ Klare Kategorisierung
✅ Hardware-Requirements
✅ Use-Case Beschreibungen
```

### 3. Einsteigerfreundlich

```
✅ "Für Chat" vs "Für Code" klar erkennbar
✅ RAM-Filter verhindert OOM-Errors
✅ Empfehlungen basierend auf Hardware
✅ Keine technischen GGUF-Details nötig
```

### 4. Maintenance

```
✅ Zentrale Liste im Code (ModelRegistry)
✅ Einfach neue Modelle hinzufügen
✅ Keine externe Datenbank nötig
✅ Versionierung via Git
```

---

## 📊 Erweiterungsmöglichkeiten

### Phase 2: Community Models

```java
/**
 * User können eigene Modelle zum Store beitragen
 */
@PostMapping("/submit")
public ModelSubmission submitModel(ModelSubmissionRequest request) {
    // Review-Process starten
    // Nach Freigabe → in Registry aufnehmen
}
```

### Phase 3: Auto-Updates

```java
/**
 * Benachrichtigt wenn neue Version verfügbar
 */
public void checkForUpdates() {
    installedModels.forEach(model -> {
        if (hasNewerVersion(model)) {
            notifyUser("Update verfügbar für: " + model);
        }
    });
}
```

### Phase 4: Smart Recommendations

```java
/**
 * Empfiehlt Modelle basierend auf:
 * - Verfügbarem RAM
 * - Bisherigen Downloads
 * - Use Cases
 */
public List<ModelRegistryEntry> getRecommendations(UserProfile user) {
    // ML-basierte Empfehlungen
}
```

---

## ✅ Implementierungs-Checkliste

### Backend (2-3 Tage)
- [ ] ModelRegistryEntry DTO
- [ ] ModelRegistry mit kuratierter Liste
- [ ] ModelDownloadService mit Progress
- [ ] ModelStoreController
- [ ] SSE für Download-Progress
- [ ] Tests

### Frontend (2-3 Tage)
- [ ] Model Store Hauptseite
- [ ] ModelCard Component
- [ ] DownloadProgress Component
- [ ] Filter & Suche
- [ ] Progress Panel (sticky)
- [ ] Responsive Design

### Content (1-2 Tage)
- [ ] 15-20 Modelle kuratieren
- [ ] Beschreibungen auf Deutsch
- [ ] Screenshots/Previews
- [ ] Use-Case Dokumentation

**Gesamt: ~1 Woche**

---

## 🎯 Launch-Strategie

### Marketing-Message:

> "Fleet Navigator - Der einzige AI-Assistant mit integriertem Model Store!
> 
> ✅ Kein Hugging Face Account nötig
> ✅ Keine Command-Line
> ✅ Einfach klicken und installieren
> 
> Wie im App Store, nur für AI-Modelle!"

---

**Das ist der Killer-Feature!** 🚀

Kein anderes Ollama-UI oder LM Studio hat einen so gut integrierten Model Store. Das ist ein **massiver Wettbewerbsvorteil!**
