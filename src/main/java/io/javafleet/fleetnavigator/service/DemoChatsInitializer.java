package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.model.Chat;
import io.javafleet.fleetnavigator.model.Message;
import io.javafleet.fleetnavigator.repository.ChatRepository;
import io.javafleet.fleetnavigator.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Erstellt Demo-Chats beim ersten Start
 * Zeigt Nutzern Beispiele für verschiedene Anwendungsfälle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemoChatsInitializer {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(3) // Nach SystemHealthCheckService (1) und DefaultDataInitializer (2)
    @Transactional
    public void initializeDemoChats() {
        // Nur beim allerersten Start (keine Chats vorhanden)
        if (chatRepository.count() > 0) {
            log.info("💬 Demo-Chats already exist, skipping initialization");
            return;
        }

        Locale systemLocale = Locale.getDefault();
        boolean isGerman = systemLocale.getLanguage().equals("de");

        log.info("💬 Creating demo chats for new users...");

        if (isGerman) {
            createGermanDemoChats();
        } else {
            createEnglishDemoChats();
        }

        log.info("✅ Demo chats initialized");
    }

    private void createGermanDemoChats() {
        // Demo 1: Brief schreiben
        Chat briefChat = createChat("📝 Beispiel: Bewerbungsschreiben", "llama3.2");

        addMessage(briefChat, Message.MessageRole.USER,
            "Hilf mir beim Schreiben eines Bewerbungsschreibens für eine Stelle als Java Entwickler bei einer Softwarefirma.");

        addMessage(briefChat, Message.MessageRole.ASSISTANT,
            "Gerne helfe ich dir bei deinem Bewerbungsschreiben! Hier ist ein professioneller Entwurf:\n\n" +
            "**Betreff: Bewerbung als Java-Entwickler**\n\n" +
            "Sehr geehrte Damen und Herren,\n\n" +
            "mit großem Interesse habe ich Ihre Stellenausschreibung für einen Java-Entwickler gelesen. " +
            "Die Kombination aus innovativen Projekten und modernen Technologien entspricht genau meinen beruflichen Zielen.\n\n" +
            "**Meine Qualifikationen:**\n" +
            "- 3+ Jahre Erfahrung in Java-Entwicklung\n" +
            "- Expertise in Spring Boot und Microservices\n" +
            "- Kenntnisse in Docker, Kubernetes und CI/CD\n\n" +
            "Ich freue mich auf ein persönliches Gespräch!\n\n" +
            "Mit freundlichen Grüßen,\n" +
            "[Dein Name]\n\n" +
            "💡 **Tipp:** Passe den Brief an deine eigene Erfahrung und die konkrete Stelle an!");

        // Demo 2: Fragen stellen
        Chat fragenChat = createChat("💬 Beispiel: Fragen zur Wissenschaft", "llama3.2");

        addMessage(fragenChat, Message.MessageRole.USER,
            "Erkläre mir, wie Photosynthese funktioniert");

        addMessage(fragenChat, Message.MessageRole.ASSISTANT,
            "🌿 **Photosynthese - einfach erklärt:**\n\n" +
            "Pflanzen sind wie kleine Fabriken, die ihr eigenes Essen herstellen!\n\n" +
            "**Was brauchen sie?**\n" +
            "1. ☀️ Sonnenlicht (Energie)\n" +
            "2. 💧 Wasser (aus dem Boden)\n" +
            "3. 🌫️ CO₂ (aus der Luft)\n\n" +
            "**Was passiert?**\n" +
            "Die grünen Blätter fangen das Sonnenlicht ein. Mit dieser Energie verwandeln sie " +
            "Wasser und CO₂ in Zucker (Nahrung) und Sauerstoff.\n\n" +
            "**Formel:** 6 CO₂ + 6 H₂O + Licht → C₆H₁₂O₆ + 6 O₂\n\n" +
            "**Warum ist das wichtig?**\n" +
            "- Pflanzen produzieren den Sauerstoff, den wir atmen\n" +
            "- Sie sind die Basis der Nahrungskette\n" +
            "- Sie helfen, CO₂ aus der Atmosphäre zu entfernen");

        log.info("✅ Created {} German demo chats", 2);
    }

    private void createEnglishDemoChats() {
        // Demo 1: Letter writing
        Chat letterChat = createChat("📝 Example: Cover Letter", "llama3.2");

        addMessage(letterChat, Message.MessageRole.USER,
            "Help me write a cover letter for a Java Developer position at a software company.");

        addMessage(letterChat, Message.MessageRole.ASSISTANT,
            "I'd be happy to help you with your cover letter! Here's a professional draft:\n\n" +
            "**Subject: Application for Java Developer Position**\n\n" +
            "Dear Hiring Manager,\n\n" +
            "I am writing to express my strong interest in the Java Developer position. " +
            "The combination of innovative projects and modern technologies aligns perfectly with my career goals.\n\n" +
            "**My Qualifications:**\n" +
            "- 3+ years of Java development experience\n" +
            "- Expertise in Spring Boot and Microservices\n" +
            "- Knowledge of Docker, Kubernetes, and CI/CD\n\n" +
            "I look forward to discussing this opportunity with you!\n\n" +
            "Best regards,\n" +
            "[Your Name]\n\n" +
            "💡 **Tip:** Customize this letter to match your experience and the specific position!");

        // Demo 2: Questions
        Chat questionsChat = createChat("💬 Example: Science Questions", "llama3.2");

        addMessage(questionsChat, Message.MessageRole.USER,
            "Explain how photosynthesis works");

        addMessage(questionsChat, Message.MessageRole.ASSISTANT,
            "🌿 **Photosynthesis - Simply Explained:**\n\n" +
            "Plants are like tiny factories that make their own food!\n\n" +
            "**What do they need?**\n" +
            "1. ☀️ Sunlight (energy)\n" +
            "2. 💧 Water (from the soil)\n" +
            "3. 🌫️ CO₂ (from the air)\n\n" +
            "**What happens?**\n" +
            "Green leaves capture sunlight. Using this energy, they transform " +
            "water and CO₂ into sugar (food) and oxygen.\n\n" +
            "**Formula:** 6 CO₂ + 6 H₂O + Light → C₆H₁₂O₆ + 6 O₂\n\n" +
            "**Why is this important?**\n" +
            "- Plants produce the oxygen we breathe\n" +
            "- They form the base of the food chain\n" +
            "- They help remove CO₂ from the atmosphere");

        log.info("✅ Created {} English demo chats", 2);
    }

    private Chat createChat(String title, String model) {
        Chat chat = new Chat();
        chat.setTitle(title);
        chat.setModel(model);
        chat.setCreatedAt(LocalDateTime.now());
        return chatRepository.save(chat);
    }

    private void addMessage(Chat chat, Message.MessageRole role, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageRepository.save(message);
    }
}
