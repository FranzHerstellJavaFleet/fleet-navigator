# Fleet Mate Migration Notes

**Datum:** 2025-11-12
**Version:** 0.2.7
**Status:** Umbenennung abgeschlossen, aber noch nicht vollständig getestet

## Übersicht

Die komplette Umbenennung von "Officer" → "Mate" wurde durchgeführt. Dies war Teil der Vereinfachungsstrategie, bevor wir in Phase 2 das gesamte System auf nur llama.cpp umbauen.

## Was wurde geändert

### Backend (Java)

#### Umbenannte Klassen (git mv):
1. `FleetOfficerService.java` → `FleetMateService.java`
2. `OfficerDiscoveryService.java` → `MateDiscoveryService.java`
3. `FleetOfficerController.java` → `FleetMateController.java`
4. `FleetOfficerWebSocketHandler.java` → `FleetMateWebSocketHandler.java`
5. `OfficerCommand.java` → `MateCommand.java`
6. `OfficerMessage.java` → `MateMessage.java`

#### Aktualisierte Dateien (Inhalte geändert):
- `CommandExecutionService.java` - `officerId` → `mateId`
- `LogAnalysisService.java` - Parameter und Variablen
- `EmailClassificationService.java` - Kommentare
- `CommandHistoryEntry.java` - Felder
- `CommandExecutionRequest.java` - Felder
- `CommandExecutionResponse.java` - Felder
- `LogAnalysisRequest.java` - Felder
- `HardwareStats.java` - JSON Properties
- `StartupDiscoveryListener.java` - Imports und Kommentare
- `WebSocketConfig.java` - WebSocket-Pfade
- `application.properties` - Logging-Konfiguration

#### API-Änderungen:
- **Alt:** `/api/fleet-officer/*`
- **Neu:** `/api/fleet-mate/*`

#### WebSocket-Änderungen:
- **Alt:** `/api/fleet-officer/ws/{officerId}`
- **Neu:** `/api/fleet-mate/ws/{mateId}`

### Frontend (Vue.js)

#### Umbenannte Komponenten (git mv):
1. `OfficerDetailModal.vue` → `MateDetailModal.vue`
2. `OfficerTerminal.vue` → `MateTerminal.vue`
3. `FleetOfficerMonitor.vue` → `FleetMateMonitor.vue`
4. `FleetOfficersDashboard.vue` → `FleetMatesDashboard.vue`
5. `FleetOfficersView.vue` → `FleetMatesView.vue`
6. `OfficerDetailView.vue` → `MateDetailView.vue`

#### Aktualisierte Dateien:
- `router/index.js` - Routes von `/agents/fleet-officers` → `/agents/fleet-mates`
- `TopBar.vue` - Navigation und API-Calls
- `SystemMonitor.vue` - Alle Officer-Referenzen
- `EmailAgentSettings.vue` - UI-Texte

#### UI-Text-Änderungen:
- Deutsch: "Offizier" → "Maat"
- Deutsch: "Offiziere" → "Maate"
- Englisch: "Officer" → "Mate"
- Englisch: "Officers" → "Mates"

### Build & Tests

✅ **Build erfolgreich:** `mvn clean package -DskipTests`
✅ **Anwendung startet:** Port 2025
✅ **Neuer API-Endpoint funktioniert:** `GET /api/fleet-mate/mates` → `[]`
✅ **Alter Endpoint ist weg:** `GET /api/fleet-officer/officers` → 404
✅ **Frontend lädt:** http://localhost:2025

---

## ⚠️ BEKANNTE PROBLEME UND OFFENE FRAGEN

### 1. **Keine Fleet Mates zum Testen vorhanden**

**Problem:**
Die Umbenennung ist Code-seitig vollständig, aber wir haben keine echten Fleet Mates, um die Funktionalität zu testen.

**Betroffen:**
- WebSocket-Verbindungen zu Mates
- Mate-Registrierung
- Mate-Monitoring (Hardware-Stats)
- Terminal-Zugriff auf Mates
- Command-Execution
- Log-Analyse

**Was wurde NICHT getestet:**
- Ob ein echter Fleet Mate sich noch verbinden kann
- Ob die WebSocket-Kommunikation funktioniert
- Ob die Hardware-Stats korrekt übertragen werden
- Ob Terminal-Befehle noch funktionieren
- Ob die Discovery-Broadcasts ankommen

**Notwendige Tests:**
```bash
# Sobald ein Fleet Mate verfügbar ist:
1. Fleet Mate starten und prüfen, ob er sich verbindet
2. WebSocket-Connection prüfen
3. Hardware-Stats im SystemMonitor prüfen
4. Terminal öffnen und Befehl ausführen
5. Log-Analyse testen
```

### 2. **Externe Fleet-Mate-Projekte nicht aktualisiert**

**Problem:**
Die externen Fleet-Mate-Projekte (früher "Fleet-Officer") wurden NICHT umbenannt.

**Betroffene Verzeichnisse:**
```
Fleet-Office-Officers/
Fleet-Office-Officer-LibreOffice/
```

**Diese Projekte erwarten noch:**
- API-Endpoint: `/api/fleet-officer/*`
- WebSocket: `/api/fleet-officer/ws/{officerId}`
- JSON-Properties: `officer_id`

**Lösung:**
Entweder:
1. **Kompatibilitäts-Endpunkte** im Navigator hinzufügen (Weiterleitung old → new)
2. **Externe Projekte aktualisieren** (alle Officer → Mate Referenzen ändern)
3. **Dokumentation erstellen** für externe Entwickler

### 3. **Datenbankmigrationen fehlen**

**Problem:**
Es gibt keine Liquibase/Flyway-Migration für existierende Datenbanken.

**Potentiell betroffene Tabellen/Spalten:**
Wir verwenden H2 File-Based Database. Wenn in der DB bereits Officer-Daten gespeichert sind:
- Spalten mit `officer_id` Namen
- JSON-Felder mit Officer-Referenzen
- Alte WebSocket-Session-Daten

**Aktueller Status:**
- Bei neuem Start wird neue DB erstellt → kein Problem
- Bei bestehender DB können Kompatibilitätsprobleme auftreten

**Lösung:**
```sql
-- Beispiel-Migration (falls nötig):
ALTER TABLE command_history RENAME COLUMN officer_id TO mate_id;
ALTER TABLE hardware_stats RENAME COLUMN officer_id TO mate_id;
-- etc.
```

### 4. **Dokumentation veraltet**

**Problem:**
Alle Markdown-Dokumente im Projekt-Root enthalten noch "Officer"-Referenzen.

**Betroffene Dateien:**
```
EMAIL-OFFICER-CONCEPT.md
FLEET-OFFICER-INTEGRATION.md
COMMAND-EXECUTION-IMPLEMENTATION.md
Fleet_Officer.md
Fleet_Email_Officer.md
IMPLEMENTATION-LOG.md
LOG-ANALYSIS-TODO.md
```

**Was zu tun ist:**
- Entweder: Dateien umbenennen und Inhalte aktualisieren
- Oder: Deprecation-Hinweis hinzufügen

### 5. **Keine Rückwärtskompatibilität**

**Problem:**
Die alten API-Endpunkte wurden vollständig entfernt. Externe Clients, die noch den alten Code verwenden, werden brechen.

**Breaking Changes:**
```
❌ /api/fleet-officer/officers
❌ /api/fleet-officer/officers/{officerId}
❌ /api/fleet-officer/ws/{officerId}
❌ JSON: { "officer_id": "..." }
```

**Lösung für Übergangszeitraum:**
```java
// Optional: Kompatibilitäts-Controller
@RestController
@RequestMapping("/api/fleet-officer")
@Deprecated
public class FleetOfficerCompatibilityController {

    @GetMapping("/officers")
    public ResponseEntity<?> redirectToMates() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
            .header("Location", "/api/fleet-mate/mates")
            .build();
    }
}
```

### 6. **Git-Historie könnte verwirrend sein**

**Problem:**
Obwohl wir `git mv` verwendet haben, könnten Entwickler, die alte Branches auschecken, verwirrt sein.

**Betroffene Szenarien:**
- Merge Conflicts bei Feature-Branches
- Alte PRs, die noch "Officer" verwenden
- Git Blame zeigt jetzt andere Dateinamen

**Lösung:**
- README-Hinweis mit Datum der Umbenennung
- Git-Tag setzen: `v0.2.7-mate-migration`

### 7. **Performance-Monitoring fehlt**

**Problem:**
Wir wissen nicht, ob die Umbenennung Performance-Einbußen verursacht hat.

**Zu überwachen:**
- WebSocket-Verbindungsaufbau-Zeit
- API-Response-Zeiten
- Frontend-Ladezeit
- Memory-Usage

---

## Nächste Schritte (Empfohlen)

### Phase 1: Validierung
1. ✅ Build erfolgreich
2. ✅ Anwendung startet
3. ⏳ **OFFEN:** Echten Fleet Mate starten und testen
4. ⏳ **OFFEN:** Alle Mate-Features durchgehen (Terminal, Stats, Commands)
5. ⏳ **OFFEN:** Frontend-UI auf "Maat"-Texte prüfen

### Phase 2: Externe Updates
1. ⏳ **OFFEN:** Fleet-Office-Officers Projekte aktualisieren
2. ⏳ **OFFEN:** Externe Dokumentation aktualisieren
3. ⏳ **OFFEN:** Migration-Guide für externe Entwickler schreiben

### Phase 3: Cleanup
1. ⏳ **OFFEN:** Alte Dokumentation umbenennen oder löschen
2. ⏳ **OFFEN:** Git-Tag setzen: `git tag v0.2.7-mate-migration`
3. ⏳ **OFFEN:** Kompatibilitäts-Endpunkte implementieren (optional)

### Phase 4: Provider-System entfernen
Nach erfolgreicher Mate-Validierung:
1. Nur noch JavaLlamaCppProvider verwenden
2. OllamaProvider, LlamaCppProvider, LLMProviderService entfernen
3. ChatService direkt auf JavaLlamaCppProvider umbauen

---

## Rollback-Plan (Falls Probleme auftreten)

Falls kritische Probleme auftreten:

```bash
# Zurück zum letzten funktionierenden Commit VOR der Umbenennung
git log --oneline | grep -B1 "Officer"
git checkout <commit-hash>

# Oder: Neuen Branch für Rollback
git checkout -b rollback-mate-migration
git revert <mate-migration-commits>
```

---

## Kontakt & Fragen

Bei Problemen mit der Mate-Migration:
- Prüfen Sie diese Dokumentation
- Suchen Sie im Code nach verbleibenden "officer" Strings:
  ```bash
  grep -ri "officer" src/ frontend/src/ --exclude-dir=node_modules
  ```
- Erstellen Sie ein Issue auf GitHub

---

**Erstellt von:** Claude Code
**Letzte Aktualisierung:** 2025-11-12
**Version:** 0.2.7
