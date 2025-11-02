# Vision-Chaining Internationalisierung (i18n)

## Aktueller Stand (2025-11-01)

Vision-Chaining ist implementiert und funktioniert mit **Deutsch** als Ausgabesprache.

## Zukünftige Sprachen (geplant)

- 🇩🇪 **Deutsch** (bereits implementiert)
- 🇪🇸 **Spanisch** (geplant)
- 🇹🇷 **Türkisch** (geplant)
- 🇫🇷 **Französisch** (geplant)

---

## Implementierungs-Notizen für Multi-Language

### Stellen die geändert werden müssen:

#### 1. **Settings** (`settingsStore.js`)
Neue Setting hinzufügen:
```javascript
// Settings
language: 'de',  // Wird bereits genutzt!
```

#### 2. **Frontend** (`chatStore.js` - Zeile 245-249)
```javascript
// AKTUELL (hardcoded Deutsch):
const deutschPrompt = 'Du antwortest IMMER auf Deutsch.'

// ZUKÜNFTIG (dynamisch):
const languagePrompts = {
  'de': 'Du antwortest IMMER auf Deutsch.',
  'es': 'Siempre respondes en español.',
  'tr': 'Her zaman Türkçe cevap veriyorsun.',
  'fr': 'Tu réponds toujours en français.'
}
const langPrompt = languagePrompts[settingsStore.getSetting('language')] || languagePrompts['de']
requestBody.systemPrompt = requestBody.systemPrompt
  ? langPrompt + '\n\n' + requestBody.systemPrompt
  : langPrompt
```

#### 3. **Backend** (`OllamaService.java` - Zeile 632-635)
```java
// AKTUELL (hardcoded Deutsch):
String chainedPrompt = "WICHTIG: Deine Antwort MUSS auf Deutsch sein!\n\n" +
        "Bildinhalt:\n" + visionOutput + "\n\n" +
        "Frage des Nutzers: " + prompt + "\n\n" +
        "Antworte jetzt auf Deutsch:";

// ZUKÜNFTIG (Parameter übergeben):
String chainedPrompt = buildLocalizedPrompt(visionOutput, prompt, language);
```

**Neue Methode:**
```java
private String buildLocalizedPrompt(String visionOutput, String prompt, String language) {
    Map<String, String[]> templates = Map.of(
        "de", new String[]{"WICHTIG: Deine Antwort MUSS auf Deutsch sein!", "Antworte jetzt auf Deutsch:"},
        "es", new String[]{"IMPORTANTE: Tu respuesta DEBE estar en español!", "Responde ahora en español:"},
        "tr", new String[]{"ÖNEMLİ: Cevabın Türkçe olmalı!", "Şimdi Türkçe cevap ver:"},
        "fr", new String[]{"IMPORTANT: Ta réponse DOIT être en français!", "Réponds maintenant en français:"}
    );

    String[] template = templates.getOrDefault(language, templates.get("de"));
    return template[0] + "\n\nBildinhalt:\n" + visionOutput + "\n\n" +
           "Frage des Nutzers: " + prompt + "\n\n" + template[1];
}
```

#### 4. **ChatRequest.java** (DTO erweitern)
```java
// Neue Felder hinzufügen:
private String language;  // "de", "es", "tr", "fr"
```

#### 5. **Backend System-Prompt** (`OllamaService.java` - Zeile 638-640)
```java
// AKTUELL (hardcoded Deutsch):
String finalSystemPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
    ? "Du antwortest IMMER auf Deutsch.\n\n" + systemPrompt
    : "Du bist ein hilfreicher Assistent und antwortest IMMER auf Deutsch.";

// ZUKÜNFTIG (dynamisch):
Map<String, String> systemPromptTemplates = Map.of(
    "de", "Du antwortest IMMER auf Deutsch.",
    "es", "Siempre respondes en español.",
    "tr", "Her zaman Türkçe cevap veriyorsun.",
    "fr", "Tu réponds toujours en français."
);

String langInstruction = systemPromptTemplates.getOrDefault(language, systemPromptTemplates.get("de"));
String finalSystemPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
    ? langInstruction + "\n\n" + systemPrompt
    : "Du bist ein hilfreicher Assistent und " + langInstruction.toLowerCase();
```

---

## Testing Checklist (für jede Sprache)

- [ ] Settings: Sprache auswählbar
- [ ] Frontend: Language-Prompt korrekt gesetzt
- [ ] Backend: Language-Parameter empfangen
- [ ] Vision-Chaining: Ausgabe in korrekter Sprache
- [ ] System-Prompt: Mehrsprachige Instruktion funktioniert
- [ ] User-Prompt: Mehrsprachige Instruktion funktioniert

---

## Vorteile der aktuellen Architektur

✅ **Defense-in-Depth**: 3-fache Sprach-Enforcement (Frontend + Backend Prompt + System-Prompt)
✅ **Einfach erweiterbar**: Nur Map-Einträge hinzufügen
✅ **Fallback auf Deutsch**: Falls Sprache nicht unterstützt
✅ **Keine Breaking Changes**: Bestehender Code bleibt kompatibel

---

## Status

**Version**: 1.0 (Deutsch-only)
**Nächster Schritt**: Multi-Language Support implementieren (später)
**Datum**: 2025-11-01
