# Fleet Navigator Model Store - Anleitung

## ✨ Was wurde implementiert?

Die vollständige **Modellverwaltung mit Download-Funktionalität** für llama.cpp ist jetzt verfügbar!

### 🎯 Features

- ✅ **Model Store** mit 9 kuratierten deutschen GGUF-Modellen
- ✅ **Direkter Download** von HuggingFace
- ✅ **Echtzeit-Progress** mit Download-Geschwindigkeit
- ✅ **Modell-Filter** nach Kategorie, RAM und Sprache
- ✅ **Neue Verzeichnisstruktur** für bessere Organisation
- ✅ **Deutsche Modelle** optimiert für deutsche Texte

---

## 📁 Neue Verzeichnisstruktur

```
models/
├── library/          ← Heruntergeladene Modelle aus dem Store
│   └── (leer am Anfang)
├── custom/           ← Eigene hochgeladene Modelle
│   └── Llama-3.2-1B-Instruct-Q4_K_M.gguf (dein existierendes Modell)
```

**Wichtig:** Dein bestehendes Modell wurde automatisch nach `models/custom/` verschoben!

---

## 🏪 Verfügbare Modelle im Store

### ⭐ Empfohlen für deutsche Texte:

1. **Qwen 2.5 (3B) - Instruct** - 1.97 GB
   - Exzellentes mehrsprachiges Modell
   - Hervorragendes Deutsch
   - Perfekt für: Briefe, E-Mails, Chat
   - Min RAM: 4 GB

2. **Llama 3.2 (3B) - Instruct** - 2.02 GB
   - Schnelles Allzweck-Modell
   - Gutes Deutsch
   - Perfekt für: Chat, Q&A
   - Min RAM: 4 GB

3. **Qwen 2.5 (7B) - Instruct** - 4.73 GB
   - Premium-Modell
   - Exzellente Qualität
   - Perfekt für: Komplexe Texte, Analyse
   - Min RAM: 8 GB

### 💻 Für Code-Generierung:

4. **Qwen 2.5 Coder (3B)** - 1.97 GB
   - Spezialisiert auf Code
   - Versteht deutsche Anweisungen
   - Min RAM: 4 GB

5. **Qwen 2.5 Coder (7B)** - 4.73 GB
   - Premium Code-Modell
   - Höchste Qualität
   - Min RAM: 8 GB

### 📦 Kompakte Modelle (für schwache Hardware):

6. **Llama 3.2 (1B)** - 771 MB (bereits vorhanden)
7. **Qwen 2.5 (1.5B)** - 1.05 GB

---

## 🚀 Wie du Modelle herunterlädst

### Schritt 1: Fleet Navigator starten

```bash
cd /home/trainer/NetBeansProjects/ProjekteFMH/Fleet-Navigator
./START.sh
```

### Schritt 2: Model Store öffnen

1. Öffne die Anwendung: http://localhost:5173
2. Klicke auf **⚙️ Einstellungen** (unten links)
3. Wähle den Tab **🏪 Model Store**

### Schritt 3: Modell auswählen

- **Empfohlene Modelle** werden oben angezeigt
- Filtere nach:
  - **Kategorie**: Chat, Code, Kompakt
  - **RAM**: Max 4 GB, 8 GB, 16 GB
  - **Suche**: Nach Namen oder Sprache

### Schritt 4: Download starten

1. Klicke auf **⬇️ Herunterladen** bei deinem gewünschten Modell
2. Beobachte den **Echtzeit-Progress**:
   - Prozent-Anzeige
   - Download-Geschwindigkeit (MB/s)
   - Verbleibende Zeit

3. **Active Downloads Panel** unten rechts zeigt alle laufenden Downloads

### Schritt 5: Fertig!

- Nach dem Download ist das Modell sofort verfügbar
- Es erscheint als **✓ Installiert**
- Du kannst es direkt im Chat verwenden

---

## 🎯 Empfehlung für dich

### Für den Einstieg (Deutsch):

**Qwen 2.5 (3B) - Instruct** herunterladen:
- Nur **1.97 GB**
- Exzellentes Deutsch
- Schnell und effizient
- Perfekt für Briefe und E-Mails

### Falls du mehr Power willst:

**Qwen 2.5 (7B) - Instruct** herunterladen:
- **4.73 GB**
- Beste Qualität
- Für komplexe Aufgaben
- Benötigt min. 8 GB RAM

---

## 🔍 Technische Details

### Backend-Komponenten

Neu implementiert:
- `ModelRegistry.java` - 9 kuratierte Modelle
- `ModelRegistryEntry.java` - Modell-Metadata
- `ModelDownloadService.java` - HuggingFace Download mit Progress
- `ModelStoreController.java` - REST API für Model Store
- `LlamaCppProvider.java` - Erweitert um neue Verzeichnisstruktur

### Frontend-Komponenten

Neu implementiert:
- `ModelStore.vue` - Hauptkomponente mit Filter
- `ModelCard.vue` - Modell-Anzeige mit Download-Button
- Integration in `SettingsModal.vue` als neuer Tab

### API Endpoints

```
GET  /api/model-store/all              → Alle Modelle
GET  /api/model-store/featured         → Empfohlene Modelle
GET  /api/model-store/category/{cat}   → Nach Kategorie
GET  /api/model-store/download/{id}    → Download mit SSE Progress
POST /api/model-store/download/{id}/cancel → Download abbrechen
```

---

## 🧪 Testen

### Download-Test:

1. Starte Fleet Navigator
2. Öffne Model Store
3. Wähle ein kleines Modell (z.B. Qwen 2.5 1.5B - 1.05 GB)
4. Klicke Download
5. Beobachte den Progress

**Erwartet:**
- Download startet sofort
- Progress-Updates alle 500ms
- Geschwindigkeit in MB/s
- Nach Download: ✓ Installiert

### Verzeichnis-Check:

```bash
ls -lh models/library/
ls -lh models/custom/
```

**Erwartet:**
- `library/` enthält heruntergeladene Modelle
- `custom/` enthält dein existierendes Llama-3.2-1B Modell

---

## ❓ Probleme?

### Download startet nicht

1. Prüfe Internet-Verbindung
2. Prüfe Backend-Logs: `mvn spring-boot:run`
3. Prüfe Browser-Console (F12)

### Download zu langsam

- HuggingFace kann bei großen Modellen Zeit brauchen
- Geschwindigkeit hängt von deiner Internet-Verbindung ab
- Du kannst den Download abbrechen und später fortsetzen

### Modell erscheint nicht

1. Prüfe `models/library/` Verzeichnis
2. Klicke auf **🔄 Aktualisieren** im Model Store
3. Starte Fleet Navigator neu

---

## 🎉 Viel Erfolg!

Du kannst jetzt direkt aus Fleet Navigator heraus hochwertige deutsche GGUF-Modelle herunterladen!

**Empfehlung:** Start mit **Qwen 2.5 (3B)** - das ist das beste Preis-Leistungs-Verhältnis für deutsche Texte.

---

**Erstellt:** 2025-11-11
**Version:** 0.2.9
**Autor:** JavaFleet Systems Consulting
