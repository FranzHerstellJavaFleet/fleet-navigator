# Dokumenten-Generierung (Brief/PDF auf Zuruf)

## Status: IN ENTWICKLUNG - NICHT PRODUKTIONSREIF

**Datum:** 2025-11-28

---

## Konzept

Der Benutzer soll im Chat mit einem Experten (z.B. Roland, Rechtsanwalt) sagen können:
- "Kannst du mir einen Brief schreiben und als Download geben?"
- "Erstelle eine Zusammenfassung als PDF"

Das System soll dann:
1. Den Experten den Inhalt schreiben lassen
2. Automatisch ein Dokument (ODT/DOCX/PDF) generieren
3. Einen Download-Link in der Chat-Bubble anzeigen

---

## Implementierte Komponenten

### 1. DocumentGeneratorService
**Datei:** `src/main/java/io/javafleet/fleetnavigator/service/DocumentGeneratorService.java`

**Funktionen:**
- `detectDocumentRequest(message)` - Erkennt ob eine Nachricht eine Dokumentanfrage enthält
- `generateLetter(expert, content, recipient, subject, format)` - Generiert Brief
- `generateOdtLetter(...)` - ODT für LibreOffice
- `generateDocxLetter(...)` - DOCX für Microsoft Word
- `generatePdfSummary(...)` - PDF Zusammenfassung

**Erkennungsmuster:**
```java
// Brief-Anfragen
"brief", "schreiben", "anschreiben", "schriftsatz"
"formulier", "verfass", "schreib mir", "erstell mir"

// Format-spezifisch
"docx", "word", "microsoft" → DOCX
"odt", "libreoffice", "openoffice" → ODT
(ohne Angabe) → ODT als Default

// PDF
"pdf", "zusammenfassung" + "download"
```

### 2. Download-Endpoint
**Datei:** `src/main/java/io/javafleet/fleetnavigator/controller/DownloadController.java`

**Endpoint:** `GET /api/downloads/doc/{docId}`

Liefert das generierte Dokument zum Download.

### 3. ChatService Integration
**Datei:** `src/main/java/io/javafleet/fleetnavigator/service/ChatService.java`

**Methoden:**
- `enhanceMessageForDocumentRequest(message)` - Fügt Anweisungen für den LLM hinzu
- `checkAndGenerateDocument(message, response, expertId)` - Generiert Dokument nach LLM-Antwort

**LLM-Anweisung (wird an Nachricht angehängt):**
```
[SYSTEM-ANWEISUNG - STRIKT BEFOLGEN:
Das Dokument wird AUTOMATISCH vom System erstellt.
Du schreibst NUR den reinen Brieftext.

VERBOTEN:
- Keine URLs oder Links (NIEMALS example.com oder ähnliches!)
- Keine Download-Hinweise
- Keine Anleitungen zur Dateierstellung
- Kein Markdown, XML, HTML
- Keine Erklärungen was du tust

ERLAUBT:
- Nur der reine Brieftext
- Beginne DIREKT mit 'Sehr geehrte...' oder 'Guten Tag...'
- Ende mit Grußformel

Der Download-Link wird automatisch vom System hinzugefügt!]
```

### 4. Frontend Integration
**Datei:** `frontend/src/stores/chatStore.js`

Das `done`-Event vom Streaming enthält optional `downloadUrl`:
```javascript
const finalMessage = {
  // ...
  downloadUrl: parsed.downloadUrl || null
}
```

---

## Bekannte Probleme

### Problem 1: LLM ignoriert Anweisungen
**Symptom:** Der Experte schreibt trotzdem:
- Anleitungen zur Dokumenterstellung
- Fake-URLs wie `https://example.com/download.odt`
- Markdown/XML statt reinem Text

**Ursache:** Die Anweisung wird nur an die Benutzer-Nachricht angehängt, nicht an den System-Prompt. Manche LLMs gewichten das unterschiedlich.

**Mögliche Lösung:**
- Anweisung in den System-Prompt des Experten integrieren
- Oder: Post-Processing der LLM-Antwort (URLs/Markdown entfernen)

### Problem 2: Download-Link erscheint nicht
**Symptom:** Dokument wird generiert (Log zeigt es), aber kein Link in der Bubble.

**Mögliche Ursachen:**
- Frontend verarbeitet `downloadUrl` nicht korrekt
- MessageBubble zeigt keinen Download-Button
- Cache-Problem

**Debug:**
```bash
# Backend-Log prüfen
sudo journalctl -u fleet-navigator --since "5 min ago" | grep -i "document\|download\|brief"
```

### Problem 3: Dokument-Erkennung zu strikt/locker
**Symptom:** Erkennung triggert nicht oder bei falschen Nachrichten.

**Debug:** Prüfen welche Schlüsselwörter in der Nachricht sind.

---

## Debug-Schritte

### 1. Prüfen ob Dokument-Anfrage erkannt wird
Im Log suchen nach:
```
Document request detected: ODT (brief)
```

### 2. Prüfen ob Dokument generiert wird
```
Generiere ODT-Brief für Experte: Roland Navarro
ODT-Brief erstellt: Brief_Roland_Navarro_2025-11-28.odt (1234 bytes)
```

### 3. Prüfen ob Download-URL gesendet wird
```
Streaming completed for chat 123 (downloadUrl: /api/downloads/doc/abc-123)
```

### 4. Frontend Console prüfen
```javascript
// Sollte erscheinen wenn downloadUrl vorhanden:
console.log('📄 Document generated:', parsed.downloadUrl)
```

---

## TODO für Produktionsreife

1. [ ] **LLM-Anweisung verbessern**
   - In Expert.basePrompt integrieren statt nur an Nachricht anhängen
   - Oder: Separates "Document Mode" Flag

2. [ ] **Post-Processing der LLM-Antwort**
   - URLs entfernen (regex)
   - Markdown-Formatierung entfernen
   - Nur Text zwischen Anrede und Grußformel extrahieren

3. [x] **Download-Button in MessageBubble**
   - Wenn `message.downloadUrl` vorhanden → Button anzeigen
   - ✅ Implementiert in MessageBubble.vue (Zeile 122)
   - ✅ DownloadButton.vue unterstützt ODT/DOCX/PDF content-types

4. [ ] **Bessere Format-Auswahl**
   - Dropdown im UI für Format (ODT/DOCX/PDF)
   - Statt Erkennung aus Text

5. [ ] **Fehlerbehandlung**
   - Was wenn Dokument-Generierung fehlschlägt?
   - Fallback / Fehlermeldung an User

6. [ ] **Temp-Dateien aufräumen**
   - Generierte Dokumente werden in `/tmp/fleet-navigator-docs/` gespeichert
   - Kein Cleanup implementiert

---

## Architektur-Diagramm

```
User: "Schreib mir einen Brief als Word"
         │
         ▼
┌─────────────────────────────────────────┐
│ ChatService.enhanceMessageForDocumentRequest │
│ → Fügt [SYSTEM-ANWEISUNG...] an          │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│ LLM (Ollama/llama.cpp)                  │
│ → Schreibt Briefinhalt                  │
│ → SOLLTE nur Text schreiben             │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│ ChatService.checkAndGenerateDocument    │
│ → Erkennt Dokument-Anfrage              │
│ → Ruft DocumentGeneratorService auf     │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│ DocumentGeneratorService.generateLetter │
│ → Generiert ODT/DOCX mit Apache POI/ODF │
│ → Speichert in /tmp/                    │
│ → Gibt fileId zurück                    │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│ SSE done-Event                          │
│ → {tokens: 123, downloadUrl: "/api/..."}│
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│ Frontend chatStore                      │
│ → Setzt message.downloadUrl             │
│ → MessageBubble zeigt Link (TODO!)      │
└─────────────────────────────────────────┘
```

---

## Dateien

| Datei | Beschreibung |
|-------|--------------|
| `DocumentGeneratorService.java` | Dokument-Generierung (ODT/DOCX/PDF) |
| `DownloadController.java` | Download-Endpoint |
| `ChatService.java` | Integration in Chat-Flow |
| `chatStore.js` | Frontend State (downloadUrl in done-Event) |
| `MessageBubble.vue` | UI mit Download-Button-Integration |
| `DownloadButton.vue` | Download-Button Komponente (ODT/DOCX/PDF support) |

---

## Fazit

Die Grundstruktur ist implementiert. Stand der Probleme:

1. **LLM-Compliance**: ⚠️ Der LLM folgt den Anweisungen nicht zuverlässig (schreibt URLs/Anleitungen)
2. **UI**: ✅ Download-Button in MessageBubble implementiert
3. **Robustheit**: ⚠️ Keine Fehlerbehandlung, kein Cleanup

**Empfehlung:** Feature testen. Hauptproblem ist die LLM-Compliance - der LLM ignoriert manchmal die Anweisungen und schreibt Fake-URLs oder Anleitungen statt nur den Brieftext.
