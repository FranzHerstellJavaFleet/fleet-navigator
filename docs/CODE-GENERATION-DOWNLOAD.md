# Code-Generierung mit Download-Feature

**Version:** 0.2.7
**Datum:** 2025-11-07

---

## 🎯 Überblick

Fleet Navigator kann jetzt **automatisch generierte Projekte als ZIP-Datei** zum Download bereitstellen. Wenn ein Benutzer um ein Projekt bittet (z.B. "Erstelle mir ein Maven-Projekt"), wird automatisch eine ZIP-Datei generiert und ein Download-Button angezeigt.

---

## 🚀 Nutzung

### Beispiel-Prompts die einen Download auslösen:

```
"Erstelle mir ein Maven Spring Boot Projekt als ZIP"
"Generiere ein einfaches Java-Projekt zum Download"
"Erstelle ein REST API Projekt mit Maven"
"Erstelle mir ein Maven-Projekt mit pom.xml und Main-Klasse"
```

### Was passiert:

1. **User stellt Anfrage** → AI generiert Code
2. **System erkennt automatisch:**
   - Schlüsselwörter: "als zip", "download", "herunterladen"
   - ODER: 3+ Code-Blöcke in der Antwort (= Multi-File-Projekt)
3. **Backend generiert ZIP** mit allen Dateien
4. **Frontend zeigt Download-Button** unter der AI-Antwort
5. **User klickt Download** → ZIP wird heruntergeladen

---

## 🏗️ Architektur

### Backend-Komponenten

#### 1. **CodeGeneratorService**
```java
Path projectPath = codeGeneratorService.generateProject(aiResponse);
```

**Funktionen:**
- Parst strukturierte JSON-Projekte
- Extrahiert Code-Blöcke aus Markdown
- Erstellt Dateien in `/tmp/fleet-navigator-generated/`
- Auto-Cleanup nach 1 Stunde

**Unterstützte Formate:**

**A) Strukturiertes JSON:**
```json
{
  "project": {
    "name": "my-project",
    "files": [
      {
        "path": "pom.xml",
        "content": "..."
      },
      {
        "path": "src/main/java/App.java",
        "content": "..."
      }
    ]
  }
}
```

**B) Markdown Code-Blöcke:**
````markdown
```java src/main/java/App.java
public class App {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```

```xml pom.xml
<project>
  ...
</project>
```
````

#### 2. **ZipService**
```java
File zipFile = zipService.createZip(projectPath, zipPath);
```

**Funktionen:**
- Rekursives Zippen von Verzeichnissen
- Erhält Dateistruktur
- Human-readable Dateigrößen

#### 3. **DownloadController**

**Endpoints:**

| Endpoint | Methode | Beschreibung |
|----------|---------|--------------|
| `/api/downloads/generate` | POST | Generiert Download aus Code |
| `/api/downloads/{id}` | GET | Lädt ZIP-Datei herunter |
| `/api/downloads/{id}/info` | GET | Download-Metadaten |
| `/api/downloads/{id}` | DELETE | Löscht Download |

**Request Body (generate):**
```json
{
  "content": "... AI response with code blocks ...",
  "filename": "my-project"
}
```

**Response:**
```json
{
  "downloadId": "uuid",
  "filename": "my-project.zip",
  "downloadUrl": "/api/downloads/uuid",
  "sizeBytes": 12345,
  "sizeHumanReadable": "12.1 KB",
  "createdAt": "2025-11-07T12:00:00",
  "expiresAt": "2025-11-07T13:00:00"
}
```

#### 4. **ChatService Integration**

```java
private String checkAndGenerateDownload(String userMessage, String aiResponse) {
    // Automatische Erkennung
    boolean wantsDownload =
        userMessage.contains("als zip") ||
        userMessage.contains("download") ||
        userMessage.contains("erstelle projekt");

    int codeBlockCount = countCodeBlocks(aiResponse);

    if (wantsDownload || codeBlockCount >= 3) {
        // Generate ZIP and return URL
        return "/api/downloads/{uuid}";
    }

    return null;
}
```

**Erweiterte ChatResponse:**
```java
public class ChatResponse {
    private String response;
    private String downloadUrl;  // NEW!
    // ...
}
```

### Frontend-Komponenten

#### 1. **DownloadButton.vue**

**Features:**
- Blaues Gradient-Design
- Icons von Heroicons
- 3 States: Normal, Downloading, Success
- Error-Handling
- Automatischer Browser-Download

**Props:**
```javascript
{
  downloadUrl: String  // z.B. "/api/downloads/uuid"
}
```

**Usage:**
```vue
<DownloadButton
  v-if="message.downloadUrl"
  :downloadUrl="message.downloadUrl"
/>
```

#### 2. **MessageBubble.vue Integration**

```vue
<!-- Download Button (only for AI messages with downloadUrl) -->
<DownloadButton
  v-if="!isUser && message.downloadUrl"
  :downloadUrl="message.downloadUrl"
/>
```

#### 3. **ChatStore Erweiterung**

```javascript
const assistantMessage = {
  role: 'ASSISTANT',
  content: response.response,
  tokens: response.tokens,
  downloadUrl: response.downloadUrl  // NEW!
}
```

---

## 🎨 UI/UX Design

### Download-Button Aussehen:

```
┌────────────────────────────────────────────────────────────┐
│  📥  Projekt bereit zum Download                            │
│      Dein generiertes Projekt als ZIP-Datei                 │
│                                                              │
│                                    [↓ Herunterladen]         │
└────────────────────────────────────────────────────────────┘
```

**Farben:**
- Background: Blau-Gradient (from-blue-50 to-indigo-50)
- Border: Blue-200
- Button: Gradient Blue-500 to Indigo-600
- Icon: DocumentArrowDown

**States:**
1. **Normal:** "Herunterladen" Button
2. **Downloading:** Spinner + "Lädt herunter..."
3. **Success:** ✓ "Download erfolgreich!" (3 Sekunden)
4. **Error:** ✗ "Download fehlgeschlagen. Bitte versuche es erneut."

---

## 🔍 Erkennungskriterien

### Automatische Download-Generierung erfolgt wenn:

#### **Explizite Anfrage:**
- User sagt: "als zip"
- User sagt: "zum download"
- User sagt: "herunterladen"
- User sagt: "download"

#### **Implizite Projekt-Anfrage:**
- User sagt: "erstelle" + ("projekt" ODER "maven" ODER "spring boot" ODER "application")

#### **Automatische Code-Block-Erkennung:**
- AI-Antwort enthält **3 oder mehr Code-Blöcke**
- Logik: Multi-File-Projekt → Automatisch ZIP anbieten

---

## 🗂️ Datei-Management

### Temporäre Dateien:

**Location:** `/tmp/fleet-navigator-generated/`

**Struktur:**
```
/tmp/fleet-navigator-generated/
├── {project-uuid}/
│   └── generated-project/
│       ├── pom.xml
│       ├── src/
│       │   └── main/
│       │       └── java/
│       │           └── App.java
│       └── README.md
└── {download-uuid}.zip
```

### Auto-Cleanup:

**Wann:** Automatisch nach 1 Stunde

**Service:** `CodeGeneratorService.cleanupOldProjects()`

**Scheduled:** Kann mit `@Scheduled` aktiviert werden

```java
@Scheduled(cron = "0 0 * * * *")  // Jede Stunde
public void cleanupTask() {
    codeGeneratorService.cleanupOldProjects();
}
```

---

## 🌐 Internationalisierung (i18n)

### Deutsch:
```javascript
download: {
  title: 'Projekt bereit zum Download',
  description: 'Dein generiertes Projekt als ZIP-Datei',
  button: 'Herunterladen',
  downloading: 'Lädt herunter...',
  success: 'Download erfolgreich!',
  error: 'Download fehlgeschlagen. Bitte versuche es erneut.'
}
```

### Englisch:
```javascript
download: {
  title: 'Project ready for download',
  description: 'Your generated project as ZIP file',
  button: 'Download',
  downloading: 'Downloading...',
  success: 'Download successful!',
  error: 'Download failed. Please try again.'
}
```

---

## 🧪 Testing

### Manuelle Tests:

1. **Einfacher Test:**
```
User: "Erstelle mir eine Java Hello World Klasse als ZIP"
Erwartung: Download-Button erscheint
```

2. **Maven-Projekt:**
```
User: "Erstelle ein Maven Spring Boot REST API Projekt"
Erwartung: Mehrere Dateien, automatischer Download
```

3. **Ohne Download:**
```
User: "Erkläre mir was Maven ist"
Erwartung: Kein Download-Button (nur Text-Antwort)
```

### API-Tests:

```bash
# Generate Download
curl -X POST http://localhost:2025/api/downloads/generate \
  -H "Content-Type: application/json" \
  -d '{
    "content": "```java\npublic class App {}\n```",
    "filename": "test-project"
  }'

# Download ZIP
curl -O http://localhost:2025/api/downloads/{uuid}

# Get Info
curl http://localhost:2025/api/downloads/{uuid}/info
```

---

## 🔒 Sicherheit

### Validierung:

- ✅ Dateinamen werden sanitisiert
- ✅ Path Traversal Prevention (kein `../`)
- ✅ Maximale Dateigröße (konfigurierbar)
- ✅ Temporäre Dateien mit Auto-Cleanup

### Zugriffskontrolle:

- ✅ Download-IDs sind UUIDs (nicht erratbar)
- ✅ Downloads expirieren nach 1 Stunde
- ✅ Keine sensiblen Daten in Logs

---

## 📊 Monitoring & Logging

### Logs:

```java
log.info("Detected download request - generating project ZIP (code blocks: {})", codeBlockCount);
log.info("Generated download: {}", downloadUrl);
log.info("Cleaned up old project: {}", projectName);
```

### Metriken (TODO):

- Anzahl generierter Downloads
- Durchschnittliche ZIP-Größe
- Download-Erfolgsrate
- Cleanup-Statistiken

---

## 🚧 Known Limitations

1. **Einzelner Download pro Antwort**
   - Nur ein Download-Button pro AI-Nachricht
   - Bei mehreren Projekten: Manuell splitten

2. **Markdown-Parsing**
   - Funktioniert am besten mit korrekt formatierten Code-Blöcken
   - Dateinamen müssen im Code-Block-Header sein

3. **ZIP-Größe**
   - Keine explizite Größenbeschränkung (TODO)
   - Abhängig von JVM Heap Size

---

## 🔮 Future Enhancements

- [ ] **Projekt-Templates:** Vordefinierte Projektstrukturen
- [ ] **Custom Build-Scripts:** Maven/Gradle build vor ZIP
- [ ] **Git Repository:** Als `.git` repo exportieren
- [ ] **Docker Support:** Dockerfile automatisch erstellen
- [ ] **Multi-Download:** Mehrere ZIPs pro Antwort
- [ ] **Persistent Storage:** Optional in DB statt `/tmp`
- [ ] **Preview:** ZIP-Inhalt vor Download anzeigen
- [ ] **Direct IDE Import:** VSCode/IntelliJ Integration

---

## 📝 Changelog

### v0.2.7 (2025-11-07)
- ✅ Initial Implementation
- ✅ CodeGeneratorService
- ✅ ZipService
- ✅ DownloadController
- ✅ ChatService Integration
- ✅ DownloadButton Frontend Component
- ✅ Automatische Erkennung
- ✅ I18n Support (DE/EN)

---

**Entwickelt von:** JavaFleet Systems Consulting
**Port 2025:** Das Geburtsjahr von Fleet Navigator 🚢
