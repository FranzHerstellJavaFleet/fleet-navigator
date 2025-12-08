# Unified Model Management - Implementierung

## ✨ Was wurde umgesetzt?

Eine **zentrale, provider-abhängige Modellverwaltung** im Model Manager!

### 🎯 Konzept:

```
┌─────────────────────────────────────────────┐
│        📊 Model Manager (Zentral)           │
├─────────────────────────────────────────────┤
│                                              │
│  Tab 1: Installierte Modelle                │
│  → Zeigt alle installierten Modelle         │
│                                              │
│  Tab 2: Verfügbare Modelle ← PROVIDER!      │
│  ┌────────────────────────────────────────┐ │
│  │ Provider = Ollama                      │ │
│  │ → Zeigt Ollama Library                 │ │
│  │ → Pull-Funktion                        │ │
│  ├────────────────────────────────────────┤ │
│  │ Provider = llama.cpp                   │ │
│  │ → Zeigt Model Store (9 deutsche GGUFs)│ │
│  │ → HuggingFace Download mit Modal      │ │
│  ├────────────────────────────────────────┤ │
│  │ Provider = OpenAI / Andere             │ │
│  │ → Leere Liste (keine Library)         │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## 🔧 Implementierte Änderungen

### Backend (bereits vorhanden):
✅ `/api/llm/providers/active` - Gibt aktiven Provider zurück
✅ `/api/model-store/*` - Model Store API für llama.cpp

### Frontend:

#### 1. ModelManager.vue erweitert:
- ✅ **Provider Detection** beim Laden
- ✅ **Dynamisches Laden** der Modelle basierend auf Provider:
  - Ollama → `api.getOllamaLibraryModels()`
  - llama.cpp → `/api/model-store/all`
  - Andere → Leere Liste
- ✅ **Provider-spezifische Anzeige**:
  - Ollama: Klassische Modellgröße + Datum
  - llama.cpp: Beschreibung, Rating, Downloads
- ✅ **Download-Funktionalität**:
  - Ollama: Bestehender Dialog
  - llama.cpp: Neues großes Modal mit Progress

#### 2. SettingsModal.vue bereinigt:
- ✅ **Model Store Tab entfernt**
- ✅ Imports bereinigt
- ✅ Tab-Liste aktualisiert

---

## 📊 Provider-Unterstützung

| Provider | Verfügbare Modelle | Download | Funktionalität |
|----------|-------------------|----------|----------------|
| **Ollama** | ~250 kuratiert | Pull via Ollama | Bestehender Dialog |
| **llama.cpp** | 9 deutsche GGUFs | HuggingFace Download | Großes Modal mit Progress |
| **OpenAI** | - | - | Nur API Keys |
| **Weitere** | - | - | Erweiterbar |

---

## 🏪 Model Store für llama.cpp

### Verfügbare Modelle:
1. **Qwen 2.5 (3B)** - 1.97 GB - ⭐ Empfohlen für Deutsch
2. **Llama 3.2 (3B)** - 2.02 GB - Schnell & gut
3. **Qwen 2.5 (7B)** - 4.73 GB - Premium-Qualität
4. **Qwen 2.5 Coder (3B/7B)** - Code-Generierung
5. **Phi-3 Mini** - 2.36 GB - Microsoft
6. **Mistral 7B** - 4.37 GB - Vielseitig
7. **Kompakte Modelle** (1B-1.5B) - Für schwache Hardware

### Features:
- ✅ Detaillierte Beschreibungen
- ✅ Ratings & Download-Zahlen
- ✅ Sprachen & Use-Cases
- ✅ RAM-Anforderungen
- ✅ Großes Download-Modal mit:
  - Echtzeit-Progress
  - Geschwindigkeit (MB/s)
  - Geschätzte Zeit
  - Abbrechen-Funktion

---

## 🚀 Wie es funktioniert

### Beim Öffnen des Model Managers:

1. **Provider wird geladen:**
   ```javascript
   GET /api/llm/providers/active
   → { "provider": "llamacpp", "available": true }
   ```

2. **Modelle werden geladen:**
   ```javascript
   if (provider === 'ollama') {
     models = await api.getOllamaLibraryModels()
   } else if (provider === 'llamacpp') {
     models = await api.get('/model-store/all')
   }
   ```

3. **Anzeige passt sich an:**
   - **Banner**: "Ollama Library" vs "Model Store"
   - **Modell-Info**: Unterschiedliche Felder
   - **Download**: Unterschiedliche Dialoge

### Beim Download (llama.cpp):

1. **Modal öffnet sich** mit Modell-Info
2. **EventSource** startet für SSE:
   ```javascript
   GET /api/model-store/download/{modelId}
   → Stream: progress events
   ```
3. **Progress wird geparst:**
   - Prozent, Größe, Geschwindigkeit
   - Status-Log wird aktualisiert
4. **Nach Abschluss:**
   - Modal schließt nach 2 Sekunden
   - Modelle werden neu geladen
   - Erscheint unter "Installierte Modelle"

---

## 📂 Verzeichnisstruktur

```
models/
├── library/          ← Heruntergeladene Modelle (Model Store)
│   └── qwen2.5-3b-instruct-q4_k_m.gguf
└── custom/           ← Eigene hochgeladene Modelle
    └── Llama-3.2-1B-Instruct-Q4_K_M.gguf
```

---

## 🎯 Erweiterbarkeit für neue Provider

Um einen neuen Provider hinzuzufügen:

### Backend:
1. Neues `XxxProvider.java` implementieren
2. In `LLMProviderService` registrieren

### Frontend (ModelManager.vue):
```javascript
async function loadLibraryModels() {
  if (activeProvider.value === 'ollama') {
    // Ollama logic
  } else if (activeProvider.value === 'llamacpp') {
    // llama.cpp logic
  } else if (activeProvider.value === 'mein-neuer-provider') {
    // Neue Provider-Logik hier
    availableModels.value = await api.get('/mein-provider/models')
  }
}
```

### Template (ModelManager.vue):
```vue
<span v-if="activeProvider === 'ollama'">
  Ollama Info
</span>
<span v-else-if="activeProvider === 'llamacpp'">
  llama.cpp Info
</span>
<span v-else-if="activeProvider === 'mein-neuer-provider'">
  Neue Provider Info
</span>
```

---

## ✅ Testing

### Schritte:
1. **Starte Fleet Navigator**
   ```bash
   ./START.sh
   ```

2. **Wechsle Provider** in Einstellungen → LLM Provider

3. **Öffne Model Manager** (🧠 Icon in TopBar)

4. **Tab "Verfügbare Modelle":**
   - Bei **Ollama**: Sollte Ollama Library zeigen
   - Bei **llama.cpp**: Sollte Model Store mit 9 Modellen zeigen

5. **Teste Download** (llama.cpp):
   - Klicke "⬇ Download" bei einem Modell
   - Großes Modal sollte erscheinen
   - Progress sollte sichtbar sein

6. **Nach Download:**
   - Modell erscheint unter "Installierte Modelle"
   - Kann im Chat ausgewählt werden

---

## 📝 Wichtige Dateien

### Backend:
- `LLMProviderController.java` - Provider API
- `ModelStoreController.java` - Model Store API
- `ModelRegistry.java` - 9 kuratierte Modelle
- `ModelDownloadService.java` - HuggingFace Download
- `LlamaCppProvider.java` - Provider mit neuer Verzeichnisstruktur

### Frontend:
- `ModelManager.vue` - Zentrale Modellverwaltung (erweitert)
- `ModelDownloadModal.vue` - Großes Download-Modal
- `SettingsModal.vue` - Model Store Tab entfernt
- `ModelStore.vue` - Standalone (optional, nicht mehr in Settings)

---

## 🎉 Vorteile

✅ **Eine zentrale Stelle** für alle Modelle
✅ **Provider-unabhängig** - leicht erweiterbar
✅ **Bessere UX** - User muss nicht wechseln
✅ **Konsistente Bedienung** - gleiche UI für alle Provider
✅ **Zukunftssicher** - neue Provider einfach hinzufügen

---

**Erstellt:** 2025-11-11
**Version:** 0.2.9
**Autor:** JavaFleet Systems Consulting & Claude Code
