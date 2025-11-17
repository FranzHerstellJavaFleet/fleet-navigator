#!/bin/bash

# Fleet Navigator - JAR-basierter Systemd Service Installation Script
# ======================================================================

set -e

echo "🚀 Fleet Navigator - JAR Service Installation"
echo "=============================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "❌ Bitte als root ausführen (mit sudo)"
    exit 1
fi

# Check if JAR exists
JAR_FILE=$(find target -name "fleet-navigator-*.jar" -type f | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ JAR-Datei nicht gefunden!"
    echo "   Bitte zuerst bauen mit: mvn clean package"
    exit 1
fi

echo "✅ Gefundenes JAR: $JAR_FILE"
echo ""

echo "📦 1. Erstelle Installations-Verzeichnis..."
mkdir -p /opt/fleet-navigator
mkdir -p /opt/fleet-navigator/data
mkdir -p /opt/fleet-navigator/logs

echo "📋 2. Kopiere JAR..."
cp "$JAR_FILE" /opt/fleet-navigator/fleet-navigator.jar
echo "   Kopiert: fleet-navigator.jar ($(du -h /opt/fleet-navigator/fleet-navigator.jar | cut -f1))"

echo "📝 3. Kopiere Konfiguration..."
# Optional: Kopiere application.yml wenn vorhanden
if [ -f "src/main/resources/application.yml" ]; then
    cp src/main/resources/application.yml /opt/fleet-navigator/
    echo "   ✓ application.yml kopiert"
fi

echo "👤 4. Setze Berechtigungen..."
chown -R $SUDO_USER:$SUDO_USER /opt/fleet-navigator

echo "🔧 5. Installiere Systemd Service..."
cp fleet-navigator-jar.service /etc/systemd/system/fleet-navigator.service
systemctl daemon-reload

echo "✅ Installation abgeschlossen!"
echo ""
echo "🎯 Nächste Schritte:"
echo ""
echo "   # Service starten:"
echo "   sudo systemctl start fleet-navigator"
echo ""
echo "   # Status prüfen:"
echo "   sudo systemctl status fleet-navigator"
echo ""
echo "   # Autostart aktivieren:"
echo "   sudo systemctl enable fleet-navigator"
echo ""
echo "   # Logs anzeigen (live):"
echo "   sudo journalctl -u fleet-navigator -f"
echo ""
echo "   # Logs der letzten 100 Zeilen:"
echo "   sudo journalctl -u fleet-navigator -n 100"
echo ""
echo "   # Service stoppen:"
echo "   sudo systemctl stop fleet-navigator"
echo ""
echo "   # Service neustarten:"
echo "   sudo systemctl restart fleet-navigator"
echo ""
echo "📊 Installation Details:"
echo "   JAR:  /opt/fleet-navigator/fleet-navigator.jar"
echo "   Data: /opt/fleet-navigator/data"
echo "   Logs: /opt/fleet-navigator/logs"
echo "   User: $SUDO_USER"
echo ""
echo "🌐 Nach dem Start erreichbar unter:"
echo "   http://localhost:2025"
echo ""
echo "🔒 Security Features aktiviert:"
echo "   ✓ NoNewPrivileges"
echo "   ✓ PrivateTmp"
echo "   ✓ ProtectSystem=strict"
echo "   ✓ RestrictRealtime"
echo ""
