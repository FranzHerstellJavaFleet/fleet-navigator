# CLAUDE.md - Anweisungen für Claude Code

## ⚠️ WICHTIG: Lies dies ZUERST!

**Fleet Navigator ist EINE Anwendung - NICHT zwei!**

```
Fleet Navigator = Spring Boot Backend + Vue.js Frontend in EINEM JAR
```

### Das bedeutet:

1. **Production:** Ein einziges JAR-File, ein Server, Port 2025
2. **Development:** Temporär zwei Server für Hot-Reload (nur während Entwicklung!)

---

## 🏗️ Architektur

### Production Mode (Standard)

```
fleet-navigator.jar
├── Spring Boot Application (Port 2025)
│   ├── REST API (/api/*)
│   ├── Static Resources (Vue.js Frontend)
│   └── H2 Database
└── Alles in EINEM Prozess!
```

**Starten:**
```bash
mvn clean package
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar
# → http://localhost:2025
```

### Development Mode (nur für Entwicklung)

Temporär getrennt für Hot-Reload:

```
Terminal 1: Spring Boot (Port 2025) - Backend API
Terminal 2: Vite Dev Server (Port 5173) - Frontend mit Hot-Reload
```

**Warum?** Schnelle Frontend-Änderungen ohne Backend-Neustart

**Starten:**
```bash
./START.sh
# ODER manuell:
mvn spring-boot:run              # Terminal 1
cd frontend && npm run dev       # Terminal 2
```

**Wichtig:** Dies ist NUR für Entwicklung! Normale Nutzer verwenden das JAR!

---

## 🚫 Häufige Fehler vermeiden

### ❌ FALSCH: "Frontend und Backend starten"
```bash
# Das impliziert zwei separate Anwendungen - FALSCH!
```

### ✅ RICHTIG: "Fleet Navigator starten"
```bash
# Production
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar

# Development (mit Hot-Reload)
./START.sh
```

---

## 📦 Build-Prozess

```bash
mvn clean package
```

**Was passiert:**
1. Maven installiert Node.js + npm (target/)
2. `npm install` im frontend/ Ordner
3. `npm run build` → frontend/dist/
4. Kopiert dist/ nach target/classes/static/
5. Erstellt JAR mit Backend + Frontend

**Ergebnis:** Ein JAR-File mit ALLEM drin!

### ⚠️ WICHTIG: Vite Build-Cache-Problem

**Problem:** Vite cached manchmal alte Builds. Symptome:
- Frontend-Änderungen erscheinen nicht nach `mvn clean package`
- Browser lädt alte JavaScript-Dateien (alter Hash in Dateinamen)
- Console zeigt alte Fehler obwohl Code gefixt wurde

**Lösung (IMMER wenn Frontend-Änderungen nicht erscheinen):**

```bash
# 1. Lösche ALLE Build-Artefakte
rm -rf frontend/dist target

# 2. Baue FRISCH ohne Cache
mvn clean package -DskipTests

# 3. Update Service
sudo ./update-fleet-navigator.sh

# 4. Browser: NEUES Inkognito-Fenster oder Ctrl+Shift+R
```

**Erkennungsmerkmale:**
- ✅ Neue Version: JavaScript-Hash ändert sich (z.B. `index-CHZ3aMt5.js` → `index-XYZ123.js`)
- ❌ Alte Version: Hash bleibt gleich, obwohl Code geändert wurde

**Faustregel:** Bei Frontend-Änderungen IMMER `rm -rf frontend/dist target` VOR dem Build!

---

## 🔧 Technologie-Stack

- **Backend:** Spring Boot 3.2.0, Java 17
- **Frontend:** Vue.js 3 + Vite
- **Database:** H2 File-Based (persistent)
- **AI:** Ollama Integration
- **Build:** Maven Frontend Plugin

---

## 📝 Für Claude Code Entwickler

Wenn der Nutzer sagt:
- ❌ "Starte das Frontend" → Frage ob Development oder Production gemeint ist
- ❌ "Backend läuft nicht" → Kläre ab: Dev-Mode oder Production?
- ✅ "Starte Fleet Navigator" → Klar! Ein Befehl, eine App

**Immer daran denken:**
- Production = 1 Server, 1 JAR, Port 2025
- Development = 2 Server (temporär), Ports 2025 + 5173

---

## 🎯 Quick Reference

| Szenario | Befehl | URL |
|----------|--------|-----|
| **Normale Nutzung** | `java -jar target/*.jar` | http://localhost:2025 |
| **Entwicklung** | `./START.sh` | http://localhost:5173 |
| **Production Build** | `mvn clean package` | - |

---

## 🚀 Deployment

```bash
# Auf Server kopieren
scp target/fleet-navigator-0.1.0-SNAPSHOT.jar user@server:/opt/fleet-navigator/

# Auf Server starten
java -jar /opt/fleet-navigator/fleet-navigator-0.1.0-SNAPSHOT.jar
```

**Das war's!** Keine separate Frontend-Deployment nötig!

---

**Erstellt von:** JavaFleet Systems Consulting
**Port 2025:** Das Geburtsjahr von Fleet Navigator 🚢
