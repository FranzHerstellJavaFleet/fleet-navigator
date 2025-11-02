# Fleet Navigator - Quick Start Guide 🚢

## 🎯 WICHTIG: Fleet Navigator ist EINE Anwendung!

**Fleet Navigator = Spring Boot Backend + Vue.js Frontend in EINEM JAR**

- **Development:** Zwei Server für Hot-Reload (Backend: 2025, Frontend: 5173)
- **Production:** Ein Server, ein JAR, alles auf Port 2025

---

## ⚡ Schnellstart (3 Schritte)

### 1. Ollama starten
```bash
ollama serve
```

### 2. Fleet Navigator starten
```bash
cd ~/NetBeansProjects/Projekte\ FMH/Fleet-Navigator
./START.sh
```

### 3. Browser öffnen

**Development (zwei Server für Hot-Reload):**
```
http://localhost:5173  ← Vite Dev Server (Frontend)
http://localhost:2025  ← Spring Boot (Backend API)
```

**Production (ein Server, ein JAR):**
```bash
mvn clean package
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar
# → http://localhost:2025  ← ALLES in einem!
```

**Das war's! 🎉**

---

## 🛠️ Manuelle Installation (einmalig)

### Backend (Spring Boot)
```bash
cd ~/NetBeansProjects/Projekte\ FMH/Fleet-Navigator
mvn clean install
```

### Frontend (Vue.js)
```bash
cd frontend
npm install
```

---

## 🚀 Starten

### ⚠️ WICHTIG: Development vs Production

Fleet Navigator ist **EINE Anwendung**, aber es gibt zwei Arten sie zu starten:

---

### 🔧 Development Mode (für Entwicklung)

**Warum?** Hot-Reload für schnelle Änderungen

**Wie?** Zwei Server temporär getrennt:
- Backend: Spring Boot auf Port 2025
- Frontend: Vite Dev Server auf Port 5173

**Option A: Start-Skript (Empfohlen)**
```bash
./START.sh
```

**Option B: Manuell**
```bash
# Terminal 1 - Backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend && npm run dev
```

**Browser:** http://localhost:5173

---

### 🚀 Production Mode (normale Nutzung)

**Was?** EINE Anwendung, EIN Server, EIN JAR

**Warum?** Einfaches Deployment, keine CORS-Probleme

**Wie?**
```bash
# Einmal bauen
mvn clean package

# Starten
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar
```

**Browser:** http://localhost:2025 (ALLES hier!)

Das JAR enthält:
- ✅ Spring Boot Backend
- ✅ Vue.js Frontend (kompiliert)
- ✅ Alle Dependencies
- ✅ Keine zwei Server mehr nötig!

---

## 📊 Verfügbare URLs

### Development Mode (zwei Server)

| Service | URL | Beschreibung |
|---------|-----|--------------|
| **Frontend** | http://localhost:5173 | Vite Dev Server (nur Dev!) |
| **Backend API** | http://localhost:2025 | REST API |
| **H2 Console** | http://localhost:2025/h2-console | Datenbank |
| **Ollama** | http://localhost:11434 | Ollama Server |

### Production Mode (ein Server)

| Service | URL | Beschreibung |
|---------|-----|--------------|
| **Alles!** | http://localhost:2025 | Frontend + Backend + API |
| **H2 Console** | http://localhost:2025/h2-console | Datenbank |
| **Ollama** | http://localhost:11434 | Ollama Server |

**Wichtig:** In Production gibt es KEINEN Port 5173 mehr!

---

## ✅ System-Check

### Ollama läuft?
```bash
curl http://localhost:11434/api/tags
```

### Backend läuft?
```bash
curl http://localhost:2025/api/models
```

### Frontend läuft?
Öffne Browser: http://localhost:5173

---

## 🎨 Features

### Implementiert ✅
- Chat-Interface (ähnlich ChatGPT/Claude)
- **Orange Theme** (JavaFleet Markenfarbe)
- Model-Switcher Dropdown
- System-Prompt Editor
- Token-Counter (Chat + Global)
- System-Monitoring (CPU, RAM)
- Chat-Historie Sidebar
- Streaming-Toggle (UI bereit)
- Stop-Button (UI bereit)
- Markdown-Support (Code-Blöcke)

### Nächste Phase 🚧
- WebSocket-Streaming (Backend)
- Stop-Funktionalität (Backend)
- GPU/VRAM-Monitoring
- Context-File-Upload

---

## 🐛 Problemlösung

### "Ollama is not running"
```bash
ollama serve
```

### "Backend failed to start"
```bash
# Prüfe ob Port 2025 frei ist
sudo lsof -i :2025

# Port freigeben falls belegt
kill -9 <PID>
```

### "npm install" Fehler
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### CORS-Fehler
In `src/main/java/.../config/WebConfig.java` prüfen ob:
```java
.allowedOrigins("http://localhost:5173")
```

---

## 📖 Weitere Dokumentation

- **Projekt-Plan:** `docs/PROJEKT-PLAN.md` (936 Zeilen)
- **API-Docs:** `docs/API-ENDPOINTS.md`
- **Frontend:** `frontend/README.md`
- **Main README:** `README.md`

---

## 🎯 Erste Schritte im UI

1. **Neuen Chat starten:** Klick "New Chat" in Sidebar
2. **Model wählen:** Dropdown in TopBar (z.B. "llama3.2:3b")
3. **Nachricht senden:** Tippe Nachricht und drücke Enter
4. **System-Prompt:** Klick "💭 System Prompt" Button
5. **Monitoring:** Klick "📊 Monitor" Button

---

## 🎉 Viel Erfolg!

**Entwickelt von:** JavaFleet Systems Consulting
**Kontakt:** Essen-Rüttenscheid, Deutschland

🚢 **Navigate your AI fleet with precision!**
