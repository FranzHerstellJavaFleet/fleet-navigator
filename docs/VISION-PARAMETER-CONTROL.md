## 🎛️ Vision Parameter Control System

**Datum:** 2025-11-15 16:16 CET
**Feature:** Vollständige Kontrolle über alle LLM-Sampling-Parameter für Vision-Modelle

---

## Übersicht

Du hast jetzt **vollständige Kontrolle** über alle Vision-Modell-Parameter! Das System bietet:

✅ **14 konfig urierbare Parameter**
✅ **5 vordefinierte Presets** (ultra-precise, balanced, detailed, creative, mirostat)
✅ **REST API** für Parameter-Management
✅ **Echtzeit-Parameter-Logging**
✅ **Validierung und Warnungen**

---

## 📊 Alle verfügbaren Parameter

### 1. Generation Control

| Parameter | Standard | Range | Beschreibung |
|-----------|----------|-------|--------------|
| `maxTokens` | 300 | 50-2048 | Maximale Token-Anzahl. Klein=kurz, groß=detailliert |

### 2. Sampling Parameters

| Parameter | Standard | Range | Beschreibung |
|-----------|----------|-------|--------------|
| `temperature` | 0.1 | 0.0-2.0 | Zufälligkeit. **0.05-0.3 für Vision empfohlen!** |
| `topP` | 0.9 | 0.0-1.0 | Nucleus Sampling. Kontrolliert Diversität |
| `topK` | 40 | 0-100 | Top-K Sampling. 0=disabled |
| `minP` | 0.05 | 0.0-1.0 | Min. Wahrscheinlichkeit für Token |

### 3. Repetition Control

| Parameter | Standard | Range | Beschreibung |
|-----------|----------|-------|--------------|
| `repeatPenalty` | 1.15 | 1.0-1.5 | Bestraft Wiederholungen. >1.0 empfohlen |
| `repeatLastN` | 64 | 0-512 | Tokens für Repeat-Detection |
| `presencePenalty` | 0.0 | -2.0 to 2.0 | Ermutigt neue Themen (positiv) |
| `frequencyPenalty` | 0.0 | -2.0 to 2.0 | Reduziert Wort-Wiederholungen |

### 4. Mirostat (Advanced)

| Parameter | Standard | Range | Beschreibung |
|-----------|----------|-------|--------------|
| `mirostatMode` | 0 | 0, 1, 2 | Perplexity-Sampling. 0=aus, 2=Mirostat 2.0 |
| `mirostatTau` | 5.0 | 0-10 | Target Entropy. 2-3=fokussiert, 7-10=divers |
| `mirostatEta` | 0.1 | 0.01-1.0 | Learning Rate für Mirostat |

### 5. Stop Conditions

| Parameter | Standard | Typ | Beschreibung |
|-----------|----------|-----|--------------|
| `stopSequences` | ["\n\n\n", ...] | String[] | Sequenzen die Generation stoppen |
| `customSystemPrompt` | null | String | Überschreibt Standard-Prompt |

---

## 🎨 Vordefinierte Presets

### 1. Ultra-Precise (Minimale Halluzination)
```json
{
  "maxTokens": 200,
  "temperature": 0.05,
  "topP": 0.8,
  "topK": 20,
  "minP": 0.1,
  "repeatPenalty": 1.2
}
```
**Verwendung:** Faktische Bildbeschreibung, technische Dokumentation

### 2. Balanced (Empfohlener Standard) ⭐
```json
{
  "maxTokens": 300,
  "temperature": 0.1,
  "topP": 0.9,
  "topK": 40,
  "minP": 0.05,
  "repeatPenalty": 1.15
}
```
**Verwendung:** Allgemeine Bild-Analyse, Standard-Use-Cases

### 3. Detailed (Ausführliche Analyse)
```json
{
  "maxTokens": 800,
  "temperature": 0.2,
  "topP": 0.95,
  "topK": 50,
  "minP": 0.05,
  "repeatPenalty": 1.1
}
```
**Verwendung:** Detaillierte Beschreibungen, komplexe Szenen

### 4. Creative (Narrative Beschreibungen)
```json
{
  "maxTokens": 500,
  "temperature": 0.5,
  "topP": 0.95,
  "topK": 60,
  "minP": 0.03,
  "repeatPenalty": 1.1
}
```
**Verwendung:** Kreative Bildbeschreibungen (**Vorsicht: mehr Halluzination!**)

### 5. Mirostat (Alternative Sampling-Strategie)
```json
{
  "maxTokens": 300,
  "temperature": 0.0,
  "topP": 1.0,
  "topK": 0,
  "mirostatMode": 2,
  "mirostatTau": 4.0,
  "mirostatEta": 0.1,
  "repeatPenalty": 1.15
}
```
**Verwendung:** Experimentell - konsistente Perplexität

---

## 🔌 API Endpunkte

### 1. Alle Presets abrufen
```bash
GET /api/vision/presets
```

**Response:**
```json
{
  "ultra-precise": { ... },
  "balanced": { ... },
  "detailed": { ... },
  "creative": { ... },
  "mirostat": { ... }
}
```

### 2. Spezifisches Preset abrufen
```bash
GET /api/vision/presets/balanced
```

**Response:**
```json
{
  "maxTokens": 300,
  "temperature": 0.1,
  "topP": 0.9,
  ...
}
```

### 3. Standard-Parameter mit Beschreibungen
```bash
GET /api/vision/defaults
```

**Response:**
```json
{
  "parameters": { ... },
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

### 4. Parameter validieren
```bash
POST /api/vision/validate
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
    "temperature": "High temperature (0.8) may cause hallucination...",
    "mirostat": "Mirostat is enabled but top_p=0.5. Set top_p=1.0..."
  },
  "parameters": { ... }
}
```

---

## 💡 Verwendung in Chat-Request

### Option 1: Mit Preset
```javascript
POST /api/chat/stream

{
  "message": "Beschreibe dieses Bild",
  "model": "llava-v1.6-mistral-7b.Q4_K_M.gguf",
  "images": ["data:image/jpeg;base64,..."],
  "visionParameters": {
    // Preset "ultra-precise" manuell nachbauen
    "maxTokens": 200,
    "temperature": 0.05,
    "topP": 0.8,
    "topK": 20,
    "repeatPenalty": 1.2
  }
}
```

### Option 2: Custom Parameters
```javascript
{
  "message": "Analysiere diese technische Zeichnung im Detail",
  "model": "llava-v1.6-mistral-7b.Q4_K_M.gguf",
  "images": ["data:image/jpeg;base64,..."],
  "visionParameters": {
    "maxTokens": 1000,           // Lang für Details
    "temperature": 0.05,         // Sehr faktisch
    "topP": 0.9,
    "topK": 30,
    "repeatPenalty": 1.3,        // Stark gegen Wiederholungen
    "customSystemPrompt": "You are a technical drawing analyst. Describe all visible dimensions, labels, and technical details precisely."
  }
}
```

### Option 3: Mirostat Experiment
```javascript
{
  "message": "Was ist auf diesem Bild?",
  "model": "llava-v1.6-mistral-7b.Q4_K_M.gguf",
  "images": ["data:image/jpeg;base64,..."],
  "visionParameters": {
    "maxTokens": 300,
    "temperature": 0.0,         // Ignored bei Mirostat
    "topP": 1.0,                // Disable für Mirostat!
    "topK": 0,                  // Disable für Mirostat!
    "mirostatMode": 2,          // Mirostat 2.0
    "mirostatTau": 3.5,         // Etwas fokussiert
    "mirostatEta": 0.1
  }
}
```

---

## 🧪 Experimente & Empfehlungen

### Halluzination reduzieren

**Problem:** Modell erfindet Details

**Lösung:**
```json
{
  "temperature": 0.05,      // SEHR niedrig!
  "topP": 0.7,              // Restriktiver
  "topK": 15,               // Wenige Optionen
  "minP": 0.15,             // Hoher Threshold
  "repeatPenalty": 1.2,
  "customSystemPrompt": "Describe ONLY what you see. Do not assume or invent any details."
}
```

### Wiederholungen vermeiden

**Problem:** Modell wiederholt sich

**Lösung:**
```json
{
  "repeatPenalty": 1.3,     // Stark bestrafen
  "repeatLastN": 128,       // Lange History
  "frequencyPenalty": 0.5,  // Wort-Wiederholungen bestrafen
  "presencePenalty": 0.3    // Neue Wörter bevorzugen
}
```

### Mehr Details erhalten

**Problem:** Beschreibung zu kurz

**Lösung:**
```json
{
  "maxTokens": 1200,        // Länger
  "temperature": 0.15,      // Etwas kreativer (aber nicht zu viel!)
  "topP": 0.95,             // Mehr Diversität
  "repeatPenalty": 1.1      // Weniger restriktiv
}
```

### Konsistenz über Requests

**Problem:** Jedes Mal andere Beschreibung

**Lösung:**
```json
{
  "temperature": 0.0,       // Deterministisch!
  "topP": 1.0,              // Oder Mirostat verwenden
  "topK": 1                 // Nur bestes Token
}
```

---

## 📋 Parameter-Kombinationen Cheat-Sheet

| Use-Case | Temperature | TopP | TopK | RepeatPenalty | MaxTokens |
|----------|-------------|------|------|---------------|-----------|
| **Technische Zeichnung** | 0.05 | 0.8 | 20 | 1.2 | 800 |
| **Foto-Beschreibung** | 0.1 | 0.9 | 40 | 1.15 | 300 |
| **OCR/Text-Extraktion** | 0.0 | 0.7 | 10 | 1.3 | 500 |
| **Kunst-Analyse** | 0.2 | 0.95 | 50 | 1.1 | 600 |
| **Screenshot** | 0.1 | 0.9 | 30 | 1.15 | 400 |
| **Diagramm** | 0.05 | 0.85 | 25 | 1.2 | 400 |
| **Meme/Fun** | 0.4 | 0.95 | 60 | 1.05 | 300 |

---

## 🔍 Logging & Debugging

Wenn Vision-Request gesendet wird, siehst du jetzt im Log:

```
INFO [LlamaCppProvider] 🎛️ Vision Request Parameters: {
  "messages": [...],
  "stream": true,
  "max_tokens": 300,
  "temperature": 0.1,
  "top_p": 0.9,
  "top_k": 40,
  "min_p": 0.05,
  "repeat_penalty": 1.15,
  "repeat_last_n": 64,
  "stop": ["\n\n\n", "USER:", "ASSISTANT:"]
}
```

**Damit kannst du:**
- Sehen, welche Parameter tatsächlich verwendet werden
- Parameter-Experimente nachvollziehen
- Probleme debuggen

---

## 🚀 Schnellstart

### 1. Presets in Frontend anzeigen
```bash
curl http://localhost:2025/api/vision/presets
```

### 2. Parameter validieren
```bash
curl -X POST http://localhost:2025/api/vision/validate \
  -H "Content-Type: application/json" \
  -d '{"temperature": 0.05, "maxTokens": 200}'
```

### 3. Mit Custom Parameters testen
```javascript
// Im Frontend:
const visionRequest = {
  message: "Was siehst du?",
  model: "llava-v1.6-mistral-7b.Q4_K_M.gguf",
  images: [base64Image],
  visionParameters: {
    temperature: 0.05,    // Sehr faktisch!
    maxTokens: 200,       // Kurz
    repeatPenalty: 1.2    // Anti-Loop
  }
};

fetch('/api/chat/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(visionRequest)
});
```

---

## 📚 Weiterführende Ressourcen

### llama.cpp Dokumentation
- Sampling Parameters: https://github.com/ggerganov/llama.cpp/wiki/Sampling
- Server API: https://github.com/ggerganov/llama.cpp/tree/master/examples/server

### Parameter-Guides
- Temperature Explained: https://smcleod.net/2025/04/comprehensive-guide-to-llm-sampling-parameters/
- Mirostat Algorithm: https://arxiv.org/abs/2007.14966

---

## ✅ Zusammenfassung

**Implementiert:**
- ✅ 14 konfigurierbare Parameter
- ✅ 5 Presets (ultra-precise, balanced, detailed, creative, mirostat)
- ✅ REST API für Parameter-Management
- ✅ Validierung mit Warnungen
- ✅ Parameter-Logging in LlamaCppProvider
- ✅ Backwards-kompatibel (Defaults wenn nicht angegeben)

**Build-Status:** ✅ SUCCESS (11.1s)

**Bereit für Tests:** ✅ Ja!

---

**Nächste Schritte:**
1. Fleet Navigator neu starten
2. Presets abrufen: `GET /api/vision/presets`
3. Mit verschiedenen Parametern experimentieren
4. MicroService.png mit `ultra-precise` Preset testen!

---

**Erstellt:** 2025-11-15 16:16 CET
**Neue Dateien:**
- `VisionParameters.java` (DTO mit 14 Parametern + 5 Presets)
- `VisionParametersController.java` (REST API)
**Geänderte Dateien:**
- `ChatRequest.java` (VisionParameters-Feld hinzugefügt)
- `LlamaCppProvider.java` (Vollständige Parameter-Unterstützung)
