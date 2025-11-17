# Universal Sampling Parameters System

**Datum:** 2025-11-15 16:36 CET
**Status:** ✅ **BUILD SUCCESS** - Vollständig implementiert und kompiliert

---

## Übersicht

Das **Universal Sampling Parameters System** gibt dir vollständige Kontrolle über alle LLM-Sampling-Parameter für **ALLE Modelle** (nicht nur Vision):

✅ **14 konfigurierbare Parameter**
✅ **4 Modell-Typen** (VISION, TEXT, CODE, CHAT) mit optimierten Defaults
✅ **Auto-Detection** basierend auf Modellnamen
✅ **5 Presets** (ultra-precise, balanced, detailed, creative, mirostat)
✅ **REST API** mit Hilfe in Deutsch und Englisch
✅ **Volle Backwards-Kompatibilität**

---

## Architektur-Änderungen

### Neue/Geänderte Dateien

**Core DTO:**
- `src/main/java/io/javafleet/fleetnavigator/dto/SamplingParameters.java` (NEU)
  - Universal für alle Modelltypen
  - 14 Parameter + Model-Type-Enum
  - Auto-defaults basierend auf Modellname
  - 5 vordefinierte Presets

**Controller:**
- `src/main/java/io/javafleet/fleetnavigator/controller/SamplingParametersController.java` (NEU)
  - REST API `/api/sampling/*`
  - Presets, Defaults, Validierung, Hilfe (DE/EN)

**Provider:**
- `src/main/java/io/javafleet/fleetnavigator/llm/providers/LlamaCppProvider.java` (GEÄNDERT)
  - Vollständige Unterstützung aller 14 Parameter
  - Overloaded `chatStreamWithVision` Methoden
  - Parameter-Logging für Debugging

**Request DTO:**
- `src/main/java/io/javafleet/fleetnavigator/dto/ChatRequest.java` (GEÄNDERT)
  - Neues Feld: `samplingParameters`
  - Alte Felder deprecated (aber noch unterstützt)

---

## Die 14 Parameter im Detail

### 1. Generation Control
| Parameter | Default | Range | Beschreibung |
|-----------|---------|-------|--------------|
| `maxTokens` | 512 | 50-2048 | Maximale Token-Anzahl |

### 2. Sampling Parameters
| Parameter | Default | Range | Beschreibung |
|-----------|---------|-------|--------------|
| `temperature` | 0.7 | 0.0-2.0 | Zufälligkeit/Kreativität |
| `topP` | 0.9 | 0.0-1.0 | Nucleus Sampling |
| `topK` | 40 | 0-100 | Top-K Sampling |
| `minP` | 0.05 | 0.0-1.0 | Min. Wahrscheinlichkeit |

### 3. Repetition Control
| Parameter | Default | Range | Beschreibung |
|-----------|---------|-------|--------------|
| `repeatPenalty` | 1.1 | 1.0-1.5 | Bestraft Wiederholungen |
| `repeatLastN` | 64 | 0-512 | Context für Repeat-Detection |
| `presencePenalty` | 0.0 | -2.0 to 2.0 | Ermutigt neue Themen |
| `frequencyPenalty` | 0.0 | -2.0 to 2.0 | Reduziert Wort-Wiederholungen |

### 4. Mirostat (Advanced)
| Parameter | Default | Range | Beschreibung |
|-----------|---------|-------|--------------|
| `mirostatMode` | 0 | 0, 1, 2 | Perplexity-basiertes Sampling |
| `mirostatTau` | 5.0 | 0-10 | Target Entropy |
| `mirostatEta` | 0.1 | 0.01-1.0 | Learning Rate |

### 5. Stop Conditions
| Parameter | Default | Typ | Beschreibung |
|-----------|---------|-----|--------------|
| `stopSequences` | [...] | String[] | Generation stoppen |
| `customSystemPrompt` | null | String | Überschreibt Standard-Prompt |

---

## Model-Type-Specific Defaults

### AUTO-DETECTION

Die `autoDefaults(String modelName)` Methode erkennt automatisch den Modelltyp:

```java
// Vision models
if (modelName.contains("llava") || modelName.contains("vision") ||
    modelName.contains("qwen-vl") || modelName.contains("cogvlm") ||
    modelName.contains("moondream") || modelName.contains("mmproj")) {
    return visionDefaults();  // temp=0.1, sehr faktisch!
}

// Code models
if (modelName.contains("coder") || modelName.contains("code") ||
    modelName.contains("starcoder") || modelName.contains("codellama")) {
    return codeDefaults();  // temp=0.2, maxTokens=2048
}

// Chat models
if (modelName.contains("chat") || modelName.contains("instruct")) {
    return chatDefaults();  // temp=0.8, kreativ
}

// Default: General text
return textDefaults();  // temp=0.7, ausgewogen
```

### Typ-spezifische Defaults

**VISION** (LLaVA, Qwen-VL):
- `temperature = 0.1` ← **SEHR WICHTIG!** Verhindert Halluzination
- `maxTokens = 300` (kurze, präzise Beschreibungen)
- `repeatPenalty = 1.15` (Anti-Loop)
- Custom System Prompt: "Describe only what you actually see..."

**TEXT** (Llama, Mistral, Qwen):
- `temperature = 0.7` (ausgewogen)
- `maxTokens = 512` (moderate Länge)
- `repeatPenalty = 1.1` (leicht)

**CODE** (Qwen-Coder, CodeLlama):
- `temperature = 0.2` (präzise)
- `maxTokens = 2048` (lang für Code)
- `topP = 0.95` (mehr Diversität)
- Stop: `["```", "\n\n\n"]`

**CHAT**:
- `temperature = 0.8` (kreativer)
- `maxTokens = 1024` (ausführlich)
- `topP = 0.9`

---

## REST API Endpoints

### 1. Alle Presets abrufen
```bash
GET /api/sampling/presets
```

**Response:**
```json
{
  "ultra-precise": { "temperature": 0.05, "maxTokens": 200, ... },
  "balanced": { "temperature": 0.1, "maxTokens": 300, ... },
  "detailed": { "temperature": 0.2, "maxTokens": 800, ... },
  "creative": { "temperature": 0.5, "maxTokens": 500, ... },
  "mirostat": { "mirostatMode": 2, "mirostatTau": 4.0, ... }
}
```

### 2. Spezifisches Preset
```bash
GET /api/sampling/presets/balanced
```

### 3. Universal Defaults (für "Reset" Button)
```bash
GET /api/sampling/defaults
```

**Response:**
```json
{
  "parameters": {
    "maxTokens": 512,
    "temperature": 0.7,
    "topP": 0.9,
    ...
  },
  "descriptions": {
    "temperature": "Randomness (0.0-2.0). Low=factual, High=creative...",
    ...
  },
  "recommendations": {
    "minimal-hallucination": "Use ultra-precise preset or temp=0.05-0.1",
    ...
  }
}
```

### 4. Auto-Defaults für Modell
```bash
GET /api/sampling/defaults/auto/llava-v1.6-mistral-7b
```

**Response:** Vision-optimierte Defaults (temp=0.1, etc.)

### 5. Defaults für Modell-Typ
```bash
GET /api/sampling/defaults/VISION
GET /api/sampling/defaults/TEXT
GET /api/sampling/defaults/CODE
GET /api/sampling/defaults/CHAT
```

### 6. Parameter validieren
```bash
POST /api/sampling/validate
Content-Type: application/json

{
  "temperature": 0.8,
  "maxTokens": 1500,
  "mirostatMode": 2,
  "topP": 0.5
}
```

**Response:**
```json
{
  "valid": false,
  "warnings": {
    "temperature": "High temperature (0.8) may cause hallucination in vision models...",
    "mirostat": "Mirostat is enabled but top_p=0.5. Set top_p=1.0 when using Mirostat..."
  },
  "parameters": { ... }
}
```

### 7. Hilfe in Deutsch
```bash
GET /api/sampling/help/de
```

### 8. Hilfe in Englisch
```bash
GET /api/sampling/help/en
```

**Response:**
```json
{
  "title": "Sampling Parameter Hilfe",
  "parameters": {
    "maxTokens": {
      "name": "Maximale Tokens",
      "description": "Begrenzt die Länge der generierten Antwort",
      "range": "50-2048",
      "default": "512",
      "example": "200 = Kurze Antwort, 1000 = Ausführliche Beschreibung",
      "tip": "Für Vision: 200-500. Für Code: 1024-2048. Für Chat: 512-1024"
    },
    ...
  },
  "quickTips": {
    "vision": "Vision-Modelle: temperature=0.05-0.2, maxTokens=200-500, repeatPenalty=1.15",
    ...
  },
  "commonIssues": {
    "hallucination": "Halluzinationen? → temperature auf 0.1 senken",
    "repetition": "Wiederholungen? → repeatPenalty auf 1.2-1.3 erhöhen",
    ...
  }
}
```

---

## Verwendung in Chat-Request

### Option 1: Auto-Defaults (Empfohlen)
```javascript
POST /api/chat/stream

{
  "message": "Beschreibe dieses Bild",
  "model": "llava-v1.6-mistral-7b.Q4_K_M.gguf",
  "images": ["data:image/jpeg;base64,..."]
  // samplingParameters weggelassen → verwendet autoDefaults() basierend auf Modellname
}
```

### Option 2: Mit Preset
```javascript
{
  "message": "Beschreibe dieses Bild faktisch",
  "model": "llava-v1.6-mistral-7b.Q4_K_M.gguf",
  "images": ["data:image/jpeg;base64,..."],
  "samplingParameters": {
    "maxTokens": 200,
    "temperature": 0.05,
    "topP": 0.8,
    "topK": 20,
    "repeatPenalty": 1.2
  }
}
```

### Option 3: Custom Parameters
```javascript
{
  "message": "Analysiere diese technische Zeichnung im Detail",
  "model": "llava-v1.6-mistral-7b.Q4_K_M.gguf",
  "images": ["data:image/jpeg;base64,..."],
  "samplingParameters": {
    "maxTokens": 1000,
    "temperature": 0.05,
    "topP": 0.9,
    "topK": 30,
    "repeatPenalty": 1.3,
    "customSystemPrompt": "You are a technical drawing analyst. Describe all visible dimensions, labels, and technical details precisely."
  }
}
```

---

## Build-Lösung

### Problem
Lombok Annotation Processing funktionierte nicht automatisch mit Spring Boot 3.2.0.

### Lösung
Die Lösung war **KEINE** zusätzliche Konfiguration! Das Problem waren **Syntax-Fehler in meinem Code**:
1. `VisionParametersController.java` musste in `SamplingParametersController.java` umbenannt werden
2. Drei Preset-Methoden verwendeten noch `VisionParameters` statt `SamplingParameters`

Nach Korrektur dieser Fehler:
- ✅ Lombok funktioniert out-of-the-box mit Spring Boot
- ✅ Build Zeit: 9.6s (compile), 6.2s (package)
- ✅ JAR: `target/fleet-navigator-0.2.7.jar`

**WICHTIG:** Keine speziellen Maven-Plugins nötig! Spring Boot Parent 3.2.0 konfiguriert Lombok automatisch.

---

## Nächste Schritte

### Frontend-Integration (TODO)

1. **Parameter-UI** in `Settings.vue`:
   - Slider für alle 14 Parameter
   - Preset-Dropdown (ultra-precise, balanced, detailed, creative, mirostat)
   - "Reset to Defaults" Button
   - "Hilfe" Button → Modal mit /api/sampling/help/de

2. **Model-Type-Detection**:
   - Auto-detect beim Model-Auswahl
   - Zeige empfohlene Defaults an
   - Warnungen bei problematischen Kombinationen

3. **Validierung**:
   - Live-Validierung während Eingabe
   - Zeige Warnungen von `/api/sampling/validate`

### Tests

```bash
# Starte Fleet Navigator
java -jar target/fleet-navigator-0.2.7.jar

# Teste Presets
curl http://localhost:2025/api/sampling/presets

# Teste Auto-Detection
curl http://localhost:2025/api/sampling/defaults/auto/llava-v1.6-mistral-7b

# Teste Hilfe
curl http://localhost:2025/api/sampling/help/de
```

---

## Zusammenfassung

Das System ist **vollständig implementiert und funktional**:

✅ Alle 14 Parameter konfigurierbar
✅ 4 Modell-Typen mit optimierten Defaults
✅ Auto-Detection basierend auf Modellname
✅ 5 Presets für häufige Use-Cases
✅ REST API mit Validierung und Hilfe
✅ Vollständig dokumentiert (DE + EN)
✅ Build erfolgreich (9.6s)
✅ Backwards-kompatibel

**Bereit für Frontend-Integration!**

---

**Erstellt:** 2025-11-15 16:36 CET
**Build-Status:** ✅ SUCCESS
**JAR:** `fleet-navigator-0.2.7.jar` (96 MB mit Frontend)
