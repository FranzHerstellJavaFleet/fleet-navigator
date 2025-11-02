# Fleet Navigator 🚢

**Deine private AI - kostenlos, lokal und ohne Cloud**

Eine benutzerfreundliche Anwendung für Gespräche mit künstlicher Intelligenz - entwickelt von JavaFleet Systems Consulting.

---

## 🎯 Für wen ist Fleet Navigator?

Fleet Navigator ist für **jeden**, der eine private AI nutzen möchte:

- 📝 **Briefe schreiben**: Bewerbungen, Kündigungen, Geschäftsbriefe
- 💬 **Fragen stellen**: Zu jedem Thema - Geschichte, Wissenschaft, Alltag
- 🌐 **Übersetzen**: Texte in viele Sprachen
- 📚 **Lernen**: Komplexe Themen einfach erklärt
- 💻 **Programmieren**: Code schreiben und verstehen (für Entwickler)

**Deine Daten bleiben bei dir!** Im Gegensatz zu ChatGPT läuft alles auf deinem Computer.

---

## ✨ Was kann Fleet Navigator?

- ✅ **Privatsphäre**: Alle Daten bleiben auf deinem Computer
- ✅ **Kostenlos**: Keine monatlichen Gebühren
- ✅ **Offline**: Funktioniert ohne Internet (nach dem Setup)
- ✅ **Brief-Agent**: Generiert Briefe und öffnet sie automatisch in Word/LibreOffice
- ✅ **Brief-Vorlagen**: Fertige Vorlagen für häufige Schreibanlässe
- ✅ **Multi-Sprache**: Deutsch, Englisch und viele mehr
- ✅ **Dokumente**: PDFs und Bilder hochladen und analysieren
- ✅ **System-Check**: Prüft automatisch, ob alles richtig installiert ist

---

## 🚀 Installation (Schritt für Schritt)

### Windows

#### Schritt 1: Ollama installieren
1. Gehe zu: https://ollama.ai/download/windows
2. Lade `Ollama Setup.exe` herunter
3. Führe die Installation aus
4. Ollama startet automatisch im Hintergrund

#### Schritt 2: AI-Modell installieren
1. Öffne die Eingabeaufforderung (CMD) oder PowerShell
2. Gib ein: `ollama pull llama3.2`
3. Warte, bis der Download fertig ist (ca. 2 GB)

#### Schritt 3: Fleet Navigator herunterladen
1. Gehe zu: https://github.com/FranzHerstellJavaFleet/fleet-navigator
2. Klicke auf "Releases" (rechte Seite)
3. Lade `fleet-navigator-windows-amd64.zip` herunter
4. Entpacke die ZIP-Datei

#### Schritt 4: Starten
1. Doppelklick auf `fleet-navigator.exe`
2. Öffne im Browser: http://localhost:2025

### macOS

#### Schritt 1: Ollama installieren
```bash
# Terminal öffnen (Programme → Dienstprogramme → Terminal)
brew install ollama
```

Oder von https://ollama.ai/download/mac herunterladen

#### Schritt 2: Ollama starten
```bash
ollama serve
```

#### Schritt 3: AI-Modell installieren
```bash
# In neuem Terminal-Fenster:
ollama pull llama3.2
```

#### Schritt 4: Fleet Navigator herunterladen
1. Gehe zu: https://github.com/FranzHerstellJavaFleet/fleet-navigator
2. Klicke auf "Releases"
3. Lade `fleet-navigator-macos-amd64.tar.gz` herunter
4. Entpacke die Datei

#### Schritt 5: Starten
```bash
cd fleet-navigator
./fleet-navigator
```

Browser öffnet automatisch: http://localhost:2025

### Linux (Ubuntu/Debian)

#### Schritt 1: Ollama installieren
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

#### Schritt 2: Ollama starten
```bash
ollama serve
```

#### Schritt 3: AI-Modell installieren
```bash
# In neuem Terminal:
ollama pull llama3.2
```

#### Schritt 4: Fleet Navigator herunterladen
```bash
# Gehe zu Releases und lade herunter:
wget https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases/download/v1.0.0/fleet-navigator-linux-amd64.tar.gz

# Entpacken:
tar -xzf fleet-navigator-linux-amd64.tar.gz
cd fleet-navigator
```

#### Schritt 5: Starten
```bash
./fleet-navigator
```

Browser: http://localhost:2025

---

## 📖 Erste Schritte

### 1. System-Check

Beim ersten Start prüft Fleet Navigator automatisch:
- ✅ Ist Ollama installiert?
- ✅ Ist ein AI-Modell vorhanden?
- ✅ Ist genug Arbeitsspeicher verfügbar?

**Falls ein Fehler angezeigt wird**, folge den Anweisungen auf dem Bildschirm.

### 2. Brief schreiben

1. Klicke auf das **Dokument-Symbol** oben rechts
2. Wähle eine **Vorlage** aus (z.B. "Bewerbungsschreiben")
3. Ersetze die Platzhalter `[...]` mit deinen Informationen
4. Klicke auf **"Generieren"**
5. Die AI erstellt den Brief - du kannst ihn kopieren und anpassen

### 3. Fragen stellen

1. Tippe deine Frage unten in das Eingabefeld
2. Drücke Enter
3. Die AI antwortet in wenigen Sekunden

**Beispiel-Fragen:**
- "Erkläre mir, wie Photosynthese funktioniert"
- "Was ist der Unterschied zwischen Java und JavaScript?"
- "Schreibe ein Gedicht über den Herbst"
- "Wie kündige ich meinen Handyvertrag?"

### 4. Dokumente analysieren

1. Klicke auf das **Büroklammer-Symbol** 📎
2. Wähle ein PDF oder Bild aus
3. Stelle eine Frage zum Dokument
4. Die AI analysiert das Dokument und antwortet

---

## 💡 Tipps für gute Ergebnisse

### Klare Anweisungen geben
❌ Schlecht: "Schreib was über Bewerbung"
✅ Gut: "Schreibe ein Bewerbungsschreiben für eine Stelle als Softwareentwickler bei Microsoft. Betone meine 5 Jahre Erfahrung mit Java und Python."

### Kontext angeben
Je mehr Informationen du gibst, desto besser die Antwort:
- Wer ist der Empfänger?
- Was ist der Anlass?
- Welcher Ton ist passend? (formell/informell)

### Schritt für Schritt
Bei komplexen Aufgaben: Teile sie in mehrere Fragen auf.

---

## 🔧 Empfohlene AI-Modelle

| Modell | Größe | Am besten für | Installation |
|--------|-------|---------------|--------------|
| **llama3.2** | 2 GB | Allgemeine Fragen, Briefe | `ollama pull llama3.2` |
| **qwen2.5:7b** | 4.4 GB | Bessere Qualität, mehrsprachig | `ollama pull qwen2.5:7b` |
| **llama3.1:8b** | 4.7 GB | Lange Texte, Dokumente | `ollama pull llama3.1:8b` |
| **codellama:13b** | 7.4 GB | Programmierung | `ollama pull codellama:13b` |
| **llava:13b** | 7.7 GB | Bildanalyse | `ollama pull llava:13b` |

**Mehr Modelle:** https://ollama.ai/library

### Welches Modell für mich?

- **4-8 GB RAM**: `llama3.2` oder `qwen2.5:3b`
- **16 GB RAM**: `qwen2.5:7b` oder `llama3.1:8b`
- **32 GB+ RAM**: `qwen2.5:14b` oder `llama3.1:70b`

---

## 📝 Brief-Agent - Automatische Textverarbeitung

Der Brief-Agent ist ein **besonderes Feature** für Briefe und Dokumente:

### Wie funktioniert's?

1. **Klicke auf "Briefe"** in der Sidebar
2. **Wähle eine Vorlage** oder beschreibe deinen Brief
3. **AI generiert den Brief** mit deinen persönlichen Daten
4. **Brief öffnet sich automatisch** in deiner Textverarbeitung
5. **Rechtschreibprüfung**, Formatierung, als PDF speichern oder drucken

### Welche Textverarbeitung?

Der Brief-Agent unterstützt **mehrere Programme**:

| Betriebssystem | Programme |
|----------------|-----------|
| **Windows** | Microsoft Word, LibreOffice, OnlyOffice, Notepad |
| **macOS** | Microsoft Word, LibreOffice, OnlyOffice, TextEdit |
| **Linux** | LibreOffice, OnlyOffice, WPS Office, AbiWord, gedit |

**Empfehlung:**
- **Windows**: Microsoft Word (wenn vorhanden) oder LibreOffice (kostenlos)
- **macOS/Linux**: LibreOffice (kostenlos)

### Wo werden Briefe gespeichert?

Alle generierten Briefe werden in diesem Ordner abgelegt:

- **Windows**: `C:\Users\[Dein Name]\FleetNavigator\Documents\`
- **macOS**: `/Users/[Dein Name]/FleetNavigator/Documents/`
- **Linux**: `/home/[Dein Name]/FleetNavigator/Documents/`

### Persönliche Daten

Beim ersten Start erstellt Fleet Navigator **Platzhalter-Daten** (Max Mustermann, Musterweg 1, 12345 Musterstadt).

**Diese Daten MUSST du ersetzen!**

1. Klicke auf **"Einstellungen"** in der Sidebar
2. Gehe zu **"Persönliche Daten"**
3. Trage deine echten Daten ein
4. Speichern

Ab jetzt werden **alle Briefe automatisch** mit deinen Daten erstellt!

---

## ❓ Häufige Fragen

### Die Anwendung startet nicht
**Lösung:** Prüfe, ob Ollama läuft:
```bash
ollama list
```
Falls nicht: `ollama serve`

### "Keine Modelle gefunden"
**Lösung:** Installiere mindestens ein Modell:
```bash
ollama pull llama3.2
```

### Antworten sind sehr langsam
**Ursachen:**
- Zu wenig RAM → Nutze kleineres Modell
- CPU zu schwach → Nutze Modell mit weniger Parametern
- Andere Programme schließen

### Kann ich mehrere Modelle gleichzeitig nutzen?
Ja! Fleet Navigator wählt automatisch das beste Modell für deine Aufgabe.

### Kostet das etwas?
**Nein!** Fleet Navigator und Ollama sind komplett kostenlos und Open Source.

### Funktioniert es offline?
Ja, nach der Installation und dem Download der Modelle benötigst du kein Internet mehr.

---

## 🔐 Datenschutz & Sicherheit

✅ **Keine Cloud**: Alle Daten bleiben auf deinem Computer
✅ **Keine Tracking**: Wir sammeln keine Nutzungsdaten
✅ **Open Source**: Der gesamte Code ist einsehbar
✅ **Lokale Datenbank**: Chats werden nur lokal gespeichert

### Wo werden meine Chats gespeichert?

- **Windows**: `C:\Users\[Dein Name]\.fleet-navigator\`
- **macOS**: `/Users/[Dein Name]/.fleet-navigator/`
- **Linux**: `/home/[Dein Name]/.fleet-navigator/`

Du kannst diese Dateien jederzeit löschen oder sichern.

---

## 🛠️ Für Entwickler

### Aus Quellcode bauen

```bash
# Repository klonen
git clone https://github.com/FranzHerstellJavaFleet/fleet-navigator.git
cd fleet-navigator

# Mit Maven bauen
mvn clean package

# Starten
java -jar target/fleet-navigator-0.1.0-SNAPSHOT.jar
```

### Development Mode

```bash
# Backend starten (Port 2025)
mvn spring-boot:run

# Frontend starten (Port 5173)
cd frontend
npm install
npm run dev
```

### Technologie-Stack

- **Backend**: Spring Boot 3.2, Java 17
- **Frontend**: Vue.js 3, Vite, Tailwind CSS
- **Datenbank**: H2 (file-based)
- **AI**: Ollama REST API
- **Monitoring**: OSHI (Hardware-Überwachung)

### Native Image (GraalVM)

Für schnelleren Start und weniger RAM-Verbrauch:

```bash
mvn -Pnative clean package
```

Mehr Infos: [NATIVE-IMAGE.md](NATIVE-IMAGE.md)

---

## 📞 Unterstützung & Community

### Bei Problemen

1. **System-Check** im Browser aufrufen: http://localhost:2025
2. **Logs** prüfen (Terminal-Ausgabe)
3. **GitHub Issues**: https://github.com/FranzHerstellJavaFleet/fleet-navigator/issues

### Dokumentation

- [GraalVM Native Image Guide](NATIVE-IMAGE.md)
- [GitHub Actions Setup](GITHUB-ACTIONS-GUIDE.md)
- [Windows Build Anleitung](build-native-windows.md)

### Updates

Fleet Navigator prüft **nicht** automatisch auf Updates.
Neue Versionen findest du auf GitHub unter "Releases".

---

## 📜 Lizenz

Fleet Navigator ist Open Source Software unter der MIT-Lizenz.
Du darfst die Software frei nutzen, verändern und weitergeben.

---

## 🙏 Danksagungen

- **Ollama Team** - Für die großartige Local AI Platform
- **Spring Boot** & **Vue.js** Communities
- **Meta** - Für die Llama-Modelle
- **Alibaba Cloud** - Für die Qwen-Modelle

---

## 🚢 Entwickelt von

**JavaFleet Systems Consulting**
Port 2025 - Das Geburtsjahr von Fleet Navigator

---

## 📥 Download

Native Binaries (ohne JDK!) für Windows, macOS und Linux sind unter **Releases** verfügbar:
https://github.com/FranzHerstellJavaFleet/fleet-navigator/releases

---

**🌟 Gefällt dir Fleet Navigator? Gib uns einen Stern auf GitHub!**

https://github.com/FranzHerstellJavaFleet/fleet-navigator
