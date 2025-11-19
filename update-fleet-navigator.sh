#!/bin/bash
# Fleet Navigator Update Script
# Aktualisiert die laufende Fleet Navigator Installation

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
INSTALL_DIR="/opt/fleet-navigator"
SERVICE_NAME="fleet-navigator"

echo "=========================================="
echo "Fleet Navigator Update"
echo "=========================================="
echo ""

# Prüfen ob als root ausgeführt
if [ "$EUID" -ne 0 ]; then
    echo "❌ Dieses Skript muss als root ausgeführt werden"
    echo "   Verwende: sudo ./update-fleet-navigator.sh"
    exit 1
fi

# Prüfen ob JAR existiert
if [ ! -f "$SCRIPT_DIR/target/fleet-navigator-0.3.0.jar" ]; then
    echo "❌ JAR nicht gefunden: $SCRIPT_DIR/target/fleet-navigator-0.3.0.jar"
    echo "   Führe zuerst aus: mvn clean package -DskipTests"
    exit 1
fi

# Service Status vor Update
echo "📊 Service Status vor Update:"
systemctl status "$SERVICE_NAME" --no-pager -l | head -5
echo ""

# Service stoppen
echo "⏸️  Stoppe $SERVICE_NAME Service..."
systemctl stop "$SERVICE_NAME"
echo "✅ Service gestoppt"
echo ""

# Backup erstellen
if [ -f "$INSTALL_DIR/fleet-navigator.jar" ]; then
    BACKUP_FILE="$INSTALL_DIR/fleet-navigator.jar.backup-$(date +%Y%m%d-%H%M%S)"
    echo "💾 Erstelle Backup: $(basename $BACKUP_FILE)"
    cp "$INSTALL_DIR/fleet-navigator.jar" "$BACKUP_FILE"
    echo "✅ Backup erstellt"
    echo ""
fi

# Neue JAR kopieren
echo "📦 Kopiere neue JAR..."
cp "$SCRIPT_DIR/target/fleet-navigator-0.3.0.jar" "$INSTALL_DIR/fleet-navigator.jar"
echo "✅ JAR aktualisiert"
echo ""

# Berechtigungen setzen
chown $SUDO_USER:$SUDO_USER "$INSTALL_DIR/fleet-navigator.jar"

# Service starten
echo "▶️  Starte $SERVICE_NAME Service..."
systemctl start "$SERVICE_NAME"
echo "✅ Service gestartet"
echo ""

# Warte kurz
sleep 3

# Status prüfen
echo "📊 Service Status nach Update:"
systemctl status "$SERVICE_NAME" --no-pager -l | head -10
echo ""

# Erfolg
echo "=========================================="
echo "✅ Fleet Navigator erfolgreich aktualisiert!"
echo "=========================================="
echo ""
echo "🌐 Fleet Navigator läuft auf: http://localhost:2025"
echo ""
echo "📋 Logs ansehen:"
echo "   sudo journalctl -u $SERVICE_NAME -f"
echo ""
echo "🔄 Service neu starten:"
echo "   sudo systemctl restart $SERVICE_NAME"
echo ""
