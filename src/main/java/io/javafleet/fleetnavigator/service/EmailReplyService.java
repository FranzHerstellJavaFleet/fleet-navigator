package io.javafleet.fleetnavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating AI-powered email replies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReplyService {

    private final OllamaService ollamaService;
    private final SettingsService settingsService;
    private static final String FALLBACK_MODEL = "llama3.1:8b";

    /**
     * Generate a professional email reply
     *
     * @param from Original sender
     * @param subject Original subject
     * @param body Original email body
     * @param model AI model to use (can be null to use settings)
     * @return Generated reply text
     */
    public String generateReply(String from, String subject, String body, String model) {
        // If no model specified, get from email settings
        if (model == null || model.isEmpty()) {
            model = settingsService.getEmailModel();
            if (model == null || model.isEmpty()) {
                // Fallback to user's selected model
                model = settingsService.getSelectedModel();
                if (model == null || model.isEmpty()) {
                    model = FALLBACK_MODEL;
                    log.info("No email model or selected model, using fallback: {}", model);
                } else {
                    log.info("No email model set, using selected model: {}", model);
                }
            } else {
                log.info("Using dedicated email model: {}", model);
            }
        }

        log.info("Generating reply for email from: {} with model: {}", from, model);

        String prompt = buildReplyPrompt(from, subject, body);

        try {
            String reply = ollamaService.chat(model, prompt, null, null);

            log.info("Successfully generated reply ({} characters)", reply.length());
            return cleanupReply(reply);

        } catch (Exception e) {
            log.error("Failed to generate reply", e);
            throw new RuntimeException("Failed to generate email reply", e);
        }
    }

    /**
     * Build prompt for reply generation
     */
    private String buildReplyPrompt(String from, String subject, String body) {
        return String.format("""
            Du bist ein professioneller Email-Assistent. Erstelle eine höfliche, professionelle Antwort auf folgende Email.

            **Wichtige Regeln:**
            - Antworte auf Deutsch
            - Sei höflich und professionell
            - Halte die Antwort präzise und relevant
            - Beziehe dich auf den Inhalt der Email
            - Verwende eine passende Anrede und Grußformel
            - Keine Metakommentare oder Erklärungen, NUR die Antwort-Email

            ---

            **Von:** %s
            **Betreff:** %s

            **Nachricht:**
            %s

            ---

            **Deine Antwort:**
            """,
            from,
            subject,
            truncateBody(body, 2000) // Limit body to 2000 chars to avoid token limits
        );
    }

    /**
     * Truncate body if too long
     */
    private String truncateBody(String body, int maxLength) {
        if (body == null) {
            return "";
        }

        if (body.length() <= maxLength) {
            return body;
        }

        return body.substring(0, maxLength) + "\n\n[... Text gekürzt ...]";
    }

    /**
     * Cleanup generated reply (remove quotes, trim, etc.)
     */
    private String cleanupReply(String reply) {
        if (reply == null) {
            return "";
        }

        // Remove surrounding quotes if present
        reply = reply.trim();
        if (reply.startsWith("\"") && reply.endsWith("\"")) {
            reply = reply.substring(1, reply.length() - 1);
        }

        // Remove common AI artifacts
        reply = reply.replaceAll("^(Antwort:|Antworten:|Reply:)\\s*", "");
        reply = reply.replaceAll("^Hier ist (meine |die )?Antwort:?\\s*", "");

        return reply.trim();
    }

    /**
     * Generate a simple completion without email context (for Office documents)
     */
    public String generateSimpleCompletion(String prompt, String model) {
        if (model == null || model.isEmpty()) {
            model = FALLBACK_MODEL;
        }

        log.info("Generating simple completion with model: {}, promptLength={}", model, prompt.length());

        try {
            String result = ollamaService.chat(model, prompt, null, "office-doc-" + System.currentTimeMillis());
            return result.trim();
        } catch (Exception e) {
            log.error("Failed to generate completion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate document: " + e.getMessage(), e);
        }
    }
}
