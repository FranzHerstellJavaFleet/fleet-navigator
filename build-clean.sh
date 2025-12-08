#!/bin/bash
# Fleet Navigator - Sauberer Build
# Löscht ALLE alten Artefakte und baut komplett neu

set -e  # Bei Fehler abbrechen

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Fleet Navigator - Sauberer Build"
echo "=========================================="

# 1. ALLE alten Build-Artefakte löschen
echo ""
echo "[1/2] Lösche alte Build-Artefakte..."

rm -rf frontend/dist
[ ! -d "frontend/dist" ] && echo "      frontend/dist: GELÖSCHT" || echo "      frontend/dist: FEHLER!"

rm -rf frontend/node_modules/.vite
[ ! -d "frontend/node_modules/.vite" ] && echo "      .vite cache:   GELÖSCHT" || echo "      .vite cache:   FEHLER!"

rm -rf target
[ ! -d "target" ] && echo "      target:        GELÖSCHT" || echo "      target:        FEHLER!"

# 2. Maven Clean Package (inkl. npm install + npm build)
echo ""
echo "[2/2] Baue Projekt neu (Maven + npm)..."
mvn clean package -DskipTests

echo ""
echo "=========================================="
echo "Build erfolgreich!"
echo "=========================================="
echo "Nächster Schritt: sudo ./update-fleet-navigator.sh"
