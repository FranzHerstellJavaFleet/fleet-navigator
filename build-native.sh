#!/bin/bash

# Fleet Navigator - GraalVM Native Image Build Script
# ===================================================

set -e

echo "🚀 Fleet Navigator - GraalVM Native Image Build"
echo "================================================"
echo ""

# Check if GraalVM is installed
if ! command -v native-image &> /dev/null; then
    echo "❌ Error: native-image command not found!"
    echo "   Please install GraalVM and run:"
    echo "   gu install native-image"
    exit 1
fi

# Show GraalVM version
echo "📦 GraalVM Version:"
java -version
echo ""

echo "🧹 Cleaning previous builds..."
mvn clean

echo ""
echo "🔨 Building JAR with frontend..."
mvn package -DskipTests

echo ""
echo "🎯 Building Native Image..."
echo "   This may take 5-15 minutes depending on your system..."
echo ""

mvn -Pnative native:compile -DskipTests

echo ""
echo "✅ Build Complete!"
echo ""
echo "📦 Native executable created:"
ls -lh target/fleet-navigator
echo ""
echo "🚀 To run the native image:"
echo "   ./target/fleet-navigator"
echo ""
echo "📊 Startup comparison:"
echo "   JVM:    ~3-5 seconds"
echo "   Native: ~0.1-0.5 seconds (10-50x faster!)"
echo ""
echo "💾 Memory comparison:"
echo "   JVM:    ~300-500 MB"
echo "   Native: ~50-100 MB (5-10x less!)"
