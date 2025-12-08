#!/bin/bash
# Auto-download models for Fleet Navigator (llama.cpp)

MODELS_DIR="/opt/fleet-navigator/models"
mkdir -p "$MODELS_DIR"

echo "📥 Lade Standard-Modelle für llama.cpp herunter..."
echo ""

# Qwen 2.5 3B - Kompaktes, schnelles Modell
echo "1️⃣  Qwen 2.5 3B (empfohlen, ~2GB)"
echo "   Quelle: HuggingFace"
if [ ! -f "$MODELS_DIR/qwen2.5-3b-instruct-q4_k_m.gguf" ]; then
    wget -O "$MODELS_DIR/qwen2.5-3b-instruct-q4_k_m.gguf" \
        "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
    echo "   ✅ Qwen 2.5 3B heruntergeladen"
else
    echo "   ⏭️  Bereits vorhanden"
fi

echo ""
echo "✅ Modell-Download abgeschlossen!"
echo ""
echo "📁 Modelle in: $MODELS_DIR"
ls -lh "$MODELS_DIR"/*.gguf 2>/dev/null || echo "   Keine GGUF-Modelle gefunden"
