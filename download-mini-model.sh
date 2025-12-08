#!/bin/bash

# Download Mini Instruct Model for Fleet Navigator
# Llama-3.2-1B-Instruct-Q4_K_M (~700MB)

MODEL_DIR="./models"
mkdir -p "$MODEL_DIR"

echo "🚢 Downloading Llama-3.2-1B-Instruct-Q4_K_M..."
echo "Size: ~700MB"

cd "$MODEL_DIR"

# Download from HuggingFace
wget -c "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"

echo ""
echo "✅ Download abgeschlossen!"
echo ""
echo "Model gespeichert in: $(pwd)/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
echo ""
echo "🔧 Zum Verwenden:"
echo "1. Fleet Navigator öffnen: http://localhost:2025"
echo "2. Settings → GGUF Model Configuration"
echo "3. Create New Configuration:"
echo "   - Name: Llama 3.2 1B Instruct"
echo "   - Model Path: $(pwd)/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
echo "   - Context Size: 8192"
echo "   - GPU Layers: 999"
