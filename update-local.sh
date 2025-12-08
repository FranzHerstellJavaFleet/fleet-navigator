#!/bin/bash
# Fleet Navigator - Lokales Update-Skript
# Baut das Projekt neu und startet Fleet Navigator im lokalen User-Modus
#
# Daten werden gespeichert in: ~/.java-fleet/
#
# Verwendung:
#   ./update-local.sh              # Build + Neustart
#   ./update-local.sh --skip-build # Nur Neustart (kein Build)

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA_FLEET_DIR="$HOME/.java-fleet"

# Farben
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}   Fleet Navigator - Lokales Update${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# Version aus pom.xml auslesen
get_pom_version() {
    grep -m1 '<version>' "$SCRIPT_DIR/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | head -1
}

VERSION=$(get_pom_version)
echo -e "Version: ${GREEN}${VERSION}${NC}"
echo ""

# =========================================
# Laufende Instanz stoppen
# =========================================
echo -e "${YELLOW}Stoppe laufende Instanz...${NC}"
"$SCRIPT_DIR/start-local.sh" --stop 2>/dev/null || true
echo ""

# =========================================
# Build (wenn nicht --skip-build)
# =========================================
if [ "${1:-}" != "--skip-build" ]; then
    echo -e "${YELLOW}Lösche alte Build-Artefakte...${NC}"
    rm -rf "$SCRIPT_DIR/frontend/dist" "$SCRIPT_DIR/target"
    echo -e "${GREEN}Build-Cache gelöscht${NC}"
    echo ""

    echo -e "${YELLOW}Baue Fleet Navigator...${NC}"
    cd "$SCRIPT_DIR"
    mvn clean package -DskipTests -q

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Build erfolgreich${NC}"
    else
        echo -e "${RED}Build fehlgeschlagen!${NC}"
        exit 1
    fi
    echo ""
else
    echo -e "${YELLOW}Build übersprungen (--skip-build)${NC}"
    echo ""
fi

# =========================================
# JAR prüfen
# =========================================
JAR_PATH=$(find "$SCRIPT_DIR/target" -maxdepth 1 -name "fleet-navigator-*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | head -1)

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}Fehler: JAR nicht gefunden!${NC}"
    exit 1
fi

echo -e "JAR: ${GREEN}$(basename "$JAR_PATH")${NC}"
echo ""

# =========================================
# Verzeichnisstruktur sicherstellen
# =========================================
echo -e "${YELLOW}Prüfe Verzeichnisstruktur...${NC}"
for dir in "$JAVA_FLEET_DIR/data" "$JAVA_FLEET_DIR/models/library" "$JAVA_FLEET_DIR/models/custom" "$JAVA_FLEET_DIR/logs" "$JAVA_FLEET_DIR/config"; do
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        echo -e "  ${GREEN}✓${NC} $dir erstellt"
    fi
done
echo ""

# =========================================
# Modelle-Info anzeigen
# =========================================
MODEL_COUNT=$(find "$JAVA_FLEET_DIR/models" -name "*.gguf" 2>/dev/null | wc -l)
echo -e "${YELLOW}GGUF-Modelle:${NC} $MODEL_COUNT gefunden"
if [ "$MODEL_COUNT" -gt 0 ]; then
    find "$JAVA_FLEET_DIR/models" -name "*.gguf" 2>/dev/null | while read -r model; do
        size=$(du -h "$model" | cut -f1)
        echo -e "  ${GREEN}✓${NC} $(basename "$model") ($size)"
    done
else
    echo -e "  ${YELLOW}Keine Modelle gefunden!${NC}"
    echo -e "  Kopiere GGUF-Modelle nach: $JAVA_FLEET_DIR/models/library/"
fi
echo ""

# =========================================
# Starten
# =========================================
echo -e "${YELLOW}Starte Fleet Navigator im Hintergrund...${NC}"
"$SCRIPT_DIR/start-local.sh" --background

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}   Update auf v${VERSION} erfolgreich!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo -e "URL: ${BLUE}http://localhost:2025${NC}"
echo ""
echo -e "Logs ansehen:   ${YELLOW}tail -f ~/.java-fleet/logs/fleet-navigator.log${NC}"
echo -e "Stoppen:        ${YELLOW}./start-local.sh --stop${NC}"
echo ""
