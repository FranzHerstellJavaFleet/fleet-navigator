# Fleet Officer Integration

**Version:** 1.0
**Datum:** 2025-11-05

---

## 🎯 Überblick

Fleet Navigator wurde um Fleet Officer Support erweitert. Jetzt können externe Agents (Fleet Officers) sich verbinden und Hardware-Daten in Echtzeit über WebSocket liefern.

---

## 🏗️ Architektur

```
Fleet Navigator (Spring Boot)
    ↓ WebSocket Server (:2025/api/fleet-officer/ws)
    ↓
Fleet Officer Linux (Go Binary)
    ↓ WebSocket Client
    ↓ Hardware Monitoring (gopsutil)
    → CPU, RAM, Disk, Temperature, Network
```

---

## 🆕 Neue Komponenten

### Backend (Spring Boot)

1. **WebSocketConfig.java**
   - Aktiviert WebSocket Support
   - Route: `/api/fleet-officer/ws/{officerId}`

2. **FleetOfficerWebSocketHandler.java**
   - Empfängt Messages von Officers
   - Sendet Commands an Officers
   - Message Types:
     - `register` - Officer registriert sich
     - `stats` - Hardware-Statistiken
     - `heartbeat` - Lebenszeichen
     - `pong` - Antwort auf Ping

3. **FleetOfficerService.java**
   - Verwaltet registrierte Officers
   - Speichert letzte Hardware-Stats
   - Trackt Online/Offline Status

4. **FleetOfficerController.java**
   - REST API für Officer-Management
   - Endpoints:
     - `GET /api/fleet-officer/officers` - Alle Officers
     - `GET /api/fleet-officer/officers/{id}/stats` - Hardware-Stats
     - `POST /api/fleet-officer/officers/{id}/ping` - Ping Officer
     - `POST /api/fleet-officer/officers/{id}/collect-stats` - Stats anfordern

5. **DTOs**
   - `OfficerMessage.java` - Messages vom Officer
   - `OfficerCommand.java` - Commands an Officer
   - `HardwareStats.java` - Hardware-Datenstruktur

---

## 🚀 Schnellstart

### 1. Navigator starten

```bash
cd Fleet-Navigator
mvn clean package
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar
```

Navigator läuft auf: **http://localhost:2025**

### 2. Fleet Officer bauen

```bash
cd ../Fleet-Officer-Linux
go build -o fleet-officer main.go
```

### 3. Officer Konfiguration anpassen

Bearbeiten Sie `config.yml`:

```yaml
officer:
  id: "ubuntu-desktop-01"
  name: "Ubuntu Desktop Trainer"

navigator:
  url: "ws://localhost:2025/api/fleet-officer/ws"
```

### 4. Officer starten

```bash
./fleet-officer
```

**Erwartete Ausgabe:**
```
Fleet Officer Linux v1.0.0 starting...
Configuration loaded from config.yml
Officer ID: ubuntu-desktop-01
Officer Name: Ubuntu Desktop Trainer
Navigator URL: ws://localhost:2025/api/fleet-officer/ws
Connecting to Fleet Navigator at ws://localhost:2025/api/fleet-officer/ws/ubuntu-desktop-01
Connected to Fleet Navigator
Sending message: type=register, size=...
```

---

## 📊 API-Endpunkte

### Officer-Liste abrufen

```bash
curl http://localhost:2025/api/fleet-officer/officers
```

**Response:**
```json
[
  {
    "officerId": "ubuntu-desktop-01",
    "name": "Ubuntu Desktop Trainer",
    "description": "Primary development machine",
    "status": "ONLINE",
    "registeredAt": "2025-11-05T14:30:00",
    "lastHeartbeat": "2025-11-05T14:31:00",
    "lastStatsUpdate": "2025-11-05T14:31:05"
  }
]
```

### Hardware-Stats abrufen

```bash
curl http://localhost:2025/api/fleet-officer/officers/ubuntu-desktop-01/stats
```

**Response:**
```json
{
  "timestamp": "2025-11-05T14:31:05",
  "officerId": "ubuntu-desktop-01",
  "cpu": {
    "usagePercent": 45.5,
    "cores": 8,
    "model": "Intel Core i7-9750H",
    "mhz": 2600.0
  },
  "memory": {
    "total": 16842752000,
    "used": 8421376000,
    "usedPercent": 50.0
  },
  "disk": [
    {
      "mountPoint": "/",
      "total": 500107862016,
      "used": 250053931008,
      "usedPercent": 50.0
    }
  ],
  "system": {
    "hostname": "ubuntu-desktop",
    "os": "linux",
    "platform": "ubuntu",
    "platformVersion": "22.04",
    "kernelVersion": "5.15.0-91-generic",
    "uptime": 345600
  }
}
```

### Officer Ping

```bash
curl -X POST http://localhost:2025/api/fleet-officer/officers/ubuntu-desktop-01/ping
```

### Stats Sammlung anfordern

```bash
curl -X POST http://localhost:2025/api/fleet-officer/officers/ubuntu-desktop-01/collect-stats
```

### Zusammenfassung

```bash
curl http://localhost:2025/api/fleet-officer/summary
```

**Response:**
```json
{
  "total": 1,
  "online": 1,
  "offline": 0,
  "active_connections": 1
}
```

---

## 🔧 Troubleshooting

### Officer verbindet nicht

**Problem:** `Failed to connect: dial tcp: connection refused`

**Lösung:**
1. Prüfen Sie ob Navigator läuft:
   ```bash
   curl http://localhost:2025/actuator/health
   ```

2. Prüfen Sie Firewall-Einstellungen

3. Prüfen Sie die URL in `config.yml`

### Keine Hardware-Daten

**Problem:** Officer verbunden, aber keine Stats

**Lösung:**
1. Prüfen Sie Officer Logs:
   ```bash
   ./fleet-officer
   # Suchen Sie nach "Failed to collect stats"
   ```

2. Auf Linux: Permissions für Sensors:
   ```bash
   sudo usermod -aG sensors $USER
   ```

### WebSocket Connection Failed

**Problem:** Navigator startet nicht

**Lösung:**
1. Prüfen Sie Logs:
   ```bash
   tail -f logs/fleet-navigator.log
   ```

2. Port 2025 bereits belegt?
   ```bash
   sudo netstat -tulpn | grep 2025
   ```

---

## 🎨 Frontend Integration ✅ FERTIG

Das Frontend wurde vollständig integriert und zeigt Fleet Officer Daten an:

### SystemMonitor.vue - Komplett überarbeitet

**Neue Architektur:**
- Zeigt **NUR** Fleet Officer Daten an (keine lokalen Java-Daten mehr)
- Auto-Refresh alle 5 Sekunden
- Support für multiple Officers gleichzeitig

**Features:**

1. **Officer-Liste**
   - Alle verbundenen Officers mit Status (ONLINE/OFFLINE)
   - Name, Description, Officer-ID
   - Farbcodierte Status-Badges

2. **System-Informationen** (vollständig)
   - Hostname
   - OS: Ubuntu/Linux mit Version
   - Kernel Version
   - Uptime (Tage/Stunden)

3. **CPU Details** (alle Kerne einzeln!)
   - CPU Modell (verkürzt angezeigt)
   - Taktfrequenz in MHz
   - Anzahl Kerne
   - **Gesamt-Auslastung**
   - **Pro-Kern Auslastung** mit:
     - Visueller Progress-Bar (farbcodiert)
     - Prozent-Wert
     - **Temperatur pro Core!**
   - Farbcodierung:
     - 🟢 Grün: < 50% / < 60°C
     - 🟡 Gelb: 50-75% / 60-80°C
     - 🔴 Rot: > 75% / > 80°C

4. **RAM**
   - Gesamt/Verwendet in GB
   - Prozent-Anzeige mit Progress-Bar
   - Swap-Informationen

5. **Disk**
   - Pro Mountpoint (/, /home, etc.)
   - Device, Filesystem-Typ
   - Belegt/Gesamt in GB
   - Prozent-Anzeige

6. **CPU Package Temperatur**
   - Zentrale CPU-Temperatur
   - Farbcodiertes Badge

**API Endpoints verwendet:**
```
GET /api/fleet-officer/officers           → Liste aller Officers
GET /api/fleet-officer/officers/{id}/stats → Hardware-Daten
```

**Technische Details:**
- Vue 3 Composition API
- Axios für REST Calls
- Auto-Refresh mit setInterval
- Responsive Design
- Custom Scrollbar
- Glassmorphism Effects

---

## 🎯 Fleet Officers Dashboard ✅ **NEU!**

### Vollständiges Dashboard für Officer-Verwaltung

**Location:** TopBar → Server-Icon Button (orange)

#### Features:

1. **Grid Layout mit Officer-Tiles**
   - Jedes Tile zeigt einen Officer mit:
     - Name, Description, Status (ONLINE/OFFLINE)
     - Betriebssystem (Ubuntu 22.04, etc.)
     - CPU-Auslastung mit farbcodierter Progress-Bar
     - RAM-Auslastung mit farbcodierter Progress-Bar
     - CPU-Temperatur mit farbcodiertem Badge
   - Responsive Grid (1-4 Spalten je nach Bildschirmgröße)
   - Hover-Effekte mit Scale und Shadow

2. **Summary Cards**
   - Gesamt-Officers
   - Online-Officers (grün)
   - Offline-Officers (rot)

3. **Officer Detail Modal** (Click auf Tile)
   - **Hardware Tab:**
     - Vollständige System-Info (Hostname, OS, Kernel, Uptime)
     - CPU-Details (alle Kerne einzeln mit Temperatur)
     - RAM-Details (Total, Used, Swap)
     - Disk-Details (alle Mountpoints)
     - CPU Package Temperatur
   - **Terminal Tab:** (Coming Soon)
     - Remote Command Execution
     - File System Navigation
     - Log-Analyse
     - Service Management

4. **Auto-Refresh**
   - Alle 5 Sekunden automatische Aktualisierung
   - Manueller Refresh-Button verfügbar

**Implementierte Komponenten:**
- `FleetOfficersDashboard.vue` - Full-Screen Dashboard
- `OfficerDetailModal.vue` - Detail-View mit Tabs
- `OfficerHardwareCard.vue` - Hardware-Daten Anzeige (wiederverwendbar)

---

## 📝 Nächste Schritte

- [x] Frontend-Integration (Vue.js) ✅ **FERTIG**
  - SystemMonitor.vue zeigt alle Officer-Daten
  - Alle CPU-Kerne einzeln mit Temperatur
  - Vollständige System-Info (OS, Kernel, Uptime)
- [x] **Fleet Officers Dashboard** ✅ **FERTIG**
  - Grid Layout mit Officer-Tiles
  - Detail-Modal mit Hardware und Terminal Tabs
  - Auto-Refresh und Live-Updates
- [ ] **Remote Command Execution** 🚀 **NEXT**
  - Officer kann Befehle vom Navigator empfangen
  - File System Navigation
  - Log-Analyse
  - Service Management (start/stop/restart)
  - Command History & Auto-Completion
- [ ] Datenbank-Persistierung (H2/MySQL)
  - Historische Daten speichern
  - Performance-Metriken über Zeit
- [ ] Alerting System
  - Bei hoher CPU/RAM/Temp
  - Email/Webhook Benachrichtigungen
- [ ] Security Features
  - TLS/SSL für WebSocket
  - Officer Authentifizierung (API Key)
  - Role-Based Access Control
- [ ] Multi-Platform Support
  - Windows Officer Version
  - Raspberry Pi ARM64 Builds
  - macOS Officer (optional)

---

**Entwickelt von:** JavaFleet Systems Consulting
**Lizenz:** MIT
