# LLM Provider System - Architektur & Implementierung

**Version:** 1.0  
**Datum:** 2025-11-09  
**Projekt:** Fleet Navigator  
**Status:** 📋 Dokumentation - Bereit für Implementierung

---

## 🎯 Zielsetzung

Implementierung eines flexiblen Multi-Provider-Systems für Large Language Models in Fleet Navigator mit folgenden Hauptzielen:

1. **Flexibilität**: Unterstützung mehrerer LLM-Backends (Ollama, llama.cpp, OpenAI)
2. **Zero-Installation**: llama.cpp als embedded Default-Provider
3. **Bestehende Integration**: Nahtlose Nutzung vorhandener Ollama-Installationen
4. **Custom Models**: Einfacher Import eigener GGUF-Modelle
5. **Native Image Ready**: Volle GraalVM Native Image Kompatibilität

---

## 📊 Provider-Übersicht

| Provider | Typ | Modelle | Installation | Native Image |
|----------|-----|---------|--------------|--------------|
| **llama.cpp** | Embedded | 39.600+ GGUF | ✅ Keine | ✅ Kompatibel |
| **Ollama** | External | ~250 kuratiert | ⚠️ Separat | ✅ Kompatibel |
| **OpenAI** | Cloud API | Proprietär | ✅ Keine | ✅ Kompatibel |

---

## 🏗️ Architektur

### Schichtenmodell

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Vue.js)                        │
│  - Provider Auswahl UI                                      │
│  - Model Browser & Download                                 │
│  - Custom Model Upload                                      │
└─────────────────────────────────────────────────────────────┘
                            ↕ REST API
┌─────────────────────────────────────────────────────────────┐
│              LLMService (Facade Pattern)                    │
│  - Provider-agnostisch                                      │
│  - Auto-Detection                                           │
│  - Fallback-Logik                                           │
└─────────────────────────────────────────────────────────────┘
                            ↕ Interface
┌─────────────────────────────────────────────────────────────┐
│                   LLMProvider Interface                     │
│  + generate(prompt, model)                                  │
│  + generateStream(prompt, model)                            │
│  + listModels()                                             │
│  + isAvailable()                                            │
└─────────────────────────────────────────────────────────────┘
                            ↕ Implementations
┌──────────────────┬──────────────────┬──────────────────────┐
│  OllamaProvider  │ LlamaCppProvider │   OpenAIProvider     │
│  Port: 11434     │  Embedded Binary │   API Key Required   │
└──────────────────┴──────────────────┴──────────────────────┘
```

---

## 🔧 Implementierung

### 1. Provider Interface

```java
package io.javafleet.fleetnavigator.llm;

import java.util.List;
import reactor.core.publisher.Flux;

/**
 * Abstraktes Interface für alle LLM Provider
 * 
 * CRITICAL für Native Image:
 * - Keine Reflection in Implementierungen
 * - Keine dynamischen Proxies
 * - Explizite Bean-Registrierung
 */
public interface LLMProvider {
    
    /**
     * Provider-Name (ollama, llamacpp, openai)
     */
    String getProviderName();
    
    /**
     * Prüft ob Provider verfügbar ist
     * 
     * @return true wenn Provider einsatzbereit
     */
    boolean isAvailable();
    
    /**
     * Generiert Text-Response (synchron)
     * 
     * @param prompt User-Prompt
     * @param model Modell-Name
     * @param systemPrompt Optional: System-Prompt
     * @return Generierte Antwort
     */
    String generate(String prompt, String model, String systemPrompt);
    
    /**
     * Generiert Text-Response (streaming)
     * 
     * @param prompt User-Prompt
     * @param model Modell-Name
     * @param systemPrompt Optional: System-Prompt
     * @return Reactive Stream von Text-Chunks
     */
    Flux<String> generateStream(String prompt, String model, String systemPrompt);
    
    /**
     * Liste verfügbarer Modelle
     * 
     * @return Liste von ModelInfo-Objekten
     */
    List<ModelInfo> listModels();
    
    /**
     * Validiert ob Modell verfügbar ist
     * 
     * @param modelName Name des Modells
     * @return true wenn Modell existiert
     */
    boolean hasModel(String modelName);
}
```

### 2. ModelInfo DTO

```java
package io.javafleet.fleetnavigator.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modell-Informationen (Provider-agnostisch)
 * 
 * Native Image: Simple POJO, keine Reflection-Probleme
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    
    private String name;              // z.B. "llama3.2:3b"
    private String displayName;       // z.B. "Llama 3.2 (3B)"
    private String provider;          // "ollama", "llamacpp", "openai"
    private Long size;                // Größe in Bytes
    private String architecture;      // z.B. "llama", "mistral"
    private String quantization;      // z.B. "Q4_K_M", "Q8_0"
    private String description;       // Beschreibung
    private boolean custom;           // User-uploaded?
    private boolean installed;        // Bereits heruntergeladen?
}
```

### 3. LLMService (Facade)

```java
package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Zentrale Facade für alle LLM-Provider
 * 
 * Native Image Considerations:
 * - Keine dynamische Provider-Suche via Reflection
 * - Explizite Constructor Injection
 * - Keine @Autowired auf Collections
 */
@Service
@Slf4j
public class LLMService {
    
    private final Map<String, LLMProvider> providers;
    private final LLMConfigProperties config;
    private LLMProvider activeProvider;
    
    /**
     * Explizite Constructor Injection für Native Image
     * 
     * WICHTIG: Keine @Autowired List<LLMProvider> - das nutzt Reflection!
     */
    public LLMService(
        OllamaProvider ollamaProvider,
        LlamaCppProvider llamaCppProvider,
        OpenAIProvider openAIProvider,
        LLMConfigProperties config
    ) {
        // Manuelle Map-Erstellung statt Reflection
        this.providers = Map.of(
            "ollama", ollamaProvider,
            "llamacpp", llamaCppProvider,
            "openai", openAIProvider
        );
        this.config = config;
        this.activeProvider = detectActiveProvider();
    }
    
    /**
     * Auto-Detection mit Fallback-Logik
     */
    private LLMProvider detectActiveProvider() {
        // 1. User-Präferenz aus Config
        String preferred = config.getDefaultProvider();
        if (preferred != null && providers.get(preferred).isAvailable()) {
            log.info("✅ Using configured provider: {}", preferred);
            return providers.get(preferred);
        }
        
        // 2. Ollama wenn verfügbar (bevorzugt)
        if (providers.get("ollama").isAvailable()) {
            log.info("✅ Ollama detected and active");
            return providers.get("ollama");
        }
        
        // 3. Fallback auf llama.cpp (immer verfügbar)
        log.info("✅ Using embedded llama.cpp provider");
        return providers.get("llamacpp");
    }
    
    /**
     * Generiert Text mit aktivem Provider
     */
    public String generate(String prompt, String model, String systemPrompt) {
        return activeProvider.generate(prompt, model, systemPrompt);
    }
    
    /**
     * Streaming-Generierung
     */
    public Flux<String> generateStream(String prompt, String model, String systemPrompt) {
        return activeProvider.generateStream(prompt, model, systemPrompt);
    }
    
    /**
     * Alle verfügbaren Modelle (alle Provider)
     */
    public List<ModelInfo> getAllModels() {
        return providers.values().stream()
            .filter(LLMProvider::isAvailable)
            .flatMap(provider -> provider.listModels().stream())
            .toList();
    }
    
    /**
     * Provider wechseln
     */
    public void switchProvider(String providerName) {
        LLMProvider newProvider = providers.get(providerName);
        if (newProvider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
        if (!newProvider.isAvailable()) {
            throw new IllegalStateException("Provider not available: " + providerName);
        }
        this.activeProvider = newProvider;
        log.info("🔄 Switched to provider: {}", providerName);
    }
    
    /**
     * Status aller Provider
     */
    public Map<String, Boolean> getProviderStatus() {
        return Map.of(
            "ollama", providers.get("ollama").isAvailable(),
            "llamacpp", providers.get("llamacpp").isAvailable(),
            "openai", providers.get("openai").isAvailable()
        );
    }
}
```

### 4. Configuration Properties

```java
package io.javafleet.fleetnavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM Provider Konfiguration
 * 
 * Native Image: @ConfigurationProperties ist kompatibel
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMConfigProperties {
    
    /**
     * Default Provider (ollama, llamacpp, openai)
     */
    private String defaultProvider = "auto"; // auto-detect
    
    /**
     * Ollama-spezifische Konfiguration
     */
    private OllamaConfig ollama = new OllamaConfig();
    
    /**
     * llama.cpp-spezifische Konfiguration
     */
    private LlamaCppConfig llamacpp = new LlamaCppConfig();
    
    /**
     * OpenAI-spezifische Konfiguration
     */
    private OpenAIConfig openai = new OpenAIConfig();
    
    @Data
    public static class OllamaConfig {
        private String url = "http://localhost:11434";
        private boolean enabled = true;
    }
    
    @Data
    public static class LlamaCppConfig {
        private String binaryPath = "./bin/llama-server";
        private int port = 8080;
        private String modelsDir = "./models";
        private boolean autoStart = true;
        private int contextSize = 4096;
        private int gpuLayers = 999; // -1 = auto, 999 = all
    }
    
    @Data
    public static class OpenAIConfig {
        private String apiKey = "${OPENAI_API_KEY:}";
        private String baseUrl = "https://api.openai.com/v1";
        private boolean enabled = false;
    }
}
```

### 5. OllamaProvider Implementation

```java
package io.javafleet.fleetnavigator.llm.providers;

import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Ollama Provider Implementation
 * 
 * Native Image Considerations:
 * - WebClient statt RestTemplate (besser für Native Image)
 * - Keine Jackson @JsonProperty Reflection
 * - Explizite DTO-Klassen statt Maps
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaProvider implements LLMProvider {
    
    private final LLMConfigProperties config;
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            if (webClient == null) {
                webClient = webClientBuilder
                    .baseUrl(config.getOllama().getUrl())
                    .build();
            }
            
            // Ping Ollama
            webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            return true;
        } catch (Exception e) {
            log.debug("Ollama not available: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String generate(String prompt, String model, String systemPrompt) {
        OllamaRequest request = new OllamaRequest();
        request.setModel(model);
        request.setPrompt(prompt);
        request.setSystem(systemPrompt);
        request.setStream(false);
        
        OllamaResponse response = webClient.post()
            .uri("/api/generate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OllamaResponse.class)
            .block();
        
        return response != null ? response.getResponse() : "";
    }
    
    @Override
    public Flux<String> generateStream(String prompt, String model, String systemPrompt) {
        OllamaRequest request = new OllamaRequest();
        request.setModel(model);
        request.setPrompt(prompt);
        request.setSystem(systemPrompt);
        request.setStream(true);
        
        return webClient.post()
            .uri("/api/generate")
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(OllamaStreamResponse.class)
            .map(OllamaStreamResponse::getResponse);
    }
    
    @Override
    public List<ModelInfo> listModels() {
        OllamaTagsResponse response = webClient.get()
            .uri("/api/tags")
            .retrieve()
            .bodyToMono(OllamaTagsResponse.class)
            .block();
        
        if (response == null || response.getModels() == null) {
            return List.of();
        }
        
        return response.getModels().stream()
            .map(this::mapToModelInfo)
            .toList();
    }
    
    @Override
    public boolean hasModel(String modelName) {
        return listModels().stream()
            .anyMatch(m -> m.getName().equals(modelName));
    }
    
    private ModelInfo mapToModelInfo(OllamaModel ollamaModel) {
        return ModelInfo.builder()
            .name(ollamaModel.getName())
            .displayName(ollamaModel.getName())
            .provider("ollama")
            .size(ollamaModel.getSize())
            .installed(true)
            .custom(false)
            .build();
    }
    
    // ===== DTOs für Native Image (keine Reflection!) =====
    
    /**
     * WICHTIG: Keine @JsonProperty Annotationen!
     * Jackson kann in Native Image Probleme machen
     * Stattdessen: Exakte Feldnamen wie in JSON
     */
    @Data
    public static class OllamaRequest {
        private String model;
        private String prompt;
        private String system;
        private boolean stream;
    }
    
    @Data
    public static class OllamaResponse {
        private String model;
        private String response;
        private boolean done;
    }
    
    @Data
    public static class OllamaStreamResponse {
        private String response;
        private boolean done;
    }
    
    @Data
    public static class OllamaTagsResponse {
        private List<OllamaModel> models;
    }
    
    @Data
    public static class OllamaModel {
        private String name;
        private Long size;
        private String digest;
        private Map<String, Object> details;
    }
}
```

### 6. LlamaCppProvider Implementation

```java
package io.javafleet.fleetnavigator.llm.providers;

import io.javafleet.fleetnavigator.llm.LLMProvider;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * llama.cpp Provider Implementation
 * 
 * Native Image Critical:
 * - ProcessBuilder ist Native Image kompatibel
 * - Keine Runtime.exec() nutzen
 * - File I/O ist OK
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlamaCppProvider implements LLMProvider {
    
    private final LLMConfigProperties config;
    private Process llamaServerProcess;
    
    /**
     * Startet llama-server beim Startup
     * 
     * Native Image: @PostConstruct funktioniert
     */
    @PostConstruct
    public void startLlamaServer() {
        if (!config.getLlamacpp().isAutoStart()) {
            log.info("llama.cpp auto-start disabled");
            return;
        }
        
        try {
            Path binaryPath = Paths.get(config.getLlamacpp().getBinaryPath());
            
            if (!Files.exists(binaryPath)) {
                log.warn("llama-server binary not found: {}", binaryPath);
                return;
            }
            
            // Finde erstes verfügbares Modell
            Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());
            Path firstModel = findFirstModel(modelsDir);
            
            if (firstModel == null) {
                log.warn("No GGUF models found in: {}", modelsDir);
                return;
            }
            
            // ProcessBuilder für Native Image
            ProcessBuilder pb = new ProcessBuilder(
                binaryPath.toString(),
                "-m", firstModel.toString(),
                "--port", String.valueOf(config.getLlamacpp().getPort()),
                "--host", "0.0.0.0",
                "-ngl", String.valueOf(config.getLlamacpp().getGpuLayers()),
                "--ctx-size", String.valueOf(config.getLlamacpp().getContextSize())
            );
            
            pb.redirectErrorStream(true);
            llamaServerProcess = pb.start();
            
            log.info("✅ llama-server started on port {}", config.getLlamacpp().getPort());
            log.info("   Model: {}", firstModel.getFileName());
            
            // Warte bis Server bereit
            Thread.sleep(2000);
            
        } catch (Exception e) {
            log.error("Failed to start llama-server", e);
        }
    }
    
    /**
     * Stoppt llama-server beim Shutdown
     */
    @PreDestroy
    public void stopLlamaServer() {
        if (llamaServerProcess != null && llamaServerProcess.isAlive()) {
            llamaServerProcess.destroy();
            log.info("🛑 llama-server stopped");
        }
    }
    
    @Override
    public String getProviderName() {
        return "llamacpp";
    }
    
    @Override
    public boolean isAvailable() {
        // llama.cpp ist immer "verfügbar" (embedded)
        return Files.exists(Paths.get(config.getLlamacpp().getBinaryPath()));
    }
    
    @Override
    public String generate(String prompt, String model, String systemPrompt) {
        // TODO: HTTP Call zu llama-server
        // Implementierung analog zu OllamaProvider
        return "";
    }
    
    @Override
    public Flux<String> generateStream(String prompt, String model, String systemPrompt) {
        // TODO: SSE Stream von llama-server
        return Flux.empty();
    }
    
    @Override
    public List<ModelInfo> listModels() {
        List<ModelInfo> models = new ArrayList<>();
        Path modelsDir = Paths.get(config.getLlamacpp().getModelsDir());
        
        try {
            Files.list(modelsDir)
                .filter(path -> path.toString().endsWith(".gguf"))
                .forEach(path -> {
                    ModelInfo info = ModelInfo.builder()
                        .name(path.getFileName().toString())
                        .displayName(extractDisplayName(path))
                        .provider("llamacpp")
                        .size(getFileSize(path))
                        .installed(true)
                        .custom(isCustomModel(path))
                        .build();
                    models.add(info);
                });
        } catch (IOException e) {
            log.error("Failed to list models", e);
        }
        
        return models;
    }
    
    @Override
    public boolean hasModel(String modelName) {
        Path modelPath = Paths.get(config.getLlamacpp().getModelsDir(), modelName);
        return Files.exists(modelPath);
    }
    
    // ===== Helper Methods =====
    
    private Path findFirstModel(Path modelsDir) throws IOException {
        if (!Files.exists(modelsDir)) {
            return null;
        }
        
        return Files.list(modelsDir)
            .filter(path -> path.toString().endsWith(".gguf"))
            .findFirst()
            .orElse(null);
    }
    
    private String extractDisplayName(Path modelPath) {
        String filename = modelPath.getFileName().toString();
        return filename.replace(".gguf", "")
            .replace("-", " ")
            .replace("_", " ");
    }
    
    private Long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }
    
    private boolean isCustomModel(Path path) {
        // TODO: Check in DB ob user-uploaded
        return false;
    }
}
```

---

## 🔥 Native Image Kompatibilität

### Kritische Punkte & Lösungen

#### 1. ❌ KEINE Reflection

```java
// ❌ FALSCH - Nutzt Reflection:
@Autowired
private List<LLMProvider> providers; // Spring scannt via Reflection

// ✅ RICHTIG - Explizite Injection:
public LLMService(
    OllamaProvider ollama,
    LlamaCppProvider llamacpp,
    OpenAIProvider openai
) {
    this.providers = Map.of(
        "ollama", ollama,
        "llamacpp", llamacpp,
        "openai", openai
    );
}
```

#### 2. ❌ KEINE dynamischen Proxies

```java
// ❌ FALSCH - JDK Dynamic Proxy:
@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {}

// ✅ RICHTIG - Concrete Class:
@Repository
public class ModelRepository {
    @PersistenceContext
    private EntityManager em;
    
    public List<Model> findAll() {
        return em.createQuery("SELECT m FROM Model m", Model.class)
            .getResultList();
    }
}
```

#### 3. ✅ ProcessBuilder ist OK

```java
// ✅ Native Image kompatibel:
ProcessBuilder pb = new ProcessBuilder("./llama-server", "-m", "model.gguf");
Process process = pb.start();
```

#### 4. ✅ WebClient statt RestTemplate

```java
// ❌ RestTemplate kann Probleme machen
RestTemplate rest = new RestTemplate();

// ✅ WebClient funktioniert besser:
WebClient client = WebClient.builder()
    .baseUrl("http://localhost:11434")
    .build();
```

#### 5. ⚠️ Jackson Serialization

```java
// ❌ FALSCH - @JsonProperty nutzt Reflection:
@JsonProperty("model_name")
private String modelName;

// ✅ RICHTIG - Feldname = JSON-Key:
private String model_name; // oder modelName wenn JSON auch so heißt

// ODER: Explizite Hints für Native Image:
// In reflect-config.json registrieren
```

### Native Image Konfiguration

#### reflect-config.json
```json
[
  {
    "name": "io.javafleet.fleetnavigator.llm.dto.ModelInfo",
    "allDeclaredConstructors": true,
    "allDeclaredFields": true,
    "allDeclaredMethods": true
  },
  {
    "name": "io.javafleet.fleetnavigator.llm.providers.OllamaProvider$OllamaRequest",
    "allDeclaredConstructors": true,
    "allDeclaredFields": true,
    "allDeclaredMethods": true
  }
]
```

#### resource-config.json
```json
{
  "resources": {
    "includes": [
      {
        "pattern": "application.yml"
      },
      {
        "pattern": "bin/llama-server.*"
      }
    ]
  }
}
```

---

## 📦 Custom Model Management

### Model Upload Flow

```
User → Upload GGUF → Backend Validation → Save to models/ → Register in DB
```

### Backend Service

```java
@Service
@RequiredArgsConstructor
public class CustomModelService {
    
    private final LLMConfigProperties config;
    private final CustomModelRepository repository;
    
    /**
     * Upload custom GGUF model
     * 
     * Native Image: MultipartFile ist kompatibel
     */
    @Transactional
    public ModelInfo uploadModel(
        MultipartFile file,
        String displayName,
        String description
    ) {
        // 1. Validate GGUF format
        if (!isValidGGUF(file)) {
            throw new InvalidModelException("Not a valid GGUF file");
        }
        
        // 2. Save to models directory
        String filename = sanitizeFilename(file.getOriginalFilename());
        Path targetPath = Paths.get(
            config.getLlamacpp().getModelsDir(),
            filename
        );
        
        try {
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException e) {
            throw new ModelUploadException("Failed to save model", e);
        }
        
        // 3. Parse GGUF metadata (optional)
        GGUFMetadata metadata = parseGGUFMetadata(targetPath);
        
        // 4. Save to database
        CustomModel model = new CustomModel();
        model.setFilename(filename);
        model.setDisplayName(displayName);
        model.setDescription(description);
        model.setFilePath(targetPath.toString());
        model.setSize(file.getSize());
        model.setArchitecture(metadata.getArchitecture());
        model.setUploadedAt(LocalDateTime.now());
        
        repository.save(model);
        
        // 5. Return ModelInfo
        return ModelInfo.builder()
            .name(filename)
            .displayName(displayName)
            .provider("llamacpp")
            .size(file.getSize())
            .description(description)
            .custom(true)
            .installed(true)
            .build();
    }
    
    /**
     * Validate GGUF magic number
     */
    private boolean isValidGGUF(MultipartFile file) {
        try {
            byte[] magic = new byte[4];
            file.getInputStream().read(magic);
            
            // GGUF starts with "GGUF" (0x47 0x47 0x55 0x46)
            return magic[0] == 'G' && magic[1] == 'G' 
                && magic[2] == 'U' && magic[3] == 'F';
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Sanitize filename for security
     */
    private String sanitizeFilename(String filename) {
        return filename
            .replaceAll("[^a-zA-Z0-9.-]", "_")
            .toLowerCase();
    }
}
```

### Entity für Custom Models

```java
@Entity
@Table(name = "custom_models")
@Data
public class CustomModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String filename;
    
    @Column(nullable = false)
    private String displayName;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private String filePath;
    
    private Long size;
    
    private String architecture; // llama, mistral, etc.
    
    private String quantization; // Q4_K_M, Q8_0, etc.
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @Column(name = "last_used")
    private LocalDateTime lastUsed;
    
    private Integer useCount = 0;
}
```

---

## 🎨 Frontend Integration

### Provider Selection Component

```vue
<template>
  <div class="provider-settings">
    <h3>🤖 AI Provider Auswahl</h3>
    
    <div class="provider-grid">
      <!-- Ollama Card -->
      <div 
        class="provider-card"
        :class="{ 
          active: selectedProvider === 'ollama',
          disabled: !providerStatus.ollama 
        }"
        @click="selectProvider('ollama')"
      >
        <div class="provider-icon">🦙</div>
        <h4>Ollama</h4>
        <span 
          class="badge"
          :class="providerStatus.ollama ? 'success' : 'error'"
        >
          {{ providerStatus.ollama ? 'Verfügbar' : 'Nicht installiert' }}
        </span>
        
        <p v-if="providerStatus.ollama">
          {{ ollamaModels.length }} Modelle verfügbar
        </p>
        <p v-else class="install-hint">
          → <a href="https://ollama.ai" target="_blank">ollama.ai</a>
        </p>
      </div>
      
      <!-- llama.cpp Card -->
      <div 
        class="provider-card"
        :class="{ active: selectedProvider === 'llamacpp' }"
        @click="selectProvider('llamacpp')"
      >
        <div class="provider-icon">⚡</div>
        <h4>llama.cpp (Embedded)</h4>
        <span class="badge success">Immer verfügbar</span>
        
        <p>{{ llamacppModels.length }} Modelle installiert</p>
        <p class="feature">✅ 39.600+ Modelle verfügbar</p>
      </div>
      
      <!-- OpenAI Card (optional) -->
      <div 
        class="provider-card"
        :class="{ 
          active: selectedProvider === 'openai',
          disabled: !providerStatus.openai 
        }"
        @click="selectProvider('openai')"
      >
        <div class="provider-icon">☁️</div>
        <h4>OpenAI (Cloud)</h4>
        <span 
          class="badge"
          :class="providerStatus.openai ? 'success' : 'warning'"
        >
          {{ providerStatus.openai ? 'Konfiguriert' : 'API Key fehlt' }}
        </span>
        
        <p>Kostenpflichtig, höchste Qualität</p>
      </div>
    </div>
    
    <!-- Model Selection -->
    <div v-if="selectedProvider" class="model-selection">
      <h4>Verfügbare Modelle</h4>
      <ModelList 
        :provider="selectedProvider"
        :models="getModelsForProvider(selectedProvider)"
        @select="selectModel"
      />
    </div>
    
    <!-- Custom Model Upload -->
    <div v-if="selectedProvider === 'llamacpp'" class="custom-models">
      <h4>🎨 Eigene Modelle</h4>
      <button @click="showUploadDialog = true" class="btn-primary">
        ⬆️ GGUF-Modell hochladen
      </button>
      
      <UploadDialog 
        v-if="showUploadDialog"
        @close="showUploadDialog = false"
        @upload="handleUpload"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const selectedProvider = ref('ollama');
const providerStatus = ref({
  ollama: false,
  llamacpp: true,
  openai: false
});
const ollamaModels = ref([]);
const llamacppModels = ref([]);
const showUploadDialog = ref(false);

onMounted(async () => {
  await loadProviderStatus();
  await loadModels();
});

async function loadProviderStatus() {
  const response = await axios.get('/api/llm/providers/status');
  providerStatus.value = response.data;
  
  // Auto-select best available provider
  if (providerStatus.value.ollama) {
    selectedProvider.value = 'ollama';
  } else {
    selectedProvider.value = 'llamacpp';
  }
}

async function loadModels() {
  const response = await axios.get('/api/llm/models');
  response.data.forEach(model => {
    if (model.provider === 'ollama') {
      ollamaModels.value.push(model);
    } else if (model.provider === 'llamacpp') {
      llamacppModels.value.push(model);
    }
  });
}

async function selectProvider(provider) {
  if (!providerStatus.value[provider]) return;
  
  selectedProvider.value = provider;
  await axios.post('/api/llm/providers/switch', { provider });
}

function getModelsForProvider(provider) {
  if (provider === 'ollama') return ollamaModels.value;
  if (provider === 'llamacpp') return llamacppModels.value;
  return [];
}

async function handleUpload(file, metadata) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('displayName', metadata.displayName);
  formData.append('description', metadata.description);
  
  await axios.post('/api/llm/models/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  
  await loadModels();
  showUploadDialog.value = false;
}
</script>
```

---

## 📋 API Endpoints

### Provider Management

```
GET    /api/llm/providers/status           → Status aller Provider
POST   /api/llm/providers/switch           → Provider wechseln
GET    /api/llm/providers/active           → Aktiver Provider
```

### Model Management

```
GET    /api/llm/models                     → Alle Modelle
GET    /api/llm/models/{provider}          → Modelle eines Providers
POST   /api/llm/models/upload              → Custom Modell hochladen
DELETE /api/llm/models/{id}                → Modell löschen
GET    /api/llm/models/available           → Downloadbare Modelle
POST   /api/llm/models/download            → Modell von HuggingFace laden
```

### Generation

```
POST   /api/llm/generate                   → Text generieren
POST   /api/llm/generate/stream            → Streaming-Generierung
```

---

## 🚀 Deployment

### Native Image Build

```bash
# 1. Maven Native Build
mvn -Pnative clean package

# 2. Resultat:
target/fleet-navigator (Native Binary ~80 MB)

# 3. Mit llama-server bundlen:
mkdir -p release/bin
cp target/fleet-navigator release/
cp bin/llama-server release/bin/
cp -r models release/

# 4. ZIP für Distribution:
cd release
zip -r fleet-navigator-native-linux-amd64.zip *
```

### Directory Structure (Release)

```
fleet-navigator-native/
├── fleet-navigator              # Native Binary
├── bin/
│   └── llama-server             # llama.cpp Binary
├── models/
│   ├── llama-3.2-3b-Q4_K_M.gguf # Default Modell (optional)
│   └── .gitkeep
├── application.yml               # Config
└── README.md
```

---

## 📊 Modell-Verfügbarkeit Vergleich

| Quelle | Anzahl | Qualität | Custom Models |
|--------|--------|----------|---------------|
| **Ollama Library** | ~250 | ✅ Kuratiert | ⚠️ Umständlich |
| **GGUF/HuggingFace** | 39.600+ | 🔀 Variabel | ✅ Einfach |
| **Eigene GGUF** | ∞ | 👤 User-definiert | ✅ Copy & Paste |

---

## ✅ Implementierungs-Checkliste

### Phase 1: Core Provider System (3-4 Tage)
- [ ] LLMProvider Interface
- [ ] ModelInfo DTO
- [ ] LLMService (Facade)
- [ ] LLMConfigProperties
- [ ] OllamaProvider Implementation
- [ ] LlamaCppProvider Implementation (Basic)
- [ ] Native Image Tests

### Phase 2: llama.cpp Integration (2-3 Tage)
- [ ] llama-server Auto-Start
- [ ] Model Discovery (GGUF files)
- [ ] HTTP Client für llama-server
- [ ] Streaming Support
- [ ] Process Management

### Phase 3: Custom Models (2 Tage)
- [ ] CustomModelService
- [ ] CustomModel Entity
- [ ] Upload Endpoint
- [ ] GGUF Validation
- [ ] Metadata Parsing

### Phase 4: Frontend (2-3 Tage)
- [ ] Provider Selection UI
- [ ] Model Browser
- [ ] Upload Dialog
- [ ] Status Dashboard
- [ ] Settings Integration

### Phase 5: Testing & Polish (2 Tage)
- [ ] Native Image Build Test
- [ ] Provider Auto-Detection Test
- [ ] Fallback-Logic Test
- [ ] Custom Model Upload Test
- [ ] Documentation

**Gesamt: ~2 Wochen**

---

## 🎯 Success Criteria

- ✅ Native Image Binary funktioniert
- ✅ Ollama wird automatisch erkannt wenn vorhanden
- ✅ llama.cpp funktioniert als Fallback
- ✅ Custom GGUF-Models können hochgeladen werden
- ✅ Provider können zur Laufzeit gewechselt werden
- ✅ Keine Reflection-Fehler in Native Image
- ✅ Startup-Zeit < 5 Sekunden (Native)
- ✅ Memory Footprint < 200 MB

---

**Ende der Dokumentation**

Bereit für Implementierung! 🚀
