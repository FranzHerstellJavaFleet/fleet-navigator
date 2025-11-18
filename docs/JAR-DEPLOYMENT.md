# Fleet Navigator - JAR Deployment Guide

**Deployment-Anleitung für Java JAR Version**

Version: 0.3.0 (JAR-basiert)
Datum: 18. November 2024

---

## 📋 Übersicht

Diese Anleitung beschreibt das Deployment von Fleet Navigator als **Java JAR** auf Linux-Servern mit **systemd**.

### Deployment-Optionen:

1. **Automatisch** - Mit Setup-Skript (empfohlen)
2. **Manuell** - Schritt für Schritt
3. **Docker** - Container-basiert (optional)

---

## 🚀 Option 1: Automatisches Deployment

### Voraussetzungen

```bash
# Java 21 installieren (falls nicht vorhanden)
sudo apt update
sudo apt install openjdk-21-jdk

# Version prüfen
java -version
# Sollte zeigen: openjdk version "21.x.x"
```

### Installation

```bash
# 1. Repository klonen oder ZIP herunterladen
git clone https://github.com/FranzHerstellJavaFleet/fleet-navigator.git
cd fleet-navigator

# 2. JAR bauen
mvn clean package -DskipTests

# 3. Setup-Skript ausführen
sudo ./setup-fleet-navigator-linux.sh --systemd
```

**Das Skript installiert:**
- ✅ Fleet Navigator JAR nach `/opt/fleet-navigator/`
- ✅ Verzeichnisstruktur (bin/, models/, data/, logs/)
- ✅ llama.cpp Binaries (optional, wenn vorhanden)
- ✅ Konfigurationsdateien
- ✅ systemd Service
- ✅ Start-Skript

### Service starten

```bash
# Service starten
sudo systemctl start fleet-navigator

# Status prüfen
sudo systemctl status fleet-navigator

# Autostart aktivieren
sudo systemctl enable fleet-navigator

# Logs anzeigen
sudo journalctl -u fleet-navigator -f
```

---

## 🔧 Option 2: Manuelles Deployment

### Schritt 1: Verzeichnisse erstellen

```bash
sudo mkdir -p /opt/fleet-navigator/{bin,models/library,models/custom,data,logs}
```

### Schritt 2: JAR kopieren

```bash
# JAR bauen (falls noch nicht geschehen)
mvn clean package -DskipTests

# JAR kopieren
sudo cp target/fleet-navigator-*.jar /opt/fleet-navigator/fleet-navigator.jar
```

### Schritt 3: Konfiguration erstellen

```bash
sudo tee /opt/fleet-navigator/application.properties > /dev/null << 'EOF'
# Fleet Navigator Configuration
server.port=2025
spring.datasource.url=jdbc:h2:file:./data/fleetnavdb
spring.jpa.hibernate.ddl-auto=update
llm.llamacpp.server-url=http://localhost:8081
llm.llamacpp.models-dir=./models
logging.file.name=./logs/fleet-navigator.log
logging.level.io.javafleet=INFO
EOF
```

### Schritt 4: Start-Skript erstellen

```bash
sudo tee /opt/fleet-navigator/start-fleet-navigator.sh > /dev/null << 'EOFSTART'
#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "Fleet Navigator startet..."

# Finde Modell
MODEL=$(find models -name "*.gguf" -type f 2>/dev/null | head -n 1)

if [ -n "$MODEL" ] && [ -f "bin/llama-server" ]; then
    echo "Starte llama-server mit: $MODEL"
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
EOFSTART

sudo chmod +x /opt/fleet-navigator/start-fleet-navigator.sh
```

### Schritt 5: systemd Service erstellen

```bash
sudo tee /etc/systemd/system/fleet-navigator.service > /dev/null << 'EOF'
[Unit]
Description=Fleet Navigator - Private AI Chat Interface
After=network.target

[Service]
Type=simple
User=trainer
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
```

**⚠️ Wichtig:** User anpassen (`User=trainer` → `User=IHR_USER`)

### Schritt 6: Berechtigungen setzen

```bash
sudo chown -R $USER:$USER /opt/fleet-navigator
```

### Schritt 7: Service aktivieren

```bash
# systemd neu laden
sudo systemctl daemon-reload

# Service starten
sudo systemctl start fleet-navigator

# Status prüfen
sudo systemctl status fleet-navigator

# Autostart aktivieren
sudo systemctl enable fleet-navigator
```

---

## 📦 Option 3: Docker Deployment

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre

# Create app directory
WORKDIR /app

# Copy JAR
COPY target/fleet-navigator-*.jar fleet-navigator.jar

# Copy binaries (optional)
COPY bin/ /app/bin/

# Create directories
RUN mkdir -p /app/{data,models,logs}

# Expose port
EXPOSE 2025

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:2025/actuator/health || exit 1

# Run
CMD ["java", "-jar", "fleet-navigator.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  fleet-navigator:
    build: .
    container_name: fleet-navigator
    ports:
      - "2025:2025"
    volumes:
      - ./data:/app/data
      - ./models:/app/models
      - ./logs:/app/logs
    environment:
      - SERVER_PORT=2025
      - LLM_LLAMACPP_MODELS_DIR=/app/models
      - SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/fleetnavdb
    restart: unless-stopped
```

### Build und Start

```bash
# Build
docker-compose build

# Start
docker-compose up -d

# Logs
docker-compose logs -f

# Stop
docker-compose down
```

---

## ⚙️ Konfiguration

### application.properties

```properties
# Server
server.port=2025
server.address=0.0.0.0

# Datenbank
spring.datasource.url=jdbc:h2:file:./data/fleetnavdb
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true

# llama.cpp
llm.llamacpp.server-url=http://localhost:8081
llm.llamacpp.models-dir=./models

# Logging
logging.file.name=./logs/fleet-navigator.log
logging.level.io.javafleet=INFO
logging.level.org.springframework=WARN

# Actuator (Health Check)
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### Umgebungsvariablen

```bash
# In systemd Service-Datei:
Environment="SERVER_PORT=2025"
Environment="LLM_LLAMACPP_MODELS_DIR=/opt/fleet-navigator/models"
Environment="SPRING_PROFILES_ACTIVE=production"
Environment="JAVA_OPTS=-Xmx2G"
```

### Port ändern

```bash
# In application.properties
server.port=8080

# Oder via Umgebungsvariable
export SERVER_PORT=8080

# Oder via Kommandozeile
java -jar fleet-navigator.jar --server.port=8080
```

---

## 🔐 Sicherheit

### Firewall konfigurieren

```bash
# Port 2025 öffnen (falls Firewall aktiv)
sudo ufw allow 2025/tcp
sudo ufw reload

# Oder nur für lokales Netzwerk
sudo ufw allow from 192.168.0.0/16 to any port 2025
```

### Reverse Proxy (nginx)

```nginx
server {
    listen 80;
    server_name fleet-navigator.example.com;

    location / {
        proxy_pass http://localhost:2025;
        proxy_http_version 1.1;

        # WebSocket Support
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

### SSL mit Let's Encrypt

```bash
# Certbot installieren
sudo apt install certbot python3-certbot-nginx

# Zertifikat erstellen
sudo certbot --nginx -d fleet-navigator.example.com

# Auto-Renewal testen
sudo certbot renew --dry-run
```

---

## 📊 Monitoring

### Service Status

```bash
# Status prüfen
sudo systemctl status fleet-navigator

# Ist service active?
sudo systemctl is-active fleet-navigator

# Ist service enabled?
sudo systemctl is-enabled fleet-navigator
```

### Logs

```bash
# Live Logs
sudo journalctl -u fleet-navigator -f

# Letzte 100 Zeilen
sudo journalctl -u fleet-navigator -n 100

# Logs der letzten Stunde
sudo journalctl -u fleet-navigator --since "1 hour ago"

# Nur Fehler
sudo journalctl -u fleet-navigator -p err

# Nach String suchen
sudo journalctl -u fleet-navigator | grep ERROR
```

### Application Logs

```bash
# Fleet Navigator Log
tail -f /opt/fleet-navigator/logs/fleet-navigator.log

# Mit Farben (falls ccze installiert)
tail -f /opt/fleet-navigator/logs/fleet-navigator.log | ccze -A
```

### Health Check

```bash
# HTTP Health Check
curl http://localhost:2025/actuator/health

# Sollte zurückgeben:
# {"status":"UP"}

# Detailliert
curl http://localhost:2025/actuator/health | jq
```

### Ressourcen-Nutzung

```bash
# Prozess finden
ps aux | grep fleet-navigator

# CPU und Memory
top -p $(pgrep -f fleet-navigator)

# Detaillierte Info
systemctl status fleet-navigator
```

---

## 🔄 Updates

### Application Update

```bash
# 1. Neue Version bauen
cd /pfad/zu/fleet-navigator
git pull origin main
mvn clean package -DskipTests

# 2. Service stoppen
sudo systemctl stop fleet-navigator

# 3. Backup erstellen
sudo cp /opt/fleet-navigator/fleet-navigator.jar \
       /opt/fleet-navigator/fleet-navigator.jar.backup

# 4. Neue Version kopieren
sudo cp target/fleet-navigator-*.jar \
       /opt/fleet-navigator/fleet-navigator.jar

# 5. Service starten
sudo systemctl start fleet-navigator

# 6. Status prüfen
sudo systemctl status fleet-navigator
```

### Rollback

```bash
# Bei Problemen: Alte Version wiederherstellen
sudo systemctl stop fleet-navigator
sudo mv /opt/fleet-navigator/fleet-navigator.jar.backup \
       /opt/fleet-navigator/fleet-navigator.jar
sudo systemctl start fleet-navigator
```

---

## 💾 Backup

### Datenbank-Backup

```bash
# Backup-Skript erstellen
sudo tee /opt/fleet-navigator/backup.sh > /dev/null << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/fleet-navigator/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Datenbank sichern
cp /opt/fleet-navigator/data/fleetnavdb.mv.db \
   "$BACKUP_DIR/fleetnavdb_$DATE.mv.db"

# Alte Backups löschen (älter als 7 Tage)
find "$BACKUP_DIR" -name "fleetnavdb_*.mv.db" -mtime +7 -delete

echo "Backup erstellt: fleetnavdb_$DATE.mv.db"
EOF

sudo chmod +x /opt/fleet-navigator/backup.sh
```

### Automatisches Backup (Cron)

```bash
# Crontab bearbeiten
sudo crontab -e

# Täglich um 2 Uhr morgens
0 2 * * * /opt/fleet-navigator/backup.sh >> /opt/fleet-navigator/logs/backup.log 2>&1
```

### Vollständiges Backup

```bash
# Komplettes Verzeichnis sichern
sudo tar -czf fleet-navigator-backup-$(date +%Y%m%d).tar.gz \
  /opt/fleet-navigator/

# Backup verschieben
sudo mv fleet-navigator-backup-*.tar.gz /backup/location/
```

---

## 🐛 Troubleshooting

### Service startet nicht

```bash
# Logs prüfen
sudo journalctl -u fleet-navigator -n 50

# Häufige Probleme:
# 1. Port bereits belegt
sudo lsof -i :2025

# 2. Java nicht gefunden
which java

# 3. Berechtigungen
ls -la /opt/fleet-navigator/
```

### Port bereits belegt

```bash
# Prüfen was auf Port 2025 läuft
sudo lsof -i :2025

# Prozess beenden (Vorsicht!)
sudo kill $(sudo lsof -t -i:2025)

# Oder anderen Port verwenden
sudo nano /opt/fleet-navigator/application.properties
# server.port=8080
```

### Datenbank-Fehler

```bash
# Datenbank zurücksetzen (⚠️ Löscht alle Daten!)
sudo systemctl stop fleet-navigator
sudo rm -rf /opt/fleet-navigator/data/*
sudo systemctl start fleet-navigator
```

### Hoher Speicherverbrauch

```bash
# Java Memory Limit setzen
sudo nano /etc/systemd/system/fleet-navigator.service

# Zeile hinzufügen:
Environment="JAVA_OPTS=-Xmx2G -Xms512M"

# Service neu laden
sudo systemctl daemon-reload
sudo systemctl restart fleet-navigator
```

---

## 📈 Performance-Tuning

### JVM-Optionen

```bash
# In systemd Service-Datei
Environment="JAVA_OPTS=-Xmx4G -Xms1G -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Datenbank-Tuning

```properties
# In application.properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.jpa.properties.hibernate.jdbc.batch_size=20
```

### Logging-Level anpassen

```properties
# Weniger Logging für bessere Performance
logging.level.io.javafleet=WARN
logging.level.org.springframework=ERROR
logging.level.org.hibernate=WARN
```

---

## ✅ Deployment-Checkliste

Nach dem Deployment:

- [ ] Java 21 installiert und geprüft
- [ ] JAR gebaut: `mvn clean package`
- [ ] Verzeichnisse erstellt unter `/opt/fleet-navigator/`
- [ ] JAR kopiert nach `/opt/fleet-navigator/`
- [ ] Konfiguration erstellt (`application.properties`)
- [ ] Start-Skript erstellt und ausführbar
- [ ] systemd Service erstellt
- [ ] Berechtigungen korrekt gesetzt
- [ ] Service gestartet: `sudo systemctl start fleet-navigator`
- [ ] Service läuft: `sudo systemctl status fleet-navigator`
- [ ] HTTP Test erfolgreich: `curl http://localhost:2025`
- [ ] Logs sauber: `sudo journalctl -u fleet-navigator -n 50`
- [ ] Autostart aktiviert: `sudo systemctl enable fleet-navigator`
- [ ] Firewall konfiguriert (falls nötig)
- [ ] Backup-Strategie eingerichtet
- [ ] Monitoring aufgesetzt
- [ ] Dokumentation gelesen

---

## 📞 Support

Bei Problemen:

1. **Logs prüfen:** `sudo journalctl -u fleet-navigator -f`
2. **GitHub Issues:** https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues
3. **E-Mail:** franz-martin@java-developer.online

---

**Erstellt:** 18. November 2024
**Version:** 0.3.0 (JAR)
**Autor:** JavaFleet Systems Consulting
