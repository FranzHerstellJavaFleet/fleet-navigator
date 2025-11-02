#!/bin/bash

# Fleet Navigator - Startup Script
# ==================================

echo "🚢 Fleet Navigator - Starting..."
echo ""

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "⚠️  Ollama is not running!"
    echo "Please start Ollama first:"
    echo "  ollama serve"
    echo ""
    exit 1
fi

echo "✅ Ollama is running"
echo ""

# Create data directory for H2 database (if not exists)
cd "$(dirname "$0")"
mkdir -p data
echo "✅ Database directory ready"
echo ""

# Start Backend
echo "🔧 Starting Spring Boot Backend..."
mvn spring-boot:run &
BACKEND_PID=$!

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 10

# Check if backend is running
if ! curl -s http://localhost:2025/api/models > /dev/null 2>&1; then
    echo "❌ Backend failed to start"
    kill $BACKEND_PID 2>/dev/null
    exit 1
fi

echo "✅ Backend is running on http://localhost:2025"
echo ""

# Start Frontend
echo "🎨 Starting Vue.js Frontend..."
cd frontend

# Install dependencies if not present
if [ ! -d "node_modules" ]; then
    echo "📦 Installing npm dependencies (first time)..."
    npm install
fi

npm run dev &
FRONTEND_PID=$!

echo ""
echo "════════════════════════════════════════════"
echo "🚢 Fleet Navigator is ready!"
echo "   Born in 2025 - Running on Port 2025"
echo "════════════════════════════════════════════"
echo ""
echo "Frontend:  http://localhost:5173"
echo "Backend:   http://localhost:2025"
echo "H2 Console: http://localhost:2025/h2-console"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Wait for Ctrl+C
trap "echo ''; echo 'Stopping Fleet Navigator...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT

# Keep script running
wait
