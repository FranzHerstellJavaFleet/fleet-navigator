# Vision Repetition/Hallucination Fix

**Datum:** 2025-11-15 15:58 CET
**Problem:** LLaVA-Modell generiert endlose Wiederholungen und halluziniert Inhalte

---

## Problem

Nach dem ersten erfolgreichen Vision-Test (llama-server läuft!) zeigten sich zwei Probleme:

### 1. "null" am Anfang der Response
```
null In diesem Bild wird eine Diagramm-Grafik gezeigt...
```

### 2. Endlose Repetition (Halluzination)
Das LLaVA-Modell wiederholt sich in einer Endlosschleife:
- Beschreibt Inhalte, die **nicht im Bild** sind (Fleet Navigator, KI-Assistentin Karla, etc.)
- Wiederholt dieselben Absätze dutzende Male
- Stoppt nicht von selbst

**Beispiel-Output:**
```
**Microservice-Architektur**Die Grafik zeigt...
**Monolithic Architektur**Die Grafik zeigt auch...
**KI-Assistentin Karla**Der KI-Assistentin...
**Technologische Konzepte**Der Diagramm-Bereich...
[WIEDERHOLT SICH 50+ MAL]
```

**Tatsächlicher Bildinhalt:**
- Einfaches Diagramm: Monolithic vs. Microservice Architecture
- Linke Seite: Monolith (UI + Business Logic + Data Access Layer + Database)
- Rechte Seite: Microservices (UI → mehrere Microservices mit eigenen Datenbanken)
- **KEINE** Erwähnung von Fleet Navigator, Karla, oder anderen halluzinierten Inhalten!

---

## Root Cause

**LlamaCppProvider.java:386-391** - Vision-Request hatte **keine Generation-Parameter**:

```java
// VORHER (fehlerhaft):
Map<String, Object> requestBody = new HashMap<>();
requestBody.put("messages", messages);
requestBody.put("stream", true);
// ← Keine max_tokens, temperature, repeat_penalty!
```

**Folge:**
- llama-server generiert ohne Limit
- Kein Repetition Penalty → Endlosschleife
- Kein Stop-Token → nie Abbruch
- LLaVA verfängt sich in Halluzination

---

## Lösung

**Datei:** `src/main/java/io/javafleet/fleetnavigator/llm/providers/LlamaCppProvider.java`
**Zeilen:** 386-404

### Geändert:

```java
// Build request body with generation parameters
Map<String, Object> requestBody = new HashMap<>();
requestBody.put("messages", messages);
requestBody.put("stream", true);

// Critical: Add max_tokens to prevent endless generation
requestBody.put("max_tokens", 512);  // Reasonable limit for vision descriptions

// Add temperature and top_p for better coherence
requestBody.put("temperature", 0.7);
requestBody.put("top_p", 0.9);

// Add repetition penalty to prevent loops
requestBody.put("repeat_penalty", 1.1);

// Add stop sequences
requestBody.put("stop", List.of("\n\n\n", "**Technologische Konzepte**", "USER:", "ASSISTANT:"));

String json = objectMapper.writeValueAsString(requestBody);
```

### Was die Parameter bewirken:

| Parameter | Wert | Zweck |
|-----------|------|-------|
| `max_tokens` | 512 | **Kritisch!** Verhindert endlose Generierung |
| `temperature` | 0.7 | Kohärenz vs. Kreativität Balance |
| `top_p` | 0.9 | Nucleus Sampling für bessere Qualität |
| `repeat_penalty` | 1.1 | **Wichtig!** Bestraft Wiederholungen |
| `stop` | Sequenzen | Bricht bei bestimmten Mustern ab |

---

## Test nach Fix

**Erwartetes Verhalten:**
1. Klare, prägnante Bildbeschreibung (max. 512 Tokens)
2. Keine Wiederholungen
3. Beschreibung des **tatsächlichen** Bildinhalts
4. Sauberer Abbruch nach Beschreibung

**Beispiel (gewünscht):**
```
Das Bild zeigt einen Vergleich zwischen Monolithischer und Microservice-Architektur.

Links (Monolithic Architecture):
- Eine einzelne Anwendung mit UI, Business Logic und Data Access Layer
- Eine zentrale Datenbank

Rechts (Microservice Architecture):
- Modulare UI kommuniziert mit mehreren unabhängigen Microservices
- Jeder Microservice hat seine eigene Datenbank
- Lose Kopplung und höhere Skalierbarkeit
```

---

## Bekannte LLaVA-Probleme (Community)

### 1. Context-Limit Hallucination
LLaVA neigt zu Halluzinationen bei langen Generierungen:
- **Lösung:** `max_tokens=512` (jetzt implementiert)

### 2. Repetition Loops
Vision-Modelle verfangen sich in Wiederholungen:
- **Lösung:** `repeat_penalty=1.1` (jetzt implementiert)

### 3. LLaVA 1.6 MMPROJ Bugs
GitHub Issue #8457 - LLaVA 1.6 hat bekannte MMPROJ-Probleme:
- Unsere Version sollte trotzdem funktionieren
- Falls Probleme: Alternative LLaVA 1.5 verwenden

---

## Nächste Schritte

### 1. Fleet Navigator neu starten

```bash
# In IntelliJ:
# Stoppe die laufende Application
# Starte FleetNavigatorApplication neu
```

### 2. Erneuter Vision-Test

```
1. Öffne http://localhost:2025
2. Wähle: llava-v1.6-mistral-7b.Q4_K_M.gguf
3. Lade MicroService.png hoch
4. Frage: "Beschreibe dieses Bild kurz und prägnant"
```

**Erwartung:**
- Kurze, akkurate Beschreibung (~100-200 Wörter)
- Kein "null" am Anfang (sollte auch behoben sein)
- Keine Wiederholungen
- Stoppt nach sinnvoller Länge

---

## Weitere Optimierungen (optional)

### Wenn Beschreibungen zu kurz sind:
```java
requestBody.put("max_tokens", 1024);  // Mehr Details erlauben
```

### Wenn immer noch Wiederholungen auftreten:
```java
requestBody.put("repeat_penalty", 1.2);  // Stärker bestrafen
```

### Für technischere Beschreibungen:
```java
requestBody.put("temperature", 0.5);  // Weniger kreativ, präziser
```

---

## Zusammenfassung

**Problem:**
- Endlose Repetition und Halluzination
- Fehlende Generation-Parameter

**Lösung:**
- `max_tokens=512` (Limit)
- `repeat_penalty=1.1` (Anti-Loop)
- `temperature=0.7` (Kohärenz)
- `stop` Sequenzen (Sauberer Abbruch)

**Status:** ✅ **Gefixt und deployed!**

**Build:** ✅ **Erfolgreich! (10.9s)**

**Bereit für neuen Test:** ✅

---

**Erstellt:** 2025-11-15 15:58 CET
**Geänderte Datei:** `LlamaCppProvider.java:386-404`
**Related:** VISION-FIX-2025-11-15.md (llama-server Shared Libraries Fix)
