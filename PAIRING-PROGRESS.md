# Fleet Mate Pairing - Fortschritt (25.11.2025)

## Ziel
DSGVO-konforme Ende-zu-Ende-Verschlüsselung zwischen Fleet Navigator (Java) und Fleet Mate (Go).
Bluetooth-ähnlicher Pairing-Flow: Mate muss vom Benutzer im Navigator akzeptiert werden.

## Was wurde implementiert

### Backend (Java) - FERTIG
1. **CryptoService.java** - Kryptographie
   - Ed25519 für Signaturen
   - X25519 für Schlüsselaustausch
   - AES-256-GCM für Verschlüsselung
   - Konvertierung zwischen raw 32-byte Keys (Go) und X.509 Format (Java)

2. **MatePairingService.java** - Pairing-Logik
   - `createPairingRequest()` - Erstellt Pairing-Anfrage mit 6-stelligem Code
   - `approvePairing()` - Genehmigt Pairing, speichert in DB
   - `rejectPairing()` - Lehnt ab
   - `authenticate()` - Challenge-Response Authentifizierung

3. **MatePairingController.java** - REST API
   - `GET /api/pairing/pending` - Wartende Anfragen
   - `POST /api/pairing/approve/{id}` - Genehmigen
   - `POST /api/pairing/reject/{id}` - Ablehnen
   - `GET /api/pairing/trusted` - Vertrauenswürdige Mates

4. **FleetMateWebSocketHandler.java** - WebSocket
   - Verarbeitet `pairing_request`, `auth`, `encrypted` Messages
   - Registriert authentifizierte Mates im FleetMateService
   - Unterstützt Go-Feldnamen (camelCase) via @JsonAlias

### Frontend (Vue.js) - IMPLEMENTIERT, ABER NICHT GELADEN
**FleetMatesView.vue** wurde erweitert um:
- Polling auf `/api/pairing/pending` alle 2 Sekunden
- Anzeige von Pairing-Anfragen mit:
  - Mate-Name und Typ
  - 6-stelliger Pairing-Code
  - Countdown-Timer
  - Akzeptieren/Ablehnen Buttons
  - DSGVO-Hinweis

## Aktueller Status

### Funktioniert:
- API `/api/pairing/pending` gibt korrekt pending Requests zurück
- API `/api/pairing/trusted` zeigt vertrauenswürdige Mates
- Mate sendet `pairing_request` korrekt
- Verschlüsselte Kommunikation nach Authentifizierung

### Problem:
Das Frontend zeigt die Pairing-Anfragen NICHT an, obwohl:
- Der Code korrekt in FleetMatesView.vue ist
- Die JAR-Datei das neue Frontend enthält
- Die API korrekt Daten liefert

**Vermutung:** Der Browser lädt noch altes JavaScript trotz Inkognito-Modus.

## Nächste Schritte (Morgen)

1. **Service komplett neu starten:**
   ```bash
   sudo systemctl stop fleet-navigator
   sudo cp /home/trainer/ProjekteFMH/Fleet-Navigator/target/fleet-navigator-0.3.1.jar /opt/fleet-navigator/
   sudo systemctl start fleet-navigator
   ```

2. **Browser-Test:**
   - Neues Inkognito-Fenster öffnen
   - http://localhost:2025/agents/fleet-mates
   - Developer Tools (F12) → Network Tab → Filter "pending"
   - Prüfen ob `/api/pairing/pending` aufgerufen wird

3. **Falls JS nicht geladen:**
   - Browser-Cache komplett leeren
   - Anderen Browser testen (Firefox/Chrome)
   - Prüfen welche JS-Datei geladen wird vs. welche im JAR ist

4. **Mate neu pairen:**
   ```bash
   rm ~/.fleet-mate/mate_keys.json
   cd ~/ProjekteFMH/Fleet-Mate-Linux && ./fleet-mate
   ```

## Relevante Dateien

### Backend
- `src/main/java/io/javafleet/fleetnavigator/security/CryptoService.java`
- `src/main/java/io/javafleet/fleetnavigator/security/MatePairingService.java`
- `src/main/java/io/javafleet/fleetnavigator/controller/MatePairingController.java`
- `src/main/java/io/javafleet/fleetnavigator/websocket/FleetMateWebSocketHandler.java`
- `src/main/java/io/javafleet/fleetnavigator/dto/MateMessage.java`

### Frontend
- `frontend/src/views/agents/FleetMatesView.vue` (Pairing-Anzeige)
- `frontend/src/components/MatePairingDialog.vue` (nicht verwendet, globaler Dialog)

## Test-Befehle

```bash
# Pending Anfragen prüfen
curl -s http://localhost:2025/api/pairing/pending | jq

# Trusted Mates prüfen
curl -s http://localhost:2025/api/pairing/trusted | jq

# Pairing genehmigen (requestId aus pending)
curl -X POST http://localhost:2025/api/pairing/approve/{requestId}

# Navigator Logs
sudo journalctl -u fleet-navigator -f | grep -i pairing
```

## Wichtig: Vite Cache

Bei Frontend-Änderungen IMMER:
```bash
rm -rf frontend/dist frontend/node_modules/.vite target
mvn clean package -DskipTests
```

---
Erstellt: 25.11.2025 23:50
