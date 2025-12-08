#!/bin/bash
# =====================================================
# Fleet Navigator - Linux Uninstall Script
# =====================================================
# Entfernt Fleet Navigator vollständig vom System
# Verwendung: sudo ./uninstall.sh [--keep-data]
#
# Optionen:
#   --keep-data    Behält Benutzerdaten (Chats, Modelle, etc.)
# =====================================================

set -e

# Farben für Ausgabe
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                                                        ║${NC}"
echo -e "${CYAN}║   🚢 Fleet Navigator - Deinstallation                  ║${NC}"
echo -e "${CYAN}║                                                        ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Parameter prüfen
KEEP_DATA=false
if [[ "$1" == "--keep-data" ]]; then
    KEEP_DATA=true
    echo -e "${YELLOW}INFO: Benutzerdaten werden beibehalten${NC}"
fi

# Prüfen ob als root ausgeführt (für DEB-Deinstallation)
if [[ $EUID -ne 0 ]]; then
    echo -e "${YELLOW}HINWEIS: Ohne root-Rechte - nur Benutzerdaten werden entfernt${NC}"
    echo ""
fi

# 1. Fleet Navigator Prozess stoppen
echo -e "${CYAN}[1/5]${NC} Stoppe Fleet Navigator Prozesse..."
pkill -f "fleet-navigator.*jar" 2>/dev/null && echo -e "${GREEN}  ✓ Prozess gestoppt${NC}" || echo -e "${YELLOW}  - Kein laufender Prozess${NC}"
pkill -f "llama-server" 2>/dev/null && echo -e "${GREEN}  ✓ llama-server gestoppt${NC}" || echo -e "${YELLOW}  - Kein llama-server Prozess${NC}"

# 2. DEB-Paket entfernen (falls installiert)
echo -e "${CYAN}[2/5]${NC} Prüfe DEB-Paket Installation..."
if dpkg -l | grep -q "fleet-navigator"; then
    if [[ $EUID -eq 0 ]]; then
        echo "  Entferne DEB-Paket..."
        apt-get remove -y fleet-navigator 2>/dev/null || dpkg --remove fleet-navigator 2>/dev/null
        echo -e "${GREEN}  ✓ DEB-Paket entfernt${NC}"
    else
        echo -e "${YELLOW}  ! DEB-Paket gefunden - bitte mit sudo ausführen zum Entfernen${NC}"
    fi
else
    echo -e "${YELLOW}  - Kein DEB-Paket installiert${NC}"
fi

# 3. Installationsverzeichnis entfernen
echo -e "${CYAN}[3/5]${NC} Entferne Installationsverzeichnisse..."

# /opt/fleet-navigator (DEB-Installation)
if [[ -d "/opt/fleet-navigator" ]]; then
    if [[ $EUID -eq 0 ]]; then
        rm -rf /opt/fleet-navigator
        echo -e "${GREEN}  ✓ /opt/fleet-navigator entfernt${NC}"
    else
        echo -e "${YELLOW}  ! /opt/fleet-navigator gefunden - bitte mit sudo ausführen${NC}"
    fi
fi

# Symlink in /usr/bin
if [[ -L "/usr/bin/fleet-navigator" ]] || [[ -f "/usr/bin/fleet-navigator" ]]; then
    if [[ $EUID -eq 0 ]]; then
        rm -f /usr/bin/fleet-navigator
        echo -e "${GREEN}  ✓ /usr/bin/fleet-navigator entfernt${NC}"
    fi
fi

# 4. Desktop-Einträge entfernen
echo -e "${CYAN}[4/5]${NC} Entferne Desktop-Einträge..."

# System-weite Desktop-Datei
if [[ -f "/usr/share/applications/fleet-navigator.desktop" ]]; then
    if [[ $EUID -eq 0 ]]; then
        rm -f /usr/share/applications/fleet-navigator.desktop
        echo -e "${GREEN}  ✓ Desktop-Eintrag (System) entfernt${NC}"
    fi
fi

# Benutzer-Desktop-Datei
if [[ -f "$HOME/.local/share/applications/fleet-navigator.desktop" ]]; then
    rm -f "$HOME/.local/share/applications/fleet-navigator.desktop"
    echo -e "${GREEN}  ✓ Desktop-Eintrag (Benutzer) entfernt${NC}"
fi

# Icon
if [[ -f "/usr/share/icons/hicolor/256x256/apps/fleet-navigator.png" ]]; then
    if [[ $EUID -eq 0 ]]; then
        rm -f /usr/share/icons/hicolor/256x256/apps/fleet-navigator.png
        gtk-update-icon-cache -q /usr/share/icons/hicolor 2>/dev/null || true
        echo -e "${GREEN}  ✓ Icon entfernt${NC}"
    fi
fi

# 5. Benutzerdaten entfernen
echo -e "${CYAN}[5/5]${NC} Benutzerdaten..."

DATA_DIR="$HOME/.java-fleet"

if [[ -d "$DATA_DIR" ]]; then
    if [[ "$KEEP_DATA" == true ]]; then
        echo -e "${YELLOW}  ! Benutzerdaten beibehalten: $DATA_DIR${NC}"
        echo "    Enthält: Chats, Modelle, Einstellungen, Datenbank"
    else
        echo ""
        echo -e "${RED}  ⚠️  WARNUNG: Alle Benutzerdaten werden gelöscht!${NC}"
        echo "    Verzeichnis: $DATA_DIR"
        echo "    Enthält:"
        echo "      - Alle Chat-Verläufe"
        echo "      - Heruntergeladene KI-Modelle (können mehrere GB sein)"
        echo "      - Experten-Konfigurationen"
        echo "      - Datenbank"
        echo ""
        read -p "    Wirklich löschen? (j/N): " confirm
        if [[ "$confirm" == "j" ]] || [[ "$confirm" == "J" ]]; then
            rm -rf "$DATA_DIR"
            echo -e "${GREEN}  ✓ Benutzerdaten entfernt${NC}"
        else
            echo -e "${YELLOW}  - Benutzerdaten beibehalten${NC}"
        fi
    fi
else
    echo -e "${YELLOW}  - Keine Benutzerdaten gefunden${NC}"
fi

# Autostart entfernen (falls in .bashrc)
if grep -q "fleet-navigator" "$HOME/.bashrc" 2>/dev/null; then
    echo ""
    echo -e "${YELLOW}HINWEIS: Autostart-Eintrag in ~/.bashrc gefunden${NC}"
    echo "  Bitte manuell entfernen falls gewünscht:"
    echo "  nano ~/.bashrc"
fi

# Zusammenfassung
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                                                        ║${NC}"
echo -e "${GREEN}║   ✓ Fleet Navigator wurde deinstalliert               ║${NC}"
echo -e "${GREEN}║                                                        ║${NC}"
if [[ "$KEEP_DATA" == true ]] || [[ -d "$DATA_DIR" ]]; then
echo -e "${GREEN}║   Benutzerdaten: $DATA_DIR${NC}"
fi
echo -e "${GREEN}║                                                        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Hinweis für DEB
if [[ $EUID -ne 0 ]] && dpkg -l 2>/dev/null | grep -q "fleet-navigator"; then
    echo -e "${YELLOW}Zum vollständigen Entfernen des DEB-Pakets:${NC}"
    echo "  sudo apt remove fleet-navigator"
    echo ""
fi
