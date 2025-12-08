# Experten-Wizard

**Status:** ✅ Vollständig Implementiert (2025-12-07)
**Version:** 0.5.0
**Komponente:** `frontend/src/components/ExpertCreationWizard.vue`

---

## Übersicht

Der Experten-Wizard führt den Benutzer durch einen **6-stufigen assistentengestützten Prozess** zur Erstellung eines personalisierten KI-Experten. Das Design ist bewusst "idiotensicher" gehalten, sodass auch IT-Fremde nicht überfordert werden.

**Prinzip:** Ein Schritt nach dem anderen - der Benutzer sieht immer nur den aktuellen Schritt, nicht alle auf einmal.

### Features
- ✅ 6-Schritte-Wizard mit Progress-Bar
- ✅ Vorwärts- und Rückwärts-Navigation
- ✅ Schritt-Indikatoren (klickbar für bereits besuchte Schritte)
- ✅ Validierung pro Schritt
- ✅ Prompt-Vorlagen für verschiedene Berufe
- ✅ Prompt aus Datei laden (.txt, .md)
- ✅ Avatar-Upload
- ✅ Dynamische Fachbereich-Verwaltung
- ✅ Theme-Unterstützung (alle 6 Themes)

---

## Wizard-Schritte

### Step 1: Modell wählen 🤖

**Ziel:** Benutzer wählt das Basis-LLM für den Experten

**UI-Elemente:**
- Cards für jedes verfügbare GGUF-Modell
- Für jedes Modell anzeigen:
  - Name (z.B. "Qwen2.5-7B-Instruct")
  - Größe (z.B. "4.4 GB")
  - Kategorie-Badge (Coder / Vision / Allgemein)
  - Stärken/Beschreibung
  - Publisher (z.B. "Alibaba Cloud")
  - Context-Window (z.B. "128k")
  - Trainingsdatum
- Ausgewähltes Modell hervorheben

**Datenquelle:** `/api/models` (alle GGUF-Modelle)

---

### Step 2: Werkzeuge 🔧

**Ziel:** Benutzer aktiviert Werkzeuge für den Experten

**UI-Elemente:**
- Toggle/Checkbox für jedes Werkzeug:

| Werkzeug | Beschreibung | Status |
|----------|--------------|--------|
| ☑️ Websuche | Automatische Internet-Recherche | Verfügbar |
| ☑️ Dateisuche | Suche in hochgeladenen Dokumenten | Verfügbar |
| ☐ Vektordatenbank | RAG aus eigenem Wissens-Index | Ausgegraut ("Kommt bald") |

**Bei Websuche aktiviert:**
- Eingabefeld für Such-Domains (komma-getrennt)
- Beispiel: "gesetze-im-internet.de, dejure.org"
- Max. Suchergebnisse (Slider: 1-10, Standard: 5)

**Bei Dateisuche aktiviert:**
- Dokumenten-Verzeichnis auswählen/erstellen

---

### Step 3: Parameter ⚙️

**Ziel:** Feintuning der Modell-Parameter

**UI-Elemente:**

| Parameter | Typ | Standard | Bereich | Beschreibung |
|-----------|-----|----------|---------|--------------|
| Context-Size | Number | Auto (Max) | 2048 - Model-Max | Automatisch Maximum des gewählten Modells |
| Max Tokens | Number | 4096 | 256 - 32768 | Maximale Antwort-Länge |
| Temperature | Slider | 0.7 | 0.0 - 2.0 | Kreativität (höher = kreativer) |
| Top-P | Slider | 0.9 | 0.0 - 1.0 | Nucleus Sampling |
| Top-K | Number | 40 | 1 - 100 | Optional, erweitert |
| Repeat Penalty | Slider | 1.1 | 1.0 - 2.0 | Optional, erweitert |

**Hinweis:** Context-Size wird automatisch auf das Maximum des gewählten Modells gesetzt, kann aber reduziert werden.

---

### Step 4: Persönlichkeit 👤

**Ziel:** Definition der Experten-Identität

**UI-Elemente:**

| Feld | Typ | Pflicht | Beispiel |
|------|-----|---------|----------|
| Name | Text | ✅ | "Roland" |
| Rolle | Text | ✅ | "Rechtsanwalt" |
| Beschreibung | Textarea | ❌ | "Spezialist für Verwaltungsrecht" |
| Avatar | Bild-Upload | ❌ | Profilbild des Experten |
| Basis-Prompt | Textarea | ✅ | "Du bist ein erfahrener Rechtsanwalt..." |
| Personality-Prompt | Textarea | ❌ | "Sprich den Nutzer mit Sie an..." |

**Basis-Prompt Vorlagen:**
- Button "Vorlage laden" mit Beispielen für verschiedene Berufe
- Rechtsanwalt, Steuerberater, Arzt, Programmierer, etc.

**Personality-Prompt Optionen:**
- Anrede: Du / Sie
- Stil: Formal / Freundlich / Humorvoll
- Ausführlichkeit: Kurz / Normal / Detailliert

---

### Step 5: Fachbereiche (Blickwinkel) 📚

**Ziel:** Definition von spezialisierten Modi

**Mindestanforderung:** 1 Fachbereich

**UI-Elemente:**
- Liste der Fachbereiche mit "Hinzufügen" Button
- Für jeden Fachbereich:

| Feld | Typ | Pflicht | Beispiel |
|------|-----|---------|----------|
| Name | Text | ✅ | "Verwaltungsrecht" |
| Beschreibung | Text | ❌ | "Behörden, Anträge, Bescheide" |
| Zusatz-Prompt | Textarea | ❌ | "Fokussiere auf verwaltungsrechtliche Aspekte..." |
| Keywords | Tags | ❌ | "behörde, antrag, bescheid, widerspruch" |
| Priorität | Number | ❌ | 1-10 (für Keyword-Matching) |

**Optionale Parameter pro Fachbereich:**
- Eigene Temperature (überschreibt Experten-Standard)
- Eigene Top-P
- Eigene Max Tokens

**Beispiel-Fachbereiche für Rechtsanwalt:**
1. Verwaltungsrecht
2. Sozialrecht
3. Strafrecht
4. Arbeitsrecht

---

### Step 6: Zusammenfassung & Erstellen ✅

**Ziel:** Übersicht und Bestätigung

**UI-Elemente:**
- Zusammenfassung aller Einstellungen in kompakter Form
- Gruppen:
  - 🤖 Modell: [Name] ([Größe])
  - 🔧 Werkzeuge: Websuche ✓, Dateisuche ✗
  - ⚙️ Parameter: Temp 0.7, Context 8192, ...
  - 👤 Persönlichkeit: [Name], [Rolle]
  - 📚 Fachbereiche: [Anzahl] definiert

**Buttons:**
- "← Zurück" - Zum vorherigen Schritt
- "Experte erstellen" - Speichern und schließen

---

## Technische Umsetzung

### Neue Komponente
```
frontend/src/components/ExpertCreationWizard.vue
```

### Props
```javascript
defineProps({
  show: Boolean,           // Modal sichtbar
  editExpert: Object       // Optional: Bestehenden Experten bearbeiten
})
```

### Emits
```javascript
defineEmits(['close', 'created', 'updated'])
```

### State
```javascript
const currentStep = ref(1)
const totalSteps = 6

const wizardData = ref({
  // Step 1
  selectedModel: null,
  modelInfo: {},

  // Step 2
  autoWebSearch: false,
  searchDomains: '',
  maxSearchResults: 5,
  autoFileSearch: false,
  documentDirectory: '',

  // Step 3
  defaultNumCtx: null,      // Auto from model
  defaultMaxTokens: 4096,
  defaultTemperature: 0.7,
  defaultTopP: 0.9,
  topK: null,
  repeatPenalty: null,

  // Step 4
  name: '',
  role: '',
  description: '',
  avatarUrl: null,
  basePrompt: '',
  personalityPrompt: '',

  // Step 5
  modes: [{
    name: '',
    description: '',
    promptAddition: '',
    keywords: '',
    priority: 0,
    temperature: null,
    topP: null,
    maxTokens: null
  }]
})
```

### API-Calls
- `GET /api/models` - Modelle laden (Step 1)
- `GET /api/models/{name}/details` - Modell-Details (Context-Window)
- `POST /api/experts` - Experte erstellen (Step 6)

---

## Design-Richtlinien

### Anwalt Hell Theme
- Navy Blue (#1E4D7B) als Akzentfarbe
- Weiße Buttons auf dunklem Hintergrund
- Serif-Schriften (Lora, Merriweather)

### Allgemein
- Progress-Bar oben (Step 1 von 6)
- Große, klickbare Cards für Modell-Auswahl
- Tooltips für Parameter-Erklärungen
- Validierung pro Step (nicht weiter wenn Pflichtfelder fehlen)
- "Idiotensicher" - keine technischen Irritationen

---

## Prompt-Vorlagen (integriert)

Der Wizard enthält folgende vorgefertigte Vorlagen:

| Vorlage | Rolle | Fachbereiche |
|---------|-------|--------------|
| Rechtsanwalt | Rechtsanwalt | Zivilrecht, Strafrecht, Verwaltungsrecht, Arbeitsrecht |
| Steuerberater | Steuerberater | Einkommensteuer, Umsatzsteuer, Gewerbesteuer, Erbschaftsteuer |
| Software-Entwickler | Senior Software-Entwickler | Code-Review, Debugging, Architektur, Best Practices |
| Arzt | Allgemeinmediziner | Prävention, Symptome, Medikamente, Ernährung |
| Marketing-Experte | Marketing-Stratege | Social Media, Content Marketing, SEO, Branding |

---

## Integration

### Aufruf des Wizards

Der Wizard wird über den ModelManager geöffnet:

```javascript
// In ModelManager.vue
import ExpertCreationWizard from './ExpertCreationWizard.vue'

const showExpertWizard = ref(false)

function openCreateExpertModal() {
  showExpertWizard.value = true
}
```

```vue
<ExpertCreationWizard
  :show="showExpertWizard"
  @close="showExpertWizard = false"
  @created="onExpertCreated"
/>
```

### Events

| Event | Beschreibung |
|-------|--------------|
| `close` | Wizard wurde geschlossen |
| `created` | Neuer Experte wurde erstellt (enthält Expert-Objekt) |
| `updated` | Bestehender Experte wurde aktualisiert |

---

## Abhängigkeiten

- Backend: Expert-Entity ✅
- Backend: ExpertMode-Entity ✅
- Backend: REST API `/api/experts` ✅
- Backend: Avatar-Upload `/api/experts/avatar/upload` ✅
- Frontend: api.js Funktionen ✅
- Frontend: useToast Composable ✅

---

## Zukünftige Erweiterungen

1. **Import/Export:** Experten als JSON exportieren/importieren
2. **Duplikation:** Bestehenden Experten als Vorlage verwenden
3. **Edit-Modus:** Wizard zum Bearbeiten bestehender Experten nutzen
