# AI-gestützte Log-Analyse - Implementierungs-TODO

**Status:** Backend Foundation implementiert (60%), Rest folgt

---

## ✅ Bereits implementiert:

### Backend (Java/Spring Boot):
1. **LogAnalysisRequest.java** - DTO für Analyse-Anfragen
2. **LogAnalysisService.java** - Vollständiger Service mit Ollama Integration
3. **FleetOfficerController.java** - Endpoints:
   - `POST /api/fleet-officer/officers/{officerId}/analyze-log`
   - `GET /api/fleet-officer/stream/{sessionId}` (SSE)
4. **FleetOfficerService.java** - Methoden für Pending Analysis Management

---

## 🔄 Noch zu implementieren:

### 1. WebSocket Handler erweitern (Java)

**Datei:** `src/main/java/io/javafleet/fleetnavigator/websocket/FleetOfficerWebSocketHandler.java`

**Zu tun:**
```java
// In handleOfficerMessage() hinzufügen:
private void handleOfficerMessage(WebSocketSession session, OfficerMessage message) {
    switch (message.getType()) {
        // ... existing cases ...

        case "log_data":
            handleLogData(message);
            break;
        case "log_complete":
            handleLogComplete(message);
            break;
    }
}

private void handleLogData(OfficerMessage message) {
    String sessionId = (String) message.getData().get("sessionId");
    String chunk = (String) message.getData().get("chunk");

    fleetOfficerService.appendLogData(sessionId, chunk);
    log.debug("Received log chunk for session: {}, size: {}", sessionId, chunk.length());
}

private void handleLogComplete(OfficerMessage message) {
    String sessionId = (String) message.getData().get("sessionId");

    // Get complete log data
    String logContent = fleetOfficerService.getLogData(sessionId);
    LogAnalysisRequest request = fleetOfficerService.getPendingAnalysis(sessionId);

    if (logContent != null && request != null) {
        // Start AI analysis
        logAnalysisService.startAnalysis(
            request.getOfficerId(),
            logContent,
            request.getModel(),
            request.getPrompt()
        );
    }

    log.info("Log reading completed for session: {}", sessionId);
}
```

---

### 2. Fleet Officer (Go) - Log Reading

**Neue Datei:** `Fleet-Officer-Linux/internal/commands/log_reader.go`

```go
package commands

import (
    "fmt"
    "os"
    "strings"
)

type LogReader struct {
    wsClient *websocket.Client
}

func NewLogReader(ws *websocket.Client) *LogReader {
    return &LogReader{wsClient: ws}
}

// HandleReadLogCommand processes read_log command from Navigator
func (lr *LogReader) HandleReadLogCommand(payload map[string]interface{}) error {
    path := payload["path"].(string)
    mode := payload["mode"].(string)  // "smart", "full", "errors-only"

    // Read log file
    content, err := os.ReadFile(path)
    if err != nil {
        return fmt.Errorf("failed to read log: %w", err)
    }

    // Apply filtering if mode is "smart"
    logData := string(content)
    if mode == "smart" {
        logData = lr.filterRelevantLines(logData)
    }

    sessionId := fmt.Sprintf("%s-%d", "officerId", time.Now().UnixMilli())

    // Send log data in chunks (10KB each)
    chunkSize := 10240
    for i := 0; i < len(logData); i += chunkSize {
        end := i + chunkSize
        if end > len(logData) {
            end = len(logData)
        }

        chunk := logData[i:end]

        lr.wsClient.SendMessage(Message{
            Type: "log_data",
            Data: map[string]interface{}{
                "sessionId": sessionId,
                "chunk": chunk,
                "progress": float64(end) / float64(len(logData)) * 100,
            },
        })
    }

    // Send completion message
    lr.wsClient.SendMessage(Message{
        Type: "log_complete",
        Data: map[string]interface{}{
            "sessionId": sessionId,
            "totalSize": len(logData),
        },
    })

    return nil
}

func (lr *LogReader) filterRelevantLines(content string) string {
    lines := strings.Split(content, "\n")
    relevant := []string{}

    keywords := []string{
        "error", "ERROR", "Error",
        "warn", "WARN", "warning",
        "fail", "FAIL", "failed",
        "critical", "CRITICAL",
        "panic", "segfault",
        "out of memory", "OOM",
        "authentication failure",
    }

    for _, line := range lines {
        for _, keyword := range keywords {
            if strings.Contains(line, keyword) {
                relevant = append(relevant, line)
                break
            }
        }
    }

    return strings.Join(relevant, "\n")
}
```

**Integration in `main.go`:**
```go
// In message handler:
case "read_log":
    logReader := commands.NewLogReader(wsClient)
    if err := logReader.HandleReadLogCommand(msg.Data); err != nil {
        log.Printf("Failed to read log: %v", err)
    }
```

---

### 3. Frontend Terminal Tab (Vue.js)

**Zu ändern:** `frontend/src/components/OfficerDetailModal.vue`

**Terminal Tab ersetzen mit:**

```vue
<!-- Terminal Tab -->
<div v-if="activeTab === 'terminal'" class="space-y-4">
  <!-- Log Analysis Form -->
  <div class="bg-gray-800/50 p-4 rounded-xl border border-gray-700/50">
    <h4 class="text-sm font-semibold text-gray-300 mb-3 flex items-center gap-2">
      <SparklesIcon class="w-4 h-4 text-yellow-400" />
      AI-gestützte Log-Analyse
    </h4>

    <div class="space-y-3">
      <!-- Log Path -->
      <div>
        <label class="text-xs text-gray-400 block mb-1">Log-Datei</label>
        <select v-model="logAnalysis.path" class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm">
          <option value="/var/log/syslog">System Log (/var/log/syslog)</option>
          <option value="/var/log/auth.log">Auth Log</option>
          <option value="/var/log/kern.log">Kernel Log</option>
        </select>
      </div>

      <!-- Mode -->
      <div>
        <label class="text-xs text-gray-400 block mb-1">Analyse-Modus</label>
        <select v-model="logAnalysis.mode" class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm">
          <option value="smart">Smart (nur relevante Events)</option>
          <option value="full">Vollständig (alle Zeilen)</option>
          <option value="errors-only">Nur Errors</option>
        </select>
      </div>

      <!-- Model -->
      <div>
        <label class="text-xs text-gray-400 block mb-1">AI Modell</label>
        <select v-model="logAnalysis.model" class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm">
          <option value="llama3.2:3b">llama3.2:3b (schnell)</option>
          <option value="qwen2.5:7b">qwen2.5:7b</option>
          <option value="mistral:7b">mistral:7b</option>
        </select>
      </div>

      <!-- Start Button -->
      <button
        @click="startLogAnalysis"
        :disabled="analyzing"
        class="w-full px-4 py-2 rounded-lg bg-gradient-to-r from-fleet-orange-500 to-orange-600
               hover:from-fleet-orange-400 hover:to-orange-500
               text-white font-semibold text-sm
               disabled:opacity-50 disabled:cursor-not-allowed
               transition-all duration-200 transform hover:scale-105 active:scale-95
               flex items-center justify-center gap-2"
      >
        <SparklesIcon class="w-4 h-4" :class="{ 'animate-spin': analyzing }" />
        {{ analyzing ? 'Analysiere...' : 'Analyse starten' }}
      </button>
    </div>
  </div>

  <!-- Terminal Output -->
  <div class="bg-black/70 p-4 rounded-xl border border-gray-700/50 font-mono text-xs min-h-[300px] max-h-[500px] overflow-y-auto">
    <div class="flex items-center justify-between mb-3 pb-2 border-b border-gray-700">
      <span class="text-green-400">fleet-officer@{{ officer.officerId }}</span>
      <span class="text-gray-500 text-xs">{{ new Date().toLocaleTimeString('de-DE') }}</span>
    </div>

    <!-- Output -->
    <div class="text-gray-300 whitespace-pre-wrap" v-html="analysisOutput"></div>

    <!-- Typing Cursor -->
    <span v-if="analyzing" class="inline-block w-2 h-4 bg-green-400 animate-pulse ml-1"></span>
  </div>
</div>
```

**Script hinzufügen:**
```javascript
const logAnalysis = ref({
  path: '/var/log/syslog',
  mode: 'smart',
  model: 'llama3.2:3b'
})

const analyzing = ref(false)
const analysisOutput = ref('$ Warte auf Analyse...\n')

async function startLogAnalysis() {
  analyzing.value = true
  analysisOutput.value = '$ Starte Log-Analyse...\n\n'

  try {
    // Send analysis request
    const response = await axios.post(
      `/api/fleet-officer/officers/${props.officer.officerId}/analyze-log`,
      {
        logPath: logAnalysis.value.path,
        mode: logAnalysis.value.mode,
        model: logAnalysis.value.model,
        prompt: 'Analysiere dieses System-Log nach Fehlern, Warnungen und Auffälligkeiten.'
      }
    )

    const sessionId = response.data.sessionId
    analysisOutput.value += `✓ Session erstellt: ${sessionId}\n`
    analysisOutput.value += `✓ Officer liest Log-Datei...\n\n`

    // Connect to SSE stream
    const eventSource = new EventSource(`/api/fleet-officer/stream/${sessionId}`)

    eventSource.addEventListener('start', (event) => {
      analysisOutput.value += '🤖 AI-Analyse gestartet...\n\n'
    })

    eventSource.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      analysisOutput.value += data.chunk
    })

    eventSource.addEventListener('done', (event) => {
      analysisOutput.value += '\n\n✓ Analyse abgeschlossen!\n'
      eventSource.close()
      analyzing.value = false
    })

    eventSource.addEventListener('error', (event) => {
      analysisOutput.value += '\n\n✗ Fehler bei der Analyse\n'
      eventSource.close()
      analyzing.value = false
      error('Fehler bei der Log-Analyse')
    })

  } catch (err) {
    console.error('Failed to start analysis:', err)
    analysisOutput.value += `\n✗ Fehler: ${err.message}\n`
    analyzing.value = false
    error('Fehler beim Starten der Analyse')
  }
}
```

---

## 🚀 Minimaler Test

```bash
# 1. Navigator starten
cd Fleet-Navigator
mvn clean package -DskipTests
java -jar target/fleet-navigator-*.jar

# 2. Fleet Officer starten (nach Go-Änderungen)
cd ../Fleet-Officer-Linux
go build -o fleet-officer main.go
./fleet-officer

# 3. Browser öffnen
# http://localhost:2025
# → Fleet Officers Dashboard
# → Officer anklicken
# → Terminal Tab
# → Analyse starten
```

---

## 📝 Nächste Erweiterungen:

1. **Progress-Anzeigen** während Log-Lesen
2. **Mehrstufiges Filtering** (Errors only, Last N lines, etc.)
3. **Log-Historie** (letzte Analysen speichern)
4. **Export-Funktion** (Analyse als Markdown/PDF)
5. **Multi-File Analysis** (mehrere Logs kombiniert analysieren)

---

**Entwickelt von:** JavaFleet Systems Consulting
