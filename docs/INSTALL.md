# Fleet Navigator - Installation Guide

Vollständige Installationsanleitung für alle Plattformen und Deployment-Szenarien.

## 📋 Inhaltsverzeichnis

1. [Desktop Installation](#desktop-installation)
   - [Windows](#windows)
   - [macOS](#macos)
   - [Linux Desktop](#linux-desktop)
2. [Server Installation](#server-installation)
   - [systemd Service (empfohlen)](#systemd-service-empfohlen)
   - [Docker](#docker-optional)
3. [Native Image Build (für Entwickler)](#native-image-build-für-entwickler)
4. [Entwicklungs-Setup](#entwicklungs-setup)

---

## Desktop Installation

### Windows

#### Voraussetzungen
- Windows 10/11 (64-bit)
- Mindestens 4 GB RAM (8 GB empfohlen)
- 5 GB freier Speicherplatz

#### Installation

1. **Download**
   ```
   https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest
   ```
   Lade `fleet-navigator-windows-amd64.zip` herunter

2. **Entpacken**
   - Rechtsklick auf ZIP-Datei → "Alle extrahieren"
   - Wähle Zielordner (z.B. `C:\Programme\FleetNavigator\`)

3. **Starten**
   - Doppelklick auf `fleet-navigator.exe`
   - Browser öffnet automatisch http://localhost:2025
   - Falls Windows SmartScreen Warnung erscheint: "Weitere Informationen" → "Trotzdem ausführen"

4. **Firewall-Freigabe**
   - Windows fragt nach Firewall-Berechtigung
   - **Erlauben** für private Netzwerke

5. **Modell herunterladen**
   - Im Fleet Navigator: Klick auf "Modelle"
   - Wähle ein Modell (empfohlen: Qwen2.5-3B)
   - Klick auf "Download"
   - Warte auf Abschluss (~2 GB)

6. **Fertig!**
   - Stelle deine erste Frage oder generiere einen Brief

#### Autostart (optional)

Erstelle eine Verknüpfung zu `fleet-navigator.exe` und lege sie in:
```
C:\Users\<Dein Name>\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup\
```

---

### macOS

#### Voraussetzungen
- macOS 12 (Monterey) oder neuer
- Intel oder Apple Silicon (M1/M2/M3)
- Mindestens 4 GB RAM (8 GB empfohlen)

#### Installation

1. **Download**
   ```
   https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest
   ```
   Lade `fleet-navigator-macos-amd64.tar.gz` herunter

2. **Entpacken**
   ```bash
   cd ~/Downloads
   tar -xzf fleet-navigator-macos-amd64.tar.gz
   mv fleet-navigator ~/Applications/
   ```

3. **Berechtigung erteilen**
   ```bash
   cd ~/Applications/fleet-navigator
   chmod +x fleet-navigator
   ```

4. **Starten**
   ```bash
   ./fleet-navigator
   ```

   Falls macOS "Entwickler nicht verifiziert" meldet:
   - Systemeinstellungen → Sicherheit
   - "Dennoch öffnen" klicken

5. **Browser öffnen**
   ```
   http://localhost:2025
   ```

6. **Modell herunterladen**
   - Im Fleet Navigator: "Modelle" → Modell wählen → "Download"

#### Autostart mit LaunchAgent (optional)

```bash
cat > ~/Library/LaunchAgents/com.javafleet.navigator.plist <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.javafleet.navigator</string>
    <key>ProgramArguments</key>
    <array>
        <string>/Users/IHR_NAME/Applications/fleet-navigator/fleet-navigator</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>WorkingDirectory</key>
    <string>/Users/IHR_NAME/Applications/fleet-navigator</string>
</dict>
</plist>
EOF

# Aktivieren
launchctl load ~/Library/LaunchAgents/com.javafleet.navigator.plist
```

---

### Linux Desktop

#### Voraussetzungen
- Linux Distribution (Ubuntu 20.04+, Debian 11+, Fedora 35+, etc.)
- glibc 2.31 oder neuer
- Mindestens 4 GB RAM

#### Installation

1. **Download**
   ```bash
   cd ~/Downloads
   wget https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest/download/fleet-navigator-linux-amd64.tar.gz
   ```

2. **Entpacken**
   ```bash
   tar -xzf fleet-navigator-linux-amd64.tar.gz
   sudo mv fleet-navigator /opt/
   ```

3. **Berechtigung**
   ```bash
   sudo chown -R $USER:$USER /opt/fleet-navigator
   chmod +x /opt/fleet-navigator/fleet-navigator
   ```

4. **Starten**
   ```bash
   cd /opt/fleet-navigator
   ./fleet-navigator
   ```

5. **Browser öffnen**
   ```
   http://localhost:2025
   ```

6. **Desktop-Verknüpfung erstellen (optional)**
   ```bash
   cat > ~/.local/share/applications/fleet-navigator.desktop <<'EOF'
   [Desktop Entry]
   Name=Fleet Navigator
   Comment=Private AI Chat Interface
   Exec=/opt/fleet-navigator/fleet-navigator
   Icon=/opt/fleet-navigator/icon.png
   Terminal=false
   Type=Application
   Categories=Office;Utility;
   EOF
   ```

---

## Server Installation

### systemd Service (empfohlen)

Für dauerhafte Server-Installation mit automatischem Start.

#### Schnellinstallation

```bash
# 1. Download
cd /tmp
wget https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest/download/fleet-navigator-linux-amd64.tar.gz
tar -xzf fleet-navigator-linux-amd64.tar.gz
cd fleet-navigator

# 2. Installation ausführen
sudo ./install-service.sh

# 3. Service starten
sudo systemctl start fleet-navigator

# 4. Status prüfen
sudo systemctl status fleet-navigator

# 5. Autostart aktivieren
sudo systemctl enable fleet-navigator
```

#### Manuelle Installation

```bash
# 1. Verzeichnisse erstellen
sudo mkdir -p /opt/fleet-navigator/{data,models/{library,custom},bin}

# 2. Binary kopieren
sudo cp fleet-navigator /opt/fleet-navigator/
sudo chmod +x /opt/fleet-navigator/fleet-navigator

# 3. Binaries kopieren
sudo cp -r bin/* /opt/fleet-navigator/bin/

# 4. Berechtigungen
sudo chown -R $USER:$USER /opt/fleet-navigator

# 5. systemd Service
sudo cp fleet-navigator.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable fleet-navigator
sudo systemctl start fleet-navigator
```

#### Konfiguration

Editiere `/etc/systemd/system/fleet-navigator.service`:

```ini
[Service]
# Modelle-Pfad anpassen
Environment="LLM_LLAMACPP_MODELS_DIR=/mnt/storage/models"

# Port ändern
Environment="SERVER_PORT=8080"

# Profil setzen
Environment="SPRING_PROFILES_ACTIVE=production"
```

Nach Änderungen:
```bash
sudo systemctl daemon-reload
sudo systemctl restart fleet-navigator
```

#### Service-Verwaltung

```bash
# Starten
sudo systemctl start fleet-navigator

# Stoppen
sudo systemctl stop fleet-navigator

# Status
sudo systemctl status fleet-navigator

# Logs anzeigen
sudo journalctl -u fleet-navigator -f

# Logs der letzten Stunde
sudo journalctl -u fleet-navigator --since "1 hour ago"
```

#### Detaillierte Anleitung

Siehe [SYSTEMD-DEPLOYMENT.md](SYSTEMD-DEPLOYMENT.md) für:
- Erweiterte Konfiguration
- Security Hardening
- Backup-Strategien
- Troubleshooting
- Monitoring

---

### Docker (optional)

Für containerisierte Deployments.

#### Dockerfile

```dockerfile
FROM ubuntu:22.04

# Install dependencies
RUN apt-get update && apt-get install -y \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy binary and dependencies
COPY fleet-navigator /app/
COPY bin/ /app/bin/

# Create data and models directories
RUN mkdir -p /app/data /app/models

# Expose port
EXPOSE 2025

# Run
CMD ["/app/fleet-navigator"]
```

#### Docker Compose

```yaml
version: '3.8'

services:
  fleet-navigator:
    build: .
    ports:
      - "2025:2025"
    volumes:
      - ./data:/app/data
      - ./models:/app/models
    environment:
      - LLM_LLAMACPP_MODELS_DIR=/app/models
      - SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/fleetnavdb
    restart: unless-stopped
```

#### Build und Start

```bash
# Build
docker build -t fleet-navigator .

# Run
docker run -d \
  -p 2025:2025 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/models:/app/models \
  --name fleet-navigator \
  fleet-navigator

# Logs
docker logs -f fleet-navigator

# Stop
docker stop fleet-navigator
```

---

## Native Image Build (für Entwickler)

Eigenes Native Image aus dem Quellcode erstellen.

### Voraussetzungen

- **GraalVM** 21 oder neuer mit Native Image
- **Maven** 3.8+
- **Node.js** 18+
- **Speicher**: Mindestens 8 GB RAM für den Build

### Build-Prozess

```bash
# 1. Repository klonen
git clone https://github.com/FranzHerstellJavaFleet/fleet-navigator.git
cd fleet-navigator

# 2. Native Image bauen
mvn -Pnative clean package -DskipTests

# 3. Binary testen
./target/fleet-navigator

# 4. Browser öffnen
# http://localhost:2025
```

### Plattform-spezifische Builds

#### Linux
```bash
mvn -Pnative clean package -DskipTests \
  "-Dnative.buildArg=-march=compatibility"
```

#### macOS
```bash
mvn -Pnative clean package -DskipTests \
  "-Dnative.buildArg=-march=compatibility"
```

#### Windows
```powershell
# Visual Studio Developer PowerShell öffnen
& "C:\Program Files\Microsoft Visual Studio\2022\Community\Common7\Tools\Launch-VsDevShell.ps1" -Arch amd64

mvn -Pnative clean package -DskipTests
```

### Build-Zeit

- **Erster Build**: 5-10 Minuten
- **Nachfolgende Builds**: 3-5 Minuten
- **RAM-Verbrauch**: Bis zu 6 GB während des Builds

### Detaillierte Anleitung

Siehe [NATIVE-IMAGE.md](NATIVE-IMAGE.md) für:
- Troubleshooting
- Optimierungen
- CI/CD Integration
- Cross-Compilation

---

## Entwicklungs-Setup

Für Entwickler, die am Projekt arbeiten möchten.

### Voraussetzungen

- **Java 17** oder neuer (JDK)
- **Maven 3.8+**
- **Node.js 18+** und npm
- **Git**

### Setup

```bash
# 1. Repository klonen
git clone https://github.com/FranzHerstellJavaFleet/fleet-navigator.git
cd fleet-navigator

# 2. Backend starten (Terminal 1)
mvn spring-boot:run

# 3. Frontend starten (Terminal 2)
cd frontend
npm install
npm run dev
```

### URLs

- **Frontend (Dev)**: http://localhost:5173 (mit Hot-Reload)
- **Backend API**: http://localhost:2025/api
- **H2 Console**: http://localhost:2025/h2-console

### Production Build (JAR)

```bash
# Komplett-Build (Backend + Frontend in einem JAR)
mvn clean package

# JAR ausführen
java -jar target/fleet-navigator-0.3.0.jar
```

### IDE-Setup

#### IntelliJ IDEA
1. "Open" → fleet-navigator Verzeichnis wählen
2. Maven-Import akzeptieren
3. Run Configuration erstellen:
   - Main Class: `io.javafleet.fleetnavigator.FleetNavigatorApplication`
   - Working Directory: `$MODULE_WORKING_DIR$`

#### VS Code
1. Öffne Projekt-Ordner
2. Installiere Extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Volar (Vue.js)
3. `F5` zum Debuggen

### Detaillierte Entwicklungs-Dokumentation

- [BUILD-PRODUCTION.md](BUILD-PRODUCTION.md) - Production Builds
- [CLAUDE.md](../CLAUDE.md) - Projekt-Architektur
- [FEATURES.md](FEATURES.md) - Feature-Übersicht

---

## Troubleshooting

### Port 2025 bereits belegt

```bash
# Anderen Port verwenden
java -jar fleet-navigator.jar --server.port=8080
# Oder bei systemd:
Environment="SERVER_PORT=8080"
```

### Modelle nicht gefunden

```bash
# Prüfe Modelle-Verzeichnis
ls -la models/

# Setze Pfad manuell
export LLM_LLAMACPP_MODELS_DIR=/pfad/zu/modellen
./fleet-navigator
```

### Zu wenig Speicher

- Verwende kleineres Modell (Llama-3.2-1B statt Qwen2.5-7B)
- Schließe andere Anwendungen
- Überprüfe Systemauslastung

### Datenbank-Fehler

```bash
# Lösche Datenbank (ACHTUNG: Löscht alle Chats!)
rm -rf data/fleetnavdb.mv.db

# Neustart
./fleet-navigator
```

---

## Support & Dokumentation

- **GitHub**: https://github.com/FranzHerstellJavaFleet/fleet-navigator
- **Issues**: https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues
- **Releases**: https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases
- **Kontakt**: franz-martin@java-developer.online

---

**Erstellt von JavaFleet Systems Consulting**
Version 0.3.0 - November 2025
