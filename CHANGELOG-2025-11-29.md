# Changelog 29.11.2025 - Fleet Navigator v0.5.0

## Session-Zusammenfassung: Dokumenten-System Verbesserungen

### 1. Standard-Dokumentformat auf ODT geändert

**Vorher:** PDF war Standard wenn kein Format angegeben
**Nachher:** ODT (LibreOffice) ist jetzt Standard - offenes Format

**Logik:**
- Explizite PDF-Anfrage ("als PDF") → PDF
- Explizite DOCX-Anfrage ("als Word") → DOCX
- Explizite ODT-Anfrage ("als odt") → ODT
- Keine Format-Angabe ("erstelle eine Datei") → ODT (Standard)

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/DocumentGeneratorService.java`
  - `detectDocumentRequest()` - Standard von PDF auf ODT geändert (Zeilen 138-146)

---

### 2. Standard-Dokumentpfad für Experten

**Neu:** Beim Erstellen eines Experten wird automatisch ein Dokumentverzeichnis gesetzt.

**Beispiel:**
- Experte "Roland" → documentDirectory = "Roland"
- Dokumente landen in: `~/Dokumente/Fleet-Navigator/Roland/`

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/experts/service/ExpertSystemService.java`
  - `mapToNewExpert()` - Setzt documentDirectory auf Expertenname wenn nicht angegeben (Zeilen 322-327)

---

### 3. Intelligente Dateinamen mit Thema und Zeitstempel

**Vorher:** `Brief_Roland_2025-11-29.odt`
**Nachher:** `Mietvertrag_2025-11-29_13-27.odt`

**Format:** `{Betreff/Thema}_{Datum}_{Uhrzeit}.{ext}`

**Features:**
- Betreff wird aus `subject` Parameter übernommen
- Datum und Uhrzeit für eindeutige Dateinamen
- Ungültige Zeichen werden bereinigt
- Maximal 50 Zeichen für Thema
- Neue Hilfsmethode `sanitizeFilename()`

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/DocumentGeneratorService.java`
  - Neuer DateTimeFormatter: `FILENAME_DATE_FORMAT` (Zeile 32)
  - ODT-Dateiname: (Zeilen 233-237)
  - DOCX-Dateiname: (Zeilen 374-378)
  - PDF-Dateiname: (Zeilen 433-437)
  - Neue Methode: `sanitizeFilename()` (Zeilen 596-613)

---

### 4. Dokumenttyp in Meldungen anzeigen

**Während Erstellung (animierte Karte):**
- 📕 Erstelle PDF...
- 📄 Erstelle LibreOffice (ODT)...
- 📘 Erstelle Word (DOCX)...

**Nach Fertigstellung (grüne Karte):**
- 📕 PDF Dokument erstellt
- 📄 ODT Dokument erstellt
- 📘 Word Dokument erstellt

**Zusätzlich angezeigt:**
```
Datei: Mietvertrag_2025-11-29_13-27.odt
Speicherort: /home/trainer/Dokumente/Fleet-Navigator/Roland
```

**Geänderte Dateien:**
- `frontend/src/components/MessageBubble.vue`
  - Dokumenttyp-Erkennung aus Dateiendung (Zeilen 238-251)
  - Pfad-Info mit Dateiname und Speicherort (Zeilen 253-259)
  - Meldungstext angepasst (Zeilen 268-269)

---

### 5. Generierte Dokumente NICHT im Kontext speichern

**Problem:** Generierte Briefe/Zusammenfassungen wurden vollständig in der DB gespeichert, obwohl die Information bereits im Chat-Verlauf existiert.

**Lösung:** Unterscheidung zwischen:
| Typ | Im Kontext? | Grund |
|-----|-------------|-------|
| Generierte Dokumente (Brief, PDF) | Nein | Abgeleitet aus vorhandener Konversation |
| Hochgeladene Dateien | Ja | Neue externe Information |

**Implementierung:**
- Bei generiertem Dokument: Speichere nur `[Dokument generiert]` statt vollständigen Inhalt
- Token-Ersparnis: Tausende Tokens → ~3 Tokens
- Frontend zeigt weiterhin Info-Karte basierend auf `downloadUrl`

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/ChatService.java`
  - Zeilen 1031-1037: Bedingtes Speichern des Inhalts

---

### 6. ODT/DOCX-Erkennung ohne "Brief"-Keyword

**Vorher:** ODT/DOCX-Erstellung erforderte "Brief"-Keyword
**Nachher:** Explizite Format-Anfragen funktionieren auch ohne "Brief"

**Neue Patterns:**
- "als Word Datei erstellen" → DOCX
- "erstelle docx" → DOCX
- "als odt" → ODT
- "erstelle odt datei" → ODT

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/DocumentGeneratorService.java`
  - Zeilen 118-126: Neue explizite DOCX/ODT-Erkennung

---

### 7. Typo-Toleranz bei Dokumenterkennung

**Erkannte Tippfehler:**
- "zusammenfasung" (statt "zusammenfassung")
- "zusammenfasen" (statt "zusammenfassen")

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/DocumentGeneratorService.java`
  - Zeilen 89-91: Typo-Varianten hinzugefügt

---

### 8. Message-Update nach Dokument-Generierung

**Problem:** `downloadUrl` zeigte Session-ID statt echten Dateipfad

**Lösung:**
- Neue Repository-Methode: `findByDownloadUrl()`
- `handleDocumentGenerated()` aktualisiert Message mit echtem Pfad
- Frontend: Zweiter Sync nach 3 Sekunden für Fleet-Mate Dokumente

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/repository/MessageRepository.java`
  - Neue Methode: `findByDownloadUrl()` (Zeilen 34-36)
- `src/main/java/io/javafleet/fleetnavigator/service/ChatService.java`
  - `handleDocumentGenerated()` - Message-Update (Zeilen 1954-1960)
- `frontend/src/stores/chatStore.js`
  - Zweiter Sync nach 3s für Fleet-Mate Dokumente

---

## Dateien-Übersicht

### Backend (Java)
| Datei | Änderung |
|-------|----------|
| `DocumentGeneratorService.java` | Standard ODT, Dateinamen, Typo-Toleranz, DOCX/ODT-Erkennung |
| `ExpertSystemService.java` | Standard documentDirectory |
| `ChatService.java` | Kontext-Optimierung, Message-Update |
| `MessageRepository.java` | findByDownloadUrl() |

### Frontend (Vue.js)
| Datei | Änderung |
|-------|----------|
| `MessageBubble.vue` | Dokumenttyp-Anzeige, Datei+Pfad-Info |
| `chatStore.js` | Zweiter Sync für Fleet-Mate Dokumente |

---

## Test-Anleitung

1. Update ausführen: `sudo ./update-fleet-navigator.sh`
2. Browser: Neues Inkognito-Fenster oder Ctrl+Shift+R
3. Neuen Chat starten mit Experte "Roland"

**Tests:**
- "Fasse das als Datei zusammen" → ODT (Standard)
- "Erstelle eine PDF" → PDF
- "Als Word Datei" → DOCX
- Dateiname sollte Thema + Zeitstempel enthalten
- Meldung zeigt "📄 ODT Dokument erstellt" mit Datei + Speicherort

---

### 9. GPU-Beschleunigung für llama.cpp Provider

**Problem:** Der Standard-LLM-Provider `java-llama-cpp` (JNI-basiert) hat **keine CUDA-Unterstützung** - läuft nur auf CPU!

**Analyse:**
- `java-llama.cpp v4.1.0` im Maven-Repository enthält nur CPU-kompilierte native Libraries
- Die `libjllama.so` (5.6 MB) hat keine CUDA-Bibliotheken gelinkt
- Die RTX 3060 (12GB VRAM) wurde nicht genutzt

**Lösung:** Provider-Wechsel zu `llamacpp` (subprocess-basiert)
- Der `llama-server` im `bin/` Ordner hat **volle CUDA-Unterstützung**
- `libggml-cuda.so` (163 MB) mit CUDA 12.8 und cuBLAS

**Konfigurationsänderung:**
```properties
# Vorher (CPU-only)
llm.default-provider=java-llama-cpp

# Nachher (GPU mit CUDA)
llm.default-provider=llamacpp
```

**Performance-Verbesserung:**
| Provider | GPU-Support | Geschwindigkeit |
|----------|-------------|-----------------|
| java-llama-cpp | ❌ Nein (CPU) | ~5-10 tokens/s |
| llamacpp | ✅ Ja (CUDA) | ~50-100+ tokens/s |

**Geänderte Dateien:**
- `src/main/resources/application.properties`
  - Zeile 27: `llm.default-provider=llamacpp`
  - Kommentare aktualisiert für GPU-Hinweise

**llama-server Konfiguration:**
- Port: 2024
- GPU-Layers: 999 (alle auf GPU)
- Context-Size: 8192
- Threads: 8
- Optimierungen: batch-size 512, ubatch 256, flash-attn, 4 parallel slots

---

## Dateien-Übersicht

### Backend (Java)
| Datei | Änderung |
|-------|----------|
| `DocumentGeneratorService.java` | Standard ODT, Dateinamen, Typo-Toleranz, DOCX/ODT-Erkennung |
| `ExpertSystemService.java` | Standard documentDirectory |
| `ChatService.java` | Kontext-Optimierung, Message-Update |
| `MessageRepository.java` | findByDownloadUrl() |
| `application.properties` | **NEU:** GPU-Provider aktiviert |

### Frontend (Vue.js)
| Datei | Änderung |
|-------|----------|
| `MessageBubble.vue` | Dokumenttyp-Anzeige, Datei+Pfad-Info |
| `chatStore.js` | Zweiter Sync für Fleet-Mate Dokumente |

---

## Test-Anleitung

1. Update ausführen: `sudo ./update-fleet-navigator.sh`
2. Browser: Neues Inkognito-Fenster oder Ctrl+Shift+R
3. Neuen Chat starten mit Experte "Roland"

**Tests:**
- "Fasse das als Datei zusammen" → ODT (Standard)
- "Erstelle eine PDF" → PDF
- "Als Word Datei" → DOCX
- Dateiname sollte Thema + Zeitstempel enthalten
- Meldung zeigt "📄 ODT Dokument erstellt" mit Datei + Speicherort

**GPU-Test:**
- Nach dem Start: `nvidia-smi` sollte llama-server mit GPU-Nutzung zeigen
- Inferenz-Geschwindigkeit sollte deutlich schneller sein

---

*Erstellt: 29.11.2025, ~13:35 Uhr*
*Aktualisiert: 29.11.2025, ~13:50 Uhr (GPU-Beschleunigung)*
