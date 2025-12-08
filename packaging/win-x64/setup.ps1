# Fleet Navigator - Windows Setup Script
# =======================================
# Kopiert Fleet Navigator nach %LOCALAPPDATA%\JavaFleet\Fleet-Navigator
# Erstellt Desktop- und Startmenu-Verknuepfungen
# Startet Fleet Navigator

param(
    [switch]$NoStart,
    [switch]$Uninstall
)

$ErrorActionPreference = "Stop"

# Konfiguration
$AppName = "Fleet Navigator"
$AppExe = "fleet-navigator.jar"
$Publisher = "JavaFleet"
$InstallDir = Join-Path $env:LOCALAPPDATA "JavaFleet\Fleet-Navigator"
$DesktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "$AppName.lnk"
$StartMenuFolder = Join-Path ([Environment]::GetFolderPath("StartMenu")) "Programs\$Publisher"
$StartMenuShortcut = Join-Path $StartMenuFolder "$AppName.lnk"

# Farbige Ausgabe
function Write-Info { Write-Host "INFO: " -ForegroundColor Cyan -NoNewline; Write-Host $args }
function Write-Success { Write-Host "OK: " -ForegroundColor Green -NoNewline; Write-Host $args }
function Write-Warn { Write-Host "WARNUNG: " -ForegroundColor Yellow -NoNewline; Write-Host $args }
function Write-Err { Write-Host "FEHLER: " -ForegroundColor Red -NoNewline; Write-Host $args }

# Banner
Write-Host ""
Write-Host "  ╔═══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "  ║                                                       ║" -ForegroundColor Cyan
Write-Host "  ║   🚢 Fleet Navigator - Windows Setup                  ║" -ForegroundColor Cyan
Write-Host "  ║                                                       ║" -ForegroundColor Cyan
Write-Host "  ╚═══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Deinstallation
if ($Uninstall) {
    Write-Info "Deinstalliere Fleet Navigator..."

    # Prozess beenden falls laeuft
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
        $_.CommandLine -like "*fleet-navigator*"
    } | Stop-Process -Force -ErrorAction SilentlyContinue

    # Verknuepfungen entfernen
    if (Test-Path $DesktopShortcut) {
        Remove-Item $DesktopShortcut -Force
        Write-Success "Desktop-Verknuepfung entfernt"
    }
    if (Test-Path $StartMenuShortcut) {
        Remove-Item $StartMenuShortcut -Force
        Write-Success "Startmenu-Verknuepfung entfernt"
    }
    if (Test-Path $StartMenuFolder) {
        Remove-Item $StartMenuFolder -Recurse -Force -ErrorAction SilentlyContinue
    }

    # Installationsverzeichnis entfernen
    if (Test-Path $InstallDir) {
        Remove-Item $InstallDir -Recurse -Force
        Write-Success "Installationsverzeichnis entfernt"
    }

    Write-Host ""
    Write-Success "Fleet Navigator wurde deinstalliert."
    Write-Host ""
    exit 0
}

# Java pruefen
Write-Info "Pruefe Java-Installation..."
try {
    $javaVersion = & java -version 2>&1 | Select-String -Pattern 'version "(\d+)'
    if ($javaVersion -match 'version "(\d+)') {
        $majorVersion = [int]$Matches[1]
        if ($majorVersion -ge 17) {
            Write-Success "Java $majorVersion gefunden"
        } else {
            Write-Err "Java 17 oder neuer wird benoetigt (gefunden: Java $majorVersion)"
            Write-Host ""
            Write-Host "Bitte installiere Java 17: https://adoptium.net/de/temurin/releases/?version=17" -ForegroundColor Yellow
            Write-Host ""
            Read-Host "Druecke Enter zum Beenden"
            exit 1
        }
    }
} catch {
    Write-Err "Java nicht gefunden!"
    Write-Host ""
    Write-Host "Bitte installiere Java 17: https://adoptium.net/de/temurin/releases/?version=17" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Druecke Enter zum Beenden"
    exit 1
}

# Quellverzeichnis ermitteln (wo setup.ps1 liegt)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SourceBin = Join-Path $ScriptDir "bin"
$SourceConfig = Join-Path $ScriptDir "config"

# Pruefen ob Quelldateien existieren
if (-not (Test-Path (Join-Path $SourceBin $AppExe))) {
    # Vielleicht liegt setup.ps1 im Hauptverzeichnis?
    $SourceBin = Join-Path $ScriptDir "..\bin"
    $SourceConfig = Join-Path $ScriptDir "..\config"

    if (-not (Test-Path (Join-Path $SourceBin $AppExe))) {
        Write-Err "fleet-navigator.jar nicht gefunden!"
        Write-Host "Bitte fuehre setup.ps1 aus dem entpackten Verzeichnis aus." -ForegroundColor Yellow
        Read-Host "Druecke Enter zum Beenden"
        exit 1
    }
}

# Installationsverzeichnis erstellen
Write-Info "Erstelle Installationsverzeichnis..."
if (-not (Test-Path $InstallDir)) {
    New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
}
$InstallBin = Join-Path $InstallDir "bin"
$InstallConfig = Join-Path $InstallDir "config"
New-Item -ItemType Directory -Path $InstallBin -Force | Out-Null
New-Item -ItemType Directory -Path $InstallConfig -Force | Out-Null

# Dateien kopieren
Write-Info "Kopiere Dateien..."
Copy-Item (Join-Path $SourceBin "*") $InstallBin -Recurse -Force
if (Test-Path $SourceConfig) {
    Copy-Item (Join-Path $SourceConfig "*") $InstallConfig -Recurse -Force -ErrorAction SilentlyContinue
}
Write-Success "Dateien kopiert nach: $InstallDir"

# Start-Script erstellen
$StartScript = Join-Path $InstallDir "start.bat"
$StartScriptContent = @"
@echo off
cd /d "%~dp0"
start "" javaw -jar bin\fleet-navigator.jar
timeout /t 3 /nobreak >nul
start http://localhost:2025
"@
$StartScriptContent | Out-File -FilePath $StartScript -Encoding ASCII
Write-Success "Start-Script erstellt"

# Desktop-Verknuepfung erstellen
Write-Info "Erstelle Desktop-Verknuepfung..."
$WshShell = New-Object -ComObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut($DesktopShortcut)
$Shortcut.TargetPath = $StartScript
$Shortcut.WorkingDirectory = $InstallDir
$Shortcut.Description = "Fleet Navigator - Lokale KI fuer alle"
$Shortcut.Save()
Write-Success "Desktop-Verknuepfung erstellt"

# Startmenu-Verknuepfung erstellen
Write-Info "Erstelle Startmenu-Verknuepfung..."
if (-not (Test-Path $StartMenuFolder)) {
    New-Item -ItemType Directory -Path $StartMenuFolder -Force | Out-Null
}
$Shortcut = $WshShell.CreateShortcut($StartMenuShortcut)
$Shortcut.TargetPath = $StartScript
$Shortcut.WorkingDirectory = $InstallDir
$Shortcut.Description = "Fleet Navigator - Lokale KI fuer alle"
$Shortcut.Save()
Write-Success "Startmenu-Verknuepfung erstellt"

# Fertig
Write-Host ""
Write-Host "  ╔═══════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ║   Installation abgeschlossen!                         ║" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ║   Installiert in:                                     ║" -ForegroundColor Green
Write-Host "  ║   $InstallDir" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ║   Starten:                                            ║" -ForegroundColor Green
Write-Host "  ║   - Desktop-Verknuepfung 'Fleet Navigator'            ║" -ForegroundColor Green
Write-Host "  ║   - Startmenu > JavaFleet > Fleet Navigator           ║" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ║   Web-Interface: http://localhost:2025                ║" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ║   Deinstallieren:                                     ║" -ForegroundColor Green
Write-Host "  ║   setup.ps1 -Uninstall                                ║" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ╚═══════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# Starten
if (-not $NoStart) {
    Write-Info "Starte Fleet Navigator..."
    Start-Process $StartScript -WorkingDirectory $InstallDir
    Write-Host ""
    Write-Host "Fleet Navigator startet... Browser oeffnet gleich automatisch." -ForegroundColor Cyan
    Write-Host ""
}

# Pause damit Fenster nicht sofort schliesst
if ($Host.Name -eq "ConsoleHost") {
    Write-Host "Druecke eine Taste zum Beenden..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
