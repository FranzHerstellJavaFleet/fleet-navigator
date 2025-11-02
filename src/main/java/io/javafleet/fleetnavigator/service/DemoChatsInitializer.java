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
        Chat briefChat = createChat("📝 Beispiel: Bewerbungsschreiben",
            "llama3.2", "Allgemeiner Assistent");

        addMessage(briefChat, "user",
            "Hilf mir beim Schreiben eines Bewerbungsschreibens für eine Stelle als Java Entwickler bei einer Softwarefirma.");

        addMessage(briefChat, "assistant",
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
        Chat fragenChat = createChat("💬 Beispiel: Fragen zur Wissenschaft",
            "llama3.2", "Lehrer");

        addMessage(fragenChat, "user",
            "Erkläre mir, wie Photosynthese funktioniert");

        addMessage(fragenChat, "assistant",
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

        // Demo 3: Übersetzen
        Chat translateChat = createChat("🌐 Beispiel: Übersetzung",
            "llama3.2", "Übersetzer");

        addMessage(translateChat, "user",
            "Übersetze ins Englische: Guten Tag, ich hätte gerne einen Tisch für zwei Personen.");

        addMessage(translateChat, "assistant",
            "**Übersetzung ins Englische:**\n\n" +
            "\"Good day, I would like a table for two people, please.\"\n\n" +
            "**Alternative (informeller):**\n" +
            "\"Hello, I'd like a table for two, please.\"\n\n" +
            "**Im Restaurant gebräuchlich:**\n" +
            "\"Hi, table for two, please.\"\n\n" +
            "💡 **Kultureller Hinweis:** Im Englischen sagt man oft einfach nur die Personenzahl " +
            "ohne \"Personen\" zu erwähnen: \"Table for two\" statt \"Table for two people\".");

        // Demo 4: Programmieren
        Chat codeChat = createChat("💻 Beispiel: Python Code",
            "llama3.2", "Programmier-Experte");

        addMessage(codeChat, "user",
            "Schreibe mir ein Python-Skript, das alle Dateien in einem Ordner nach Datum sortiert");

        addMessage(codeChat, "assistant",
            "Hier ist ein Python-Skript, das Dateien nach Änderungsdatum sortiert:\n\n" +
            "```python\nimport os\nfrom pathlib import Path\nfrom datetime import datetime\n\n" +
            "def sort_files_by_date(directory):\n" +
            "    \"\"\"Sortiert Dateien in einem Ordner nach Änderungsdatum\"\"\"\n" +
            "    \n" +
            "    # Pfad zum Ordner\n" +
            "    path = Path(directory)\n" +
            "    \n" +
            "    # Alle Dateien mit Änderungszeit\n" +
            "    files = []\n" +
            "    for file in path.iterdir():\n" +
            "        if file.is_file():\n" +
            "            mtime = file.stat().st_mtime\n" +
            "            files.append((file.name, mtime))\n" +
            "    \n" +
            "    # Nach Datum sortieren (neueste zuerst)\n" +
            "    files.sort(key=lambda x: x[1], reverse=True)\n" +
            "    \n" +
            "    # Ausgabe\n" +
            "    print(f\"📁 Dateien in '{directory}' (nach Datum):\")\n" +
            "    for name, mtime in files:\n" +
            "        date = datetime.fromtimestamp(mtime)\n" +
            "        print(f\"  {date.strftime('%Y-%m-%d %H:%M')} - {name}\")\n\n" +
            "# Verwendung\n" +
            "sort_files_by_date('.')\n" +
            "```\n\n" +
            "**Was macht der Code?**\n" +
            "1. Liest alle Dateien aus dem angegebenen Ordner\n" +
            "2. Ermittelt das Änderungsdatum jeder Datei\n" +
            "3. Sortiert die Liste nach Datum (neueste zuerst)\n" +
            "4. Gibt die Dateien mit formatiertem Datum aus\n\n" +
            "**Verwendung:**\n" +
            "- `sort_files_by_date('.')` - aktueller Ordner\n" +
            "- `sort_files_by_date('/pfad/zum/ordner')` - spezifischer Pfad");

        log.info("✅ Created {} German demo chats", 4);
    }

    private void createEnglishDemoChats() {
        // Demo 1: Letter writing
        Chat letterChat = createChat("📝 Example: Cover Letter",
            "llama3.2", "General Assistant");

        addMessage(letterChat, "user",
            "Help me write a cover letter for a Java Developer position at a software company.");

        addMessage(letterChat, "assistant",
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
        Chat questionsChat = createChat("💬 Example: Science Questions",
            "llama3.2", "Teacher");

        addMessage(questionsChat, "user",
            "Explain how photosynthesis works");

        addMessage(questionsChat, "assistant",
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

        // Demo 3: Translation
        Chat translateChat = createChat("🌐 Example: Translation",
            "llama3.2", "Translator");

        addMessage(translateChat, "user",
            "Translate to German: Good morning, I would like a table for two people.");

        addMessage(translateChat, "assistant",
            "**Translation to German:**\n\n" +
            "\"Guten Morgen, ich hätte gerne einen Tisch für zwei Personen.\"\n\n" +
            "**Alternative (more formal):**\n" +
            "\"Guten Morgen, ich möchte einen Tisch für zwei Personen reservieren.\"\n\n" +
            "**Commonly used in restaurants:**\n" +
            "\"Hallo, einen Tisch für zwei bitte.\"\n\n" +
            "💡 **Cultural Note:** In German restaurants, it's polite to use \"hätte gerne\" " +
            "(would like) which is a bit more formal than just \"möchte\" (want).");

        // Demo 4: Programming
        Chat codeChat = createChat("💻 Example: Python Code",
            "llama3.2", "Programming Expert");

        addMessage(codeChat, "user",
            "Write me a Python script that sorts all files in a folder by date");

        addMessage(codeChat, "assistant",
            "Here's a Python script that sorts files by modification date:\n\n" +
            "```python\nimport os\nfrom pathlib import Path\nfrom datetime import datetime\n\n" +
            "def sort_files_by_date(directory):\n" +
            "    \"\"\"Sorts files in a folder by modification date\"\"\"\n" +
            "    \n" +
            "    # Path to folder\n" +
            "    path = Path(directory)\n" +
            "    \n" +
            "    # All files with modification time\n" +
            "    files = []\n" +
            "    for file in path.iterdir():\n" +
            "        if file.is_file():\n" +
            "            mtime = file.stat().st_mtime\n" +
            "            files.append((file.name, mtime))\n" +
            "    \n" +
            "    # Sort by date (newest first)\n" +
            "    files.sort(key=lambda x: x[1], reverse=True)\n" +
            "    \n" +
            "    # Output\n" +
            "    print(f\"📁 Files in '{directory}' (by date):\")\n" +
            "    for name, mtime in files:\n" +
            "        date = datetime.fromtimestamp(mtime)\n" +
            "        print(f\"  {date.strftime('%Y-%m-%d %H:%M')} - {name}\")\n\n" +
            "# Usage\n" +
            "sort_files_by_date('.')\n" +
            "```\n\n" +
            "**What does the code do?**\n" +
            "1. Reads all files from the specified folder\n" +
            "2. Gets the modification date of each file\n" +
            "3. Sorts the list by date (newest first)\n" +
            "4. Displays files with formatted date\n\n" +
            "**Usage:**\n" +
            "- `sort_files_by_date('.')` - current folder\n" +
            "- `sort_files_by_date('/path/to/folder')` - specific path");

        log.info("✅ Created {} English demo chats", 4);
    }

    private Chat createChat(String title, String modelName, String systemPrompt) {
        Chat chat = new Chat();
        chat.setTitle(title);
        chat.setModelName(modelName);
        chat.setSystemPrompt(systemPrompt);
        chat.setCreatedAt(LocalDateTime.now());
        return chatRepository.save(chat);
    }

    private void addMessage(Chat chat, String role, String content) {
        Message message = new Message();
        message.setChat(chat);
        message.setRole(role);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now().plusSeconds(1)); // Slight delay for realistic timing
        messageRepository.save(message);
    }
}
