# Fleet Navigator - Installation Guide

Vollständige Installationsanleitung für alle Plattformen mit **automatischen Setup-Skripten**.

**NEU:** Fleet Navigator läuft jetzt als **Java JAR** - schneller zu bauen, einfacher zu deployen!

---

## 🚀 Schnellstart (Automatische Installation)

Fleet Navigator bietet **automatische Setup-Skripte** für alle Plattformen:

### Windows

```powershell
# PowerShell als Administrator öffnen
.\setup-fleet-navigator.ps1
```

**Das Skript:**
- ✅ Prüft Java 21
- ✅ Lädt llama.cpp herunter
- ✅ Lädt KI-Modell herunter (~2 GB)
- ✅ Konfiguriert alles
- ✅ Erstellt Desktop-Verknüpfung

**Nach Installation:** Doppelklick auf "Fleet Navigator" Desktop-Icon

---

### macOS

```bash
chmod +x setup-fleet-navigator-macos.sh
./setup-fleet-navigator-macos.sh
```

**Das Skript:**
- ✅ Prüft Java 21
- ✅ Erkennt Apple Silicon vs Intel
- ✅ Lädt passende llama.cpp Binary
- ✅ Lädt KI-Modell (~2 GB)
- ✅ Erstellt LaunchAgent

**Nach Installation:**
```bash
cd ~/Applications/FleetNavigator
./start-fleet-navigator.sh
```

---

### Linux

```bash
chmod +x setup-fleet-navigator-linux.sh
./setup-fleet-navigator-linux.sh --systemd  # Mit systemd Service
```

**Das Skript:**
- ✅ Prüft Java 21
- ✅ Lädt llama.cpp für x86_64/ARM64
- ✅ Lädt KI-Modell (~2 GB)
- ✅ Installiert systemd Service (optional)

**Nach Installation:**
```bash
sudo systemctl start fleet-navigator
# Oder manuell:
cd /opt/fleet-navigator
./start-fleet-navigator.sh
```

---

## 📋 Systemanforderungen

### Alle Plattformen

| Komponente | Minimum | Empfohlen |
|------------|---------|-----------|
| **Java** | OpenJDK 21 | OpenJDK 21 |
| **RAM** | 4 GB | 8 GB |
| **Festplatte** | 5 GB frei | 10 GB frei |
| **Prozessor** | Dual-Core | Quad-Core+ |
| **GPU** | Keine | NVIDIA/AMD für schnellere Inferenz |

### Java 21 Installation

**Windows:**
```
https://adoptium.net/de/temurin/releases/?version=21
```

**macOS:**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

**Linux (Fedora):**
```bash
sudo dnf install java-21-openjdk
```

---

## 📦 Inhaltsverzeichnis

1. [Schnellstart (Automatisch)](#-schnellstart-automatische-installation)
2. [Manuelle Installation](#-manuelle-installation)
   - [Windows](#windows-1)
   - [macOS](#macos-1)
   - [Linux Desktop](#linux-desktop)
   - [Linux Server (systemd)](#linux-server-systemd)
3. [Build aus Quellcode](#-build-aus-quellcode)
4. [Konfiguration](#-konfiguration)
5. [Troubleshooting](#-troubleshooting)

---

## 🔧 Manuelle Installation

Falls die automatischen Skripte nicht funktionieren, hier die manuellen Schritte:

### Windows

#### Schritt 1: Java 21 installieren

Download von https://adoptium.net/de/temurin/releases/?version=21

Installieren und Pfad in PATH aufnehmen.

#### Schritt 2: Fleet Navigator JAR herunterladen

```powershell
# Download vom GitHub Release
$url = "https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest/download/fleet-navigator.tar.gz"
Invoke-WebRequest -Uri $url -OutFile fleet-navigator.tar.gz

# Entpacken
tar -xzf fleet-navigator.tar.gz
cd fleet-navigator
```

#### Schritt 3: llama.cpp herunterladen

```powershell
# Neueste llama.cpp Release
$llamaUrl = "https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-win-vulkan-x64.zip"
Invoke-WebRequest -Uri $llamaUrl -OutFile llama.zip

# Entpacken
Expand-Archive llama.zip -DestinationPath bin
```

#### Schritt 4: Modell herunterladen

```powershell
# Qwen 2.5 3B (~2 GB)
mkdir models
$modelUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
Invoke-WebRequest -Uri $modelUrl -OutFile models\qwen2.5-3b-instruct.gguf
```

#### Schritt 5: Starten

```powershell
# llama-server starten
start bin\llama-server.exe --port 8081 --model models\qwen2.5-3b-instruct.gguf

# Fleet Navigator starten
java -jar fleet-navigator.jar
```

Browser öffnen: http://localhost:2025

---

### macOS

#### Schritt 1: Java 21 installieren

```bash
brew install openjdk@21
```

#### Schritt 2: Fleet Navigator JAR herunterladen

```bash
# Download
wget https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest/download/fleet-navigator.tar.gz

# Entpacken
tar -xzf fleet-navigator.tar.gz
cd fleet-navigator
```

#### Schritt 3: llama.cpp herunterladen

```bash
# Apple Silicon (M1/M2/M3)
wget https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-macos-arm64.zip

# ODER Intel Mac
wget https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-macos-x64.zip

# Entpacken
unzip llama-*.zip
mkdir -p bin
mv llama-server bin/
chmod +x bin/llama-server
```

#### Schritt 4: Modell herunterladen

```bash
mkdir -p models
wget -O models/qwen2.5-3b-instruct.gguf \
  https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf
```

#### Schritt 5: Starten

```bash
# llama-server im Hintergrund
bin/llama-server --port 8081 --model models/qwen2.5-3b-instruct.gguf &

# Fleet Navigator
java -jar fleet-navigator.jar
```

Browser öffnen: http://localhost:2025

---

### Linux Desktop

#### Schritt 1: Java 21 installieren

```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openjdk-21-jdk

# Fedora
sudo dnf install java-21-openjdk

# Arch
sudo pacman -S jdk-openjdk
```

#### Schritt 2: Fleet Navigator JAR herunterladen

```bash
wget https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest/download/fleet-navigator.tar.gz
tar -xzf fleet-navigator.tar.gz
cd fleet-navigator
```

#### Schritt 3: llama.cpp herunterladen

```bash
# x86_64
wget https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-ubuntu-x64.zip

# ARM64 (Raspberry Pi etc.)
wget https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-ubuntu-arm64.zip

# Entpacken
unzip llama-*.zip
mkdir -p bin
mv llama-server bin/
chmod +x bin/llama-server
```

#### Schritt 4: Modell herunterladen

```bash
mkdir -p models
wget -O models/qwen2.5-3b-instruct.gguf \
  https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf
```

#### Schritt 5: Starten

```bash
# llama-server im Hintergrund
bin/llama-server --port 8081 --model models/qwen2.5-3b-instruct.gguf &

# Fleet Navigator
java -jar fleet-navigator.jar
```

Browser öffnen: http://localhost:2025

---

### Linux Server (systemd)

Für dauerhafte Server-Installation mit automatischem Start.

#### Automatische Installation (empfohlen)

```bash
sudo ./setup-fleet-navigator-linux.sh --systemd
```

#### Manuelle systemd Installation

```bash
# Als root/sudo
sudo mkdir -p /opt/fleet-navigator/{bin,models,data,logs}

# JAR kopieren
sudo cp fleet-navigator.jar /opt/fleet-navigator/
sudo cp bin/llama-server /opt/fleet-navigator/bin/
sudo cp models/*.gguf /opt/fleet-navigator/models/

# Berechtigungen
sudo chown -R $USER:$USER /opt/fleet-navigator

# systemd Service erstellen
sudo tee /etc/systemd/system/fleet-navigator.service > /dev/null << 'EOF'
[Unit]
Description=Fleet Navigator - Private AI Chat
After=network.target

[Service]
Type=simple
User=YOUR_USER
WorkingDirectory=/opt/fleet-navigator
ExecStart=/opt/fleet-navigator/start-fleet-navigator.sh
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Service aktivieren
sudo systemctl daemon-reload
sudo systemctl enable fleet-navigator
sudo systemctl start fleet-navigator

# Status prüfen
sudo systemctl status fleet-navigator

# Logs anzeigen
sudo journalctl -u fleet-navigator -f
```

---

## 🛠️ Build aus Quellcode

Für Entwickler, die aus dem Quellcode bauen möchten.

### Voraussetzungen

- **Java 21** (JDK)
- **Maven 3.8+**
- **Node.js 18+** und npm
- **Git**

### Build-Schritte

```bash
# Repository klonen
git clone https://github.com/FranzHerstellJavaFleet/fleet-navigator.git
cd fleet-navigator

# Kompletter Build (Frontend + Backend)
mvn clean package

# JAR ist nun in:
# target/fleet-navigator-*.jar

# JAR ausführen
java -jar target/fleet-navigator-*.jar
```

### Entwicklungs-Modus

Für Frontend-Entwicklung mit Hot-Reload:

```bash
# Terminal 1: Backend starten
mvn spring-boot:run

# Terminal 2: Frontend Dev-Server
cd frontend
npm install
npm run dev
```

**URLs:**
- Frontend (Dev): http://localhost:5173 (mit Hot-Reload)
- Backend API: http://localhost:2025/api
- H2 Console: http://localhost:2025/h2-console

---

## ⚙️ Konfiguration

### application.properties

Fleet Navigator kann mit `application.properties` konfiguriert werden:

```properties
# Server Port
server.port=2025

# Datenbank
spring.datasource.url=jdbc:h2:file:./data/fleetnavdb
spring.jpa.hibernate.ddl-auto=update

# llama.cpp Server
llm.llamacpp.server-url=http://localhost:8081
llm.llamacpp.models-dir=./models

# Logging
logging.file.name=./logs/fleet-navigator.log
logging.level.io.javafleet=INFO
```

### Umgebungsvariablen

```bash
# Server Port ändern
export SERVER_PORT=8080

# Modelle-Verzeichnis
export LLM_LLAMACPP_MODELS_DIR=/mnt/models

# Profil
export SPRING_PROFILES_ACTIVE=production
```

### Kommandozeilen-Parameter

```bash
java -jar fleet-navigator.jar \
  --server.port=8080 \
  --llm.llamacpp.server-url=http://localhost:8081 \
  --spring.profiles.active=production
```

---

## 🐛 Troubleshooting

### Java nicht gefunden

```bash
# Prüfe Java Version
java -version

# Sollte zeigen: openjdk version "21.x.x"
```

**Lösung:** Java 21 installieren (siehe [Systemanforderungen](#-systemanforderungen))

---

### Port 2025 bereits belegt

```bash
# Anderen Port verwenden
java -jar fleet-navigator.jar --server.port=8080
```

Oder in `application.properties` ändern:
```properties
server.port=8080
```

---

### llama-server startet nicht

**Problem:** `llama-server: command not found`

**Lösung:**
1. llama.cpp Binary herunterladen (siehe Setup-Skripte)
2. In `bin/` Verzeichnis ablegen
3. Ausführbar machen: `chmod +x bin/llama-server`

---

### Modell nicht gefunden

**Problem:** Fleet Navigator findet kein Modell

**Prüfen:**
```bash
ls -lh models/
# Sollte .gguf Dateien zeigen
```

**Lösung:** Modell herunterladen:
```bash
wget -O models/qwen2.5-3b-instruct.gguf \
  https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf
```

---

### Zu wenig Speicher

**Problem:** `OutOfMemoryError` oder System wird langsam

**Ursache:** LLM-Modell braucht ~3-4 GB RAM

**Lösungen:**
1. **Kleineres Modell verwenden** (z.B. Llama-3.2-1B statt Qwen-3B)
2. **Mehr RAM zuweisen:**
   ```bash
   java -Xmx2G -jar fleet-navigator.jar
   ```
3. **Andere Anwendungen schließen**

---

### Datenbank-Fehler

**Problem:** Fehler beim Starten, Datenbank korrupt

**Lösung:** Datenbank zurücksetzen (⚠️ **Löscht alle Chats!**)
```bash
rm -rf data/fleetnavdb*
java -jar fleet-navigator.jar
```

---

### GPU wird nicht erkannt (CUDA)

**Für NVIDIA GPUs mit CUDA:**

llama-server muss mit CUDA-Support neu gebaut werden:

```bash
# Verwende rebuild-llama-server.sh (Linux)
./rebuild-llama-server.sh
```

Oder manuell:
```bash
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
cmake -B build -DGGML_CUDA=ON
cmake --build build --target llama-server
```

Siehe auch: [GPU-ACTIVATION-GUIDE.md](GPU-ACTIVATION-GUIDE.md)

---

## 📚 Weitere Dokumentation

- **[CLAUDE.md](../CLAUDE.md)** - Projekt-Architektur und Entwickler-Infos
- **[FEATURES.md](FEATURES.md)** - Feature-Übersicht
- **[API-ENDPOINTS.md](API-ENDPOINTS.md)** - REST API Dokumentation
- **[SYSTEMD-DEPLOYMENT.md](SYSTEMD-DEPLOYMENT.md)** - Detaillierte Server-Deployment
- **[GPU-ACTIVATION-GUIDE.md](GPU-ACTIVATION-GUIDE.md)** - GPU-Beschleunigung

---

## 🆘 Support

### GitHub

- **Issues:** https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues
- **Releases:** https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases
- **Wiki:** https://github.com/FranzHerstellJavaFleet/fleet-navigator/wiki

### Kontakt

- **E-Mail:** franz-martin@java-developer.online
- **Website:** https://java-developer.online

---

## 📝 Changelog

### Version 0.3.0 (2024-11)

**Wichtige Änderung:** Von GraalVM Native Image zu **Standard JAR**

**Vorteile:**
- ✅ Schnellere Builds (~3 Min statt 40 Min)
- ✅ Keine Native Image Konfiguration nötig
- ✅ Reflection funktioniert ohne Hints
- ✅ Einfacheres Deployment
- ✅ Benötigt Java 21 Runtime

**Neue Features:**
- ✅ Automatische Setup-Skripte für Windows/macOS/Linux
- ✅ Verbesserte Installations-Dokumentation
- ✅ JAR-basiertes Deployment

---

**Erstellt von:** JavaFleet Systems Consulting
**Version:** 0.3.0
**Datum:** November 2024
**Lizenz:** Siehe LICENSE Datei
