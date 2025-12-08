# Eigene LLM Web-Schnittstelle - Vollständige Planung

## Zusammenfassung

**Ziel:** Eigene Web-UI für Ollama mit voller Kontrolle über System-Prompts und optimiert für produktive Code-Arbeit.

**Tech-Stack:** Spring Boot + Vue.js + SQLite + Ollama

**Entwicklungszeit:** 3-5 Tage für vollständige Version 1

---

## Verfügbare Modelle & Context Windows

| Modell | Parameter | Context Window | Tokens | Zeichen (ca.) | Empfohlen für |
|--------|-----------|----------------|--------|---------------|---------------|
| **CodeLlama 70B** | 70B | 16.384 | 16k | ~65.000 | Komplexe Fragen, beste Qualität |
| **Qwen2.5-Coder 7B** | 7B | 32.768 | 32k | ~130.000 | Viele Dateien, Code-Reviews |
| **Llama 3.2 3B** | 3B | 128.000 | 128k | ~512.000 | Riesige Kontexte, simple Fragen |

**Wichtig:** Größere Modelle ≠ größerer Context! Neuere, kleinere Modelle haben oft mehr Context.

---

## Version 1 - Kern-Features (MVP)

### ✅ Basis-Funktionalität
1. **Chat-Interface**
   - Nachrichten senden/empfangen
   - Chat-History anzeigen
   - Neue Chats erstellen

2. **System-Prompt Management**
   - System-Prompt pro Chat konfigurierbar
   - Vorlagen (Templates) für häufige Use Cases

3. **Multi-Modell-Support** ⚠️ WICHTIG
   - Dropdown zur Modell-Auswahl
   - Anzeige: Context-Größe, Parameter-Count
   - Wechsel zwischen Modellen möglich

4. **SQLite Persistierung**
   - Chats speichern
   - Messages speichern
   - Global Stats (Token-Counter)

### ✅ Kritische Features

5. **Streaming mit Toggle** ⚠️ PFLICHT
   - An/Aus schaltbar in Settings
   - Checkbox: "Streaming aktivieren"
   - Server-Sent Events (SSE) für Streaming
   - Fallback auf normale Requests

6. **Stop-Button** ⚠️ PFLICHT
   - Immer sichtbar während Generierung
   - SSE Connection abbrechen
   - Feedback: "Generierung gestoppt"

7. **Systemlast-Monitor** ⚠️ PFLICHT
   - **Live-Anzeige** (Update alle 2 Sekunden)
   - CPU-Auslastung (%)
   - RAM-Nutzung (GB / Total GB)
   - GPU-Auslastung (% - falls vorhanden)
   - GPU-VRAM (GB / Total GB - falls vorhanden)
   - Sidebar oder Header-Bereich

8. **Token-Counter** ⚠️ PFLICHT
   - **Pro Chat**: Aktuelle Token-Count mit Progress-Bar
   - **Warnung bei 80%** des Context-Limits
   - **Gesamt-Statistik**: Alle generierten Tokens
   - **Reset-Funktion** für Gesamt-Statistik

### ✅ Essentials für Code-Arbeit

9. **Markdown-Rendering** ⚠️ PFLICHT
   - `marked.js` für Markdown-Parsing
   - Überschriften (#, ##)
   - Fett/Kursiv (**, *)
   - Listen, Links

10. **Code-Highlighting** ⚠️ PFLICHT
    - `highlight.js` für Syntax-Highlighting
    - Auto-Detection der Sprache
    - Unterstützung: Java, JavaScript, Python, etc.
    - Copy-Button für Code-Blöcke

### ✅ Context-Management (KRITISCH!)

11. **File Upload**
    - Dateien hochladen und in Context laden
    - Unterstützte Formate: .java, .js, .py, .xml, .json, .txt
    - Max. Größe: Warnung bei Context-Limit

12. **Context-Viewer**
    - Liste aller Items im Context:
      - System-Prompt
      - Hochgeladene Dateien
      - Chat-History
    - Token-Count pro Item
    - Gesamt-Token-Anzeige mit Progress-Bar

13. **Context-Management**
    - Manuelle Entfernung einzelner Items
    - Auto-Sliding-Window (alte Messages entfernen)
    - Warnung: "Context-Limit erreicht"

14. **Smart Model Recommendation** ⚠️ WICHTIG
    - Analysiert Context-Größe
    - Empfiehlt passendes Modell
    - Beispiel: "18k Tokens → Nutze Qwen2.5 statt CodeLlama"

---

## Datenbank-Schema (SQLite)

### Entities

```java
@Entity
public class Chat {
    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private String systemPrompt;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL)
    private List<Message> messages;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL)
    private List<ContextItem> contextItems;
}

@Entity
public class Message {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    private Chat chat;
    private String role; // "system", "user", "assistant"
    @Lob
    private String content;
    private Integer tokenCount;
    private LocalDateTime timestamp;
}

@Entity
public class ContextItem {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    private Chat chat;
    private String type; // "file", "text"
    private String name; // Filename oder "Manual Text"
    @Lob
    private String content;
    private Integer tokenCount;
    private LocalDateTime addedAt;
}

@Entity
public class GlobalStats {
    @Id
    private Long id = 1L; // Singleton
    private Long totalTokensGenerated = 0L;
    private Long totalTokensInput = 0L;
    private Long totalChats = 0L;
    private Long totalMessages = 0L;
    private LocalDateTime lastReset;
}
```

---

## Backend API Endpoints

```
# Modelle
GET    /api/models                      # Liste aller Ollama-Modelle mit Context-Info

# Chats
GET    /api/chats                       # Alle Chats
POST   /api/chats                       # Neuen Chat erstellen
GET    /api/chats/{id}                  # Chat laden
PUT    /api/chats/{id}                  # Chat aktualisieren (Titel, System-Prompt)
DELETE /api/chats/{id}                  # Chat löschen

# Messages
POST   /api/chats/{id}/message          # Message senden (non-streaming)
GET    /api/chats/{id}/stream           # SSE Stream für Messages
POST   /api/chats/{id}/stop             # Generierung abbrechen

# Context-Management
POST   /api/chats/{id}/context/file     # Datei hochladen
POST   /api/chats/{id}/context/text     # Text manuell hinzufügen
GET    /api/chats/{id}/context          # Alle Context-Items
DELETE /api/chats/{id}/context/{itemId} # Context-Item entfernen
GET    /api/chats/{id}/context/tokens   # Token-Count des Context

# System-Monitoring
GET    /api/system/metrics              # CPU, RAM, GPU, VRAM (Live)

# Statistiken
GET    /api/stats/global                # Gesamt-Token-Counter
POST   /api/stats/reset                 # Reset Global Stats

# Export
GET    /api/chats/{id}/export/markdown  # Chat als Markdown
GET    /api/chats/{id}/export/json      # Chat als JSON
```

---

## Systemlast-Monitor (Backend)

```java
@RestController
@RequestMapping("/api/system")
public class SystemMetricsController {

    @GetMapping("/metrics")
    public SystemMetrics getMetrics() {
        OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        Runtime runtime = Runtime.getRuntime();

        return SystemMetrics.builder()
            .cpuUsage(osBean.getSystemCpuLoad() * 100)
            .ramUsedGB((runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0 / 1024.0)
            .ramTotalGB(runtime.maxMemory() / 1024.0 / 1024.0 / 1024.0)
            .gpuUsage(getGPUUsage())
            .gpuVramUsedGB(getGPUVRAM())
            .gpuVramTotalGB(getGPUVRAMTotal())
            .build();
    }

    private Double getGPUUsage() {
        try {
            Process process = Runtime.getRuntime()
                .exec("nvidia-smi --query-gpu=utilization.gpu --format=csv,noheader,nounits");

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            return line != null ? Double.parseDouble(line.trim()) : null;
        } catch (Exception e) {
            return null; // Keine GPU oder nvidia-smi nicht verfügbar
        }
    }

    private Double getGPUVRAM() {
        try {
            Process process = Runtime.getRuntime()
                .exec("nvidia-smi --query-gpu=memory.used --format=csv,noheader,nounits");

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            return line != null ? Double.parseDouble(line.trim()) / 1024.0 : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Double getGPUVRAMTotal() {
        try {
            Process process = Runtime.getRuntime()
                .exec("nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits");

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            return line != null ? Double.parseDouble(line.trim()) / 1024.0 : null;
        } catch (Exception e) {
            return null;
        }
    }
}
```

---

## Token-Counter (Backend)

```java
@Service
public class TokenCounterService {

    /**
     * Approximation: 1 Token ≈ 4 Zeichen (Englisch), ≈ 3-4 Zeichen (Deutsch)
     * Für genauere Zählung: tiktoken-java oder sentencepiece
     */
    public int countTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }

    public int getTotalChatTokens(Chat chat) {
        int total = 0;

        // System-Prompt
        if (chat.getSystemPrompt() != null) {
            total += countTokens(chat.getSystemPrompt());
        }

        // Messages
        for (Message msg : chat.getMessages()) {
            total += msg.getTokenCount();
        }

        // Context-Items
        for (ContextItem item : chat.getContextItems()) {
            total += item.getTokenCount();
        }

        return total;
    }

    public boolean isContextLimitReached(Chat chat, String modelName) {
        int contextLimit = getModelContextLimit(modelName);
        int currentTokens = getTotalChatTokens(chat);

        return currentTokens > (contextLimit * 0.8); // 80% Warnung
    }

    private int getModelContextLimit(String modelName) {
        if (modelName.contains("codellama:70b")) return 16384;
        if (modelName.contains("qwen2.5-coder")) return 32768;
        if (modelName.contains("llama3.2")) return 128000;
        return 4096; // Default fallback
    }
}
```

---

## Smart Model Recommendation

```java
@Service
public class ModelRecommendationService {

    @Autowired
    private TokenCounterService tokenCounter;

    public ModelRecommendation recommendModel(Chat chat, List<OllamaModel> availableModels) {
        int contextTokens = tokenCounter.getTotalChatTokens(chat);
        String currentModel = chat.getModelName();

        // Aktuelles Modell passt noch
        int currentLimit = tokenCounter.getModelContextLimit(currentModel);
        if (contextTokens < currentLimit * 0.7) {
            return ModelRecommendation.builder()
                .currentModelOk(true)
                .currentModel(currentModel)
                .build();
        }

        // Suche besseres Modell
        for (OllamaModel model : availableModels) {
            int limit = tokenCounter.getModelContextLimit(model.getName());

            if (contextTokens < limit * 0.7) {
                return ModelRecommendation.builder()
                    .currentModelOk(false)
                    .recommendedModel(model.getName())
                    .reason("Dein Context (" + contextTokens + " Tokens) überschreitet "
                           + currentModel + " Limit (" + currentLimit + " Tokens)")
                    .build();
            }
        }

        // Kein Modell passt - Context reduzieren
        return ModelRecommendation.builder()
            .currentModelOk(false)
            .warningTooLarge(true)
            .message("Context zu groß für alle Modelle. Bitte Dateien/Messages entfernen.")
            .build();
    }
}
```

---

## Context-Management (Backend)

```java
@Service
public class ContextManagementService {

    @Autowired
    private ContextItemRepository contextRepo;

    @Autowired
    private TokenCounterService tokenCounter;

    public ContextItem addFile(Long chatId, MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        int tokens = tokenCounter.countTokens(content);

        // Context-Limit prüfen
        Chat chat = chatService.getChat(chatId);
        int currentTokens = tokenCounter.getTotalChatTokens(chat);
        int limit = tokenCounter.getModelContextLimit(chat.getModelName());

        if (currentTokens + tokens > limit * 0.9) {
            throw new ContextLimitException(
                "Datei zu groß! Würde Context-Limit überschreiten.");
        }

        ContextItem item = new ContextItem();
        item.setChat(chat);
        item.setType("file");
        item.setName(file.getOriginalFilename());
        item.setContent(content);
        item.setTokenCount(tokens);
        item.setAddedAt(LocalDateTime.now());

        return contextRepo.save(item);
    }

    public List<Message> prepareMessagesWithSlidingWindow(Chat chat) {
        List<Message> messages = chat.getMessages();
        int maxTokens = tokenCounter.getModelContextLimit(chat.getModelName());

        // System-Prompt
        int usedTokens = tokenCounter.countTokens(chat.getSystemPrompt());

        // Context-Items (Dateien)
        for (ContextItem item : chat.getContextItems()) {
            usedTokens += item.getTokenCount();
        }

        // Reserve für Antwort
        int availableForMessages = maxTokens - usedTokens - 2000;

        // Messages von hinten nach vorne (neueste zuerst)
        List<Message> result = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);

            if (usedTokens + msg.getTokenCount() > availableForMessages) {
                break; // Zu alt, nicht mehr einbeziehen
            }

            result.add(0, msg);
            usedTokens += msg.getTokenCount();
        }

        return result;
    }
}
```

---

## System-Prompt Vorschläge

### Template 1: Deutscher Code-Assistent (Empfohlen)
```
Du bist ein erfahrener deutscher Software-Entwickler und Code-Assistent.

SPRACHE: Antworte IMMER auf Deutsch, egal in welcher Sprache du gefragt wirst.

FORMATIERUNG:
- Nutze Markdown für Struktur
- # für Hauptüberschriften, ## für Unterüberschriften
- ** für wichtige Begriffe
- ` für inline Code
- ``` für Code-Blöcke mit Sprach-Tag (```java, ```javascript, ```python)
- KEINE Emojis verwenden

STIL:
- Präzise und professionell
- Erkläre Code verständlich
- Best Practices beachten
- Sicherheitsprobleme ansprechen (SQL-Injection, XSS, etc.)
- Bei Code-Reviews: Konstruktives Feedback

REGELN:
- Wenn du Code schreibst: Füge Kommentare hinzu
- Wenn du unsicher bist: Sage es
- Keine halluzinierten Bibliotheken oder APIs
```

### Template 2: Code-Reviewer
```
Du bist ein erfahrener Code-Reviewer.

Deine Aufgabe:
- Code auf Bugs analysieren
- Performance-Probleme identifizieren
- Sicherheitslücken finden (OWASP Top 10)
- Best Practices vorschlagen
- Code-Smell erkennen

Format:
- Nutze Markdown mit Überschriften
- Strukturiere nach: Bugs, Security, Performance, Style
- Zeige problematischen Code mit ```
- Gib Verbesserungsvorschläge

Sprache: Deutsch
Keine Emojis.
```

### Template 3: Architektur-Berater
```
Du bist ein Software-Architektur-Experte.

Fokus:
- Design Patterns (SOLID, Gang of Four)
- Microservices vs. Monolith
- Datenbank-Design
- API-Design (REST, GraphQL)
- Skalierbarkeit

Stil:
- Erkläre Trade-offs
- Zeige Vor- und Nachteile
- Nutze Diagramme (ASCII-Art oder Mermaid)
- Markdown-Formatierung

Sprache: Deutsch
Keine Emojis.
```

---

## UI-Layout (3-Spalten-Design)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ 🔷 LLM WebUI          [Modell: CodeLlama 70B ▼] [Context: 12k/16k]  [⚙️]   │
├──────────┬──────────────────────────────────────────────┬────────────────────┤
│          │                                              │  📊 SYSTEM         │
│ 💬 Chats │  Chat: "Spring Boot Projekt"                │                    │
│          │  ────────────────────────────────────────    │  CPU: 45%          │
│ + Neu    │                                              │  ████░░░░░░        │
│          │  👤 User:                                    │                    │
│ ───────  │  Erkläre Dependency Injection                │  RAM: 12/32 GB     │
│          │                                              │  ████░░░░░░        │
│ Sprint 1 │  🤖 Assistant:                               │                    │
│ Code Rev │  # Dependency Injection                      │  GPU: 98%          │
│ Archit.. │                                              │  █████████░        │
│          │  **Dependency Injection** ist ein Design-    │                    │
│          │  Pattern zur Entkopplung...                  │  VRAM: 18/24 GB    │
│          │                                              │  ████████░░        │
│          │  ```java                                     │                    │
│          │  @Autowired                                  │  ──────────────    │
│          │  private MyService service;                  │  📈 TOKENS         │
│          │  ```                                         │                    │
│          │  [streaming...]                              │  Chat:             │
│          │                                              │  12.456 / 16.384   │
│          │  ────────────────────────────────────────    │  ████████░░░       │
│          │                                              │  76% ⚠️            │
│          │  📚 Context (4 Items):                       │                    │
│          │  • System-Prompt (200 T)                     │  Gesamt:           │
│          │  • UserService.java (1.2k T) [✖]            │  1.234.567         │
│          │  • Config.xml (456 T) [✖]                    │  42 Chats          │
│          │  • Chat History (8.7k T)                     │  [🔄 Reset]        │
│          │  [📎 Datei] [📝 Text]                        │                    │
│          │                                              │  ──────────────    │
│          │  ────────────────────────────────────────    │  ⚙️ SETTINGS       │
│          │  💡 Empfehlung: Context bei 76%!            │                    │
│          │  Nutze Qwen2.5 (32k) für mehr Platz?       │  Streaming:        │
│          │  [Wechseln] [Ignorieren]                     │  ☑ Aktiviert      │
│          │                                              │                    │
│          │  System-Prompt: [Code-Assistent ▼]          │  Theme:            │
│          │  ────────────────────────────────────────    │  🌙 Dunkel        │
│          │                                              │                    │
│          │  [Deine Frage...]                            └────────────────────┘
│          │
│          │  [🛑 Stop] [📤 Senden]
│          │
└──────────┴──────────────────────────────────────────────────────────────────┘
```

### Bereiche:

#### Linke Sidebar (Chat-Liste)
- Button: Neuer Chat
- Liste aller Chats (scrollbar)
- Aktiver Chat hervorgehoben
- Hover: Delete-Button

#### Mittlerer Bereich (Haupt-Chat)
- Chat-Titel (editierbar)
- Nachrichtenverlauf
  - User-Messages (rechts, blau)
  - Assistant-Messages (links, grau)
  - Markdown-gerendert
  - Code mit Syntax-Highlighting
- **Context-Viewer** (aufklappbar)
  - Liste aller Context-Items
  - Token-Count pro Item
  - X-Button zum Entfernen
- **Smart Recommendation** (wenn nötig)
- System-Prompt Dropdown
- Eingabefeld mit Auto-Resize
- Stop/Senden Buttons

#### Rechte Sidebar (Monitoring & Stats)
- **System-Monitor** (Live, alle 2s)
  - CPU Progress-Bar
  - RAM Progress-Bar
  - GPU Progress-Bar (falls vorhanden)
  - VRAM Progress-Bar (falls vorhanden)
- **Token-Statistiken**
  - Chat-Token mit Progress-Bar
  - Warnung bei >80%
  - Gesamt-Token-Counter
  - Reset-Button
- **Settings** (kompakt)
  - Streaming Toggle
  - Theme Switch

---

## Technologie-Stack

### Backend
- **Spring Boot 3.x** (Java 17+)
- **Spring WebFlux** (für Streaming)
- **Spring Data JPA** (für SQLite)
- **SQLite** (embedded DB)
- **RestTemplate/WebClient** (Ollama API)
- **Lombok** (Boilerplate reduzieren)
- **Spring Boot Actuator** (System-Metriken)

### Frontend
- **Vue.js 3** (Composition API)
- **Vite** (Build-Tool, schnell)
- **Axios** (HTTP Client)
- **EventSource** (für SSE/Streaming)
- **Tailwind CSS** (Utility-First CSS)
- **marked.js** (Markdown → HTML)
- **highlight.js** (Code Syntax-Highlighting)
- **Chart.js** (optional: für Token-Statistik-Diagramme)

### DevOps
- **Maven** (Build)
- **H2** (für Tests, statt SQLite)
- **JUnit 5** + **Mockito** (Testing)

---

## Aufwand-Schätzung (Finale Version)

### Version 1 - MVP mit allen Kern-Features

**Features:**
- ✅ Basis-Chat (senden/empfangen)
- ✅ System-Prompt Management
- ✅ Multi-Modell-Support
- ✅ Streaming mit Toggle
- ✅ Stop-Button
- ✅ Systemlast-Monitor (CPU, RAM, GPU, VRAM)
- ✅ Token-Counter (pro Chat + gesamt)
- ✅ Markdown-Rendering
- ✅ Code-Highlighting
- ✅ File Upload
- ✅ Context-Viewer
- ✅ Context-Management (Entfernen, Sliding-Window)
- ✅ Smart Model Recommendation
- ✅ SQLite Persistierung

**Aufwand:**

| Komponente | Stunden | Details |
|------------|---------|---------|
| **Backend** | **16-20h** | |
| Projekt-Setup | 1h | Spring Initializr, Dependencies |
| Ollama API Client | 2h | REST Client mit Streaming |
| JPA Entities & Repos | 2h | Chat, Message, ContextItem, Stats |
| Chat Service | 2h | CRUD, Message-Handling |
| Context Management | 3h | File Upload, Token-Counter, Sliding-Window |
| System-Monitoring | 2h | CPU/RAM/GPU-Auslastung |
| Smart Model Recommendation | 2h | Logik + API |
| API Controllers | 2h | REST Endpoints |
| | | |
| **Frontend** | **18-22h** | |
| Vue.js Setup | 1h | Vite, Router, State Management |
| Chat-UI (Basis) | 4h | Layout, Message-Liste, Input |
| Streaming-Integration | 2h | EventSource, Stop-Button |
| Sidebar (Chats) | 2h | Chat-Liste, Neu-Button |
| Sidebar (Monitoring) | 3h | Live-Metriken, Token-Stats |
| Markdown + Highlighting | 2h | marked.js + highlight.js Integration |
| File Upload UI | 2h | Drag&Drop, Progress |
| Context-Viewer | 2h | Liste, Entfernen-Funktion |
| Model Recommendation UI | 1h | Modal/Toast mit Empfehlung |
| Settings-Dialog | 2h | Streaming Toggle, Theme |
| | | |
| **Testing & Polish** | **6-8h** | |
| Backend-Tests | 2h | Unit-Tests für Services |
| Frontend-Tests | 2h | Component-Tests |
| Integration-Testing | 2h | End-to-End Szenarien |
| Bug-Fixing | 2h | |
| | | |
| **GESAMT** | **40-50h** | **≈ 5-6 Arbeitstage** |

**Realistisch:** Bei 8h/Tag konzentrierter Arbeit: **5-7 Tage**

---

## Entwicklungs-Roadmap

### Phase 1: Foundation (Tag 1-2)
1. ✅ Spring Boot Projekt aufsetzen
2. ✅ SQLite + JPA Entities (Chat, Message, ContextItem, GlobalStats)
3. ✅ Ollama REST Client (Basic, ohne Streaming)
4. ✅ Vue.js Projekt + Layout (3-Spalten)
5. ✅ Chat CRUD (erstellen, laden, löschen)
6. ✅ Messages senden/empfangen (non-streaming erst)
7. ✅ Modell-Auswahl Dropdown

### Phase 2: Kern-Features (Tag 3-4)
1. ✅ **Streaming** mit SSE implementieren
2. ✅ **Stop-Button** (SSE Abbruch)
3. ✅ **Token-Counter** Backend + Frontend
4. ✅ **Systemlast-Monitor** (nvidia-smi Integration)
5. ✅ **Live-Updates** für Monitoring (Polling alle 2s)
6. ✅ **Markdown-Rendering** (marked.js)
7. ✅ **Code-Highlighting** (highlight.js)
8. ✅ **Streaming Toggle** in Settings

### Phase 3: Context-Management (Tag 4-5)
1. ✅ **File Upload** Backend + Frontend
2. ✅ **Context-Viewer** (Liste aller Items)
3. ✅ **Context-Item Entfernung**
4. ✅ **Sliding-Window** Implementierung
5. ✅ **Smart Model Recommendation**
6. ✅ **Warnung bei Context-Limit**

### Phase 4: Polish & Testing (Tag 5-6)
1. ✅ Error-Handling (Backend + Frontend)
2. ✅ Loading-States & Spinners
3. ✅ Responsive Design (Mobile-Ansicht)
4. ✅ Dark Theme (optional)
5. ✅ Keyboard-Shortcuts (Enter = Senden, Strg+L = Neuer Chat)
6. ✅ Unit-Tests
7. ✅ Integration-Tests
8. ✅ Bug-Fixing

### Phase 5: Optional Erweiterungen (Tag 6+)
1. ⭐ Export/Import (JSON, Markdown)
2. ⭐ Parameter-Sliders (Temperature, Top-P, etc.)
3. ⭐ Message-Pinning (wichtige Messages behalten)
4. ⭐ RAG-System (für riesige Codebasen)
5. ⭐ Auto-Summarization (alte Chats zusammenfassen)
6. ⭐ Multi-User Support (Login/Auth)

---

## Ollama API Referenz

### Liste aller Modelle
```bash
curl http://localhost:11434/api/tags
```

**Response:**
```json
{
  "models": [
    {
      "name": "codellama:70b",
      "size": 38818143488,
      "digest": "...",
      "details": {
        "format": "gguf",
        "family": "llama",
        "parameter_size": "70B"
      }
    }
  ]
}
```

### Chat (non-streaming)
```bash
curl http://localhost:11434/api/chat -d '{
  "model": "codellama:70b",
  "messages": [
    {"role": "system", "content": "Du bist ein deutscher Code-Assistent"},
    {"role": "user", "content": "Erkläre Spring Boot"}
  ],
  "stream": false,
  "options": {
    "temperature": 0.7,
    "top_p": 0.9
  }
}'
```

**Response:**
```json
{
  "model": "codellama:70b",
  "message": {
    "role": "assistant",
    "content": "Spring Boot ist ein Framework..."
  },
  "done": true,
  "total_duration": 12345678900,
  "prompt_eval_count": 50,
  "eval_count": 234
}
```

### Chat (streaming)
```bash
curl http://localhost:11434/api/chat -d '{
  "model": "codellama:70b",
  "messages": [
    {"role": "system", "content": "Du bist ein Code-Assistent"},
    {"role": "user", "content": "Hallo"}
  ],
  "stream": true
}'
```

**Response (NDJSON - eine Zeile pro Token):**
```json
{"message":{"role":"assistant","content":"Hallo"},"done":false}
{"message":{"role":"assistant","content":"!"},"done":false}
{"message":{"role":"assistant","content":" Wie"},"done":false}
{"message":{"role":"assistant","content":" kann"},"done":false}
{"message":{"role":"assistant","content":" ich"},"done":false}
{"message":{"role":"assistant","content":" helfen"},"done":false}
{"message":{"role":"assistant","content":"?"},"done":false}
{"done":true,"total_duration":123456789,"eval_count":7}
```

---

## Vorteile gegenüber Open WebUI

✅ **Volle Kontrolle** über System-Prompts (keine Bugs!)
✅ **Keine Blob-Inkompatibilität** - direkter Ollama-Zugriff
✅ **Maßgeschneidert** für deine Bedürfnisse
✅ **Transparenz** - du verstehst jeden Teil
✅ **Erweiterbar** - Features nach Bedarf
✅ **Kein Tool-Lock-In** - nutzt Standard Ollama API
✅ **Lerneffekt** - du baust es selbst
✅ **Performance** - optimiert für deine Hardware
✅ **Context-Management** - besser als Open WebUI
✅ **Multi-Modell** - smart switching basierend auf Context

---

## Nächste Schritte

### 1. Entscheidung treffen
- [ ] MVP jetzt bauen?
- [ ] Features priorisieren?
- [ ] Andere Tools erst testen?

### 2. Projekt aufsetzen
```bash
# Backend
spring init --dependencies=web,data-jpa,lombok \
  --groupId=com.myapp --artifactId=llm-webui \
  --name=llm-webui backend

# Frontend
npm create vue@latest frontend
cd frontend
npm install
npm install axios marked highlight.js
```

### 3. Erste Schritte
1. Ollama API testen (curl)
2. Backend: Ollama Client implementieren
3. Frontend: Basis-Layout
4. Erste Message senden/empfangen

---

## Ressourcen

- **Ollama API Docs:** https://github.com/ollama/ollama/blob/main/docs/api.md
- **Spring WebFlux SSE:** https://www.baeldung.com/spring-server-sent-events
- **Vue.js EventSource:** https://developer.mozilla.org/en-US/docs/Web/API/EventSource
- **SQLite + Spring Boot:** https://www.baeldung.com/spring-boot-sqlite
- **marked.js:** https://marked.js.org/
- **highlight.js:** https://highlightjs.org/
- **Tailwind CSS:** https://tailwindcss.com/
- **nvidia-smi Cheatsheet:** https://nvidia.custhelp.com/app/answers/detail/a_id/3751

---

## FAQ

### Warum Spring Boot und nicht Node.js?
- Du kennst Java/Spring Boot bereits
- Bessere Typ-Sicherheit
- JPA für Datenbank einfacher
- Performance bei CPU-intensiven Tasks

### Warum Vue.js und nicht React?
- Einfacher für Einsteiger
- Weniger Boilerplate
- Gute Dokumentation
- Beide funktionieren - wähle was du kennst!

### Kann ich andere LLM-Backends nutzen?
Ja! Die Architektur ist flexibel:
- Ollama (aktuell)
- LM Studio
- OpenAI API
- LocalAI
- Jedes REST-API-basierte Backend

### Was ist mit Docker?
Später kannst du Dockerize:
```dockerfile
FROM eclipse-temurin:17-jre
COPY target/llm-webui.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

**🎯 Bist du bereit zu starten? Sag mir, wenn du mit dem Bau beginnen willst!**
