#!/bin/bash

# Fleet Navigator - Linux Setup Script
# ======================================
# Automatische Installation für Linux (Ubuntu, Debian, Fedora, etc.)
#
# Dieses Skript:
# - Prüft Java 21 Installation
# - Lädt llama.cpp Binary herunter
# - Lädt Standard-Modell herunter (Qwen 2.5 3B)
# - Konfiguriert Fleet Navigator
# - Erstellt systemd Service (optional)
#
# Verwendung:
#   chmod +x setup-fleet-navigator-linux.sh
#   ./setup-fleet-navigator-linux.sh
#
#   Für systemd Service (als root):
#   sudo ./setup-fleet-navigator-linux.sh --systemd

set -e

# Konfiguration
INSTALL_DIR="/opt/fleet-navigator"
SKIP_MODEL=false
INSTALL_SYSTEMD=false
USER_INSTALL=false

# Parameter parsing
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-model)
            SKIP_MODEL=true
            shift
            ;;
        --systemd)
            INSTALL_SYSTEMD=true
            shift
            ;;
        --user)
            USER_INSTALL=true
            INSTALL_DIR="$HOME/fleet-navigator"
            shift
            ;;
        --install-dir)
            INSTALL_DIR="$2"
            shift 2
            ;;
        *)
            echo "Unbekannter Parameter: $1"
            echo "Verwendung: $0 [--skip-model] [--systemd] [--user] [--install-dir DIR]"
            exit 1
            ;;
    esac
done

# Prüfe Root-Rechte für systemd
if [[ "$INSTALL_SYSTEMD" == true ]] && [[ $EUID -ne 0 ]]; then
    echo "❌ Für systemd Installation bitte mit sudo ausführen"
    echo "   sudo $0 --systemd"
    exit 1
fi

# Farben
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Funktionen
print_header() {
    echo -e "\n${CYAN}========================================"
    echo -e "$1"
    echo -e "========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_progress() {
    echo -e "${MAGENTA}⏳ $1${NC}"
}

# Banner
clear
cat << "EOF"

 ███████╗██╗     ███████╗███████╗████████╗
 ██╔════╝██║     ██╔════╝██╔════╝╚══██╔══╝
 █████╗  ██║     █████╗  █████╗     ██║
 ██╔══╝  ██║     ██╔══╝  ██╔══╝     ██║
 ██║     ███████╗███████╗███████╗   ██║
 ╚═╝     ╚══════╝╚══════╝╚══════╝   ╚═╝

 ███╗   ██╗ █████╗ ██╗   ██╗██╗ ██████╗  █████╗ ████████╗ ██████╗ ██████╗
 ████╗  ██║██╔══██╗██║   ██║██║██╔════╝ ██╔══██╗╚══██╔══╝██╔═══██╗██╔══██╗
 ██╔██╗ ██║███████║██║   ██║██║██║  ███╗███████║   ██║   ██║   ██║██████╔╝
 ██║╚██╗██║██╔══██║╚██╗ ██╔╝██║██║   ██║██╔══██║   ██║   ██║   ██║██╔══██╗
 ██║ ╚████║██║  ██║ ╚████╔╝ ██║╚██████╔╝██║  ██║   ██║   ╚██████╔╝██║  ██║
 ╚═╝  ╚═══╝╚═╝  ╚═╝  ╚═══╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝

EOF

echo -e "${CYAN}Linux Setup - Automatische Installation${NC}"
echo -e "${BLUE}Version 0.3.0 | JavaFleet Systems Consulting${NC}\n"

# System-Info
print_header "System-Information"

# Linux Distribution
if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    print_info "Distribution: $NAME $VERSION"
else
    print_info "Distribution: Unbekannt"
fi

# Kernel
KERNEL=$(uname -r)
print_info "Kernel: $KERNEL"

# Architektur
ARCH=$(uname -m)
print_info "Architektur: $ARCH"
echo ""

# Schritt 1: Java 21 Prüfung
print_header "Schritt 1/5: Java 21 Überprüfung"

print_progress "Prüfe Java Installation..."

if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)

    if [[ "$JAVA_VERSION" -ge 21 ]]; then
        print_success "Java $JAVA_VERSION gefunden"
        JAVA_PATH=$(which java)
        echo -e "   ${BLUE}Pfad: $JAVA_PATH${NC}"
    else
        print_error "Java $JAVA_VERSION ist zu alt (benötigt: Java 21+)"
        echo -e "\n${YELLOW}📥 Bitte Java 21 installieren:${NC}"
        echo -e "\n   ${BLUE}Ubuntu/Debian:${NC}"
        echo -e "   sudo apt update && sudo apt install openjdk-21-jdk"
        echo -e "\n   ${BLUE}Fedora:${NC}"
        echo -e "   sudo dnf install java-21-openjdk"
        echo -e "\n   ${BLUE}Arch Linux:${NC}"
        echo -e "   sudo pacman -S jdk-openjdk"
        echo -e "\n   ${BLUE}Oder manuell von:${NC}"
        echo -e "   https://adoptium.net/de/temurin/releases/?version=21\n"
        exit 1
    fi
else
    print_error "Java nicht gefunden!"
    echo -e "\n${YELLOW}📥 Installiere Java 21:${NC}"
    echo -e "\n   ${BLUE}Ubuntu/Debian:${NC}"
    echo -e "   sudo apt update && sudo apt install openjdk-21-jdk"
    echo -e "\n   ${BLUE}Fedora:${NC}"
    echo -e "   sudo dnf install java-21-openjdk"
    echo -e "\n   ${BLUE}Arch Linux:${NC}"
    echo -e "   sudo pacman -S jdk-openjdk\n"
    exit 1
fi

# Schritt 2: Verzeichnisse erstellen
print_header "Schritt 2/5: Verzeichnisstruktur erstellen"

print_progress "Erstelle Installations-Verzeichnis: $INSTALL_DIR"

DIRECTORIES=(
    "$INSTALL_DIR"
    "$INSTALL_DIR/bin"
    "$INSTALL_DIR/models"
    "$INSTALL_DIR/models/library"
    "$INSTALL_DIR/models/custom"
    "$INSTALL_DIR/data"
    "$INSTALL_DIR/logs"
)

for dir in "${DIRECTORIES[@]}"; do
    if [[ ! -d "$dir" ]]; then
        mkdir -p "$dir"
        print_success "Erstellt: $dir"
    else
        print_info "Bereits vorhanden: $dir"
    fi
done

# Berechtigungen setzen
if [[ "$INSTALL_SYSTEMD" == true ]]; then
    chown -R $SUDO_USER:$SUDO_USER "$INSTALL_DIR"
    print_success "Berechtigungen gesetzt für User: $SUDO_USER"
fi

# Schritt 3: llama.cpp Binary herunterladen
print_header "Schritt 3/5: llama.cpp Server herunterladen"

# Architektur-spezifische URL
if [[ "$ARCH" == "x86_64" ]]; then
    LLAMA_URL="https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-ubuntu-x64.zip"
    print_info "Download: llama.cpp für x86_64 (Ubuntu)"
elif [[ "$ARCH" == "aarch64" ]] || [[ "$ARCH" == "arm64" ]]; then
    LLAMA_URL="https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-ubuntu-arm64.zip"
    print_info "Download: llama.cpp für ARM64"
else
    print_error "Architektur $ARCH wird nicht unterstützt"
    echo -e "\n${YELLOW}💡 Alternative: llama.cpp selbst bauen${NC}"
    echo -e "   ${BLUE}https://github.com/ggerganov/llama.cpp${NC}\n"
    exit 1
fi

print_info "Quelle: GitHub - ggerganov/llama.cpp"
echo ""

LLAMA_ZIP="/tmp/llama-cpp.zip"
LLAMA_EXTRACT="/tmp/llama-cpp-extracted"

print_progress "Lade llama-server Binary herunter (~40 MB)..."

if wget --progress=bar:force "$LLAMA_URL" -O "$LLAMA_ZIP" 2>&1 | tail -n 7; then
    FILE_SIZE=$(du -h "$LLAMA_ZIP" | cut -f1)
    print_success "Download abgeschlossen ($FILE_SIZE)"
else
    print_error "Download fehlgeschlagen"
    echo -e "\n${YELLOW}💡 Alternative: Manuell herunterladen von:${NC}"
    echo -e "   ${BLUE}https://github.com/ggerganov/llama.cpp/releases/latest${NC}"
    echo -e "   Datei: llama-b*-bin-ubuntu-$ARCH.zip"
    echo -e "   Entpacken nach: $INSTALL_DIR/bin/${NC}\n"
    exit 1
fi

print_progress "Entpacke Archive..."

# Cleanup
rm -rf "$LLAMA_EXTRACT"
mkdir -p "$LLAMA_EXTRACT"

unzip -q "$LLAMA_ZIP" -d "$LLAMA_EXTRACT"

# Finde llama-server Binary
LLAMA_SERVER=$(find "$LLAMA_EXTRACT" -name "llama-server" -type f | head -n 1)

if [[ -f "$LLAMA_SERVER" ]]; then
    cp "$LLAMA_SERVER" "$INSTALL_DIR/bin/llama-server"
    chmod +x "$INSTALL_DIR/bin/llama-server"
    print_success "llama-server installiert"

    # Kopiere auch andere Binaries und Libraries
    find "$LLAMA_EXTRACT" -name "llama-*" -type f -exec cp {} "$INSTALL_DIR/bin/" \; 2>/dev/null || true
    find "$LLAMA_EXTRACT" -name "*.so*" -type f -exec cp {} "$INSTALL_DIR/bin/" \; 2>/dev/null || true
    chmod +x "$INSTALL_DIR/bin/"llama-* 2>/dev/null || true
    print_success "llama.cpp Binaries und Libraries kopiert"
else
    print_error "llama-server nicht gefunden im Archive"
    exit 1
fi

# Cleanup
rm -f "$LLAMA_ZIP"
rm -rf "$LLAMA_EXTRACT"

# Schritt 4: Fleet Navigator JAR kopieren
print_header "Schritt 4/5: Fleet Navigator JAR"

JAR_FILE=$(find target -name "fleet-navigator-*.jar" -type f 2>/dev/null | head -n 1)

if [[ -f "$JAR_FILE" ]]; then
    print_progress "Kopiere Fleet Navigator JAR..."
    cp "$JAR_FILE" "$INSTALL_DIR/fleet-navigator.jar"
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "fleet-navigator.jar installiert ($JAR_SIZE)"
else
    print_error "fleet-navigator.jar nicht gefunden in target/"
    echo -e "\n${YELLOW}⚠️  Bitte zuerst bauen mit:${NC}"
    echo -e "   ${BLUE}mvn clean package${NC}\n"
    exit 1
fi

# Schritt 5: Modell herunterladen (optional)
print_header "Schritt 5/5: KI-Modell herunterladen"

if [[ "$SKIP_MODEL" == true ]]; then
    print_info "Modell-Download übersprungen (--skip-model)"
    echo -e "   ${BLUE}Modell kann später in der Anwendung heruntergeladen werden.${NC}\n"
else
    MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
    MODEL_FILE="$INSTALL_DIR/models/library/qwen2.5-3b-instruct-q4_k_m.gguf"

    print_info "Modell: Qwen 2.5 3B Instruct (empfohlen)"
    print_info "Größe: ~2 GB"
    print_info "Quelle: HuggingFace - Qwen/Qwen2.5-3B-Instruct-GGUF"
    echo ""

    if [[ -f "$MODEL_FILE" ]]; then
        print_success "Modell bereits vorhanden, überspringe Download"
    else
        print_progress "Lade Modell herunter... (Das kann 5-10 Minuten dauern)"
        echo -e "   ${BLUE}💡 Tipp: Perfekte Zeit für einen Kaffee! ☕${NC}\n"

        if wget --progress=bar:force "$MODEL_URL" -O "$MODEL_FILE" 2>&1 | tail -n 7; then
            MODEL_SIZE=$(du -h "$MODEL_FILE" | cut -f1)
            print_success "Modell heruntergeladen ($MODEL_SIZE)"
        else
            print_error "Fehler beim Download"
            echo -e "\n${YELLOW}💡 Alternative: Modell manuell herunterladen von:${NC}"
            echo -e "   ${BLUE}$MODEL_URL${NC}"
            echo -e "   Speichern als: ${BLUE}$MODEL_FILE${NC}\n"
        fi
    fi
fi

# Start-Skript erstellen
print_header "Start-Skript erstellen"

cat > "$INSTALL_DIR/start-fleet-navigator.sh" << 'EOFSTART'
#!/bin/bash

# Fleet Navigator Starter für Linux

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo ""
echo "========================================"
echo "   Fleet Navigator wird gestartet..."
echo "========================================"
echo ""

# Prüfe Java
if ! command -v java &> /dev/null; then
    echo "FEHLER: Java nicht gefunden!"
    echo "Bitte Java 21 installieren."
    exit 1
fi

# Prüfe llama-server
if [[ ! -f "bin/llama-server" ]]; then
    echo "FEHLER: llama-server nicht gefunden!"
    echo "Bitte Setup erneut ausführen."
    exit 1
fi

# Finde Modell
MODEL=$(find models -name "*.gguf" -type f | head -n 1)
if [[ -z "$MODEL" ]]; then
    echo "WARNUNG: Kein Modell gefunden in models/"
    echo "Fleet Navigator startet ohne lokales LLM."
    sleep 2
else
    echo "Gefundenes Modell: $MODEL"

    # Starte llama-server im Hintergrund
    echo "Starte llama-server im Hintergrund..."
    export LD_LIBRARY_PATH="$SCRIPT_DIR/bin:$LD_LIBRARY_PATH"
    bin/llama-server --port 8081 --n-gpu-layers 999 --model "$MODEL" &
    LLAMA_PID=$!

    sleep 3
fi

# Starte Fleet Navigator
echo "Starte Fleet Navigator..."
echo ""
java -jar fleet-navigator.jar

# Cleanup: Stoppe llama-server
if [[ -n "$LLAMA_PID" ]]; then
    echo ""
    echo "Stoppe llama-server..."
    kill $LLAMA_PID 2>/dev/null
fi

echo "Fleet Navigator wurde beendet."
EOFSTART

chmod +x "$INSTALL_DIR/start-fleet-navigator.sh"
print_success "Start-Skript erstellt: start-fleet-navigator.sh"

# Konfigurationsdatei
print_header "Konfiguration erstellen"

cat > "$INSTALL_DIR/application.properties" << EOF
# Fleet Navigator Configuration
# ==============================

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
EOF

print_success "Konfiguration erstellt: application.properties"

# systemd Service (optional)
if [[ "$INSTALL_SYSTEMD" == true ]]; then
    print_header "systemd Service erstellen"

    cat > /etc/systemd/system/fleet-navigator.service << EOF
[Unit]
Description=Fleet Navigator - Private AI Chat Interface
After=network.target

[Service]
Type=simple
User=$SUDO_USER
WorkingDirectory=$INSTALL_DIR
ExecStart=$INSTALL_DIR/start-fleet-navigator.sh
Restart=on-failure
RestartSec=10

# Environment
Environment="JAVA_HOME=$JAVA_HOME"
Environment="LD_LIBRARY_PATH=$INSTALL_DIR/bin"

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=fleet-navigator

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    print_success "systemd Service erstellt: fleet-navigator.service"

    echo -e "\n${YELLOW}Service Befehle:${NC}"
    echo -e "   ${BLUE}sudo systemctl start fleet-navigator${NC}   - Service starten"
    echo -e "   ${BLUE}sudo systemctl stop fleet-navigator${NC}    - Service stoppen"
    echo -e "   ${BLUE}sudo systemctl enable fleet-navigator${NC}  - Autostart aktivieren"
    echo -e "   ${BLUE}sudo systemctl status fleet-navigator${NC}  - Status prüfen"
    echo -e "   ${BLUE}sudo journalctl -u fleet-navigator -f${NC}  - Logs anzeigen\n"
fi

# Fertig!
print_header "Installation abgeschlossen!"

cat << EOF

   🎉 Fleet Navigator wurde erfolgreich installiert!

📁 Installations-Verzeichnis:
   $INSTALL_DIR

🚀 Starten:
EOF

if [[ "$INSTALL_SYSTEMD" == true ]]; then
    echo -e "   ${BLUE}sudo systemctl start fleet-navigator${NC}"
    echo -e "   ODER"
fi

cat << EOF
   cd $INSTALL_DIR
   ./start-fleet-navigator.sh

🌐 Nach dem Start im Browser öffnen:
   http://localhost:2025

📊 Installierte Komponenten:
   ✓ Fleet Navigator JAR
   ✓ llama-server (llama.cpp für $ARCH)
EOF

if [[ "$SKIP_MODEL" == false ]]; then
    echo "   ✓ Qwen 2.5 3B Modell (2 GB)"
fi

cat << EOF
   ✓ Konfiguration

💡 Tipps:
   - Beim ersten Start kann es 30-60 Sekunden dauern
   - llama-server lädt das Modell in den Speicher (braucht ~3 GB RAM)
   - Bei Problemen: Logs in $INSTALL_DIR/logs/
   - GPU-Support: llama-server mit CUDA neu bauen (siehe Doku)

📚 Dokumentation:
   - README.md im Projekt-Verzeichnis
   - Online: https://github.com/FranzHerstellJavaFleet/fleet-navigator

🆘 Support:
   - GitHub Issues: https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues
   - E-Mail: franz-martin@java-developer.online

EOF

echo -e "${GREEN}✓ Installation erfolgreich abgeschlossen!${NC}\n"
