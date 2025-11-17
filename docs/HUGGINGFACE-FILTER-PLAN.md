# HuggingFace GGUF-Modell Filter - Implementierungsplan

**Datum:** 2025-11-14
**Status:** ⏳ GEPLANT
**Ziel:** Vollständiger Zugriff auf alle HuggingFace GGUF-Modelle mit intelligenten Filtern

---

## 🎯 User Requirements

> "Ich möchte auf alle GGUF zugreifen können!"

**Was der Nutzer will:**
1. ✅ Zugriff auf **ALLE** GGUF-Modelle von HuggingFace (nicht nur 12 handverlesene)
2. ✅ **Quick-Filter-Buttons** für häufige Kategorien
3. ✅ **Freie Suche** für beliebige Begriffe

---

## 📋 Aktuelle Situation

### Problem 1: Zwei getrennte Systeme

**Model Store (Registry):**
- Datei: `src/main/java/io/javafleet/fleetnavigator/llm/ModelRegistry.java`
- Nur ~12 Modelle manuell gepflegt
- Eingeschränkt, nicht skalierbar
- **WIRD BEIBEHALTEN** für kuratierte "Featured Models"

**HuggingFace Suche:**
- Controller: `ModelStoreController.java` → `/api/model-store/huggingface/*`
- Service: `HuggingFaceService.java`
- Frontend: `ModelManager.vue` (Zeilen 939-1000)
- **FUNKTIONIERT BEREITS**, aber unvollständig!

### Problem 2: Fehlende Filter

**Aktuell vorhanden:**
- ✅ "⭐ Beliebte Modelle" Button (Zeile 942-947)
- ✅ "🇩🇪 Deutsche Modelle" Button (Zeile 948-954)
- ✅ Suchfeld (Zeile 923-938)

**Fehlt:**
- ❌ Filter: Instruct/Chat
- ❌ Filter: Code
- ❌ Filter: Vision
- ❌ Bessere UI-Organisation
- ❌ Kombinierbare Filter

---

## 🏗️ Implementierungsplan

### Phase 1: Backend - Neue Filter-Endpoints

**Datei:** `src/main/java/io/javafleet/fleetnavigator/controller/ModelStoreController.java`

#### Neue Endpoints hinzufügen:

```java
/**
 * Instruct/Chat Modelle
 */
@GetMapping("/huggingface/instruct")
public ResponseEntity<List<HuggingFaceModelInfo>> getInstructModels(
        @RequestParam(defaultValue = "30") int limit
) {
    log.info("Fetching Instruct/Chat GGUF models (limit: {})", limit);
    List<HuggingFaceModelInfo> models = huggingFaceService.searchInstructModels(limit);
    return ResponseEntity.ok(models);
}

/**
 * Code-Generation Modelle
 */
@GetMapping("/huggingface/code")
public ResponseEntity<List<HuggingFaceModelInfo>> getCodeModels(
        @RequestParam(defaultValue = "30") int limit
) {
    log.info("Fetching Code GGUF models (limit: {})", limit);
    List<HuggingFaceModelInfo> models = huggingFaceService.searchCodeModels(limit);
    return ResponseEntity.ok(models);
}

/**
 * Vision Modelle (experimentell)
 */
@GetMapping("/huggingface/vision")
public ResponseEntity<List<HuggingFaceModelInfo>> getVisionModels(
        @RequestParam(defaultValue = "20") int limit
) {
    log.info("Fetching Vision GGUF models (limit: {})", limit);
    List<HuggingFaceModelInfo> models = huggingFaceService.searchVisionModels(limit);
    return ResponseEntity.ok(models);
}
```

---

### Phase 2: Service - Such-Implementierung

**Datei:** `src/main/java/io/javafleet/fleetnavigator/service/HuggingFaceService.java`

#### Neue Methoden:

```java
/**
 * Search for Instruct/Chat models
 */
public List<HuggingFaceModelInfo> searchInstructModels(int limit) {
    List<HuggingFaceModelInfo> allResults = new ArrayList<>();
    // Suche nach verschiedenen Instruct-Varianten
    allResults.addAll(searchModels("instruct", limit / 3));
    allResults.addAll(searchModels("chat", limit / 3));
    allResults.addAll(searchModels("assistant", limit / 3));

    // Deduplizieren und nach Downloads sortieren
    return deduplicateAndSort(allResults, limit);
}

/**
 * Search for Code models
 */
public List<HuggingFaceModelInfo> searchCodeModels(int limit) {
    List<HuggingFaceModelInfo> allResults = new ArrayList<>();
    allResults.addAll(searchModels("coder", limit / 2));
    allResults.addAll(searchModels("code", limit / 2));
    return deduplicateAndSort(allResults, limit);
}

/**
 * Search for Vision models (experimental)
 */
public List<HuggingFaceModelInfo> searchVisionModels(int limit) {
    List<HuggingFaceModelInfo> allResults = new ArrayList<>();
    allResults.addAll(searchModels("llava", limit / 2));
    allResults.addAll(searchModels("vision", limit / 2));
    return deduplicateAndSort(allResults, limit);
}

/**
 * Deduplicate and sort by downloads
 */
private List<HuggingFaceModelInfo> deduplicateAndSort(
        List<HuggingFaceModelInfo> models,
        int limit
) {
    // Deduplizieren nach modelId
    Map<String, HuggingFaceModelInfo> uniqueModels = new LinkedHashMap<>();
    for (HuggingFaceModelInfo model : models) {
        uniqueModels.putIfAbsent(model.getModelId(), model);
    }

    // Nach Downloads sortieren
    return uniqueModels.values().stream()
        .sorted(Comparator.comparingInt(HuggingFaceModelInfo::getDownloads).reversed())
        .limit(limit)
        .collect(Collectors.toList());
}
```

---

### Phase 3: Frontend - Filter-Buttons

**Datei:** `frontend/src/components/ModelManager.vue`

#### Neue Filter-Buttons (nach Zeile 954):

```vue
<button
  @click="loadInstructHF"
  :disabled="isSearchingHF"
  class="px-3 py-1 bg-purple-100 hover:bg-purple-200 dark:bg-purple-900 dark:hover:bg-purple-800 text-purple-800 dark:text-purple-200 text-sm rounded transition-colors"
>
  💬 Instruct/Chat
</button>
<button
  @click="loadCodeHF"
  :disabled="isSearchingHF"
  class="px-3 py-1 bg-teal-100 hover:bg-teal-200 dark:bg-teal-900 dark:hover:bg-teal-800 text-teal-800 dark:text-teal-200 text-sm rounded transition-colors"
>
  💻 Code
</button>
<button
  @click="loadVisionHF"
  :disabled="isSearchingHF"
  class="px-3 py-1 bg-orange-100 hover:bg-orange-200 dark:bg-orange-900 dark:hover:bg-orange-800 text-orange-800 dark:text-orange-200 text-sm rounded transition-colors"
>
  👁️ Vision
</button>
```

#### JavaScript-Funktionen (nach Zeile 2243):

```javascript
async function loadInstructHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getInstructHuggingFaceModels(30)
    hfSearchResults.value = results
    console.log('Instruct HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load instruct models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

async function loadCodeHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getCodeHuggingFaceModels(30)
    hfSearchResults.value = results
    console.log('Code HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load code models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

async function loadVisionHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getVisionHuggingFaceModels(20)
    hfSearchResults.value = results
    console.log('Vision HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load vision models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}
```

---

### Phase 4: API Service - Neue Endpoints

**Datei:** `frontend/src/services/api.js`

```javascript
async getInstructHuggingFaceModels(limit = 30) {
  const response = await api.get('/model-store/huggingface/instruct', {
    params: { limit }
  })
  return response.data
},

async getCodeHuggingFaceModels(limit = 30) {
  const response = await api.get('/model-store/huggingface/code', {
    params: { limit }
  })
  return response.data
},

async getVisionHuggingFaceModels(limit = 20) {
  const response = await api.get('/model-store/huggingface/vision', {
    params: { limit }
  })
  return response.data
},
```

---

### Phase 5: UI-Verbesserungen

#### 5.1 Filter-Section umorganisieren

```vue
<!-- HuggingFace Quick Filters -->
<div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 mb-4">
  <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
    🔍 Quick Filters
  </h4>

  <!-- Row 1: Sprache & Beliebtheit -->
  <div class="flex gap-2 flex-wrap mb-2">
    <button @click="loadGermanHF" ...>🇩🇪 Deutsche Modelle</button>
    <button @click="loadPopularHF" ...>⭐ Beliebte Modelle</button>
  </div>

  <!-- Row 2: Kategorien -->
  <div class="flex gap-2 flex-wrap">
    <button @click="loadInstructHF" ...>💬 Instruct/Chat</button>
    <button @click="loadCodeHF" ...>💻 Code</button>
    <button @click="loadVisionHF" ...>👁️ Vision</button>
  </div>
</div>
```

#### 5.2 Aktiver Filter-Indikator

```javascript
const activeFilter = ref('') // 'german', 'popular', 'instruct', 'code', 'vision'

function loadGermanHF() {
  activeFilter.value = 'german'
  // ... existing code
}

// In Template:
<button
  @click="loadGermanHF"
  :class="activeFilter === 'german' ? 'ring-2 ring-green-500' : ''"
  ...
>
  🇩🇪 Deutsche Modelle
</button>
```

---

## 🚀 Erweiterte Features (Optional)

### Feature 1: Kombinierbare Filter

```javascript
const selectedFilters = ref({
  language: '', // 'german', 'multilingual'
  category: '', // 'instruct', 'code', 'vision'
  size: '',     // 'small', 'medium', 'large'
})

async function applyFilters() {
  let query = 'gguf'

  if (selectedFilters.value.language === 'german') {
    query += ' german OR deutsch'
  }
  if (selectedFilters.value.category === 'instruct') {
    query += ' instruct OR chat'
  }
  // ... etc

  const results = await api.searchHuggingFace(query, 50)
  hfSearchResults.value = results
}
```

### Feature 2: Sort & Group

```javascript
// Sortierung
const sortBy = ref('downloads') // 'downloads', 'likes', 'recent'

// Gruppierung
const groupBy = ref('none') // 'none', 'provider', 'size'
```

### Feature 3: Erweiterte Suche

```vue
<div class="flex gap-2">
  <input v-model="searchQuery" placeholder="Suche..." class="flex-1" />
  <select v-model="searchScope">
    <option value="all">Überall</option>
    <option value="name">Nur Name</option>
    <option value="description">Nur Beschreibung</option>
  </select>
</div>
```

---

## ✅ Testing Checklist

### Backend Tests
- [ ] `/api/model-store/huggingface/instruct` liefert Instruct-Modelle
- [ ] `/api/model-store/huggingface/code` liefert Code-Modelle
- [ ] `/api/model-store/huggingface/vision` liefert Vision-Modelle
- [ ] Deduplizierung funktioniert korrekt
- [ ] Sortierung nach Downloads funktioniert

### Frontend Tests
- [ ] Alle Filter-Buttons sind sichtbar
- [ ] Klick auf Filter lädt korrekte Modelle
- [ ] Download funktioniert für alle gefilterten Modelle
- [ ] Aktiver Filter wird visuell markiert
- [ ] Suchfeld funktioniert unabhängig von Filtern

### Integration Tests
- [ ] Download deutscher Modelle funktioniert
- [ ] Download Code-Modelle funktioniert
- [ ] Vision-Modelle zeigen Warnung (nicht mit llama.cpp kompatibel)
- [ ] File-Auswahl bei Modellen mit mehreren GGUF-Dateien

---

## 📊 Erwartete Ergebnisse

### Vorher (aktuell)
- Zugriff auf ~12 handverlesene Modelle (Model Registry)
- 2 Filter: Deutsche, Beliebte
- Suchfeld vorhanden

### Nachher (geplant)
- Zugriff auf **ALLE** GGUF-Modelle von HuggingFace
- 5 Filter: Deutsche, Beliebte, Instruct, Code, Vision
- Verbessertes Suchfeld
- Aktive Filter-Markierung
- Bessere UI-Organisation

---

## 🎨 UI-Mockup (Text)

```
┌─────────────────────────────────────────────┐
│  🔍 Quick Filters                           │
│                                             │
│  [🇩🇪 Deutsch] [⭐ Beliebte]                │
│  [💬 Instruct] [💻 Code] [👁️ Vision]        │
│                                             │
│  Suche: [________________] [🔍]             │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  📋 Ergebnisse (45 Modelle)                 │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ Qwen 2.5 3B Instruct               │   │
│  │ Alibaba • 120K Downloads           │   │
│  │ [⬇ Download]                       │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ DeepSeek Coder 6.7B                │   │
│  │ DeepSeek • 95K Downloads           │   │
│  │ [⬇ Download]                       │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

---

## 📝 Implementation Steps

1. **Backend:**
   - ✅ Fix HuggingFace download (DONE - siblings loading)
   - ⏳ Add new filter endpoints in `ModelStoreController.java`
   - ⏳ Implement search methods in `HuggingFaceService.java`
   - ⏳ Add deduplication logic

2. **Frontend:**
   - ⏳ Add filter buttons to `ModelManager.vue`
   - ⏳ Implement filter functions
   - ⏳ Add API calls to `api.js`
   - ⏳ Update UI styling

3. **Testing:**
   - ⏳ Test all filters
   - ⏳ Verify downloads work
   - ⏳ Check vision model warnings

4. **Polish:**
   - ⏳ Active filter highlighting
   - ⏳ Loading states
   - ⏳ Error handling
   - ⏳ German translations

---

## 🔄 Next Session Tasks

**Beim nächsten Mal umsetzen:**
1. Backend-Endpoints für Instruct/Code/Vision
2. Service-Methoden implementieren
3. Frontend-Buttons hinzufügen
4. Testen & Polieren

**Geschätzte Zeit:** 45-60 Minuten

---

**Status:** Plan erstellt, bereit zur Umsetzung! 🚀
