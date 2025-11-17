# LibreOffice Writer Extension mit Python und WebSocket

**Projekt:** Fleet Writer Officer  
**Version:** 1.0  
**Datum:** 2025-11-09  
**Technologie:** Python + UNO + WebSocket

---

## 📋 Inhaltsverzeichnis

1. [Überblick](#überblick)
2. [Voraussetzungen](#voraussetzungen)
3. [Projekt-Struktur](#projekt-struktur)
4. [UNO Bridge Grundlagen](#uno-bridge-grundlagen)
5. [WebSocket Integration](#websocket-integration)
6. [Extension Entwicklung](#extension-entwicklung)
7. [Dialoge und UI](#dialoge-und-ui)
8. [Packaging und Installation](#packaging-und-installation)
9. [Testing und Debugging](#testing-und-debugging)
10. [Deployment](#deployment)

---

## 🎯 Überblick

Diese Anleitung erklärt, wie man eine voll funktionsfähige LibreOffice Writer Extension in Python erstellt, die:

- ✅ Sich über WebSocket mit Fleet Navigator verbindet
- ✅ AI-generierte Texte direkt in Writer einfügt
- ✅ Einen grafischen Dialog zur Benutzerinteraktion bietet
- ✅ Persönliche Daten vom Server lädt
- ✅ Real-time Updates über WebSocket empfängt

### Architektur

```
┌─────────────────────────────────────────────────────────────┐
│              LibreOffice Writer (Benutzer-UI)               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Fleet Writer Officer Extension (Python)               │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │ │
│  │  │   UNO API    │←→│  WebSocket   │←→│    Logic    │ │ │
│  │  │   Bridge     │  │    Client    │  │   Handler   │ │ │
│  │  └──────────────┘  └──────────────┘  └─────────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            ↕ WebSocket (ws://localhost:2025)
┌─────────────────────────────────────────────────────────────┐
│         Fleet Navigator Server + Ollama AI Backend         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Voraussetzungen

### Software

```bash
# LibreOffice (Version 7.0+)
sudo apt install libreoffice libreoffice-dev

# Python 3.8+ (meist mit LibreOffice installiert)
python3 --version

# Python Development Tools
sudo apt install python3-pip python3-venv

# UNO Bridge (kommt mit LibreOffice)
# Pfad: /usr/lib/libreoffice/program/python
```

### Python Pakete

```bash
# WebSocket Client
pip3 install websocket-client

# Für asynchrone WebSocket-Verbindungen
pip3 install websockets asyncio

# Für JSON-Handling (Standard-Bibliothek, aber zur Sicherheit)
pip3 install python-json
```

### Umgebungsvariablen

```bash
# In ~/.bashrc oder ~/.zshrc
export URE_BOOTSTRAP="file:///usr/lib/libreoffice/program/fundamentalrc"
export UNO_PATH="/usr/lib/libreoffice/program"
export PYTHONPATH="$PYTHONPATH:$UNO_PATH"
```

### 🔧 Python-Scripting in LibreOffice aktivieren

**Wichtig:** Python-Scripting muss in LibreOffice aktiviert sein, damit Extensions funktionieren.

#### Schritt 1: Prüfen ob Python verfügbar ist

```bash
# LibreOffice Python-Pfad finden
find /usr/lib/libreoffice -name "python*" -type d

# Sollte ausgeben:
# /usr/lib/libreoffice/program/python-core-3.8.10
# /usr/lib/libreoffice/program/python
```

#### Schritt 2: Python in LibreOffice aktivieren

**Linux (Ubuntu/Debian):**

```bash
# 1. Prüfe ob Python-Modul installiert ist
dpkg -l | grep libreoffice-script-provider-python

# Falls nicht installiert:
sudo apt install libreoffice-script-provider-python

# 2. LibreOffice neu starten
killall soffice.bin
soffice --writer
```

**macOS:**

```bash
# Python sollte mit LibreOffice vorinstalliert sein
# Falls nicht, LibreOffice neu installieren von:
# https://www.libreoffice.org/download/download/
```

**Windows:**

```powershell
# Python ist standardmäßig in LibreOffice enthalten
# Falls Probleme auftreten, LibreOffice neu installieren:
# https://www.libreoffice.org/download/download/
```

#### Schritt 3: Python-Scripting in LibreOffice-Einstellungen prüfen

1. **LibreOffice öffnen**
2. **Extras** → **Optionen**
3. **LibreOffice** → **Sicherheit**
4. **Makrosicherheit** → Button klicken
5. **Vertrauenswürdige Quellen** → Tab wählen
6. Stelle sicher dass **"Makros von vertrauenswürdigen Quellen"** aktiviert ist
7. Setze Sicherheitsstufe auf **"Mittel"** (für Entwicklung) oder **"Niedrig"** (nur für Tests!)

#### Schritt 4: Test ob Python funktioniert

**Python-Test-Makro erstellen:**

1. LibreOffice Writer öffnen
2. **Extras** → **Makros** → **Makros verwalten** → **Python**
3. Falls Dialog erscheint: Python ist verfügbar ✅
4. Falls Fehler: Python-Provider ist nicht installiert ❌

**Alternativer Test (Terminal):**

```bash
# LibreOffice mit Python-Shell starten
python3 << 'EOF'
import uno
from com.sun.star.beans import PropertyValue

# Versuche UNO zu laden
local_context = uno.getComponentContext()
if local_context:
    print("✅ Python-UNO Bridge funktioniert!")
else:
    print("❌ Python-UNO Bridge nicht verfügbar")
EOF
```

#### Schritt 5: Benötigte Python-Module installieren

**Für die Extension benötigt:**

```bash
# Installiere websocket-client
pip3 install websocket-client

# WICHTIG: Installiere in LibreOffice's Python!
# Linux:
/usr/lib/libreoffice/program/python -m pip install websocket-client

# macOS:
/Applications/LibreOffice.app/Contents/Resources/python -m pip install websocket-client

# Windows:
"C:\Program Files\LibreOffice\program\python.exe" -m pip install websocket-client
```

**Falls pip nicht verfügbar ist:**

```bash
# Linux: get-pip installieren
wget https://bootstrap.pypa.io/get-pip.py
/usr/lib/libreoffice/program/python get-pip.py

# Dann websocket-client installieren
/usr/lib/libreoffice/program/python -m pip install websocket-client
```

#### Troubleshooting

**Problem: "Python-UNO Bridge nicht gefunden"**

```bash
# Lösung: Setze PYTHONPATH explizit
export PYTHONPATH="/usr/lib/libreoffice/program:$PYTHONPATH"
export URE_BOOTSTRAP="vnd.sun.star.pathname:/usr/lib/libreoffice/program/fundamentalrc"

# Starte LibreOffice mit gesetzten Variablen
soffice --writer
```

**Problem: "websocket module not found in Extension"**

Die Extension kann externe Pakete nicht finden. **Lösung:**

1. Bundle `websocket-client` in die Extension:

```bash
# Erstelle lib-Ordner
mkdir -p FleetWriterOfficer/lib

# Installiere websocket-client in lib/
pip3 install websocket-client -t FleetWriterOfficer/lib/

# In fleet_writer.py, am Anfang:
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'lib'))

# Jetzt funktioniert:
import websocket
```

2. Update `build.py` um lib-Ordner zu inkludieren:

```python
# In build.py, füge hinzu:
files_to_include = [
    # ... existing files ...
    
    # Externe Pakete
    ('lib/', 'lib/'),  # Rekursiv alle Module in lib/
]

# Oder explizit:
import glob
for file in glob.glob('lib/**/*.py', recursive=True):
    zipf.write(file, file)
```

**Problem: "Extension lädt nicht"**

```bash
# Debug-Modus aktivieren
export DEBUG_EXTENSION=1

# LibreOffice mit Logging starten
soffice --writer --norestore --nologo --nodefault 2>&1 | tee libreoffice.log

# Log-Datei prüfen
tail -f ~/.libreoffice/4/user/Scripts/python/log.txt
```

#### Zusammenfassung: Checkliste

- [ ] `libreoffice-script-provider-python` installiert
- [ ] Python-UNO Bridge funktioniert (Test erfolgreich)
- [ ] `websocket-client` in LibreOffice's Python installiert ODER in Extension gebundled
- [ ] Makrosicherheit auf "Mittel" oder niedriger gesetzt
- [ ] PYTHONPATH und URE_BOOTSTRAP gesetzt (optional, aber empfohlen)
- [ ] LibreOffice neu gestartet nach Änderungen

---

## 📁 Projekt-Struktur

```
FleetWriterOfficer/
├── src/
│   ├── __init__.py
│   ├── fleet_writer.py          # Haupt-Extension-Klasse
│   ├── websocket_client.py      # WebSocket-Verbindung
│   ├── uno_helper.py             # UNO API Helper
│   ├── dialog_handler.py         # Dialog-Logik
│   └── config.py                 # Konfiguration
├── dialogs/
│   ├── BriefWizard.xdl          # Dialog-Definition (XML)
│   └── Preferences.xdl          # Einstellungen-Dialog
├── icons/
│   ├── fleet_icon_16.png
│   ├── fleet_icon_26.png
│   └── fleet_icon_32.png
├── META-INF/
│   └── manifest.xml             # Extension Manifest
├── description.xml              # Extension Beschreibung
├── Addons.xcu                   # Menü-Integration
├── ProtocolHandler.xcu          # Protokoll-Handler
├── build.py                     # Build-Script
├── requirements.txt             # Python Dependencies
└── README.md
```

---

## 🔌 UNO Bridge Grundlagen

### Was ist UNO?

**UNO** (Universal Network Objects) ist die Komponentenarchitektur von LibreOffice. Sie ermöglicht:

- Zugriff auf LibreOffice-Dokumente
- Manipulation von Text, Tabellen, Grafiken
- Erstellung von Dialogen
- Event-Handling

### Python-UNO Verbindung

```python
# src/uno_helper.py

import uno
import unohelper
from com.sun.star.task import XJobExecutor
from com.sun.star.beans import PropertyValue
from com.sun.star.text import XTextDocument

class UNOHelper:
    """
    Helper-Klasse für UNO-Bridge-Operationen
    """
    
    def __init__(self, context):
        """
        Initialisiert UNO Helper mit Component Context
        
        Args:
            context: com.sun.star.uno.XComponentContext
        """
        self.ctx = context
        self.smgr = context.ServiceManager
        
    def get_desktop(self):
        """
        Holt Desktop-Service (Zugriff auf alle Dokumente)
        
        Returns:
            com.sun.star.frame.XDesktop
        """
        return self.smgr.createInstanceWithContext(
            "com.sun.star.frame.Desktop", 
            self.ctx
        )
    
    def get_current_document(self):
        """
        Holt das aktuell geöffnete Dokument
        
        Returns:
            XTextDocument oder None
        """
        desktop = self.get_desktop()
        current_component = desktop.getCurrentComponent()
        
        # Prüfe ob es ein Text-Dokument ist
        if current_component and self.is_text_document(current_component):
            return current_component
        return None
    
    def is_text_document(self, component):
        """
        Prüft ob Component ein Text-Dokument ist
        
        Args:
            component: com.sun.star.lang.XComponent
            
        Returns:
            bool
        """
        return component.supportsService("com.sun.star.text.TextDocument")
    
    def get_text_cursor(self, doc):
        """
        Erstellt Text-Cursor für Dokument
        
        Args:
            doc: XTextDocument
            
        Returns:
            XTextCursor
        """
        text = doc.getText()
        return text.createTextCursor()
    
    def insert_text(self, doc, text, at_cursor=True):
        """
        Fügt Text in Dokument ein
        
        Args:
            doc: XTextDocument
            text: String zum Einfügen
            at_cursor: bool - an aktueller Cursor-Position oder am Ende
        """
        text_obj = doc.getText()
        
        if at_cursor:
            # An aktueller Cursor-Position
            cursor = doc.getCurrentController().getViewCursor()
        else:
            # Am Ende des Dokuments
            cursor = text_obj.createTextCursor()
            cursor.gotoEnd(False)
        
        text_obj.insertString(cursor, text, False)
    
    def create_paragraph_break(self, doc):
        """
        Fügt Absatz-Umbruch ein
        
        Args:
            doc: XTextDocument
        """
        text = doc.getText()
        cursor = text.createTextCursor()
        cursor.gotoEnd(False)
        text.insertControlCharacter(
            cursor, 
            uno.Enum("com.sun.star.text.ControlCharacter", "PARAGRAPH_BREAK"),
            False
        )
    
    def get_selected_text(self, doc):
        """
        Holt den aktuell markierten Text
        
        Args:
            doc: XTextDocument
            
        Returns:
            str oder None
        """
        controller = doc.getCurrentController()
        selection = controller.getSelection()
        
        if selection and selection.getCount() > 0:
            text_range = selection.getByIndex(0)
            return text_range.getString()
        return None
    
    def replace_selected_text(self, doc, new_text):
        """
        Ersetzt markierten Text mit neuem Text
        
        Args:
            doc: XTextDocument
            new_text: str
        """
        controller = doc.getCurrentController()
        selection = controller.getSelection()
        
        if selection and selection.getCount() > 0:
            text_range = selection.getByIndex(0)
            text_range.setString(new_text)
    
    def show_message_box(self, title, message, type_msg="infobox"):
        """
        Zeigt MessageBox an
        
        Args:
            title: str - Titel
            message: str - Nachricht
            type_msg: str - "infobox", "warningbox", "errorbox"
        """
        desktop = self.get_desktop()
        frame = desktop.getCurrentFrame()
        window = frame.getContainerWindow()
        
        toolkit = self.smgr.createInstanceWithContext(
            "com.sun.star.awt.Toolkit",
            self.ctx
        )
        
        msgbox = toolkit.createMessageBox(
            window,
            uno.Enum("com.sun.star.awt.MessageBoxType", type_msg.upper()),
            uno.getConstantByName("com.sun.star.awt.MessageBoxButtons.BUTTONS_OK"),
            title,
            message
        )
        
        return msgbox.execute()
```

---

## 🌐 WebSocket Integration

### WebSocket Client

```python
# src/websocket_client.py

import json
import threading
import time
from websocket import WebSocketApp
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class FleetWebSocketClient:
    """
    WebSocket Client für Fleet Navigator Server
    """
    
    def __init__(self, server_url="ws://localhost:2025/api/fleet-officer/ws"):
        """
        Initialisiert WebSocket Client
        
        Args:
            server_url: str - WebSocket Server URL
        """
        self.server_url = server_url
        self.ws = None
        self.connected = False
        self.callbacks = {}
        self.thread = None
        self.should_reconnect = True
        
    def connect(self, on_message_callback=None):
        """
        Stellt WebSocket-Verbindung her
        
        Args:
            on_message_callback: Callback-Funktion für eingehende Nachrichten
        """
        if on_message_callback:
            self.callbacks['message'] = on_message_callback
            
        self.ws = WebSocketApp(
            self.server_url,
            on_open=self._on_open,
            on_message=self._on_message,
            on_error=self._on_error,
            on_close=self._on_close
        )
        
        # Starte WebSocket in separatem Thread
        self.thread = threading.Thread(target=self._run_forever)
        self.thread.daemon = True
        self.thread.start()
        
        # Warte auf Verbindung (max 5 Sekunden)
        timeout = 5
        while not self.connected and timeout > 0:
            time.sleep(0.1)
            timeout -= 0.1
            
        if not self.connected:
            raise ConnectionError("WebSocket-Verbindung fehlgeschlagen")
            
        logger.info(f"✅ WebSocket verbunden: {self.server_url}")
    
    def _run_forever(self):
        """
        Läuft in separatem Thread und hält Verbindung aufrecht
        """
        while self.should_reconnect:
            try:
                self.ws.run_forever()
            except Exception as e:
                logger.error(f"WebSocket-Fehler: {e}")
                
            if self.should_reconnect:
                logger.info("🔄 Reconnect in 5 Sekunden...")
                time.sleep(5)
    
    def _on_open(self, ws):
        """
        Callback wenn Verbindung hergestellt wurde
        """
        self.connected = True
        logger.info("🔌 WebSocket geöffnet")
        
        # Sende Initial-Nachricht (z.B. Registrierung)
        self.send({
            "type": "register",
            "client_type": "libreoffice_extension",
            "version": "1.0.0"
        })
    
    def _on_message(self, ws, message):
        """
        Callback für eingehende Nachrichten
        
        Args:
            message: str - JSON-String
        """
        try:
            data = json.loads(message)
            logger.info(f"📨 Nachricht empfangen: {data.get('type', 'unknown')}")
            
            # Rufe registrierte Callbacks auf
            if 'message' in self.callbacks:
                self.callbacks['message'](data)
                
        except json.JSONDecodeError as e:
            logger.error(f"JSON-Fehler: {e}")
    
    def _on_error(self, ws, error):
        """
        Callback bei Fehlern
        """
        logger.error(f"❌ WebSocket-Fehler: {error}")
    
    def _on_close(self, ws, close_status_code, close_msg):
        """
        Callback wenn Verbindung geschlossen wurde
        """
        self.connected = False
        logger.info(f"🔌 WebSocket geschlossen: {close_status_code} - {close_msg}")
    
    def send(self, data):
        """
        Sendet Daten über WebSocket
        
        Args:
            data: dict - wird zu JSON konvertiert
        """
        if not self.connected:
            raise ConnectionError("WebSocket nicht verbunden")
            
        json_data = json.dumps(data)
        self.ws.send(json_data)
        logger.info(f"📤 Gesendet: {data.get('type', 'unknown')}")
    
    def request_letter_generation(self, letter_type, details):
        """
        Sendet Anfrage zur Brief-Generierung
        
        Args:
            letter_type: str - Typ des Briefs
            details: dict - Zusätzliche Informationen
            
        Returns:
            str - Request-ID für Tracking
        """
        request_id = f"req_{int(time.time() * 1000)}"
        
        self.send({
            "type": "generate_letter",
            "request_id": request_id,
            "letter_type": letter_type,
            "details": details
        })
        
        return request_id
    
    def close(self):
        """
        Schließt WebSocket-Verbindung
        """
        self.should_reconnect = False
        if self.ws:
            self.ws.close()
        if self.thread:
            self.thread.join(timeout=2)
        logger.info("👋 WebSocket-Client geschlossen")
```

---

## 🚀 Extension Entwicklung

### Haupt-Extension-Klasse

```python
# src/fleet_writer.py

import uno
import unohelper
from com.sun.star.task import XJobExecutor
from .uno_helper import UNOHelper
from .websocket_client import FleetWebSocketClient
from .dialog_handler import DialogHandler
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class FleetWriterOfficer(unohelper.Base, XJobExecutor):
    """
    Haupt-Klasse der Fleet Writer Officer Extension
    
    Diese Klasse wird von LibreOffice instanziiert wenn der Benutzer
    die Extension über das Menü aktiviert.
    """
    
    def __init__(self, ctx):
        """
        Initialisierung
        
        Args:
            ctx: com.sun.star.uno.XComponentContext
        """
        logger.info("🚢 Fleet Writer Officer wird initialisiert...")
        
        self.ctx = ctx
        self.uno_helper = UNOHelper(ctx)
        self.ws_client = None
        self.dialog_handler = DialogHandler(ctx, self)
        self.pending_requests = {}
        
        logger.info("✅ Fleet Writer Officer bereit")
    
    def trigger(self, args):
        """
        Haupteinstiegspunkt - wird aufgerufen wenn Extension gestartet wird
        
        Args:
            args: Tuple mit Argumenten (meist leer)
        """
        logger.info("🎯 Fleet Writer Officer gestartet")
        
        try:
            # Prüfe ob Writer-Dokument geöffnet ist
            doc = self.uno_helper.get_current_document()
            if not doc:
                self.uno_helper.show_message_box(
                    "Fleet Writer Officer",
                    "Bitte öffnen Sie zuerst ein Writer-Dokument.",
                    "warningbox"
                )
                return
            
            # Verbinde mit Fleet Navigator (falls nicht verbunden)
            if not self.ws_client or not self.ws_client.connected:
                self.connect_to_server()
            
            # Zeige Brief-Wizard-Dialog
            self.show_brief_wizard()
            
        except Exception as e:
            logger.error(f"❌ Fehler: {e}", exc_info=True)
            self.uno_helper.show_message_box(
                "Fehler",
                f"Ein Fehler ist aufgetreten:\n{str(e)}",
                "errorbox"
            )
    
    def connect_to_server(self):
        """
        Stellt Verbindung zu Fleet Navigator Server her
        """
        logger.info("🔌 Verbinde zu Fleet Navigator...")
        
        try:
            self.ws_client = FleetWebSocketClient()
            self.ws_client.connect(on_message_callback=self.on_websocket_message)
            
            self.uno_helper.show_message_box(
                "Verbunden",
                "Erfolgreich mit Fleet Navigator verbunden!",
                "infobox"
            )
            
        except Exception as e:
            logger.error(f"Verbindungsfehler: {e}")
            self.uno_helper.show_message_box(
                "Verbindungsfehler",
                f"Konnte nicht mit Fleet Navigator verbinden:\n{str(e)}\n\n"
                "Stellen Sie sicher, dass der Server läuft (Port 2025).",
                "errorbox"
            )
            raise
    
    def on_websocket_message(self, data):
        """
        Callback für eingehende WebSocket-Nachrichten
        
        Args:
            data: dict - Empfangene Daten
        """
        msg_type = data.get("type")
        logger.info(f"📨 Nachricht vom Typ: {msg_type}")
        
        if msg_type == "letter_generated":
            self.handle_letter_generated(data)
            
        elif msg_type == "error":
            self.handle_error(data)
            
        elif msg_type == "progress":
            self.handle_progress(data)
    
    def handle_letter_generated(self, data):
        """
        Verarbeitet generierte Brief-Daten
        
        Args:
            data: dict mit 'request_id', 'content', 'metadata'
        """
        request_id = data.get("request_id")
        content = data.get("content", "")
        
        logger.info(f"📝 Brief generiert (Request: {request_id})")
        
        # Füge in Dokument ein
        doc = self.uno_helper.get_current_document()
        if doc:
            self.uno_helper.insert_text(doc, content, at_cursor=False)
            
            self.uno_helper.show_message_box(
                "Erfolg",
                "Brief wurde erfolgreich eingefügt!",
                "infobox"
            )
        
        # Entferne aus Pending
        if request_id in self.pending_requests:
            del self.pending_requests[request_id]
    
    def handle_error(self, data):
        """
        Verarbeitet Fehler-Nachrichten
        """
        error_msg = data.get("message", "Unbekannter Fehler")
        logger.error(f"Server-Fehler: {error_msg}")
        
        self.uno_helper.show_message_box(
            "Server-Fehler",
            error_msg,
            "errorbox"
        )
    
    def handle_progress(self, data):
        """
        Verarbeitet Progress-Updates
        """
        progress = data.get("progress", 0)
        message = data.get("message", "")
        logger.info(f"⏳ Progress: {progress}% - {message}")
        
        # TODO: Update Progress-Dialog falls vorhanden
    
    def show_brief_wizard(self):
        """
        Zeigt Brief-Wizard-Dialog an
        """
        logger.info("🧙 Öffne Brief-Wizard...")
        self.dialog_handler.show_wizard()
    
    def generate_letter(self, letter_type, details):
        """
        Startet Brief-Generierung
        
        Args:
            letter_type: str - Typ des Briefs
            details: dict - Details vom Benutzer
        """
        logger.info(f"🎯 Generiere Brief: {letter_type}")
        
        if not self.ws_client or not self.ws_client.connected:
            raise ConnectionError("Keine Verbindung zum Server")
        
        # Sende Anfrage
        request_id = self.ws_client.request_letter_generation(
            letter_type,
            details
        )
        
        # Merke Request
        self.pending_requests[request_id] = {
            "type": letter_type,
            "timestamp": time.time()
        }
        
        logger.info(f"📤 Request gesendet: {request_id}")
        
        return request_id
    
    def cleanup(self):
        """
        Aufräumen beim Beenden
        """
        logger.info("🧹 Cleanup...")
        
        if self.ws_client:
            self.ws_client.close()


# Factory-Funktion für LibreOffice
def createInstance(ctx):
    """
    Factory-Funktion die von LibreOffice aufgerufen wird
    
    Args:
        ctx: com.sun.star.uno.XComponentContext
        
    Returns:
        FleetWriterOfficer Instanz
    """
    return FleetWriterOfficer(ctx)
```

---

## 🎨 Dialoge und UI

### Dialog-Definition (XML)

```xml
<!-- dialogs/BriefWizard.xdl -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE dlg:window PUBLIC "-//OpenOffice.org//DTD OfficeDocument 1.0//EN" "dialog.dtd">
<dlg:window xmlns:dlg="http://openoffice.org/2000/dialog" 
            xmlns:script="http://openoffice.org/2000/script" 
            dlg:id="BriefWizardDialog" 
            dlg:left="100" 
            dlg:top="100" 
            dlg:width="300" 
            dlg:height="200" 
            dlg:closeable="true" 
            dlg:moveable="true" 
            dlg:title="🚢 Fleet Writer Officer - Brief-Assistent">
    
    <!-- Überschrift -->
    <dlg:text dlg:id="lblTitle" 
              dlg:tab-index="0" 
              dlg:left="10" 
              dlg:top="10" 
              dlg:width="280" 
              dlg:height="15" 
              dlg:value="Welchen Brief möchten Sie erstellen?"
              dlg:align="center"/>
    
    <!-- Brief-Typ Dropdown -->
    <dlg:text dlg:id="lblBriefTyp" 
              dlg:tab-index="1" 
              dlg:left="10" 
              dlg:top="35" 
              dlg:width="80" 
              dlg:height="15" 
              dlg:value="Brief-Typ:"/>
    
    <dlg:menulist dlg:id="cmbBriefTyp" 
                  dlg:tab-index="2" 
                  dlg:left="95" 
                  dlg:top="33" 
                  dlg:width="195" 
                  dlg:height="15" 
                  dlg:spin="true">
        <dlg:menupopup>
            <dlg:menuitem dlg:value="Bewerbung"/>
            <dlg:menuitem dlg:value="Kündigung"/>
            <dlg:menuitem dlg:value="Beschwerde"/>
            <dlg:menuitem dlg:value="Geschäftsbrief"/>
            <dlg:menuitem dlg:value="Behördenbrief"/>
        </dlg:menupopup>
    </dlg:menulist>
    
    <!-- Empfänger -->
    <dlg:text dlg:id="lblEmpfaenger" 
              dlg:tab-index="3" 
              dlg:left="10" 
              dlg:top="60" 
              dlg:width="80" 
              dlg:height="15" 
              dlg:value="Empfänger:"/>
    
    <dlg:textfield dlg:id="txtEmpfaenger" 
                   dlg:tab-index="4" 
                   dlg:left="95" 
                   dlg:top="58" 
                   dlg:width="195" 
                   dlg:height="15"/>
    
    <!-- Details -->
    <dlg:text dlg:id="lblDetails" 
              dlg:tab-index="5" 
              dlg:left="10" 
              dlg:top="85" 
              dlg:width="80" 
              dlg:height="15" 
              dlg:value="Details:"/>
    
    <dlg:textfield dlg:id="txtDetails" 
                   dlg:tab-index="6" 
                   dlg:left="10" 
                   dlg:top="105" 
                   dlg:width="280" 
                   dlg:height="40" 
                   dlg:multiline="true" 
                   dlg:vscroll="true"/>
    
    <!-- Buttons -->
    <dlg:button dlg:id="btnGenerieren" 
                dlg:tab-index="7" 
                dlg:left="115" 
                dlg:top="160" 
                dlg:width="80" 
                dlg:height="25" 
                dlg:value="Generieren" 
                dlg:button-type="ok" 
                dlg:default="true"/>
    
    <dlg:button dlg:id="btnAbbrechen" 
                dlg:tab-index="8" 
                dlg:left="205" 
                dlg:top="160" 
                dlg:width="80" 
                dlg:height="25" 
                dlg:value="Abbrechen" 
                dlg:button-type="cancel"/>
</dlg:window>
```

### Dialog-Handler (Python)

```python
# src/dialog_handler.py

import uno
import unohelper
from com.sun.star.awt import XActionListener
import logging

logger = logging.getLogger(__name__)

class DialogHandler:
    """
    Verwaltet Dialoge der Extension
    """
    
    def __init__(self, ctx, extension):
        """
        Args:
            ctx: ComponentContext
            extension: FleetWriterOfficer Instanz
        """
        self.ctx = ctx
        self.extension = extension
        self.dialog = None
    
    def show_wizard(self):
        """
        Zeigt Brief-Wizard-Dialog an
        """
        try:
            # Dialog-Provider erstellen
            dialog_provider = self.ctx.ServiceManager.createInstanceWithContext(
                "com.sun.star.awt.DialogProvider",
                self.ctx
            )
            
            # Dialog aus XDL-Datei laden
            dialog_url = self._get_dialog_url("BriefWizard.xdl")
            self.dialog = dialog_provider.createDialog(dialog_url)
            
            # Button-Listener registrieren
            btn_generieren = self.dialog.getControl("btnGenerieren")
            btn_generieren.addActionListener(GenerierenListener(self))
            
            # Dialog anzeigen (modal)
            result = self.dialog.execute()
            
            # Aufräumen
            self.dialog.dispose()
            
        except Exception as e:
            logger.error(f"Dialog-Fehler: {e}", exc_info=True)
            raise
    
    def _get_dialog_url(self, dialog_name):
        """
        Erstellt URL für Dialog-Datei
        
        Args:
            dialog_name: str - Name der XDL-Datei
            
        Returns:
            str - vnd.sun.star.script URL
        """
        # URL für Extension-Package
        return (
            f"vnd.sun.star.script:FleetWriterOfficer.dialogs."
            f"{dialog_name}?location=application"
        )
    
    def get_form_data(self):
        """
        Extrahiert Daten aus Dialog-Formular
        
        Returns:
            dict mit Formular-Daten
        """
        if not self.dialog:
            return {}
        
        # Controls holen
        cmb_brief_typ = self.dialog.getControl("cmbBriefTyp")
        txt_empfaenger = self.dialog.getControl("txtEmpfaenger")
        txt_details = self.dialog.getControl("txtDetails")
        
        # Werte extrahieren
        brief_typ = cmb_brief_typ.getModel().Text
        empfaenger = txt_empfaenger.getModel().Text
        details = txt_details.getModel().Text
        
        return {
            "brief_typ": brief_typ,
            "empfaenger": empfaenger,
            "details": details
        }


class GenerierenListener(unohelper.Base, XActionListener):
    """
    Action-Listener für Generieren-Button
    """
    
    def __init__(self, dialog_handler):
        """
        Args:
            dialog_handler: DialogHandler Instanz
        """
        self.dialog_handler = dialog_handler
    
    def actionPerformed(self, event):
        """
        Wird aufgerufen wenn Button geklickt wird
        
        Args:
            event: ActionEvent
        """
        logger.info("🎯 Generieren-Button geklickt")
        
        try:
            # Hole Formular-Daten
            form_data = self.dialog_handler.get_form_data()
            
            # Validierung
            if not form_data["brief_typ"]:
                raise ValueError("Bitte wählen Sie einen Brief-Typ")
            
            # Starte Brief-Generierung
            self.dialog_handler.extension.generate_letter(
                form_data["brief_typ"],
                {
                    "empfaenger": form_data["empfaenger"],
                    "details": form_data["details"]
                }
            )
            
            # Dialog schließen
            self.dialog_handler.dialog.endExecute()
            
        except Exception as e:
            logger.error(f"Generierung fehlgeschlagen: {e}")
            # Zeige Fehler (Dialog bleibt offen)
            # TODO: Fehler-Label im Dialog aktualisieren
    
    def disposing(self, event):
        """Cleanup"""
        pass
```

---

## 📦 Packaging und Installation

### Konfigurationsdateien

#### description.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<description xmlns="http://openoffice.org/extensions/description/2006"
             xmlns:d="http://openoffice.org/extensions/description/2006"
             xmlns:xlink="http://www.w3.org/1999/xlink">
    
    <identifier value="io.javafleet.FleetWriterOfficer"/>
    
    <version value="1.0.0"/>
    
    <display-name>
        <name lang="en">Fleet Writer Officer</name>
        <name lang="de">Fleet Writer Officer</name>
    </display-name>
    
    <publisher>
        <name xlink:href="https://javafleet.io" lang="en">JavaFleet Systems</name>
    </publisher>
    
    <extension-description>
        <src xlink:href="description-en.txt" lang="en"/>
        <src xlink:href="description-de.txt" lang="de"/>
    </extension-description>
    
    <icon>
        <default xlink:href="icons/fleet_icon_32.png"/>
        <high-contrast xlink:href="icons/fleet_icon_32.png"/>
    </icon>
    
    <dependencies>
        <OpenOffice.org-minimal-version value="7.0" d:name="OpenOffice.org 7.0"/>
    </dependencies>
</description>
```

#### META-INF/manifest.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE manifest:manifest PUBLIC "-//OpenOffice.org//DTD Manifest 1.0//EN" "Manifest.dtd">
<manifest:manifest xmlns:manifest="http://openoffice.org/2001/manifest">
    
    <!-- Python-Komponente -->
    <manifest:file-entry 
        manifest:media-type="application/vnd.sun.star.uno-component;type=Python" 
        manifest:full-path="src/fleet_writer.py"/>
    
    <!-- Dialoge -->
    <manifest:file-entry 
        manifest:media-type="application/vnd.sun.star.dialog-library" 
        manifest:full-path="dialogs/"/>
    
    <!-- Konfiguration -->
    <manifest:file-entry 
        manifest:media-type="application/vnd.sun.star.configuration-data" 
        manifest:full-path="Addons.xcu"/>
    
    <manifest:file-entry 
        manifest:media-type="application/vnd.sun.star.configuration-data" 
        manifest:full-path="ProtocolHandler.xcu"/>
    
</manifest:manifest>
```

#### Addons.xcu (Menü-Integration)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<oor:component-data 
    xmlns:oor="http://openoffice.org/2001/registry" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    oor:name="Addons" 
    oor:package="org.openoffice.Office">
    
    <node oor:name="AddonUI">
        <node oor:name="OfficeMenuBar">
            <node oor:name="io.javafleet.FleetWriterOfficer" oor:op="replace">
                <prop oor:name="Title" oor:type="xs:string">
                    <value>Fleet Writer Officer</value>
                </prop>
                <prop oor:name="Target" oor:type="xs:string">
                    <value>_self</value>
                </prop>
                <prop oor:name="URL" oor:type="xs:string">
                    <value>vnd.sun.star.script:FleetWriterOfficer.src.fleet_writer$trigger?language=Python&amp;location=user</value>
                </prop>
                <prop oor:name="ImageIdentifier" oor:type="xs:string">
                    <value>%origin%/icons/fleet_icon_16.png</value>
                </prop>
            </node>
        </node>
        
        <node oor:name="OfficeToolBar">
            <node oor:name="io.javafleet.FleetWriterOfficer.Toolbar" oor:op="replace">
                <node oor:name="m1" oor:op="replace">
                    <prop oor:name="URL" oor:type="xs:string">
                        <value>vnd.sun.star.script:FleetWriterOfficer.src.fleet_writer$trigger?language=Python&amp;location=user</value>
                    </prop>
                    <prop oor:name="Title" oor:type="xs:string">
                        <value>Fleet Writer</value>
                    </prop>
                    <prop oor:name="Target" oor:type="xs:string">
                        <value>_self</value>
                    </prop>
                    <prop oor:name="Context" oor:type="xs:string">
                        <value>com.sun.star.text.TextDocument</value>
                    </prop>
                </node>
            </node>
        </node>
    </node>
</oor:component-data>
```

### Build-Script

```python
# build.py

import os
import zipfile
import shutil
from pathlib import Path

def build_extension():
    """
    Erstellt .oxt Package
    """
    print("🔨 Building Fleet Writer Officer Extension...")
    
    # Ausgabe-Datei
    output_file = "FleetWriterOfficer.oxt"
    
    # Lösche alte Version
    if os.path.exists(output_file):
        os.remove(output_file)
        print(f"🗑️  Alte Version gelöscht: {output_file}")
    
    # Erstelle ZIP (=OXT)
    with zipfile.ZipFile(output_file, 'w', zipfile.ZIP_DEFLATED) as zipf:
        
        # Füge alle Dateien hinzu
        files_to_include = [
            # Python-Code
            ('src/fleet_writer.py', 'src/fleet_writer.py'),
            ('src/uno_helper.py', 'src/uno_helper.py'),
            ('src/websocket_client.py', 'src/websocket_client.py'),
            ('src/dialog_handler.py', 'src/dialog_handler.py'),
            ('src/config.py', 'src/config.py'),
            ('src/__init__.py', 'src/__init__.py'),
            
            # Dialoge
            ('dialogs/BriefWizard.xdl', 'dialogs/BriefWizard.xdl'),
            
            # Icons
            ('icons/fleet_icon_16.png', 'icons/fleet_icon_16.png'),
            ('icons/fleet_icon_26.png', 'icons/fleet_icon_26.png'),
            ('icons/fleet_icon_32.png', 'icons/fleet_icon_32.png'),
            
            # Konfiguration
            ('META-INF/manifest.xml', 'META-INF/manifest.xml'),
            ('description.xml', 'description.xml'),
            ('Addons.xcu', 'Addons.xcu'),
            ('ProtocolHandler.xcu', 'ProtocolHandler.xcu'),
            
            # Dokumentation
            ('description-en.txt', 'description-en.txt'),
            ('description-de.txt', 'description-de.txt'),
        ]
        
        for source, target in files_to_include:
            if os.path.exists(source):
                zipf.write(source, target)
                print(f"✅ Added: {target}")
            else:
                print(f"⚠️  Missing: {source}")
        
        # Externe Pakete (websocket-client)
        # TODO: Bundle dependencies
        
    print(f"\n✅ Extension erstellt: {output_file}")
    print(f"📦 Größe: {os.path.getsize(output_file) / 1024:.1f} KB")
    print(f"\n📝 Installation:")
    print(f"   unopkg add {output_file}")

if __name__ == "__main__":
    build_extension()
```

### Installation

```bash
# Extension installieren
unopkg add FleetWriterOfficer.oxt

# Extension für alle Benutzer installieren (benötigt Root)
sudo unopkg add --shared FleetWriterOfficer.oxt

# Installierte Extensions auflisten
unopkg list

# Extension deinstallieren
unopkg remove io.javafleet.FleetWriterOfficer
```

---

## 🐛 Testing und Debugging

### Python-Logging aktivieren

```python
# src/config.py

import logging
import os

# Log-Datei in Home-Verzeichnis
LOG_FILE = os.path.expanduser("~/fleet_writer_officer.log")

# Konfiguriere Logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(LOG_FILE),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)
logger.info("🚀 Fleet Writer Officer Logger gestartet")
```

### Test-Runner

```python
# test_extension.py

"""
Testet Extension außerhalb von LibreOffice
"""

import sys
sys.path.append("/usr/lib/libreoffice/program")

import uno
from src.websocket_client import FleetWebSocketClient
import time

def test_websocket():
    """Test WebSocket-Verbindung"""
    print("🧪 Test WebSocket...")
    
    def on_message(data):
        print(f"📨 Nachricht: {data}")
    
    client = FleetWebSocketClient()
    client.connect(on_message_callback=on_message)
    
    # Sende Test-Nachricht
    client.send({"type": "ping"})
    
    # Warte
    time.sleep(2)
    
    client.close()
    print("✅ WebSocket-Test abgeschlossen")

def test_uno_helper():
    """Test UNO Helper (benötigt laufendes LibreOffice)"""
    print("🧪 Test UNO Helper...")
    
    # Verbinde zu laufendem LibreOffice
    local_context = uno.getComponentContext()
    resolver = local_context.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver",
        local_context
    )
    
    ctx = resolver.resolve(
        "uno:socket,host=localhost,port=2002;"
        "urp;StarOffice.ComponentContext"
    )
    
    from src.uno_helper import UNOHelper
    helper = UNOHelper(ctx)
    
    doc = helper.get_current_document()
    print(f"📄 Aktuelles Dokument: {doc}")
    
    print("✅ UNO-Test abgeschlossen")

if __name__ == "__main__":
    test_websocket()
    # test_uno_helper()  # Benötigt laufendes LibreOffice
```

### LibreOffice mit Socket starten (für Tests)

```bash
# LibreOffice mit UNO-Socket starten
soffice --accept="socket,host=localhost,port=2002;urp;" --norestore --nologo
```

### Log-Datei beobachten

```bash
# Echtzeit-Logs verfolgen
tail -f ~/fleet_writer_officer.log
```

---

## 🚀 Deployment

### Automatisiertes Build & Deploy

```bash
#!/bin/bash
# deploy.sh

set -e

echo "🚀 Fleet Writer Officer - Deployment"
echo "===================================="

# 1. Build Extension
echo "📦 Building Extension..."
python3 build.py

# 2. Deinstalliere alte Version
echo "🗑️  Removing old version..."
unopkg remove io.javafleet.FleetWriterOfficer 2>/dev/null || true

# 3. Installiere neue Version
echo "📥 Installing new version..."
unopkg add FleetWriterOfficer.oxt

# 4. LibreOffice neu starten
echo "🔄 Restarting LibreOffice..."
killall soffice.bin 2>/dev/null || true
sleep 2

# 5. LibreOffice starten
echo "✅ Starting LibreOffice Writer..."
soffice --writer &

echo ""
echo "✅ Deployment abgeschlossen!"
echo "📝 Extension ist jetzt verfügbar im Menü 'Tools' oder 'Extras'"
```

### Distribution

```bash
# Erstelle Release-Paket
mkdir -p release/
cp FleetWriterOfficer.oxt release/
cp README.md release/
cp LICENSE release/

# Erstelle ZIP für Distribution
cd release/
zip -r ../FleetWriterOfficer-v1.0.0.zip *
cd ..

echo "✅ Release-Paket erstellt: FleetWriterOfficer-v1.0.0.zip"
```

---

## 📚 Weiterführende Ressourcen

### Dokumentation

- **LibreOffice API:** https://api.libreoffice.org/
- **Python-UNO Bridge:** https://wiki.openoffice.org/wiki/Python
- **Extension Development:** https://wiki.documentfoundation.org/Documentation/DevGuide

### Beispiel-Projekte

```bash
# Alternative Python Extensions zum Studieren
git clone https://github.com/hanya/pyuno-examples
```

### Community

- **LibreOffice Forum:** https://ask.libreoffice.org/
- **Mailing List:** libreoffice@lists.freedesktop.org

---

## ✅ Checkliste

### Vor dem Start
- [ ] LibreOffice 7.0+ installiert
- [ ] Python 3.8+ verfügbar
- [ ] `websocket-client` installiert
- [ ] Fleet Navigator Server läuft auf Port 2025

### Entwicklung
- [ ] Projekt-Struktur erstellt
- [ ] `uno_helper.py` implementiert
- [ ] `websocket_client.py` implementiert
- [ ] `fleet_writer.py` implementiert
- [ ] `dialog_handler.py` implementiert
- [ ] Dialog XDL erstellt
- [ ] Konfigurationsdateien erstellt

### Testing
- [ ] WebSocket-Verbindung getestet
- [ ] UNO-Bridge-Funktionen getestet
- [ ] Dialog wird korrekt angezeigt
- [ ] Text-Einfügung funktioniert
- [ ] Brief-Generierung funktioniert

### Deployment
- [ ] Build-Script funktioniert
- [ ] OXT-Package erstellt
- [ ] Extension installiert
- [ ] Menü-Eintrag sichtbar
- [ ] Extension funktioniert in LibreOffice

---

## 🎓 Zusammenfassung

Dieses Dokument beschreibt die komplette Entwicklung einer LibreOffice Writer Extension in Python mit WebSocket-Anbindung. Die wichtigsten Komponenten sind:

1. **UNO Bridge** - Zugriff auf LibreOffice-API
2. **WebSocket Client** - Kommunikation mit Fleet Navigator
3. **Dialog-System** - Benutzer-Interaktion
4. **Extension Packaging** - OXT-Format für Distribution

Die Extension ermöglicht es, AI-generierte Briefe direkt in LibreOffice Writer zu erstellen und bietet eine nahtlose Integration mit dem Fleet Navigator System.

---

**Erstellt:** 2025-11-09  
**Version:** 1.0  
**Lizenz:** MIT  
**Autor:** JavaFleet Systems Consulting
