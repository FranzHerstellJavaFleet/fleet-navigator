# Fleet Navigator - systemd Service Deployment Guide

## Übersicht

Diese Anleitung zeigt, wie du Fleet Navigator als systemd-Service auf einem Linux-Server installierst und betreibst.

## Voraussetzungen

- Linux-System mit systemd (Ubuntu, Debian, RHEL, etc.)
- GraalVM Native Image Binary (`fleet-navigator`)
- Sudo/Root-Zugriff

## Installation

### 1. Schnellinstallation mit Script

```bash
# Navigiere zum Projekt-Root
cd /pfad/zu/fleet-navigator

# Führe das Installations-Script aus
sudo ./install-service.sh
```

Das Script erstellt automatisch:
- `/opt/fleet-navigator/` - Installations-Verzeichnis
- `/opt/fleet-navigator/data/` - Datenbank und Konfiguration
- `/opt/fleet-navigator/models/` - LLM-Modelle-Verzeichnis
  - `/opt/fleet-navigator/models/library/` - HuggingFace Modelle
  - `/opt/fleet-navigator/models/custom/` - Eigene Modelle
- `/opt/fleet-navigator/bin/` - llama.cpp Binaries

### 2. Manuelle Installation

```bash
# 1. Verzeichnisse erstellen
sudo mkdir -p /opt/fleet-navigator/{data,models/{library,custom},bin,logs}

# 2. Binary kopieren
sudo cp target/fleet-navigator /opt/fleet-navigator/
sudo chmod +x /opt/fleet-navigator/fleet-navigator

# 3. Binaries kopieren (llama-server-wrapper.sh etc.)
sudo cp -r bin/* /opt/fleet-navigator/bin/
sudo chmod +x /opt/fleet-navigator/bin/*.sh

# 4. Berechtigungen setzen
sudo chown -R $USER:$USER /opt/fleet-navigator

# 5. Systemd Service installieren
sudo cp fleet-navigator.service /etc/systemd/system/
sudo systemctl daemon-reload
```

## Service-Verwaltung

### Service starten
```bash
sudo systemctl start fleet-navigator
```

### Service stoppen
```bash
sudo systemctl stop fleet-navigator
```

### Status prüfen
```bash
sudo systemctl status fleet-navigator
```

### Autostart aktivieren
```bash
sudo systemctl enable fleet-navigator
```

### Autostart deaktivieren
```bash
sudo systemctl disable fleet-navigator
```

### Logs ansehen
```bash
# Live-Logs (mit Follow)
sudo journalctl -u fleet-navigator -f

# Logs der letzten Stunde
sudo journalctl -u fleet-navigator --since "1 hour ago"

# Logs des letzten Boots
sudo journalctl -u fleet-navigator -b
```

## Konfiguration

### Environment-Variablen

Die Service-Konfiguration befindet sich in `/etc/systemd/system/fleet-navigator.service`.

**Wichtige Environment-Variablen:**

```ini
# LLM-Konfiguration
Environment="LLM_LLAMACPP_MODELS_DIR=/opt/fleet-navigator/models"
Environment="LLM_LLAMACPP_BINARY_PATH=/opt/fleet-navigator/bin/llama-server-wrapper.sh"

# Datenbank
Environment="SPRING_DATASOURCE_URL=jdbc:h2:file:/opt/fleet-navigator/data/fleetnavdb"

# Server
Environment="SERVER_PORT=2025"
Environment="SPRING_PROFILES_ACTIVE=production"
```

### Konfiguration ändern

1. Editiere die Service-Datei:
   ```bash
   sudo nano /etc/systemd/system/fleet-navigator.service
   ```

2. Lade systemd neu:
   ```bash
   sudo systemctl daemon-reload
   ```

3. Starte den Service neu:
   ```bash
   sudo systemctl restart fleet-navigator
   ```

## Modelle-Verwaltung

### Modelle herunterladen

```bash
# Beispiel: LLaMA 3.2 1B Instruct von HuggingFace
cd /opt/fleet-navigator/models/library
wget https://huggingface.co/.../.../Llama-3.2-1B-Instruct-Q4_K_M.gguf
```

### Eigene Modelle hinzufügen

```bash
# Kopiere eigene GGUF-Modelle nach custom/
cp /pfad/zu/modell.gguf /opt/fleet-navigator/models/custom/
```

### Modelle-Verzeichnis extern mounten

Wenn deine Modelle auf einem separaten Storage liegen:

```bash
# Symlink erstellen
sudo rm -rf /opt/fleet-navigator/models
sudo ln -s /mnt/storage/llm-models /opt/fleet-navigator/models

# ODER: In systemd-Service-Datei anpassen
Environment="LLM_LLAMACPP_MODELS_DIR=/mnt/storage/llm-models"
```

## Verzeichnis-Struktur

```
/opt/fleet-navigator/
├── fleet-navigator           # Native Image Binary
├── data/                     # Datenbank und Konfiguration
│   └── fleetnavdb.mv.db      # H2 Datenbank
├── models/                   # LLM-Modelle
│   ├── library/              # HuggingFace Modelle
│   │   └── *.gguf
│   └── custom/               # Eigene Modelle
│       └── *.gguf
├── bin/                      # llama.cpp Binaries
│   ├── llama-server
│   ├── llama-server-wrapper.sh
│   └── *.so (CUDA Libraries)
└── logs/                     # Anwendungs-Logs
```

## Security

Der Service läuft mit folgenden Sicherheitseinstellungen:

```ini
# Verhindert Rechte-Eskalation
NoNewPrivileges=true

# Isoliertes /tmp Verzeichnis
PrivateTmp=true

# Schreibschutz für /usr, /boot, /efi
ProtectSystem=strict

# Home-Verzeichnisse nicht sichtbar
ProtectHome=true

# Nur diese Pfade sind beschreibbar
ReadWritePaths=/opt/fleet-navigator/data
ReadWritePaths=/opt/fleet-navigator/models
```

## Troubleshooting

### Service startet nicht

```bash
# Detaillierte Fehler anzeigen
sudo journalctl -u fleet-navigator -n 50 --no-pager

# Systemd-Unit testen
sudo systemd-analyze verify fleet-navigator.service
```

### Port 2025 bereits belegt

```bash
# Prüfe welcher Prozess Port 2025 nutzt
sudo lsof -i :2025

# Ändere Port in systemd-Service
Environment="SERVER_PORT=8080"
```

### Modelle nicht gefunden

```bash
# Prüfe Modelle-Verzeichnis
ls -la /opt/fleet-navigator/models/

# Prüfe Berechtigungen
sudo chown -R $USER:$USER /opt/fleet-navigator/models

# Teste Pfad
Environment="LLM_LLAMACPP_MODELS_DIR=/opt/fleet-navigator/models"
```

### Datenbank-Fehler

```bash
# Lösche Datenbank und lasse neu erstellen
sudo rm /opt/fleet-navigator/data/fleetnavdb.mv.db
sudo systemctl restart fleet-navigator
```

## Backup

### Datenbank sichern

```bash
# Stoppe Service
sudo systemctl stop fleet-navigator

# Sichere Datenbank
sudo cp -r /opt/fleet-navigator/data /backup/fleet-navigator-data-$(date +%Y%m%d)

# Starte Service
sudo systemctl start fleet-navigator
```

### Modelle sichern

```bash
# Modelle separat sichern (können sehr groß sein!)
sudo tar czf /backup/fleet-navigator-models-$(date +%Y%m%d).tar.gz \
  /opt/fleet-navigator/models
```

## Updates

### Binary aktualisieren

```bash
# 1. Neues Binary bauen
mvn -Pnative clean package

# 2. Service stoppen
sudo systemctl stop fleet-navigator

# 3. Binary ersetzen
sudo cp target/fleet-navigator /opt/fleet-navigator/

# 4. Service starten
sudo systemctl start fleet-navigator

# 5. Logs prüfen
sudo journalctl -u fleet-navigator -f
```

## Monitoring

### Ressourcen-Nutzung

```bash
# Speicher-Nutzung
sudo systemctl status fleet-navigator | grep Memory

# CPU-Nutzung
top -p $(pgrep fleet-navigator)

# Disk-Nutzung
du -sh /opt/fleet-navigator/*
```

### Systemd-Journal-Größe limitieren

```bash
# Journal-Größe limitieren (z.B. 100MB)
sudo journalctl --vacuum-size=100M

# Alte Logs löschen (älter als 7 Tage)
sudo journalctl --vacuum-time=7d
```

## Deinstallation

```bash
# 1. Service stoppen und deaktivieren
sudo systemctl stop fleet-navigator
sudo systemctl disable fleet-navigator

# 2. Service-Datei entfernen
sudo rm /etc/systemd/system/fleet-navigator.service
sudo systemctl daemon-reload

# 3. Installations-Verzeichnis entfernen
sudo rm -rf /opt/fleet-navigator

# Optional: Backup erstellen vor dem Löschen
sudo tar czf /backup/fleet-navigator-backup-$(date +%Y%m%d).tar.gz \
  /opt/fleet-navigator
```

## Weitere Informationen

- **GitHub**: https://github.com/FranzHerstellJavaFleet/fleet-navigator
- **Native Image Build**: [NATIVE-IMAGE.md](NATIVE-IMAGE.md)
- **Production Build**: [BUILD-PRODUCTION.md](BUILD-PRODUCTION.md)

## Support

Bei Problemen:
1. Prüfe die Logs: `sudo journalctl -u fleet-navigator -f`
2. Öffne ein Issue auf GitHub
3. Kontaktiere: Franz Herstell (franz-martin@java-developer.online)
