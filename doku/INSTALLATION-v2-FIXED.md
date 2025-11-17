# Fleet Writer Officer - Installation (KORRIGIERT)

## ⚠️ Das Problem war:

Der ursprüngliche Code hatte:
- ❌ Falsche URL-Syntax in `Addons.xcu`
- ❌ Fehlende `XJob` Interface-Implementierung  
- ❌ Keine `Jobs.xcu` Registrierung
- ❌ Fehlende `XServiceInfo` Implementierung

## ✅ Die Lösung:

**Neue Version verwendet:**
1. ✅ `XJob` Interface statt `XJobExecutor`
2. ✅ `execute()` Methode statt `trigger()`
3. ✅ `Jobs.xcu` für Service-Registrierung
4. ✅ Korrekte `vnd.sun.star.job:service=...` URL

---

## 🚀 Installation

### Schritt 1: Alte Version entfernen (falls vorhanden)

```bash
# Entferne alte Version
unopkg remove io.javafleet.FleetWriterOfficer

# LibreOffice komplett beenden
killall soffice.bin
```

### Schritt 2: Python-Provider prüfen

**Linux:**
```bash
sudo apt install libreoffice-script-provider-python
```

**macOS/Windows:**
- Bereits vorhanden

### Schritt 3: NEUE Extension installieren

```bash
unopkg add FleetWriterOfficer-v2-FIXED.oxt
```

### Schritt 4: LibreOffice neu starten

```bash
killall soffice.bin
soffice --writer
```

---

## ✅ Test

1. **Writer-Dokument öffnen** (wichtig!)
2. **Menü:** Extras → Add-Ons → **Fleet Writer Officer**
3. ✅ MessageBox sollte erscheinen
4. ✅ Test-Text wird eingefügt

---

## 🔍 Was wurde geändert?

### fleet_writer_officer.py

**Vorher:**
```python
class FleetWriterOfficer(unohelper.Base, XJobExecutor):
    def trigger(self, args):
        # ...
```

**Nachher:**
```python
class FleetWriterOfficer(unohelper.Base, XJob, XServiceInfo):
    def execute(self, args):
        # ...
    
    def getImplementationName(self):
        return "io.javafleet.FleetWriterOfficer"
    
    def supportsService(self, ServiceName):
        return ServiceName in ("com.sun.star.task.Job",)
```

### Addons.xcu

**Vorher:**
```xml
<value>vnd.sun.star.script:fleet_writer_officer.py$trigger?language=Python...</value>
```

**Nachher:**
```xml
<value>vnd.sun.star.job:service=io.javafleet.FleetWriterOfficer</value>
```

### Neue Datei: Jobs.xcu

```xml
<node oor:name="io.javafleet.FleetWriterOfficer" oor:op="replace">
    <prop oor:name="Service" oor:type="xs:string">
        <value>io.javafleet.FleetWriterOfficer</value>
    </prop>
</node>
```

---

## 🐛 Falls es immer noch nicht funktioniert

### Debug-Schritte

```bash
# 1. Prüfe ob Extension installiert ist
unopkg list

# Sollte zeigen:
# Identifier: io.javafleet.FleetWriterOfficer
# Version: 1.0.0

# 2. Cache löschen
rm -rf ~/.config/libreoffice/4/user/cache
rm -rf ~/.config/libreoffice/4/user/uno_packages/cache

# 3. Komplett neu installieren
unopkg remove io.javafleet.FleetWriterOfficer
killall soffice.bin
unopkg add FleetWriterOfficer-v2-FIXED.oxt
soffice --writer

# 4. Python-Logs checken
# Extension schreibt nach: ~/fleet_writer_officer.log (falls aktiviert)
```

### LibreOffice mit Debug-Output starten

```bash
SAL_LOG="+WARN+INFO" soffice --writer 2>&1 | grep -i python
```

---

## 📋 Checkliste

- [ ] Alte Extension deinstalliert
- [ ] LibreOffice komplett beendet
- [ ] Python-Provider installiert (Linux)
- [ ] Cache gelöscht
- [ ] Neue Extension installiert
- [ ] LibreOffice neu gestartet
- [ ] Writer-Dokument geöffnet
- [ ] Menü-Eintrag sichtbar
- [ ] Extension funktioniert

---

## 💡 Technische Details

### Warum XJob statt XJobExecutor?

**XJobExecutor** ist veraltet und funktioniert nicht mehr zuverlässig mit Python-Extensions in neueren LibreOffice-Versionen.

**XJob** ist der moderne Standard für:
- Menu-Aktionen
- Toolbar-Buttons
- Event-Handler

### Service-Registrierung

Die Extension registriert sich jetzt als **UNO Service**:

```
Service-Name: com.sun.star.task.Job
Implementation: io.javafleet.FleetWriterOfficer
```

Dies ermöglicht LibreOffice, die Extension korrekt zu laden und aufzurufen.

---

**Version:** 2.0 (KORRIGIERT)  
**Datum:** 2025-11-09  
**Projekt:** Fleet Writer Officer
