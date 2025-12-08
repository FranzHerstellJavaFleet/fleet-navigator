# Fleet Navigator - Model Management System Analysis

## Executive Summary

The Fleet Navigator model management system is **complex but has several critical issues**:

1. **Provider Confusion**: Three overlapping provider implementations (Ollama, llama.cpp, java-llama-cpp) with inconsistent model handling
2. **Model Storage Fragmentation**: Models split across Ollama's internal storage, filesystem, and database metadata
3. **Missing Implementations**: Several TODO items blocking critical features
4. **API Endpoint Chaos**: Multiple redundant endpoints with unclear separation of concerns
5. **Configuration Persistence Issues**: Config changes lost on restart

---

## Architecture Overview

### Model Sources (4 Different Systems!)
```
1. Ollama (HTTP API) - External server, models in ~/.ollama/models/
2. llama.cpp (Embedded) - Server-based, models in ./models/library/
3. java-llama-cpp (JNI) - In-process, models in ./models/library/
4. Custom Models (Database) - Stored in Ollama + tracked in DB + in ./models/custom/
```

### Controller Endpoints (Multiple Overlap!)

**ModelController** (`/api/models`)
- GET /api/models - Get available models from ACTIVE provider
- DELETE /api/models/{name} - Delete model from ACTIVE provider
- POST /api/models/pull - Download model (OLLAMA ONLY)
- GET /api/models/{name}/details - Get model details
- POST /api/models/{name}/default - Set default model
- PUT /api/models/{name}/metadata - Update metadata
- GET /api/models/default - Get default model
- GET /api/models/library - Get Ollama library models (TODO: Not implemented)
- POST /api/models/create - Create custom model (OLLAMA ONLY, via CustomModelController delegates here)

**CustomModelController** (`/api/custom-models`)
- GET /api/custom-models - Get all custom models from DATABASE
- POST /api/custom-models - Create custom model in Ollama THEN Database
- PUT /api/custom-models/{id} - Update custom model (creates new version)
- DELETE /api/custom-models/{id} - Delete from DATABASE only (not from Ollama!)
- GET /api/custom-models/{id}/ancestry - Get model versions

**GgufModelConfigController** (`/api/gguf-models`)
- GET /api/gguf-models - Get GGUF configurations (java-llama-cpp only)
- POST /api/gguf-models - Create GGUF config
- PUT /api/gguf-models/{id} - Update GGUF config
- DELETE /api/gguf-models/{id} - Delete GGUF config
- GET /api/gguf-models/by-name/{name} - Get config by name
- PATCH /api/gguf-models/{id}/set-default - Set GGUF model as default

**LLMProviderController** (`/api/llm/providers`)
- GET /api/llm/providers - Get provider status
- POST /api/llm/providers/switch - Switch active provider
- GET /api/llm/providers/config - Get provider configuration
- PUT /api/llm/providers/config - Update provider config (NOT IMPLEMENTED!)

**ModelStoreController** (`/api/model-store`)
- (File not examined - may contain additional endpoints)

---

## Data Models

### GgufModelConfig (JPA Entity)
Purpose: Configuration for java-llama-cpp models
```
- name (unique)
- baseModel (filename)
- systemPrompt
- contextSize, gpuLayers, temperature, topP, topK, repeatPenalty
- advanced params: mirostat, ropeFreq, tfsZ, typicalP, etc.
- organizationalMeta: category, tags, description, isDefault
```
Issues:
- Only used by java-llama-cpp provider
- GgufModelConfigRepository has separate default model logic
- No integration with ModelMetadata

### ModelMetadata (JPA Entity)
Purpose: Metadata for Ollama models
```
- name (unique)
- size, description, specialties, publisher
- releaseDate, trainedUntil, license
- isDefault
- notes
```
Issues:
- Only for Ollama
- Separate default model logic from GgufModelConfig
- Two different "default" model systems!

### CustomModel (JPA Entity)
Purpose: Track custom models created in Ollama
```
- name, baseModel
- systemPrompt, temperature, topP, topK, repeatPenalty
- parentModel (for versioning)
- version
```
Issues:
- Data duplicated in Ollama (which also stores this)
- DELETE endpoint deletes from DB but NOT from Ollama!

---

## Critical Issues Found

### ISSUE #1: Duplicate Default Model Tracking
**Severity**: HIGH

Two separate systems track "default" models:
1. `ModelMetadata.isDefault` (Ollama models)
2. `GgufModelConfig.isDefault` (llama.cpp models)
3. `SettingsService.getDefaultModel()` (generic, stores in settings table?)

**Code Locations**:
- ModelController.java:189-200 (sets ModelMetadata default)
- GgufModelConfigController.java:73-79, 141-147, 184-199 (sets GgufModelConfig default)
- SettingsController likely has another default model mechanism

**Problem**: When switching providers, which default model is used?
- If switching from Ollama to llama.cpp, the system loses the model selection
- Frontend uses `chatStore.selectedModel` (localStorage) but backend uses different logic

**Example Conflict**:
```java
// ModelController sets this
metadataService.setDefaultModel("qwen:7b");  // Ollama model

// But GgufModelConfigController sets this
ggufModelConfigRepository.findByIsDefaultTrue();  // llama.cpp model

// Which does chat use? Undefined!
```

### ISSUE #2: Model Deletion Inconsistency
**Severity**: HIGH

CustomModelController.deleteCustomModel() only deletes from DATABASE:
```java
// Line 285: Only deletes from DB!
customModelService.deleteCustomModel(id);

// Response tells user model still exists in Ollama:
"note", "Model still exists in Ollama. Use 'ollama rm " + modelName + "' to remove it."
```

But ModelController.deleteModel() deletes from ACTIVE PROVIDER:
```java
// Line 86: Deletes from provider
llmProviderService.deleteModel(name);

// Then also from database
metadataService.deleteMetadata(name);
```

**Problem**: User thinks custom model is deleted, but it still exists in Ollama consuming storage!

### ISSUE #3: Create Model Implementation Split
**Severity**: MEDIUM

Two different endpoints create models:
1. POST /api/models/create (ModelController) 
2. POST /api/custom-models (CustomModelController)

Both delegate to OllamaService.createModel(), but:
- ModelController has TODO comments (lines 161, 242, 279)
- CustomModelController has better error handling (cleanup on failure)
- No clear documentation on which to use

### ISSUE #4: Missing Provider Integration
**Severity**: MEDIUM

OllamaService is injected directly in CustomModelController:
```java
private final OllamaService ollamaService;  // Line 30
```

But the LLMProvider pattern exists to abstract providers! Should use:
```java
private final LLMProviderService llmProviderService;
```

This creates tight coupling to Ollama.

### ISSUE #5: java-llama-cpp Provider Incomplete
**Severity**: MEDIUM

JavaLlamaCppProvider has multiple unimplemented methods:
```java
// Line 150: TODO: Add when library supports these methods
@Override
public void pullModel(String modelName, Consumer<String> progressConsumer) {
    throw new UnsupportedOperationException("pullModel not supported by java-llama-cpp");
}

// Line 403: TODO: Add when library supports these methods
@Override
public Map<String, Object> getModelDetails(String modelName) {
    throw new UnsupportedOperationException("getModelDetails not supported by java-llama-cpp");
}

// Line 410: TODO: Add when library supports these methods
@Override
public void createModel(...) {
    throw new UnsupportedOperationException("createModel not supported by java-llama-cpp");
}
```

**Impact**: Feature detection is missing - UI doesn't know which operations are supported!

### ISSUE #6: Configuration Persistence Not Implemented
**Severity**: MEDIUM**

LLMProviderController.updateProviderConfig() is a stub:
```java
@PutMapping("/config")
public ResponseEntity<ConfigUpdateResponse> updateProviderConfig(...) {
    // ... Line 141:
    log.warn("Config update not yet implemented - changes will be lost on restart!");
    response.setRestartRequired(true);
    return ResponseEntity.ok(response);
}
```

User can't change configuration through UI - must edit application.properties manually!

### ISSUE #7: Model Storage Path Inconsistency
**Severity**: MEDIUM**

Three different model storage locations:
1. Ollama: `~/.ollama/models/` (external)
2. llama.cpp embedded: `./models/library/` (internal)
3. Custom models: `./models/custom/` (internal)

No filesystem scanning/sync - models added to disk aren't auto-discovered.

### ISSUE #8: Missing ModelSelectionProperties Integration
**Severity**: LOW**

ModelSelectionService uses cached model selection based on prompt content:
```java
// Line 56: Cached by prompt hash - BUT settings aren't cached properly!
@Cacheable(value = "modelSelection", key = "#prompt.hashCode()")
public String selectModel(String prompt, String defaultModel)
```

Two problems:
1. Different prompts with same hash collision
2. Settings loaded from database on every call (not cached with model selection)

### ISSUE #9: Model Metadata Enrichment Logic
**Severity**: LOW**

ModelController.getAvailableModels() calls enrichmentService:
```java
// Lines 54-67: Multiple layers of metadata merging
enrichmentService.enrichModelInfo(enriched);  // Curated metadata
// Then override with database metadata
metadata.ifPresent(meta -> { ... });
```

If database has incomplete metadata, curated metadata might not be visible.

### ISSUE #10: Frontend API Inconsistency
**Severity**: MEDIUM**

Frontend uses inconsistent API endpoints:
```javascript
// ModelManager.vue - Line 1: Uses /api/custom-models
await fetch('/api/custom-models')

// ModelStore.vue - Uses /api/llm/models
await axios.get('/api/llm/models')

// ProviderSettings.vue - Uses /api/llm/providers/config
await axios.get('/api/llm/providers/config')
```

No standardized endpoint naming - hard to debug!

---

## Database Schema Issues

### ModelMetadata Table
```sql
CREATE TABLE model_metadata (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,  -- Ollama model name
    is_default BOOLEAN DEFAULT FALSE,   -- Default for Ollama provider
    ...
);
```

### GgufModelConfig Table
```sql
CREATE TABLE gguf_model_config (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,  -- Display name (different from Ollama!)
    base_model VARCHAR(255) NOT NULL,   -- GGUF filename
    is_default BOOLEAN DEFAULT FALSE,   -- Default for llama.cpp
    ...
);
```

### CustomModel Table
```sql
CREATE TABLE custom_model (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,         -- Ollama model name
    base_model VARCHAR(255) NOT NULL,   -- Parent model
    parent_model_id BIGINT,             -- Versioning
    version INT,
    ...
);
```

**Problem**: No referential integrity between providers!
- ModelMetadata for Ollama doesn't link to CustomModel
- GgufModelConfig names are display names, not filenames
- No way to know which provider owns which model

---

## Service Layer Issues

### OllamaService vs LLMProviderService

**OllamaService** (Legacy):
- Directly communicates with Ollama HTTP API
- Used by CustomModelController (tight coupling!)
- Complete implementation with vision support
- 900+ lines

**LLMProviderService** (New):
- Abstract facade over providers
- Proper abstraction pattern
- Should be used everywhere

**Problem**: Both exist, code uses whichever is convenient!

### ModelMetadataService Missing Details
No getAvailableModels() method - relies on LLMProviderService instead.

### Missing Services
No:
- ModelSyncService (sync filesystem with database)
- ModelValidationService (validate model files exist)
- ModelStorageService (unified access to all storage locations)
- ModelCapabilityService (query supported features per provider/model)

---

## Frontend Issues

### chatStore.selectedModel Fallback Chain

```javascript
// Multiple fallbacks, unclear priority
const selectedModel = ref(loadLastModel())  // localStorage

// Later, might load default from backend
const defaultModelResponse = await fetch('/api/models/default');
selectedModel.value = defaultModelResponse.model;

// Or smart selection kicks in
modelToUse = modelSelectionService.selectModel(prompt, defaultModel);
```

**Problem**: 
- Is default from Ollama or llama.cpp?
- What if both providers have different defaults?
- Smart selection doesn't know provider boundaries

### Model Manager Missing Features

ModelManager.vue shows:
- Installed models (from active provider)
- Available models (from Ollama Library?)
- Custom models (from database)

But:
- Can't see models from non-active providers
- Can't see what provider owns each model
- Can't switch providers AND see their models

---

## Recommendations (Priority Order)

### CRITICAL (Fix Immediately)

1. **Unify Default Model Tracking**
   - One AppSettings field: `activeDefaultModel` maps (providerName -> modelName)
   - Use when provider switches
   - Update all default-model setters to use this

2. **Fix CustomModel Deletion**
   - Delete from BOTH Ollama AND database
   - Or add confirmation dialog warning about Ollama cleanup needed

3. **Implement Configuration Persistence**
   - LLMProviderController.updateProviderConfig() should save to application.properties
   - Or use ConfigServer approach

### HIGH (Complete Before Release)

4. **Standardize API Endpoints**
   - Consolidate to single /api/models/* namespace
   - Clear separation: /api/models/ollama/*, /api/models/llamacpp/*, /api/models/custom/*
   - Or: /api/models/{provider}/*

5. **Provider Capability Detection**
   - Add isSupported(String operation) to LLMProvider interface
   - UI queries before showing features
   - Example: "pull" not supported by java-llama-cpp

6. **Remove OllamaService Direct Injection**
   - CustomModelController should use LLMProviderService
   - Allows supporting custom models in other providers

### MEDIUM (Next Sprint)

7. **Model Discovery and Sync**
   - Scan ./models/ directory
   - Compare with database
   - Auto-register new models found

8. **Fix Smart Model Selection**
   - Consider provider boundaries
   - Don't suggest model if not available in active provider
   - Cache settings separately from model selection

9. **Add ModelCapabilityService**
   - Query: "Can this model run vision?"
   - Query: "Does this provider support custom models?"
   - Query: "Can I pull/delete models with this provider?"

### LOW (Polish)

10. **Better Error Messages**
    - "Model X not available in provider Y. Available: ..."
    - "Feature Z not supported by provider Y. Try provider X"

---

## Testing Scenarios Needed

1. **Provider Switching**
   - Switch Ollama → llama.cpp with both having different default models
   - Verify correct model is used in chat

2. **Custom Model Lifecycle**
   - Create custom model in Ollama
   - Delete from database
   - Verify model still exists in Ollama (expected) OR is cleaned up (desirable)

3. **Multi-Provider Configuration**
   - Install models for both Ollama and llama.cpp
   - Switch between them
   - Verify models discovered correctly

4. **Model Discovery**
   - Add GGUF to ./models/library/
   - Restart app
   - Verify model appears in java-llama-cpp provider list

---

## Files Requiring Changes

### Backend
- `/src/main/java/io/javafleet/fleetnavigator/controller/ModelController.java` - Consolidate endpoints
- `/src/main/java/io/javafleet/fleetnavigator/controller/CustomModelController.java` - Use LLMProviderService
- `/src/main/java/io/javafleet/fleetnavigator/service/OllamaService.java` - Mark as legacy/internal
- `/src/main/java/io/javafleet/fleetnavigator/llm/providers/JavaLlamaCppProvider.java` - Implement missing methods
- `/src/main/java/io/javafleet/fleetnavigator/model/AppSettings.java` - Add providerDefaults map
- New: `ModelDiscoveryService.java` - Scan filesystem
- New: `ModelCapabilityService.java` - Feature detection

### Frontend
- `/frontend/src/components/ModelManager.vue` - Show provider per model
- `/frontend/src/stores/chatStore.js` - Fix default model selection logic
- `/frontend/src/components/ProviderSettings.vue` - Implement config save

### Database
- New migration: Add `provider_name` column to model tables
- New migration: Add `providerDefaults` to app_settings table

---

## Conclusion

The model management system is **architecturally sound** (LLMProvider abstraction is good) but **operationally broken** due to:

1. Incomplete implementation (too many TODOs)
2. Inconsistent data models (3 different default tracking systems)
3. Orphaned data (custom models deleted from DB but not Ollama)
4. Tight coupling (OllamaService everywhere)
5. Configuration not persistent

**Estimated effort to fix**: 
- Critical issues: 2-3 days
- High priority: 4-5 days
- Total: ~1 week to production-ready state

