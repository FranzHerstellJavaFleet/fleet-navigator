# Fleet Email Officer - Intelligente Email-Verarbeitung

## 🎯 Vision

Ein **Fleet Officer für Email-Programme** (Thunderbird & Outlook), der eingehende Emails analysiert, kategorisiert und automatisch verarbeitet - gesteuert durch KI-Modelle vom Fleet Navigator Server.

---

## 🏗️ Architektur-Überblick

```
┌─────────────────────────────────────────────────────────────┐
│                    Fleet Navigator Server                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Spring Boot Backend (Port 2025)                    │    │
│  │  • EmailTaskController (REST API)                   │    │
│  │  • EmailTaskService                                 │    │
│  │  • OllamaService (KI-Analyse)                       │    │
│  │  • EmailRuleEngine                                  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                          ↕ REST/JSON
┌─────────────────────────────────────────────────────────────┐
│                      Email Clients                           │
│  ┌────────────────────┐         ┌──────────────────────┐   │
│  │  Thunderbird       │         │  Outlook             │   │
│  │  ┌──────────────┐  │         │  ┌────────────────┐ │   │
│  │  │ WebExtension │  │         │  │  Office Add-In │ │   │
│  │  │ (JavaScript) │  │         │  │  (JavaScript)  │ │   │
│  │  └──────────────┘  │         │  └────────────────┘ │   │
│  └────────────────────┘         └──────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          ↕ IMAP/POP3
┌─────────────────────────────────────────────────────────────┐
│                      Mail Server                             │
│  • Gmail, Outlook.com, eigener Server, etc.                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Komponenten

### 1. **Thunderbird WebExtension**

#### Manifest (manifest.json)

```json
{
  "manifest_version": 3,
  "name": "Fleet Email Officer",
  "version": "1.0.0",
  "description": "KI-gestützte Email-Verarbeitung mit Fleet Navigator",
  "permissions": [
    "messagesRead",
    "messagesMove",
    "messagesTags",
    "storage",
    "notifications"
  ],
  "background": {
    "scripts": ["background.js"]
  },
  "browser_action": {
    "default_popup": "popup.html",
    "default_icon": "icons/icon-48.png"
  },
  "icons": {
    "48": "icons/icon-48.png",
    "96": "icons/icon-96.png"
  }
}
```

#### Background Script (background.js)

```javascript
// Thunderbird WebExtension - Fleet Email Officer
const FLEET_SERVER = 'http://localhost:2025'
let apiKey = null
let isEnabled = false

// Beim Start Extension laden
browser.storage.local.get(['apiKey', 'isEnabled']).then(data => {
  apiKey = data.apiKey
  isEnabled = data.isEnabled || false
  if (isEnabled) {
    console.log('🚢 Fleet Email Officer aktiviert')
    startMonitoring()
  }
})

// Neue Emails überwachen
function startMonitoring() {
  browser.messages.onNewMailReceived.addListener(async (folder, messages) => {
    console.log(`📧 Neue Email(s) in ${folder.name}: ${messages.messages.length}`)

    for (const messageHeader of messages.messages) {
      await processEmail(messageHeader)
    }
  })
}

// Email an Fleet Navigator senden
async function processEmail(messageHeader) {
  try {
    // Vollständige Email-Daten abrufen
    const fullMessage = await browser.messages.getFull(messageHeader.id)

    const emailData = {
      id: messageHeader.id,
      subject: messageHeader.subject,
      from: messageHeader.author,
      date: messageHeader.date,
      body: extractBody(fullMessage),
      attachments: messageHeader.attachments || []
    }

    // An Fleet Navigator Server senden
    const response = await fetch(`${FLEET_SERVER}/api/email/analyze`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': apiKey
      },
      body: JSON.stringify(emailData)
    })

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`)
    }

    const result = await response.json()
    console.log('🤖 KI-Analyse:', result)

    // Aktionen ausführen basierend auf KI-Entscheidung
    await executeActions(messageHeader, result)

  } catch (error) {
    console.error('❌ Email-Verarbeitung fehlgeschlagen:', error)
    showNotification('Fehler', `Email konnte nicht verarbeitet werden: ${error.message}`)
  }
}

// Email-Body extrahieren (plain text bevorzugt)
function extractBody(fullMessage) {
  if (fullMessage.parts) {
    for (const part of fullMessage.parts) {
      if (part.contentType === 'text/plain') {
        return part.body
      }
    }
    // Fallback: HTML
    for (const part of fullMessage.parts) {
      if (part.contentType === 'text/html') {
        return stripHtml(part.body)
      }
    }
  }
  return ''
}

// Aktionen ausführen (Verschieben, Taggen, Benachrichtigen)
async function executeActions(messageHeader, aiResult) {
  const { category, priority, suggestedFolder, tags, shouldNotify } = aiResult

  // Tags hinzufügen
  if (tags && tags.length > 0) {
    for (const tag of tags) {
      await browser.messages.addTag(messageHeader.id, tag)
    }
  }

  // In Ordner verschieben
  if (suggestedFolder) {
    const folders = await browser.folders.query({ name: suggestedFolder })
    if (folders.length > 0) {
      await browser.messages.move([messageHeader.id], folders[0])
      console.log(`📁 Verschoben nach: ${suggestedFolder}`)
    }
  }

  // Benachrichtigung bei wichtigen Emails
  if (shouldNotify) {
    showNotification(
      `Wichtige Email: ${category}`,
      `Von: ${messageHeader.author}\nBetreff: ${messageHeader.subject}`
    )
  }
}

// Notification anzeigen
function showNotification(title, message) {
  browser.notifications.create({
    type: 'basic',
    iconUrl: 'icons/icon-48.png',
    title: title,
    message: message
  })
}

// HTML-Tags entfernen
function stripHtml(html) {
  return html.replace(/<[^>]*>/g, '').trim()
}

// API Key und Status von Popup erhalten
browser.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'updateSettings') {
    apiKey = message.apiKey
    isEnabled = message.isEnabled

    if (isEnabled) {
      startMonitoring()
    }

    sendResponse({ success: true })
  }
})
```

#### Popup UI (popup.html)

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Fleet Email Officer</title>
  <style>
    body {
      width: 350px;
      padding: 15px;
      font-family: Arial, sans-serif;
    }
    h2 {
      margin-top: 0;
      color: #7c3aed;
    }
    .setting {
      margin-bottom: 15px;
    }
    label {
      display: block;
      margin-bottom: 5px;
      font-weight: bold;
    }
    input[type="text"] {
      width: 100%;
      padding: 8px;
      border: 1px solid #ccc;
      border-radius: 4px;
    }
    button {
      background: #7c3aed;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 4px;
      cursor: pointer;
    }
    button:hover {
      background: #6d28d9;
    }
    .status {
      margin-top: 15px;
      padding: 10px;
      border-radius: 4px;
      background: #f3f4f6;
    }
    .status.active {
      background: #d1fae5;
      color: #065f46;
    }
  </style>
</head>
<body>
  <h2>⚓ Fleet Email Officer</h2>

  <div class="setting">
    <label for="serverUrl">Fleet Navigator Server:</label>
    <input type="text" id="serverUrl" value="http://localhost:2025">
  </div>

  <div class="setting">
    <label for="apiKey">API Key:</label>
    <input type="text" id="apiKey" placeholder="Ihr API-Schlüssel">
  </div>

  <div class="setting">
    <label>
      <input type="checkbox" id="enabled"> Automatische Verarbeitung aktivieren
    </label>
  </div>

  <button id="saveBtn">Speichern</button>
  <button id="testBtn">Verbindung testen</button>

  <div id="status" class="status">
    Status: Nicht verbunden
  </div>

  <script src="popup.js"></script>
</body>
</html>
```

#### Popup Script (popup.js)

```javascript
// Einstellungen laden
browser.storage.local.get(['apiKey', 'isEnabled', 'serverUrl']).then(data => {
  document.getElementById('apiKey').value = data.apiKey || ''
  document.getElementById('enabled').checked = data.isEnabled || false
  document.getElementById('serverUrl').value = data.serverUrl || 'http://localhost:2025'

  updateStatus(data.isEnabled)
})

// Speichern Button
document.getElementById('saveBtn').addEventListener('click', async () => {
  const apiKey = document.getElementById('apiKey').value
  const isEnabled = document.getElementById('enabled').checked
  const serverUrl = document.getElementById('serverUrl').value

  await browser.storage.local.set({ apiKey, isEnabled, serverUrl })

  // Background Script benachrichtigen
  browser.runtime.sendMessage({
    action: 'updateSettings',
    apiKey,
    isEnabled
  })

  updateStatus(isEnabled)
  showMessage('Einstellungen gespeichert!')
})

// Test Button
document.getElementById('testBtn').addEventListener('click', async () => {
  const serverUrl = document.getElementById('serverUrl').value
  const apiKey = document.getElementById('apiKey').value

  try {
    const response = await fetch(`${serverUrl}/api/email/ping`, {
      headers: { 'X-API-Key': apiKey }
    })

    if (response.ok) {
      showMessage('✅ Verbindung erfolgreich!')
    } else {
      showMessage('❌ Verbindung fehlgeschlagen (Status: ' + response.status + ')')
    }
  } catch (error) {
    showMessage('❌ Server nicht erreichbar: ' + error.message)
  }
})

function updateStatus(isEnabled) {
  const statusDiv = document.getElementById('status')
  if (isEnabled) {
    statusDiv.className = 'status active'
    statusDiv.textContent = '✅ Aktiv - Emails werden analysiert'
  } else {
    statusDiv.className = 'status'
    statusDiv.textContent = '⏸️ Pausiert'
  }
}

function showMessage(msg) {
  alert(msg)
}
```

---

### 2. **Outlook Add-In**

#### Manifest (manifest.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<OfficeApp
  xmlns="http://schemas.microsoft.com/office/appforoffice/1.1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:type="MailApp">

  <Id>fleet-email-officer-v1</Id>
  <Version>1.0.0.0</Version>
  <ProviderName>JavaFleet Systems</ProviderName>
  <DefaultLocale>de-DE</DefaultLocale>
  <DisplayName DefaultValue="Fleet Email Officer"/>
  <Description DefaultValue="KI-gestützte Email-Verarbeitung"/>

  <IconUrl DefaultValue="https://your-domain.com/icons/icon-64.png"/>
  <HighResolutionIconUrl DefaultValue="https://your-domain.com/icons/icon-128.png"/>

  <Hosts>
    <Host Name="Mailbox"/>
  </Hosts>

  <Requirements>
    <Sets>
      <Set Name="Mailbox" MinVersion="1.1"/>
    </Sets>
  </Requirements>

  <FormSettings>
    <Form xsi:type="ItemRead">
      <DesktopSettings>
        <SourceLocation DefaultValue="https://your-domain.com/taskpane.html"/>
        <RequestedHeight>250</RequestedHeight>
      </DesktopSettings>
    </Form>
  </FormSettings>

  <Permissions>ReadWriteMailbox</Permissions>

  <Rule xsi:type="RuleCollection" Mode="Or">
    <Rule xsi:type="ItemIs" ItemType="Message" FormType="Read"/>
  </Rule>

  <VersionOverrides xmlns="http://schemas.microsoft.com/office/mailappversionoverrides" xsi:type="VersionOverridesV1_0">
    <Requirements>
      <bt:Sets DefaultMinVersion="1.3">
        <bt:Set Name="Mailbox"/>
      </bt:Sets>
    </Requirements>

    <Hosts>
      <Host xsi:type="MailHost">
        <DesktopFormFactor>
          <FunctionFile resid="functionFile"/>

          <ExtensionPoint xsi:type="MessageReadCommandSurface">
            <OfficeTab id="TabDefault">
              <Group id="fleetGroup">
                <Label resid="fleetGroupLabel"/>

                <Control xsi:type="Button" id="analyzeBtn">
                  <Label resid="analyzeBtnLabel"/>
                  <Supertip>
                    <Title resid="analyzeBtnTitle"/>
                    <Description resid="analyzeBtnDesc"/>
                  </Supertip>
                  <Icon>
                    <bt:Image size="16" resid="icon16"/>
                    <bt:Image size="32" resid="icon32"/>
                    <bt:Image size="80" resid="icon80"/>
                  </Icon>
                  <Action xsi:type="ExecuteFunction">
                    <FunctionName>analyzeEmail</FunctionName>
                  </Action>
                </Control>
              </Group>
            </OfficeTab>
          </ExtensionPoint>
        </DesktopFormFactor>
      </Host>
    </Hosts>

    <Resources>
      <bt:Images>
        <bt:Image id="icon16" DefaultValue="https://your-domain.com/icons/icon-16.png"/>
        <bt:Image id="icon32" DefaultValue="https://your-domain.com/icons/icon-32.png"/>
        <bt:Image id="icon80" DefaultValue="https://your-domain.com/icons/icon-80.png"/>
      </bt:Images>
      <bt:Urls>
        <bt:Url id="functionFile" DefaultValue="https://your-domain.com/functions.html"/>
      </bt:Urls>
      <bt:ShortStrings>
        <bt:String id="fleetGroupLabel" DefaultValue="Fleet Officer"/>
        <bt:String id="analyzeBtnLabel" DefaultValue="Analysieren"/>
        <bt:String id="analyzeBtnTitle" DefaultValue="Email analysieren"/>
      </bt:ShortStrings>
      <bt:LongStrings>
        <bt:String id="analyzeBtnDesc" DefaultValue="Sendet Email zur KI-Analyse an Fleet Navigator"/>
      </bt:LongStrings>
    </Resources>
  </VersionOverrides>
</OfficeApp>
```

#### Functions (functions.js)

```javascript
// Outlook Add-In - Fleet Email Officer
const FLEET_SERVER = 'http://localhost:2025'

Office.initialize = function() {
  console.log('🚢 Fleet Email Officer geladen')
}

// Email analysieren (Button-Klick)
function analyzeEmail(event) {
  const item = Office.context.mailbox.item

  // Email-Daten sammeln
  const emailData = {
    id: item.itemId,
    subject: item.subject,
    from: item.from.emailAddress,
    date: item.dateTimeCreated,
    body: null,
    attachments: item.attachments.map(a => ({
      name: a.name,
      size: a.size,
      contentType: a.contentType
    }))
  }

  // Body asynchron laden
  item.body.getAsync(Office.CoercionType.Text, async (result) => {
    if (result.status === Office.AsyncResultStatus.Succeeded) {
      emailData.body = result.value

      try {
        // An Fleet Navigator senden
        const response = await fetch(`${FLEET_SERVER}/api/email/analyze`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-API-Key': getApiKey()
          },
          body: JSON.stringify(emailData)
        })

        if (!response.ok) {
          throw new Error(`Server error: ${response.status}`)
        }

        const aiResult = await response.json()
        console.log('🤖 KI-Analyse:', aiResult)

        // Kategorien als Kategorien setzen
        if (aiResult.tags && aiResult.tags.length > 0) {
          item.categories.addAsync(aiResult.tags, (catResult) => {
            if (catResult.status === Office.AsyncResultStatus.Succeeded) {
              showNotification('Analysiert!', `Kategorie: ${aiResult.category}`)
            }
          })
        }

        // Priorität setzen
        if (aiResult.priority === 'HIGH') {
          item.importance.setAsync(Office.MailboxEnums.Importance.High)
        }

        event.completed({ allowEvent: true })

      } catch (error) {
        console.error('❌ Analyse fehlgeschlagen:', error)
        showNotification('Fehler', error.message)
        event.completed({ allowEvent: false })
      }
    } else {
      console.error('❌ Body konnte nicht geladen werden')
      event.completed({ allowEvent: false })
    }
  })
}

// Notification anzeigen
function showNotification(title, message) {
  Office.context.mailbox.item.notificationMessages.addAsync('fleet-notification', {
    type: 'informationalMessage',
    message: `${title}: ${message}`,
    icon: 'icon-16',
    persistent: false
  })
}

// API Key aus Storage laden
function getApiKey() {
  // In Produktion: Office.context.roamingSettings
  return localStorage.getItem('fleetApiKey') || ''
}
```

---

### 3. **Spring Boot Backend (Fleet Navigator Server)**

#### EmailTaskController.java

```java
package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.EmailAnalysisRequest;
import io.javafleet.fleetnavigator.dto.EmailAnalysisResponse;
import io.javafleet.fleetnavigator.service.EmailTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API für Email Officers (Thunderbird & Outlook Extensions)
 */
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmailTaskController {

    private final EmailTaskService emailTaskService;

    /**
     * Email analysieren und Aktionsempfehlungen zurückgeben
     */
    @PostMapping("/analyze")
    public ResponseEntity<EmailAnalysisResponse> analyzeEmail(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody EmailAnalysisRequest request) {

        log.info("📧 Email-Analyse: {} von {}", request.getSubject(), request.getFrom());

        // API-Key validieren (optional, für Sicherheit)
        if (apiKey == null || !emailTaskService.isValidApiKey(apiKey)) {
            log.warn("⚠️ Ungültiger API-Key: {}", apiKey);
            return ResponseEntity.status(401).build();
        }

        EmailAnalysisResponse response = emailTaskService.analyzeEmail(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Ping-Endpoint für Verbindungstest
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        if (apiKey == null || !emailTaskService.isValidApiKey(apiKey)) {
            return ResponseEntity.status(401).body("Ungültiger API-Key");
        }

        return ResponseEntity.ok("🚢 Fleet Navigator Server online!");
    }
}
```

#### EmailAnalysisRequest.java

```java
package io.javafleet.fleetnavigator.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmailAnalysisRequest {
    private String id;
    private String subject;
    private String from;
    private LocalDateTime date;
    private String body;
    private List<EmailAttachment> attachments;

    @Data
    public static class EmailAttachment {
        private String name;
        private Long size;
        private String contentType;
    }
}
```

#### EmailAnalysisResponse.java

```java
package io.javafleet.fleetnavigator.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class EmailAnalysisResponse {
    private String category;           // z.B. "Rechnung", "Bewerbung", "Newsletter"
    private String priority;            // "LOW", "MEDIUM", "HIGH"
    private String suggestedFolder;     // Ordner-Vorschlag
    private List<String> tags;          // Tags für Kategorisierung
    private boolean shouldNotify;       // Benachrichtigung senden?
    private String summary;             // KI-generierte Zusammenfassung
    private List<String> suggestedActions; // z.B. ["Rechnung bezahlen", "Termin eintragen"]
}
```

#### EmailTaskService.java

```java
package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.EmailAnalysisRequest;
import io.javafleet.fleetnavigator.dto.EmailAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service für Email-Analyse mit KI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTaskService {

    private final OllamaService ollamaService;

    // Einfache API-Key Validierung (in Produktion: DB-gestützt)
    private static final Set<String> VALID_API_KEYS = Set.of(
            "fleet-demo-key-12345",
            "thunderbird-extension-key",
            "outlook-addon-key"
    );

    /**
     * API-Key validieren
     */
    public boolean isValidApiKey(String apiKey) {
        return VALID_API_KEYS.contains(apiKey);
    }

    /**
     * Email analysieren mit KI
     */
    public EmailAnalysisResponse analyzeEmail(EmailAnalysisRequest request) {
        log.info("🤖 Starte KI-Analyse für: {}", request.getSubject());

        // Prompt für Ollama zusammenstellen
        String prompt = buildAnalysisPrompt(request);

        // KI-Modell aufrufen
        String aiResponse = ollamaService.generateSimple(prompt);

        // Response parsen und strukturieren
        return parseAiResponse(aiResponse, request);
    }

    /**
     * Prompt für KI-Analyse erstellen
     */
    private String buildAnalysisPrompt(EmailAnalysisRequest email) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analysiere folgende Email und kategorisiere sie:\n\n");
        prompt.append("Betreff: ").append(email.getSubject()).append("\n");
        prompt.append("Von: ").append(email.getFrom()).append("\n");
        prompt.append("Text:\n").append(email.getBody()).append("\n\n");

        if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
            prompt.append("Anhänge:\n");
            for (var att : email.getAttachments()) {
                prompt.append("- ").append(att.getName()).append(" (").append(att.getContentType()).append(")\n");
            }
            prompt.append("\n");
        }

        prompt.append("Gib eine Analyse im folgenden Format zurück:\n");
        prompt.append("KATEGORIE: [Rechnung/Bewerbung/Newsletter/Privat/Geschäftlich/Spam/...]\n");
        prompt.append("PRIORITÄT: [LOW/MEDIUM/HIGH]\n");
        prompt.append("ORDNER: [Vorgeschlagener Ordner-Name]\n");
        prompt.append("TAGS: [Tag1, Tag2, Tag3]\n");
        prompt.append("BENACHRICHTIGUNG: [JA/NEIN]\n");
        prompt.append("ZUSAMMENFASSUNG: [Kurze Zusammenfassung in 1-2 Sätzen]\n");
        prompt.append("AKTIONEN: [Vorgeschlagene Aktion 1; Aktion 2; ...]\n");

        return prompt.toString();
    }

    /**
     * KI-Response in strukturierte Response umwandeln
     */
    private EmailAnalysisResponse parseAiResponse(String aiResponse, EmailAnalysisRequest request) {
        // Einfaches Parsing (in Produktion: robuster Parser oder JSON-Output)
        String category = extractValue(aiResponse, "KATEGORIE:");
        String priority = extractValue(aiResponse, "PRIORITÄT:");
        String folder = extractValue(aiResponse, "ORDNER:");
        String tagsStr = extractValue(aiResponse, "TAGS:");
        String notifyStr = extractValue(aiResponse, "BENACHRICHTIGUNG:");
        String summary = extractValue(aiResponse, "ZUSAMMENFASSUNG:");
        String actionsStr = extractValue(aiResponse, "AKTIONEN:");

        List<String> tags = parseTags(tagsStr);
        List<String> actions = parseActions(actionsStr);
        boolean shouldNotify = "JA".equalsIgnoreCase(notifyStr.trim());

        return EmailAnalysisResponse.builder()
                .category(category)
                .priority(priority.toUpperCase())
                .suggestedFolder(folder)
                .tags(tags)
                .shouldNotify(shouldNotify)
                .summary(summary)
                .suggestedActions(actions)
                .build();
    }

    private String extractValue(String text, String key) {
        int start = text.indexOf(key);
        if (start == -1) return "";

        start += key.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.length();

        return text.substring(start, end).trim();
    }

    private List<String> parseTags(String tagsStr) {
        List<String> tags = new ArrayList<>();
        if (tagsStr != null && !tagsStr.isEmpty()) {
            String[] parts = tagsStr.split(",");
            for (String part : parts) {
                String tag = part.trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private List<String> parseActions(String actionsStr) {
        List<String> actions = new ArrayList<>();
        if (actionsStr != null && !actionsStr.isEmpty()) {
            String[] parts = actionsStr.split(";");
            for (String part : parts) {
                String action = part.trim();
                if (!action.isEmpty()) {
                    actions.add(action);
                }
            }
        }
        return actions;
    }
}
```

---

## 🎯 Use Cases

### 1. **Rechnungsverarbeitung**

**Scenario:** Eine PDF-Rechnung kommt per Email.

**KI-Analyse:**
- **Kategorie:** "Rechnung"
- **Priorität:** HIGH
- **Ordner:** "Finanzen/Rechnungen"
- **Tags:** ["Rechnung", "Fällig", "2025"]
- **Benachrichtigung:** JA
- **Zusammenfassung:** "Rechnung von Firma XY über 1.250€, fällig bis 15.02.2025"
- **Aktionen:** ["Rechnung in Buchhaltungssoftware importieren", "Zahlung einleiten"]

**Extension-Verhalten:**
- Email wird automatisch in Ordner "Rechnungen" verschoben
- Tags werden gesetzt
- Desktop-Benachrichtigung: "⚠️ Neue Rechnung eingegangen!"

---

### 2. **Bewerbungseingang (Recruiter)**

**Scenario:** Bewerbung mit Lebenslauf und Anschreiben.

**KI-Analyse:**
- **Kategorie:** "Bewerbung"
- **Priorität:** MEDIUM
- **Ordner:** "HR/Bewerbungen/Entwickler"
- **Tags:** ["Bewerbung", "Java", "Senior"]
- **Benachrichtigung:** JA
- **Zusammenfassung:** "Bewerbung von Max Mustermann für Senior Java Developer Position"
- **Aktionen:** ["Kandidat in ATS eintragen", "Erstgespräch planen"]

---

### 3. **Newsletter (Low Priority)**

**Scenario:** Wöchentlicher Tech-Newsletter.

**KI-Analyse:**
- **Kategorie:** "Newsletter"
- **Priorität:** LOW
- **Ordner:** "Newsletter/Tech"
- **Tags:** ["Newsletter", "Tech"]
- **Benachrichtigung:** NEIN
- **Zusammenfassung:** "Wöchentlicher Newsletter mit neuen Java-Features und Spring Boot Updates"
- **Aktionen:** []

**Extension-Verhalten:**
- Email wird still verschoben, keine Benachrichtigung

---

### 4. **Terminanfrage**

**Scenario:** Email mit Meeting-Vorschlag.

**KI-Analyse:**
- **Kategorie:** "Termin"
- **Priorität:** HIGH
- **Ordner:** "Termine"
- **Tags:** ["Meeting", "Q1-2025"]
- **Benachrichtigung:** JA
- **Zusammenfassung:** "Meeting-Anfrage für Projekt-Kickoff am 20.02.2025, 14:00 Uhr"
- **Aktionen:** ["Kalender-Eintrag erstellen", "Zusage senden"]

---

## 🔐 Sicherheit

### API-Key Authentifizierung

Jede Extension benötigt einen gültigen API-Key:

```java
// In Produktion: API-Keys in DB mit User-Zuordnung
@Entity
public class ApiKey {
    @Id
    private String key;
    private String owner;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean active;
}
```

### Email-Daten

- **Keine Speicherung:** Emails werden nur analysiert, nicht gespeichert
- **TLS-Verschlüsselung:** API-Calls über HTTPS
- **Lokale Verarbeitung:** Emails bleiben auf dem Client (Thunderbird/Outlook)

### Berechtigungen

**Thunderbird:**
- `messagesRead` - Email lesen
- `messagesMove` - Email verschieben
- `messagesTags` - Tags setzen

**Outlook:**
- `ReadWriteMailbox` - Email lesen und kategorisieren

---

## 📱 Installation

### Thunderbird Extension

1. Extension als `.xpi` Datei packen:
   ```bash
   zip -r fleet-email-officer.xpi manifest.json background.js popup.html popup.js icons/
   ```

2. In Thunderbird installieren:
   - Menü → Add-ons → Zahnrad → "Add-on aus Datei installieren"
   - `fleet-email-officer.xpi` auswählen

3. Konfigurieren:
   - Extension-Icon klicken
   - Server-URL und API-Key eingeben
   - "Automatische Verarbeitung aktivieren"

### Outlook Add-In

1. Manifest auf Webserver hosten (HTTPS erforderlich):
   ```
   https://your-domain.com/manifest.xml
   ```

2. In Outlook installieren:
   - Datei → Informationen → Add-Ins verwalten
   - "+ Aus URL hinzufügen"
   - Manifest-URL eingeben

3. Oder: Über Microsoft AppSource veröffentlichen

---

## 🚀 Implementierungs-Phasen

### Phase 1: Backend (Fleet Navigator Server)
**Dauer:** 2-3 Tage

- [ ] `EmailTaskController` erstellen
- [ ] `EmailTaskService` mit Ollama-Integration
- [ ] DTOs (`EmailAnalysisRequest`, `EmailAnalysisResponse`)
- [ ] API-Key Verwaltung
- [ ] Prompt Engineering für Email-Analyse
- [ ] Tests schreiben

### Phase 2: Thunderbird Extension
**Dauer:** 3-4 Tage

- [ ] Manifest und Permissions definieren
- [ ] `background.js` - Email-Monitoring
- [ ] API-Integration mit Fleet Navigator
- [ ] Popup UI für Einstellungen
- [ ] Aktionen (Verschieben, Taggen, Benachrichtigen)
- [ ] Error Handling
- [ ] Extension packen und testen

### Phase 3: Outlook Add-In
**Dauer:** 4-5 Tage

- [ ] Manifest.xml erstellen
- [ ] Office.js Integration
- [ ] Button in Ribbon integrieren
- [ ] API-Calls implementieren
- [ ] Kategorien und Prioritäten setzen
- [ ] Web-Hosting für Manifest und Scripts
- [ ] Sideloading testen

### Phase 4: KI-Optimierung
**Dauer:** 2-3 Tage

- [ ] Prompt-Templates für verschiedene Email-Typen
- [ ] Lernende Regeln (User-Feedback)
- [ ] Attachment-Analyse (PDFs, Bilder)
- [ ] Multi-Language Support
- [ ] Performance-Optimierung (Caching)

### Phase 5: Erweiterte Features
**Dauer:** 1 Woche

- [ ] **Auto-Reply Suggestions:** KI schreibt Antwort-Vorschläge
- [ ] **Email-Chaining:** Zusammenhängende Emails erkennen
- [ ] **Smart Folders:** Dynamische Ordner basierend auf KI-Kategorien
- [ ] **Calendar Integration:** Termine automatisch eintragen
- [ ] **CRM Integration:** Kontakte und Leads synchronisieren
- [ ] **Analytics Dashboard:** Email-Statistiken im Fleet Navigator

---

## 🔧 Technologie-Stack

| Komponente | Technologie |
|------------|-------------|
| **Thunderbird Extension** | JavaScript (WebExtensions API) |
| **Outlook Add-In** | JavaScript (Office.js) |
| **Backend Server** | Spring Boot 3.2.0 (Java 17) |
| **KI-Modell** | Ollama (lokal) |
| **API** | REST/JSON |
| **Authentifizierung** | API Keys |
| **Hosting (Outlook)** | HTTPS Webserver |

---

## 🎓 Lernressourcen

### Thunderbird Development
- [Thunderbird WebExtension API](https://webextension-api.thunderbird.net/)
- [MDN Web Extensions](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions)

### Outlook Development
- [Office Add-ins Documentation](https://learn.microsoft.com/en-us/office/dev/add-ins/)
- [Outlook Add-in API](https://learn.microsoft.com/en-us/javascript/api/outlook)

### Fleet Navigator Integration
- Spring Boot REST API Development
- Ollama API Integration
- Prompt Engineering für Email-Analyse

---

## 💡 Erweiterte Ideen

### 1. **Multi-Account Support**
- Mehrere Email-Accounts gleichzeitig überwachen
- Pro Account eigene Regeln und Ordnerstrukturen

### 2. **Smart Replies**
- KI generiert 3 Antwortvorschläge (kurz/mittel/lang)
- One-Click Antworten

### 3. **Email Summaries**
- Tägliche Zusammenfassung: "5 wichtige Emails, 12 Newsletter, 3 Spam-Mails erkannt"
- Weekly Digest mit Statistiken

### 4. **Attachment Intelligence**
- PDF-Rechnungen → Beträge extrahieren
- Verträge → Kündigungsfristen erkennen
- Bilder → Objekte erkennen

### 5. **Workflow Automation**
- "Wenn Rechnung → Erstelle Eintrag in Excel"
- "Wenn Bewerbung → Sende Auto-Reply"
- "Wenn Spam > 90% Confidence → Sofort löschen"

---

## 📊 Erfolgsmetriken

Nach 3 Monaten Nutzung:

- **Zeit gespart:** Durchschnittlich 15min/Tag pro User
- **Auto-Kategorisierung:** 85% Genauigkeit
- **False Positives:** < 5%
- **User Satisfaction:** > 4.5/5 Sterne

---

**Erstellt von:** JavaFleet Systems Consulting
**Version:** 1.0.0
**Stand:** Januar 2025
**Ziel:** Intelligente Email-Verarbeitung mit KI-Power! 📧🤖
