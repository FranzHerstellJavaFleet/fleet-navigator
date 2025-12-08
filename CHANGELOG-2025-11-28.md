# Changelog 28.11.2025 - Fleet Navigator v0.5.0

## Session-Zusammenfassung: Dokument-Generierung & UI-Verbesserungen

### 1. Dokument-Inhalt nicht mehr in Chat-Bubble anzeigen

**Problem:** Wenn ein Experte einen Brief generierte, wurde der volle Brieftext in der Chat-Bubble angezeigt.

**Lösung:**
- `MessageBubble.vue` erkennt jetzt `downloadUrl` mit Prefix `fleet-mate://`
- Zeigt stattdessen eine grüne Erfolgskarte:
  - "📄 Dokument erstellt"
  - "Das Dokument wurde lokal gespeichert und in LibreOffice geöffnet."
  - "Pfad: ~/Dokumente/Fleet-Navigator/"

**Geänderte Dateien:**
- `frontend/src/components/MessageBubble.vue` (Zeilen 209-243)

---

### 2. LLM schreibt nicht mehr "Das Dokument wurde gespeichert..."

**Problem:** Das LLM generierte am Ende des Briefs den Text "Das Dokument wurde gespeichert unter ~/Dokumente/..." weil es im System-Prompt so angewiesen wurde.

**Lösung:** System-Prompt für Dokument-Anfragen bereinigt:
- Entfernt: Anweisung dem Nutzer den Speicherpfad mitzuteilen
- Hinzugefügt: Explizites Verbot für Speicherort-Erwähnungen
- Anweisung: "Ende mit Grußformel und Unterschrift - DANN SOFORT AUFHÖREN!"

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/ChatService.java`
  - Methode: `enhanceMessageForDocumentRequest()` (Zeilen 1676-1714)

---

### 3. Keine Fake-URLs vom Experten

**Problem:** Experten generierten manchmal erfundene URLs (Halluzinationen) auch wenn keine Web-Suche aktiv war.

**Lösung:** Anti-Halluzinations-Anweisung im System-Prompt wenn KEINE Web-Suche aktiv:
```
⚠️ WICHTIG: Erfinde NIEMALS URLs oder Weblinks!
Wenn du keine verifizierte Quelle hast, erwähne keine Websites.
Nutze nur URLs, die dir explizit durch Web-Suche bereitgestellt wurden.
```

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/ChatService.java`
  - Zeilen 358-368 (nicht-streaming)
  - Zeilen 675-685 (streaming)

---

### 4. Web-Suche nur bei expliziten Keywords oder Button

**Problem:** Web-Suche wurde automatisch bei zeit-bezogenen Fragen ausgelöst (z.B. "aktuell", "2024").

**Lösung:** `shouldAutoSearch()` vereinfacht:
- Triggert NUR noch bei expliziten Keywords: "recherchiere", "google", "suche im internet", etc.
- Zeit-basierte Keywords triggern NICHT mehr automatisch
- Für alle anderen Fälle muss der Nutzer den Web-Suche-Button klicken

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/service/WebSearchService.java`
  - Methode: `shouldAutoSearch()` (Zeilen 1056-1081)

---

### 5. downloadUrl in Datenbank persistieren

**Problem:** Nach Browser-Refresh ging die `downloadUrl` verloren weil sie nicht in der DB gespeichert wurde.

**Lösung:**
- Neues Feld `download_url` in Message-Entity
- Neues Feld `downloadUrl` in MessageDTO
- Mapping in `mapToMessageDTO()` erweitert
- downloadUrl wird VOR dem Speichern der Nachricht ermittelt

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/model/Message.java` (Zeilen 52-59)
- `src/main/java/io/javafleet/fleetnavigator/dto/MessageDTO.java` (Zeilen 26-32)
- `src/main/java/io/javafleet/fleetnavigator/service/ChatService.java`
  - `mapToMessageDTO()` (Zeile 1355)
  - Streaming-Endpoint: downloadUrl wird vor Nachricht-Speicherung ermittelt (Zeilen 976-992)

---

### 6. document_generated Handler implementiert

**Problem:** Fleet-Mate sendet nach Dokumenterstellung eine `document_generated` Nachricht, die verarbeitet werden muss.

**Lösung:**
- `handleDocumentGeneratedResponse()` in FleetMateWebSocketHandler
- `handleDocumentGenerated()` in ChatService
- Korrekte Feldnamen aus Go-Struct: `path`, `sessionId`, `success`, `error`

**Geänderte Dateien:**
- `src/main/java/io/javafleet/fleetnavigator/websocket/FleetMateWebSocketHandler.java` (Zeilen 1164-1192)
- `src/main/java/io/javafleet/fleetnavigator/service/ChatService.java` (Zeilen 1895-1915)

---

## Bekannte Einschränkungen / TODOs

1. **Kein klickbarer file:// Link mit Dateinamen**
   - Aktuell zeigt die Karte nur einen generischen Pfad
   - Echten Dateipfad von Fleet-Mate Response nutzen (für später)

2. **downloadUrl wird erst nach Cache-Sync geladen**
   - Frontend synchronisiert nach 500ms mit DB
   - Sofortige Anzeige würde WebSocket-Broadcast benötigen

---

## Dateien-Übersicht

### Backend (Java)
| Datei | Änderung |
|-------|----------|
| `ChatService.java` | System-Prompts, downloadUrl-Handling, document_generated |
| `WebSearchService.java` | shouldAutoSearch() vereinfacht |
| `Message.java` | Neues Feld `downloadUrl` |
| `MessageDTO.java` | Neues Feld `downloadUrl` |
| `FleetMateWebSocketHandler.java` | document_generated Handler |

### Frontend (Vue.js)
| Datei | Änderung |
|-------|----------|
| `MessageBubble.vue` | Grüne Dokument-Karte statt Brieftext |

---

## Test-Anleitung

1. Fleet Navigator starten: `sudo ./update-fleet-navigator.sh`
2. Fleet-Mate Linux starten (muss laufen für Dokument-Generierung)
3. Neues Inkognito-Fenster öffnen → http://localhost:2025
4. Experte "Roland" auswählen (mit documentDirectory konfiguriert)
5. Brief anfordern: "Schreib mir einen Brief an die Familienkasse"
6. Erwartetes Ergebnis:
   - LLM streamt NUR den Brieftext
   - LibreOffice öffnet das Dokument
   - Chat-Bubble zeigt grüne Karte "Dokument erstellt"
   - KEINE URLs oder Speicherpfad-Erwähnung vom LLM

---

*Erstellt: 28.11.2025, ~23:20 Uhr*
