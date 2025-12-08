@echo off
:: Fleet Navigator - Windows Uninstaller
:: ======================================
:: Startet das PowerShell Uninstall-Script

echo.
echo   Fleet Navigator - Deinstallation
echo   =================================
echo.

:: PowerShell mit Bypass starten
powershell -ExecutionPolicy Bypass -File "%~dp0uninstall.ps1"

:: Falls PowerShell fehlschlaegt
if errorlevel 1 (
    echo.
    echo PowerShell Uninstall fehlgeschlagen.
    echo Bitte manuell loeschen:
    echo   - %LOCALAPPDATA%\JavaFleet\
    echo.
    pause
)
