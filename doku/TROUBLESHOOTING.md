# Fleet Writer Officer - Installation & Troubleshooting

## 🎯 Schnellstart

### Schritt 1: Extension installieren

```bash
# Im FleetWriterOfficer-Verzeichnis:
unopkg add FleetWriterOfficer.oxt

# ODER für alle Benutzer (benötigt sudo):
sudo unopkg add --shared FleetWriterOfficer.oxt
```

### Schritt 2: LibreOffice neu starten

```bash
# Alle LibreOffice-Prozesse beenden
killall soffice.bin

# LibreOffice Writer starten
soffice --writer
```

### Schritt 3: Extension aufrufen

1. **Writer-Dokument öffnen** (wichtig!)
2. **Menü:** Extras → Add-Ons → **Fleet Writer Officer**
3. Oder: **Werkzeugleiste** → Fleet Writer Icon

---

## ❌ Häufige Fehler und Lösungen

### Fehler 1: "Scripting Framework Error"

**Symptom:**
```
Ein Scripting Framework Fehler trat während der Ausführung vom Python Skript 
vnd.sun.star.script:fleet_writer_officer$trigger?language=Python&location=user auf.
```

**Ursache:** Python-Provider nicht installiert oder falsche Registrierung

**Lösung:**

#### Linux (Ubuntu/Debian)
```bash
# Python-Provider installieren
sudo apt install libreoffice-script-provider-python

# LibreOffice neu installieren falls nötig
sudo apt install --reinstall libreoffice

# Extension neu installieren
unopkg remove io.javafleet.FleetWriterOfficer
unopkg add FleetWriterOfficer.oxt
```

#### macOS
```bash
# LibreOffice komplett neu installieren
brew uninstall --cask libreoffice
brew install --cask libreoffice

# Extension installieren
unopkg add FleetWriterOfficer.oxt
```

#### Windows
```powershell
# LibreOffice reparieren:
# Systemsteuerung → Programme → LibreOffice → Reparieren

# Dann Extension installieren
unopkg add FleetWriterOfficer.oxt
```

---

### Fehler 2: "ModuleNotFoundError" oder "getModuleByUrl"

**Symptom:**
```
File "/usr/lib/libreoffice/program/pythonscript.py", line 1058
mod = self.provCtx.getModuleByUrl(fileUri)
```

**Ursache:** Extension-Datei nicht korrekt in manifest.xml registriert

**Lösung:**

1. **Prüfe manifest.xml:**
   ```bash
   # In FleetWriterOfficer/META-INF/manifest.xml muss stehen:
   manifest:full-path="fleet_writer_officer.py"
   
   # NICHT:
   manifest:full-path="src/fleet_writer_officer.py"
   ```

2. **Extension neu bauen:**
   ```bash
   cd FleetWriterOfficer
   python3 build.py
   unopkg remove io.javafleet.FleetWriterOfficer
   unopkg add FleetWriterOfficer.oxt
   ```

3. **Dateipfad prüfen:**
   ```bash
   # OXT ist nur ein ZIP - entpacken und prüfen:
   unzip -l FleetWriterOfficer.oxt
   
   # Sollte ausgeben:
   # fleet_writer_officer.py      <- MUSS im Root sein!
   # META-INF/manifest.xml
   # Addons.xcu
   # ...
   ```

---

### Fehler 3: "InteractiveAugmentedIOException"

**Symptom:**
```
<class 'com.sun.star.ucb.InteractiveAugmentedIOException'>: 
an error occurred during file opening
```

**Ursache:** 
- Falsche Berechtigungen
- .pyc Datei statt .py Datei verwendet
- Encoding-Problem

**Lösung:**

1. **Verwende NIEMALS .pyc Dateien:**
   ```bash
   # Lösche alle .pyc Dateien
   find . -name "*.pyc" -delete
   find . -name "__pycache__" -type d -delete
   
   # Nur .py Dateien verwenden!
   ```

2. **UTF-8 Encoding sicherstellen:**
   ```python
   # Am Anfang jeder Python-Datei:
   # -*- coding: utf-8 -*-
   ```

3. **Berechtigungen prüfen:**
   ```bash
   chmod 644 fleet_writer_officer.py
   chmod 644 META-INF/manifest.xml
   ```

---

### Fehler 4: Extension erscheint nicht im Menü

**Symptom:** Extension ist installiert, aber kein Menü-Eintrag sichtbar

**Lösung:**

1. **Prüfe ob Extension installiert ist:**
   ```bash
   unopkg list
   
   # Sollte zeigen:
   # io.javafleet.FleetWriterOfficer
   ```

2. **Prüfe Addons.xcu:**
   ```xml
   <!-- URL muss exakt so aussehen: -->
   <value>vnd.sun.star.script:fleet_writer_officer.py$trigger?language=Python&amp;location=user:uno_packages</value>
   
   <!-- WICHTIG: 
        - Dateiname: fleet_writer_officer.py (nicht .pyc!)
        - Funktion: $trigger (mit $)
        - location=user:uno_packages (nicht nur "user")
   -->
   ```

3. **LibreOffice Cache löschen:**
   ```bash
   # Linux
   rm -rf ~/.config/libreoffice/4/user/cache
   
   # macOS
   rm -rf ~/Library/Application\ Support/LibreOffice/4/user/cache
   
   # Windows
   # Löschen: C:\Users\[Name]\AppData\Roaming\LibreOffice\4\user\cache
   ```

4. **Extension neu registrieren:**
   ```bash
   unopkg remove io.javafleet.FleetWriterOfficer
   killall soffice.bin
   unopkg add FleetWriterOfficer.oxt
   soffice --writer
   ```

---

### Fehler 5: "No module named 'websocket'"

**Symptom:**
```
ModuleNotFoundError: No module named 'websocket'
```

**Lösung:** WebSocket-Modul in Extension bundlen

1. **Erstelle lib-Ordner:**
   ```bash
   cd FleetWriterOfficer
   mkdir -p lib
   ```

2. **Installiere websocket-client in lib:**
   ```bash
   pip3 install websocket-client -t lib/
   ```

3. **Update fleet_writer_officer.py:**
   ```python
   # Ganz am Anfang, VOR allen imports:
   import sys
   import os
   
   # Füge lib-Ordner zum Python-Path hinzu
   lib_path = os.path.join(os.path.dirname(__file__), 'lib')
   if lib_path not in sys.path:
       sys.path.insert(0, lib_path)
   
   # Jetzt erst importieren:
   import uno
   import websocket  # Funktioniert jetzt!
   ```

4. **Update build.py:**
   ```python
   # Füge in files_to_include hinzu:
   import glob
   
   # Nach den normalen Dateien:
   for file in glob.glob('lib/**/*.py', recursive=True):
       if '__pycache__' not in file:
           files_to_include.append(file)
   ```

5. **Neu bauen:**
   ```bash
   python3 build.py
   unopkg remove io.javafleet.FleetWriterOfficer
   unopkg add FleetWriterOfficer.oxt
   ```

---

## 🔍 Debug-Tipps

### Python-Logs ansehen

1. **Python-Logging aktivieren:**
   ```python
   # In fleet_writer_officer.py:
   import logging
   
   logging.basicConfig(
       filename=os.path.expanduser('~/fleet_writer_officer.log'),
       level=logging.DEBUG,
       format='%(asctime)s - %(levelname)s - %(message)s'
   )
   logger = logging.getLogger(__name__)
   logger.info("Extension gestartet")
   ```

2. **Log-Datei beobachten:**
   ```bash
   tail -f ~/fleet_writer_officer.log
   ```

### LibreOffice mit Debugging starten

```bash
# Linux/macOS
export DEBUG_EXTENSION=1
soffice --writer --norestore 2>&1 | tee libreoffice_debug.log

# Oder mit mehr Details:
SAL_LOG="+WARN+INFO" soffice --writer
```

### Extension manuell prüfen

```bash
# Extension-Inhalt ansehen
unzip -l FleetWriterOfficer.oxt

# Extension entpacken und Dateien prüfen
mkdir temp_extract
cd temp_extract
unzip ../FleetWriterOfficer.oxt

# Dateien prüfen:
cat fleet_writer_officer.py  # Muss Python-Code enthalten
cat META-INF/manifest.xml    # Prüfe Pfade
```

### Python-UNO Bridge testen

```python
#!/usr/bin/env python3
# test_uno.py

import sys
sys.path.append('/usr/lib/libreoffice/program')

try:
    import uno
    print("✅ UNO importiert")
    
    import unohelper
    print("✅ unohelper importiert")
    
    from com.sun.star.task import XJobExecutor
    print("✅ XJobExecutor importiert")
    
    print("\n🎉 Python-UNO Bridge funktioniert!")
    
except ImportError as e:
    print(f"❌ Import-Fehler: {e}")
    print("\nLösung:")
    print("sudo apt install libreoffice-script-provider-python")
```

```bash
python3 test_uno.py
```

---

## ✅ Erfolgreiche Installation verifizieren

### Test 1: Extension ist installiert
```bash
unopkg list | grep FleetWriterOfficer
# ✅ Sollte zeigen: io.javafleet.FleetWriterOfficer
```

### Test 2: Menü-Eintrag ist sichtbar
1. LibreOffice Writer öffnen
2. Menü **Extras** → **Add-Ons** 
3. ✅ **Fleet Writer Officer** sollte sichtbar sein

### Test 3: Extension funktioniert
1. Writer-Dokument öffnen
2. **Extras** → **Add-Ons** → **Fleet Writer Officer** klicken
3. ✅ Test-Text sollte eingefügt werden
4. ✅ MessageBox "Erfolg" erscheint

---

## 🔧 Vollständige Neuinstallation

Falls nichts hilft:

```bash
# 1. Alle Extensions entfernen
unopkg list | grep -v "^ID" | while read line; do
    id=$(echo $line | awk '{print $1}')
    unopkg remove $id
done

# 2. LibreOffice Cache löschen
rm -rf ~/.config/libreoffice/4/user/cache
rm -rf ~/.config/libreoffice/4/user/extensions
rm -rf ~/.config/libreoffice/4/user/uno_packages

# 3. LibreOffice komplett beenden
killall soffice.bin

# 4. Extension neu installieren
cd FleetWriterOfficer
python3 build.py
unopkg add FleetWriterOfficer.oxt

# 5. LibreOffice starten
soffice --writer
```

---

## 📞 Support

Falls Probleme weiterhin bestehen:

1. **Logs sammeln:**
   ```bash
   unopkg list > unopkg_list.txt
   ls -la ~/.config/libreoffice/4/user/uno_packages > packages.txt
   ```

2. **System-Info:**
   ```bash
   soffice --version
   python3 --version
   uname -a
   ```

3. **Fehler beschreiben:**
   - Welche Fehlermeldung?
   - Wann tritt der Fehler auf?
   - Was wurde bereits versucht?

---

**Erstellt:** 2025-11-09  
**Version:** 1.0  
**Projekt:** Fleet Writer Officer
