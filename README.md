# Fleet Navigator 🚢

**Deine private AI - kostenlos, lokal und ohne Cloud**

Eine benutzerfreundliche Anwendung für Gespräche mit künstlicher Intelligenz - entwickelt von JavaFleet Systems Consulting.

**Powered by llama.cpp** - Die schnellste lokale AI-Engine für dein System!

![Fleet Navigator Screenshot](screenshotFleetNavigator.png)

---

## 🆕 NEU: Version 0.3.1 - Fleet Mates & RAG-Support!

### 🤝 Fleet Mates - Verteilte KI-Agenten

**Fleet Navigator wird zum Zentrum eines verteilten Agentennetzwerks!**

- **Fleet Mate Linux** - System-Agent für Linux-Server und -Desktops
  - Echtzeit-Systemüberwachung (CPU, RAM, Festplatte, Temperatur)
  - Log-Analyse und Terminal-Zugriff
  - Sicheres Pairing mit Ende-zu-Ende-Verschlüsselung (TweetNaCl)

- **Fleet Email Mate** - Thunderbird-Extension für E-Mail-KI
  - Automatische E-Mail-Kategorisierung
  - KI-gestützte Terminvorschläge
  - Statistiken über verarbeitete E-Mails

### 📎 RAG - Dokumente im Chat analysieren

**Erweiterte Dateiunterstützung für Retrieval-Augmented Generation:**

| Typ | Formate |
|-----|---------|
| **Dokumente** | PDF, TXT, Markdown, HTML, JSON, XML, CSV |
| **Bilder** | PNG, JPG/JPEG, WebP, BMP, GIF, TIFF |

- Vision-Modelle analysieren Bilder automatisch
- Automatischer Modell-Wechsel bei Bild-Upload
- Text-Extraktion aus PDFs mit Apache PDFBox

### 🔐 Sicheres Mate-Pairing

- NaCl-Verschlüsselung für alle Verbindungen
- QR-Code-basiertes Pairing (geplant)
- Mate-Verwaltung im Fleet Mates Dashboard

---

**Fleet Navigator läuft als Java JAR** - schneller zu bauen, einfacher zu installieren!

### ⚡ Automatische Setup-Skripte für alle Plattformen:

- **Windows:** `setup-fleet-navigator.ps1` - Vollautomatisch mit PowerShell
- **macOS:** `setup-fleet-navigator-macos.sh` - Erkennt Apple Silicon/Intel automatisch
- **Linux:** `setup-fleet-navigator-linux.sh` - Mit systemd Service Support

**Alle Skripte:**
- ✅ Prüfen Java 21 (wird automatisch erkannt)
- ✅ Laden llama.cpp herunter
- ✅ Laden KI-Modell herunter (~2 GB)
- ✅ Konfigurieren alles
- ✅ Erstellen Desktop-Verknüpfung/Service

**→ Siehe [Installation](#-installation-schritt-für-schritt) unten für Details**

---

## 🎯 Für wen ist Fleet Navigator?

Fleet Navigator ist für **jeden**, der eine private AI nutzen möchte:

- 📝 **Briefe schreiben**: Bewerbungen, Kündigungen, Geschäftsbriefe
- 💬 **Fragen stellen**: Zu jedem Thema - Geschichte, Wissenschaft, Alltag
- 🌐 **Übersetzen**: Texte in viele Sprachen
- 📚 **Lernen**: Komplexe Themen einfach erklärt
- 💻 **Programmieren**: Code schreiben und verstehen (für Entwickler)

**Deine Daten bleiben bei dir!** Im Gegensatz zu ChatGPT läuft alles auf deinem Computer.

---

## ✨ Was kann Fleet Navigator?

### Grundfunktionen
- ✅ **Privatsphäre**: Alle Daten bleiben auf deinem Computer
- ✅ **Kostenlos**: Keine monatlichen Gebühren
- ✅ **Offline**: Funktioniert ohne Internet (nach dem Setup)
- ✅ **Model Store**: Lade AI-Modelle direkt aus HuggingFace herunter - keine externe Software nötig!
- ✅ **Blitzschnell**: Powered by llama.cpp - optimiert für deine Hardware (CPU & GPU)

### RAG & Dokumentenanalyse
- ✅ **Dokumente hochladen**: PDF, TXT, MD, HTML, JSON, XML, CSV
- ✅ **Bilder analysieren**: PNG, JPG, WebP, BMP, GIF, TIFF (mit Vision-Modellen)
- ✅ **Auto Vision-Switch**: Wechselt automatisch zu Vision-Modell bei Bild-Upload
- ✅ **PDF-Extraktion**: Automatische Textextraktion aus PDF-Dateien

### Fleet Mates - Verteilte Agenten
- ✅ **Fleet Mate Linux**: System-Agent für Linux (CPU, RAM, Logs, Terminal)
- ✅ **Fleet Email Mate**: Thunderbird-Extension für E-Mail-KI
- ✅ **Sicheres Pairing**: NaCl-verschlüsselte Verbindungen
- ✅ **Live-Dashboard**: Echtzeit-Überwachung aller verbundenen Mates

### Brief-Assistent
- ✅ **Brief-Agent**: Generiert Briefe und öffnet sie automatisch in Word/LibreOffice
- ✅ **Brief-Vorlagen**: Fertige Vorlagen für häufige Schreibanlässe
- ✅ **Brief-Assistenten**: Spezialisierte Prompts für Behördenbriefe (Kita, Finanzamt, Stadtverwaltung)

### Weitere Features
- ✅ **System Prompts**: 12+ vorkonfigurierte Persönlichkeiten (Karla, Steuerberater, Pirat, Shakespeare, uvm.)
- ✅ **Multi-Sprache**: Deutsch, Englisch - automatische Erkennung der Systemsprache
- ✅ **Vision Support**: Analysiere Bilder und PDFs mit Vision-Modellen (llava, bakllava)
- ✅ **Projekte & Chats**: Organisiere deine Gespräche in Projekten
- ✅ **Collapsible Sidebar**: Mehr Platz durch ausblendbare Seitenleiste
- ✅ **System-Check**: Prüft automatisch, ob alles richtig installiert ist

---

## 💻 Systemanforderungen

Fleet Navigator läuft auf fast jedem modernen PC - hier sind die Empfehlungen für optimale Leistung:

**⚠️ NEU:** Fleet Navigator benötigt jetzt **Java 21** (wird automatisch von Setup-Skripten geprüft)

### Minimum (Für kleine Modelle wie Llama-3.2-1B)
- **Java**: OpenJDK 21 oder neuer
- **Prozessor**: Intel Core i3 / AMD Ryzen 3 (ab 2015)
- **RAM**: 4 GB
- **Speicher**: 5 GB frei
- **Grafikkarte**: Nicht erforderlich (läuft auf CPU)
- **Betriebssystem**: Windows 10+, macOS 12+, Linux (glibc 2.31+)

### Empfohlen (Für mittlere Modelle wie Qwen2.5-3B)
- **Prozessor**: Intel Core i5 / AMD Ryzen 5 (ab 2018) oder neuer
- **RAM**: 8 GB
- **Speicher**: 10 GB frei
- **Grafikkarte**: Optional - NVIDIA GTX 1050+ mit 4GB VRAM (für GPU-Beschleunigung)
- **Betriebssystem**: Windows 10+, macOS 12+, Linux

### Optimal (Für große Modelle wie Qwen2.5-7B)
- **Prozessor**: Intel Core i7 / AMD Ryzen 7 oder besser
- **RAM**: 16 GB oder mehr
- **Speicher**: 20 GB frei
- **Grafikkarte**: NVIDIA RTX 3060+ mit 8GB+ VRAM (stark empfohlen!)
- **Betriebssystem**: Windows 11, macOS 13+, Linux

### 🎮 GPU-Beschleunigung (Optional aber empfohlen!)

**NVIDIA GPUs (CUDA):**
- ✅ **GeForce RTX Serie**: RTX 4090, 4080, 4070, 3090, 3080, 3070, 3060
- ✅ **GeForce GTX Serie**: GTX 1660, 1650, 1080 Ti, 1070
- ✅ **Quadro/Tesla**: Für Workstations und Server
- 📊 **Geschwindigkeit**: Bis zu 10x schneller als CPU!

**AMD GPUs (ROCm - Linux):**
- ✅ Radeon RX 7000 Serie
- ✅ Radeon RX 6000 Serie
- ⚠️ Eingeschränkter Support, Linux erforderlich

**Apple Silicon (M1/M2/M3):**
- ✅ Metal Acceleration automatisch aktiviert
- 🚀 Sehr schnell dank unified memory

**Intel GPUs:**
- ⚠️ Experimenteller Support via oneAPI

### 📊 Modellgröße vs. RAM/VRAM

| Modell | RAM (CPU) | VRAM (GPU) | Tokens/Sek (CPU) | Tokens/Sek (GPU) |
|--------|-----------|------------|------------------|------------------|
| **Llama-3.2-1B** | 4 GB | 2 GB | 15-25 | 60-100 |
| **Qwen2.5-3B** | 8 GB | 4 GB | 8-15 | 40-80 |
| **Qwen2.5-7B** | 16 GB | 8 GB | 3-8 | 25-50 |
| **Llama-3.1-70B** | 64 GB | 48 GB | <1 | 5-15 |

**Tipp:** Mit GPU ist die Antwortgeschwindigkeit deutlich höher - für die beste Erfahrung nutze eine NVIDIA GPU mit mindestens 4GB VRAM!

---

## 🚀 Installation (Schritt für Schritt)

**NEU:** Automatische Setup-Skripte machen die Installation super einfach!

### Windows (Automatisch)

#### Voraussetzung: Java 21
Falls noch nicht installiert:
1. Download: https://adoptium.net/de/temurin/releases/?version=21
2. Installer ausführen und durchklicken

#### Installation:
1. Repository herunterladen oder klonen
2. PowerShell **als Administrator** öffnen
3. Zum Projektordner navigieren:
   ```powershell
   cd C:\Pfad\zu\fleet-navigator
   ```
4. Setup-Skript ausführen:
   ```powershell
   .\setup-fleet-navigator.ps1
   ```

**Das Skript:**
- Prüft Java 21 Installation
- Lädt llama.cpp herunter (~50 MB)
- Lädt Qwen 2.5 3B Modell herunter (~2 GB)
- Erstellt Desktop-Verknüpfung
- Konfiguriert alles

**Nach Installation:** Doppelklick auf "Fleet Navigator" auf dem Desktop!

### macOS (Automatisch)

#### Voraussetzung: Java 21
```bash
# Mit Homebrew
brew install openjdk@21

# Oder manuell von:
# https://adoptium.net/de/temurin/releases/?version=21
```

#### Installation:
```bash
# Repository klonen oder ZIP herunterladen
cd /pfad/zu/fleet-navigator

# Setup-Skript ausführen
chmod +x setup-fleet-navigator-macos.sh
./setup-fleet-navigator-macos.sh
```

**Das Skript:**
- Erkennt automatisch Apple Silicon (M1/M2/M3) vs Intel
- Lädt passende llama.cpp Binary
- Lädt Qwen 2.5 3B Modell (~2 GB)
- Erstellt LaunchAgent für Autostart

**Nach Installation:**
```bash
cd ~/Applications/FleetNavigator
./start-fleet-navigator.sh
```

Browser öffnen: http://localhost:2025

### Linux (Automatisch)

#### Voraussetzung: Java 21
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openjdk-21-jdk

# Fedora
sudo dnf install java-21-openjdk

# Arch
sudo pacman -S jdk-openjdk
```

#### Desktop Installation:
```bash
cd /pfad/zu/fleet-navigator

chmod +x setup-fleet-navigator-linux.sh
./setup-fleet-navigator-linux.sh
```

#### Server Installation mit systemd:
```bash
# Repository klonen oder bauen
mvn clean package -DskipTests

# Setup mit systemd Service
sudo ./setup-fleet-navigator-linux.sh --systemd

# Service starten
sudo systemctl start fleet-navigator

# Autostart aktivieren
sudo systemctl enable fleet-navigator
```

**Das Skript:**
- Erkennt x86_64 vs ARM64
- Lädt llama.cpp Binary
- Lädt Modell (~2 GB)
- Optional: Erstellt systemd Service

**Nach Installation:**
```bash
# Als User
cd /opt/fleet-navigator
./start-fleet-navigator.sh

# Oder als Service
sudo systemctl start fleet-navigator
```

Browser öffnen: http://localhost:2025

---

### Manuelle Installation

Falls die Setup-Skripte nicht funktionieren, siehe:
- **Detaillierte Anleitung:** [docs/INSTALL.md](docs/INSTALL.md)
- **systemd Deployment:** [docs/JAR-DEPLOYMENT.md](docs/JAR-DEPLOYMENT.md)
- **GitHub Releases:** https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases

#### Schritt 1: Fleet Navigator herunterladen
```bash
# Gehe zu Releases und lade herunter:
wget https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest/download/fleet-navigator-linux-amd64.tar.gz

# Entpacken:
tar -xzf fleet-navigator-linux-amd64.tar.gz
cd fleet-navigator
```

#### Schritt 2: Starten
```bash
./fleet-navigator
```

Browser: http://localhost:2025

#### Schritt 3: AI-Modell herunterladen
1. Im Fleet Navigator klicke auf **"Modelle"** in der Sidebar
2. Wähle ein Modell aus:
   - **Qwen2.5-3B-Instruct** (empfohlen für 8GB RAM) - Deutsch + Englisch
   - **Llama-3.2-1B-Instruct** (für 4GB RAM) - Englisch
   - **Llava-1.5-7B** (für Bildanalyse) - benötigt 8GB+ RAM
3. Klicke auf **"Download"**
4. Der Download läuft direkt von HuggingFace - kein Ollama oder andere Tools nötig!
5. Live-Progress zeigt MB/s und verbleibende Zeit
6. Nach dem Download ist das Modell sofort verfügbar!

---

## 📖 Erste Schritte

### 1. System-Check

Beim ersten Start prüft Fleet Navigator automatisch:
- ✅ Ist die llama.cpp Engine bereit?
- ✅ Ist ein AI-Modell heruntergeladen?
- ✅ Ist genug Arbeitsspeicher verfügbar?

**Falls noch kein Modell vorhanden ist**, wirst du automatisch zum Model Store weitergeleitet!

**Empfehlung für den Start:**
- **4-8 GB RAM**: Llama-3.2-1B-Instruct (~1.3 GB Download)
- **8-16 GB RAM**: Qwen2.5-3B-Instruct (~2 GB Download)
- **16+ GB RAM**: Qwen2.5-7B-Instruct (~4.4 GB Download)

### 2. Brief schreiben

1. Klicke auf das **Dokument-Symbol** oben rechts
2. Wähle eine **Vorlage** aus (z.B. "Bewerbungsschreiben")
3. Ersetze die Platzhalter `[...]` mit deinen Informationen
4. Klicke auf **"Generieren"**
5. Die AI erstellt den Brief - du kannst ihn kopieren und anpassen

### 3. Fragen stellen

1. Tippe deine Frage unten in das Eingabefeld
2. Drücke Enter
3. Die AI antwortet in wenigen Sekunden

**Beispiel-Fragen:**
- "Erkläre mir, wie Photosynthese funktioniert"
- "Was ist der Unterschied zwischen Java und JavaScript?"
- "Schreibe ein Gedicht über den Herbst"
- "Wie kündige ich meinen Handyvertrag?"

### 4. Dokumente analysieren

1. Klicke auf das **Büroklammer-Symbol** 📎
2. Wähle ein PDF oder Bild aus
3. Stelle eine Frage zum Dokument
4. Die AI analysiert das Dokument und antwortet

---

## 💡 Tipps für gute Ergebnisse

### Klare Anweisungen geben
❌ Schlecht: "Schreib was über Bewerbung"
✅ Gut: "Schreibe ein Bewerbungsschreiben für eine Stelle als Softwareentwickler bei Microsoft. Betone meine 5 Jahre Erfahrung mit Java und Python."

### Kontext angeben
Je mehr Informationen du gibst, desto besser die Antwort:
- Wer ist der Empfänger?
- Was ist der Anlass?
- Welcher Ton ist passend? (formell/informell)

### Schritt für Schritt
Bei komplexen Aufgaben: Teile sie in mehrere Fragen auf.

---

## 🔧 Empfohlene AI-Modelle (aus HuggingFace Model Store)

Fleet Navigator lädt Modelle **direkt aus HuggingFace** herunter - keine externe Software nötig!

### Kuratierte Modelle (empfohlen für Einsteiger)

| Modell | Größe | RAM | Am besten für |
|--------|-------|-----|---------------|
| **Llama-3.2-1B-Instruct** | 1.3 GB | 4-8 GB | Schnelle Antworten, Englisch |
| **Qwen2.5-3B-Instruct** | 2 GB | 8 GB | **Beste Wahl!** Deutsch + Englisch, gute Qualität |
| **Qwen2.5-7B-Instruct** | 4.4 GB | 16 GB | Noch bessere Qualität, mehrsprachig |
| **Llava-1.5-7B** | 4.7 GB | 8+ GB | **Bildanalyse** - PDF/Fotos verstehen |
| **DeepSeek-Coder-1.3B** | 1.5 GB | 4-8 GB | Programmierung, Code-Generierung |

### HuggingFace Suche (Tausende weitere Modelle!)

1. Klicke im Model Store auf **"HuggingFace durchsuchen"**
2. Suche nach Modellen (z.B. "german", "vision", "code")
3. Filtere nach:
   - **Popular** - Meistgenutzte Modelle
   - **German** - Deutsche Sprachmodelle
   - **Instruct** - Chat/Dialog-Modelle
   - **Code** - Programmier-Assistenten
   - **Vision** - Bildanalyse

### Welches Modell für mich?

- **4-8 GB RAM**: Llama-3.2-1B oder DeepSeek-Coder-1.3B
- **8-16 GB RAM**: **Qwen2.5-3B** (beste Wahl!) oder Llava-1.5-7B (Bilder)
- **16-32 GB RAM**: Qwen2.5-7B oder Qwen2.5-Coder-7B
- **32+ GB RAM**: Llama-3.1-70B oder Qwen2.5-14B

**Tipp:** Lade zuerst ein kleines Modell (Qwen2.5-3B) zum Testen herunter!

---

## 📝 Brief-Agent - Automatische Textverarbeitung

Der Brief-Agent ist ein **besonderes Feature** für Briefe und Dokumente:

### Wie funktioniert's?

1. **Klicke auf "Briefe"** in der Sidebar
2. **Wähle eine Vorlage** oder beschreibe deinen Brief
3. **AI generiert den Brief** mit deinen persönlichen Daten
4. **Brief öffnet sich automatisch** in deiner Textverarbeitung
5. **Rechtschreibprüfung**, Formatierung, als PDF speichern oder drucken

### Welche Textverarbeitung?

Der Brief-Agent unterstützt **mehrere Programme**:

| Betriebssystem | Programme |
|----------------|-----------|
| **Windows** | Microsoft Word, LibreOffice, OnlyOffice, Notepad |
| **macOS** | Microsoft Word, LibreOffice, OnlyOffice, TextEdit |
| **Linux** | LibreOffice, OnlyOffice, WPS Office, AbiWord, gedit |

**Empfehlung:**
- **Windows**: Microsoft Word (wenn vorhanden) oder LibreOffice (kostenlos)
- **macOS/Linux**: LibreOffice (kostenlos)

### Wo werden Briefe gespeichert?

Alle generierten Briefe werden in diesem Ordner abgelegt:

- **Windows**: `C:\Users\[Dein Name]\FleetNavigator\Documents\`
- **macOS**: `/Users/[Dein Name]/FleetNavigator/Documents/`
- **Linux**: `/home/[Dein Name]/FleetNavigator/Documents/`

### Persönliche Daten

Beim ersten Start erstellt Fleet Navigator **Platzhalter-Daten** (Max Mustermann, Musterweg 1, 12345 Musterstadt).

**Diese Daten MUSST du ersetzen!**

1. Klicke auf **"Einstellungen"** in der Sidebar
2. Gehe zu **"Persönliche Daten"**
3. Trage deine echten Daten ein
4. Speichern

Ab jetzt werden **alle Briefe automatisch** mit deinen Daten erstellt!

---

## 🤝 Fleet Mates - Verteilte KI-Agenten

Fleet Navigator kann mit **Fleet Mates** verbunden werden - spezialisierten Agenten, die auf anderen Geräten laufen.

### Verfügbare Mates

| Mate | Plattform | Funktion |
|------|-----------|----------|
| **Fleet Mate Linux** | Linux (Go) | Systemüberwachung, Log-Analyse, Terminal |
| **Fleet Email Mate** | Thunderbird (JS) | E-Mail-Kategorisierung, Terminplanung |

### Pairing-Prozess

1. Starte den Mate auf dem Zielgerät
2. Der Mate sendet eine **Pairing-Anfrage** an Fleet Navigator
3. Im Fleet Navigator unter **"Fleet Mates"** erscheint die Anfrage
4. Klicke auf **"Akzeptieren"** um die Verbindung herzustellen
5. Der Mate ist jetzt vertrauenswürdig und sendet Daten

### Dashboard

Nach erfolgreichem Pairing siehst du im **Fleet Mates Dashboard**:

- **System-Agents (blau)**: CPU, RAM, Temperatur, Festplattennutzung
- **Email-Agents (lila)**: Verarbeitete E-Mails, Kategorien-Statistiken
- **Live-Updates**: Echtzeit-Daten über WebSocket

### Sicherheit

- **NaCl-Verschlüsselung**: Alle Verbindungen sind Ende-zu-Ende verschlüsselt
- **Einmalige Schlüssel**: Jeder Mate generiert ein eigenes Schlüsselpaar
- **Lokales Netzwerk**: Kommunikation bleibt in deinem Netzwerk

---

## ❓ Häufige Fragen

### Die Anwendung startet nicht
**Lösung:**
1. Prüfe, ob Port 2025 bereits belegt ist:
   ```bash
   # Linux/macOS
   lsof -ti:2025

   # Windows
   netstat -ano | findstr :2025
   ```
2. Starte Fleet Navigator neu

### "Keine Modelle gefunden"
**Lösung:**
1. Klicke auf **"Modelle"** in der Sidebar
2. Wähle ein Modell aus der kuratierten Liste (z.B. Qwen2.5-3B)
3. Klicke auf **"Download"**
4. Warte, bis der Download abgeschlossen ist
5. Das Modell ist sofort verfügbar!

### Download sehr langsam
**Ursache:** HuggingFace Server können bei großen Modellen langsam sein

**Lösung:**
- Lade kleinere Modelle zuerst (Qwen2.5-3B statt 7B)
- Prüfe deine Internetverbindung
- Download läuft im Hintergrund - du kannst den Browser schließen

### Antworten sind sehr langsam
**Ursachen:**
- Zu wenig RAM → Nutze kleineres Modell
- CPU zu schwach → Nutze Modell mit weniger Parametern
- Andere Programme schließen

### Kann ich mehrere Modelle gleichzeitig nutzen?
Ja! Fleet Navigator wählt automatisch das beste Modell für deine Aufgabe.

### Kostet das etwas?
**Nein!** Fleet Navigator, llama.cpp und alle Modelle von HuggingFace sind komplett kostenlos und Open Source.

### Funktioniert es offline?
Ja, nach der Installation und dem Download der Modelle benötigst du kein Internet mehr.

---

## 🔐 Datenschutz & Sicherheit

✅ **Keine Cloud**: Alle Daten bleiben auf deinem Computer
✅ **Keine Tracking**: Wir sammeln keine Nutzungsdaten
✅ **Open Source**: Der gesamte Code ist einsehbar
✅ **Lokale Datenbank**: Chats werden nur lokal gespeichert

### Wo werden meine Chats gespeichert?

- **Windows**: `C:\Users\[Dein Name]\.fleet-navigator\`
- **macOS**: `/Users/[Dein Name]/.fleet-navigator/`
- **Linux**: `/home/[Dein Name]/.fleet-navigator/`

Du kannst diese Dateien jederzeit löschen oder sichern.

---

## 🛠️ Für Entwickler

### Aus Quellcode bauen

```bash
# Repository klonen
git clone https://github.com/FranzHerstellJavaFleet/fleet-navigator.git
cd fleet-navigator

# Mit Maven bauen
mvn clean package

# Starten
java -jar target/fleet-navigator-0.3.1.jar
```

### Development Mode

```bash
# Backend starten (Port 2025)
mvn spring-boot:run

# Frontend starten (Port 5173)
cd frontend
npm install
npm run dev
```

### Technologie-Stack

- **Backend**: Spring Boot 3.2, Java 17
- **Frontend**: Vue.js 3, Vite, Tailwind CSS
- **Datenbank**: H2 (file-based)
- **AI Engine**: llama.cpp (via java-llama.cpp JNI bindings)
- **Model Source**: HuggingFace Model Hub (GGUF format)
- **Monitoring**: OSHI (Hardware-Überwachung)

### Native Image (GraalVM)

Für schnelleren Start und weniger RAM-Verbrauch:

```bash
mvn -Pnative clean package
```

Mehr Infos: [NATIVE-IMAGE.md](docs/NATIVE-IMAGE.md)

---

## 📞 Unterstützung & Community

### Bei Problemen

1. **System-Check** im Browser aufrufen: http://localhost:2025
2. **Logs** prüfen (Terminal-Ausgabe)
3. **GitHub Issues**: https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues

### Dokumentation

- [GraalVM Native Image Guide](docs/NATIVE-IMAGE.md)
- [HuggingFace Integration](docs/HUGGINGFACE-INTEGRATION.md)
- [Vision Support Status](docs/VISION-SUPPORT-STATUS.md)
- [GitHub Actions Setup](docs/GITHUB-ACTIONS-GUIDE.md)
- [Vollständige Doku-Index](docs/INDEX.md)

### Updates

Fleet Navigator prüft **nicht** automatisch auf Updates.
Neue Versionen findest du auf GitHub unter "Releases".

---

## 🖥️ Server-Deployment (für Admins)

Fleet Navigator kann als **systemd-Service** auf Linux-Servern installiert werden.

### Schnellinstallation

```bash
# Download und Entpacken
wget https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/latest/download/fleet-navigator-linux-amd64.tar.gz
tar -xzf fleet-navigator-linux-amd64.tar.gz
cd fleet-navigator

# Installation als systemd-Service
sudo ./install-service.sh

# Service starten
sudo systemctl start fleet-navigator
sudo systemctl enable fleet-navigator

# Status prüfen
sudo systemctl status fleet-navigator
```

### Vorteile

- ✅ Automatischer Start beim Systemstart
- ✅ Prozess-Überwachung und Neustart bei Fehler
- ✅ Security Hardening (Sandboxing)
- ✅ Zentrale Log-Verwaltung mit journald
- ✅ Absolute Pfade für Modelle und Datenbank
- ✅ Environment-Variablen für flexible Konfiguration

### Konfiguration

Die Service-Konfiguration ist in `/etc/systemd/system/fleet-navigator.service`:

```ini
# Modelle-Pfad anpassen
Environment="LLM_LLAMACPP_MODELS_DIR=/mnt/storage/models"

# Port ändern
Environment="SERVER_PORT=8080"

# Externes Modell-Storage einbinden
Environment="LLM_LLAMACPP_MODELS_DIR=/nfs/shared/ai-models"
```

Nach Änderungen:
```bash
sudo systemctl daemon-reload
sudo systemctl restart fleet-navigator
```

### Logs anzeigen

```bash
# Live-Logs verfolgen
sudo journalctl -u fleet-navigator -f

# Logs der letzten Stunde
sudo journalctl -u fleet-navigator --since "1 hour ago"
```

### Dokumentation

Detaillierte Anleitungen findest du in:
- **[docs/SYSTEMD-DEPLOYMENT.md](docs/SYSTEMD-DEPLOYMENT.md)** - Vollständige systemd-Anleitung
- **[docs/INSTALL.md](docs/INSTALL.md)** - Installations-Guide für alle Plattformen

---

## 📜 Lizenz

Fleet Navigator ist Open Source Software unter der MIT-Lizenz.
Du darfst die Software frei nutzen, verändern und weitergeben.

---

## 🙏 Danksagungen

- **llama.cpp Team** - Für die schnellste lokale AI-Engine
- **HuggingFace** - Für den Zugang zu tausenden Open-Source-Modellen
- **Spring Boot** & **Vue.js** Communities
- **Meta** - Für die Llama-Modelle
- **Alibaba Cloud** - Für die Qwen-Modelle
- **Georgi Gerganov** - Für llama.cpp
- **kherud** - Für java-llama.cpp JNI bindings

---

## 🚢 Entwickelt von

**JavaFleet Systems Consulting**
Port 2025 - Das Geburtsjahr von Fleet Navigator

---

## 📥 Download

Native Binaries (ohne JDK!) für Windows, macOS und Linux sind unter **Releases** verfügbar:
https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases

---

**🌟 Gefällt dir Fleet Navigator? Gib uns einen Stern auf GitHub!**

https://github.com/FranzHerstellJavaFleet/fleet-navigator
