#!/bin/bash

# Fleet Navigator - GitHub Setup Script
# =====================================

set -e

echo "🚀 Fleet Navigator - GitHub Setup"
echo "=================================="
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "❌ Git is not installed!"
    echo "   Install with: sudo apt install git"
    exit 1
fi

# Initialize git if not already done
if [ ! -d ".git" ]; then
    echo "📦 Initializing Git repository..."
    git init
    echo "✅ Git initialized"
else
    echo "✅ Git repository already initialized"
fi

# Create .gitignore if not exists
if [ ! -f ".gitignore" ]; then
    echo "📝 .gitignore already exists"
fi

# Check git user config
if [ -z "$(git config --global user.name)" ]; then
    echo ""
    echo "⚙️  Git user not configured!"
    read -p "Enter your name: " git_name
    read -p "Enter your email: " git_email
    git config --global user.name "$git_name"
    git config --global user.email "$git_email"
    echo "✅ Git user configured"
fi

# Add all files
echo ""
echo "📦 Adding files to git..."
git add .

# Show status
echo ""
echo "📊 Git status:"
git status

# Create initial commit
echo ""
read -p "Create initial commit? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git commit -m "Initial commit: Fleet Navigator with GraalVM native image support"
    echo "✅ Initial commit created"
fi

# Instructions for GitHub
echo ""
echo "📚 Next steps:"
echo "=============="
echo ""
echo "1. Go to GitHub: https://github.com/new"
echo "2. Repository name: fleet-navigator"
echo "3. Choose Public or Private"
echo "4. DON'T initialize with README (we have code already!)"
echo "5. Click 'Create repository'"
echo ""
echo "6. Then run these commands (replace USERNAME with your GitHub username):"
echo ""
echo "   git remote add origin https://github.com/USERNAME/fleet-navigator.git"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "7. After push, go to GitHub → Your Repo → Actions tab"
echo "8. Click 'GraalVM Native Image Build' → 'Run workflow'"
echo "9. Wait ~20 minutes"
echo "10. Download binaries from 'Artifacts' section"
echo ""
echo "📖 For detailed instructions, see: GITHUB-ACTIONS-GUIDE.md"
echo ""
echo "✅ Setup complete!"
