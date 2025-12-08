package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.model.SystemPromptTemplate;
import io.javafleet.fleetnavigator.repository.SystemPromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system-prompts")
@RequiredArgsConstructor
@Slf4j
public class SystemPromptController {

    private final SystemPromptTemplateRepository promptRepository;

    @GetMapping
    public ResponseEntity<List<SystemPromptTemplate>> getAllPrompts() {
        return ResponseEntity.ok(promptRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/default")
    public ResponseEntity<SystemPromptTemplate> getDefaultPrompt() {
        SystemPromptTemplate defaultPrompt = promptRepository.findByIsDefaultTrue();
        if (defaultPrompt != null) {
            return ResponseEntity.ok(defaultPrompt);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<SystemPromptTemplate> createPrompt(@RequestBody SystemPromptTemplate prompt) {
        log.info("Creating new system prompt: {}", prompt.getName());
        SystemPromptTemplate saved = promptRepository.save(prompt);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SystemPromptTemplate> updatePrompt(
            @PathVariable Long id,
            @RequestBody SystemPromptTemplate prompt) {

        return promptRepository.findById(id)
                .map(existing -> {
                    existing.setName(prompt.getName());
                    existing.setContent(prompt.getContent());
                    existing.setDefault(prompt.isDefault());

                    // If this is set as default, unset all others
                    if (prompt.isDefault()) {
                        promptRepository.findAll().forEach(p -> {
                            if (!p.getId().equals(id) && p.isDefault()) {
                                p.setDefault(false);
                                promptRepository.save(p);
                            }
                        });
                    }

                    return ResponseEntity.ok(promptRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(@PathVariable Long id) {
        if (promptRepository.existsById(id)) {
            promptRepository.deleteById(id);
            log.info("Deleted system prompt: {}", id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<SystemPromptTemplate> setDefaultPrompt(@PathVariable Long id) {
        return promptRepository.findById(id)
                .map(prompt -> {
                    // Unset all other defaults
                    promptRepository.findAll().forEach(p -> {
                        if (p.isDefault()) {
                            p.setDefault(false);
                            promptRepository.save(p);
                        }
                    });

                    // Set this prompt as default
                    prompt.setDefault(true);
                    SystemPromptTemplate saved = promptRepository.save(prompt);
                    log.info("Set system prompt '{}' (ID: {}) as default", saved.getName(), id);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/update-markdown")
    public ResponseEntity<String> updatePromptsWithMarkdown() {
        log.info("Updating existing prompts with Markdown instructions");

        // Update Karla (default prompt)
        SystemPromptTemplate karla = promptRepository.findByIsDefaultTrue();
        if (karla != null) {
            karla.setContent("Du bist Karla, eine erfahrene deutsche KI-Assistentin mit Expertise in Technologie, Wissenschaft und Alltag.\n\n" +
                    "Dein Kommunikationsstil:\n" +
                    "- Klar und präzise formuliert\n" +
                    "- Freundlich und professionell\n" +
                    "- Verwendet deutsche Fachterminologie wo angebracht\n" +
                    "- Erklärt komplexe Sachverhalte verständlich\n\n" +
                    "Formatierung deiner Antworten:\n" +
                    "- Nutze **Markdown-Formatierung** für bessere Lesbarkeit\n" +
                    "- Verwende **fett** für wichtige Begriffe und Hervorhebungen\n" +
                    "- Nutze *kursiv* für Betonung\n" +
                    "- Code-Snippets in `backticks` für Inline-Code\n" +
                    "- Code-Blöcke mit ```sprache für mehrzeiligen Code\n" +
                    "- Überschriften (# ## ###) für Struktur bei längeren Antworten\n" +
                    "- Listen (- oder 1.) für Aufzählungen\n" +
                    "- Tabellen (| | |) wenn sinnvoll\n\n" +
                    "Bei Bildern:\n" +
                    "- Analysiere alle visuellen Details sorgfältig\n" +
                    "- Erkenne Text, Objekte und deren Beziehungen\n" +
                    "- Beschreibe Farben, Komposition und Kontext\n" +
                    "- Identifiziere technische Elemente wie UI-Komponenten, Diagramme oder Code\n\n" +
                    "Bei Code-Fragen:\n" +
                    "- Nutze Best Practices und moderne Standards\n" +
                    "- Erläutere Konzepte mit praktischen Beispielen\n" +
                    "- Weise auf potenzielle Fallstricke hin\n\n" +
                    "Deine Stärken sind Genauigkeit, Gründlichkeit und die Fähigkeit, komplexe Themen zugänglich zu machen.");
            promptRepository.save(karla);
            log.info("Updated Karla prompt with Markdown instructions");
        }

        return ResponseEntity.ok("Prompts updated with Markdown instructions");
    }

    @PostMapping("/init-defaults")
    public ResponseEntity<String> initDefaultPrompts() {
        // Check if defaults already exist
        if (promptRepository.count() > 0) {
            return ResponseEntity.ok("Default prompts already exist");
        }

        // Karla - Deutscher Assistent (DEFAULT)
        SystemPromptTemplate karla = new SystemPromptTemplate();
        karla.setName("Karla 🇩🇪");
        karla.setContent("Du bist Karla, eine erfahrene deutsche KI-Assistentin mit Expertise in Technologie, Wissenschaft und Alltag.\n\n" +
                "Dein Kommunikationsstil:\n" +
                "- Klar und präzise formuliert\n" +
                "- Freundlich und professionell\n" +
                "- Verwendet deutsche Fachterminologie wo angebracht\n" +
                "- Erklärt komplexe Sachverhalte verständlich\n\n" +
                "Formatierung deiner Antworten:\n" +
                "- Nutze **Markdown-Formatierung** für bessere Lesbarkeit\n" +
                "- Verwende **fett** für wichtige Begriffe und Hervorhebungen\n" +
                "- Nutze *kursiv* für Betonung\n" +
                "- Code-Snippets in `backticks` für Inline-Code\n" +
                "- Code-Blöcke mit ```sprache für mehrzeiligen Code\n" +
                "- Überschriften (# ## ###) für Struktur bei längeren Antworten\n" +
                "- Listen (- oder 1.) für Aufzählungen\n" +
                "- Tabellen (| | |) wenn sinnvoll\n\n" +
                "Bei Bildern:\n" +
                "- Analysiere alle visuellen Details sorgfältig\n" +
                "- Erkenne Text, Objekte und deren Beziehungen\n" +
                "- Beschreibe Farben, Komposition und Kontext\n" +
                "- Identifiziere technische Elemente wie UI-Komponenten, Diagramme oder Code\n\n" +
                "Bei Code-Fragen:\n" +
                "- Nutze Best Practices und moderne Standards\n" +
                "- Erläutere Konzepte mit praktischen Beispielen\n" +
                "- Weise auf potenzielle Fallstricke hin\n\n" +
                "Deine Stärken sind Genauigkeit, Gründlichkeit und die Fähigkeit, komplexe Themen zugänglich zu machen.");
        karla.setDefault(true);
        promptRepository.save(karla);

        // English Assistant
        SystemPromptTemplate english = new SystemPromptTemplate();
        english.setName("English Assistant 🇬🇧");
        english.setContent("You are a helpful AI assistant.\n\n" +
                "Always respond in English, regardless of the question language.\n\n" +
                "Be concise, accurate, and professional.");
        english.setDefault(false);
        promptRepository.save(english);

        // Code Expert
        SystemPromptTemplate coder = new SystemPromptTemplate();
        coder.setName("Code Expert 💻");
        coder.setContent("Du bist ein erfahrener Software-Entwickler.\n\n" +
                "Antworte auf Deutsch.\n\n" +
                "Fokus:\n" +
                "- Clean Code Prinzipien\n" +
                "- Best Practices\n" +
                "- Ausführliche Code-Erklärungen\n" +
                "- Performance-Optimierung\n\n" +
                "Formatierung:\n" +
                "- **IMMER** Markdown verwenden\n" +
                "- Code in ```sprache Blöcken\n" +
                "- Wichtige Konzepte **fett**\n" +
                "- Kommentare *kursiv*");
        coder.setDefault(false);
        promptRepository.save(coder);

        // Vision Expert
        SystemPromptTemplate vision = new SystemPromptTemplate();
        vision.setName("Vision Expert 🖼️");
        vision.setContent("Du bist spezialisiert auf Bildanalyse.\n\n" +
                "Antworte auf Deutsch.\n\n" +
                "Bei Bildern:\n" +
                "- Beschreibe alle sichtbaren Details\n" +
                "- Erkenne Text und Objekte\n" +
                "- Analysiere Farben und Komposition\n" +
                "- Erkläre den Kontext");
        vision.setDefault(false);
        promptRepository.save(vision);

        log.info("Initialized 4 default system prompts");
        return ResponseEntity.ok("Default prompts created successfully");
    }
}
