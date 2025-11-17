#!/bin/bash

# Fleet Navigator - Systemd Service Installation Script
# ======================================================

set -e

echo "🚀 Fleet Navigator - Systemd Service Installation"
echo "=================================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "❌ Bitte als root ausführen (mit sudo)"
    exit 1
fi

# Check if native binary exists
if [ ! -f "target/fleet-navigator" ]; then
    echo "❌ Native Image nicht gefunden!"
    echo "   Bitte zuerst bauen mit: ./build-native.sh"
    exit 1
fi

echo "📦 1. Erstelle Installations-Verzeichnis..."
mkdir -p /opt/fleet-navigator
mkdir -p /opt/fleet-navigator/data
mkdir -p /opt/fleet-navigator/logs
mkdir -p /opt/fleet-navigator/models
mkdir -p /opt/fleet-navigator/models/library
mkdir -p /opt/fleet-navigator/models/custom
mkdir -p /opt/fleet-navigator/bin

echo "📋 2. Kopiere Binary..."
cp target/fleet-navigator /opt/fleet-navigator/
chmod +x /opt/fleet-navigator/fleet-navigator

echo "📝 3. Kopiere Konfiguration und Binaries..."
# Optional: Kopiere application.yml wenn vorhanden
if [ -f "src/main/resources/application.yml" ]; then
    cp src/main/resources/application.yml /opt/fleet-navigator/
fi

# Kopiere bin/ Verzeichnis (llama-server-wrapper.sh etc.)
if [ -d "bin" ]; then
    echo "   Kopiere llama.cpp Binaries..."
    cp -r bin/* /opt/fleet-navigator/bin/
    chmod +x /opt/fleet-navigator/bin/*.sh 2>/dev/null || true
fi

# Kopiere existierende Modelle (falls vorhanden)
if [ -d "models" ]; then
    echo "   Kopiere Modelle (kann dauern)..."
    cp -r models/* /opt/fleet-navigator/models/ 2>/dev/null || true
fi

echo "👤 4. Setze Berechtigungen..."
chown -R $SUDO_USER:$SUDO_USER /opt/fleet-navigator

echo "🔧 5. Installiere Systemd Service..."
cp fleet-navigator.service /etc/systemd/system/
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
echo "   # Logs anzeigen:"
echo "   sudo journalctl -u fleet-navigator -f"
echo ""
echo "   # Service stoppen:"
echo "   sudo systemctl stop fleet-navigator"
echo ""
echo "📊 Binary Info:"
ls -lh /opt/fleet-navigator/fleet-navigator
echo ""
echo "🌐 URL: http://localhost:2025"
echo ""
echo "📁 Modelle-Verzeichnis:"
echo "   /opt/fleet-navigator/models/"
echo "   ├── library/    (HuggingFace Modelle)"
echo "   └── custom/     (Eigene Modelle)"
echo ""
echo "💡 Tipp: Modelle mit Environment-Variablen konfigurieren:"
echo "   LLM_LLAMACPP_MODELS_DIR=/pfad/zu/modellen"
echo ""
