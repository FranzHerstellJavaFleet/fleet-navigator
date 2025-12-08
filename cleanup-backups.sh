#!/bin/bash
# Fleet Navigator Backup Cleanup Script
# Einmalige Bereinigung alter Backup-Dateien
#
# Dieses Skript räumt alte Backup-Dateien auf und richtet
# die neue Verzeichnisstruktur ein.

set -e

INSTALL_DIR="/opt/fleet-navigator"
BACKUP_DIR="$INSTALL_DIR/backups"
MAX_BACKUPS=5

# Farben
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}   Fleet Navigator Backup Cleanup${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# Prüfen ob als root ausgeführt
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Fehler: Dieses Skript muss als root ausgeführt werden${NC}"
    echo "   Verwende: sudo ./cleanup-backups.sh"
    exit 1
fi

# Backup-Verzeichnis erstellen
echo -e "${YELLOW}1. Erstelle Backup-Verzeichnis...${NC}"
if [ ! -d "$BACKUP_DIR" ]; then
    mkdir -p "$BACKUP_DIR"
    chown trainer:trainer "$BACKUP_DIR"
    echo -e "   ${GREEN}✓${NC} $BACKUP_DIR erstellt"
else
    echo -e "   ${GREEN}✓${NC} $BACKUP_DIR existiert bereits"
fi
echo ""

# Zähle alte Backups im Hauptverzeichnis
legacy_count=$(ls -1 "$INSTALL_DIR"/*.backup* 2>/dev/null | wc -l || echo "0")

if [ "$legacy_count" -gt 0 ]; then
    echo -e "${YELLOW}2. Gefunden: $legacy_count alte Backup-Dateien${NC}"
    echo ""

    # Zeige Größe der alten Backups
    old_size=$(du -sh "$INSTALL_DIR"/*.backup* 2>/dev/null | tail -1 | cut -f1 || echo "0")
    echo -e "   Gesamtgröße: ${RED}$old_size${NC}"
    echo ""

    # Bestätigung einholen
    echo -e "${YELLOW}Diese Dateien werden GELÖSCHT (nicht verschoben):${NC}"
    echo -e "${RED}WARNUNG: Diese Aktion kann nicht rückgängig gemacht werden!${NC}"
    echo ""
    read -p "Fortfahren? (j/N): " confirm

    if [ "$confirm" != "j" ] && [ "$confirm" != "J" ]; then
        echo -e "${YELLOW}Abgebrochen.${NC}"
        exit 0
    fi

    echo ""
    echo -e "${YELLOW}3. Lösche alte Backups...${NC}"

    deleted=0
    for backup in "$INSTALL_DIR"/*.backup*; do
        if [ -f "$backup" ]; then
            rm -f "$backup"
            ((deleted++))
            # Fortschrittsanzeige alle 10 Dateien
            if [ $((deleted % 10)) -eq 0 ]; then
                echo -e "   Gelöscht: $deleted / $legacy_count"
            fi
        fi
    done

    echo -e "   ${GREEN}✓${NC} $deleted Backup-Dateien gelöscht"
else
    echo -e "${GREEN}2. Keine alten Backup-Dateien gefunden.${NC}"
fi
echo ""

# Aktueller Stand
echo -e "${YELLOW}4. Verzeichnisstruktur nach Bereinigung:${NC}"
echo ""
echo -e "   ${BLUE}/opt/fleet-navigator/${NC}"
ls -la "$INSTALL_DIR" | grep -E "^d|\.jar$" | head -20 | while read -r line; do
    echo "      $line"
done
echo ""

# Backup-Verzeichnis anzeigen
if [ -d "$BACKUP_DIR" ]; then
    backup_count=$(ls -1 "$BACKUP_DIR" 2>/dev/null | wc -l || echo "0")
    echo -e "   ${BLUE}$BACKUP_DIR/${NC} ($backup_count Dateien)"
    ls -la "$BACKUP_DIR" 2>/dev/null | head -10 | while read -r line; do
        echo "      $line"
    done
fi
echo ""

# Erfolg
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}   Bereinigung abgeschlossen!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo -e "Zukünftige Updates verwenden automatisch:"
echo -e "  - Backup-Verzeichnis: ${BLUE}$BACKUP_DIR/${NC}"
echo -e "  - Max. Backups: ${GREEN}$MAX_BACKUPS${NC}"
echo ""
