# Changelog

Alle wesentlichen Änderungen an Fleet Navigator werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/).

## [Unreleased]

### Hinzugefügt
- ✨ **Internationalisierung (i18n)**: Automatische Spracherkennung mit Browser-Locale
  - Unterstützung für 10+ Sprachen (Deutsch, Englisch, Französisch, Spanisch, etc.)
  - LocalStorage-Persistenz für Sprachpräferenzen
  - Composable `useLocale.js` für globales Sprach-Management

- 🎨 **Benutzerfreundlicher Willkommensbildschirm**:
  - 6 Kategorien als Einstiegspunkte (Brief, Fragen, Übersetzen, Lernen, Code, Kreativ)
  - Icons von @heroicons/vue für visuelle Orientierung
  - Suggestion Cards mit Hover-Effekten

- 📝 **Default-Daten beim ersten Start**:
  - Automatische Initialisierung von Brief-Vorlagen (Deutsch & Englisch)
  - Max Mustermann / John Doe Platzhalter für persönliche Daten
  - System-Locale-Erkennung für passende Sprache

- 💬 **Demo-Chats für neue Nutzer**:
  - Beispiel-Konversationen (Bewerbungsschreiben, Wissenschaftserklärungen)
  - Zeigt verschiedene Anwendungsfälle
  - Automatisch erstellt bei leerem System

- 📄 **Dokumentation**:
  - Komplett überarbeitetes README für alle Nutzergruppen
  - Brief-Agent Dokumentation mit Textverarbeitungs-Integration
  - Installationsanleitungen für Windows, macOS, Linux
  - Download-Sektion für Native Binaries

### Geändert
- 🔧 **Entity-Struktur korrigiert**:
  - `DefaultDataInitializer`: Verwendet korrekte Entity-Felder
  - `DemoChatsInitializer`: `Message.MessageRole` Enum statt String
  - `LetterTemplate`: `prompt` statt `content`
  - `Chat`: `model` statt `modelName`

### Behoben
- 🐛 **GraalVM Native Image Build**:
  - Fixed Apache Commons Logging Initialisierung zur Runtime
  - Korrigierte Maven Build-Befehle in GitHub Actions
  - Entity-Referenz-Fehler in Initializer-Klassen behoben

- 🎨 **Frontend**:
  - Welcome Screen zeigt nur bei fehlender Chat-Auswahl
  - Suggestions verwenden i18n-Keys statt hardcoded Text

### Entfernt
- ❌ SystemPrompts aus Initializer (nicht benötigt für MVP)

---

## Ältere Versionen

### [0.1.0] - Initial Release

#### Hinzugefügt
- 🚀 Erste Version von Fleet Navigator
- 💬 Chat-Interface mit Ollama Integration
- 📋 Brief-Agent mit Textverarbeitungs-Integration
- 🏥 System Health Checks beim Start
- 💾 H2 File-Based Database
- 🎨 Vue.js 3 Frontend mit Tailwind CSS
- 🔧 Spring Boot 3.2 Backend
- 🏗️ GraalVM Native Image Support
- 📦 Multi-Platform Builds (Windows, macOS, Linux)

---

## Legende

- ✨ Neue Features
- 🔧 Änderungen
- 🐛 Bugfixes
- 📝 Dokumentation
- 🎨 UI/UX Verbesserungen
- 🔥 Breaking Changes
- 🚀 Performance
- 📦 Build/Deploy
- ❌ Entfernt
