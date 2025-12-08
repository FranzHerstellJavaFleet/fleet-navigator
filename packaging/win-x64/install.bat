@echo off
:: Fleet Navigator - Windows Installer
:: ====================================
:: Startet das PowerShell Setup-Script mit den richtigen Rechten

echo.
echo   Fleet Navigator - Installation
echo   ==============================
echo.

:: PowerShell mit Bypass starten (umgeht Execution Policy)
powershell -ExecutionPolicy Bypass -File "%~dp0setup.ps1"

:: Falls PowerShell fehlschlaegt, einfach direkt starten
if errorlevel 1 (
    echo.
    echo PowerShell Setup fehlgeschlagen - starte direkt...
    echo.
    cd /d "%~dp0"
    start "" javaw -jar bin\fleet-navigator.jar
    timeout /t 3 /nobreak >nul
    start http://localhost:2025
)
