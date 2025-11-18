# Fleet Navigator - Windows Setup Script
# ========================================
# Automatische Installation für Windows 10/11
#
# Dieses Skript:
# - Prüft Java 21 Installation
# - Lädt llama.cpp Binary herunter
# - Lädt Standard-Modell herunter (Qwen 2.5 3B)
# - Konfiguriert Fleet Navigator
# - Erstellt Desktop-Verknüpfung (optional)
#
# Verwendung:
#   PowerShell als Administrator ausführen
#   .\setup-fleet-navigator.ps1

param(
    [string]$InstallDir = "$env:USERPROFILE\FleetNavigator",
    [switch]$SkipModel,
    [switch]$NoDesktopShortcut
)

# Farben für Output
function Write-Header {
    param([string]$Text)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host $Text -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Text)
    Write-Host "✓ $Text" -ForegroundColor Green
}

function Write-Info {
    param([string]$Text)
    Write-Host "ℹ $Text" -ForegroundColor Yellow
}

function Write-Error-Custom {
    param([string]$Text)
    Write-Host "✗ $Text" -ForegroundColor Red
}

function Write-Progress-Custom {
    param([string]$Text)
    Write-Host "⏳ $Text" -ForegroundColor Magenta
}

# Banner
Clear-Host
Write-Host @"

 ███████╗██╗     ███████╗███████╗████████╗
 ██╔════╝██║     ██╔════╝██╔════╝╚══██╔══╝
 █████╗  ██║     █████╗  █████╗     ██║
 ██╔══╝  ██║     ██╔══╝  ██╔══╝     ██║
 ██║     ███████╗███████╗███████╗   ██║
 ╚═╝     ╚══════╝╚══════╝╚══════╝   ╚═╝

 ███╗   ██╗ █████╗ ██╗   ██╗██╗ ██████╗  █████╗ ████████╗ ██████╗ ██████╗
 ████╗  ██║██╔══██╗██║   ██║██║██╔════╝ ██╔══██╗╚══██╔══╝██╔═══██╗██╔══██╗
 ██╔██╗ ██║███████║██║   ██║██║██║  ███╗███████║   ██║   ██║   ██║██████╔╝
 ██║╚██╗██║██╔══██║╚██╗ ██╔╝██║██║   ██║██╔══██║   ██║   ██║   ██║██╔══██╗
 ██║ ╚████║██║  ██║ ╚████╔╝ ██║╚██████╔╝██║  ██║   ██║   ╚██████╔╝██║  ██║
 ╚═╝  ╚═══╝╚═╝  ╚═╝  ╚═══╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝

"@ -ForegroundColor Cyan

Write-Host "Windows Setup - Automatische Installation" -ForegroundColor White
Write-Host "Version 0.3.0 | JavaFleet Systems Consulting`n" -ForegroundColor Gray

# Schritt 1: Java 21 Prüfung
Write-Header "Schritt 1/5: Java 21 Überprüfung"

Write-Progress-Custom "Prüfe Java Installation..."

try {
    $javaVersion = & java -version 2>&1 | Select-String -Pattern "version"
    $versionNumber = $javaVersion -replace '.*"(\d+).*', '$1'

    if ([int]$versionNumber -ge 21) {
        Write-Success "Java $versionNumber gefunden"
        Write-Host "   Pfad: $($(Get-Command java).Source)" -ForegroundColor Gray
    } else {
        Write-Error-Custom "Java $versionNumber ist zu alt (benötigt: Java 21+)"
        Write-Host "`n📥 Bitte Java 21 installieren von:" -ForegroundColor Yellow
        Write-Host "   https://adoptium.net/de/temurin/releases/?version=21`n" -ForegroundColor White
        exit 1
    }
} catch {
    Write-Error-Custom "Java nicht gefunden!"
    Write-Host "`n📥 Bitte Java 21 installieren von:" -ForegroundColor Yellow
    Write-Host "   https://adoptium.net/de/temurin/releases/?version=21" -ForegroundColor White
    Write-Host "`nNach Installation PowerShell neu starten und Skript erneut ausführen.`n" -ForegroundColor Gray
    exit 1
}

# Schritt 2: Verzeichnisse erstellen
Write-Header "Schritt 2/5: Verzeichnisstruktur erstellen"

Write-Progress-Custom "Erstelle Installations-Verzeichnis: $InstallDir"

$directories = @(
    $InstallDir,
    "$InstallDir\bin",
    "$InstallDir\models",
    "$InstallDir\data",
    "$InstallDir\logs"
)

foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Success "Erstellt: $dir"
    } else {
        Write-Info "Bereits vorhanden: $dir"
    }
}

# Schritt 3: llama.cpp Binary herunterladen
Write-Header "Schritt 3/5: llama.cpp Server herunterladen"

$llamaCppUrl = "https://github.com/ggerganov/llama.cpp/releases/latest/download/llama-b3886-bin-win-vulkan-x64.zip"
$llamaCppZip = "$env:TEMP\llama-cpp.zip"
$llamaCppExtract = "$env:TEMP\llama-cpp-extracted"

Write-Info "Download von: llama.cpp (Windows Vulkan Build)"
Write-Info "Quelle: GitHub - ggerganov/llama.cpp"
Write-Host ""

Write-Progress-Custom "Lade llama-server Binary herunter (~50 MB)..."

try {
    # Download mit Progress
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $llamaCppUrl -OutFile $llamaCppZip -UseBasicParsing
    $ProgressPreference = 'Continue'

    $fileSize = (Get-Item $llamaCppZip).Length / 1MB
    Write-Success "Download abgeschlossen ($([math]::Round($fileSize, 2)) MB)"

    Write-Progress-Custom "Entpacke Archive..."

    # Cleanup old extraction
    if (Test-Path $llamaCppExtract) {
        Remove-Item $llamaCppExtract -Recurse -Force
    }

    Expand-Archive -Path $llamaCppZip -DestinationPath $llamaCppExtract -Force

    # Finde und kopiere llama-server.exe
    $llamaServerExe = Get-ChildItem -Path $llamaCppExtract -Filter "llama-server.exe" -Recurse | Select-Object -First 1

    if ($llamaServerExe) {
        Copy-Item $llamaServerExe.FullName -Destination "$InstallDir\bin\llama-server.exe" -Force
        Write-Success "llama-server.exe installiert"

        # Kopiere auch DLLs falls vorhanden
        $dllFiles = Get-ChildItem -Path $llamaServerExe.DirectoryName -Filter "*.dll"
        foreach ($dll in $dllFiles) {
            Copy-Item $dll.FullName -Destination "$InstallDir\bin\" -Force
            Write-Success "Kopiert: $($dll.Name)"
        }
    } else {
        Write-Error-Custom "llama-server.exe nicht gefunden im Archive"
        exit 1
    }

    # Cleanup
    Remove-Item $llamaCppZip -Force
    Remove-Item $llamaCppExtract -Recurse -Force

} catch {
    Write-Error-Custom "Fehler beim Download: $_"
    Write-Host "`n💡 Alternative: Manuell herunterladen von:" -ForegroundColor Yellow
    Write-Host "   https://github.com/ggerganov/llama.cpp/releases/latest" -ForegroundColor White
    Write-Host "   Datei: llama-b*-bin-win-vulkan-x64.zip" -ForegroundColor White
    Write-Host "   Entpacken nach: $InstallDir\bin\`n" -ForegroundColor White
    exit 1
}

# Schritt 4: Fleet Navigator JAR prüfen
Write-Header "Schritt 4/5: Fleet Navigator JAR"

$jarFile = Get-ChildItem -Path "target" -Filter "fleet-navigator-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1

if ($jarFile) {
    Write-Progress-Custom "Kopiere Fleet Navigator JAR..."
    Copy-Item $jarFile.FullName -Destination "$InstallDir\fleet-navigator.jar" -Force
    $jarSize = $jarFile.Length / 1MB
    Write-Success "fleet-navigator.jar installiert ($([math]::Round($jarSize, 2)) MB)"
} else {
    Write-Error-Custom "fleet-navigator.jar nicht gefunden in target/"
    Write-Host "`n⚠️  Bitte zuerst bauen mit:" -ForegroundColor Yellow
    Write-Host "   mvn clean package`n" -ForegroundColor White
    exit 1
}

# Schritt 5: Modell herunterladen (optional)
Write-Header "Schritt 5/5: KI-Modell herunterladen"

if ($SkipModel) {
    Write-Info "Modell-Download übersprungen (Parameter -SkipModel)"
    Write-Host "   Modell kann später in der Anwendung heruntergeladen werden.`n" -ForegroundColor Gray
} else {
    $modelUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
    $modelFile = "$InstallDir\models\qwen2.5-3b-instruct-q4_k_m.gguf"

    Write-Info "Modell: Qwen 2.5 3B Instruct (empfohlen)"
    Write-Info "Größe: ~2 GB"
    Write-Info "Quelle: HuggingFace - Qwen/Qwen2.5-3B-Instruct-GGUF"
    Write-Host ""

    if (Test-Path $modelFile) {
        Write-Success "Modell bereits vorhanden, überspringe Download"
    } else {
        Write-Progress-Custom "Lade Modell herunter... (Das kann 5-10 Minuten dauern)"
        Write-Host "   💡 Tipp: Tee oder Kaffee holen! ☕`n" -ForegroundColor Gray

        try {
            # Download mit Progress Bar
            $webClient = New-Object System.Net.WebClient

            # Progress Event
            $webClient.DownloadProgressChanged += {
                param($sender, $e)
                $percent = $e.ProgressPercentage
                $downloaded = $e.BytesReceived / 1MB
                $total = $e.TotalBytesToReceive / 1MB

                Write-Progress -Activity "Modell Download" `
                    -Status "$([math]::Round($downloaded, 2)) MB von $([math]::Round($total, 2)) MB" `
                    -PercentComplete $percent
            }

            # Download starten
            $downloadTask = $webClient.DownloadFileTaskAsync($modelUrl, $modelFile)

            while (-not $downloadTask.IsCompleted) {
                Start-Sleep -Milliseconds 200
            }

            Write-Progress -Activity "Modell Download" -Completed

            if ($downloadTask.IsFaulted) {
                throw $downloadTask.Exception
            }

            $modelSize = (Get-Item $modelFile).Length / 1GB
            Write-Success "Modell heruntergeladen ($([math]::Round($modelSize, 2)) GB)"

        } catch {
            Write-Error-Custom "Fehler beim Download: $_"
            Write-Host "`n💡 Alternative: Modell manuell herunterladen von:" -ForegroundColor Yellow
            Write-Host "   $modelUrl" -ForegroundColor White
            Write-Host "   Speichern als: $modelFile`n" -ForegroundColor White
        }
    }
}

# Desktop-Verknüpfung erstellen (optional)
if (-not $NoDesktopShortcut) {
    Write-Header "Desktop-Verknüpfung erstellen"

    $desktopPath = [Environment]::GetFolderPath("Desktop")
    $shortcutPath = "$desktopPath\Fleet Navigator.lnk"

    try {
        $WScriptShell = New-Object -ComObject WScript.Shell
        $shortcut = $WScriptShell.CreateShortcut($shortcutPath)
        $shortcut.TargetPath = "java.exe"
        $shortcut.Arguments = "-jar `"$InstallDir\fleet-navigator.jar`""
        $shortcut.WorkingDirectory = $InstallDir
        $shortcut.Description = "Fleet Navigator - Private AI Chat"
        $shortcut.Save()

        Write-Success "Desktop-Verknüpfung erstellt"
    } catch {
        Write-Info "Desktop-Verknüpfung konnte nicht erstellt werden"
    }
}

# Start-Skript erstellen
Write-Header "Start-Skript erstellen"

$startScript = @"
@echo off
title Fleet Navigator
echo.
echo ========================================
echo    Fleet Navigator wird gestartet...
echo ========================================
echo.

cd /d "$InstallDir"

echo Pruefe llama-server...
if not exist "bin\llama-server.exe" (
    echo FEHLER: llama-server.exe nicht gefunden!
    echo Bitte Setup erneut ausfuehren.
    pause
    exit /b 1
)

echo Starte llama-server im Hintergrund...
start /B bin\llama-server.exe --port 8081 --n-gpu-layers 999 --model models\qwen2.5-3b-instruct-q4_k_m.gguf

timeout /t 3 /nobreak >nul

echo Starte Fleet Navigator...
java -jar fleet-navigator.jar

echo.
echo Fleet Navigator wurde beendet.
pause
"@

$startScript | Out-File -FilePath "$InstallDir\start-fleet-navigator.bat" -Encoding ASCII
Write-Success "Start-Skript erstellt: start-fleet-navigator.bat"

# Konfigurationsdatei erstellen
Write-Header "Konfiguration erstellen"

$config = @"
# Fleet Navigator Configuration
# ==============================

# Server Port
server.port=2025

# Datenbank
spring.datasource.url=jdbc:h2:file:./data/fleetnavdb
spring.jpa.hibernate.ddl-auto=update

# llama.cpp Server
llm.llamacpp.server-url=http://localhost:8081
llm.llamacpp.models-dir=./models

# Logging
logging.file.name=./logs/fleet-navigator.log
logging.level.io.javafleet=INFO
"@

$config | Out-File -FilePath "$InstallDir\application.properties" -Encoding UTF8
Write-Success "Konfiguration erstellt: application.properties"

# Fertig!
Write-Header "Installation abgeschlossen!"

Write-Host @"

   🎉 Fleet Navigator wurde erfolgreich installiert!

📁 Installations-Verzeichnis:
   $InstallDir

🚀 Starten:
   1. Doppelklick auf Desktop-Verknüpfung "Fleet Navigator"
      ODER
   2. Doppelklick auf: $InstallDir\start-fleet-navigator.bat

🌐 Nach dem Start im Browser öffnen:
   http://localhost:2025

📊 Installierte Komponenten:
   ✓ Fleet Navigator JAR
   ✓ llama-server (llama.cpp)
   ✓ Qwen 2.5 3B Modell (2 GB)
   ✓ Konfiguration

💡 Tipps:
   - Beim ersten Start kann es 30-60 Sekunden dauern
   - llama-server lädt das Modell in den Speicher (braucht ~3 GB RAM)
   - Bei Problemen: Logs in $InstallDir\logs\

📚 Dokumentation:
   - README.md im Projekt-Verzeichnis
   - Online: https://github.com/FranzHerstellJavaFleet/fleet-navigator

🆘 Support:
   - GitHub Issues: https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues
   - E-Mail: franz-martin@java-developer.online

"@ -ForegroundColor White

Write-Host "Drücke eine beliebige Taste zum Beenden..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
