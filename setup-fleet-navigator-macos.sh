#!/bin/bash

# Fleet Navigator - macOS Setup Script
# ======================================
# Automatische Installation für macOS
#
# Dieses Skript:
# - Prüft Java 21 Installation
# - Erkennt Apple Silicon (M1/M2/M3) vs Intel
# - Lädt llama.cpp Binary herunter
# - Lädt Standard-Modell herunter (Qwen 2.5 3B)
# - Konfiguriert Fleet Navigator
# - Erstellt LaunchAgent (optional)
#
# Verwendung:
#   chmod +x setup-fleet-navigator-macos.sh
#   ./setup-fleet-navigator-macos.sh

set -e

# Konfiguration
INSTALL_DIR="$HOME/Applications/FleetNavigator"
SKIP_MODEL=false
NO_LAUNCHAGENT=false

# Parameter parsing
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-model)
            SKIP_MODEL=true
            shift
            ;;
        --no-launchagent)
            NO_LAUNCHAGENT=true
            shift
            ;;
        --install-dir)
            INSTALL_DIR="$2"
            shift 2
            ;;
        *)
            echo "Unbekannter Parameter: $1"
            echo "Verwendung: $0 [--skip-model] [--no-launchagent] [--install-dir DIR]"
            exit 1
            ;;
    esac
done

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

echo -e "${CYAN}macOS Setup - Automatische Installation${NC}"
echo -e "${BLUE}Version 0.3.0 | JavaFleet Systems Consulting${NC}\n"

# System-Info
print_header "System-Information"

# Erkennung: Apple Silicon vs Intel
ARCH=$(uname -m)
if [[ "$ARCH" == "arm64" ]]; then
    print_info "Prozessor: Apple Silicon (M1/M2/M3/M4)"
    LLAMA_ARCH="arm64"
elif [[ "$ARCH" == "x86_64" ]]; then
    print_info "Prozessor: Intel x86_64"
    LLAMA_ARCH="x86_64"
else
    print_error "Unbekannte Architektur: $ARCH"
    exit 1
fi

# macOS Version
MACOS_VERSION=$(sw_vers -productVersion)
print_info "macOS Version: $MACOS_VERSION"
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
        echo -e "   ${BLUE}brew install openjdk@21${NC}"
        echo -e "   ODER"
        echo -e "   ${BLUE}https://adoptium.net/de/temurin/releases/?version=21${NC}\n"
        exit 1
    fi
else
    print_error "Java nicht gefunden!"
    echo -e "\n${YELLOW}📥 Installiere Java 21 mit Homebrew:${NC}"
    echo -e "   ${BLUE}brew install openjdk@21${NC}"
    echo -e "\n   Oder manuell von:"
    echo -e "   ${BLUE}https://adoptium.net/de/temurin/releases/?version=21${NC}\n"
    exit 1
fi

# Schritt 2: Verzeichnisse erstellen
print_header "Schritt 2/5: Verzeichnisstruktur erstellen"

print_progress "Erstelle Installations-Verzeichnis: $INSTALL_DIR"

DIRECTORIES=(
    "$INSTALL_DIR"
    "$INSTALL_DIR/bin"
    "$INSTALL_DIR/models"
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

# Schritt 3: llama.cpp Binary herunterladen
print_header "Schritt 3/5: llama.cpp Server herunterladen"

# llama.cpp Release URL (macOS)
if [[ "$LLAMA_ARCH" == "arm64" ]]; then
    LLAMA_URL="https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-macos-arm64.zip"
    print_info "Download: llama.cpp für Apple Silicon (ARM64)"
else
    LLAMA_URL="https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-macos-x64.zip"
    print_info "Download: llama.cpp für Intel (x86_64)"
fi

print_info "Quelle: GitHub - ggerganov/llama.cpp"
echo ""

LLAMA_ZIP="/tmp/llama-cpp.zip"
LLAMA_EXTRACT="/tmp/llama-cpp-extracted"

print_progress "Lade llama-server Binary herunter (~30 MB)..."

if curl -L -# "$LLAMA_URL" -o "$LLAMA_ZIP"; then
    FILE_SIZE=$(du -h "$LLAMA_ZIP" | cut -f1)
    print_success "Download abgeschlossen ($FILE_SIZE)"
else
    print_error "Download fehlgeschlagen"
    echo -e "\n${YELLOW}💡 Alternative: Manuell herunterladen von:${NC}"
    echo -e "   ${BLUE}https://github.com/ggerganov/llama.cpp/releases/latest${NC}"
    echo -e "   Datei: llama-b*-bin-macos-$LLAMA_ARCH.zip"
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

    # Kopiere auch andere Binaries (llama-cli etc.)
    find "$LLAMA_EXTRACT" -name "llama-*" -type f -exec cp {} "$INSTALL_DIR/bin/" \;
    chmod +x "$INSTALL_DIR/bin/"llama-*
    print_success "llama.cpp Binaries kopiert"
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
    MODEL_FILE="$INSTALL_DIR/models/qwen2.5-3b-instruct-q4_k_m.gguf"

    print_info "Modell: Qwen 2.5 3B Instruct (empfohlen)"
    print_info "Größe: ~2 GB"
    print_info "Quelle: HuggingFace - Qwen/Qwen2.5-3B-Instruct-GGUF"
    echo ""

    if [[ -f "$MODEL_FILE" ]]; then
        print_success "Modell bereits vorhanden, überspringe Download"
    else
        print_progress "Lade Modell herunter... (Das kann 5-10 Minuten dauern)"
        echo -e "   ${BLUE}💡 Tipp: Zeit für einen Kaffee! ☕${NC}\n"

        if curl -L -# "$MODEL_URL" -o "$MODEL_FILE"; then
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

cat > "$INSTALL_DIR/start-fleet-navigator.sh" << 'EOF'
#!/bin/bash

# Fleet Navigator Starter für macOS

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo ""
echo "========================================"
echo "   Fleet Navigator wird gestartet..."
echo "========================================"
echo ""

# Prüfe llama-server
if [[ ! -f "bin/llama-server" ]]; then
    echo "FEHLER: llama-server nicht gefunden!"
    echo "Bitte Setup erneut ausführen."
    exit 1
fi

# Starte llama-server im Hintergrund
echo "Starte llama-server im Hintergrund..."
bin/llama-server --port 8081 --n-gpu-layers 999 --model models/qwen2.5-3b-instruct-q4_k_m.gguf &
LLAMA_PID=$!

sleep 3

# Starte Fleet Navigator
echo "Starte Fleet Navigator..."
java -jar fleet-navigator.jar

# Cleanup: Stoppe llama-server
echo ""
echo "Stoppe llama-server..."
kill $LLAMA_PID 2>/dev/null

echo "Fleet Navigator wurde beendet."
EOF

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

# LaunchAgent erstellen (optional)
if [[ "$NO_LAUNCHAGENT" == false ]]; then
    print_header "LaunchAgent für Autostart erstellen (optional)"

    PLIST_FILE="$HOME/Library/LaunchAgents/com.javafleet.fleetnavigator.plist"

    cat > "$PLIST_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.javafleet.fleetnavigator</string>
    <key>ProgramArguments</key>
    <array>
        <string>$INSTALL_DIR/start-fleet-navigator.sh</string>
    </array>
    <key>RunAtLoad</key>
    <false/>
    <key>WorkingDirectory</key>
    <string>$INSTALL_DIR</string>
    <key>StandardOutPath</key>
    <string>$INSTALL_DIR/logs/launchagent.log</string>
    <key>StandardErrorPath</key>
    <string>$INSTALL_DIR/logs/launchagent-error.log</string>
</dict>
</plist>
EOF

    print_success "LaunchAgent erstellt (Autostart deaktiviert)"
    echo -e "   ${BLUE}Aktivieren mit: launchctl load $PLIST_FILE${NC}"
fi

# Fertig!
print_header "Installation abgeschlossen!"

cat << EOF

   🎉 Fleet Navigator wurde erfolgreich installiert!

📁 Installations-Verzeichnis:
   $INSTALL_DIR

🚀 Starten:
   cd $INSTALL_DIR
   ./start-fleet-navigator.sh

   ODER als Programm im Finder:
   Doppelklick auf start-fleet-navigator.sh

🌐 Nach dem Start im Browser öffnen:
   http://localhost:2025

📊 Installierte Komponenten:
   ✓ Fleet Navigator JAR
   ✓ llama-server (llama.cpp für $LLAMA_ARCH)
   ✓ Qwen 2.5 3B Modell (2 GB)
   ✓ Konfiguration

💡 Tipps:
   - Beim ersten Start kann es 30-60 Sekunden dauern
   - llama-server lädt das Modell in den Speicher (braucht ~3 GB RAM)
   - Bei Problemen: Logs in $INSTALL_DIR/logs/

🔧 Autostart aktivieren:
   launchctl load ~/Library/LaunchAgents/com.javafleet.fleetnavigator.plist

📚 Dokumentation:
   - README.md im Projekt-Verzeichnis
   - Online: https://github.com/FranzHerstellJavaFleet/fleet-navigator

🆘 Support:
   - GitHub Issues: https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues
   - E-Mail: franz-martin@java-developer.online

EOF

echo -e "${GREEN}✓ Installation erfolgreich abgeschlossen!${NC}\n"
