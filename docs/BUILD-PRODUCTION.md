# Fleet Navigator - Production Build 🚀

## 📦 Alles in einem JAR - Keine zwei Server mehr!

Fleet Navigator wird jetzt als **ein einziges JAR** gebaut, das sowohl Backend als auch Frontend enthält.

---

## 🎯 Production Build erstellen

### Ein Befehl - Alles drin!

```bash
cd ~/NetBeansProjects/Projekte\ FMH/Fleet-Navigator
mvn clean package
```

**Das passiert automatisch:**
1. ✅ Maven installiert Node.js und npm
2. ✅ `npm install` läuft automatisch
3. ✅ Vue.js Frontend wird gebaut (`npm run build`)
4. ✅ Frontend-Dateien werden ins JAR kopiert
5. ✅ Spring Boot JAR wird erstellt

**Ergebnis:** `target/fleet-navigator-0.1.0-SNAPSHOT.jar` (enthält ALLES!)

---

## 🚀 Production JAR starten

```bash
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar
```

**Nur noch EIN Server - Port 2025! (Geburtsjahr von Fleet Navigator)**

Öffne Browser: **http://localhost:2025**

✅ Kein Vite Dev Server mehr nötig!
✅ Keine CORS-Probleme!
✅ Einfaches Deployment!

---

## 🔄 Development vs Production

### Development (wie bisher)

**Vorteil:** Hot-Reload, schnelle Änderungen

**Backend:** IntelliJ Run Button
**Frontend:** `cd frontend && npm run dev`
**URLs:**
- Frontend: http://localhost:5173
- Backend: http://localhost:2025

### Production ⭐ (NEU)

**Vorteil:** Ein JAR, ein Server, fertig!

**Build:** `mvn clean package`
**Start:** `java -jar target/*.jar`
**URL:** http://localhost:2025 (ALLES)

---

## 📁 Was wird ins JAR gepackt

```
fleet-navigator.jar
├── BOOT-INF/
│   ├── classes/
│   │   ├── static/              # Vue.js Frontend (hier!)
│   │   │   ├── index.html
│   │   │   ├── assets/
│   │   │   └── ...
│   │   ├── io/javafleet/...     # Java Backend
│   │   └── application.properties
│   └── lib/                      # Dependencies
└── org/springframework/boot/     # Spring Boot Loader
```

**Spring Boot liefert automatisch:**
- `/` → `static/index.html` (Vue.js App)
- `/api/*` → REST Controllers (Backend)

---

## 🎨 Frontend-Build-Prozess

### Was macht `mvn package`?

```bash
# 1. Maven Plugin installiert Node.js
[INFO] Installing node version v18.18.0

# 2. npm install
[INFO] Running 'npm install'

# 3. Vue.js Build
[INFO] Running 'npm run build'
# Erstellt: frontend/dist/

# 4. Kopiere nach Spring Boot
[INFO] Copying frontend/dist/ → target/classes/static/

# 5. Package JAR
[INFO] Building jar: target/fleet-navigator-0.1.0-SNAPSHOT.jar
```

---

## ✅ Deployment-Strategien

### Lokaler Server

```bash
# Build
mvn clean package

# Start
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar

# Optional: Als Service
sudo systemctl enable fleet-navigator
sudo systemctl start fleet-navigator
```

### Docker (Optional)

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/fleet-navigator-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 2025
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t fleet-navigator .
docker run -p 2025:2025 fleet-navigator
```

### Cloud Deployment

**Das JAR läuft überall:**
- ✅ AWS EC2
- ✅ Google Cloud Run
- ✅ Azure App Service
- ✅ Heroku
- ✅ DigitalOcean Droplet

---

## 🛠️ Troubleshooting

### "npm: command not found"

**Kein Problem!** Maven installiert Node/npm automatisch in `target/`

### Frontend-Änderungen nicht sichtbar?

```bash
# Neu bauen
mvn clean package

# Oder nur Frontend neu bauen
cd frontend
npm run build
cd ..
mvn package
```

### JAR zu groß?

**Typische Größe:** ~70-80 MB
- Spring Boot: ~30 MB
- Dependencies: ~30 MB
- Vue.js Frontend: ~5 MB
- Node/npm (nur während Build): wird NICHT ins JAR gepackt

---

## 🎯 IntelliJ Integration

### Maven Goal für Production Build

1. IntelliJ rechts: Maven Tab
2. fleet-navigator → Lifecycle → **package** (Doppelklick)
3. Fertig! JAR ist in `target/`

### Run Configuration für Production JAR

1. Run → Edit Configurations
2. ➕ Add → JAR Application
3. Path to JAR: `target/fleet-navigator-0.1.0-SNAPSHOT.jar`
4. Name: "Fleet Navigator (Production)"
5. OK

**Jetzt kannst du mit einem Klick das Production-JAR starten!**

---

## 📊 Vergleich

| Aspekt | Development | Production |
|--------|-------------|------------|
| **Server** | 2 (Vite + Spring) | 1 (nur Spring) |
| **Ports** | 5173 + 2025 | nur 2025 |
| **Hot-Reload** | ✅ Ja | ❌ Nein |
| **Build-Zeit** | Schnell | 1-2 Min |
| **Deployment** | Kompliziert | Einfach |
| **CORS** | Braucht Config | Nicht nötig |
| **URL** | localhost:5173 | localhost:2025 |

---

## 🚢 Production Checklist

Vor Deployment prüfen:

- [ ] `mvn clean package` läuft ohne Fehler
- [ ] JAR startet: `java -jar target/*.jar`
- [ ] Frontend lädt: http://localhost:2025
- [ ] API funktioniert: http://localhost:2025/api/models
- [ ] H2 Database Pfad korrekt (nicht :mem:)
- [ ] Ollama erreichbar
- [ ] Logs prüfen

---

## 🎉 Zusammenfassung

**Vorher (Development):**
```bash
# Terminal 1
mvn spring-boot:run

# Terminal 2
cd frontend && npm run dev

# 2 Server, 2 Ports
```

**Jetzt (Production):**
```bash
mvn clean package
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar

# 1 Server, 1 Port, 1 JAR ✅
```

---

**Development:** Nutze weiter `npm run dev` für schnelles Entwickeln
**Production:** Build mit `mvn package` → Deploye JAR → Fertig! 🚀
