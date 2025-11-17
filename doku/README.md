# Fleet Writer Officer - Schnellanleitung

## 🚀 Installation (3 Schritte)

### 1. Python-Provider installieren (nur einmal)

**Linux (Ubuntu/Debian):**
```bash
sudo apt install libreoffice-script-provider-python
```

**macOS/Windows:**
- Standardmäßig bereits installiert

### 2. Extension installieren

```bash
unopkg add FleetWriterOfficer.oxt
```

### 3. LibreOffice neu starten

```bash
killall soffice.bin
soffice --writer
```

---

## ✅ Test

1. **Writer-Dokument öffnen**
2. **Menü:** Extras → Add-Ons → **Fleet Writer Officer**
3. ✅ Test-Text sollte eingefügt werden

---

## ❌ Fehler?

### "Scripting Framework Error"
```bash
# Python-Provider installieren:
sudo apt install libreoffice-script-provider-python

# Extension neu installieren:
unopkg remove io.javafleet.FleetWriterOfficer
unopkg add FleetWriterOfficer.oxt
```

### Menü-Eintrag fehlt
```bash
# Cache löschen:
rm -rf ~/.config/libreoffice/4/user/cache

# LibreOffice neu starten:
killall soffice.bin
soffice --writer
```

### Mehr Lösungen
➡️ Siehe **TROUBLESHOOTING.md**

---

## 📁 Dateien

- **FleetWriterOfficer.oxt** - Extension-Package
- **TROUBLESHOOTING.md** - Detaillierte Fehlerbehebung
- **LIBREOFFICE-PYTHON-WEBSOCKET-EXTENSION.md** - Vollständige Dokumentation

---

## 🔨 Extension selbst bauen

```bash
cd FleetWriterOfficer
python3 build.py
```

---

## 🗑️ Deinstallation

```bash
unopkg remove io.javafleet.FleetWriterOfficer
```

---

**Version:** 1.0.0  
**Projekt:** Fleet Writer Officer  
**Autor:** JavaFleet Systems Consulting
