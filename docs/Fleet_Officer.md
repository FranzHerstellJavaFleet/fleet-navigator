# Fleet Officer Architecture

**Version:** 1.0
**Datum:** 2025-11-04
**Projekt:** Fleet Navigator - Distributed Agent System

---

## 🎯 Vision

Fleet Navigator erweitert sich von einer lokalen Chat-Anwendung zu einem **verteilten System** mit zentralem Server (Fleet Navigator) und dezentralen Agents (Fleet Officers), die auf verschiedenen Geräten (z.B. Raspberry Pis) laufen und vom AI-Modell gesteuert werden.

---

## 📐 Architektur-Übersicht

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Fleet Navigator Server                            │
│                  (Spring Boot 3.2.0 + Vue.js)                        │
│                       Port 2025                                      │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Fleet Admiral (AI - Ollama)                                    │ │
│  │ - Erstellt Execution Plans                                     │ │
│  │ - Überwacht Fleet Officers                                     │ │
│  │ - Koordiniert parallele Aufgaben                               │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               ▲                                      │
│                               │                                      │
│  ┌────────────────────────────┼───────────────────────────────────┐ │
│  │ Agent Management Layer     │                                   │ │
│  ├────────────────────────────┴───────────────────────────────────┤ │
│  │ • Agent Registry (Registrierte Officers)                       │ │
│  │ • Task Queue (Warteschlange für Pläne)                         │ │
│  │ • Execution Monitor (Überwacht laufende Tasks)                 │ │
│  │ • Result Aggregator (Sammelt Ergebnisse)                       │ │
│  │ • Health Checker (Prüft Officer-Status)                        │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ REST API (/api/fleet-officer/*)                                │ │
│  │ - POST   /register          → Officer registrieren             │ │
│  │ - GET    /plans             → Neue Pläne abrufen               │ │
│  │ - POST   /heartbeat         → Lebenszeichen senden             │ │
│  │ - POST   /report            → Ergebnis melden                  │ │
│  │ - GET    /officers          → Alle Officers auflisten          │ │
│  │ - GET    /officers/{id}     → Officer-Details                  │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                               │
                               │ JSON über HTTPS
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Fleet Officer #1 │  │ Fleet Officer #2 │  │ Fleet Officer #3 │
│  pi-kamera-01    │  │  pi-sensor-01    │  │  pi-gateway-01   │
│  (Go Binary)     │  │  (Go Binary)     │  │  (Go Binary)     │
├──────────────────┤  ├──────────────────┤  ├──────────────────┤
│ • Raspberry Pi 4 │  │ • Raspberry Pi 3 │  │ • Raspberry Pi 5 │
│ • ARM64          │  │ • ARM64          │  │ • ARM64          │
│ • 2 GB RAM       │  │ • 1 GB RAM       │  │ • 4 GB RAM       │
│ • Kamera-Dienst  │  │ • Sensor-Dienst  │  │ • Gateway-Dienst │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## 🏗️ Komponenten-Detail

### 1. Fleet Navigator Server (Spring Boot)

#### 1.1 Neue Entities

```java
@Entity
@Table(name = "fleet_officers")
public class FleetOfficer {
    @Id
    private String id;                    // z.B. "pi-kamera-01"

    private String hostname;
    private String ipAddress;
    private String apiKey;                // UUID für Authentifizierung

    @Enumerated(EnumType.STRING)
    private OfficerStatus status;         // ONLINE, OFFLINE, BUSY, ERROR

    private String architecture;          // "arm64", "amd64"
    private String osVersion;             // "Raspbian 11"

    private LocalDateTime lastHeartbeat;
    private LocalDateTime registeredAt;

    // System Stats (aktualisiert bei jedem Heartbeat)
    private Double cpuUsage;
    private Long memoryUsed;
    private Long memoryTotal;
    private Long diskUsed;
    private Long diskTotal;
    private Double temperature;

    @OneToMany(mappedBy = "officer")
    private List<ExecutionPlan> plans;
}

@Entity
@Table(name = "execution_plans")
public class ExecutionPlan {
    @Id
    @GeneratedValue
    private Long id;

    private String planId;                // "plan_20251104_001"

    @ManyToOne
    @JoinColumn(name = "officer_id")
    private FleetOfficer officer;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;            // PENDING, EXECUTING, SUCCESS, FAILED

    @Enumerated(EnumType.STRING)
    private PlanPriority priority;        // LOW, NORMAL, HIGH, CRITICAL

    @Column(columnDefinition = "TEXT")
    private String planJson;              // Der komplette Plan als JSON

    @Column(columnDefinition = "TEXT")
    private String resultJson;            // Das Ergebnis als JSON

    private LocalDateTime createdAt;
    private LocalDateTime scheduledFor;   // Für zeitgesteuerte Ausführung
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Integer retryCount;
    private Integer maxRetries;

    @ManyToOne
    @JoinColumn(name = "created_by_chat_id")
    private Chat createdByChat;           // Aus welchem Chat kam die Anfrage?
}

@Entity
@Table(name = "execution_tasks")
public class ExecutionTask {
    @Id
    @GeneratedValue
    private Long id;

    private String taskId;                // "task_001"

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private ExecutionPlan plan;

    @Enumerated(EnumType.STRING)
    private TaskType type;                // SYSTEM_CHECK, UPDATE, SERVICE_RESTART, CLEANUP, CUSTOM

    private String description;

    @Column(columnDefinition = "TEXT")
    private String commandsJson;          // Liste von Befehlen

    @Enumerated(EnumType.STRING)
    private TaskStatus status;            // PENDING, RUNNING, SUCCESS, FAILED

    @Column(columnDefinition = "TEXT")
    private String output;                // Befehlsausgabe

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Integer exitCode;
    private Integer orderIndex;           // Reihenfolge der Ausführung
}
```

#### 1.2 REST Controller

```java
@RestController
@RequestMapping("/api/fleet-officer")
@RequiredArgsConstructor
public class FleetOfficerController {

    private final FleetOfficerService officerService;

    /**
     * Officer registriert sich beim Server
     * POST /api/fleet-officer/register
     */
    @PostMapping("/register")
    public ResponseEntity<OfficerRegistrationResponse> register(
        @RequestBody OfficerRegistrationRequest request
    ) {
        // Erstellt neuen Officer oder updated bestehenden
        // Generiert API Key
        // Gibt Server-Config zurück (Heartbeat-Interval, etc.)
    }

    /**
     * Officer fragt nach neuen Plänen
     * GET /api/fleet-officer/plans?officer_id=pi-kamera-01
     */
    @GetMapping("/plans")
    public ResponseEntity<PendingPlansResponse> getPendingPlans(
        @RequestParam String officerId,
        @RequestHeader("X-API-Key") String apiKey
    ) {
        // Authentifiziert Officer
        // Holt PENDING Plans für diesen Officer
        // Markiert Plans als EXECUTING
        // Gibt maximal 1 Plan zurück (Officer kann nur 1 gleichzeitig)
    }

    /**
     * Officer sendet Heartbeat (alle 30 Sekunden)
     * POST /api/fleet-officer/heartbeat
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(
        @RequestBody HeartbeatRequest request,
        @RequestHeader("X-API-Key") String apiKey
    ) {
        // Updated lastHeartbeat
        // Updated System Stats (CPU, RAM, etc.)
        // Status bleibt ONLINE
    }

    /**
     * Officer meldet Plan-Ergebnis
     * POST /api/fleet-officer/report
     */
    @PostMapping("/report")
    public ResponseEntity<Void> report(
        @RequestBody ExecutionReportRequest request,
        @RequestHeader("X-API-Key") String apiKey
    ) {
        // Speichert Ergebnis in execution_plans
        // Updated Plan-Status (SUCCESS/FAILED)
        // Benachrichtigt User über Chat (wenn gewünscht)
    }
}
```

#### 1.3 Service Layer

```java
@Service
@RequiredArgsConstructor
public class FleetOfficerService {

    private final FleetOfficerRepository officerRepository;
    private final ExecutionPlanRepository planRepository;
    private final ChatService chatService;

    /**
     * Erstellt einen neuen Execution Plan
     * Wird vom AI-Modell aufgerufen
     */
    public ExecutionPlan createPlan(String officerId, ExecutionPlanDTO planDto) {
        FleetOfficer officer = officerRepository.findById(officerId)
            .orElseThrow(() -> new OfficerNotFoundException(officerId));

        ExecutionPlan plan = new ExecutionPlan();
        plan.setPlanId("plan_" + System.currentTimeMillis());
        plan.setOfficer(officer);
        plan.setStatus(PlanStatus.PENDING);
        plan.setPriority(planDto.getPriority());
        plan.setPlanJson(planDto.toJson());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setScheduledFor(planDto.getScheduledFor());

        return planRepository.save(plan);
    }

    /**
     * Überwacht alle Officers und markiert OFFLINE wenn kein Heartbeat
     */
    @Scheduled(fixedRate = 60000) // Alle 60 Sekunden
    public void monitorOfficers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);

        List<FleetOfficer> onlineOfficers = officerRepository
            .findByStatus(OfficerStatus.ONLINE);

        for (FleetOfficer officer : onlineOfficers) {
            if (officer.getLastHeartbeat().isBefore(threshold)) {
                officer.setStatus(OfficerStatus.OFFLINE);
                officerRepository.save(officer);

                log.warn("Officer {} went OFFLINE (last heartbeat: {})",
                    officer.getId(), officer.getLastHeartbeat());
            }
        }
    }
}
```

#### 1.4 AI Integration - Fleet Admiral

```java
@Service
@RequiredArgsConstructor
public class FleetAdmiralService {

    private final OllamaService ollamaService;
    private final FleetOfficerService officerService;

    /**
     * User fragt: "Führe Wartung auf pi-kamera-01 durch"
     * AI erstellt Execution Plan
     */
    public String processMaintenanceRequest(String userMessage, String chatId) {
        // 1. Parse User-Request
        // 2. Hole verfügbare Officers
        List<FleetOfficer> officers = officerService.getOnlineOfficers();

        // 3. Erstelle Prompt für AI
        String systemPrompt = buildFleetAdmiralPrompt(officers);

        // 4. AI generiert Plan
        String aiResponse = ollamaService.generateCompletion(
            systemPrompt,
            userMessage
        );

        // 5. Parse JSON Plan aus AI-Response
        ExecutionPlanDTO plan = parseExecutionPlan(aiResponse);

        // 6. Validiere Plan
        validatePlan(plan);

        // 7. Speichere Plan in DB
        ExecutionPlan savedPlan = officerService.createPlan(
            plan.getOfficerId(),
            plan
        );

        // 8. Antwort an User
        return String.format(
            "✅ Wartungsplan erstellt für %s (Plan-ID: %s)\n\n" +
            "Geplante Tasks:\n%s\n\n" +
            "Status: PENDING - Wird beim nächsten Heartbeat ausgeführt.",
            plan.getOfficerId(),
            savedPlan.getPlanId(),
            formatTasks(plan.getTasks())
        );
    }

    private String buildFleetAdmiralPrompt(List<FleetOfficer> officers) {
        return """
            Du bist der Fleet Admiral der Fleet Navigator Plattform.
            Du steuerst Fleet Officers - kleine Go-Agents auf Geräten.

            Verfügbare Fleet Officers:
            %s

            Unterstützte Task-Typen:
            - system_check: System-Informationen abrufen (df, free, uptime)
            - system_update: Pakete aktualisieren (apt update && apt upgrade)
            - service_restart: Service neustarten (systemctl restart <service>)
            - cleanup: Logs/Cache bereinigen (journalctl --vacuum, apt clean)
            - custom_command: Beliebiger Shell-Befehl

            Erstelle Execution Plans im JSON-Format:
            {
              "target_officer": "pi-kamera-01",
              "priority": "normal",
              "tasks": [
                {
                  "type": "system_check",
                  "description": "Überprüfe Festplattenspeicher",
                  "command": "df -h /"
                },
                ...
              ]
            }

            WICHTIG:
            - Nutze nur sichere Befehle
            - Achte auf korrekte Reihenfolge
            - Bei Updates immer erst "apt update"
            - Gib NUR das JSON zurück, keine Markdown-Blöcke
            """.formatted(formatOfficerList(officers));
    }
}
```

---

## 🚀 Fleet Officer (Go Agent)

### 2.1 Projekt-Struktur

```
fleet-officer/
├── cmd/
│   └── officer/
│       └── main.go              # Einstiegspunkt
├── internal/
│   ├── agent/
│   │   ├── agent.go             # Haupt-Agent Logik
│   │   ├── config.go            # Konfiguration
│   │   └── executor.go          # Task-Ausführung
│   ├── api/
│   │   ├── client.go            # REST Client zum Server
│   │   └── models.go            # JSON Strukturen
│   └── system/
│       ├── stats.go             # System-Statistiken
│       ├── commands.go          # Shell-Befehle
│       └── services.go          # Service-Management
├── config.yaml                  # Konfigurationsdatei
├── go.mod
└── go.sum
```

### 2.2 Haupt-Code (main.go)

```go
package main

import (
    "context"
    "log"
    "os"
    "os/signal"
    "syscall"
    "time"

    "fleet-officer/internal/agent"
    "fleet-officer/internal/api"
)

func main() {
    // Lade Konfiguration
    config, err := agent.LoadConfig("config.yaml")
    if err != nil {
        log.Fatalf("Failed to load config: %v", err)
    }

    // Erstelle API Client
    apiClient := api.NewClient(
        config.ServerURL,
        config.OfficerID,
        config.APIKey,
    )

    // Erstelle Agent
    officer := agent.NewOfficer(config, apiClient)

    // Kontext für Graceful Shutdown
    ctx, cancel := context.WithCancel(context.Background())
    defer cancel()

    // Signal Handler für SIGTERM/SIGINT
    sigCh := make(chan os.Signal, 1)
    signal.Notify(sigCh, syscall.SIGTERM, syscall.SIGINT)

    // Starte Agent
    go func() {
        if err := officer.Start(ctx); err != nil {
            log.Fatalf("Officer failed: %v", err)
        }
    }()

    log.Printf("✅ Fleet Officer %s started", config.OfficerID)

    // Warte auf Shutdown Signal
    <-sigCh
    log.Println("🛑 Shutting down Fleet Officer...")
    cancel()

    // Warte bis alle Tasks beendet sind
    time.Sleep(2 * time.Second)
    log.Println("👋 Fleet Officer stopped")
}
```

### 2.3 Agent Logik (agent.go)

```go
package agent

import (
    "context"
    "encoding/json"
    "log"
    "time"

    "fleet-officer/internal/api"
    "fleet-officer/internal/system"
)

type Officer struct {
    config    *Config
    apiClient *api.Client
    executor  *Executor

    isExecuting bool
    currentPlan *api.ExecutionPlan
}

func NewOfficer(config *Config, apiClient *api.Client) *Officer {
    return &Officer{
        config:      config,
        apiClient:   apiClient,
        executor:    NewExecutor(),
        isExecuting: false,
    }
}

func (o *Officer) Start(ctx context.Context) error {
    // 1. Registriere beim Server
    if err := o.register(); err != nil {
        return fmt.Errorf("registration failed: %w", err)
    }

    log.Printf("✅ Registered with server as %s", o.config.OfficerID)

    // 2. Starte Heartbeat-Loop
    heartbeatTicker := time.NewTicker(30 * time.Second)
    defer heartbeatTicker.Stop()

    // 3. Starte Plan-Polling-Loop
    planTicker := time.NewTicker(10 * time.Second)
    defer planTicker.Stop()

    for {
        select {
        case <-ctx.Done():
            return nil

        case <-heartbeatTicker.C:
            if err := o.sendHeartbeat(); err != nil {
                log.Printf("❌ Heartbeat failed: %v", err)
            }

        case <-planTicker.C:
            // Nur nach neuen Plänen fragen, wenn nicht beschäftigt
            if !o.isExecuting {
                if err := o.checkForPlans(ctx); err != nil {
                    log.Printf("❌ Check plans failed: %v", err)
                }
            }
        }
    }
}

func (o *Officer) register() error {
    req := &api.RegistrationRequest{
        OfficerID:    o.config.OfficerID,
        Hostname:     o.getHostname(),
        Architecture: runtime.GOARCH,
        OSVersion:    o.getOSVersion(),
    }

    resp, err := o.apiClient.Register(req)
    if err != nil {
        return err
    }

    // Speichere API Key aus Response
    o.config.APIKey = resp.APIKey
    o.saveConfig()

    return nil
}

func (o *Officer) sendHeartbeat() error {
    stats := system.GetSystemStats()

    req := &api.HeartbeatRequest{
        OfficerID:   o.config.OfficerID,
        CPUUsage:    stats.CPUUsage,
        MemoryUsed:  stats.MemoryUsed,
        MemoryTotal: stats.MemoryTotal,
        DiskUsed:    stats.DiskUsed,
        DiskTotal:   stats.DiskTotal,
        Temperature: stats.Temperature,
    }

    return o.apiClient.Heartbeat(req)
}

func (o *Officer) checkForPlans(ctx context.Context) error {
    // Hole neue Pläne vom Server
    resp, err := o.apiClient.GetPendingPlans(o.config.OfficerID)
    if err != nil {
        return err
    }

    if !resp.HasPendingPlans {
        return nil
    }

    log.Printf("📋 Received new plan: %s", resp.Plan.PlanID)

    // Führe Plan aus
    o.isExecuting = true
    o.currentPlan = &resp.Plan

    go func() {
        result := o.executor.ExecutePlan(ctx, &resp.Plan)

        // Sende Ergebnis zurück
        if err := o.reportResult(result); err != nil {
            log.Printf("❌ Failed to report result: %v", err)
        }

        o.isExecuting = false
        o.currentPlan = nil
    }()

    return nil
}

func (o *Officer) reportResult(result *api.ExecutionReport) error {
    return o.apiClient.ReportResult(result)
}
```

### 2.4 Task Executor (executor.go)

```go
package agent

import (
    "context"
    "fmt"
    "log"
    "os/exec"
    "strings"
    "time"

    "fleet-officer/internal/api"
    "fleet-officer/internal/system"
)

type Executor struct{}

func NewExecutor() *Executor {
    return &Executor{}
}

func (e *Executor) ExecutePlan(ctx context.Context, plan *api.ExecutionPlan) *api.ExecutionReport {
    report := &api.ExecutionReport{
        PlanID:        plan.PlanID,
        OfficerID:     plan.TargetOfficer,
        ExecutedAt:    time.Now(),
        OverallStatus: "success",
        TasksResults:  []api.TaskResult{},
    }

    startTime := time.Now()

    for _, task := range plan.Tasks {
        log.Printf("▶️  Executing task %s: %s", task.TaskID, task.Description)

        result := e.executeTask(ctx, task)
        report.TasksResults = append(report.TasksResults, result)

        // Bei Fehler abbrechen?
        if result.Status == "failed" && plan.StopOnError {
            report.OverallStatus = "failed"
            break
        }
    }

    report.DurationSeconds = int(time.Since(startTime).Seconds())
    report.SystemInfo = system.GetSystemStats()

    return report
}

func (e *Executor) executeTask(ctx context.Context, task api.Task) api.TaskResult {
    result := api.TaskResult{
        TaskID:    task.TaskID,
        StartedAt: time.Now(),
    }

    switch task.Type {
    case "system_check":
        output, err := e.executeCommand(ctx, task.Command)
        if err != nil {
            result.Status = "failed"
            result.Output = err.Error()
        } else {
            result.Status = "success"
            result.Output = output
        }

    case "system_update":
        for _, cmd := range task.Commands {
            output, err := e.executeCommand(ctx, cmd)
            result.Output += output + "\n"
            if err != nil {
                result.Status = "failed"
                result.Output += fmt.Sprintf("Error: %v", err)
                break
            }
        }
        if result.Status != "failed" {
            result.Status = "success"
        }

    case "service_restart":
        output, err := system.RestartService(task.Service)
        result.Output = output
        if err != nil {
            result.Status = "failed"
        } else {
            result.Status = "success"
            result.ServiceHealthy = system.IsServiceHealthy(task.Service)
        }

    case "cleanup":
        totalFreed := int64(0)
        for _, cmd := range task.Commands {
            output, err := e.executeCommand(ctx, cmd)
            result.Output += output + "\n"
            if err != nil {
                result.Status = "failed"
                break
            }
            // Parse freed space from output
            freed := parseFreedSpace(output)
            totalFreed += freed
        }
        if result.Status != "failed" {
            result.Status = "success"
            result.SpaceFreedMB = totalFreed
        }

    case "custom_command":
        output, err := e.executeCommand(ctx, task.Command)
        result.Output = output
        if err != nil {
            result.Status = "failed"
        } else {
            result.Status = "success"
        }

    default:
        result.Status = "failed"
        result.Output = fmt.Sprintf("Unknown task type: %s", task.Type)
    }

    result.CompletedAt = time.Now()

    return result
}

func (e *Executor) executeCommand(ctx context.Context, command string) (string, error) {
    // Sicherheitsprüfung
    if isCommandDangerous(command) {
        return "", fmt.Errorf("dangerous command rejected: %s", command)
    }

    // Mit Timeout ausführen
    ctx, cancel := context.WithTimeout(ctx, 5*time.Minute)
    defer cancel()

    cmd := exec.CommandContext(ctx, "bash", "-c", command)
    output, err := cmd.CombinedOutput()

    return string(output), err
}

func isCommandDangerous(command string) bool {
    dangerous := []string{
        "rm -rf /",
        "dd if=",
        "> /dev/sd",
        "mkfs.",
        ":(){ :|:& };:",  // Fork bomb
    }

    for _, pattern := range dangerous {
        if strings.Contains(command, pattern) {
            return true
        }
    }

    return false
}
```

---

## ⚡ Parallele Ausführung - Mehrere Officers gleichzeitig

### 3.1 Konzept

**Problem:** Was passiert, wenn mehrere Officers gleichzeitig Aufgaben erhalten?

**Lösung:** Jeder Officer arbeitet **unabhängig** und meldet seine Ergebnisse zurück.

### 3.2 Szenarien

#### Szenario A: Gleiche Aufgabe auf mehreren Officers

**User-Anfrage:**
```
"Führe System-Update auf allen Kamera-Pis durch"
```

**AI-Response:**
```json
{
  "parallel_execution": true,
  "plans": [
    {
      "target_officer": "pi-kamera-01",
      "tasks": [...]
    },
    {
      "target_officer": "pi-kamera-02",
      "tasks": [...]
    },
    {
      "target_officer": "pi-kamera-03",
      "tasks": [...]
    }
  ]
}
```

**Ablauf:**
1. Server erstellt 3 separate ExecutionPlans
2. Jeder Plan wird dem jeweiligen Officer zugewiesen
3. Beim nächsten Polling holt jeder Officer seinen Plan
4. Alle 3 Officers führen **parallel** aus
5. Jeder Officer meldet sein Ergebnis **unabhängig**

**Frontend zeigt:**
```
✅ pi-kamera-01: System-Update erfolgreich (142 MB freed)
✅ pi-kamera-02: System-Update erfolgreich (98 MB freed)
⚠️  pi-kamera-03: System-Update fehlgeschlagen (Network error)
```

#### Szenario B: Unterschiedliche Aufgaben gleichzeitig

**User-Anfrage:**
```
"Starte Kamera-Service auf pi-kamera-01 neu
 und überprüfe Disk Space auf pi-gateway-01"
```

**AI-Response:**
```json
{
  "parallel_execution": true,
  "plans": [
    {
      "target_officer": "pi-kamera-01",
      "tasks": [
        {
          "type": "service_restart",
          "service": "motion"
        }
      ]
    },
    {
      "target_officer": "pi-gateway-01",
      "tasks": [
        {
          "type": "system_check",
          "command": "df -h"
        }
      ]
    }
  ]
}
```

**Ablauf:**
1. Beide Plans werden gleichzeitig erstellt
2. pi-kamera-01 und pi-gateway-01 holen ihre Plans
3. Beide führen **parallel** aus
4. pi-kamera-01: Restart dauert 5 Sekunden
5. pi-gateway-01: Check dauert 2 Sekunden
6. Beide melden unabhängig Erfolg

#### Szenario C: Warteschlange bei hoher Last

**Situation:** Officer ist bereits beschäftigt, neue Aufgabe kommt rein

**Verhalten:**
```java
// Im FleetOfficerService
public ExecutionPlan createPlan(String officerId, ExecutionPlanDTO planDto) {
    FleetOfficer officer = getOfficer(officerId);

    // Prüfe ob Officer bereits beschäftigt
    boolean isBusy = planRepository.existsByOfficerAndStatus(
        officer,
        PlanStatus.EXECUTING
    );

    ExecutionPlan plan = new ExecutionPlan();

    if (isBusy) {
        // Plan wird in Warteschlange gestellt
        plan.setStatus(PlanStatus.PENDING);
        log.info("Officer {} is busy, plan queued", officerId);
    } else {
        plan.setStatus(PlanStatus.PENDING);
        log.info("Officer {} is free, plan ready", officerId);
    }

    // Plan wird erst beim nächsten Polling abgeholt
    return planRepository.save(plan);
}
```

**Ablauf:**
1. Officer führt Plan A aus (`isExecuting = true`)
2. Neuer Plan B kommt rein → Status PENDING
3. Officer fragt nach neuen Plans → Server gibt nichts zurück (Officer busy)
4. Officer beendet Plan A → `isExecuting = false`
5. Officer fragt erneut → Bekommt Plan B

### 3.3 Aggregation von Ergebnissen

**Spring Boot Service:**
```java
@Service
public class ExecutionAggregatorService {

    /**
     * Fasst Ergebnisse von parallel ausgeführten Plänen zusammen
     */
    public String aggregateResults(List<ExecutionPlan> plans) {
        int total = plans.size();
        int success = 0;
        int failed = 0;
        int pending = 0;

        StringBuilder summary = new StringBuilder();
        summary.append("📊 Parallel Execution Summary:\n\n");

        for (ExecutionPlan plan : plans) {
            String status = switch (plan.getStatus()) {
                case SUCCESS -> {
                    success++;
                    yield "✅";
                }
                case FAILED -> {
                    failed++;
                    yield "❌";
                }
                case EXECUTING -> {
                    yield "⏳";
                }
                default -> {
                    pending++;
                    yield "⏸️";
                }
            };

            summary.append(String.format(
                "%s %s: %s\n",
                status,
                plan.getOfficer().getId(),
                plan.getStatus()
            ));

            if (plan.getStatus() == PlanStatus.SUCCESS) {
                // Parse result JSON
                ExecutionResult result = parseResult(plan.getResultJson());
                summary.append(String.format(
                    "   Duration: %ds, Tasks: %d/%d successful\n",
                    result.getDurationSeconds(),
                    result.getSuccessfulTasks(),
                    result.getTotalTasks()
                ));
            }
        }

        summary.append(String.format(
            "\n📈 Total: %d | ✅ Success: %d | ❌ Failed: %d | ⏸️ Pending: %d",
            total, success, failed, pending
        ));

        return summary.toString();
    }
}
```

### 3.4 Frontend - Parallel Execution View

**Vue Component:**
```vue
<template>
  <div class="parallel-execution-monitor">
    <h3>Parallel Execution Monitor</h3>

    <!-- Alle laufenden Pläne -->
    <div class="execution-grid">
      <ExecutionCard
        v-for="plan in activePlans"
        :key="plan.planId"
        :plan="plan"
        :officer="plan.officer"
      />
    </div>

    <!-- Aggregierte Statistik -->
    <div class="summary">
      <div class="stat">
        <span class="label">Total Officers:</span>
        <span class="value">{{ totalOfficers }}</span>
      </div>
      <div class="stat">
        <span class="label">Active Plans:</span>
        <span class="value">{{ activePlans.length }}</span>
      </div>
      <div class="stat">
        <span class="label">Success Rate:</span>
        <span class="value">{{ successRate }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/services/api'

const activePlans = ref([])
const allOfficers = ref([])

const totalOfficers = computed(() => allOfficers.value.length)

const successRate = computed(() => {
  const completed = activePlans.value.filter(p =>
    p.status === 'SUCCESS' || p.status === 'FAILED'
  )
  if (completed.length === 0) return 0

  const successful = completed.filter(p => p.status === 'SUCCESS').length
  return Math.round((successful / completed.length) * 100)
})

onMounted(async () => {
  await loadData()

  // Refresh alle 5 Sekunden
  setInterval(loadData, 5000)
})

async function loadData() {
  const [plansResp, officersResp] = await Promise.all([
    api.getActivePlans(),
    api.getOfficers()
  ])

  activePlans.value = plansResp.data
  allOfficers.value = officersResp.data
}
</script>
```

---

## 🔐 Sicherheit

### 4.1 API Key Authentifizierung

```java
// Security Filter
@Component
public class OfficerAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Nur /api/fleet-officer/* prüfen
        if (!path.startsWith("/api/fleet-officer/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Registration braucht keinen API Key
        if (path.endsWith("/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        // API Key aus Header holen
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isEmpty()) {
            response.sendError(401, "Missing API Key");
            return;
        }

        // Validieren
        FleetOfficer officer = officerRepository.findByApiKey(apiKey)
            .orElse(null);

        if (officer == null) {
            response.sendError(401, "Invalid API Key");
            return;
        }

        // Officer in Request Context speichern
        request.setAttribute("officer", officer);

        filterChain.doFilter(request, response);
    }
}
```

### 4.2 Command Whitelisting

```java
@Service
public class CommandValidator {

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "df", "free", "uptime", "systemctl", "journalctl",
        "apt", "dpkg", "ps", "top", "htop", "ls", "cat"
    );

    private static final Set<String> DANGEROUS_PATTERNS = Set.of(
        "rm -rf /",
        "dd if=",
        "> /dev/",
        "mkfs",
        ":(){ :|:& };:"
    );

    public void validate(String command) throws InvalidCommandException {
        // Prüfe auf gefährliche Patterns
        for (String pattern : DANGEROUS_PATTERNS) {
            if (command.contains(pattern)) {
                throw new InvalidCommandException(
                    "Dangerous command detected: " + pattern
                );
            }
        }

        // Prüfe ob Befehl erlaubt ist
        String baseCommand = command.split(" ")[0];
        if (!ALLOWED_COMMANDS.contains(baseCommand)) {
            throw new InvalidCommandException(
                "Command not whitelisted: " + baseCommand
            );
        }
    }
}
```

---

## 📅 Implementierungsplan

### Phase 1: Backend Foundation (2-3 Tage)
- [ ] Entities erstellen (FleetOfficer, ExecutionPlan, ExecutionTask)
- [ ] Repositories & Services
- [ ] REST API Controller
- [ ] Security Filter
- [ ] Basic Tests

### Phase 2: Go Agent Prototype (2-3 Tage)
- [ ] Go Projekt Setup
- [ ] Registration
- [ ] Heartbeat
- [ ] Plan Polling
- [ ] Basic Task Execution
- [ ] Kompilieren für ARM64

### Phase 3: AI Integration (1-2 Tage)
- [ ] Fleet Admiral System Prompt
- [ ] Plan-Generierung
- [ ] Plan-Validierung
- [ ] Chat-Integration

### Phase 4: Frontend (2-3 Tage)
- [ ] Officer Manager View
- [ ] Execution Monitor
- [ ] Plan Creator UI
- [ ] Log Viewer

### Phase 5: Advanced Features (3-4 Tage)
- [ ] Scheduled Executions (Cron)
- [ ] Officer Groups
- [ ] Alert System
- [ ] Result Aggregation
- [ ] WebSocket für Real-Time Updates

### Phase 6: Deployment & Testing (2-3 Tage)
- [ ] Deploy auf echten Raspberry Pis
- [ ] Stress-Tests (10+ Officers parallel)
- [ ] Network Resilience Tests
- [ ] Documentation

---

## 🎯 Nächste Schritte

1. **Review dieses Dokuments** - Feedback?
2. **Entscheidungen:**
   - REST Polling oder WebSocket?
   - MQTT als Option?
3. **Start mit Phase 1** - Backend Entities?

---

**Erstellt:** 2025-11-04
**Version:** 1.0
**Status:** Planning ✏️
