# =====================================================
# Fleet Navigator - Windows Uninstall Script
# =====================================================
# Entfernt Fleet Navigator vollständig vom System
# Verwendung: .\uninstall.ps1 [-KeepData]
#
# Optionen:
#   -KeepData    Behält Benutzerdaten (Chats, Modelle, etc.)
# =====================================================

param(
    [switch]$KeepData,
    [switch]$Silent
)

$ErrorActionPreference = "SilentlyContinue"

# Konfiguration
$AppName = "Fleet Navigator"
$Publisher = "JavaFleet"
$InstallDir = Join-Path $env:LOCALAPPDATA "JavaFleet\Fleet-Navigator"
$DataDir = Join-Path $env:LOCALAPPDATA "JavaFleet"
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
Write-Host "  ║   🚢 Fleet Navigator - Deinstallation                 ║" -ForegroundColor Cyan
Write-Host "  ║                                                       ║" -ForegroundColor Cyan
Write-Host "  ╚═══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

if ($KeepData) {
    Write-Warn "Benutzerdaten werden beibehalten"
    Write-Host ""
}

# 1. Fleet Navigator Prozess stoppen
Write-Info "[1/5] Stoppe Fleet Navigator Prozesse..."

# Java-Prozesse mit fleet-navigator stoppen
$javaProcesses = Get-Process -Name "java", "javaw" -ErrorAction SilentlyContinue | Where-Object {
    try {
        $_.CommandLine -like "*fleet-navigator*"
    } catch {
        $false
    }
}

if ($javaProcesses) {
    $javaProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Success "Fleet Navigator Prozess gestoppt"
} else {
    Write-Host "  - Kein laufender Prozess" -ForegroundColor Gray
}

# llama-server stoppen
$llamaProcesses = Get-Process -Name "llama-server" -ErrorAction SilentlyContinue
if ($llamaProcesses) {
    $llamaProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Success "llama-server gestoppt"
}

Start-Sleep -Seconds 1

# 2. Desktop-Verknuepfung entfernen
Write-Info "[2/5] Entferne Desktop-Verknuepfung..."
if (Test-Path $DesktopShortcut) {
    Remove-Item $DesktopShortcut -Force
    Write-Success "Desktop-Verknuepfung entfernt"
} else {
    Write-Host "  - Keine Desktop-Verknuepfung gefunden" -ForegroundColor Gray
}

# 3. Startmenu-Verknuepfung entfernen
Write-Info "[3/5] Entferne Startmenu-Verknuepfung..."
if (Test-Path $StartMenuShortcut) {
    Remove-Item $StartMenuShortcut -Force
    Write-Success "Startmenu-Verknuepfung entfernt"
}
if (Test-Path $StartMenuFolder) {
    # Nur loeschen wenn leer oder nur fleet-navigator drin
    $items = Get-ChildItem $StartMenuFolder -ErrorAction SilentlyContinue
    if ($null -eq $items -or $items.Count -eq 0) {
        Remove-Item $StartMenuFolder -Force -Recurse -ErrorAction SilentlyContinue
        Write-Success "Startmenu-Ordner entfernt"
    }
} else {
    Write-Host "  - Keine Startmenu-Verknuepfung gefunden" -ForegroundColor Gray
}

# 4. Installationsverzeichnis entfernen
Write-Info "[4/5] Entferne Installationsverzeichnis..."
if (Test-Path $InstallDir) {
    Remove-Item $InstallDir -Recurse -Force -ErrorAction SilentlyContinue
    if (-not (Test-Path $InstallDir)) {
        Write-Success "Installationsverzeichnis entfernt: $InstallDir"
    } else {
        Write-Warn "Konnte Installationsverzeichnis nicht vollstaendig entfernen"
        Write-Host "  Bitte manuell loeschen: $InstallDir" -ForegroundColor Yellow
    }
} else {
    Write-Host "  - Kein Installationsverzeichnis gefunden" -ForegroundColor Gray
}

# 5. Benutzerdaten entfernen
Write-Info "[5/5] Benutzerdaten..."

if (Test-Path $DataDir) {
    if ($KeepData) {
        Write-Warn "Benutzerdaten beibehalten: $DataDir"
        Write-Host "    Enthaelt: Chats, Modelle, Einstellungen, Datenbank" -ForegroundColor Gray
    } else {
        Write-Host ""
        Write-Host "  ⚠️  WARNUNG: Alle Benutzerdaten werden geloescht!" -ForegroundColor Red
        Write-Host "    Verzeichnis: $DataDir" -ForegroundColor Yellow
        Write-Host "    Enthaelt:" -ForegroundColor Gray
        Write-Host "      - Alle Chat-Verlaeufe" -ForegroundColor Gray
        Write-Host "      - Heruntergeladene KI-Modelle (koennen mehrere GB sein)" -ForegroundColor Gray
        Write-Host "      - Experten-Konfigurationen" -ForegroundColor Gray
        Write-Host "      - Datenbank" -ForegroundColor Gray
        Write-Host ""

        if (-not $Silent) {
            $confirm = Read-Host "    Wirklich loeschen? (j/N)"
            if ($confirm -eq "j" -or $confirm -eq "J") {
                Remove-Item $DataDir -Recurse -Force -ErrorAction SilentlyContinue
                if (-not (Test-Path $DataDir)) {
                    Write-Success "Benutzerdaten entfernt"
                } else {
                    Write-Warn "Konnte Benutzerdaten nicht vollstaendig entfernen"
                    Write-Host "  Bitte manuell loeschen: $DataDir" -ForegroundColor Yellow
                }
            } else {
                Write-Host "  - Benutzerdaten beibehalten" -ForegroundColor Yellow
            }
        } else {
            # Silent mode - nicht loeschen ohne explizite Bestaetigung
            Write-Warn "Silent-Modus: Benutzerdaten nicht geloescht"
            Write-Host "  Zum Loeschen: .\uninstall.ps1 (ohne -Silent)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "  - Keine Benutzerdaten gefunden" -ForegroundColor Gray
}

# Registry-Eintraege entfernen (falls vorhanden)
$regPath = "HKCU:\Software\JavaFleet\Fleet-Navigator"
if (Test-Path $regPath) {
    Remove-Item $regPath -Recurse -Force -ErrorAction SilentlyContinue
    Write-Success "Registry-Eintraege entfernt"
}

# Zusammenfassung
Write-Host ""
Write-Host "  ╔═══════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ║   ✓ Fleet Navigator wurde deinstalliert              ║" -ForegroundColor Green
Write-Host "  ║                                                       ║" -ForegroundColor Green
if ($KeepData -or (Test-Path $DataDir)) {
Write-Host "  ║   Benutzerdaten: $DataDir" -ForegroundColor Green
}
Write-Host "  ║                                                       ║" -ForegroundColor Green
Write-Host "  ╚═══════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# Pause damit Fenster nicht sofort schliesst (wenn direkt ausgefuehrt)
if ($Host.Name -eq "ConsoleHost" -and -not $Silent) {
    Write-Host "Druecke eine Taste zum Beenden..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
