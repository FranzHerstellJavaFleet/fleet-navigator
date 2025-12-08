# Release Notes v0.3.0 - Fleet Navigator

**Release-Datum**: 17. November 2025
**Codename**: "Mate Migration & Professional Deployment"

---

## 🎉 Highlights

### 🚢 Complete Officer→Mate Migration
Die gesamte Anwendung verwendet jetzt **FleetMates** statt FleetOfficers für eine konsistentere Terminologie.

### 🖥️ Systemd Service Support
Professionelle Server-Deployments mit vollständiger systemd-Integration:
- Automatischer Start beim Systemstart
- Prozess-Überwachung und Neustart bei Fehlern
- Security Hardening (Sandboxing)
- Zentrale Log-Verwaltung
- Flexible Konfiguration via Environment-Variablen

### 📚 Umfassende Dokumentation
Drei neue Guides für alle Deployment-Szenarien:
- **docs/INSTALL.md** - Kompletter Installations-Guide
- **docs/SYSTEMD-DEPLOYMENT.md** - Server-Deployment
- **Aktualisierte README.md** - Erweiterte Systemanforderungen

---

## 💻 Systemanforderungen & Performance

### Minimum-Konfiguration (Llama-3.2-1B)
- **CPU**: Intel Core i3 / AMD Ryzen 3 (ab 2015)
- **RAM**: 4 GB
- **Speicher**: 5 GB frei
- **GPU**: Nicht erforderlich
- **Performance**: 15-25 Tokens/Sek (CPU)

### Empfohlen (Qwen2.5-3B)
- **CPU**: Intel Core i5 / AMD Ryzen 5 (ab 2018)
- **RAM**: 8 GB
- **Speicher**: 10 GB frei
- **GPU**: NVIDIA GTX 1050+ mit 4GB VRAM (optional)
- **Performance**: 8-15 Tokens/Sek (CPU), 40-80 Tokens/Sek (GPU)

### Optimal (Qwen2.5-7B)
- **CPU**: Intel Core i7 / AMD Ryzen 7
- **RAM**: 16 GB+
- **Speicher**: 20 GB frei
- **GPU**: NVIDIA RTX 3060+ mit 8GB+ VRAM
- **Performance**: 3-8 Tokens/Sek (CPU), 25-50 Tokens/Sek (GPU)

### 🎮 Unterstützte GPUs

**NVIDIA (CUDA) - Stark empfohlen!**
- ✅ GeForce RTX 4090, 4080, 4070
- ✅ GeForce RTX 3090, 3080, 3070, 3060
- ✅ GeForce GTX 1660, 1650, 1080 Ti, 1070
- ✅ Quadro/Tesla Serie (Workstations/Server)
- 📊 Bis zu 10x schneller als CPU!

**AMD (ROCm) - Linux Only**
- ✅ Radeon RX 7000 Serie
- ✅ Radeon RX 6000 Serie
- ⚠️ Experimenteller Support

**Apple Silicon**
- ✅ M1/M2/M3 - Metal Acceleration
- 🚀 Optimiert durch unified memory

**Intel GPUs**
- ⚠️ Experimenteller Support via oneAPI

### 📊 Performance-Vergleich

| Hardware | Modell | Tokens/Sek | Antwortzeit (50 Tokens) |
|----------|--------|------------|-------------------------|
| **Intel i5 (CPU)** | Llama-3.2-1B | 20 | ~2.5s |
| **Intel i5 (CPU)** | Qwen2.5-3B | 12 | ~4s |
| **NVIDIA RTX 3060** | Llama-3.2-1B | 80 | ~0.6s |
| **NVIDIA RTX 3060** | Qwen2.5-3B | 50 | ~1s |
| **NVIDIA RTX 3060** | Qwen2.5-7B | 30 | ~1.7s |
| **Apple M2** | Llama-3.2-1B | 60 | ~0.8s |
| **Apple M2** | Qwen2.5-3B | 35 | ~1.4s |

**Tipp**: Mit einer NVIDIA GPU (ab GTX 1050) erreichst du 5-10x schnellere Antworten!

---

## 🚀 Neue Features

### Server-Deployment
- **systemd Service** mit automatischem Start
- **Absolute Pfade** für Modelle via Environment-Variablen
- **Security Hardening**: NoNewPrivileges, ProtectSystem, ProtectHome
- **Flexible Konfiguration**: Port, Modellpfad, Datenbankpfad
- **Installations-Script**: Automatische Setup mit `install-service.sh`

### Dokumentation
- **INSTALL.md**: Desktop, Server, Docker, Native Image Build
- **SYSTEMD-DEPLOYMENT.md**: Professionelle Server-Anleitung
  - Installation & Konfiguration
  - Service-Verwaltung
  - Logs & Monitoring
  - Backup & Updates
  - Troubleshooting
- **README.md**: Erweitert mit Systemanforderungen und GPU-Support

### Environment-Variablen Support
```bash
LLM_LLAMACPP_MODELS_DIR=/opt/fleet-navigator/models
LLM_LLAMACPP_BINARY_PATH=/opt/fleet-navigator/bin/llama-server-wrapper.sh
SPRING_DATASOURCE_URL=jdbc:h2:file:/opt/fleet-navigator/data/fleetnavdb
SERVER_PORT=2025
```

---

## 🔧 Technische Verbesserungen

### GraalVM Native Image Fixes
- ✅ Fixed `org.apache.commons.logging.LogFactory` Initialisierungsfehler
- ✅ Excluded commons-logging von flexmark-all dependency
- ✅ Korrigierter NATIVE-IMAGE.md Pfad in GitHub Actions
- ✅ Erfolgreiche Builds für Linux, macOS, Windows

### Build-Optimierungen
- Kompatibilitäts-Modus für maximale CPU-Kompatibilität (`-march=compatibility`)
- Optimierte Packaging-Scripts
- Verbesserte Fehlerbehandlung

### Deployment-Flexibilität
- Support für externe Modell-Storage (NFS, SAN)
- Konfigurierbare Pfade ohne Neucompilierung
- Production-ready Security-Einstellungen

---

## 🐛 Bug Fixes

- Fixed GraalVM commons-logging dependency conflict
- Fixed NATIVE-IMAGE.md path in GitHub Actions workflow
- Improved model path resolution for server deployments
- Enhanced install-service.sh with model directory structure

---

## 📦 Deployment-Optionen

### Desktop (Einfach)
```bash
# Windows: Doppelklick auf fleet-navigator.exe
# macOS/Linux:
./fleet-navigator
```

### Server (systemd)
```bash
sudo ./install-service.sh
sudo systemctl start fleet-navigator
sudo systemctl enable fleet-navigator
```

### Docker
```bash
docker run -d \
  -p 2025:2025 \
  -v ./data:/app/data \
  -v ./models:/app/models \
  fleet-navigator
```

---

## 📥 Download

Native Binaries für alle Plattformen:

### Windows
- **fleet-navigator-windows-amd64.zip** (~80 MB)
- Keine JDK erforderlich!
- Windows 10/11 (64-bit)

### macOS
- **fleet-navigator-macos-amd64.tar.gz** (~75 MB)
- Intel und Apple Silicon (M1/M2/M3)
- macOS 12 (Monterey) oder neuer

### Linux
- **fleet-navigator-linux-amd64.tar.gz** (~70 MB)
- Ubuntu 20.04+, Debian 11+, RHEL 8+
- glibc 2.31+

---

## 🔄 Migration von v0.2.x

### Für Desktop-Nutzer
Keine Änderungen erforderlich! Einfach die neue Version herunterladen und starten.

### Für Server-Nutzer
1. Neues Binary herunterladen
2. Service stoppen: `sudo systemctl stop fleet-navigator`
3. Binary ersetzen: `sudo cp fleet-navigator /opt/fleet-navigator/`
4. Service starten: `sudo systemctl start fleet-navigator`

### Datenbank-Migration
Keine Migration erforderlich - H2 Datenbank ist kompatibel.

---

## 📖 Dokumentation

- **Installation**: [docs/INSTALL.md](docs/INSTALL.md)
- **Server Deployment**: [docs/SYSTEMD-DEPLOYMENT.md](docs/SYSTEMD-DEPLOYMENT.md)
- **Native Image Build**: [docs/NATIVE-IMAGE.md](docs/NATIVE-IMAGE.md)
- **HuggingFace Integration**: [docs/HUGGINGFACE-INTEGRATION.md](docs/HUGGINGFACE-INTEGRATION.md)
- **Vollständiger Index**: [docs/INDEX.md](docs/INDEX.md)

---

## 🙏 Danksagungen

- **llama.cpp Team** - Für die schnellste lokale AI-Engine
- **HuggingFace** - Für tausende Open-Source-Modelle
- **GraalVM Team** - Für Native Image Support
- **Spring Boot & Vue.js Communities**
- **Alle Contributor und Tester**

---

## 📞 Support

- **GitHub Issues**: https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues
- **Diskussionen**: https://github.com/FranzHerstellJavaFleet/fleet-navigator/discussions
- **Email**: franz-martin@java-developer.online

---

**🚢 Entwickelt von JavaFleet Systems Consulting**
Port 2025 - Das Geburtsjahr von Fleet Navigator

---

## Nächste Schritte

Nach der Installation:
1. **System-Check** aufrufen: http://localhost:2025
2. **Modell herunterladen** (Model Store → Qwen2.5-3B empfohlen)
3. **Erste Frage stellen** oder Brief generieren
4. **GPU prüfen** falls vorhanden (automatische Erkennung)

Viel Spaß mit Fleet Navigator v0.3.0! 🎉
