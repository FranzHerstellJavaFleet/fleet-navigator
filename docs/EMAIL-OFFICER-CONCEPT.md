# Email Officer - AI-gestützte Email-Sortierung

**Version:** 0.2.0 (Überarbeitetes Konzept)
**Datum:** 2025-11-07
**Status:** 🚧 Planung

---

## 🎯 Vision

**Email Officer = AI Email Agent** der automatisch Emails in drei Kategorien sortiert:

1. ✅ **Wichtig** - Benötigt sofortige Aufmerksamkeit
2. 📋 **Abzuarbeiten** - Tasks, Termine, Projekte
3. 🗑️ **Werbung** - Newsletter, Marketing, Spam

**Das Markenzeichen:** Intelligente, KI-gestützte Email-Triage!

---

## 🏗️ Architektur (Überarbeitet)

```
┌─────────────────────────────────────────────────────────┐
│  Email Officer (Extension ODER IMAP Client)             │
│  ├── Thunderbird Extension (WebExtension)               │
│  ├── Outlook Extension (Office.js Add-in)               │
│  └── IMAP/SMTP Client (für Freemail-Accounts)           │
│                                                           │
│  Features:                                               │
│  • Email-Abruf (IMAP/API)                                │
│  • Fleet Officer Client (WebSocket)                      │
│  • Ordner-Verwaltung                                     │
└─────────────────────────────────────────────────────────┘
                    ↓ WebSocket
┌─────────────────────────────────────────────────────────┐
│  Fleet Navigator (Spring Boot + AI)                     │
│  ├── Email Processing Service                            │
│  │   └── AI Model (User-wählbar)                         │
│  │       • llama3.2:3b                                    │
│  │       • qwen2.5:7b                                     │
│  │       • mistral:7b                                     │
│  │       • deepseek-r1:7b                                 │
│  │                                                         │
│  ├── AI Classifier                                        │
│  │   └── Kategorisierung:                                 │
│  │       → "wichtig" (Inbox)                              │
│  │       → "abzuarbeiten" (Todo)                          │
│  │       → "werbung" (Spam/Archive)                       │
│  │                                                         │
│  └── Notification Service                                 │
│      └── Desktop-Benachrichtigung bei wichtigen Emails    │
└─────────────────────────────────────────────────────────┘
                    ↓ WebSocket Commands
┌─────────────────────────────────────────────────────────┐
│  Email Officer                                           │
│  └── Empfängt Sortier-Kommandos:                         │
│      • moveEmail(id, folder)                              │
│      • markAsImportant(id)                                │
│      • createFolder(name)                                 │
└─────────────────────────────────────────────────────────┘
```

---

## 📧 Email-Quellen

### 1. **Thunderbird Integration** (WebExtension)
```javascript
// Zugriff auf alle Thunderbird-Accounts
const accounts = await browser.accounts.list();
const messages = await browser.messages.list(folder);
```

### 2. **Outlook Integration** (Office.js Add-in)
```javascript
// Zugriff auf Outlook-Postfach
Office.context.mailbox.getCallbackTokenAsync((token) => {
  // Microsoft Graph API
});
```

### 3. **Freemail IMAP/SMTP** (Standalone Client)
```java
// JavaMail API (im Email Officer als separater Prozess)
Properties props = new Properties();
props.put("mail.store.protocol", "imaps");
props.put("mail.smtp.host", "smtp.gmail.com");

Store store = session.getStore("imaps");
store.connect("imap.gmail.com", "user@gmail.com", "password");
```

**Unterstützte Provider:**
- ✅ Gmail (smtp.gmail.com / imap.gmail.com)
- ✅ GMX (mail.gmx.net)
- ✅ Web.de (imap.web.de)
- ✅ Outlook.com (outlook.office365.com)
- ✅ Yahoo (imap.mail.yahoo.com)
- ✅ Beliebige IMAP/SMTP Server

---

## 🤖 AI-Klassifizierung

### Ablauf:

```
1. Email Officer ruft neue Email ab
   ↓
2. Sendet Email-Metadaten an Fleet Navigator:
   {
     "from": "chef@firma.de",
     "subject": "Projektmeeting morgen 10 Uhr",
     "preview": "Hallo Team, morgen findet...",
     "date": "2025-11-07T10:30:00"
   }
   ↓
3. Fleet Navigator → AI Model (z.B. llama3.2:3b)
   Prompt:
   "Klassifiziere diese Email als 'wichtig', 'abzuarbeiten' oder 'werbung':

   Von: chef@firma.de
   Betreff: Projektmeeting morgen 10 Uhr
   Vorschau: Hallo Team, morgen findet...

   Wichtig: Dringende Emails, Chef, Kunden, Termine
   Abzuarbeiten: Tasks, Projekte, Aufgaben
   Werbung: Newsletter, Marketing, Angebote

   Antwort (nur ein Wort):"
   ↓
4. AI antwortet: "wichtig"
   ↓
5. Fleet Navigator sendet Kommando an Email Officer:
   {
     "type": "move_email",
     "messageId": "abc123",
     "folder": "wichtig",
     "priority": "high"
   }
   ↓
6. Email Officer verschiebt Email in "Wichtig"-Ordner
   ↓
7. Desktop-Benachrichtigung:
   "📧 Wichtige Email von chef@firma.de"
```

---

## 📂 Ordnerstruktur (automatisch erstellt)

```
Email-Account (z.B. user@gmail.com)
├── 📥 Inbox (Standard)
├── ✅ Wichtig        ← AI sortiert hier rein
├── 📋 Abzuarbeiten   ← AI sortiert hier rein
├── 🗑️ Werbung        ← AI sortiert hier rein
└── 📁 Archiv
```

**Automatische Erstellung:**
- Email Officer erstellt diese Ordner beim ersten Start
- Nutzer kann in Settings anpassen

---

## 🎨 AI-Klassifizierungs-Logik

### Wichtig ✅
- **Absender:** Chef, Team-Kollegen, Kunden
- **Betreff:** "Dringend", "ASAP", "Wichtig", Namen von Personen
- **Inhalt:** Meeting-Einladungen, Deadlines, Entscheidungen
- **Keywords:** "morgen", "heute", "sofort", "bitte", "deadline"

### Abzuarbeiten 📋
- **Absender:** Projektmanagement-Tools (Jira, Asana, etc.)
- **Betreff:** "Task", "TODO", "Aufgabe", "Projekt"
- **Inhalt:** Tickets, Aufgaben, Projekte, Reviews
- **Keywords:** "erledigen", "bearbeiten", "prüfen", "review"

### Werbung 🗑️
- **Absender:** "no-reply@", "newsletter@", "marketing@"
- **Betreff:** "Angebot", "Sale", "Rabatt", "Newsletter"
- **Inhalt:** Werbung, Marketing, Promotions
- **Keywords:** "jetzt kaufen", "rabatt", "angebot", "kostenlos"

**AI-Prompt Template:**
```
Du bist ein Email-Klassifizierer. Sortiere diese Email in genau eine Kategorie:

Email:
Von: {from}
Betreff: {subject}
Vorschau: {preview}

Kategorien:
1. "wichtig" - Dringende Emails von Chef, Kunden, Team. Meeting-Einladungen, Deadlines.
2. "abzuarbeiten" - Tasks, Aufgaben, Projekte, die erledigt werden müssen.
3. "werbung" - Newsletter, Marketing, Spam, Werbung.

Antwort (nur ein Wort - wichtig, abzuarbeiten oder werbung):
```

---

## 🔧 Backend-Komponenten

### 1. EmailProcessingService.java

```java
@Service
@RequiredArgsConstructor
public class EmailProcessingService {

    private final OllamaService ollamaService;
    private final FleetOfficerWebSocketHandler wsHandler;

    public EmailClassification classifyEmail(EmailMessage email, String model) {
        // Build AI prompt
        String prompt = buildClassificationPrompt(email);

        // Ask AI
        String response = ollamaService.chat(model, prompt, null, null);

        // Parse response
        String category = parseCategory(response); // "wichtig" | "abzuarbeiten" | "werbung"

        return new EmailClassification(
            email.getMessageId(),
            category,
            extractReasoning(response)
        );
    }

    public void processNewEmail(String officerId, EmailMessage email) {
        // Get user's selected model from settings
        String model = settingsService.getEmailModel(); // Default: "llama3.2:3b"

        // Classify
        EmailClassification classification = classifyEmail(email, model);

        // Send move command to Email Officer
        OfficerCommand command = new OfficerCommand();
        command.setType("move_email");
        command.setPayload(Map.of(
            "messageId", email.getMessageId(),
            "folder", classification.getCategory(),
            "reason", classification.getReasoning()
        ));

        wsHandler.sendCommand(officerId, command);

        // If important: send notification
        if (classification.getCategory().equals("wichtig")) {
            sendDesktopNotification(email);
        }
    }
}
```

### 2. Email DTOs

```java
@Data
@Builder
public class EmailMessage {
    private String messageId;
    private String officerId;
    private String accountEmail;
    private String from;
    private String to;
    private String subject;
    private String preview;  // First 200 chars
    private LocalDateTime date;
    private boolean read;
    private boolean flagged;
}

@Data
public class EmailClassification {
    private String messageId;
    private String category;  // "wichtig" | "abzuarbeiten" | "werbung"
    private String reasoning; // AI's explanation
    private double confidence; // 0.0 - 1.0
}
```

### 3. Settings

```java
public class EmailSettings {
    private String model = "llama3.2:3b";  // Default - User kann JEDES Modell wählen
    private boolean autoClassify = true;
    private boolean notifyImportant = true;
    private Map<String, String> folderNames = Map.of(
        "wichtig", "Wichtig",
        "abzuarbeiten", "Abzuarbeiten",
        "werbung", "Werbung"
    );
}

// Frontend lädt verfügbare Modelle dynamisch:
// GET /api/ollama/models → Liste ALLER installierten Modelle
```

---

## 🖥️ Frontend (Email Dashboard)

### Settings Page

```vue
<template>
  <div class="email-settings">
    <h2>📧 Email Officer Einstellungen</h2>

    <!-- Model Selection -->
    <div class="setting-group">
      <label>KI-Modell für Email-Klassifizierung</label>
      <select v-model="settings.model">
        <!-- Dynamisch geladen: ALLE installierten Ollama-Modelle -->
        <option v-for="model in availableModels" :key="model.name" :value="model.name">
          {{ model.name }}
          <span v-if="model.size">({{ formatSize(model.size) }})</span>
        </option>
      </select>
      <p class="text-sm text-gray-500 mt-1">
        Empfohlen: llama3.2:3b (schnell) oder qwen2.5:7b (genauer)
      </p>
    </div>

    <!-- Model Info -->
    <div v-if="selectedModelInfo" class="bg-gray-100 p-3 rounded mt-2">
      <p class="text-xs text-gray-600">
        <strong>{{ selectedModelInfo.name }}</strong><br>
        Größe: {{ formatSize(selectedModelInfo.size) }}<br>
        Empfohlen für: {{ getRecommendation(selectedModelInfo.name) }}
      </p>
    </div>

    <!-- Auto-Classify -->
    <div class="setting-group">
      <label>
        <input type="checkbox" v-model="settings.autoClassify" />
        Emails automatisch sortieren
      </label>
    </div>

    <!-- Notifications -->
    <div class="setting-group">
      <label>
        <input type="checkbox" v-model="settings.notifyImportant" />
        Bei wichtigen Emails benachrichtigen
      </label>
    </div>

    <!-- Folder Names -->
    <div class="setting-group">
      <label>Ordnernamen anpassen</label>
      <input v-model="settings.folderNames.wichtig" placeholder="Wichtig" />
      <input v-model="settings.folderNames.abzuarbeiten" placeholder="Abzuarbeiten" />
      <input v-model="settings.folderNames.werbung" placeholder="Werbung" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

const settings = ref({
  model: 'llama3.2:3b',
  autoClassify: true,
  notifyImportant: true,
  folderNames: {
    wichtig: 'Wichtig',
    abzuarbeiten: 'Abzuarbeiten',
    werbung: 'Werbung'
  }
})

const availableModels = ref([])

onMounted(async () => {
  // Lade ALLE installierten Ollama-Modelle
  const response = await axios.get('/api/ollama/models')
  availableModels.value = response.data
})

const selectedModelInfo = computed(() => {
  return availableModels.value.find(m => m.name === settings.value.model)
})

function formatSize(bytes) {
  if (!bytes) return ''
  const gb = bytes / (1024 ** 3)
  return `${gb.toFixed(1)} GB`
}

function getRecommendation(modelName) {
  if (modelName.includes('3b')) return 'Schnelle Klassifizierung'
  if (modelName.includes('7b')) return 'Hohe Genauigkeit'
  if (modelName.includes('13b')) return 'Sehr präzise (langsamer)'
  return 'Email-Sortierung'
}
</script>
```

### Email Dashboard

```vue
<template>
  <div class="email-dashboard">
    <!-- Stats -->
    <div class="stats-grid">
      <StatCard
        icon="✅"
        title="Wichtig"
        :count="stats.wichtig"
        color="red"
      />
      <StatCard
        icon="📋"
        title="Abzuarbeiten"
        :count="stats.abzuarbeiten"
        color="blue"
      />
      <StatCard
        icon="🗑️"
        title="Werbung"
        :count="stats.werbung"
        color="gray"
      />
    </div>

    <!-- Recent Classifications -->
    <div class="recent-emails">
      <h3>Kürzlich sortiert</h3>
      <div v-for="email in recentClassifications" :key="email.id">
        <EmailClassificationCard :email="email" />
      </div>
    </div>
  </div>
</template>
```

---

## 🔌 Email Officer Implementierungen

### Option 1: Thunderbird Extension

**Vorteile:**
- ✅ Direkter Zugriff auf Thunderbird-Accounts
- ✅ Keine zusätzlichen Credentials nötig
- ✅ Funktioniert mit allen Thunderbird-Accounts

**Nachteile:**
- ⚠️ Nur wenn Thunderbird läuft
- ⚠️ WebExtension API Limitierungen

### Option 2: Outlook Add-in

**Vorteile:**
- ✅ Direkter Zugriff auf Outlook-Accounts
- ✅ Microsoft Graph API für Cloud-Emails
- ✅ Funktioniert in Outlook Desktop + Web

**Nachteile:**
- ⚠️ Nur Microsoft-Accounts
- ⚠️ OAuth2 erforderlich

### Option 3: Standalone IMAP Client (Empfohlen für Freemail!)

**Vorteile:**
- ✅ Funktioniert mit JEDEM IMAP-Account
- ✅ Läuft unabhängig (als Service)
- ✅ Keine Email-Client Installation nötig
- ✅ Unterstützt Gmail, GMX, Web.de, etc.

**Implementierung:**

```java
// Fleet-Email-Officer-IMAP (Java/Go Binary)
public class EmailOfficerIMAP extends FleetOfficer {

    private Store store;
    private Folder inbox;

    public void connect(String host, String user, String password) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", "993");

        Session session = Session.getInstance(props);
        store = session.getStore("imaps");
        store.connect(host, user, password);

        inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
    }

    public void monitorInbox() {
        // Poll for new emails every 60 seconds
        while (true) {
            Message[] messages = inbox.getMessages();

            for (Message msg : messages) {
                if (!msg.isSet(Flags.Flag.SEEN)) {
                    // New unread email!
                    EmailMessage email = convertToEmailMessage(msg);
                    sendToNavigator(email);
                }
            }

            Thread.sleep(60000); // 60 seconds
        }
    }

    public void moveEmail(String messageId, String folderName) {
        Message msg = findMessageById(messageId);
        Folder targetFolder = store.getFolder(folderName);

        if (!targetFolder.exists()) {
            targetFolder.create(Folder.HOLDS_MESSAGES);
        }

        inbox.copyMessages(new Message[]{msg}, targetFolder);
        msg.setFlag(Flags.Flag.DELETED, true);
        inbox.expunge();
    }
}
```

---

## 🚀 Entwicklungsplan (Überarbeitet)

### Phase 1: IMAP Email Officer (MVP)

**Woche 1:**
- [ ] Java IMAP Client (JavaMail API)
- [ ] Fleet Officer Integration (WebSocket)
- [ ] Email-Abruf (IMAP)
- [ ] Ordner-Verwaltung (create, move)

**Woche 2:**
- [ ] Backend: EmailProcessingService
- [ ] AI-Klassifizierung (Ollama Integration)
- [ ] Move-Command Handling
- [ ] Desktop-Benachrichtigungen

**Woche 3:**
- [ ] Frontend: Email Dashboard
- [ ] Settings Page (Model-Auswahl)
- [ ] Statistics View
- [ ] Testing

**Deliverables:**
- Fleet-Email-Officer-IMAP (JAR)
- Fleet Navigator v0.3.0 mit Email Support

---

### Phase 2: Thunderbird Extension

**Woche 4-5:**
- [ ] WebExtension Grundstruktur
- [ ] Shared Fleet Client Library
- [ ] Email Monitoring
- [ ] Folder Operations

---

### Phase 3: Outlook Add-in

**Woche 6-7:**
- [ ] Office.js Add-in
- [ ] Microsoft Graph Integration
- [ ] Shared Fleet Client Library
- [ ] Task Pane UI

---

## 🎯 User Stories

### Story 1: Auto-Sortierung
```
Als Nutzer
möchte ich, dass wichtige Emails automatisch sortiert werden
damit ich sofort sehe, was dringend ist.

Akzeptanzkriterien:
✅ Email von Chef → "Wichtig"-Ordner
✅ Newsletter → "Werbung"-Ordner
✅ Jira-Ticket → "Abzuarbeiten"-Ordner
✅ Desktop-Benachrichtigung bei wichtigen Emails
```

### Story 2: Model-Auswahl
```
Als Nutzer
möchte ich das KI-Modell selbst wählen
damit ich die beste Balance zwischen Geschwindigkeit und Genauigkeit finde.

Akzeptanzkriterien:
✅ Dropdown mit allen verfügbaren Modellen
✅ Modell wird in Settings gespeichert
✅ Wechsel wirkt sofort
```

### Story 3: Freemail Integration
```
Als Nutzer
möchte ich meinen Gmail/GMX-Account verbinden
ohne Thunderbird oder Outlook installieren zu müssen.

Akzeptanzkriterien:
✅ IMAP/SMTP Credentials eingeben
✅ Verbindung testen
✅ Emails werden abgerufen
✅ Sortierung funktioniert
```

---

## 🔒 Sicherheit

### Credentials-Speicherung:
- ✅ Verschlüsselt in lokaler Datenbank (H2)
- ✅ AES-256 Encryption
- ✅ Master-Passwort (optional)

### Email-Inhalte:
- ✅ Nur Metadaten + Preview (200 Zeichen) an AI
- ✅ Vollständiger Email-Body bleibt lokal
- ✅ Anhänge werden NICHT verarbeitet

---

## 📊 Metriken

### Dashboard zeigt:
- 📧 Emails gesamt (letzten 30 Tage)
- ✅ Sortiert als "Wichtig"
- 📋 Sortiert als "Abzuarbeiten"
- 🗑️ Sortiert als "Werbung"
- 🎯 AI-Genauigkeit (User-Feedback)
- ⚡ Durchschnittliche Verarbeitungszeit

---

**Entwickelt von:** JavaFleet Systems Consulting
**Port 2025:** Das Geburtsjahr von Fleet Navigator 🚢

**Status:** 📝 Überarbeitetes Konzept - Bereit für Implementierung
**Nächster Schritt:** IMAP Email Officer (standalone Java Binary)
