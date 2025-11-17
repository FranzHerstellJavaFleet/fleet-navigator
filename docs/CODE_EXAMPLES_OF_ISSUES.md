# Fleet Navigator - Model Management Issues
## Code Examples Showing Problems

### ISSUE #1: Duplicate Default Model Tracking

**Problem**: Three different places track which model is "default"

**Example 1: ModelController sets default for Ollama**
```java
// File: src/main/java/io/javafleet/fleetnavigator/controller/ModelController.java
// Lines: 189-200

@PostMapping("/{name}/default")
public ResponseEntity<Map<String, String>> setDefaultModel(@PathVariable String name) {
    try {
        log.info("Setting default model to: {}", name);
        metadataService.setDefaultModel(name);  // Sets ModelMetadata.isDefault = true
        return ResponseEntity.ok(Map.of("message", "Default model set successfully", "model", name));
    } catch (Exception e) {
        log.error("Error setting default model: {}", name, e);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to set default model: " + e.getMessage()));
    }
}
```

**Example 2: GgufModelConfigController sets default for llama.cpp**
```java
// File: src/main/java/io/javafleet/fleetnavigator/controller/GgufModelConfigController.java
// Lines: 184-200

@PatchMapping("/{id}/set-default")
public ResponseEntity<GgufModelConfig> setDefault(@PathVariable Long id) {
    return ggufModelConfigRepository.findById(id)
            .map(config -> {
                // Unset other defaults
                ggufModelConfigRepository.findByIsDefaultTrue().ifPresent(existing -> {
                    existing.setIsDefault(false);
                    ggufModelConfigRepository.save(existing);
                });

                config.setIsDefault(true);
                GgufModelConfig saved = ggufModelConfigRepository.save(config);
                log.info("Set GGUF model config as default: {}", saved.getName());
                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.notFound().build());
}
```

**Example 3: SettingsService might have another default**
```java
// File: src/main/java/io/javafleet/fleetnavigator/service/SettingsService.java
// Probably has:
public Optional<ModelMetadata> getDefaultModel() {
    // But which provider's default?
    return metadataService.getDefaultModel();
}
```

**Problem Scenario**:
```
User switches from Ollama to llama.cpp:
1. Active provider = llama.cpp
2. ModelMetadata.isDefault = "qwen:7b" (Ollama model - doesn't exist in llama.cpp!)
3. GgufModelConfig.isDefault = "mistral-7b" (llama.cpp model)
4. When user chats, which model is used?
   - If code queries ModelMetadata, it tries to use qwen:7b in llama.cpp → ERROR
   - If code queries GgufModelConfig, it uses mistral-7b → OK
   - Behavior is undefined and depends on implementation order
```

---

### ISSUE #2: Custom Model Orphaned Data

**Problem**: Deleting custom model from DB doesn't delete from Ollama

**Code Location**: CustomModelController.java, lines 274-297
```java
@DeleteMapping("/{id}")
public ResponseEntity<Map<String, String>> deleteCustomModel(@PathVariable Long id) {
    try {
        log.info("Deleting custom model: {}", id);

        // Get model name before deleting
        CustomModel model = customModelService.getCustomModelById(id)
                .orElseThrow(() -> new IllegalArgumentException("Custom model not found: " + id));

        String modelName = model.getName();

        // ONLY deletes from database!
        customModelService.deleteCustomModel(id);

        return ResponseEntity.ok(Map.of(
                "message", "Custom model deleted from database",
                "model", modelName,
                "note", "Model still exists in Ollama. Use 'ollama rm " + modelName + "' to remove it."
                // ↑ User has to manually clean up! Bad UX!
        ));
    } catch (Exception e) {
        log.error("Error deleting custom model: {}", id, e);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to delete custom model: " + e.getMessage()));
    }
}
```

**Contrast with ModelController** (which does it right):
```java
// File: src/main/java/io/javafleet/fleetnavigator/controller/ModelController.java
// Lines: 82-97

@DeleteMapping("/{name}")
public ResponseEntity<Map<String, String>> deleteModel(@PathVariable String name) {
    try {
        log.info("Deleting model: {}", name);
        llmProviderService.deleteModel(name);  // Deletes from provider
        
        // Also delete metadata if exists
        metadataService.deleteMetadata(name);  // Deletes from DB

        return ResponseEntity.ok(Map.of("message", "Model deleted successfully", "model", name));
    } catch (IOException e) {
        log.error("Error deleting model: {}", name, e);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to delete model: " + e.getMessage()));
    }
}
```

**Problem Scenario**:
```
1. User creates "mymodel:latest" custom model
   - Created in Ollama (✓)
   - Stored in CustomModel table (✓)
   
2. User deletes custom model
   - Deleted from database (✓)
   - Still exists in Ollama (✗)
   
3. Days later...
   - Model still consuming 4GB of disk space
   - User doesn't know why
   - Must manually run: ollama rm mymodel:latest
```

---

### ISSUE #3: Configuration Persistence Not Implemented

**Problem**: Config changes aren't saved to application.properties

**Code Location**: LLMProviderController.java, lines 134-145
```java
@PutMapping("/config")
public ResponseEntity<ConfigUpdateResponse> updateProviderConfig(
        @RequestBody ProviderConfigUpdateRequest request) {
    
    ConfigUpdateResponse response = new ConfigUpdateResponse();
    response.setSuccess(true);
    response.setMessage("Konfiguration gespeichert. Neustart erforderlich für die meisten Änderungen.");
    response.setRestartRequired(true);

    // TODO: Implement config persistence to application.properties or separate config file
    log.warn("Config update not yet implemented - changes will be lost on restart!");
    // ↑ Doesn't actually save anything!

    return ResponseEntity.ok(response);
}
```

**Problem Scenario**:
```
1. User opens Settings
2. Changes ollama.url from http://localhost:11434 to http://192.168.1.100:11434
3. Clicks Save
4. Gets message: "Config saved. Restart required."
5. Restarts application
6. ollama.url is back to http://localhost:11434!
7. User frustrated - changes lost

Real issue:
- Configuration only exists in memory/application.properties
- No database persistence
- No application-{profile}.properties file
- No ConfigServer integration
```

---

### ISSUE #4: Missing Provider Implementations

**Problem**: JavaLlamaCppProvider throws UnsupportedOperationException for basic features

**Code Location**: JavaLlamaCppProvider.java, multiple lines

```java
@Override
public void pullModel(String modelName, Consumer<String> progressConsumer) throws IOException {
    // Line 150: TODO: Add when library supports these methods
    throw new UnsupportedOperationException(
        "pullModel not supported by java-llama-cpp provider"
    );
}

@Override
public Map<String, Object> getModelDetails(String modelName) throws IOException {
    // Line 403: TODO: Add when library supports these methods
    throw new UnsupportedOperationException(
        "getModelDetails not supported by java-llama-cpp provider"
    );
}

@Override
public void createModel(String modelName, String baseModel, String systemPrompt,
                        Double temperature, Double topP, Integer topK,
                        Double repeatPenalty, Consumer<String> progressConsumer) 
        throws IOException {
    // Line 410: TODO: Add when library supports these methods
    throw new UnsupportedOperationException(
        "createModel not supported by java-llama-cpp provider"
    );
}
```

**Problem Scenario**:
```
User switches to java-llama-cpp provider and tries:
1. Download model → UnsupportedOperationException (user gets error page)
2. View model details → UnsupportedOperationException
3. Create custom model → UnsupportedOperationException

UI doesn't disable these features or explain why.
User frustrated - "Why can't I download models?"
```

**Missing Feature Detection**:
```java
// What we need (but don't have):
interface LLMProvider {
    default boolean supportsModelDownload() {
        return false;  // Override in implementations
    }
    
    default boolean supportsModelDetails() {
        return false;
    }
    
    default boolean supportsCustomModels() {
        return false;
    }
}

// Frontend would then do:
if (provider.supportsModelDownload()) {
    // Show download button
} else {
    // Hide button with tooltip: "Not supported by java-llama-cpp"
}
```

---

### ISSUE #5: Tight Coupling to Ollama

**Problem**: CustomModelController directly uses OllamaService, blocking other providers

**Code Location**: CustomModelController.java, lines 29-30
```java
@RestController
@RequestMapping("/api/custom-models")
@RequiredArgsConstructor
@Slf4j
public class CustomModelController {

    private final CustomModelService customModelService;
    private final OllamaService ollamaService;  // ← DIRECT OLLAMA COUPLING!
    
    // Thread pool for handling model creation operations
    private final ExecutorService executorService = Executors.newCachedThreadPool();
```

**Using OllamaService directly**:
```java
// Line 114-132:
ollamaService.createModel(
        modelNameWithTag,
        request.getBaseModel(),
        request.getSystemPrompt(),
        request.getTemperature(),
        request.getTopP(),
        request.getTopK(),
        request.getRepeatPenalty(),
        progress -> { ... }
);
```

**Problem**: Can't use LLMProviderService (the abstraction pattern!)
```java
// What it SHOULD be:
private final LLMProviderService llmProviderService;

// Then it would work with ALL providers:
llmProviderService.createModel(
        modelNameWithTag,
        request.getBaseModel(),
        request.getSystemPrompt(),
        // ... same parameters
);

// And we could support custom models for any provider
// But currently locked to Ollama only
```

---

### ISSUE #6: Inconsistent API Endpoints

**Problem**: No standardized endpoint naming

**Endpoints exist at multiple paths:**
```
Model operations:
  GET  /api/models                      - List models (from ACTIVE provider)
  POST /api/models/pull                 - Download model (OLLAMA ONLY)
  POST /api/models/create               - Create custom model
  DELETE /api/models/{name}             - Delete model
  GET  /api/models/{name}/details       - Get details (TODO)
  POST /api/models/{name}/default       - Set default
  GET  /api/models/default              - Get default
  PUT  /api/models/{name}/metadata      - Update metadata

Custom models:
  GET  /api/custom-models               - List custom models
  POST /api/custom-models               - Create custom model
  PUT  /api/custom-models/{id}          - Update custom model
  DELETE /api/custom-models/{id}        - Delete custom model
  GET  /api/custom-models/{id}/ancestry - Get versions

GGUF models:
  GET  /api/gguf-models                 - List GGUF configs
  POST /api/gguf-models                 - Create GGUF config
  PUT  /api/gguf-models/{id}            - Update GGUF config
  DELETE /api/gguf-models/{id}          - Delete GGUF config
  GET  /api/gguf-models/by-name/{name}  - Get by name
  PATCH /api/gguf-models/{id}/set-default - Set default

Provider management:
  GET  /api/llm/providers               - List providers
  POST /api/llm/providers/switch        - Switch provider
  GET  /api/llm/providers/config        - Get config
  PUT  /api/llm/providers/config        - Update config (NOT IMPLEMENTED)
```

**Problem**: 
- Unclear which endpoint to use
- Duplicate functionality (e.g., two ways to create models)
- Inconsistent naming (id vs name as parameter)
- Hard to document or test
- Easy to break things when refactoring

**Better approach**:
```
/api/models                              - Generic, uses active provider
/api/models/ollama                       - Ollama-specific
/api/models/llamacpp                     - llama.cpp-specific
/api/models/custom                       - Custom model versioning

OR:

/api/models/{provider}/{operation}       - Explicit provider in path
GET  /api/models/ollama/available
POST /api/models/ollama/download/{name}
POST /api/models/custom/create
```

---

### ISSUE #7: Model Selection Ignores Provider Boundaries

**Problem**: Smart selection doesn't check if model exists in active provider

**Code Location**: ModelSelectionService.java, lines 55-87
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSelectionService {
    
    private final ModelSelectionProperties properties;
    private final SettingsService settingsService;

    @Cacheable(value = "modelSelection", key = "#prompt.hashCode()")
    public String selectModel(String prompt, String defaultModel) {
        // ... code determines which model to use ...
        
        // But DOESN'T CHECK if model exists in active provider!
        return settings.getCodeModel();  // Might not exist!
    }
}
```

**Frontend usage**: chatStore.js, line 146
```javascript
// ChatService.ts
modelToUse = modelSelectionService.selectModel(request.getMessage(), defaultModel);
// ↑ Returns model name without checking availability!
```

**Problem Scenario**:
```
Configuration says:
- code-model=qwen-coder:7b (for code tasks)
- fast-model=phi:latest (for simple questions)

But active provider is java-llama-cpp which has:
- mistral-7b-instruct-q4_K_M.gguf
- neural-chat-7b-q4_K_M.gguf

User asks "Write me a function"
→ System detects code task
→ Returns "qwen-coder:7b" 
→ Chat tries to use qwen-coder:7b in java-llama-cpp
→ Model not found → ERROR!

What should happen:
→ Smart selection returns qwen-coder:7b
→ System checks: "Is qwen-coder:7b available in active provider?"
→ If no: "Model not available. Using default or best match"
```

---

## Summary of Issues

| Issue | Severity | Location | Quick Fix Time |
|-------|----------|----------|-----------------|
| Duplicate default tracking | CRITICAL | Controller methods, SettingsService | 2 days |
| Custom model orphaned | CRITICAL | CustomModelController:285 | 1 day |
| Config not persistent | CRITICAL | LLMProviderController:141 | 2 days |
| Provider incomplete | HIGH | JavaLlamaCppProvider | 3 days |
| Ollama coupling | HIGH | CustomModelController:30 | 1 day |
| Inconsistent endpoints | HIGH | All controllers | 2 days |
| Model selection boundary | MEDIUM | ModelSelectionService | 1 day |

**Total estimated fix time: 8-10 days**

