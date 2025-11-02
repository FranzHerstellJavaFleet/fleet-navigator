# Fleet Navigator Frontend 🚢

Vue.js 3 Frontend für Fleet Navigator - Eine moderne Benutzeroberfläche für Ollama LLM-Modelle.

## 🎨 Features

### Design
- ✅ **Orange Theme** - JavaFleet Markenfarbe (#FF9500)
- ✅ **Angelehnt an ChatGPT/Claude** - Moderne, vertraute UI
- ✅ **Responsive** - Funktioniert auf allen Bildschirmgrößen
- ✅ **Dark/Light** - Sidebar dunkel, Chat-Bereich hell

### Funktionen
- ✅ **Chat-Interface** - Nachrichten senden und empfangen
- ✅ **Streaming-Toggle** - Ein/Aus-Schalter für Streaming
- ✅ **Stop-Button** - Laufende Anfragen abbrechen
- ✅ **System-Monitoring** - CPU, RAM, Ollama-Status
- ✅ **Token-Counter** - Pro Chat und Global
- ✅ **Model-Switcher** - Modell-Auswahl im Dropdown
- ✅ **Chat-Historie** - Sidebar mit allen Chats
- ✅ **System-Prompt** - Anpassbare System-Prompts
- ✅ **Markdown-Support** - Code-Blöcke, Bold, Italic

## 🚀 Installation

### 1. Dependencies installieren

```bash
cd frontend
npm install
```

### 2. Entwicklungsserver starten

```bash
npm run dev
```

Öffnet automatisch `http://localhost:5173`

### 3. Build für Production

```bash
npm run build
```

Output: `dist/` Verzeichnis

## 📁 Projektstruktur

```
frontend/
├── public/              # Statische Assets
├── src/
│   ├── assets/         # CSS, Bilder
│   │   └── main.css    # Tailwind + Custom Styles
│   ├── components/     # Vue-Komponenten
│   │   ├── Sidebar.vue           # Chat-Historie
│   │   ├── TopBar.vue            # Model-Switcher, Settings
│   │   ├── ChatWindow.vue        # Chat-Bereich
│   │   ├── MessageBubble.vue     # Einzelne Nachricht
│   │   ├── MessageInput.vue      # Eingabefeld
│   │   └── SystemMonitor.vue     # System-Monitoring
│   ├── services/       # API-Integration
│   │   └── api.js      # Axios HTTP-Calls
│   ├── stores/         # Pinia State-Management
│   │   └── chatStore.js
│   ├── App.vue         # Hauptkomponente
│   └── main.js         # Entry-Point
├── index.html
├── package.json
├── vite.config.js
└── tailwind.config.js  # Orange Theme-Konfiguration
```

## 🎨 Design-System

### Farben (Tailwind Config)

```javascript
'fleet-orange': {
  50: '#FFF7ED',
  100: '#FFEDD5',
  200: '#FED7AA',
  300: '#FDBA74',
  400: '#FB923C',
  500: '#FF9500',  // Hauptfarbe
  600: '#EA580C',
  700: '#C2410C',
  800: '#9A3412',
  900: '#7C2D12',
}
```

Verwendung:
- `bg-fleet-orange-500` - Hauptfarbe
- `text-fleet-orange-500` - Text
- `border-fleet-orange-500` - Rahmen

### Komponenten-Design

**Sidebar (Links)**
- Dunkelgrau (`bg-gray-900`)
- Orange Akzente
- Chat-Liste mit Hover-Effekten
- Stats-Footer

**Chat-Bereich (Mitte)**
- Heller Hintergrund (`bg-gray-50`)
- Weiße Nachrichten-Bubbles
- User: Orange Bubble
- AI: Weiße Bubble mit Border

**System-Monitor (Rechts, optional)**
- Dunkelgrau
- Orange Progress-Bars
- Live-Updates alle 5 Sekunden

## 🔧 Konfiguration

### API-Proxy (Vite)

```javascript
// vite.config.js
proxy: {
  '/api': {
    target: 'http://localhost:8080',  // Spring Boot Backend
    changeOrigin: true
  }
}
```

### Backend-URL ändern

Für Production in `src/services/api.js` anpassen:

```javascript
const api = axios.create({
  baseURL: 'http://your-backend-url:8080/api'
})
```

## 📊 State-Management (Pinia)

### Chat Store

```javascript
// Verwendung in Komponenten
import { useChatStore } from '@/stores/chatStore'

const chatStore = useChatStore()

// Aktionen
await chatStore.sendMessage('Hello!')
await chatStore.loadChats()
await chatStore.loadModels()
await chatStore.loadSystemStatus()

// State
chatStore.messages
chatStore.currentChat
chatStore.globalStats
chatStore.systemStatus
```

## 🎯 Komponenten-API

### ChatWindow.vue

Props: keine
Events: keine
Features:
- Zeigt alle Nachrichten
- Auto-Scroll zu neuester Nachricht
- Welcome-Screen mit Vorschlägen
- Loading-Indicator

### MessageInput.vue

Props: keine
Events: `@send(message: string)`
Features:
- Textarea mit Auto-Height
- Shift+Enter für neue Zeile
- Enter zum Senden
- Stop-Button während Loading
- Streaming-Toggle
- Token-Counter

### Sidebar.vue

Props: keine
Events: keine
Features:
- Chat-Liste (sortiert nach Datum)
- "New Chat" Button
- Chat löschen
- Global Stats Footer

### SystemMonitor.vue

Props: keine
Events: `@close`
Features:
- Ollama Status (Online/Offline)
- Memory Usage (Progress Bar)
- CPU Load
- Global Stats
- Auto-Refresh (5s)

## 🚀 Deployment

### Option 1: Vite Build + Spring Boot Static

```bash
# 1. Build Frontend
cd frontend
npm run build

# 2. Kopiere dist/ nach Spring Boot
cp -r dist/* ../src/main/resources/static/

# 3. Spring Boot starten
cd ..
mvn spring-boot:run
```

Zugriff: `http://localhost:8080`

### Option 2: Separate Deployments

Frontend: Nginx, Netlify, Vercel
Backend: Spring Boot auf separatem Server

**CORS aktivieren** in Spring Boot `WebConfig.java`

## 🐛 Troubleshooting

### Backend nicht erreichbar

```bash
# Prüfe ob Spring Boot läuft
curl http://localhost:8080/api/models

# Prüfe Ollama
curl http://localhost:11434/api/tags
```

### Tailwind Styles werden nicht angezeigt

```bash
npm run dev
# Tailwind muss compilen
```

### CORS-Fehler

In `WebConfig.java` prüfen:
```java
.allowedOrigins("http://localhost:5173")
```

## 📝 TODOs (Phase 2)

- [ ] WebSocket-Streaming implementieren
- [ ] Stop-Button funktional machen
- [ ] GPU/VRAM-Monitoring hinzufügen
- [ ] Context-File-Upload
- [ ] Chat-Export/Import
- [ ] Dark-Mode-Toggle
- [ ] Multi-User-Support

## 🎉 Features

### Bereits implementiert ✅
- Chat-Interface mit User/AI-Bubbles
- Model-Auswahl Dropdown
- System-Prompt-Editor
- Token-Zähler (Chat + Global)
- System-Monitoring (CPU, RAM)
- Chat-Historie in Sidebar
- Streaming-Toggle (UI bereit)
- Stop-Button (UI bereit)
- Markdown-Rendering (Code-Blöcke)
- Responsive Design

### In Arbeit 🚧
- WebSocket-Streaming (Backend)
- Stop-Funktionalität (Backend)
- GPU-Monitoring

---

**Version:** 0.1.0-SNAPSHOT
**Framework:** Vue.js 3 + Vite + Tailwind CSS
**Entwickler:** JavaFleet Systems Consulting

🚢 Navigate your AI fleet!
