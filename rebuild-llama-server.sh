#!/bin/bash
# Fleet Navigator - llama-server Rebuild Script with CUDA Support
# ================================================================

set -e

echo "🔨 Rebuilding llama-server with CUDA support"
echo "=============================================="
echo ""

# Check for CUDA
if ! command -v nvcc &> /dev/null; then
    echo "❌ CUDA not found! Please install CUDA first."
    echo "   For Ubuntu: sudo apt install nvidia-cuda-toolkit"
    exit 1
fi

# Show CUDA version
echo "✅ CUDA detected:"
nvcc --version | grep "release"
echo ""

# Clone llama.cpp (fresh copy)
WORK_DIR="/tmp/llama-cpp-rebuild-$(date +%s)"
echo "📦 Cloning llama.cpp to $WORK_DIR"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

echo ""
echo "🔧 Configuring build with CUDA for RTX 3060 (Compute Capability 8.6)"
echo "   Options:"
echo "   - GGML_CUDA=ON (GPU acceleration)"
echo "   - CMAKE_CUDA_ARCHITECTURES=86 (RTX 3060)"
echo "   - LLAMA_CURL=OFF (no external dependencies)"
echo "   - BUILD_SHARED_LIBS=ON (shared libraries)"
echo ""

cmake -B build \
  -DGGML_CUDA=ON \
  -DCMAKE_CUDA_ARCHITECTURES=86 \
  -DLLAMA_CURL=OFF \
  -DBUILD_SHARED_LIBS=ON \
  -DCMAKE_BUILD_TYPE=Release

echo ""
echo "🏗️ Building llama-server (this may take 5-10 minutes)..."
cmake --build build --config Release --target llama-server -j$(nproc)

echo ""
echo "✅ Build complete!"
echo ""

# Verify the binary
BINARY_PATH="$WORK_DIR/llama.cpp/build/bin/llama-server"
if [ ! -f "$BINARY_PATH" ]; then
    echo "❌ Error: llama-server binary not found at $BINARY_PATH"
    exit 1
fi

echo "📊 Binary info:"
ls -lh "$BINARY_PATH"
echo ""

# Check dependencies
echo "🔍 Checking shared library dependencies:"
ldd "$BINARY_PATH" | grep -E "(llama|ggml)" || true
echo ""

# Test if it runs
echo "🧪 Testing llama-server..."
if "$BINARY_PATH" --version 2>&1 | grep -q "llama"; then
    echo "   ✅ llama-server works!"
else
    echo "   ⚠️ Warning: Could not verify llama-server version"
fi
echo ""

# Copy to Fleet Navigator
DEST_DIR="/home/trainer/NetBeansProjects/ProjekteFMH/Fleet-Navigator/bin"
echo "📦 Installing to Fleet Navigator..."
mkdir -p "$DEST_DIR"

# Backup old binary
if [ -f "$DEST_DIR/llama-server" ]; then
    echo "   📦 Backing up old binary to llama-server.old"
    mv "$DEST_DIR/llama-server" "$DEST_DIR/llama-server.old"
fi

# Copy new binary
cp "$BINARY_PATH" "$DEST_DIR/llama-server"
chmod +x "$DEST_DIR/llama-server"

# Copy required shared libraries
echo "   📦 Copying shared libraries..."
LIB_DIR="$WORK_DIR/llama.cpp/build/bin"
for lib in libggml.so* libggml-base.so* libggml-cuda.so* libllama.so*; do
    if [ -f "$LIB_DIR/$lib" ]; then
        cp "$LIB_DIR/$lib" "$DEST_DIR/"
        echo "      ✅ Copied $lib"
    fi
done

echo "   ✅ Installed to: $DEST_DIR/"
echo ""

# Verify installation
echo "🔍 Verifying installation:"
ls -lh "$DEST_DIR/"
echo ""

echo "🎯 Testing installed binary:"
cd /home/trainer/NetBeansProjects/ProjekteFMH/Fleet-Navigator
if LD_LIBRARY_PATH=./bin ./bin/llama-server --version 2>&1 | head -5; then
    echo ""
    echo "✅ Installation successful!"
else
    echo "⚠️ Warning: Binary test returned error (this might be OK)"
fi

echo ""
echo "🎉 llama-server rebuild complete!"
echo ""
echo "📝 What was installed:"
echo "   - llama-server (main binary)"
echo "   - libggml*.so (GGML libraries)"
echo "   - libllama.so (LLaMA libraries)"
echo "   - All with CUDA support for RTX 3060"
echo ""
echo "⚙️ Important: LlamaCppProvider needs to set LD_LIBRARY_PATH"
echo ""
echo "🚀 Next steps:"
echo "   1. Update LlamaCppProvider.java to set LD_LIBRARY_PATH"
echo "   2. Rebuild Fleet Navigator: mvn clean package"
echo "   3. Start Fleet Navigator"
echo "   4. Upload image and test Vision Support!"
echo ""
echo "🗑️ Cleanup (optional):"
echo "   rm -rf $WORK_DIR"
echo ""
