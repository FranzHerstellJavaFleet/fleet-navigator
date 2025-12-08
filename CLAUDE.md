# CLAUDE.md - Anweisungen für Claude Code

---

## 🎯 ROADMAP: Version 0.6.0 - "Idiotensicher"

**Ziel bis v0.6.0:** Die Anwendung muss **vermarktungsfähig** und **benutzerfreundlich** sein für den durchschnittlichen Endanwender ("Wald- und Wieseninformationskonsument").

### Prinzipien für alle Entscheidungen:

1. **Keine technischen Irritationen** - Der Nutzer soll nie verwirrt sein
   - Keine Cache-Probleme (✅ gelöst: Auto-Cache-Clearing bei Version-Updates)
   - Benutzer-Authentifizierung (✅ gelöst: Login/Register System)
   - Keine widersprüchlichen UI-Zustände
   - Klare, verständliche Fehlermeldungen auf Deutsch

2. **Sinnvolle Defaults** - Alles muss "out of the box" funktionieren
   - Keine Konfiguration nötig für Standardnutzung
   - Werbe-Tiles standardmäßig aus (✅ gelöst)
   - Vernünftige Voreinstellungen für alle Parameter

3. **Selbsterklärende UI** - Keine Dokumentation nötig
   - Tooltips wo nötig
   - Konsistente Begriffe (deutsch!)
   - Logische Anordnung der Elemente

4. **Robustheit** - Nichts darf kaputtgehen
   - Graceful Degradation bei Fehlern
   - Automatische Wiederherstellung wo möglich
   - Keine "hängenden" Zustände

**Bei jeder Implementierung fragen:** *"Würde meine Oma das verstehen?"*

---

## 🧪 Entwicklungsrichtlinien - ENTERPRISE QUALITÄT

### JUnit Tests sind PFLICHT - KEINE AUSNAHMEN!

**Fleet Navigator ist eine Enterprise-fähige Anwendung. Bei JEDER Änderung MÜSSEN JUnit-Tests geschrieben werden!**

**Aktueller Test-Stand:** 158+ Tests (Stand: 2025-11-30)

```
src/main/java/.../MyClass.java
     ↓
src/test/java/.../MyClassTest.java
```

**Test-Struktur:**
- Verwende `@Nested` für thematische Gruppierung
- Verwende `@DisplayName` für lesbare Test-Namen (deutsch!)
- Verwende AssertJ für fluent assertions
- Mocke externe Abhängigkeiten mit Mockito

**Beispiel:**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MeineKlasse Tests")
class MeineKlasseTest {

    @Nested
    @DisplayName("Initialisierung")
    class InitializationTests {
        @Test
        @DisplayName("Sollte korrekt initialisiert werden")
        void shouldInitializeCorrectly() {
            // ...
        }
    }
}
```

**Testabdeckung Mindestanforderungen:**
- Positive Fälle (Happy Path)
- Edge Cases (null, leere Listen, etc.)
- Fehlerbehandlung
- Caching-Verhalten (wo relevant)

**Vorhandene Test-Klassen (Stand 2025-11-30):**

| Test-Klasse | Bereich | Tests |
|-------------|---------|-------|
| `WorkingConfigurationTest` | Konfiguration (context-size, gpu-layers) | 8 |
| `JavaLlamaCppProviderIntegrationTest` | GGUF-Modell-Pfade | 8 |
| `DocumentGeneratorServiceTest` | ODT/DOCX/PDF Generierung | 11 |
| `ModelPathResolutionTest` | Modell-Pfad-Auflösung | 17 |
| `ChatExpertMappingTest` | Chat-Experten-Zuordnung | 13 |
| `ExpertIdTypeSafetyTest` | Typ-Sicherheit für Expert-IDs | 13 |
| ... | weitere | ... |

**WICHTIG:** Bei Regressionen (etwas funktioniert nicht mehr) → SOFORT neuen Test schreiben!

**Tests ausführen:**
```bash
# Alle Tests
mvn test

# Spezifische Tests
mvn test -Dtest="MyClassTest"
```

---

## ⚠️ WICHTIG: Lies dies ZUERST!

**Fleet Navigator ist EINE Anwendung - NICHT zwei!**

```
Fleet Navigator = Spring Boot Backend + Vue.js Frontend in EINEM JAR
```

### Das bedeutet:

1. **Production:** Ein einziges JAR-File, ein Server, Port 2025
2. **Development:** Temporär zwei Server für Hot-Reload (nur während Entwicklung!)

---

## 🏗️ Architektur

### Production Mode (Standard)

```
fleet-navigator.jar
├── Spring Boot Application (Port 2025)
│   ├── REST API (/api/*)
│   ├── Static Resources (Vue.js Frontend)
│   └── H2 Database
└── Alles in EINEM Prozess!
```

**Starten:**
```bash
mvn clean package
java -jar target/fleet-navigator-0.5.0.jar
# → http://localhost:2025
```

### Development Mode (nur für Entwicklung)

Temporär getrennt für Hot-Reload:

```
Terminal 1: Spring Boot (Port 2025) - Backend API
Terminal 2: Vite Dev Server (Port 5173) - Frontend mit Hot-Reload
```

**Warum?** Schnelle Frontend-Änderungen ohne Backend-Neustart

**Starten:**
```bash
./START.sh
# ODER manuell:
mvn spring-boot:run              # Terminal 1
cd frontend && npm run dev       # Terminal 2
```

**Wichtig:** Dies ist NUR für Entwicklung! Normale Nutzer verwenden das JAR!

---

## 🚫 Häufige Fehler vermeiden

### ❌ FALSCH: "Frontend und Backend starten"
```bash
# Das impliziert zwei separate Anwendungen - FALSCH!
```

### ✅ RICHTIG: "Fleet Navigator starten"
```bash
# Production
java -jar target/fleet-navigator-0.5.0.jar

# Development (mit Hot-Reload)
./START.sh
```

---

## 📦 Build-Prozess

```bash
mvn clean package
```

**Was passiert:**
1. Maven installiert Node.js + npm (target/)
2. `npm install` im frontend/ Ordner
3. `npm run build` → frontend/dist/
4. Kopiert dist/ nach target/classes/static/
5. Erstellt JAR mit Backend + Frontend

**Ergebnis:** Ein JAR-File mit ALLEM drin!

### ⚠️ WICHTIG: Vite Build-Cache-Problem

**Problem:** Vite cached manchmal alte Builds. Symptome:
- Frontend-Änderungen erscheinen nicht nach `mvn clean package`
- Browser lädt alte JavaScript-Dateien (alter Hash in Dateinamen)
- Console zeigt alte Fehler obwohl Code gefixt wurde

**Lösung (IMMER wenn Frontend-Änderungen nicht erscheinen):**

```bash
# 1. Lösche ALLE Build-Artefakte
rm -rf frontend/dist target

# 2. Baue FRISCH ohne Cache
mvn clean package -DskipTests

# 3. Starte lokal
./start-local.sh

# 4. Browser: NEUES Inkognito-Fenster oder Ctrl+Shift+R
```

**Erkennungsmerkmale:**
- ✅ Neue Version: JavaScript-Hash ändert sich (z.B. `index-CHZ3aMt5.js` → `index-XYZ123.js`)
- ❌ Alte Version: Hash bleibt gleich, obwohl Code geändert wurde

**Faustregel:** Bei Frontend-Änderungen IMMER `rm -rf frontend/dist target` VOR dem Build!

---

## 🔄 Automatische Browser-Cache-Invalidierung

Fleet Navigator erkennt automatisch wenn eine neue Version deployed wurde und löscht den Browser-Cache.

### Wie es funktioniert

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Browser   │────▶│  main.js        │────▶│  /api/system/   │
│   startet   │     │  (Version-Check)│     │  version        │
└─────────────┘     └─────────────────┘     └─────────────────┘
                            │                       │
                            │  localStorage:        │
                            │  fleet-nav-version    │
                            │                       │
                    ┌───────▼───────────────────────▼───────┐
                    │  Version geändert?                     │
                    │  JA → localStorage.clear()             │
                    │     → caches.delete()                  │
                    │     → window.location.reload(true)     │
                    │  NEIN → Normal weiter                  │
                    └────────────────────────────────────────┘
```

### Backend-Endpunkt

```java
// SystemController.java
@GetMapping("/version")
public ResponseEntity<VersionResponse> getVersion() {
    return ResponseEntity.ok(new VersionResponse(
        appVersion,      // z.B. "0.5.0"
        buildTime,       // z.B. "2025-12-02 16:45"
        System.currentTimeMillis()
    ));
}
```

### Frontend-Check (main.js)

```javascript
async function checkVersionAndClearCache() {
  try {
    const response = await fetch('/api/system/version')
    const versionInfo = await response.json()
    const currentVersion = `${versionInfo.version}-${versionInfo.buildTime}`

    const storedVersion = localStorage.getItem('fleet-navigator-version')

    if (storedVersion && storedVersion !== currentVersion) {
      console.log('🔄 Neue Version erkannt, lösche Cache...')

      // Clear localStorage
      localStorage.clear()

      // Clear Service Worker caches
      if ('caches' in window) {
        const cacheNames = await caches.keys()
        await Promise.all(cacheNames.map(name => caches.delete(name)))
      }

      // Store new version and reload
      localStorage.setItem('fleet-navigator-version', currentVersion)
      window.location.reload(true)
      return
    }

    localStorage.setItem('fleet-navigator-version', currentVersion)
  } catch (e) {
    console.warn('Version check failed:', e)
  }
}
```

### HTTP Cache-Control Headers

```java
// WebConfig.java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/assets/**")
        .addResourceLocations("classpath:/static/assets/")
        .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));

    registry.addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .setCacheControl(CacheControl.noCache());
}
```

### Vorteile

| Problem | Lösung |
|---------|--------|
| User sieht alte Version nach Update | ✅ Automatisches Cache-Clearing |
| Inkonsistente JS/CSS nach Deploy | ✅ Hard Reload erzwungen |
| Kein manuelles Ctrl+Shift+R nötig | ✅ Passiert automatisch |

---

## 📁 Plattformspezifische Pfade (Idiotensicher!)

**Fleet Navigator erkennt automatisch das Betriebssystem und verwendet die korrekten Pfade.**

### Automatische Pfaderkennung

| Plattform | Basis-Verzeichnis | Beispiel |
|-----------|------------------|----------|
| **Linux** | `~/.java-fleet/` | `/home/user/.java-fleet/` |
| **macOS** | `~/.java-fleet/` | `/Users/user/.java-fleet/` |
| **Windows** | `%LOCALAPPDATA%\JavaFleet\` | `C:\Users\User\AppData\Local\JavaFleet\` |

### Verzeichnisstruktur (alle Plattformen gleich)

```
{basis-verzeichnis}/
├── data/                       # ← Persistente Daten (NIEMALS LÖSCHEN!)
│   ├── fleetnavdb.mv.db        #    H2 Datenbank
│   ├── file-index/             #    Suchindex für Dokumente
│   ├── generated-documents/    #    KI-generierte Dokumente
│   └── images/                 #    Hochgeladene Bilder
│
├── models/                     # ← LLM-Modelle (GGUF-Dateien)
│   ├── library/                #    Vorinstallierte Modelle
│   └── custom/                 #    Vom Benutzer hinzugefügte Modelle
│
├── logs/                       # ← Log-Dateien
│   └── fleet-navigator.log
│
└── config/                     # ← Optionale Konfiguration
```

### Anwendungsverzeichnis (Entwicklung)

Die Anwendung liegt im Entwicklungsverzeichnis:

```
Linux:   ~/ProjekteFMH/Fleet-Navigator/target/fleet-navigator-*.jar
```

**Benutzerdaten sind getrennt in:** `~/.java-fleet/`

### Vorteile dieser Trennung:

| Vorteil | Beschreibung |
|---------|--------------|
| **Update ohne Datenverlust** | JAR austauschen → Daten bleiben |
| **Multi-User** | Jeder Benutzer hat eigene Daten |
| **Backup-freundlich** | Home-Verzeichnis wird oft gesichert |
| **Plattform-konform** | Folgt OS-Konventionen |

### Benutzer-Konfiguration (Notfall/Troubleshooting)

Fleet Navigator lädt automatisch eine Benutzer-Konfiguration aus dem Home-Verzeichnis:

```
~/.java-fleet/config/application.properties   (Linux/macOS)
%LOCALAPPDATA%\JavaFleet\config\application.properties   (Windows)
```

**Wichtig:** Normale Einstellungen werden über das **Frontend** gemacht!
Diese Datei ist nur für Notfälle/Troubleshooting.

**Beispiel-Inhalt:**
```properties
# =====================================================
# Fleet Navigator - Notfall-Konfiguration
# =====================================================
# Nur für Troubleshooting! Normale Einstellungen
# werden über das Frontend (Einstellungen) gemacht.
# =====================================================

# ----- Provider-Fallback -----
# Falls java-llama-cpp nicht funktioniert:
# llm.default-provider=ollama

# ----- Server-Port (falls 2025 belegt) -----
# server.port=2026

# ----- Debug-Modus -----
# logging.level.io.javafleet=DEBUG
```

**Priorität der Konfiguration:**

| Priorität | Quelle | Beschreibung |
|-----------|--------|--------------|
| 1 (höchste) | `~/.java-fleet/config/` | Benutzer-Überschreibungen |
| 2 | JAR-internes `application.properties` | Standard-Werte |

### Pfade überschreiben

Falls nötig, können Pfade überschrieben werden:

```properties
# In application.properties oder via Environment
fleet-navigator.paths.base-dir=/custom/path
fleet-navigator.paths.data-dir=/custom/data
fleet-navigator.paths.models-dir=/custom/models

# Oder via Environment Variable
FLEET_NAVIGATOR_DATA_DIR=/custom/data
```

### ⚠️ KEINE SYMLINKS für Modelle!

**Symlinks vermeiden** - Native Bibliotheken (llama.cpp) und Windows haben Probleme damit!

Stattdessen: Direkt auf echtes Modell-Verzeichnis zeigen:

```properties
# In ~/.java-fleet/config/application.properties
fleet-navigator.paths.models-dir=/opt/fleet-navigator/models
```

| OS | Empfohlener Modell-Pfad |
|----|-------------------------|
| **Linux** | `/opt/fleet-navigator/models` |
| **macOS** | `/Applications/FleetNavigator/models` |
| **Windows** | `C:\ProgramData\FleetNavigator\models` |

### Wichtige Regeln:

| Verzeichnis | Darf gelöscht werden? | Beschreibung |
|-------------|----------------------|--------------|
| `data/` | ❌ **NIEMALS** | Enthält alle Benutzerdaten! |
| `models/` | ⚠️ Vorsicht | Modelle müssen neu heruntergeladen werden |
| `logs/` | ✅ Ja | Nur Debug-Informationen |
| `config/` | ⚠️ Vorsicht | Eigene Einstellungen gehen verloren |

### 🚀 Autostart beim Login (.bashrc)

Fleet Navigator kann automatisch beim Login starten:

**In `~/.bashrc` einfügen:**
```bash
# Fleet Navigator Autostart
if ! pgrep -f "fleet-navigator.*jar" > /dev/null; then
    echo "🚢 Starte Fleet Navigator..."
    cd ~/ProjekteFMH/Fleet-Navigator
    nohup java -jar target/fleet-navigator-*.jar > ~/.java-fleet/logs/fleet-navigator.log 2>&1 &
    disown
    sleep 2
    echo "✅ Fleet Navigator läuft auf http://localhost:2025"
fi
```

**Vorteile:**
- Startet automatisch bei jedem Login
- Läuft im Hintergrund
- Logs in `~/.java-fleet/logs/`
- Kein sudo/root nötig

**Manuell stoppen:**
```bash
pkill -f "fleet-navigator.*jar"
```

**Status prüfen:**
```bash
pgrep -a -f "fleet-navigator.*jar"
```

---

## 🔧 Technologie-Stack

- **Backend:** Spring Boot 3.2.0, Java 17
- **Frontend:** Vue.js 3 + Vite + Pinia (State Management)
- **Database:** H2 File-Based (persistent)
- **AI:** Ollama Integration + java-llama.cpp (JNI)
- **Build:** Maven Frontend Plugin

---

## 🖥️ CPU-Only Mode (ohne GPU/CUDA)

Fleet Navigator unterstützt einen CPU-Only Modus für Demos auf Laptops ohne NVIDIA GPU.

### Aktivierung

1. **Einstellungen** → **Modellauswahl** Tab
2. Ganz unten: **Hardware & Performance** Sektion
3. Toggle **"CPU-Modus (ohne GPU)"** einschalten
4. **Neuen Chat starten** (wichtig! Modell wird neu geladen)

### Technische Details

| Provider | CPU-Only Implementierung |
|----------|--------------------------|
| **java-llama.cpp** | `gpuLayers=0` beim Modell-Laden |
| **Ollama** | `num_gpu: 0` in den Request-Options |

### Wie es funktioniert

```
Toggle AN → settingsStore.cpuOnly = true
         → Request enthält cpuOnly: true
         → Backend: gpuLayers=0 / num_gpu=0
         → Modell läuft auf CPU statt GPU
```

### Verhalten

| Einstellung | GPU-Auslastung | VRAM | Geschwindigkeit |
|-------------|----------------|------|-----------------|
| **Toggle AUS** | 50-100% | Modell geladen | Schnell |
| **Toggle AN** | ~0% (kurzer Spike beim Init) | Leer | Langsamer |

### Cache-Verhalten (java-llama.cpp)

- Modelle werden separat gecached: `modelName` vs `modelName_CPU_ONLY`
- Bei Toggle-Wechsel wird das Modell neu geladen (dauert ein paar Sekunden)
- Beide Versionen können parallel im Speicher sein

### Anwendungsfall

Ideal für:
- YouTube-Videos und Blog-Posts (Demos auf Laptops ohne NVIDIA)
- Systeme mit integrierter GPU (Intel/AMD)
- Debugging wenn CUDA-Probleme auftreten

---

## 🎓 Experten-System

Fleet Navigator verfügt über ein vollständiges Experten-System für personalisierte KI-Assistenten.

### Konzept

Experten sind spezialisierte KI-Persönlichkeiten mit:
- **Name & Rolle**: z.B. "Roland, Rechtsanwalt"
- **Basis-Prompt**: Definiert die Persönlichkeit
- **Basis-Modell**: Das zugrundeliegende Ollama-Modell
- **Blickwinkel (Modi)**: Verschiedene Perspektiven wie "Kritisch", "Kreativ", "Formal"

### Backend-Struktur

```
src/main/java/io/javafleet/fleetnavigator/experts/
├── model/
│   ├── Expert.java          # Hauptentität
│   └── ExpertMode.java       # Blickwinkel/Modi
├── repository/
│   ├── ExpertRepository.java
│   └── ExpertModeRepository.java
├── service/
│   └── ExpertService.java    # CRUD-Operationen
└── controller/
    └── ExpertController.java # REST API
```

### API-Endpunkte

```bash
# Experten
GET    /api/experts              # Alle Experten
GET    /api/experts/{id}         # Einzelner Experte
POST   /api/experts              # Erstellen
PUT    /api/experts/{id}         # Aktualisieren
DELETE /api/experts/{id}         # Löschen

# Blickwinkel (Modi)
GET    /api/experts/{id}/modes   # Modi eines Experten
POST   /api/experts/{id}/modes   # Modus erstellen
DELETE /api/experts/{id}/modes/{modeId} # Modus löschen
```

### Frontend-Komponenten

- **ExpertManager.vue**: Hauptverwaltung der Experten
- **ExpertCreationWizard.vue**: 6-Schritte-Wizard für Experten-Erstellung (NEU!)
- **CreateExpertModal.vue**: Legacy-Modal (ersetzt durch Wizard)
- **ModelManager.vue**: Experten sind auch in der Modell-Auswahl wählbar

### Experten-Wizard (NEU - v0.5.0)

Der Experten-Wizard führt Benutzer durch einen **6-Schritte-Prozess**:

| Schritt | Emoji | Beschreibung |
|---------|-------|--------------|
| 1 | 🤖 | Modell wählen (GGUF-Modelle als Cards) |
| 2 | 🔧 | Werkzeuge aktivieren (Websuche, Dateisuche) |
| 3 | ⚙️ | Parameter einstellen (Temperature, Context, etc.) |
| 4 | 👤 | Persönlichkeit definieren (Name, Rolle, Prompt) |
| 5 | 📚 | Fachbereiche definieren (Modi/Blickwinkel) |
| 6 | ✅ | Zusammenfassung & Erstellen |

**Features:**
- Ein Schritt nach dem anderen (idiotensicher!)
- Vorwärts- und Rückwärts-Navigation
- Prompt-Vorlagen (Rechtsanwalt, Steuerberater, etc.)
- Prompt aus Datei laden (.txt, .md)
- Avatar-Upload
- Validierung pro Schritt

**Dokumentation:** `docs/EXPERTEN-WIZARD.md`

### Integration in Chat

Wenn ein Experte ausgewählt wird:
1. TopBar zeigt 🎓 + Expertenname (statt Modellname)
2. System-Prompt wird aus Expert.basePrompt + aktiver Modus zusammengesetzt
3. Das Basis-Modell des Experten wird für die Anfrage verwendet

---

## 🎨 UI-Einstellungen (settingsStore)

Der `settingsStore` (Pinia) speichert Benutzereinstellungen im localStorage.

### Wichtige Settings

```javascript
// frontend/src/stores/settingsStore.js
const defaultSettings = {
  // Allgemein
  language: 'de',
  theme: 'auto',
  sidebarCollapsed: false,
  showWelcomeTiles: true,    // Kacheln auf Willkommensbildschirm

  // Model Settings
  markdownEnabled: true,
  streamingEnabled: true,
  temperature: 0.7,
  // ... weitere Parameter

  // Vision
  autoSelectVisionModel: true,
  preferredVisionModel: 'llava:7b',
  visionChainEnabled: true,
}
```

### Toggle in Settings.vue

Einstellungen werden in Settings.vue unter "🎨 Allgemeine Einstellungen" verwaltet:

```vue
<input
  type="checkbox"
  v-model="settingsStore.settings.showWelcomeTiles"
/>
```

Die Änderungen werden automatisch im localStorage gespeichert und sind sofort wirksam.

---

## 🎨 Theme-System (Stand: 2025-12-06)

Fleet Navigator bietet 6 verschiedene Themes, organisiert in 3 Kategorien.

### Verfügbare Themes

| Theme | CSS-Klasse | Beschreibung | Status |
|-------|------------|--------------|--------|
| **Tech Dark** | `theme-tech-dark` | Cyberpunk mit Cyan Glow | ✅ Default |
| **Tech Hell** | `theme-tech-light` | Lila/Indigo auf Weiß | ✅ Fertig |
| **Crazy Hell** | `theme-crazy-light` | Neon Pink/Violett auf Rosa | ✅ Fertig |
| **Crazy Dunkel** | `theme-crazy-dark` | Violett/Pink auf Dunkel | ✅ Fertig |
| **Anwalt Hell** | `theme-lawyer-light` | Navy Blue auf Weiß | ✅ Fertig |
| **Anwalt Dunkel** | `theme-lawyer-dark` | Navy Blue auf Dunkel | ✅ Fertig |

### Theme-Architektur

```
frontend/src/
├── assets/
│   └── main.css              # Haupt-Theme-Definitionen
├── components/topbar/
│   └── ThemeSelector.vue     # Theme-Auswahl Dropdown
└── stores/
    └── settingsStore.js      # Speichert uiTheme in localStorage + Backend
```

### Farbpaletten (Stand: 2025-12-06)

#### Tech Dark (Default)
```css
--fleet-orange: #00D9FF;      /* Cyan Glow */
--bg-primary: #0A0A0F;        /* Fast Schwarz */
--text-primary: #E0E0E0;      /* Hellgrau */
```

#### Tech Hell
```css
--fleet-orange: #8B5CF6;      /* Lila */
--text-primary: #6366F1;      /* Indigo */
--text-secondary: #8B5CF6;    /* Lila */
--bg-primary: #FFFFFF;        /* Weiß */
```

#### Crazy Hell
```css
--fleet-orange: #FF0D57;      /* Neon Pink */
--text-primary: #6A0dad;      /* Violett */
--text-secondary: #813c8a;    /* Mittleres Violett */
--bg-primary: #FFF0F5;        /* Rosa */
```

#### Crazy Dunkel
```css
--fleet-orange: #FF0D57;      /* Neon Pink */
--bg-primary: #1A0A1F;        /* Dunkles Violett */
--text-primary: #F0E6F5;      /* Helles Lila */
```

#### Anwalt Hell
```css
--fleet-orange: #1E4D7B;      /* Klassisches Navy Blue */
--text-primary: #1E4D7B;      /* Navy Blue */
--text-secondary: #2C6AA0;    /* Helleres Navy */
--bg-primary: #FFFFFF;        /* Weiß */
/* Serif-Schriften: Lora, Merriweather */
```

#### Anwalt Dunkel
```css
--fleet-orange: #4A90C2;      /* Helles Navy */
--bg-primary: #0D1B2A;        /* Tiefes Navy */
--text-primary: #F0F4F8;      /* Fast Weiß */
/* Serif-Schriften: Lora, Merriweather */
```

### Theme-Komponenten

Jedes Theme definiert Styles für:

| Komponente | CSS-Selektor | Beschreibung |
|------------|--------------|--------------|
| Sidebar | `.sidebar-nav` | Navigation links |
| TopBar | `.topbar-nav` | Header oben |
| Hauptbereich | `.bg-gray-50`, etc. | Content-Bereich |
| Input-Tile | `.input-tile` | Chat-Eingabefeld |
| Textarea | `.input-tile-textarea` | Textfeld im Tile |
| Buttons | `.input-tile-button` | Aktions-Buttons |
| Send-Button | `.input-tile-send` | Senden-Button |
| Nachrichten | `.message-user`, `.message-assistant` | Chat-Nachrichten |
| Begrüßung | `.greeting-text` | "Guten Tag, Max!" |

### Theme-Konsistenz-Regeln

**WICHTIG bei Theme-Änderungen:**

1. **Alle Textfarben müssen zur Palette passen**
   - NICHT generisches Schwarz (`#000000`, `#1A1A1A`) verwenden
   - Stattdessen Theme-Akzentfarbe für Text (z.B. Navy Blue `#1E4D7B`)

2. **Input-Tile braucht eigene Styles**
   - Runde Ecken: `border-radius: 1rem`
   - Textarea mit Padding: `padding: 1rem 1.25rem`
   - Textarea mit inneren runden Ecken: `border-radius: 0.75rem`

3. **Helle Themes brauchen dunkle Akzente**
   - Anwalt Hell: Navy Blue Text auf Weiß
   - Crazy Hell: Violett Text auf Rosa
   - Tech Hell: Indigo Text auf Weiß

4. **CSS-Spezifität beachten**
   - `!important` für Theme-Overrides verwenden
   - Tailwind-Klassen werden sonst nicht überschrieben

### Theme ändern (Frontend)

```javascript
// In ThemeSelector.vue
function setTheme(theme) {
  settingsStore.settings.uiTheme = theme
  settingsStore.saveUiThemeToBackend(theme)
}
```

### Theme persistieren (Backend)

Das Theme wird in der H2-Datenbank gespeichert:

```java
// SystemSettingsController.java
@PutMapping("/ui-theme")
public ResponseEntity<?> setUiTheme(@RequestBody Map<String, String> body) {
    String theme = body.get("theme");
    systemSettingsService.setUiTheme(theme);
    return ResponseEntity.ok().build();
}
```

---

## 📝 Für Claude Code Entwickler

Wenn der Nutzer sagt:
- ❌ "Starte das Frontend" → Frage ob Development oder Production gemeint ist
- ❌ "Backend läuft nicht" → Kläre ab: Dev-Mode oder Production?
- ✅ "Starte Fleet Navigator" → Klar! Ein Befehl, eine App

**Immer daran denken:**
- Production = 1 Server, 1 JAR, Port 2025
- Development = 2 Server (temporär), Ports 2025 + 5173

### ⚠️ Lokaler Entwicklungsmodus (kein systemd!)

Fleet Navigator läuft **lokal** aus dem Projektverzeichnis - KEIN systemd-Service!

**Nach einem Build:**
```bash
# 1. Build
mvn clean package -DskipTests

# 2. Direkt starten (im Projektverzeichnis)
java -jar target/fleet-navigator-0.5.0.jar
```

**Kein sudo nötig!** Alles läuft als normaler Benutzer.

---

## 🎯 Quick Reference

| Szenario | Befehl | URL |
|----------|--------|-----|
| **Normale Nutzung** | `java -jar target/*.jar` | http://localhost:2025 |
| **Entwicklung** | `./START.sh` | http://localhost:5173 |
| **Production Build** | `mvn clean package` | - |

---

## 🚀 Lokaler Betrieb

```bash
# Im Projektverzeichnis
cd ~/ProjekteFMH/Fleet-Navigator

# Build
mvn clean package -DskipTests

# Starten
java -jar target/fleet-navigator-0.5.0.jar

# → http://localhost:2025
```

**Das war's!** Frontend ist im JAR integriert!

---

## 📝 System-Prompts Verwaltung

Fleet Navigator verfügt über ein persistentes System-Prompt-System mit Datenbank-Speicherung.

### Konzept

System-Prompts definieren die Persönlichkeit des KI-Assistenten (z.B. "Karla", "Steuerberater", "Code Expert").

### Architektur

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   TopBar.vue    │────▶│ SettingsModal.vue│────▶│  chatStore.js   │
│ (Anzeige)       │     │ (Auswahl)        │     │ (State)         │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                │                        │
                                ▼                        ▼
                        ┌──────────────────┐     ┌─────────────────┐
                        │     api.js       │────▶│ H2 Database     │
                        │ (REST Calls)     │     │ (Persistenz)    │
                        └──────────────────┘     └─────────────────┘
```

### Backend API-Endpunkte

```bash
# System-Prompts
GET    /api/system-prompts              # Alle Prompts laden
GET    /api/system-prompts/default      # Aktiven/Standard-Prompt laden
POST   /api/system-prompts              # Neuen Prompt erstellen
PUT    /api/system-prompts/{id}         # Prompt aktualisieren
PUT    /api/system-prompts/{id}/set-default  # Als Standard aktivieren
DELETE /api/system-prompts/{id}         # Prompt löschen
```

### Wichtige Dateien

| Datei | Zweck |
|-------|-------|
| `TopBar.vue` | Zeigt aktuellen System-Prompt an (Button klickbar → öffnet Settings) |
| `SettingsModal.vue` | Templates-Tab mit Prompt-Liste und Aktivieren-Button |
| `Settings.vue` | Alternative Prompt-Verwaltung (vollständige Seite) |
| `chatStore.js` | Hält `systemPrompt` und `systemPromptTitle` im State |
| `SystemPromptController.java` | Backend REST Controller |
| `SystemPromptTemplate.java` | JPA Entity mit `isDefault` Flag |

### Aktivierungs-Flow

Wenn User einen System-Prompt aktiviert:

1. **Frontend:** `activateSystemPrompt(prompt)` in SettingsModal.vue
2. **API Call:** `PUT /api/system-prompts/{id}/set-default`
3. **Backend:** Setzt alle anderen `isDefault=false`, diesen auf `true`
4. **Frontend:** `chatStore.systemPrompt = prompt.content`
5. **Frontend:** `chatStore.systemPromptTitle = prompt.name`
6. **Persistenz:** chatStore-Watch speichert in localStorage
7. **TopBar:** Zeigt neuen Prompt-Namen an

### Drei Varianten in TopBar

```vue
<!-- 1. Normale Modelle: Klickbar, öffnet Settings -->
<button v-if="!expertSelected && !customModel" @click="openSettings">
  {{ systemPromptTitle || 'Kein System-Prompt' }}
</button>

<!-- 2. Custom Models: Nicht klickbar (eigener Prompt) -->
<div v-if="!expertSelected && customModel">
  Eigener Modell-Prompt
</div>

<!-- 3. Experten: Prompt kommt vom Experten (nicht hier angezeigt) -->
```

### Bekannte Stolpersteine

⚠️ **Zwei Komponenten, gleiche Funktion:** Sowohl `Settings.vue` als auch `SettingsModal.vue` haben `activatePrompt`-Funktionen. Beide müssen synchron gehalten werden!

⚠️ **chatStore aktualisieren:** Nach DB-Änderung IMMER `chatStore.systemPrompt` und `chatStore.systemPromptTitle` setzen, sonst zeigt TopBar falschen Wert.

⚠️ **Vite Cache:** Bei Frontend-Änderungen `rm -rf frontend/dist frontend/node_modules/.vite target` vor Build.

---

## 🔐 Authentifizierung & Benutzerverwaltung

Fleet Navigator verfügt über ein vollständiges Authentifizierungssystem mit Session-basierter Sicherheit.

### Aktueller Stand (v0.5.0)

| Feature | Status | Beschreibung |
|---------|--------|--------------|
| Login/Logout | ✅ Implementiert | Session-basiert mit Spring Security |
| Registrierung | ✅ Implementiert | Lokale Registrierung ohne E-Mail-Bestätigung |
| Standard-Admin | ✅ Implementiert | `admin` / `admin` beim ersten Start |
| User-Isolation | ✅ Vorbereitet | TrustedMate hat `owner`-Feld |
| E-Mail-Validierung | ❌ Nicht implementiert | Siehe Optionen unten |

### Standard-Zugangsdaten

```
Benutzername: admin
Passwort:     admin
```

⚠️ **WICHTIG:** Passwort nach erstem Login ändern!

### Backend-Architektur

```
src/main/java/io/javafleet/fleetnavigator/
├── config/
│   ├── SecurityConfig.java       # Spring Security Konfiguration
│   └── PasswordConfig.java       # BCrypt PasswordEncoder (separiert wg. circular dependency)
├── model/
│   └── User.java                 # User-Entity (implementiert UserDetails)
├── repository/
│   └── UserRepository.java       # JPA Repository
├── service/
│   └── UserService.java          # UserDetailsService + CRUD
└── controller/
    └── AuthController.java       # Login/Register/Check Endpunkte
```

### Frontend-Architektur

```
frontend/src/
├── stores/
│   └── authStore.js              # Pinia Store für Auth-State
├── views/
│   └── LoginView.vue             # Login/Register UI
└── router/
    └── index.js                  # Navigation Guards
```

### API-Endpunkte

```bash
# Öffentliche Endpunkte (kein Login nötig)
POST   /api/auth/login            # Login (form-urlencoded)
POST   /api/auth/register         # Registrierung (JSON)
GET    /api/auth/check            # Auth-Status prüfen
POST   /api/auth/logout           # Logout
GET    /api/system/version        # Version (für Cache-Invalidierung)

# Geschützte Endpunkte (Login erforderlich)
POST   /api/auth/change-password  # Passwort ändern
GET    /api/**                    # Alle anderen API-Endpunkte
```

### User-Rollen

| Rolle | Rechte |
|-------|--------|
| `USER` | Standard-Benutzer, eigene Daten |
| `ADMIN` | Vollzugriff, Benutzerverwaltung |

### User-Entity

```java
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;           // BCrypt-verschlüsselt
    private String email;              // Optional
    private String displayName;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;     // USER oder ADMIN

    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
```

### Session-Management

- **Session-Timeout:** Standard Spring Boot (30 Minuten)
- **Max Sessions:** 3 pro Benutzer
- **Cookie:** `JSESSIONID` (HttpOnly)
- **CSRF:** Aktiviert mit Cookie-basiertem Token

### Frontend Auth-Flow

```
┌──────────────┐     ┌─────────────┐     ┌──────────────┐
│  LoginView   │────▶│  authStore  │────▶│   Backend    │
│  (UI)        │     │  (State)    │     │  (API)       │
└──────────────┘     └─────────────┘     └──────────────┘
       │                    │                    │
       │  1. User gibt      │                    │
       │     Credentials    │                    │
       │         ─────────▶ │  2. POST /login    │
       │                    │ ─────────────────▶ │
       │                    │                    │
       │                    │  3. Session Cookie │
       │                    │ ◀───────────────── │
       │                    │                    │
       │  4. Redirect       │                    │
       │     to Home        │                    │
       │ ◀───────────────── │                    │
```

### Navigation Guards (router/index.js)

```javascript
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Initialize auth on first navigation
  if (!authStore.isInitialized) {
    await authStore.checkAuth()
  }

  // Public routes (login, register)
  if (to.meta.public) {
    next()
    return
  }

  // Protected routes need authentication
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  next()
})
```

---

## 📧 E-Mail-Validierung (ZUKUNFT)

### Optionen für E-Mail-Handling

| Option | Beschreibung | Aufwand | Empfehlung |
|--------|--------------|---------|------------|
| **A) Keine Validierung** | E-Mail optional, keine Prüfung | ✅ Aktuell | Einfachste Lösung |
| **B) Pflicht mit Bestätigung** | E-Mail required + Confirm-Link | Hoch | Für SaaS/Cloud |
| **C) Optional mit Bestätigung** | E-Mail optional, aber wenn angegeben → Bestätigung | Mittel | **Empfohlen** |

### Option C im Detail (Empfohlen für v0.6.0+)

**Vorteile:**
- User ohne E-Mail können sofort loslegen (idiotensicher!)
- User MIT E-Mail werden verifiziert
- Nur echte, verifizierte Adressen in der Datenbank
- Newsletter-Funktion später möglich

**Benötigte Komponenten:**

```
Backend:
├── EmailVerificationToken.java    # Entity für Tokens
├── EmailService.java              # SMTP-Versand
├── EmailVerificationController.java
└── application.properties         # SMTP-Konfiguration

Frontend:
├── EmailVerificationView.vue      # Bestätigungsseite
└── Hinweis in Registrierung       # "Bitte E-Mail bestätigen"
```

**SMTP-Konfiguration (application.properties):**

```properties
# E-Mail Versand (nur wenn E-Mail-Validierung aktiviert)
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=noreply@javafleet.io
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Fleet Navigator E-Mail Settings
fleet-navigator.email.enabled=false
fleet-navigator.email.from=noreply@javafleet.io
fleet-navigator.email.verification-url=https://app.javafleet.io/verify
```

**Flow mit E-Mail-Bestätigung:**

```
1. User registriert sich mit E-Mail
2. Account erstellt mit emailVerified=false
3. Token generiert + gespeichert (24h gültig)
4. E-Mail mit Bestätigungslink gesendet
5. User klickt Link → emailVerified=true
6. User kann sich einloggen

Ohne E-Mail:
1. User registriert sich OHNE E-Mail
2. Account sofort aktiv (emailVerified=null)
3. Volle Funktionalität
```

**Datenbank-Änderungen:**

```sql
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT NULL;
ALTER TABLE users ADD COLUMN verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN verification_token_expires TIMESTAMP;
```

### DSGVO-Hinweise

Bei E-Mail-Sammlung MUSS beachtet werden:

1. **Einwilligung:** Checkbox "Ich stimme zu, dass..."
2. **Zweck:** Klar kommunizieren wofür E-Mail verwendet wird
3. **Widerruf:** Möglichkeit zur Account-Löschung
4. **Datenschutzerklärung:** Link bei Registrierung

```vue
<label>
  <input type="checkbox" v-model="gdprConsent" required />
  Ich stimme der <a href="/privacy">Datenschutzerklärung</a> zu
  und möchte gelegentlich Updates erhalten.
</label>
```

---

## 🔒 User-Isolation für Multi-Tenancy

### Aktueller Stand

TrustedMate-Entity hat bereits ein `owner`-Feld:

```java
@Entity
public class TrustedMate {
    // ... andere Felder

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;
}
```

### Noch zu implementieren

| Entity | User-Isolation | Status |
|--------|----------------|--------|
| TrustedMate | `owner` Feld | ✅ Vorbereitet |
| Chat | `user_id` | ❌ Noch nicht |
| Expert | Global (shared) | ✅ Bewusst global |
| SystemPrompt | Global (shared) | ✅ Bewusst global |
| CustomModel | `user_id` | ❌ Noch nicht |

### Repository-Pattern für User-Isolation

```java
public interface TrustedMateRepository extends JpaRepository<TrustedMate, Long> {

    // Alle Mates des aktuellen Users
    List<TrustedMate> findByOwnerOrderByLastSeenAtDesc(User owner);

    // Mate eines Users finden
    Optional<TrustedMate> findByMateIdAndOwner(String mateId, User owner);

    // Global (nur für Admins)
    @Query("SELECT t FROM TrustedMate t ORDER BY t.lastSeenAt DESC")
    List<TrustedMate> findAllAdmin();
}
```

### Service-Pattern

```java
@Service
public class TrustedMateService {

    @Autowired
    private UserService userService;

    public List<TrustedMate> getMyMates() {
        User currentUser = userService.getCurrentUser()
            .orElseThrow(() -> new AccessDeniedException("Nicht angemeldet"));
        return repository.findByOwnerOrderByLastSeenAtDesc(currentUser);
    }
}
```

---

## 🔌 LLM Provider Management

Fleet Navigator unterstützt mehrere LLM-Provider mit einfacher Umschaltung in den Einstellungen.

### Verfügbare Provider

| Provider | Beschreibung | Port | FleetCode |
|----------|--------------|------|-----------|
| **llama-server** | Externer llama.cpp Server | 2026 | ✅ Erforderlich |
| **java-llama-cpp** | Eingebetteter JNI-basierter Provider | - | ❌ |
| **llamacpp** | Legacy Server-basierter Provider | 2024 | ❌ |
| **ollama** | Externer Ollama Server | 11434 | ❌ |

### Provider-Architektur

```
┌─────────────────────────────────────────────────────────────┐
│                     LLMProviderService                       │
├──────────────┬──────────────┬──────────────┬────────────────┤
│ llama-server │java-llama-cpp│   llamacpp   │     ollama     │
│ (FleetCode)  │   (JNI)      │   (legacy)   │   (external)   │
│  Port 2026   │   embedded   │  Port 2024   │   Port 11434   │
└──────────────┴──────────────┴──────────────┴────────────────┘
```

### Backend-Komponenten

| Datei | Beschreibung |
|-------|--------------|
| `ExternalLlamaServerProvider.java` | Provider für externen llama-server (FleetCode) |
| `JavaLlamaCppProvider.java` | JNI-basierter eingebetteter Provider |
| `LlamaCppProvider.java` | Legacy Server-basierter Provider |
| `OllamaProvider.java` | Ollama API Integration |
| `LLMProviderService.java` | Provider-Verwaltung und Umschaltung |
| `LLMProviderController.java` | REST API für Provider-Management |

### API-Endpunkte

```bash
# Provider-Status abfragen
GET /api/llm/providers
Response: { activeProvider, availableProviders, providerStatus }

# Provider wechseln
POST /api/llm/providers/switch
Body: { "provider": "llama-server" }

# llama-server Health Check
GET /api/llm/providers/llama-server/health?port=2026
Response: { port, online, status }

# llama-server neu starten (stoppt nur, manueller Start nötig)
POST /api/llm/providers/llama-server/restart
Body: { "port": 2026 }
```

### Frontend-Komponente

Die Provider-Einstellungen befinden sich in `ProviderSettings.vue`:

- 3-Spalten-Grid für Provider-Auswahl
- llama-server als Default markiert (für FleetCode)
- FleetCode Info-Box mit Hinweis auf Port 2026
- Server-Status-Anzeige und Restart-Button
- Manueller Startbefehl-Anzeige

### llama-server starten (für FleetCode)

```bash
# Standard-Startbefehl
LD_LIBRARY_PATH=./bin ./bin/llama-server \
  -m ~/.java-fleet/models/library/qwen2.5-coder-7b-instruct-q4_k_m.gguf \
  --port 2026 \
  --ctx-size 8192 \
  -ngl 99

# Wichtige Parameter:
# --port 2026      : FleetCode erwartet diesen Port
# --ctx-size 8192  : Kontextgröße für Code-Aufgaben
# -ngl 99          : Alle Layer auf GPU (falls verfügbar)
```

### Provider-Priorität bei Auto-Detection

1. Gespeicherter Provider aus Datenbank (höchste Priorität)
2. Konfigurierter Default-Provider (`llm.default-provider`)
3. llama-server (wenn verfügbar)
4. java-llama-cpp (wenn Modelle vorhanden)
5. Erster verfügbarer Provider

---

## 🤖 FleetCode AI Coding Agent

Fleet Navigator integriert **FleetCode** - einen lokalen KI Coding Agent wie Claude Code, aber 100% offline.

### Architektur

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Fleet Navigator│     │  Fleet-Mate     │     │  llama-server   │
│  (Java/Vue.js)  │────▶│  (Go)           │────▶│  (Port 2026)    │
│  Port 2025      │     │  + FleetCode    │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │  WebSocket            │  HTTP /completion
        │  fleetcode_execute    │  + GBNF Grammar
        │  fleetcode_step       │
        │  fleetcode_result     │
```

### Backend-Komponenten

| Datei | Beschreibung |
|-------|--------------|
| `FleetCodeService.java` | Session Management, SSE Streaming |
| `FleetCodeController.java` | REST API `/api/fleetcode/*` |
| `FleetMateWebSocketHandler.java` | WebSocket Handler für `fleetcode_step`, `fleetcode_result` |

### Frontend-Komponenten

| Datei | Beschreibung |
|-------|--------------|
| `FleetCodeTab.vue` | UI Komponente für FleetCode |
| `MateDetailView.vue` | Enthält FleetCode Tab |

### API-Endpunkte

```bash
# FleetCode auf Mate starten
POST /api/fleetcode/execute/{mateId}
Body: { "task": "Finde alle TODOs", "workingDir": "/home/user/projekt" }
Response: { "sessionId": "abc123" }

# Ergebnisse streamen (SSE)
GET /api/fleetcode/stream/{sessionId}
Events: connected, step, result, error

# Session-Status abfragen
GET /api/fleetcode/session/{sessionId}
```

### WebSocket Commands

**Navigator → Mate:**
```json
{
  "type": "fleetcode_execute",
  "payload": {
    "sessionId": "abc123",
    "task": "Finde alle TODO-Kommentare",
    "workingDir": "/home/user/projekt"
  }
}
```

**Mate → Navigator (Step):**
```json
{
  "type": "fleetcode_step",
  "data": {
    "sessionId": "abc123",
    "step": 1,
    "tool": "grep",
    "input": "{\"pattern\": \"TODO\", \"path\": \".\"}",
    "output": "src/main.go:15: // TODO: Fix this",
    "error": null
  }
}
```

**Mate → Navigator (Result):**
```json
{
  "type": "fleetcode_result",
  "data": {
    "sessionId": "abc123",
    "success": true,
    "summary": "Gefunden: 5 TODOs in 3 Dateien",
    "totalSteps": 4,
    "durationSecs": 12.5
  }
}
```

### FleetCode Tools

| Tool | Beschreibung |
|------|--------------|
| `read` | Datei lesen |
| `write` | Datei schreiben |
| `edit` | Text ersetzen |
| `bash` | Shell-Befehl (Windows/Linux) |
| `grep` | Pattern-Suche |
| `glob` | Dateien finden |
| `done` | Aufgabe abgeschlossen |

### Voraussetzungen

1. **llama-server** muss auf Port 2026 laufen:
   ```bash
   ./llama-server -m /pfad/zum/modell.gguf --port 2026 --ctx-size 8192
   ```

2. **Fleet-Mate** muss mit FleetCode aktiviert sein:
   ```yaml
   # config.yml
   fleetcode:
     enabled: true
     llama_server_url: "http://localhost:2026"
   ```

### UI Flow

1. User öffnet Mate Detail View
2. Klickt auf "FleetCode" Tab
3. Gibt Aufgabe und Arbeitsverzeichnis ein
4. Klickt "FleetCode starten"
5. Sieht Schritte in Echtzeit (SSE)
6. Erhält Endergebnis

### Timeout-Verhalten

- Default: 5 Minuten
- Bei Timeout: Partial Result mit bisherigen Schritten
- SSE Connection: 10 Minuten Timeout

---

**Erstellt von:** JavaFleet Systems Consulting
**Port 2025:** Das Geburtsjahr von Fleet Navigator 🚢
