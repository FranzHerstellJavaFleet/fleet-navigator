package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.model.LetterTemplate;
import io.javafleet.fleetnavigator.model.SystemPrompt;
import io.javafleet.fleetnavigator.repository.LetterTemplateRepository;
import io.javafleet.fleetnavigator.repository.SystemPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Initialisiert Default-Daten beim ersten Start
 * - Brief-Vorlagen (Deutsch & Englisch)
 * - System-Prompts für verschiedene Anwendungsfälle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultDataInitializer {

    private final LetterTemplateRepository letterTemplateRepository;
    private final SystemPromptRepository systemPromptRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(2) // Nach SystemHealthCheckService (Order 1)
    @Transactional
    public void initializeDefaultData() {
        Locale systemLocale = Locale.getDefault();
        boolean isGerman = systemLocale.getLanguage().equals("de");

        log.info("🌍 System locale detected: {} ({})", systemLocale, isGerman ? "German" : "English");

        if (letterTemplateRepository.count() == 0) {
            log.info("📝 Initializing default letter templates...");
            initializeLetterTemplates(isGerman);
            log.info("✅ Letter templates initialized");
        } else {
            log.info("📝 Letter templates already exist, skipping initialization");
        }

        if (systemPromptRepository.count() == 0) {
            log.info("🤖 Initializing default system prompts...");
            initializeSystemPrompts(isGerman);
            log.info("✅ System prompts initialized");
        } else {
            log.info("🤖 System prompts already exist, skipping initialization");
        }
    }

    private void initializeLetterTemplates(boolean german) {
        if (german) {
            initializeGermanLetterTemplates();
        } else {
            initializeEnglishLetterTemplates();
        }
    }

    private void initializeGermanLetterTemplates() {
        // Bewerbungsschreiben
        LetterTemplate application = new LetterTemplate();
        application.setName("Bewerbungsschreiben");
        application.setCategory("Karriere");
        application.setDescription("Professionelles Bewerbungsschreiben erstellen");
        application.setPrompt("Erstelle ein professionelles Bewerbungsschreiben für eine Stelle als [Position] bei [Firma]. " +
                "Betone meine Erfahrung in [Bereich] und meine Stärken: [Stärken auflisten]. " +
                "Verwende einen formellen aber persönlichen Ton.");
        letterTemplateRepository.save(application);

        // Geschäftsbrief
        LetterTemplate business = new LetterTemplate();
        business.setName("Geschäftsbrief");
        business.setCategory("Geschäftlich");
        business.setDescription("Formeller Geschäftsbrief");
        business.setPrompt("Verfasse einen formellen Geschäftsbrief an [Empfänger/Firma] bezüglich [Thema]. " +
                "Der Ton sollte professionell und höflich sein. Strukturiere den Brief mit Anrede, Hauptteil und Schlussformel.");
        letterTemplateRepository.save(business);

        // Kündigungsschreiben
        LetterTemplate termination = new LetterTemplate();
        termination.setName("Kündigungsschreiben");
        termination.setCategory("Arbeitsrecht");
        termination.setDescription("Kündigung des Arbeitsverhältnisses");
        termination.setPrompt("Erstelle ein korrektes Kündigungsschreiben für mein Arbeitsverhältnis bei [Firma]. " +
                "Kündigungsfrist: [z.B. 3 Monate zum Quartalsende]. Kündigungsgrund: [optional]. " +
                "Halte dich an die rechtlichen Anforderungen und verwende einen sachlichen, professionellen Ton.");
        letterTemplateRepository.save(termination);

        // Empfehlungsschreiben
        LetterTemplate recommendation = new LetterTemplate();
        recommendation.setName("Empfehlungsschreiben");
        recommendation.setCategory("Karriere");
        recommendation.setDescription("Empfehlung für Person oder Produkt");
        recommendation.setPrompt("Verfasse ein Empfehlungsschreiben für [Name/Produkt]. " +
                "Hebe folgende Qualitäten hervor: [Stärken/Eigenschaften]. " +
                "Zielgruppe: [z.B. potenzieller Arbeitgeber, Universität, Kunde].");
        letterTemplateRepository.save(recommendation);

        // Beschwerdebrief
        LetterTemplate complaint = new LetterTemplate();
        complaint.setName("Beschwerdebrief");
        complaint.setCategory("Verbraucher");
        complaint.setDescription("Beschwerde über Produkt oder Dienstleistung");
        complaint.setPrompt("Schreibe einen Beschwerdebrief an [Firma] bezüglich [Problem/Mangel]. " +
                "Schildere den Sachverhalt objektiv, nenne Fakten (Datum, Rechnungsnummer, etc.) und " +
                "formuliere klar deine Erwartungen (Erstattung, Umtausch, Entschädigung). " +
                "Ton: bestimmt aber höflich.");
        letterTemplateRepository.save(complaint);

        // Dankesbrief
        LetterTemplate thankYou = new LetterTemplate();
        thankYou.setName("Dankesbrief");
        thankYou.setCategory("Persönlich");
        thankYou.setDescription("Dank für Unterstützung oder Geschenk");
        thankYou.setPrompt("Verfasse einen herzlichen Dankesbrief an [Name/Organisation] für [Anlass/Geschenk/Hilfe]. " +
                "Drücke aufrichtige Dankbarkeit aus und gehe persönlich auf die Bedeutung ein.");
        letterTemplateRepository.save(thankYou);

        // Einladung
        LetterTemplate invitation = new LetterTemplate();
        invitation.setName("Einladung");
        invitation.setCategory("Persönlich");
        invitation.setDescription("Einladung zu Veranstaltung");
        invitation.setPrompt("Erstelle eine Einladung zu [Veranstaltung] am [Datum] um [Uhrzeit] in [Ort]. " +
                "Anlass: [z.B. Geburtstag, Hochzeit, Firmenfeier]. " +
                "Ton: [formell/informell]. Bitte um Rückmeldung bis [Datum].");
        letterTemplateRepository.save(invitation);

        // Motivationsschreiben
        LetterTemplate motivation = new LetterTemplate();
        motivation.setName("Motivationsschreiben");
        motivation.setCategory("Bildung");
        motivation.setDescription("Für Studium oder Stipendium");
        motivation.setPrompt("Verfasse ein überzeugendes Motivationsschreiben für [Studiengang/Stipendium] an [Universität/Organisation]. " +
                "Erkläre deine Motivation, relevante Erfahrungen und Ziele. Zeige Begeisterung und Engagement.");
        letterTemplateRepository.save(motivation);
    }

    private void initializeEnglishLetterTemplates() {
        // Cover Letter
        LetterTemplate application = new LetterTemplate();
        application.setName("Cover Letter");
        application.setCategory("Career");
        application.setDescription("Professional job application letter");
        application.setPrompt("Create a professional cover letter for a position as [Position] at [Company]. " +
                "Highlight my experience in [Field] and my strengths: [List strengths]. " +
                "Use a formal but personal tone.");
        letterTemplateRepository.save(application);

        // Business Letter
        LetterTemplate business = new LetterTemplate();
        business.setName("Business Letter");
        business.setCategory("Business");
        business.setDescription("Formal business correspondence");
        business.setPrompt("Write a formal business letter to [Recipient/Company] regarding [Topic]. " +
                "The tone should be professional and polite. Structure the letter with greeting, body, and closing.");
        letterTemplateRepository.save(business);

        // Resignation Letter
        LetterTemplate termination = new LetterTemplate();
        termination.setName("Resignation Letter");
        termination.setCategory("Employment");
        termination.setDescription("Formal resignation from employment");
        termination.setPrompt("Create a proper resignation letter for my employment at [Company]. " +
                "Notice period: [e.g., 2 weeks, 1 month]. Reason (optional): [reason]. " +
                "Follow legal requirements and use a professional, courteous tone.");
        letterTemplateRepository.save(termination);

        // Recommendation Letter
        LetterTemplate recommendation = new LetterTemplate();
        recommendation.setName("Recommendation Letter");
        recommendation.setCategory("Career");
        recommendation.setDescription("Recommendation for person or product");
        recommendation.setPrompt("Write a recommendation letter for [Name/Product]. " +
                "Emphasize these qualities: [Strengths/Characteristics]. " +
                "Target audience: [e.g., potential employer, university, customer].");
        letterTemplateRepository.save(recommendation);

        // Complaint Letter
        LetterTemplate complaint = new LetterTemplate();
        complaint.setName("Complaint Letter");
        complaint.setCategory("Consumer");
        complaint.setDescription("Complaint about product or service");
        complaint.setPrompt("Write a complaint letter to [Company] regarding [Problem/Issue]. " +
                "Describe the situation objectively, include facts (date, invoice number, etc.) and " +
                "clearly state your expectations (refund, exchange, compensation). " +
                "Tone: firm but polite.");
        letterTemplateRepository.save(complaint);

        // Thank You Letter
        LetterTemplate thankYou = new LetterTemplate();
        thankYou.setName("Thank You Letter");
        thankYou.setCategory("Personal");
        thankYou.setDescription("Thanks for support or gift");
        thankYou.setPrompt("Write a heartfelt thank you letter to [Name/Organization] for [Occasion/Gift/Help]. " +
                "Express sincere gratitude and personally address the significance.");
        letterTemplateRepository.save(thankYou);

        // Invitation
        LetterTemplate invitation = new LetterTemplate();
        invitation.setName("Invitation");
        invitation.setCategory("Personal");
        invitation.setDescription("Invitation to event");
        invitation.setPrompt("Create an invitation to [Event] on [Date] at [Time] in [Location]. " +
                "Occasion: [e.g., birthday, wedding, company celebration]. " +
                "Tone: [formal/informal]. Please RSVP by [Date].");
        letterTemplateRepository.save(invitation);

        // Motivation Letter
        LetterTemplate motivation = new LetterTemplate();
        motivation.setName("Motivation Letter");
        motivation.setCategory("Education");
        motivation.setDescription("For study program or scholarship");
        motivation.setPrompt("Write a compelling motivation letter for [Program/Scholarship] at [University/Organization]. " +
                "Explain your motivation, relevant experiences, and goals. Show enthusiasm and commitment.");
        letterTemplateRepository.save(motivation);
    }

    private void initializeSystemPrompts(boolean german) {
        if (german) {
            initializeGermanSystemPrompts();
        } else {
            initializeEnglishSystemPrompts();
        }
    }

    private void initializeGermanSystemPrompts() {
        // Allgemeiner Assistent
        SystemPrompt general = new SystemPrompt();
        general.setName("Allgemeiner Assistent");
        general.setDescription("Hilfreicher und freundlicher Assistent");
        general.setPrompt("Du bist ein hilfreicher, freundlicher und kompetenter Assistent. " +
                "Beantworte Fragen präzise und verständlich. Wenn du etwas nicht weißt, gib das ehrlich zu.");
        general.setActive(true);
        systemPromptRepository.save(general);

        // Professioneller Brief-Schreiber
        SystemPrompt letterWriter = new SystemPrompt();
        letterWriter.setName("Brief-Experte");
        letterWriter.setDescription("Spezialist für formelle und informelle Schreiben");
        letterWriter.setPrompt("Du bist ein Experte für das Verfassen von Briefen aller Art. " +
                "Du beherrschst den richtigen Ton für jede Situation - von formell-geschäftlich bis persönlich-herzlich. " +
                "Achte auf korrekte Anrede, Struktur und Schlussformeln nach deutschen Standards.");
        letterWriter.setActive(false);
        systemPromptRepository.save(letterWriter);

        // Code-Experte
        SystemPrompt coder = new SystemPrompt();
        coder.setName("Programmier-Experte");
        coder.setDescription("Hilfe bei Coding und Software-Entwicklung");
        coder.setPrompt("Du bist ein erfahrener Software-Entwickler mit Expertise in modernen Programmiersprachen und Best Practices. " +
                "Erkläre Code verständlich, gib praktische Beispiele und achte auf Code-Qualität, Lesbarkeit und Wartbarkeit.");
        coder.setActive(false);
        systemPromptRepository.save(coder);

        // Übersetzer
        SystemPrompt translator = new SystemPrompt();
        translator.setName("Übersetzer");
        translator.setDescription("Präzise Übersetzungen");
        translator.setPrompt("Du bist ein professioneller Übersetzer mit Expertise in mehreren Sprachen. " +
                "Übersetze präzise und berücksichtige kulturelle Nuancen. Erkläre bei Bedarf Redewendungen oder idiomatische Ausdrücke.");
        translator.setActive(false);
        systemPromptRepository.save(translator);

        // Lehrer/Erklärer
        SystemPrompt teacher = new SystemPrompt();
        teacher.setName("Lehrer");
        teacher.setDescription("Erklärt komplexe Themen einfach");
        teacher.setPrompt("Du bist ein geduldiger und verständnisvoller Lehrer. " +
                "Erkläre komplexe Themen Schritt für Schritt mit anschaulichen Beispielen. " +
                "Passe deine Erklärungen dem Kenntnisstand des Lernenden an.");
        teacher.setActive(false);
        systemPromptRepository.save(teacher);
    }

    private void initializeEnglishSystemPrompts() {
        // General Assistant
        SystemPrompt general = new SystemPrompt();
        general.setName("General Assistant");
        general.setDescription("Helpful and friendly assistant");
        general.setPrompt("You are a helpful, friendly, and competent assistant. " +
                "Answer questions precisely and understandably. If you don't know something, admit it honestly.");
        general.setActive(true);
        systemPromptRepository.save(general);

        // Professional Letter Writer
        SystemPrompt letterWriter = new SystemPrompt();
        letterWriter.setName("Letter Expert");
        letterWriter.setDescription("Specialist for formal and informal writing");
        letterWriter.setPrompt("You are an expert in writing letters of all kinds. " +
                "You master the right tone for every situation - from formal business to personal and warm. " +
                "Pay attention to correct salutations, structure, and closings according to professional standards.");
        letterWriter.setActive(false);
        systemPromptRepository.save(letterWriter);

        // Code Expert
        SystemPrompt coder = new SystemPrompt();
        coder.setName("Programming Expert");
        coder.setDescription("Help with coding and software development");
        coder.setPrompt("You are an experienced software developer with expertise in modern programming languages and best practices. " +
                "Explain code understandably, provide practical examples, and focus on code quality, readability, and maintainability.");
        coder.setActive(false);
        systemPromptRepository.save(coder);

        // Translator
        SystemPrompt translator = new SystemPrompt();
        translator.setName("Translator");
        translator.setDescription("Precise translations");
        translator.setPrompt("You are a professional translator with expertise in multiple languages. " +
                "Translate precisely and consider cultural nuances. Explain idioms or idiomatic expressions when necessary.");
        translator.setActive(false);
        systemPromptRepository.save(translator);

        // Teacher/Explainer
        SystemPrompt teacher = new SystemPrompt();
        teacher.setName("Teacher");
        teacher.setDescription("Explains complex topics simply");
        teacher.setPrompt("You are a patient and understanding teacher. " +
                "Explain complex topics step by step with clear examples. " +
                "Adapt your explanations to the learner's level of knowledge.");
        teacher.setActive(false);
        systemPromptRepository.save(teacher);
    }
}
