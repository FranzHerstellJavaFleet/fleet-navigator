# GitHub Actions - Komplette Anleitung für Anfänger 🚀

## Was ist GitHub Actions?

GitHub Actions ist ein kostenloser CI/CD Service von GitHub, der automatisch Code compiliert, testet und deployed. Wir nutzen es, um Native Images für Windows, Linux und macOS zu bauen.

## Schritt 1: Git Repository initialisieren

```bash
cd "/home/trainer/NetBeansProjects/Projekte FMH/Fleet-Navigator"

# Git initialisieren
git init

# .gitignore erstellen (damit nicht alles hochgeladen wird)
cat > .gitignore << 'EOF'
# Maven
target/
!.mvn/wrapper/maven-wrapper.jar

# Node
node_modules/
frontend/node_modules/
frontend/dist/

# IntelliJ
.idea/
*.iml

# Eclipse
.classpath
.project
.settings/

# VS Code
.vscode/

# OS
.DS_Store
Thumbs.db

# Logs
*.log

# Database
*.db
*.h2.db

# Temp
tmp/
temp/
EOF

# Alle Dateien hinzufügen
git add .

# Ersten Commit erstellen
git commit -m "Initial commit: Fleet Navigator with GraalVM support"
```

## Schritt 2: GitHub Repository erstellen

### Option A: Über GitHub Website (Einfach)

1. **Gehe zu GitHub:** https://github.com
2. **Einloggen** (oder Account erstellen, falls noch nicht vorhanden)
3. **Neues Repository erstellen:**
   - Klicke oben rechts auf `+` → `New repository`
   - Repository Name: `fleet-navigator`
   - Description: "AI Fleet Navigator with Ollama integration"
   - **WICHTIG:** Wähle `Public` (für kostenlose Actions) oder `Private`
   - **NICHT** "Initialize with README" ankreuzen (wir haben schon Code!)
   - Klicke `Create repository`

4. **Repository URL kopieren:**
   - Kopiere die URL, z.B.: `https://github.com/DEINUSERNAME/fleet-navigator.git`

### Option B: Mit GitHub CLI (gh)

```bash
# GitHub CLI installieren (falls nicht vorhanden)
# Debian/Ubuntu:
sudo apt install gh

# Einloggen
gh auth login

# Repository erstellen
gh repo create fleet-navigator --public --source=. --remote=origin --push
```

## Schritt 3: Code zu GitHub pushen

```bash
# Remote hinzufügen (ersetze USERNAME mit deinem GitHub Username)
git remote add origin https://github.com/USERNAME/fleet-navigator.git

# Code hochladen
git branch -M main
git push -u origin main
```

**Wenn Authentifizierung fehlschlägt:**
```bash
# Option 1: SSH Key verwenden (empfohlen)
# SSH Key generieren
ssh-keygen -t ed25519 -C "deine@email.de"

# Public Key zu GitHub hinzufügen
cat ~/.ssh/id_ed25519.pub
# Kopiere die Ausgabe und füge sie auf GitHub hinzu:
# GitHub → Settings → SSH and GPG keys → New SSH key

# Remote auf SSH ändern
git remote set-url origin git@github.com:USERNAME/fleet-navigator.git

# Option 2: Personal Access Token
# GitHub → Settings → Developer settings → Personal access tokens → Generate new token
# Nutze den Token als Passwort beim git push
```

## Schritt 4: GitHub Actions aktivieren und nutzen

### 4.1 Workflow-Datei prüfen

Die Datei `.github/workflows/native-build.yml` ist bereits vorhanden!

```bash
# Prüfen
ls -la .github/workflows/native-build.yml
```

### 4.2 Workflow auf GitHub ansehen

1. Gehe zu deinem Repository auf GitHub
2. Klicke auf den Tab **"Actions"** (oben in der Mitte)
3. Du siehst jetzt "GraalVM Native Image Build"

### 4.3 Workflow manuell starten

**Methode 1: Über GitHub Website**

1. GitHub → Dein Repository → Tab "Actions"
2. Links: "GraalVM Native Image Build" klicken
3. Rechts: Button "Run workflow" klicken
4. Branch auswählen: `main`
5. Klicke grünen Button "Run workflow"

**Jetzt passiert:**
- ⏱️ Build startet (dauert ca. 15-20 Minuten)
- 🔵 Status: Gelb = Running
- ✅ Status: Grün = Success
- ❌ Status: Rot = Failed

**Methode 2: Mit GitHub CLI**

```bash
gh workflow run "GraalVM Native Image Build"
```

### 4.4 Build-Status verfolgen

**Website:**
1. GitHub → Actions → Klicke auf den laufenden Build
2. Siehst du 3 Jobs:
   - `build-native (ubuntu-latest)` - Linux
   - `build-native (windows-latest)` - Windows
   - `build-native (macos-latest)` - macOS
3. Klicke auf einen Job, um Logs zu sehen

**Terminal:**
```bash
# Liste alle Runs
gh run list --workflow="GraalVM Native Image Build"

# Verfolge aktuellen Run
gh run watch
```

### 4.5 Binaries downloaden

Nach erfolgreichem Build (grüner Haken):

**Website:**
1. GitHub → Actions → Erfolgreicher Build anklicken
2. Scrolle nach unten zu "Artifacts"
3. Download:
   - `fleet-navigator-linux-amd64`
   - `fleet-navigator-windows-amd64`
   - `fleet-navigator-macos-amd64`

**Terminal:**
```bash
# Liste Artifacts
gh run list --workflow="GraalVM Native Image Build" --limit 1

# Download (RUN_ID aus obigem Befehl)
gh run download RUN_ID
```

## Schritt 5: Automatische Releases erstellen

Mit Git-Tags kannst du automatisch Releases mit Binaries erstellen:

```bash
# Tag erstellen
git tag v1.0.0

# Tag pushen (triggert automatisch den Workflow!)
git push origin v1.0.0

# Nach dem Build:
# GitHub → Dein Repo → "Releases" → Siehst du v1.0.0 mit allen Binaries!
```

## Tipps & Tricks

### GitHub Actions Limits (Free Tier)

- ✅ **Public Repos:** Unbegrenzte Minutes
- ⚠️ **Private Repos:** 2000 Minutes/Monat
- 💾 **Artifacts:** 500 MB Storage, 30 Tage Retention

### Build-Zeit sparen

Nur bestimmte Plattform bauen:

```yaml
# In .github/workflows/native-build.yml ändern:
strategy:
  matrix:
    os: [ubuntu-latest]  # Nur Linux
    # os: [windows-latest]  # Nur Windows
    # os: [ubuntu-latest, windows-latest]  # Linux + Windows
```

### Fehlersuche

**Build failed?**

1. GitHub → Actions → Failed Run anklicken
2. Klicke auf den roten Job
3. Erweitere den fehlgeschlagenen Step
4. Lies die Error-Meldung

**Häufige Fehler:**

| Fehler | Lösung |
|--------|--------|
| "Class not found" | Reflection-Config erweitern |
| "Resource not found" | Resource-Config erweitern |
| "Out of memory" | Workflow RAM erhöhen |
| "Permission denied" | Scripts executable machen |

### Secrets für private Repos

Falls du später Secrets brauchst (z.B. für Docker Hub):

1. GitHub → Dein Repo → Settings → Secrets and variables → Actions
2. "New repository secret"
3. In Workflow nutzen: `${{ secrets.SECRET_NAME }}`

## Alternative: Lokal bauen

Falls GitHub Actions nicht geht:

```bash
# Linux
./build-native.sh

# Windows (auf Windows-PC)
.\build-native.ps1
```

## Zusammenfassung

```bash
# 1. Git initialisieren
git init
git add .
git commit -m "Initial commit"

# 2. GitHub Repo erstellen (auf github.com)
# 3. Remote hinzufügen
git remote add origin https://github.com/USERNAME/fleet-navigator.git
git push -u origin main

# 4. Workflow starten
# GitHub → Actions → "Run workflow"

# 5. Binaries downloaden
# GitHub → Actions → Build → Artifacts
```

## Nächste Schritte

Nach dem ersten erfolgreichen Build:

1. ✅ Teste die Binaries auf verschiedenen Systemen
2. 🚀 Erstelle ein Release mit `git tag v1.0.0`
3. 📦 Verteile die Binaries an Nutzer
4. 🔄 Bei Änderungen: Einfach `git push` → Neuer Build!

---

**Fragen?** GitHub Actions Dashboard zeigt alles an!
**Probleme?** Logs in GitHub Actions ansehen!
**Kostenlos?** Ja, für Public Repos! 🎉
