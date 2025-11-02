# Fleet Navigator 🚢

**Dein selbst-gehostetes ChatGPT - mit voller Kontrolle über deine AI-Modelle**

Eine moderne, leistungsstarke Web-Anwendung für Ollama LLM-Modelle mit **intelligenter Modellauswahl** - entwickelt von JavaFleet Systems Consulting.

---

## 🎯 Was ist Fleet Navigator?

Fleet Navigator ist eine **selbst-gehostete ChatGPT-Alternative**, die dir vollständige Kontrolle über deine AI-Modelle gibt. Im Gegensatz zu Cloud-Lösungen laufen alle Modelle lokal auf deinem Computer - deine Daten bleiben bei dir!

### Warum Fleet Navigator?

- ✅ **100% Lokal:** Alle Daten bleiben auf deinem Computer
- ✅ **Intelligente Modellauswahl:** System wählt automatisch das beste Modell für deine Aufgabe
- ✅ **Vision-Chaining:** 2-stufige Bildanalyse mit Smart Model Selection
- ✅ **High-Performance Caching:** Reduziert redundante LLM-Aufrufe
- ✅ **Multi-Modal:** Text, Bilder, PDFs, Dokumente
- ✅ **Echtzeit-Monitoring:** CPU, RAM, GPU, VRAM Überwachung
- ✅ **Modell-Verwaltung:** Download, Update, Löschen von Ollama-Modellen
- ✅ **Chat-Historie:** Alle Gespräche persistent gespeichert

---

## 🏗️ Architektur

Fleet Navigator ist **EINE Anwendung** - nicht zwei!

```
Fleet Navigator = Spring Boot Backend + Vue.js Frontend in EINEM JAR
```

### Production Mode (Standard)
```
fleet-navigator.jar (Port 2025)
├── Spring Boot Backend (REST API)
├── Vue.js Frontend (Static Resources)
├── H2 Datenbank (Persistent)
└── Caffeine Cache (Model Selection)
```

### Development Mode (nur für Entwicklung)
```
Terminal 1: Spring Boot (Port 2025)    → Backend API
Terminal 2: Vite Dev Server (Port 5173) → Frontend mit Hot-Reload
```

---

## 🚀 Quick Start

### Voraussetzungen

- **Java 17+** (OpenJDK empfohlen)
- **Maven 3.6+**
- **Ollama** installiert und laufend
- **Node.js 18+** (nur für Entwicklung)

### Installation

```bash
# 1. Ollama installieren (falls noch nicht geschehen)
curl -fsSL https://ollama.com/install.sh | sh

# 2. Ollama starten
ollama serve

# 3. Modelle herunterladen (Beispiele)
ollama pull qwen2.5:7b              # Standard-Modell
ollama pull qwen2.5-coder:14b       # Code-Modell
ollama pull llama3.2:3b             # Schnelles Modell
ollama pull llava:13b               # Vision-Modell

# 4. Fleet Navigator bauen
cd /path/to/Fleet-Navigator
mvn clean package

# 5. Anwendung starten
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar

# 6. Browser öffnen
http://localhost:2025
```

### Entwicklungsmodus (mit Hot-Reload)

```bash
# Start-Script ausführen
./START.sh

# ODER manuell:
# Terminal 1 - Backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm install
npm run dev

# Dann im Browser:
http://localhost:5173
```

---

## ✨ Features

### 🤖 Intelligente Modellauswahl (NEU!)

Fleet Navigator analysiert deine Eingabe und wählt **automatisch** das beste Modell:

#### **Wie es funktioniert:**

```
Code-Frage erkannt
    → Verwendet Code-Modell (z.B. qwen2.5-coder:14b)

Einfache Frage erkannt
    → Verwendet schnelles Modell (z.B. llama3.2:3b)

Komplexe Aufgabe
    → Verwendet Standard-Modell (z.B. qwen2.5:7b)
```

#### **Beispiele:**

```
✅ "Implementiere einen Binärbaum in Java"
   → qwen2.5-coder:14b (Code-Modell)

✅ "Was ist HTTP?"
   → llama3.2:3b (Fast-Modell)

✅ "Analysiere die Performance-Probleme in diesem 500-Zeilen Code..."
   → qwen2.5:7b (Standard-Modell)
```

#### **Ein/Aus-Schalten:**

- **Aktiviert:** System wählt automatisch Code/Fast/Standard-Modell
- **Deaktiviert:** Verwendet immer dein manuell gewähltes Modell
- **Konfigurierbar:** In Einstellungen ⚙️ → Intelligente Modellauswahl

#### **Erkennungslogik:**

**Code-Erkennung:**
- Keywords: `code`, `function`, `class`, `method`, `bug`, `error`, `implement`, `refactor`
- Tech-Begriffe: `HTTP`, `REST`, `API`, `JSON`, `SQL`, `HTML`, `CSS`
- Sprachen: `java`, `python`, `javascript`, `typescript`, `vue`, `react`
- Muster: ` ``` `, Klammern `{}()`, Semikolons `;`

**Einfache Fragen:**
- Kurze Prompts (< 100 Zeichen)
- Fragewörter: "Was ist", "Was bedeutet", "Erkläre", "What is"
- Fragezeichen vorhanden

### 👁️ Vision-Chaining (NEU!)

**2-stufiger Bildanalyse-Prozess mit intelligenter Modellauswahl:**

```
Schritt 1: Vision Model (z.B. llava:13b)
    ↓ analysiert Bild
    ↓ erstellt detaillierte Beschreibung

Schritt 2: Haupt-Model (automatisch gewählt!)
    ↓ bekommt Bildbeschreibung
    ↓ wählt basierend auf deiner Frage:
    ↓   - Code-Frage → Code-Modell
    ↓   - Einfache Frage → Fast-Modell
    ↓   - Komplexe Frage → Standard-Modell
    ↓ gibt detaillierte Analyse
```

#### **Beispiel:**

```
[Screenshot mit Java-Code hochgeladen]
Du: "Was ist falsch in diesem Code?"

1️⃣ llava:13b: "Der Screenshot zeigt Java-Code mit NullPointerException..."
2️⃣ System erkennt "Code" → wählt qwen2.5-coder:14b
3️⃣ qwen2.5-coder:14b: "Das Problem liegt in Zeile 42: object ist null..."
```

#### **Konfiguration:**

In Einstellungen ⚙️ → Vision Model:
- ✅ **Vision-Chaining aktivieren** - 2-stufiger Prozess
- ✅ **Smart Selection für Haupt-Model** - Automatische Modellwahl nach Vision-Analyse

### 💾 High-Performance Caching (NEU!)

**Caffeine-basiertes Caching reduziert redundante LLM-Aufrufe:**

- **Cache-Name:** `modelSelection`
- **TTL:** 30 Minuten
- **Größe:** 500 Einträge max
- **Zweck:** Speichert Modellauswahl-Entscheidungen

**Vorteile:**
- ⚡ Schnellere Antwortzeiten bei ähnlichen Prompts
- 💾 Reduziert Analyse-Overhead
- 🔄 Automatische Cache-Invalidierung bei Settings-Änderungen

**Konfiguration:**
```properties
# application.yaml
fleet-navigator.cache.ttl-minutes=30
fleet-navigator.cache.max-size=500
```

### 🎨 Chat-Interface

- **Multi-Chat-Management:** Mehrere Gespräche parallel
- **Streaming-Antworten:** Echtzeit-Antworten von AI-Modellen
- **Stop-Button:** Laufende Generierungen abbrechen
- **Chat-Verwaltung:**
  - Chats umbenennen
  - Chats löschen
  - Chat-Historie durchsuchen
  - Automatische Titel-Generierung

### 🖼️ Multi-Modal Support

**Datei-Upload für Kontext:**
- **Bilder:** PNG, JPG, JPEG, GIF, WebP, BMP
- **PDFs:** Automatische Text-Extraktion
- **Text-Dokumente:** TXT, MD, CSV, JSON, XML, YAML
- **Code-Dateien:** Java, Python, JavaScript, etc.

**Vision-Modelle:**
- Automatische Erkennung bei Bild-Upload
- Unterstützt: llava:13b, llava:7b, moondream, bakllava
- Bildanalyse, OCR, UI-Element-Erkennung
- **NEU:** Vision-Chaining mit Smart Model Selection

### 🤖 System-Prompt-Verwaltung

**Vorinstallierte Templates:**
- **Karla 🇩🇪** - Deutscher KI-Assistent (Standard)
- **English Assistant 🇬🇧** - Englischer Assistent
- **Code Expert 💻** - Software-Entwicklungs-Experte
- **Vision Expert 🖼️** - Spezialist für Bildanalyse

**Funktionen:**
- ✅ Eigene System-Prompts erstellen und speichern
- ✅ Templates laden und verwenden
- ✅ Templates löschen
- ✅ Persistent in Datenbank gespeichert
- ✅ Kein localStorage - zentrale Verwaltung

### 🔧 Modell-Verwaltung

**Download & Updates:**
- Neue Modelle direkt aus der UI herunterladen
- Bestehende Modelle updaten
- Fortschrittsanzeige beim Download
- ⚠️ **Geplant:** Modal-Dialog mit Blocking UI während Downloads

**Modell-Auswahl:**
- Standard-Modell festlegen (mit ⭐ markiert)
- **NEU:** Code-Modell für Code-Aufgaben
- **NEU:** Fast-Modell für einfache Fragen
- Automatischer Wechsel zu Vision-Modellen bei Bild-Upload
- Modell-Details anzeigen (Größe, Parameter, Familie)
- Modelle löschen

**Empfohlene Modelle:**

| Modell | Größe | Verwendung | Smart Selection |
|--------|-------|------------|-----------------|
| **qwen2.5-coder:14b** | 8.9 GB | Code-Analyse | Code-Modell ⭐ |
| **llama3.2:3b** | 2 GB | Einfache Fragen | Fast-Modell ⚡ |
| **qwen2.5:7b** | 4.7 GB | Allgemein | Standard-Modell 🎯 |
| **llava:13b** | 8.1 GB | Bildanalyse | Vision-Modell 👁️ |
| **mistral:7b** | 4.1 GB | Reasoning | Alternative |

### ⚙️ Einstellungen

**Intelligente Modellauswahl (NEU!):**
- ✅ Automatisches Modell-Routing aktivieren/deaktivieren
- 💻 Code-Modell auswählen (z.B. qwen2.5-coder:14b)
- ⚡ Schnelles Modell auswählen (z.B. llama3.2:3b)
- 🎯 Standard-Modell auswählen (Fallback)

**Vision-Chaining (NEU!):**
- ✅ Vision-Chaining aktivieren (2-stufiger Prozess)
- ✅ Smart Selection für Haupt-Model (automatische Wahl)
- 👁️ Vision-Modell auswählen (z.B. llava:13b)

**Allgemein:**
- Sprache (Deutsch/Englisch)
- Theme (Hell/Dunkel/Auto)
- Streaming aktiviert/deaktiviert

**Modell-Parameter:**
- Temperature (0.0 - 2.0)
- Top-P (0.0 - 1.0)
- Top-K (1 - 100)
- Repeat Penalty
- Context Length
- Max Tokens

### 📊 System-Monitor

**Echtzeit-Überwachung:**
- CPU-Auslastung
- RAM-Nutzung
- GPU-Auslastung (NVIDIA)
- VRAM-Nutzung
- Laufende Ollama-Modelle

**Globale Statistiken:**
- Gesamt-Nachrichten
- Gesamt-Tokens
- Anzahl Chats

### 🤖 Distributed Agent System (NEU! - Coming Soon)

Fleet Navigator integriert ein verteiltes Agent-System für spezialisierte Aufgaben:

#### **📧 Email Agent (Coming Soon)**

**Funktionen (in Entwicklung):**
- Intelligente E-Mail-Kategorisierung (Wichtig, Spam, Newsletter)
- Automatische Antwortvorschläge generieren
- E-Mail-Zusammenfassungen erstellen
- Anhänge analysieren (PDFs, Bilder, Dokumente)
- Multi-Account-Unterstützung
- Kalendererkennung und Terminvorschläge

**Eigene Modell-Konfiguration:**
- Standard-Modell für E-Mail-Analyse (z.B. qwen2.5:7b)
- Vision-Modell für Bild-Anhänge (z.B. llava:13b)

**Einstellungen:**
- IMAP/SMTP Server-Konfiguration
- Auto-Kategorisierung aktivieren/deaktivieren
- SSL/TLS-Verschlüsselung

#### **📄 Dokumenten Agent (Coming Soon)**

**Funktionen (in Entwicklung):**
- Automatische Texterkennung (OCR) für PDFs und Bilder
- Dokumenten-Zusammenfassungen generieren
- Semantische Suche über alle Dokumente
- Metadaten-Extraktion (Autor, Datum, Keywords)
- Dokument-Vergleich und Diff-Anzeige
- Export in verschiedene Formate

**Eigene Modell-Konfiguration:**
- Standard-Modell für Dokumentenanalyse (z.B. qwen2.5-coder:7b)
- Vision-Modell für OCR (z.B. llava:13b)

**Einstellungen:**
- Upload-Verzeichnis festlegen
- Unterstützte Dateiformate (PDF, DOCX, TXT, MD, ODT)
- Max. Dateigröße konfigurieren
- Indexierungs-Strategie (Volltext, Semantisch, Hybrid)
- Auto-OCR und Auto-Indexierung

#### **💻 OS Agent (Coming Soon)**

**Funktionen (in Entwicklung):**
- Sichere Befehls-Ausführung in Sandbox
- Dateioperationen (ls, cat, grep, find)
- Screenshot-Analyse mit Vision Model
- Automatische Task-Ausführung
- Git-Integration (status, log, diff)
- System-Monitoring (CPU, RAM, Disk)

**Eigene Modell-Konfiguration:**
- Standard-Modell für OS-Befehle (z.B. llama3.2:3b - schnell!)
- Vision-Modell für Screenshot-Analyse (z.B. llava:13b)

**Einstellungen:**
- Erlaubte Befehle (Whitelist-System)
- Sandbox-Modus (EMPFOHLEN!)
- Max. Ausführungszeit
- Arbeitsverzeichnis
- Dateisystem-Zugriff
- Netzwerk-Zugriff (Sicherheitsrisiko!)

#### **Warum eigene Modelle pro Agent?**

Jeder Agent hat **spezielle Anforderungen:**

- **Email Agent:** Braucht gutes Sprachverständnis für Kontext → Allround-Modell
- **Dokumenten Agent:** Braucht Code-Verständnis für technische Dokumente → Code-Modell
- **OS Agent:** Braucht schnelle Antworten für Befehle → Kleines, schnelles Modell

**Zugriff über TopBar:**
- 📧 Email Agent Button (mit gelber "Coming Soon" Badge)
- 📄 Dokumenten Agent Button (mit gelber "Coming Soon" Badge)
- 💻 OS Agent Button (mit gelber "Coming Soon" Badge)

**Jeder Agent hat sein eigenes Einstellungs-Modal:**
- Modell-Auswahl (Standard + Vision)
- Agent-spezifische Konfiguration
- Persistent gespeichert in Datenbank
- Unabhängig von Chat-Einstellungen

**Status:**
- Backend-API: ✅ Implementiert
- Frontend-UI: ✅ Implementiert
- Einstellungen: ✅ Konfigurierbar
- Funktionalität: ⚠️ Coming Soon - wird schrittweise implementiert

---

## 🗂️ Projektstruktur

```
Fleet-Navigator/
├── src/
│   ├── main/
│   │   ├── java/io/javafleet/fleetnavigator/
│   │   │   ├── FleetNavigatorApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── ChatController.java          # Chat REST API
│   │   │   │   ├── ModelController.java         # Modell-Verwaltung
│   │   │   │   ├── StatsController.java         # Statistiken
│   │   │   │   ├── SystemController.java        # System-Monitoring
│   │   │   │   ├── FileUploadController.java    # Datei-Upload
│   │   │   │   ├── SystemPromptController.java  # System-Prompts
│   │   │   │   ├── SettingsController.java      # Settings
│   │   │   │   └── AgentController.java         # Distributed Agents (NEU!)
│   │   │   ├── service/
│   │   │   │   ├── OllamaService.java           # Ollama API Integration
│   │   │   │   ├── ChatService.java             # Chat-Logik
│   │   │   │   ├── FileProcessingService.java   # Datei-Verarbeitung
│   │   │   │   ├── SystemMonitorService.java    # System-Monitoring
│   │   │   │   ├── ModelSelectionService.java   # Smart Selection
│   │   │   │   ├── SettingsService.java         # Settings-Verwaltung
│   │   │   │   └── AgentSettingsService.java    # Agent Settings (NEU!)
│   │   │   ├── model/
│   │   │   │   ├── Chat.java                    # Chat-Entity
│   │   │   │   ├── Message.java                 # Nachrichten-Entity
│   │   │   │   ├── ContextItem.java             # Upload-Kontext
│   │   │   │   ├── SystemPromptTemplate.java    # System-Prompts
│   │   │   │   └── AppSettings.java             # Settings (NEU!)
│   │   │   ├── repository/
│   │   │   │   ├── ChatRepository.java
│   │   │   │   ├── MessageRepository.java
│   │   │   │   ├── ContextItemRepository.java
│   │   │   │   ├── SystemPromptTemplateRepository.java
│   │   │   │   └── AppSettingsRepository.java   # Settings (NEU!)
│   │   │   └── config/
│   │   │       ├── WebConfig.java               # CORS, WebSocket
│   │   │       └── CacheConfig.java             # Caffeine Cache (NEU!)
│   │   └── resources/
│   │       ├── application.yaml                 # Spring Boot Config
│   │       └── static/                          # Vue.js Frontend (gebaut)
│   └── test/
├── frontend/
│   ├── src/
│   │   ├── App.vue                              # Hauptkomponente
│   │   ├── components/
│   │   │   ├── Sidebar.vue                      # Chat-Liste
│   │   │   ├── TopBar.vue                       # Top-Leiste mit Buttons
│   │   │   ├── ChatArea.vue                     # Chat-Anzeige
│   │   │   ├── MessageInput.vue                 # Eingabefeld
│   │   │   ├── FileUpload.vue                   # Datei-Upload
│   │   │   ├── SystemMonitor.vue                # System-Überwachung
│   │   │   ├── ModelManager.vue                 # Modell-Verwaltung
│   │   │   ├── SettingsModal.vue                # Einstellungen
│   │   │   ├── EmailAgentModal.vue              # Email Agent (NEU!)
│   │   │   ├── DocumentAgentModal.vue           # Dokumenten Agent (NEU!)
│   │   │   └── OSAgentModal.vue                 # OS Agent (NEU!)
│   │   ├── stores/
│   │   │   ├── chatStore.js                     # Pinia Store (Chat-State)
│   │   │   └── settingsStore.js                 # Pinia Store (Settings)
│   │   └── services/
│   │       └── api.js                           # Backend API Client (erweitert!)
│   ├── package.json
│   └── vite.config.js
├── data/
│   └── fleetnavdb.mv.db                         # H2 Datenbank (persistent)
├── docs/
│   └── FLEET_NAVIGATOR_INTELLIGENTE_MODELLAUSWAHL.md  # Feature-Dokumentation
├── pom.xml                                       # Maven Build
├── CLAUDE.md                                     # Entwickler-Dokumentation
├── QUICK-START.md                                # Schnellstart-Anleitung
├── BUILD-PRODUCTION.md                           # Production-Build Guide
├── START.sh                                      # Dev-Mode Start-Script
└── README.md                                     # Diese Datei
```

---

## 🔌 API-Endpunkte

### Chat-Verwaltung

```
POST   /api/chat/new              # Neuen Chat erstellen
POST   /api/chat/send             # Nachricht senden (Stream)
GET    /api/chat/all              # Alle Chats abrufen
GET    /api/chat/history/{id}     # Chat-Historie abrufen
PATCH  /api/chat/{id}/rename      # Chat umbenennen
DELETE /api/chat/{id}             # Chat löschen
POST   /api/chat/abort/{id}       # Request abbrechen
```

### Modell-Verwaltung

```
GET    /api/models                # Verfügbare Modelle
GET    /api/models/default        # Standard-Modell
POST   /api/models/{name}/default # Standard-Modell setzen
GET    /api/models/{name}/details # Modell-Details
PUT    /api/models/{name}/metadata # Metadaten aktualisieren
DELETE /api/models/{name}         # Modell löschen
POST   /api/models/pull           # Modell herunterladen (SSE)
```

### Datei-Upload

```
POST   /api/files/upload          # Datei hochladen
```

### System-Prompts

```
GET    /api/system-prompts        # Alle Prompts
GET    /api/system-prompts/default # Standard-Prompt
POST   /api/system-prompts        # Prompt erstellen
PUT    /api/system-prompts/{id}   # Prompt aktualisieren
DELETE /api/system-prompts/{id}   # Prompt löschen
POST   /api/system-prompts/init-defaults # Defaults initialisieren
```

### Settings

```
GET    /api/settings/model-selection        # Model Selection Settings
PUT    /api/settings/model-selection        # Settings aktualisieren
```

### Distributed Agents (NEU!)

```
# Email Agent
GET    /api/agents/email/settings           # Email Agent Settings
PUT    /api/agents/email/settings           # Settings aktualisieren
GET    /api/agents/email/status             # Agent Status

# Document Agent
GET    /api/agents/document/settings        # Document Agent Settings
PUT    /api/agents/document/settings        # Settings aktualisieren
GET    /api/agents/document/status          # Agent Status

# OS Agent
GET    /api/agents/os/settings              # OS Agent Settings
PUT    /api/agents/os/settings              # Settings aktualisieren
GET    /api/agents/os/status                # Agent Status

# Overview
GET    /api/agents/overview                 # Alle Agenten Overview
```

### System & Statistiken

```
GET    /api/system/status         # System-Status
GET    /api/stats/global          # Globale Stats
GET    /api/stats/chat/{id}       # Chat-Statistiken
```

---

## 🗄️ Datenbank

**H2 File-Based Database (Persistent)**

```
Speicherort: ./data/fleetnavdb.mv.db
JDBC URL:     jdbc:h2:file:./data/fleetnavdb
Username:     sa
Password:     (leer)
```

**Tabellen:**
- `chat` - Chat-Sitzungen
- `message` - Nachrichten (User & Assistant)
- `context_item` - Hochgeladene Dateien
- `system_prompt_template` - System-Prompt-Vorlagen
- `app_settings` - Anwendungs-Einstellungen (NEU!)

**H2 Console (Development):**
```
URL: http://localhost:2025/h2-console
```

---

## ⚙️ Konfiguration

`src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: fleet-navigator
  datasource:
    url: jdbc:h2:file:./data/fleetnavdb
  jpa:
    hibernate:
      ddl-auto: update
  cache:
    type: caffeine
    cache-names: modelSelection
    caffeine:
      spec: expireAfterWrite=30m,maximumSize=500

server:
  port: 2025

ollama:
  base-url: http://localhost:11434
  default-model: qwen2.5:7b

fleet-navigator:
  cache:
    ttl-minutes: 30
    max-size: 500
```

---

## 🛠️ Entwicklung

### Build & Run

```bash
# Backend + Frontend bauen
mvn clean package

# Nur Backend bauen
mvn clean compile

# Nur Frontend bauen
cd frontend && npm run build

# Tests ausführen
mvn test

# Backend starten (Dev-Mode)
mvn spring-boot:run

# Frontend starten (Dev-Mode mit Hot-Reload)
cd frontend && npm run dev
```

### Frontend-Deployment

```bash
# Frontend bauen
cd frontend
npm run build

# Output nach target/classes/static kopieren
cp -r dist/* ../target/classes/static/
```

Dies passiert automatisch bei `mvn package` durch das Maven Frontend Plugin.

---

## 🐛 Troubleshooting

### Port 2025 bereits belegt

```bash
# Prozess finden und beenden
lsof -i :2025
kill -9 <PID>
```

### Ollama läuft nicht

```bash
# Ollama starten
ollama serve

# Oder im Hintergrund
nohup ollama serve &
```

### H2 Datenbank gesperrt

```bash
# Alle Fleet Navigator Prozesse beenden
pkill -f fleet-navigator

# Lock-File entfernen
rm data/fleetnavdb.mv.db.lock
```

### Frontend zeigt alte Version

```bash
# Hard-Reload im Browser
Strg + Shift + R

# Oder Frontend neu bauen
cd frontend
npm run build
cp -r dist/* ../target/classes/static/
```

### Vision-Modell antwortet auf Englisch

Das Modell könnte korrupt sein (z.B. nach abgebrochenem Update):

```bash
# Modell neu herunterladen
ollama rm llava:13b
ollama pull llava:13b
```

### Smart Selection funktioniert nicht

**Symptome:** Alle Anfragen verwenden Standard-Modell

**Lösung:**
1. Öffne Einstellungen ⚙️
2. Prüfe: "Automatisches Modell-Routing" ist ✅ aktiviert
3. Code-Modell, Fast-Modell konfiguriert
4. Speichern und neu versuchen

**Logs prüfen:**
```bash
grep "Smart model selection" logs/spring.log
grep "Code-related prompt detected" logs/spring.log
```

### Cache veraltet

**Symptome:** Settings geändert, aber alte Modelle werden verwendet

**Lösung:**
Cache wird automatisch geleert beim Speichern, aber manuell:

```bash
# Backend neu starten
./START.sh
```

---

## 📚 HowTo - Schritt-für-Schritt-Anleitungen

### 🤖 Wie man neue Modelle hinzufügt

Fleet Navigator macht es einfach, neue Ollama-Modelle hinzuzufügen:

**Methode 1: Über die UI (empfohlen)**

1. **Modell-Manager öffnen:**
   - Klicke auf den Button **🔧 Modelle** in der oberen Leiste

2. **Modell herunterladen:**
   - Scrolle nach unten zum Bereich "Neues Modell herunterladen"
   - Gib den Modellnamen ein (z.B. `llama3.2:3b`, `qwen2.5:7b`, `mistral:7b`)
   - Klicke auf "Modell herunterladen"
   - Warte, bis der Download abgeschlossen ist (Fortschrittsanzeige beachten!)

3. **Als Standard setzen (optional):**
   - Finde das neue Modell in der Liste
   - Klicke auf "Als Standard setzen"
   - Das Modell wird jetzt mit ⭐ markiert

**Methode 2: Über die Kommandozeile**

```bash
# Terminal öffnen
ollama pull llama3.2:3b

# Modell testen
ollama run llama3.2:3b "Hallo, wie geht's?"

# Fleet Navigator neu laden - das Modell erscheint automatisch
```

**Beliebte Modelle zum Ausprobieren:**

| Modell | Größe | Beschreibung | Verwendung |
|--------|-------|--------------|------------|
| `qwen2.5:7b` | 4.7 GB | Exzellentes Allround-Modell | Text, Code, Deutsch |
| `qwen2.5-coder:14b` | 8.9 GB | Code-Spezialist | Code-Analyse ⭐ |
| `llama3.2:3b` | 2 GB | Schnell, effizient | Alltags-Chats ⚡ |
| `llava:13b` | 8.1 GB | Vision-Modell | Bildanalyse, OCR 👁️ |
| `mistral:7b` | 4.1 GB | Stark bei Reasoning | Komplexe Fragen |

**⚠️ Wichtig:**
- Stelle sicher, dass genug Speicherplatz vorhanden ist
- Größere Modelle (>7B Parameter) benötigen mehr RAM/VRAM
- Während des Downloads ist die UI blockiert (geplanter Fix: Modal-Dialog)

---

### 🤖 Wie man Smart Selection konfiguriert (NEU!)

**Schritt 1: Einstellungen öffnen**
1. Klicke auf **⚙️ Einstellungen** in der oberen Leiste

**Schritt 2: Modelle auswählen**

```
🤖 Intelligente Modellauswahl
├─ [✓] Automatisches Modell-Routing aktivieren
│
├─ 💻 Code-Modell: [qwen2.5-coder:14b ▼]
│   └─ Wird verwendet für: Code, Debugging, technische Fragen
│
├─ ⚡ Schnelles Modell: [llama3.2:3b ▼]
│   └─ Wird verwendet für: Einfache Fragen, Definitionen
│
└─ 🎯 Standard-Modell: [qwen2.5:7b ▼]
    └─ Wird verwendet für: Komplexe Aufgaben, Fallback
```

**Schritt 3: Vision-Chaining konfigurieren**

```
🖼️ Vision Model
├─ 👁️ Vision Model: [llava:13b ▼]
│
├─ [✓] Vision-Chaining aktivieren
│   └─ Vision Output → Haupt-Model (2-stufig)
│
└─ [✓] Intelligente Modellauswahl für Haupt-Model
    ├─ AN:  Vision → Smart Model (Code/Fast/Default)
    └─ AUS: Vision → Manuell gewähltes Model
```

**Schritt 4: Speichern**
- Klicke auf "Speichern"
- Cache wird automatisch geleert
- Änderungen sind sofort aktiv

**Empfohlene Konfiguration:**

```
✅ Intelligente Modellauswahl: AN
💻 Code-Modell: qwen2.5-coder:14b (größeres Modell für präzise Analyse)
⚡ Schnelles Modell: llama3.2:3b (kleines Modell für Q&A)
🎯 Standard-Modell: qwen2.5:7b (gute Balance)
👁️ Vision Model: llava:13b (bestes Vision-Modell)
✅ Vision-Chaining: AN
✅ Vision Smart Selection: AN
```

---

### 💬 Wie man System-Prompts erstellt

System-Prompts steuern das Verhalten und die Persönlichkeit der AI. So erstellst du eigene:

**Schritt 1: System-Prompt-Editor öffnen**

1. Klicke auf **💭 System Prompt** in der oberen Leiste
2. Der Editor klappt auf

**Schritt 2: Prompt schreiben**

Schreibe deinen System-Prompt in das Textfeld. **Gute Beispiele:**

```
Du bist ein erfahrener Python-Entwickler.

Dein Fokus:
- Clean Code und Best Practices
- Detaillierte Code-Erklärungen
- Type Hints verwenden
- Ausführliche Kommentare

Antworte auf Deutsch und erkläre komplexe Konzepte verständlich.
```

```
Du bist ein SEO-Experte für deutsche Webseiten.

Deine Expertise:
- Keyword-Recherche und -Optimierung
- Meta-Tags und Strukturierte Daten
- Content-Strategie für Google.de
- DSGVO-konforme Analytics

Gib konkrete, umsetzbare Empfehlungen.
```

**Schritt 3: Als Vorlage speichern**

1. Klicke auf **💾 Als Vorlage speichern**
2. Gib einen Namen ein (z.B. "Python Expert" oder "SEO Berater")
3. Klicke auf "Speichern"

**Schritt 4: Vorlage verwenden**

1. Öffne **💭 System Prompt** erneut
2. Unter "Gespeicherte Vorlagen" siehst du deine Templates
3. Klicke auf einen Template-Namen, um ihn zu laden
4. Klicke auf "Fertig"

**📝 Tipps für gute System-Prompts:**

✅ **Mach:**
- Sei spezifisch über Rolle und Expertise
- Gib klare Anweisungen zum Stil (Deutsch/Englisch, formal/casual)
- Liste konkrete Fähigkeiten oder Fokus-Bereiche auf
- Nutze Struktur (Überschriften, Listen)

❌ **Vermeide:**
- Aggressive Befehle ("Du MUSST...") - wirkt unnatürlich
- Zu vage ("Sei hilfreich") - zu allgemein
- Widersprüchliche Anweisungen
- Zu lange Prompts (>500 Wörter)

**Vorinstallierte Templates:**
- **Karla 🇩🇪** - Deutscher Allround-Assistent (Standard)
- **English Assistant 🇬🇧** - Englischer Assistent
- **Code Expert 💻** - Software-Entwicklung
- **Vision Expert 🖼️** - Bildanalyse

---

### 📁 Wie man Chats verwaltet

Fleet Navigator bietet volle Kontrolle über deine Chat-Historie:

**Neuen Chat erstellen**

1. **Option A:** Klicke auf **+ Neuer Chat** in der Sidebar
2. **Option B:** Sende einfach eine Nachricht - ein neuer Chat wird automatisch erstellt

**Chat umbenennen**

1. Fahre mit der Maus über einen Chat in der Sidebar
2. Klicke auf das **✏️ Bearbeiten**-Icon
3. Gib einen neuen Titel ein
4. Drücke Enter oder klicke auf "Speichern"

**Tipp:** Gute Chat-Titel helfen beim Wiederfinden:
- ✅ "Python FastAPI Tutorial"
- ✅ "MySQL Performance-Problem"
- ❌ "Chat 1" (zu generisch)

**Chat löschen**

1. Fahre mit der Maus über einen Chat in der Sidebar
2. Klicke auf das **🗑️ Löschen**-Icon
3. Bestätige die Sicherheitsabfrage

⚠️ **Warnung:** Gelöschte Chats können NICHT wiederhergestellt werden!

**Chat-Organisation (Tipps):**

📌 **Benennungs-Schema verwenden:**
```
[Projekt] Thema - Datum
Beispiele:
- [FleetNav] Bugfix GPU Monitoring - 31.10
- [Lernen] Vue.js Composition API
- [Privat] Urlaubsplanung Italien
```

📌 **Alte Chats regelmäßig archivieren:**
- Aktuell: Exportiere wichtige Chats manuell (Copy & Paste)
- Geplant: Export/Import-Feature (siehe Roadmap)

📌 **Standard-Modell pro Thema:**
- Coding: `qwen2.5-coder:14b` (Code-Modell)
- Bildanalyse: `llava:13b` (Vision-Modell)
- Allgemein: `qwen2.5:7b` (Standard-Modell)

---

### 🚀 Wie man die Anwendung deployed

Fleet Navigator kann auf verschiedene Arten deployed werden:

#### **Option 1: Standalone JAR (einfachste Methode)**

**Für den lokalen Gebrauch:**

```bash
# 1. Projekt bauen
cd /path/to/Fleet-Navigator
mvn clean package

# 2. JAR-Datei ausführen
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar

# 3. Browser öffnen
# http://localhost:2025
```

**Als Systemd-Service (Linux):**

```bash
# 1. Service-Datei erstellen
sudo nano /etc/systemd/system/fleet-navigator.service
```

Inhalt:
```ini
[Unit]
Description=Fleet Navigator - Self-Hosted AI Chat
After=network.target

[Service]
Type=simple
User=dein-username
WorkingDirectory=/home/dein-username/Fleet-Navigator
ExecStart=/usr/bin/java -jar /home/dein-username/Fleet-Navigator/target/fleet-navigator-0.1.0-SNAPSHOT.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
# 2. Service aktivieren und starten
sudo systemctl daemon-reload
sudo systemctl enable fleet-navigator
sudo systemctl start fleet-navigator

# 3. Status prüfen
sudo systemctl status fleet-navigator

# Logs anzeigen
sudo journalctl -u fleet-navigator -f
```

#### **Option 2: Mit Nginx Reverse Proxy (Production)**

**Vorteile:**
- HTTPS-Verschlüsselung
- Domain-Namen verwenden (z.B. `ai.meinefirma.de`)
- Port 80/443 statt 2025

**Nginx-Konfiguration:**

```nginx
server {
    listen 80;
    server_name ai.meinefirma.de;

    # Optional: HTTPS-Weiterleitung
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ai.meinefirma.de;

    # SSL-Zertifikate (z.B. Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/ai.meinefirma.de/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ai.meinefirma.de/privkey.pem;

    location / {
        proxy_pass http://localhost:2025;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket-Support für Streaming
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }
}
```

```bash
# Nginx neu laden
sudo nginx -t
sudo systemctl reload nginx
```

#### **Option 3: Docker (geplant)**

```dockerfile
# Dockerfile (Beispiel - noch nicht getestet)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/fleet-navigator-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 2025
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build und Run
docker build -t fleet-navigator .
docker run -p 2025:2025 -v ./data:/app/data fleet-navigator
```

#### **Sicherheits-Checkliste für Production:**

- [ ] **Firewall konfigurieren** (nur Port 80/443 öffnen, nicht 2025)
- [ ] **HTTPS aktivieren** (Let's Encrypt empfohlen)
- [ ] **Authentifizierung einrichten** (z.B. Nginx Basic Auth oder Spring Security)
- [ ] **Regelmäßige Backups** der H2-Datenbank (`data/fleetnavdb.mv.db`)
- [ ] **Log-Rotation** konfigurieren
- [ ] **Monitoring** einrichten (z.B. Uptime-Checks)
- [ ] **Updates** regelmäßig einspielen

#### **Deployment-Szenarien:**

**🏠 Privat zu Hause:**
```bash
java -jar fleet-navigator.jar
# → http://localhost:2025
```

**🏢 Im lokalen Netzwerk:**
```bash
java -jar fleet-navigator.jar
# → http://192.168.1.100:2025 (von anderen Geräten erreichbar)
```

**☁️ Auf VPS/Cloud-Server:**
```bash
# Systemd-Service + Nginx Reverse Proxy
# → https://ai.meinefirma.de
```

**🔐 Nur per VPN erreichbar:**
```bash
# Fleet Navigator auf VPS, Zugriff nur per WireGuard/OpenVPN
# → https://ai.internal.meinefirma.de
```

---

## 📋 To-Do / Geplante Features

### Kurzfristig
- [ ] **Modal-Dialog für Model-Downloads** mit Fortschrittsanzeige und UI-Blocking
- [ ] **System-Prompt Upload** (Prompts als Datei hochladen)
- [ ] **Projekt-Verwaltung** (Chats in Projekte gruppieren)
- [ ] **Export/Import** von Chats (JSON/Markdown)
- [ ] **Response-Caching** für vollständige LLM-Antworten

### Mittelfristig
- [ ] **Multi-Model-Conversations** (Modell während Chat wechseln)
- [ ] **RAG-System mit Embeddings** (Semantic Search)
- [ ] **Code-Execution** (sichere Code-Ausführung in Sandbox)
- [ ] **Voice-Input** (Whisper-Integration)
- [ ] **Prometheus-Integration** für Metriken

### Langfristig
- [ ] **Multi-User Support** mit Authentifizierung
- [ ] **Collaboration-Features** (Chats teilen)
- [ ] **Plugin-System** für Erweiterungen
- [ ] **Mobile App** (React Native)
- [ ] **A/B Testing** für Modellauswahl-Strategien

---

## 🔐 Sicherheit

- ✅ **Keine Cloud:** Alle Daten bleiben lokal
- ✅ **CORS:** Korrekt konfiguriert
- ✅ **File-Upload:** Validierung und Größenlimits
- ✅ **SQL-Injection:** Geschützt durch JPA/Hibernate
- ✅ **Caching:** Sichere Cache-Invalidierung
- ⚠️ **Auth:** Aktuell keine Authentifizierung (Single-User)

**Empfehlung für Production:**
- Reverse Proxy (nginx/Apache) mit HTTPS
- Authentifizierung (Spring Security)
- Firewall-Regeln (nur localhost oder VPN)

---

## 📊 Performance

**Empfohlene Hardware:**

| Komponente | Minimum | Empfohlen | Optimal |
|------------|---------|-----------|---------|
| CPU | 4 Cores | 8+ Cores | Ryzen 9 9950X3D |
| RAM | 8 GB | 16+ GB | 128 GB |
| GPU | Keine | NVIDIA (8+ GB VRAM) | RTX 5090 32GB |
| Disk | 10 GB | 50+ GB SSD | 2 TB NVMe |

**Modell-Größen (Beispiele):**
- llama3.2:3b → ~2 GB
- qwen2.5:7b → ~4.7 GB
- qwen2.5-coder:14b → ~8.9 GB
- llava:13b → ~8.1 GB
- mixtral:8x7b → ~26 GB

**Performance-Erwartungen (mit RTX 5090):**
- 8B Modell: ~100-150 tokens/s 🚀
- 70B Modell: ~20-35 tokens/s
- Code-Modell: Präzise Analyse ⭐
- Fast-Modell: 30-50% schneller bei einfachen Fragen ⚡

---

## 🤝 Entwickelt von

**JavaFleet Systems Consulting**
Essen-Rüttenscheid, Deutschland

**Lead Developer:** Franz-Martin Heßmer (Der Kapitän)

**Tech Stack:**
- Enterprise Java (Spring Boot, JPA, Hibernate)
- Modern Frontend (Vue.js 3, Tailwind CSS, Pinia)
- AI/ML Integration (Ollama, LLMs)
- Caching (Caffeine)
- DevOps (Maven, Docker)

---

## 📜 Lizenz

**Proprietary - JavaFleet Systems Consulting**
Alle Rechte vorbehalten.

Dieses Projekt ist privat und nicht für die öffentliche Nutzung oder Verbreitung bestimmt.

---

## 🚀 Status

| Info | Wert |
|------|------|
| **Version** | 0.1.0-SNAPSHOT |
| **Status** | 🟢 **Produktiv** (MVP + Smart Features) |
| **Port** | 2025 (Geburtsjahr von Fleet Navigator!) |
| **Letzte Aktualisierung** | 1. November 2025 |

---

## 📝 Changelog

### Version 0.1.0 (1. November 2025)

**✨ Neue Features:**
- ✅ **Intelligente Modellauswahl** - Automatisches Routing zu Code/Fast/Standard-Modellen
- ✅ **Vision-Chaining mit Smart Selection** - 2-stufige Bildanalyse mit intelligenter Modellwahl
- ✅ **High-Performance Caching** - Caffeine-basiert, 30 Min TTL, 500 Einträge
- ✅ **Settings-Verwaltung** - Datenbank-persistierte Einstellungen
- ✅ **Code-Modell-Erkennung** - Keywords, Tech-Begriffe, Muster-Analyse
- ✅ **Fast-Model für einfache Fragen** - Optimierte Performance
- 🆕 **Distributed Agent System** - Email, Dokumenten & OS Agents (UI + Settings implementiert, Funktionalität Coming Soon)

**✨ Bestehende Features:**
- ✅ Chat-Interface mit Streaming
- ✅ Multi-Chat-Management (Erstellen, Umbenennen, Löschen)
- ✅ Datei-Upload (Bilder, PDFs, Text)
- ✅ Vision-Modell-Support mit Auto-Erkennung
- ✅ System-Prompt-Verwaltung mit DB-Persistenz
- ✅ Modell-Verwaltung (Download, Update, Löschen)
- ✅ System-Monitoring (CPU, RAM, GPU, VRAM)
- ✅ Einstellungen-Modal (Theme, Sprache, Modell-Parameter)
- ✅ Token-Counter und Statistiken
- ✅ H2-Datenbank mit persistenter Speicherung

**🐛 Fixes:**
- ✅ CORS-Konfiguration korrigiert
- ✅ Frontend/Backend als EINE Anwendung dokumentiert
- ✅ Vision-Modell-Erkennung verbessert
- ✅ System-Prompt aus localStorage in DB migriert
- ✅ LazyInitializationException in Vision-Chaining behoben

**🔧 Technische Änderungen:**
- ✅ Caffeine Cache-Integration
- ✅ ModelSelectionService mit Caching
- ✅ SettingsService für zentrale Konfiguration
- ✅ AppSettings Entity für DB-Persistenz
- ✅ Erweiterte SettingsModal.vue UI
- ✅ API-Endpoints für Settings

---

## 🙏 Credits

**Technologien:**
- [Ollama](https://ollama.com/) - Lokale LLM-Runtime
- [Spring Boot](https://spring.io/projects/spring-boot) - Backend-Framework
- [Vue.js](https://vuejs.org/) - Frontend-Framework
- [Tailwind CSS](https://tailwindcss.com/) - CSS-Framework
- [H2 Database](https://www.h2database.com/) - Embedded Database
- [Caffeine](https://github.com/ben-manes/caffeine) - High-Performance Caching

**Inspiration:**
- OpenAI ChatGPT - Für das Interface-Design
- Open WebUI - Als Ausgangspunkt (aber mit mehr Kontrolle!)
- local-llm-demo-full - Für Smart Model Selection Features

---

## 📚 Weitere Dokumentation

- **CLAUDE.md** - Entwickler-Dokumentation für Claude Code
- **QUICK-START.md** - Schnellstart-Anleitung
- **BUILD-PRODUCTION.md** - Production-Build Guide
- **docs/FLEET_NAVIGATOR_INTELLIGENTE_MODELLAUSWAHL.md** - Detaillierte Feature-Dokumentation für Smart Selection & Vision-Chaining

---

**Navigate your AI fleet with precision - powered by JavaFleet Systems** 🚢

```
     🚢 Fleet Navigator
    _____|_____
   |   2025   |
   |___________|
     |     |
     |     |
  ~~~~~~~~~~~~~~~~~
```

**Starte deine Reise in die Welt der lokalen AI!**

→ `java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar`

→ `http://localhost:2025`
