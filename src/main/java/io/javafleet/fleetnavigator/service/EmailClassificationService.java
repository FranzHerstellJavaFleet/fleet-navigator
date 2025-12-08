package io.javafleet.fleetnavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for AI-powered email classification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailClassificationService {

    private final OllamaService ollamaService;
    private final SettingsService settingsService;
    private static final String FALLBACK_MODEL = "llama3.2:3b"; // Fixed: exact model name

    /**
     * Classify email into categories: wichtig, abzuarbeiten, werbung
     */
    public EmailClassification classifyEmail(Map<String, Object> emailData) {
        String from = (String) emailData.get("from");
        String subject = (String) emailData.get("subject");
        String preview = (String) emailData.get("preview");

        // Extract custom category prompts if provided
        @SuppressWarnings("unchecked")
        Map<String, String> categoryPrompts = (Map<String, String>) emailData.get("categoryPrompts");

        // Priority 1: Mate's preferred model
        String model = (String) emailData.get("preferredModel");

        if (model == null || model.isEmpty()) {
            // Priority 2: Email settings model
            model = settingsService.getEmailModel();
            if (model == null || model.isEmpty()) {
                // Priority 3: User's selected model
                model = settingsService.getSelectedModel();
                if (model == null || model.isEmpty()) {
                    // Priority 4: Fallback
                    model = FALLBACK_MODEL;
                    log.info("Using fallback model: {}", model);
                } else {
                    log.info("Using selected model: {}", model);
                }
            } else {
                log.info("Using email settings model: {}", model);
            }
        } else {
            log.info("Using mate's preferred model: {}", model);
        }

        log.info("Classifying email from: {} - Subject: {} with model: {}", from, subject, model);

        String prompt = buildClassificationPrompt(from, subject, preview, categoryPrompts);

        try {
            String response = ollamaService.chat(model, prompt, null, null);
            EmailClassification classification = parseClassification(response);

            log.info("Email classified as: {} (confidence: {})",
                    classification.category, classification.confidence);

            return classification;

        } catch (Exception e) {
            log.error("Failed to classify email", e);
            // Fallback classification
            return new EmailClassification("abzuarbeiten", 0.5, "Fehler bei Klassifizierung");
        }
    }

    /**
     * Build classification prompt
     */
    private String buildClassificationPrompt(String from, String subject, String preview, Map<String, String> customPrompts) {
        // Default category definitions (detailliert)
        String wichtigDef = """
            Wichtige Emails von bekannten Kontakten, Vorgesetzten, wichtigen Kunden.
            Dringende geschäftliche Anfragen.
            Vertragliche oder rechtliche Angelegenheiten.
            Persönliche Nachrichten von Familie und Freunden.
            Terminbestätigungen für wichtige Meetings.
            Emails mit Keywords: DRINGEND, URGENT, ASAP, WICHTIG.
            """;

        String abzuarbeitenDef = """
            Emails die eine Antwort oder Aktion erfordern.
            Projekt-Updates, Aufgaben-Zuweisungen.
            Meeting-Anfragen die bestätigt werden müssen.
            Fragen von Kollegen oder Kunden die beantwortet werden müssen.
            Technische Support-Tickets.
            Rechnungen die geprüft werden müssen.
            Anfragen oder Bitten um Feedback.
            """;

        String werbungDef = """
            Newsletter und Marketing-Emails, auch wenn abonniert.
            Produktwerbung und Angebote.
            Social Media Benachrichtigungen (LinkedIn, Xing, Facebook, Twitter).
            Automatische System-Benachrichtigungen.
            Spam und unerwünschte Emails.
            Unpersönliche Massen-Emails (Sehr geehrte Damen und Herren).
            Emails von no-reply@ oder newsletter@ Adressen.
            """;

        // Use custom definitions if provided (from Thunderbird Mate settings)
        if (customPrompts != null) {
            if (customPrompts.containsKey("wichtig") && !customPrompts.get("wichtig").isEmpty()) {
                wichtigDef = customPrompts.get("wichtig");
            }
            if (customPrompts.containsKey("abzuarbeiten") && !customPrompts.get("abzuarbeiten").isEmpty()) {
                abzuarbeitenDef = customPrompts.get("abzuarbeiten");
            }
            if (customPrompts.containsKey("werbung") && !customPrompts.get("werbung").isEmpty()) {
                werbungDef = customPrompts.get("werbung");
            }
            log.info("Using custom category prompts from Mate");
        }

        return String.format("""
            Du bist ein intelligenter Email-Klassifizierungs-Assistent.
            Klassifiziere folgende Email in GENAU EINE dieser Kategorien:

            **WICHTIG:**
            %s

            **ABZUARBEITEN:**
            %s

            **WERBUNG:**
            %s

            **ZU KLASSIFIZIERENDE EMAIL:**
            Von: %s
            Betreff: %s
            Vorschau: %s

            **ANTWORT-FORMAT (GENAU SO):**
            Kategorie: [wichtig|abzuarbeiten|werbung]
            Confidence: [0.0-1.0]
            Begründung: [1-2 Sätze warum]
            """,
            wichtigDef, abzuarbeitenDef, werbungDef,
            from, subject, preview
        );
    }

    /**
     * Parse AI response into classification
     */
    private EmailClassification parseClassification(String response) {
        String category = "abzuarbeiten"; // default
        double confidence = 0.7;
        String reasoning = response;

        try {
            // Extract category
            if (response.toLowerCase().contains("kategorie:")) {
                String categoryLine = response.lines()
                        .filter(line -> line.toLowerCase().contains("kategorie:"))
                        .findFirst()
                        .orElse("");

                if (categoryLine.contains("wichtig")) {
                    category = "wichtig";
                } else if (categoryLine.contains("werbung")) {
                    category = "werbung";
                } else if (categoryLine.contains("abzuarbeiten")) {
                    category = "abzuarbeiten";
                }
            }

            // Extract confidence
            if (response.toLowerCase().contains("confidence:")) {
                String confidenceLine = response.lines()
                        .filter(line -> line.toLowerCase().contains("confidence:"))
                        .findFirst()
                        .orElse("");

                String confidenceStr = confidenceLine.replaceAll("[^0-9.]", "");
                if (!confidenceStr.isEmpty()) {
                    confidence = Double.parseDouble(confidenceStr);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse classification response, using defaults", e);
        }

        return new EmailClassification(category, confidence, reasoning);
    }

    /**
     * Get available Ollama models (for Office Mate)
     */
    public java.util.List<String> getAvailableModels() {
        try {
            return ollamaService.getAvailableModels().stream()
                    .map(model -> model.getName())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get available models", e);
            return java.util.List.of("llama3.2", "qwen2.5:7b"); // Fallback
        }
    }

    /**
     * Email classification result
     */
    public static class EmailClassification {
        public String category;
        public double confidence;
        public String reasoning;

        public EmailClassification(String category, double confidence, String reasoning) {
            this.category = category;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
    }
}
