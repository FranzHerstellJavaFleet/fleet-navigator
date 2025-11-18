#!/bin/bash
# Fleet Navigator - Einfache systemd Installation
# Führe dieses Skript mit sudo aus!

set -e

if [ "$EUID" -ne 0 ]; then
    echo "❌ Bitte mit sudo ausführen:"
    echo "   sudo ./install-systemd-simple.sh"
    exit 1
fi

echo "🚀 Fleet Navigator systemd Installation"
echo "========================================"
echo ""

# Verzeichnisse erstellen
echo "📁 Erstelle Verzeichnisse..."
mkdir -p /opt/fleet-navigator/{bin,models/library,models/custom,data,logs}

# JAR kopieren
echo "📦 Kopiere JAR..."
if [ -f "target/fleet-navigator-0.3.0.jar" ]; then
    cp target/fleet-navigator-0.3.0.jar /opt/fleet-navigator/fleet-navigator.jar
    echo "   ✓ fleet-navigator.jar kopiert ($(du -h target/fleet-navigator-0.3.0.jar | cut -f1))"
else
    echo "   ❌ JAR nicht gefunden in target/"
    exit 1
fi

# Binaries kopieren (falls vorhanden)
if [ -d "bin" ]; then
    echo "📦 Kopiere llama.cpp Binaries..."
    cp -r bin/* /opt/fleet-navigator/bin/ 2>/dev/null || true
    chmod +x /opt/fleet-navigator/bin/*.sh 2>/dev/null || true
    chmod +x /opt/fleet-navigator/bin/llama-* 2>/dev/null || true
fi

# Modelle kopieren (falls vorhanden)
if [ -d "models" ] && [ "$(ls -A models/*.gguf 2>/dev/null)" ]; then
    echo "📦 Kopiere Modelle..."
    cp models/*.gguf /opt/fleet-navigator/models/library/ 2>/dev/null || true
    echo "   ✓ $(ls /opt/fleet-navigator/models/library/*.gguf 2>/dev/null | wc -l) Modelle kopiert"
fi

# Berechtigungen
echo "👤 Setze Berechtigungen..."
chown -R $SUDO_USER:$SUDO_USER /opt/fleet-navigator

# Konfiguration
echo "⚙️  Erstelle Konfiguration..."
cat > /opt/fleet-navigator/application.properties << 'EOF'
# Fleet Navigator Configuration
server.port=2025
spring.datasource.url=jdbc:h2:file:./data/fleetnavdb
spring.jpa.hibernate.ddl-auto=update
llm.llamacpp.server-url=http://localhost:8081
llm.llamacpp.models-dir=./models
logging.file.name=./logs/fleet-navigator.log
logging.level.io.javafleet=INFO
EOF

# Start-Skript
echo "📝 Erstelle Start-Skript..."
cat > /opt/fleet-navigator/start-fleet-navigator.sh << 'EOF'
#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "Fleet Navigator startet..."

# Finde Modell
MODEL=$(find models -name "*.gguf" -type f 2>/dev/null | head -n 1)

if [ -n "$MODEL" ] && [ -f "bin/llama-server" ]; then
    echo "Starte llama-server mit Modell: $MODEL"
    export LD_LIBRARY_PATH="$SCRIPT_DIR/bin:$LD_LIBRARY_PATH"
    bin/llama-server --port 8081 --n-gpu-layers 999 --model "$MODEL" &
    LLAMA_PID=$!
    sleep 3
fi

# Starte Fleet Navigator
java -jar fleet-navigator.jar

# Cleanup
if [ -n "$LLAMA_PID" ]; then
    kill $LLAMA_PID 2>/dev/null
fi
EOF

chmod +x /opt/fleet-navigator/start-fleet-navigator.sh

# systemd Service
echo "🔧 Erstelle systemd Service..."
cat > /etc/systemd/system/fleet-navigator.service << EOF
[Unit]
Description=Fleet Navigator - Private AI Chat Interface
After=network.target

[Service]
Type=simple
User=$SUDO_USER
WorkingDirectory=/opt/fleet-navigator
ExecStart=/opt/fleet-navigator/start-fleet-navigator.sh
Restart=on-failure
RestartSec=10

Environment="LD_LIBRARY_PATH=/opt/fleet-navigator/bin"

StandardOutput=journal
StandardError=journal
SyslogIdentifier=fleet-navigator

[Install]
WantedBy=multi-user.target
EOF

# systemd reload
echo "🔄 Lade systemd neu..."
systemctl daemon-reload

echo ""
echo "✅ Installation abgeschlossen!"
echo ""
echo "📊 Installierte Dateien:"
ls -lh /opt/fleet-navigator/*.jar
echo ""
echo "🚀 Service starten:"
echo "   sudo systemctl start fleet-navigator"
echo ""
echo "📊 Status prüfen:"
echo "   sudo systemctl status fleet-navigator"
echo ""
echo "🔄 Autostart aktivieren:"
echo "   sudo systemctl enable fleet-navigator"
echo ""
echo "📜 Logs anzeigen:"
echo "   sudo journalctl -u fleet-navigator -f"
echo ""
