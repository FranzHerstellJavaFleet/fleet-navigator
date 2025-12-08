# Fleet Navigator - Schnellstart-Anleitung

## 🚀 In 5 Minuten startklar

### Schritt 1: Download

Lade die passende Version für dein System herunter:

| System | Download |
|--------|----------|
| **Windows** | [fleet-navigator-0.6.0-windows-x64.zip](https://github.com/FranzHerstellJavaFleet/Fleet-Navigator/releases/latest) |
| **Linux** | [fleet-navigator-0.6.0-linux-x64.tar.gz](https://github.com/FranzHerstellJavaFleet/Fleet-Navigator/releases/latest) |
| **macOS** | [fleet-navigator-0.6.0-macos-arm64.tar.gz](https://github.com/FranzHerstellJavaFleet/Fleet-Navigator/releases/latest) |

### Schritt 2: Entpacken

**Windows:**
- Rechtsklick auf ZIP → "Alle extrahieren..."
- Zielordner wählen (z.B. `C:\Programme\FleetNavigator`)

**Linux/macOS:**
```bash
tar -xzf fleet-navigator-*.tar.gz
cd fleet-navigator-*/
```

### Schritt 3: Java installieren (falls nicht vorhanden)

Fleet Navigator benötigt **Java 17** oder neuer.

**Windows:**
1. [Java 17 herunterladen](https://adoptium.net/de/temurin/releases/?version=17&os=windows)
2. Installer ausführen
3. Nach Installation: Terminal öffnen und `java -version` prüfen

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jre
```

**macOS:**
```bash
brew install openjdk@17
```

### Schritt 4: Starten

**Windows:**
- Doppelklick auf `start.bat`

**Linux/macOS:**
```bash
./start.sh
```

### Schritt 5: Browser öffnen

Gehe zu: **http://localhost:2025**

Beim ersten Start wirst du aufgefordert, ein KI-Modell herunterzuladen.

---

## 🎮 GPU-Beschleunigung (NVIDIA)

Für **10x schnellere** KI-Antworten brauchst du CUDA.

### Voraussetzungen

| Komponente | Minimum | Empfohlen |
|------------|---------|-----------|
| NVIDIA GPU | GTX 1060 (6GB) | RTX 3060 (12GB) |
| VRAM | 6 GB | 12 GB+ |
| CUDA | 11.8 | 12.x |
| Treiber | 520+ | 535+ |

### Windows: CUDA installieren

1. **NVIDIA Treiber aktualisieren**
   - [GeForce Experience](https://www.nvidia.com/de-de/geforce/geforce-experience/) öffnen
   - Auf Updates prüfen und installieren

2. **CUDA Toolkit installieren**
   - [CUDA Toolkit 12.x herunterladen](https://developer.nvidia.com/cuda-downloads?target_os=Windows)
   - "Express Installation" wählen
   - **Neustart erforderlich!**

3. **Installation prüfen**
   ```cmd
   nvidia-smi
   ```
   Sollte deine GPU und CUDA-Version anzeigen.

### Linux: CUDA installieren

**Ubuntu 22.04/24.04:**
```bash
# NVIDIA Treiber
sudo apt install nvidia-driver-535

# CUDA Toolkit
wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/cuda-keyring_1.1-1_all.deb
sudo dpkg -i cuda-keyring_1.1-1_all.deb
sudo apt update
sudo apt install cuda-toolkit-12-4

# Neustart
sudo reboot
```

**Prüfen:**
```bash
nvidia-smi
nvcc --version
```

### GPU-Nutzung in Fleet Navigator prüfen

1. Fleet Navigator starten
2. Einstellungen öffnen (⚙️)
3. Tab "Modellauswahl"
4. Unten: "Hardware & Performance"
5. **"CPU-Modus"** muss **AUS** sein

Bei korrekter GPU-Nutzung siehst du im `nvidia-smi`:
- GPU-Auslastung steigt beim Chat
- VRAM wird vom Modell belegt (4-8 GB typisch)

---

## 🐌 Ohne GPU (CPU-Modus)

Funktioniert auch ohne NVIDIA GPU, nur langsamer:

1. Einstellungen → Modellauswahl
2. "CPU-Modus (ohne GPU)" **einschalten**
3. Kleineres Modell wählen (z.B. 3B statt 7B Parameter)

**Erwartete Geschwindigkeit:**

| Hardware | Tokens/Sekunde | Antwortzeit |
|----------|----------------|-------------|
| RTX 3060 | ~40-60 t/s | 2-5 Sek |
| RTX 4090 | ~100+ t/s | <2 Sek |
| CPU (i7) | ~5-10 t/s | 15-30 Sek |
| CPU (Ryzen 7) | ~8-15 t/s | 10-20 Sek |

---

## 📦 Empfohlene Modelle

Beim ersten Start werden dir 3 Modelle angeboten:

| Modell | Größe | VRAM | Gut für |
|--------|-------|------|---------|
| **Qwen2.5-Coder 3B** | ~2 GB | 4 GB | Code, schnell |
| **Qwen2.5-Coder 7B** | ~4 GB | 8 GB | Code, ausgewogen |
| **Mistral 7B Instruct** | ~4 GB | 8 GB | Allgemein, deutsch |

**Empfehlung:**
- RTX 3060 (12GB): **Qwen2.5-Coder 7B**
- GTX 1060 (6GB): **Qwen2.5-Coder 3B**
- Nur CPU: **Qwen2.5-Coder 3B**

---

## ❓ Häufige Probleme

### "Java nicht gefunden"
→ Java 17 installieren (siehe Schritt 3)

### "Port 2025 bereits belegt"
→ Andere Anwendung beenden oder Port ändern:
```bash
java -jar bin/fleet-navigator.jar --server.port=2026
```

### GPU wird nicht erkannt
1. `nvidia-smi` prüfen - zeigt GPU?
2. CUDA installiert? `nvcc --version`
3. CPU-Modus ausgeschaltet?
4. Fleet Navigator neu starten

### Modell-Download bricht ab
→ Internetverbindung prüfen, erneut versuchen

---

## 🔗 Links

- **GitHub:** https://github.com/FranzHerstellJavaFleet/Fleet-Navigator
- **Releases:** https://github.com/FranzHerstellJavaFleet/Fleet-Navigator/releases
- **Issues:** https://github.com/FranzHerstellJavaFleet/Fleet-Navigator/issues

---

🚢 **Fleet Navigator** - Lokale KI für alle
