# Fleet Navigator - Entwicklungs-Changelog

## 26. November 2025

### Zusammenfassung

Session fokussiert auf ThemeSelector-Integration und Deployment-Pipeline-Probleme.
**Gesamtzeit:** ~3-4 Stunden (davon ~2-3h Debugging von Deployment-Problemen)

---

## Erledigte Aufgaben

### 1. ThemeSelector Dropdown (NEU)

**Datei:** `frontend/src/components/topbar/ThemeSelector.vue`

- Neue Komponente für Theme-Auswahl erstellt
- Integriert in TopBar.vue (Zeile 184)
- Dropdown mit zwei Sektionen:
  - **UI Design:** Tech (default/lila) | Anwalt (Navy/Gold)
  - **Helligkeit:** Dunkel | Hell
- Speichert in `settingsStore.settings.uiTheme`
- Default-Wert: `'default'` (Tech-Theme)

### 2. Update-Script Repariert

**Datei:** `update-fleet-navigator.sh`

**Problem:** Script nutzte nur `systemctl stop`, aber "wilde" Prozesse (manuell gestartet) wurden nicht beendet.

**Lösung:** Erweitert um:
```bash
systemctl stop "$SERVICE_NAME" 2>/dev/null || true
pkill -f "fleet-navigator.jar" 2>/dev/null || true
# Plus Port-Check mit lsof und force-kill falls nötig
```

### 3. Clean-Build Script (NEU)

**Datei:** `build-clean.sh`

Garantiert saubere Builds durch Löschen ALLER Artefakte:
- `frontend/dist/`
- `frontend/node_modules/.vite/`
- `target/`

Dann `mvn clean package -DskipTests`

### 4. TopBar Refactoring

- ThemeSelector als separate Komponente extrahiert
- Import hinzugefügt: `import ThemeSelector from './topbar/ThemeSelector.vue'`
- Dead Code entfernt (alte Theme-Funktionen)

---

## Probleme & Lösungen

### Problem 1: Deployment funktioniert nicht

**Symptom:** Nach Build + Update-Script keine Änderungen sichtbar

**Ursache:**
1. "Wilder" Java-Prozess lief (nicht von systemd verwaltet)
2. `systemctl stop` beendete nur systemd-Prozess
3. Port 2025 war noch belegt → neuer Start schlug fehl

**Lösung:** Update-Script erweitert (siehe oben)

### Problem 2: Firefox Inkognito cached trotzdem

**Symptom:** Server liefert neue Version (curl bestätigt), Browser zeigt alte

**Ursache:** Firefox Inkognito-Modus cached auf Disk-Level

**Lösung:** Chromium zum Testen verwenden, Firefox Cache komplett leeren

### Problem 3: Vite Build-Cache

**Symptom:** Alte JavaScript-Hashes bleiben erhalten

**Lösung:** `build-clean.sh` nutzen, das auch `.vite/` Cache löscht

---

## TODOs (Nächste Sessions)

### Hohe Priorität

#### 1. Web-Suche Empfindlichkeit
- [ ] Trigger-Kriterien für automatische Websuche anpassen
- [ ] Aktuell zu empfindlich / zu unempfindlich?
- [ ] Konfigurierbar machen in Settings

#### 2. Theme-Styling implementieren
- [ ] Tech-Theme: Lila/Indigo Akzente (aktuell)
- [ ] Anwalt-Theme: Navy Blue + Gold Akzente
- [ ] CSS-Variablen oder Tailwind-Config anpassen
- [ ] Komponenten müssen auf `uiTheme` reagieren

#### 3. API Redundanzen entfernen
- [ ] `getSystemPrompts()` vs `getAllSystemPrompts()` - vereinheitlichen
- [ ] Doppelte API-Aufrufe in chatStore eliminieren

### Mittlere Priorität

#### 4. Error-Handling verbessern
- [ ] chatStore: Fehlende try/catch bei API-Calls
- [ ] MessageBubble: XSS-Risiko bei Markdown-Rendering prüfen
- [ ] Graceful Degradation bei Ollama-Verbindungsproblemen

#### 5. Settings Inkonsistenz
- [ ] localStorage vs Backend-Sync klären
- [ ] Einheitliche Quelle der Wahrheit definieren

#### 6. TopBar weiter aufteilen
- [ ] ProviderSelector extrahieren
- [ ] SystemPromptPanel extrahieren
- [ ] Ziel: TopBar.vue < 500 Zeilen

### Niedrige Priorität

#### 7. Performance
- [ ] Home.vue: 1MB+ Chunk - Code-Splitting
- [ ] Lazy Loading für große Komponenten

#### 8. Code-Qualität
- [ ] TypeScript für kritische Stores
- [ ] Unit Tests für chatStore

---

## Technische Notizen

### Deployment-Workflow (korrigiert)

```bash
# 1. Clean Build
./build-clean.sh

# 2. Deploy (als root)
sudo ./update-fleet-navigator.sh

# 3. Testen (WICHTIG: Chromium verwenden!)
chromium http://localhost:2025
```

### Wichtige Dateien

| Datei | Beschreibung |
|-------|--------------|
| `build-clean.sh` | Sauberer Build ohne Cache |
| `update-fleet-navigator.sh` | Deploy + Service-Neustart |
| `frontend/src/components/topbar/ThemeSelector.vue` | Theme-Auswahl Dropdown |
| `frontend/src/stores/settingsStore.js` | `uiTheme` Setting |

### Server-Prozess prüfen

```bash
# Welche Version läuft?
curl -s http://localhost:2025/ | grep -o 'index-[^"]*\.js'

# Prozess-Info
ps aux | grep fleet-navigator | grep -v grep

# Port-Belegung
lsof -i:2025
```

---

## Erkenntnisse

1. **Firefox Inkognito ist unzuverlässig** für Frontend-Tests
2. **Wilde Prozesse** können systemd-Services blockieren
3. **Vite-Cache** muss explizit gelöscht werden
4. **Immer die komplette Pipeline testen** - nicht nur Build prüfen

---

*Dokumentiert am 26.11.2025*
*Fleet Navigator v0.3.1*
