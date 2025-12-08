#!/bin/bash
# Fleet Navigator - Systemd Cleanup-Skript
# Entfernt den systemd-Service und /opt/fleet-navigator/
#
# WICHTIG: Mit sudo ausführen!
# Verwendung: sudo ./cleanup-systemd.sh

set -e

# Farben
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}   Fleet Navigator - Systemd Cleanup${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# Prüfen ob als root ausgeführt
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Fehler: Dieses Skript muss als root ausgeführt werden${NC}"
    echo "   Verwende: sudo ./cleanup-systemd.sh"
    exit 1
fi

SERVICE_NAME="fleet-navigator"
INSTALL_DIR="/opt/fleet-navigator"

# =========================================
# Service Status prüfen
# =========================================
echo -e "${YELLOW}Prüfe Service Status...${NC}"
if systemctl is-active --quiet "$SERVICE_NAME" 2>/dev/null; then
    echo -e "Service läuft: ${GREEN}Ja${NC}"
    RUNNING=true
else
    echo -e "Service läuft: ${YELLOW}Nein${NC}"
    RUNNING=false
fi

if systemctl is-enabled --quiet "$SERVICE_NAME" 2>/dev/null; then
    echo -e "Service enabled: ${GREEN}Ja${NC}"
    ENABLED=true
else
    echo -e "Service enabled: ${YELLOW}Nein${NC}"
    ENABLED=false
fi
echo ""

# =========================================
# Service stoppen
# =========================================
if [ "$RUNNING" = true ]; then
    echo -e "${YELLOW}Stoppe Service...${NC}"
    systemctl stop "$SERVICE_NAME" 2>/dev/null || true
    echo -e "${GREEN}Service gestoppt${NC}"
    echo ""
fi

# =========================================
# Service deaktivieren
# =========================================
if [ "$ENABLED" = true ]; then
    echo -e "${YELLOW}Deaktiviere Service...${NC}"
    systemctl disable "$SERVICE_NAME" 2>/dev/null || true
    echo -e "${GREEN}Service deaktiviert${NC}"
    echo ""
fi

# =========================================
# Service-Datei entfernen
# =========================================
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
if [ -f "$SERVICE_FILE" ]; then
    echo -e "${YELLOW}Entferne Service-Datei...${NC}"
    rm -f "$SERVICE_FILE"
    systemctl daemon-reload
    echo -e "${GREEN}Service-Datei entfernt${NC}"
    echo ""
fi

# =========================================
# /opt/fleet-navigator/ entfernen
# =========================================
if [ -d "$INSTALL_DIR" ]; then
    echo -e "${YELLOW}Prüfe /opt/fleet-navigator/...${NC}"

    # Zähle verbleibende Dateien
    FILE_COUNT=$(find "$INSTALL_DIR" -type f 2>/dev/null | wc -l)
    DIR_SIZE=$(du -sh "$INSTALL_DIR" 2>/dev/null | cut -f1)

    echo -e "  Dateien: $FILE_COUNT"
    echo -e "  Größe: $DIR_SIZE"
    echo ""

    # Bestätigung
    echo -e "${YELLOW}Möchtest du /opt/fleet-navigator/ löschen? [j/N]${NC}"
    read -r response

    if [[ "$response" =~ ^[jJyY]$ ]]; then
        echo -e "${YELLOW}Lösche /opt/fleet-navigator/...${NC}"
        rm -rf "$INSTALL_DIR"
        echo -e "${GREEN}/opt/fleet-navigator/ gelöscht${NC}"
    else
        echo -e "${YELLOW}Übersprungen${NC}"
    fi
    echo ""
else
    echo -e "${YELLOW}/opt/fleet-navigator/ existiert nicht${NC}"
    echo ""
fi

# =========================================
# Zusammenfassung
# =========================================
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}   Cleanup abgeschlossen!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo -e "Fleet Navigator ist jetzt im ${BLUE}lokalen Modus${NC}."
echo ""
echo -e "Starten:  ${YELLOW}./start-local.sh${NC}"
echo -e "Stoppen:  ${YELLOW}./start-local.sh --stop${NC}"
echo -e "Update:   ${YELLOW}./update-local.sh${NC}"
echo ""
echo -e "Daten:    ${BLUE}~/.java-fleet/${NC}"
echo -e "Modelle:  ${BLUE}~/.java-fleet/models/library/${NC}"
echo ""
