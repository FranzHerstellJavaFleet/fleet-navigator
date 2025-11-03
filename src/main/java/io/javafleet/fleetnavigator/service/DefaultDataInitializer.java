package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.model.LetterTemplate;
import io.javafleet.fleetnavigator.model.PersonalInfo;
import io.javafleet.fleetnavigator.repository.LetterTemplateRepository;
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
 * - Platzhalter für persönliche Daten (Max Mustermann)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultDataInitializer {

    private final LetterTemplateRepository letterTemplateRepository;
    private final PersonalInfoService personalInfoService;

    @EventListener(ApplicationReadyEvent.class)
    @Order(2) // Nach SystemHealthCheckService (Order 1)
    @Transactional
    public void initializeDefaultData() {
        boolean isGerman = detectGermanLocale();
        String detectedLanguage = isGerman ? "German (Deutsch)" : "English";

        log.info("🌍 System locale detected: {} - Initializing in {}", Locale.getDefault(), detectedLanguage);

        if (letterTemplateRepository.count() == 0) {
            log.info("📝 Initializing default letter templates...");
            initializeLetterTemplates(isGerman);
            log.info("✅ Letter templates initialized");
        } else {
            log.info("📝 Letter templates already exist, skipping initialization");
        }

        if (!personalInfoService.hasPersonalInfo()) {
            log.info("👤 Initializing placeholder personal info (Max Mustermann)...");
            initializePlaceholderPersonalInfo(isGerman);
            log.info("✅ Placeholder personal info initialized");
        } else {
            log.info("👤 Personal info already exists, skipping initialization");
        }
    }

    /**
     * Detects German locale from multiple sources (more reliable in native image)
     */
    private boolean detectGermanLocale() {
        // 1. Check Java Locale
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale.getLanguage().equals("de")) {
            log.debug("German detected via Locale.getDefault(): {}", defaultLocale);
            return true;
        }

        // 2. Check environment variables (works better in native image)
        String lang = System.getenv("LANG");
        if (lang != null && lang.toLowerCase().startsWith("de")) {
            log.debug("German detected via LANG environment variable: {}", lang);
            return true;
        }

        String language = System.getenv("LANGUAGE");
        if (language != null && language.toLowerCase().startsWith("de")) {
            log.debug("German detected via LANGUAGE environment variable: {}", language);
            return true;
        }

        // 3. Check user.language system property
        String userLanguage = System.getProperty("user.language");
        if (userLanguage != null && userLanguage.equals("de")) {
            log.debug("German detected via user.language property: {}", userLanguage);
            return true;
        }

        log.debug("No German locale detected, defaulting to English");
        return false;
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
        LetterTemplate bewerbung = new LetterTemplate();
        bewerbung.setName("Bewerbungsschreiben");
        bewerbung.setPrompt(
            "Betreff: Bewerbung als [Position]\n\n" +
            "Sehr geehrte Damen und Herren,\n\n" +
            "mit großem Interesse habe ich Ihre Stellenausschreibung gelesen.\n\n" +
            "[Beschreibe hier deine Qualifikationen und Motivation]\n\n" +
            "Über die Einladung zu einem persönlichen Gespräch freue ich mich sehr.\n\n" +
            "Mit freundlichen Grüßen"
        );
        letterTemplateRepository.save(bewerbung);

        // Kündigungsschreiben
        LetterTemplate kuendigung = new LetterTemplate();
        kuendigung.setName("Kündigungsschreiben");
        kuendigung.setPrompt(
            "Betreff: Kündigung meines [Vertrags/Mitgliedschaft]\n\n" +
            "Sehr geehrte Damen und Herren,\n\n" +
            "hiermit kündige ich meinen [Vertrag/Mitgliedschaft] zum [Datum].\n\n" +
            "Bitte bestätigen Sie mir den Erhalt dieser Kündigung schriftlich.\n\n" +
            "Mit freundlichen Grüßen"
        );
        letterTemplateRepository.save(kuendigung);

        // Geschäftsbrief
        LetterTemplate geschaeft = new LetterTemplate();
        geschaeft.setName("Geschäftsbrief");
        geschaeft.setPrompt(
            "Betreff: [Betreff]\n\n" +
            "Sehr geehrte Damen und Herren,\n\n" +
            "[Ihr Anliegen]\n\n" +
            "Für Rückfragen stehe ich Ihnen gerne zur Verfügung.\n\n" +
            "Mit freundlichen Grüßen"
        );
        letterTemplateRepository.save(geschaeft);

        log.info("✅ Created {} German letter templates", 3);
    }

    private void initializeEnglishLetterTemplates() {
        // Cover Letter
        LetterTemplate coverLetter = new LetterTemplate();
        coverLetter.setName("Cover Letter");
        coverLetter.setPrompt(
            "Subject: Application for [Position]\n\n" +
            "Dear Hiring Manager,\n\n" +
            "I am writing to express my interest in the advertised position.\n\n" +
            "[Describe your qualifications and motivation here]\n\n" +
            "I look forward to hearing from you.\n\n" +
            "Best regards"
        );
        letterTemplateRepository.save(coverLetter);

        // Resignation Letter
        LetterTemplate resignation = new LetterTemplate();
        resignation.setName("Resignation Letter");
        resignation.setPrompt(
            "Subject: Resignation\n\n" +
            "Dear [Manager Name],\n\n" +
            "I am writing to inform you of my resignation from [Position], effective [Date].\n\n" +
            "Thank you for the opportunities I have had during my time with the company.\n\n" +
            "Sincerely"
        );
        letterTemplateRepository.save(resignation);

        // Business Letter
        LetterTemplate business = new LetterTemplate();
        business.setName("Business Letter");
        business.setPrompt(
            "Subject: [Subject]\n\n" +
            "Dear Sir/Madam,\n\n" +
            "[Your message]\n\n" +
            "Please feel free to contact me if you have any questions.\n\n" +
            "Best regards"
        );
        letterTemplateRepository.save(business);

        log.info("✅ Created {} English letter templates", 3);
    }

    private void initializePlaceholderPersonalInfo(boolean german) {
        PersonalInfo placeholder = new PersonalInfo();

        if (german) {
            placeholder.setFirstName("Max");
            placeholder.setLastName("Mustermann");
            placeholder.setStreet("Musterweg");
            placeholder.setHouseNumber("1");
            placeholder.setPostalCode("12345");
            placeholder.setCity("Musterstadt");
            placeholder.setCountry("Deutschland");
            placeholder.setPhone("+49 123 456789");
            placeholder.setMobile("+49 170 1234567");
            placeholder.setEmail("max.mustermann@beispiel.de");
        } else {
            placeholder.setFirstName("John");
            placeholder.setLastName("Doe");
            placeholder.setStreet("Example Street");
            placeholder.setHouseNumber("1");
            placeholder.setPostalCode("12345");
            placeholder.setCity("Sample City");
            placeholder.setCountry("USA");
            placeholder.setPhone("+1 555 123-4567");
            placeholder.setMobile("+1 555 987-6543");
            placeholder.setEmail("john.doe@example.com");
        }

        personalInfoService.savePersonalInfo(placeholder);
        log.info("✅ Placeholder personal info created: {} {} from {}",
            placeholder.getFirstName(), placeholder.getLastName(), placeholder.getCity());
    }
}
