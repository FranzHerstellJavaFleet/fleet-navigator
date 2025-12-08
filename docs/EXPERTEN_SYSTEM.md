# Fleet Navigator - Experten-System

## Übersicht

Das Experten-System ermöglicht es, spezialisierte AI-Experten mit verschiedenen "Blickwinkeln" (Modi) zu erstellen. Jeder Experte basiert auf einem Ollama-Modell und kann dynamisch um neue Perspektiven erweitert werden.

## Konzept

### Experte
Ein Experte ist eine AI-Persönlichkeit mit:
- **Name**: z.B. "Roland", "Ayşe", "Dr. Schmidt"
- **Rolle**: z.B. "Rechtsanwalt", "Marketing-Expertin", "Steuerberater"
- **Basis-Prompt**: Definiert die Grundpersönlichkeit
- **Basis-Modell**: Das verwendete Ollama-Modell
- **Modi**: Verschiedene Blickwinkel/Perspektiven

### Modus (Blickwinkel)
Ein Modus erweitert den Experten um eine spezifische Perspektive:
- **Name**: z.B. "Verwaltungsrecht", "Sozialrecht", "Strafrecht"
- **Prompt-Erweiterung**: Wird an den Basis-Prompt angehängt
- **Keywords**: Für automatische Modus-Erkennung
- **Temperatur**: Optionale Anpassung pro Modus
- **Priorität**: Bestimmt Reihenfolge bei Auto-Erkennung

## Verwendung

### 1. Experte erstellen

1. Klicke auf den lila **Experten-System** Button in der TopBar
2. Klicke auf "Neuen Experten erstellen"
3. Fülle aus:
   - **Name**: Der Name des Experten
   - **Rolle**: Beruf/Funktion
   - **Basis-Modell**: Wähle ein Ollama-Modell
   - **Basis-Prompt**: Die Grundpersönlichkeit

**Beispiel Basis-Prompt für Roland (Rechtsanwalt):**
```
Du bist Roland, ein erfahrener Rechtsanwalt mit 20 Jahren Berufserfahrung.

DEINE EXPERTISE:
- Umfassendes juristisches Fachwissen
- Praktische Erfahrung aus hunderten Mandaten
- Verständliche Erklärung komplexer Sachverhalte

DEIN STIL:
- Professionell aber zugänglich
- Strukturierte Antworten
- Praxisnahe Beispiele
- Immer mit Hinweis auf individuelle Rechtsberatung
```

### 2. Modi hinzufügen

1. Bei einem Experten auf "Hinzufügen" klicken
2. Modus konfigurieren:
   - **Name**: z.B. "Verwaltungsrecht"
   - **Prompt-Erweiterung**: Spezifische Anweisungen
   - **Keywords**: Für Auto-Erkennung (komma-separiert)
   - **Priorität**: Höher = wird zuerst geprüft

**Beispiel Modus "Verwaltungsrecht":**
```
Name: Verwaltungsrecht
Keywords: behörde,antrag,bescheid,verwaltung,amt,genehmigung

Prompt-Erweiterung:
AKTUELLER MODUS: Verwaltungsrecht

DEINE AUFGABE:
- Fokussiere auf verwaltungsrechtliche Aspekte
- Erkläre Verwaltungsverfahren verständlich
- Berücksichtige Fristen und Rechtsmittel

WICHTIG:
- Verweise auf zuständige Behörden
- Erkläre den Instanzenzug
- Beachte Besonderheiten des Verwaltungsrechts
```

### 3. Experte testen

1. Klicke auf "Testen" beim Experten
2. Wähle optional einen Modus (oder "Auto-Detect")
3. Stelle eine Frage
4. Der verwendete Modus wird in der Antwort angezeigt

## Auto-Modus-Erkennung

Wenn kein Modus explizit gewählt wird, analysiert das System die Anfrage:

1. Durchsucht alle Modi nach Keyword-Matches
2. Modi mit höherer Priorität werden zuerst geprüft
3. Erster Treffer wird verwendet
4. Ohne Treffer: Nur Basis-Prompt (kein Modus)

**Beispiel:**
- Frage: "Wie kann ich gegen einen Bescheid der Behörde vorgehen?"
- System erkennt: "Bescheid", "Behörde" → Modus "Verwaltungsrecht"

## REST-API

### Experten

```bash
# Alle Experten abrufen
GET /api/experts

# Experte erstellen
POST /api/experts
{
  "name": "Roland",
  "role": "Rechtsanwalt",
  "description": "Erfahrener Jurist",
  "basePrompt": "Du bist Roland...",
  "baseModel": "llama3.2:latest",
  "defaultTemperature": 0.7,
  "defaultNumCtx": 8192
}

# Experte aktualisieren
PUT /api/experts/{id}

# Experte löschen
DELETE /api/experts/{id}
```

### Modi

```bash
# Modi eines Experten abrufen
GET /api/experts/{id}/modes

# Modus hinzufügen
POST /api/experts/{id}/modes
{
  "name": "Verwaltungsrecht",
  "description": "Verwaltungsrechtliche Perspektive",
  "promptAddition": "AKTUELLER MODUS: Verwaltungsrecht...",
  "keywords": "behörde,antrag,bescheid",
  "temperature": 0.7,
  "priority": 10
}

# Modus aktualisieren
PUT /api/experts/modes/{modeId}

# Modus löschen
DELETE /api/experts/modes/{modeId}
```

### Experte befragen

```bash
POST /api/experts/{id}/ask
{
  "input": "Wie wehre ich mich gegen einen Bescheid?",
  "mode": null  // null = Auto-Detect, oder expliziter Modus-Name
}

# Response
{
  "answer": "Um sich gegen einen Bescheid zu wehren...",
  "usedMode": "Verwaltungsrecht",
  "expertName": "Roland"
}
```

## Technische Details

### Datenbank-Entitäten

**Expert:**
- id, name, role, description
- basePrompt, baseModel
- defaultTemperature, defaultTopP, defaultNumCtx
- active, createdAt, updatedAt
- modes (OneToMany)

**ExpertMode:**
- id, name, description
- promptAddition, keywords
- temperature, priority
- active, createdAt, updatedAt
- expert (ManyToOne)

### Dateien

**Backend:**
- `src/main/java/io/javafleet/fleetnavigator/experts/`
  - `model/Expert.java`
  - `model/ExpertMode.java`
  - `repository/ExpertRepository.java`
  - `repository/ExpertModeRepository.java`
  - `service/ExpertSystemService.java`
  - `controller/ExpertController.java`

**Frontend:**
- `frontend/src/components/ExpertManager.vue`
- `frontend/src/components/CreateExpertModal.vue`
- `frontend/src/components/ExpertModeModal.vue`
- `frontend/src/components/TestExpertModal.vue`

## Beispiel: Rechtsanwalt mit Modi

### Experte "Roland"

```
Name: Roland
Rolle: Rechtsanwalt
Modell: llama3.2:latest

Basis-Prompt:
Du bist Roland, ein erfahrener Rechtsanwalt mit 20 Jahren Berufserfahrung
in verschiedenen Rechtsgebieten. Du erklärst juristische Sachverhalte
verständlich und gibst praxisnahe Einschätzungen.

WICHTIG: Weise immer darauf hin, dass deine Antworten keine individuelle
Rechtsberatung ersetzen.
```

### Modus "Verwaltungsrecht"
```
Keywords: behörde,antrag,bescheid,verwaltung,amt,widerspruch
Priorität: 10

Prompt-Erweiterung:
AKTUELLER BLICKWINKEL: Verwaltungsrecht

Fokussiere auf:
- Verwaltungsverfahrensgesetz (VwVfG)
- Widerspruchsverfahren
- Verwaltungsgerichtsbarkeit
- Fristen und Rechtsmittel
```

### Modus "Sozialrecht"
```
Keywords: rente,arbeitslosengeld,krankenkasse,sozial,hartz,bürgergeld
Priorität: 10

Prompt-Erweiterung:
AKTUELLER BLICKWINKEL: Sozialrecht

Fokussiere auf:
- Sozialgesetzbücher (SGB)
- Anspruchsvoraussetzungen
- Widerspruchsverfahren bei Sozialbehörden
- Sozialgericht
```

### Modus "Strafrecht"
```
Keywords: anzeige,straftat,polizei,staatsanwalt,strafe,verfahren
Priorität: 10

Prompt-Erweiterung:
AKTUELLER BLICKWINKEL: Strafrecht

Fokussiere auf:
- Strafgesetzbuch (StGB)
- Strafprozessordnung (StPO)
- Ermittlungsverfahren
- Verteidigungsrechte
```

## Tipps

1. **Keywords clever wählen**: Verwende spezifische Begriffe, die eindeutig einem Rechtsgebiet zugeordnet werden können

2. **Prioritäten nutzen**: Wenn Keywords überlappen, bestimmt die Priorität welcher Modus gewählt wird

3. **Basis-Prompt allgemein halten**: Der Basis-Prompt definiert die Persönlichkeit, die Modi die Spezialisierung

4. **Testen**: Nutze das Test-Interface um die Auto-Erkennung zu überprüfen

5. **Temperatur anpassen**: Niedrigere Temperatur (0.3-0.5) für faktische Antworten, höhere (0.7-0.9) für kreative

## Version

- Implementiert in Fleet Navigator 0.3.1
- November 2024
