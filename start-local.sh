#!/bin/bash
# Fleet Navigator - Lokales Start-Skript
# Startet Fleet Navigator UND Fleet-Mate im lokalen User-Modus
#
# Daten werden gespeichert in: ~/.java-fleet/
#   - ~/.java-fleet/data/         (Datenbank)
#   - ~/.java-fleet/models/       (GGUF-Modelle)
#   - ~/.java-fleet/logs/         (Log-Dateien)
#   - ~/.java-fleet/config/       (Benutzer-Konfiguration)
#
# Verwendung:
#   ./start-local.sh              # Startet im Vordergrund (Navigator + Mate)
#   ./start-local.sh --background # Startet im Hintergrund (Navigator + Mate)
#   ./start-local.sh --stop       # Stoppt Navigator + Mate
#   ./start-local.sh --no-mate    # Startet NUR Navigator (ohne Mate)

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA_FLEET_DIR="$HOME/.java-fleet"
LOG_DIR="$JAVA_FLEET_DIR/logs"
PID_FILE="$JAVA_FLEET_DIR/fleet-navigator.pid"
MATE_PID_FILE="$JAVA_FLEET_DIR/fleet-mate.pid"
PORT=2025

# Fleet-Mate Pfad (relativ zum Fleet-Navigator)
MATE_DIR="$SCRIPT_DIR/../Fleet-Mate-Linux"
MATE_BINARY="$MATE_DIR/fleet-mate"

# Farben
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# =========================================
# Verzeichnisstruktur sicherstellen
# =========================================
ensure_directory_structure() {
    for dir in "$JAVA_FLEET_DIR/data" "$JAVA_FLEET_DIR/models/library" "$JAVA_FLEET_DIR/models/custom" "$LOG_DIR" "$JAVA_FLEET_DIR/config"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            echo -e "  ${GREEN}✓${NC} $dir erstellt"
        fi
    done
}

# =========================================
# Fleet-Mate Funktionen
# =========================================
is_mate_running() {
    if [ -f "$MATE_PID_FILE" ]; then
        local pid=$(cat "$MATE_PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        fi
    fi
    # Prüfe auch via pgrep
    if pgrep -f "fleet-mate" > /dev/null 2>&1; then
        return 0
    fi
    return 1
}

start_fleet_mate() {
    if ! [ -x "$MATE_BINARY" ]; then
        echo -e "${YELLOW}Hinweis: Fleet-Mate nicht gefunden in $MATE_DIR${NC}"
        return 1
    fi

    if is_mate_running; then
        echo -e "${GREEN}Fleet-Mate läuft bereits${NC}"
        return 0
    fi

    echo -e "${YELLOW}Starte Fleet-Mate...${NC}"
    cd "$MATE_DIR"
    nohup "$MATE_BINARY" > "$LOG_DIR/fleet-mate.log" 2>&1 &
    echo $! > "$MATE_PID_FILE"
    cd "$SCRIPT_DIR"

    sleep 1
    if is_mate_running; then
        echo -e "${GREEN}Fleet-Mate gestartet (PID: $(cat "$MATE_PID_FILE"))${NC}"
        return 0
    else
        echo -e "${RED}Fleet-Mate konnte nicht gestartet werden${NC}"
        return 1
    fi
}

stop_fleet_mate() {
    echo -e "${YELLOW}Stoppe Fleet-Mate...${NC}"

    # PID-basiert stoppen
    if [ -f "$MATE_PID_FILE" ]; then
        local pid=$(cat "$MATE_PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            kill "$pid" 2>/dev/null || true
            sleep 1
        fi
        rm -f "$MATE_PID_FILE"
    fi

    # pgrep-basiert stoppen (falls PID-File nicht korrekt)
    local mate_pid=$(pgrep -f "fleet-mate" 2>/dev/null || true)
    if [ -n "$mate_pid" ]; then
        kill "$mate_pid" 2>/dev/null || true
    fi

    echo -e "${GREEN}Fleet-Mate gestoppt${NC}"
}

# =========================================
# JAR finden
# =========================================
find_jar() {
    # Versuche versioniertes JAR zu finden
    local jar=$(find "$SCRIPT_DIR/target" -maxdepth 1 -name "fleet-navigator-*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | head -1)

    if [ -n "$jar" ] && [ -f "$jar" ]; then
        echo "$jar"
    else
        echo ""
    fi
}

# =========================================
# Prüfen ob bereits läuft
# =========================================
is_running() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        fi
    fi
    # Prüfe auch Port
    if lsof -i:$PORT -t > /dev/null 2>&1; then
        return 0
    fi
    return 1
}

# =========================================
# Stop-Funktion
# =========================================
stop_fleet_navigator() {
    echo -e "${YELLOW}Stoppe Fleet Navigator...${NC}"

    # PID-basiert stoppen
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            kill "$pid" 2>/dev/null || true
            sleep 2
            # Force kill wenn nötig
            if ps -p "$pid" > /dev/null 2>&1; then
                kill -9 "$pid" 2>/dev/null || true
            fi
        fi
        rm -f "$PID_FILE"
    fi

    # Port-basiert stoppen
    local port_pid=$(lsof -i:$PORT -t 2>/dev/null || true)
    if [ -n "$port_pid" ]; then
        kill "$port_pid" 2>/dev/null || true
        sleep 2
        port_pid=$(lsof -i:$PORT -t 2>/dev/null || true)
        if [ -n "$port_pid" ]; then
            kill -9 "$port_pid" 2>/dev/null || true
        fi
    fi

    echo -e "${GREEN}Fleet Navigator gestoppt${NC}"

    # Auch Fleet-Mate stoppen
    if is_mate_running; then
        stop_fleet_mate
    fi
}

# =========================================
# Status anzeigen
# =========================================
show_status() {
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE}   Fleet Navigator + Mate Status${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""

    # Navigator Status
    echo -e "${YELLOW}Fleet Navigator:${NC}"
    if is_running; then
        echo -e "  Status: ${GREEN}Läuft${NC}"
        if [ -f "$PID_FILE" ]; then
            echo -e "  PID: $(cat "$PID_FILE")"
        fi
        echo -e "  URL: ${BLUE}http://localhost:$PORT${NC}"
    else
        echo -e "  Status: ${RED}Gestoppt${NC}"
    fi
    echo ""

    # Mate Status
    echo -e "${YELLOW}Fleet-Mate:${NC}"
    if is_mate_running; then
        echo -e "  Status: ${GREEN}Läuft${NC}"
        local mate_pid=$(pgrep -f "fleet-mate" 2>/dev/null || cat "$MATE_PID_FILE" 2>/dev/null || echo "?")
        echo -e "  PID: $mate_pid"
    else
        echo -e "  Status: ${RED}Gestoppt${NC}"
    fi
    echo ""

    echo -e "Datenverzeichnis: $JAVA_FLEET_DIR"
    echo -e "Modelle: $JAVA_FLEET_DIR/models/"
    echo -e "Logs: $LOG_DIR/"
}

# =========================================
# Hauptlogik
# =========================================

# Flag für --no-mate Option
START_MATE=true
if [[ "${1:-}" == "--no-mate" ]] || [[ "${2:-}" == "--no-mate" ]]; then
    START_MATE=false
    # Entferne --no-mate aus den Argumenten
    set -- "${@/--no-mate/}"
fi

case "${1:-}" in
    --stop)
        stop_fleet_navigator
        exit 0
        ;;
    --status)
        show_status
        exit 0
        ;;
    --help|-h)
        echo "Fleet Navigator - Lokales Start-Skript"
        echo ""
        echo "Verwendung:"
        echo "  ./start-local.sh              # Startet Navigator + Mate im Vordergrund"
        echo "  ./start-local.sh --background # Startet Navigator + Mate im Hintergrund"
        echo "  ./start-local.sh --stop       # Stoppt Navigator + Mate"
        echo "  ./start-local.sh --status     # Zeigt Status"
        echo "  ./start-local.sh --no-mate    # Startet NUR Navigator (ohne Mate)"
        echo ""
        echo "Datenverzeichnis: ~/.java-fleet/"
        echo "Fleet-Mate Pfad: $MATE_DIR"
        exit 0
        ;;
esac

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}   Fleet Navigator - Lokaler Modus${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# Prüfen ob bereits läuft
if is_running; then
    echo -e "${YELLOW}Fleet Navigator läuft bereits!${NC}"
    echo -e "URL: ${BLUE}http://localhost:$PORT${NC}"
    echo ""
    echo "Zum Stoppen: ./start-local.sh --stop"
    exit 0
fi

# JAR finden
JAR_PATH=$(find_jar)

if [ -z "$JAR_PATH" ]; then
    echo -e "${RED}Fehler: JAR nicht gefunden!${NC}"
    echo "Führe zuerst aus: mvn clean package -DskipTests"
    exit 1
fi

echo -e "JAR: ${GREEN}$(basename "$JAR_PATH")${NC}"
echo ""

# Verzeichnisstruktur sicherstellen
echo -e "${YELLOW}Prüfe Verzeichnisstruktur...${NC}"
ensure_directory_structure
echo ""

# Modelle prüfen
MODEL_COUNT=$(find "$JAVA_FLEET_DIR/models" -name "*.gguf" 2>/dev/null | wc -l)
if [ "$MODEL_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}Hinweis: Keine GGUF-Modelle gefunden in $JAVA_FLEET_DIR/models/${NC}"
    echo "Kopiere deine Modelle nach: $JAVA_FLEET_DIR/models/library/"
    echo ""
fi

# Fleet-Mate starten (wenn gewünscht)
if [ "$START_MATE" = true ]; then
    echo -e "${YELLOW}Starte Fleet-Mate...${NC}"
    start_fleet_mate || echo -e "${YELLOW}Fleet-Mate nicht verfügbar - Navigator startet trotzdem${NC}"
    echo ""
fi

# Starten
if [ "${1:-}" = "--background" ]; then
    echo -e "${YELLOW}Starte Fleet Navigator im Hintergrund...${NC}"
    nohup java -jar "$JAR_PATH" --spring.profiles.active=production > "$LOG_DIR/fleet-navigator.log" 2>&1 &
    echo $! > "$PID_FILE"

    echo -e "${GREEN}Fleet Navigator gestartet (PID: $(cat "$PID_FILE"))${NC}"
    echo ""
    echo -e "URL: ${BLUE}http://localhost:$PORT${NC}"
    echo -e "Logs: tail -f $LOG_DIR/fleet-navigator.log"
    if [ "$START_MATE" = true ]; then
        echo -e "Mate-Logs: tail -f $LOG_DIR/fleet-mate.log"
    fi
    echo ""
    echo "Zum Stoppen: ./start-local.sh --stop"
else
    echo -e "${YELLOW}Starte Fleet Navigator im Vordergrund...${NC}"
    echo -e "Drücke Ctrl+C zum Beenden"
    echo ""
    echo -e "URL: ${BLUE}http://localhost:$PORT${NC}"
    echo ""

    # Vordergrund mit Trap für sauberes Beenden (inkl. Mate)
    cleanup() {
        echo ""
        echo -e "${YELLOW}Beende Fleet Navigator...${NC}"
        if [ "$START_MATE" = true ] && is_mate_running; then
            stop_fleet_mate
        fi
        exit 0
    }
    trap cleanup INT TERM

    java -jar "$JAR_PATH" --spring.profiles.active=production
fi
