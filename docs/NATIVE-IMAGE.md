# GraalVM Native Image Build

Fleet Navigator kann als natives Binary mit GraalVM kompiliert werden für:
- ⚡ **10-50x schnelleren Start** (~0.1-0.5s statt 3-5s)
- 💾 **5-10x weniger RAM** (~50-100MB statt 300-500MB)
- 📦 **Keine JVM benötigt** - Standalone-Binary
- 🖥️ **Multi-Platform:** Linux, Windows, macOS

## ⚠️ Wichtig: Cross-Compilation

**GraalVM unterstützt KEINE Cross-Compilation!**

Das bedeutet:
- ✅ Linux Binary muss auf Linux gebaut werden
- ✅ Windows Binary muss auf Windows gebaut werden
- ✅ macOS Binary muss auf macOS gebaut werden

**Lösung:** Verwende GitHub Actions (siehe unten) für Multi-Platform Builds!

## Voraussetzungen

### Windows

**Siehe:** [build-native-windows.md](build-native-windows.md) für detaillierte Windows-Anleitung

**Kurz:**
1. GraalVM für Windows installieren
2. Visual Studio 2022 Build Tools (C++)
3. PowerShell Script ausführen: `.\build-native.ps1`

### Linux

### 1. GraalVM installieren

**Option A: SDKMAN (empfohlen)**
```bash
# SDKMAN installieren (falls nicht vorhanden)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# GraalVM installieren
sdk install java 21.0.1-graal

# Native Image Tool installieren
gu install native-image
```

**Option B: Manuelle Installation**
```bash
# Download von https://www.graalvm.org/downloads/
wget https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.1/graalvm-community-jdk-21.0.1_linux-x64_bin.tar.gz

# Entpacken
tar -xzf graalvm-community-jdk-21.0.1_linux-x64_bin.tar.gz
sudo mv graalvm-community-openjdk-21.0.1+12.1 /opt/graalvm

# Environment variables setzen
export JAVA_HOME=/opt/graalvm
export PATH=$JAVA_HOME/bin:$PATH

# Native Image installieren
gu install native-image
```

### 2. System-Dependencies (Linux)

```bash
# Debian/Ubuntu
sudo apt-get install build-essential libz-dev zlib1g-dev

# Fedora/RHEL
sudo dnf install gcc glibc-devel zlib-devel libstdc++-static
```

## Build-Prozess

### Option 1: Mit Build-Script (einfach)

```bash
./build-native.sh
```

### Option 2: Manuell

```bash
# 1. Normales JAR bauen
mvn clean package -DskipTests

# 2. Native Image erstellen
mvn -Pnative native:compile -DskipTests
```

### Option 3: Nur Native Image (JAR muss bereits existieren)

```bash
mvn -Pnative native:compile -DskipTests
```

## Ausführen

```bash
# Native Image starten
./target/fleet-navigator

# Mit Optionen
./target/fleet-navigator --server.port=8080
```

## Bekannte Einschränkungen

### ✅ Funktioniert:
- Spring Boot REST API
- JPA/Hibernate mit H2
- WebSocket für Streaming
- Ollama Integration
- Vue.js Frontend (statische Ressourcen)
- OSHI Hardware-Monitoring (mit JNI)
- PDF-Verarbeitung (PDFBox)

### ⚠️ Potenzielle Probleme:
- **Reflection**: Alle Klassen müssen in `reflect-config.json` registriert sein
- **Dynamic Proxies**: Können Probleme verursachen
- **Class Loading**: Kein dynamisches Laden zur Laufzeit
- **OSHI auf Windows**: Kann zusätzliche Konfiguration benötigen

## Troubleshooting

### Problem: "native-image: command not found"
**Lösung:**
```bash
gu install native-image
```

### Problem: Build-Fehler wegen fehlender Reflection-Config
**Lösung:** Klasse zu `src/main/resources/META-INF/native-image/reflect-config.json` hinzufügen:
```json
{
  "name": "your.package.YourClass",
  "allDeclaredConstructors": true,
  "allDeclaredMethods": true,
  "allDeclaredFields": true
}
```

### Problem: Resources nicht gefunden
**Lösung:** Pattern zu `src/main/resources/META-INF/native-image/resource-config.json` hinzufügen:
```json
{
  "pattern": "\\Qyour/resource/path/\\E.*"
}
```

### Problem: Build dauert sehr lange / Out of Memory
**Lösung:**
```bash
# Mehr RAM für native-image Build
export MAVEN_OPTS="-Xmx8g"
mvn -Pnative native:compile -DskipTests
```

## Performance-Vergleich

| Metrik | JVM (java -jar) | Native Image |
|--------|----------------|--------------|
| **Startup** | 3-5 Sekunden | 0.1-0.5 Sekunden |
| **RAM (Idle)** | 300-500 MB | 50-100 MB |
| **RAM (Load)** | 500-800 MB | 100-200 MB |
| **Binary Size** | ~100 MB (JAR + JVM) | ~80-150 MB |
| **Build Time** | 30 Sekunden | 5-15 Minuten |

## Advanced: Build-Optimierungen

### Kleinere Binary
```bash
# UPX compression
upx --best --lzma target/fleet-navigator

# Vor: 150 MB
# Nach: 40-50 MB
```

### Noch schnellerer Start
```xml
<buildArg>--initialize-at-build-time</buildArg>
<buildArg>-march=native</buildArg>
<buildArg>-O3</buildArg>
```

### PGO (Profile-Guided Optimization)
```bash
# 1. Mit Instrumentation bauen
mvn -Pnative native:compile -Dpgo.instrument=true

# 2. Binary ausführen und Profile sammeln
./target/fleet-navigator

# 3. Mit Profil optimiert bauen
mvn -Pnative native:compile -Dpgo.profiles-path=default.iprof
```

## Distribution

```bash
# Binary für Distribution vorbereiten
strip target/fleet-navigator  # Debug-Symbole entfernen
upx target/fleet-navigator     # Komprimieren

# Erstelle Tarball
tar -czf fleet-navigator-native-linux-amd64.tar.gz \
  target/fleet-navigator \
  README.md \
  LICENSE
```

## CI/CD Integration - Multi-Platform Build 🚀

### GitHub Actions (Automatisch für Linux, Windows, macOS)

Die Datei `.github/workflows/native-build.yml` ist bereits vorbereitet!

**Nutzung:**

1. **Manueller Trigger:**
   - Gehe zu GitHub → Actions → "GraalVM Native Image Build"
   - Klicke "Run workflow"
   - Warte 10-20 Minuten
   - Download Binaries unter "Artifacts"

2. **Automatisch bei Tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   → Erstellt automatisch Release mit allen Binaries!

**Was wird gebaut:**
- ✅ `fleet-navigator-linux-amd64.tar.gz` (Linux 64-bit)
- ✅ `fleet-navigator-windows-amd64.zip` (Windows 64-bit .exe)
- ✅ `fleet-navigator-macos-amd64.tar.gz` (macOS 64-bit)

**Download:**
- Artifacts: 30 Tage verfügbar
- Releases: Permanent

### Andere CI/CD Systeme

```yaml
# GitLab CI Example
build-native-linux:
  image: ghcr.io/graalvm/graalvm-ce:21.0.2
  script:
    - gu install native-image
    - ./build-native.sh
  artifacts:
    paths:
      - target/fleet-navigator

build-native-windows:
  tags: [windows]
  script:
    - ./build-native.ps1
  artifacts:
    paths:
      - target/fleet-navigator.exe
```

---

**Tipp:** Für Entwicklung weiter das normale JAR verwenden (schnellerer Build).
Native Image ist ideal für Production-Deployments! 🚀
