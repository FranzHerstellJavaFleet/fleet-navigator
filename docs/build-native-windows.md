# Native Image Build für Windows

## Voraussetzungen auf Windows

### 1. GraalVM für Windows installieren

**Option A: SDKMAN für Windows (Git Bash)**
```bash
# In Git Bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.2-graal
gu install native-image
```

**Option B: Scoop (PowerShell)**
```powershell
# Scoop installieren (falls nicht vorhanden)
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# GraalVM installieren
scoop bucket add java
scoop install graalvm-jdk21
gu install native-image
```

**Option C: Manueller Download**
1. Download: https://github.com/graalvm/graalvm-ce-builds/releases
2. Datei: `graalvm-community-jdk-21.0.2_windows-x64_bin.zip`
3. Entpacken nach `C:\graalvm`
4. Environment Variables setzen:
   ```
   JAVA_HOME=C:\graalvm
   PATH=%JAVA_HOME%\bin;%PATH%
   ```
5. Native Image installieren:
   ```cmd
   gu install native-image
   ```

### 2. Visual Studio Build Tools installieren

**WICHTIG:** GraalVM auf Windows benötigt die C++ Build Tools!

**Option A: Visual Studio 2022 Community (empfohlen)**
1. Download: https://visualstudio.microsoft.com/downloads/
2. Installiere "Desktop development with C++"
3. Minimum Components:
   - MSVC v143 - VS 2022 C++ x64/x86 build tools
   - Windows 10/11 SDK
   - C++ CMake tools for Windows

**Option B: Build Tools (ohne IDE)**
1. Download: https://visualstudio.microsoft.com/downloads/#build-tools-for-visual-studio-2022
2. Wähle "C++ build tools"

## Build-Prozess auf Windows

### Variante 1: Developer Command Prompt (empfohlen)

```cmd
# 1. "Developer Command Prompt for VS 2022" öffnen
# (Suche im Startmenü)

# 2. Zum Projektverzeichnis navigieren
cd C:\Projekte\Fleet-Navigator

# 3. Build starten
mvn clean package -DskipTests
mvn -Pnative native:compile -DskipTests
```

### Variante 2: PowerShell Script

Erstelle `build-native.ps1`:

```powershell
# Fleet Navigator - GraalVM Native Image Build (Windows)
# ======================================================

# Visual Studio Environment laden
$vsPath = & "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe" `
    -latest -property installationPath

if ($vsPath) {
    & "$vsPath\Common7\Tools\Launch-VsDevShell.ps1" -Arch amd64 -SkipAutomaticLocation
} else {
    Write-Error "Visual Studio 2022 nicht gefunden!"
    exit 1
}

# Java/GraalVM Version anzeigen
Write-Host "GraalVM Version:" -ForegroundColor Green
java -version

# Clean
Write-Host "`nCleaning..." -ForegroundColor Green
mvn clean

# Build JAR
Write-Host "`nBuilding JAR with Frontend..." -ForegroundColor Green
mvn package -DskipTests

# Build Native Image
Write-Host "`nBuilding Native Image (this may take 10-15 minutes)..." -ForegroundColor Green
mvn -Pnative native:compile -DskipTests

# Fertig
Write-Host "`n✅ Build Complete!" -ForegroundColor Green
Write-Host "Binary: target\fleet-navigator.exe" -ForegroundColor Cyan
