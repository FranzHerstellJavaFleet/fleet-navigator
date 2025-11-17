package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Document Agent Service - Generates documents using AI and opens them in LibreOffice
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentAgentService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentSettingsService agentSettingsService;
    private final PersonalInfoService personalInfoService;

    // DEPRECATED: Ollama support removed, this is kept for compatibility
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /**
     * Generate a document from a prompt using configured model
     *
     * @param prompt The user's request (e.g., "Create a cancellation letter...")
     * @return Path to the generated document
     */
    public Path generateDocument(String prompt) throws IOException {
        // Get configured model and text editor from settings
        String modelName;
        String textEditor;
        try {
            var settings = agentSettingsService.getDocumentAgentSettings();
            modelName = settings.getModel();
            textEditor = settings.getTextEditor();

            if (modelName == null || modelName.trim().isEmpty()) {
                modelName = "llama3.1:8b"; // Fallback to default
                log.warn("No model configured in settings, using default: {}", modelName);
            }
            if (textEditor == null || textEditor.trim().isEmpty()) {
                textEditor = "libreoffice"; // Fallback to default
                log.warn("No text editor configured in settings, using default: {}", textEditor);
            }
        } catch (Exception e) {
            modelName = "llama3.1:8b"; // Fallback to default
            textEditor = "libreoffice"; // Fallback to default
            log.error("Error reading document agent settings, using defaults: model={}, editor={}", modelName, textEditor, e);
        }

        log.info("Generating document with configured model: {}, editor: {}, prompt: {}", modelName, textEditor, prompt);

        // Step 1: Send prompt to Ollama and get the generated text
        String generatedText = callOllama(prompt, modelName);

        // Step 2: Create HTML document with the generated text
        Path documentPath = createODTDocument(generatedText);

        // Step 3: Open the document in configured text editor
        openInTextEditor(documentPath, textEditor);

        return documentPath;
    }

    /**
     * Generate a document from a prompt with specific model (for backwards compatibility)
     *
     * @param prompt     The user's request (e.g., "Create a cancellation letter...")
     * @param modelName  The Ollama model to use
     * @return Path to the generated document
     */
    public Path generateDocument(String prompt, String modelName) throws IOException {
        log.info("Generating document with model: {}, prompt: {}", modelName, prompt);

        // Step 1: Send prompt to Ollama and get the generated text
        String generatedText = callOllama(prompt, modelName);

        // Step 2: Create HTML document with the generated text
        Path documentPath = createODTDocument(generatedText);

        // Step 3: Open the document in LibreOffice
        openInLibreOffice(documentPath);

        return documentPath;
    }

    /**
     * Call Ollama API to generate text
     */
    private String callOllama(String prompt, String modelName) throws IOException {
        // Get personal info and enhance prompt
        String enhancedPrompt = enhancePromptWithPersonalInfo(prompt);

        // Enhanced system prompt for document generation
        String systemPrompt = "Du bist ein professioneller Assistent für Geschäftsbriefe und Dokumente. " +
                "Erstelle formale, präzise und gut strukturierte Dokumente auf Deutsch. " +
                "Verwende die korrekte Briefform mit Absender, Empfänger, Betreff, Anrede, Text und Grußformel. " +
                "Die persönlichen Daten des Absenders werden im Prompt bereitgestellt - verwende sie direkt. " +
                "Nutze für unbekannte Empfängerdaten Platzhalter wie [Empfängername], [Empfängeradresse] etc. " +
                "WICHTIG: Schreibe NUR den reinen Brief ohne Einleitung wie 'Ich kann Ihnen gerne...' " +
                "und ohne abschließende Hinweise oder Erklärungen. " +
                "Beginne direkt mit dem Absender und ende mit der Grußformel.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", enhancedPrompt);
        requestBody.put("system", systemPrompt);
        requestBody.put("stream", false); // No streaming for document generation

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(ollamaBaseUrl + "/api/generate")
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama API call failed: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            return jsonResponse.get("response").asText();
        }
    }

    /**
     * Create an ODT document with the generated text
     */
    private Path createODTDocument(String content) throws IOException {
        // Create documents directory in user home
        Path documentsDir = Paths.get(System.getProperty("user.home"), "FleetNavigator", "Documents");
        Files.createDirectories(documentsDir);

        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "Document_" + timestamp + ".odt";
        Path documentPath = documentsDir.resolve(filename);

        // For now, create a simple text file
        // TODO: In the future, use ODFDOM or similar library to create proper ODT
        // For MVP, we'll create a simple text document and let LibreOffice convert it

        // Create a simple HTML-like structure that LibreOffice can read
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset=\"UTF-8\"></head>\n" +
                "<body style=\"font-family: Arial; font-size: 12pt; line-height: 1.6;\">\n" +
                content.replace("\n", "<br>\n") +
                "\n</body>\n" +
                "</html>";

        // Save as HTML first (LibreOffice will open it)
        Path htmlPath = documentsDir.resolve("Document_" + timestamp + ".html");
        Files.writeString(htmlPath, htmlContent);

        log.info("Document created at: {}", htmlPath);
        return htmlPath;
    }

    /**
     * Enhance prompt with personal information
     */
    private String enhancePromptWithPersonalInfo(String originalPrompt) {
        try {
            return personalInfoService.getPersonalInfo()
                    .map(info -> {
                        StringBuilder enhanced = new StringBuilder(originalPrompt);
                        enhanced.append("\n\nMeine persönlichen Daten für den Absender:\n");

                        if (info.getFullName() != null && !info.getFullName().trim().isEmpty()) {
                            enhanced.append("Name: ").append(info.getFullName()).append("\n");
                        }

                        if (info.getFullAddress() != null && !info.getFullAddress().trim().isEmpty()) {
                            enhanced.append("Adresse: ").append(info.getFullAddress()).append("\n");
                        }

                        if (info.getPhone() != null && !info.getPhone().trim().isEmpty()) {
                            enhanced.append("Telefon: ").append(info.getPhone()).append("\n");
                        }

                        if (info.getEmail() != null && !info.getEmail().trim().isEmpty()) {
                            enhanced.append("E-Mail: ").append(info.getEmail()).append("\n");
                        }

                        return enhanced.toString();
                    })
                    .orElse(originalPrompt); // Return original if no personal info found
        } catch (Exception e) {
            log.warn("Could not load personal info, using prompt as-is", e);
            return originalPrompt;
        }
    }

    /**
     * Open the document in configured text editor
     */
    private void openInTextEditor(Path documentPath, String textEditor) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        // Normalize editor name
        textEditor = textEditor.toLowerCase().trim();

        if (os.contains("linux")) {
            switch (textEditor) {
                case "libreoffice":
                    processBuilder = new ProcessBuilder("libreoffice", "--writer", documentPath.toString());
                    break;
                case "onlyoffice":
                    processBuilder = new ProcessBuilder("onlyoffice-desktopeditors", documentPath.toString());
                    break;
                case "msword":
                case "word":
                    // Try WPS Office on Linux
                    processBuilder = new ProcessBuilder("wps", documentPath.toString());
                    break;
                case "abiword":
                    processBuilder = new ProcessBuilder("abiword", documentPath.toString());
                    break;
                case "gedit":
                    processBuilder = new ProcessBuilder("gedit", documentPath.toString());
                    break;
                default:
                    log.warn("Unknown text editor: {}, falling back to libreoffice", textEditor);
                    processBuilder = new ProcessBuilder("libreoffice", "--writer", documentPath.toString());
            }
        } else if (os.contains("windows")) {
            switch (textEditor) {
                case "libreoffice":
                    processBuilder = new ProcessBuilder("cmd", "/c", "start", "soffice", "-writer", documentPath.toString());
                    break;
                case "onlyoffice":
                    processBuilder = new ProcessBuilder("cmd", "/c", "start", "DesktopEditors", documentPath.toString());
                    break;
                case "msword":
                case "word":
                    processBuilder = new ProcessBuilder("cmd", "/c", "start", "winword", documentPath.toString());
                    break;
                case "notepad":
                    processBuilder = new ProcessBuilder("notepad", documentPath.toString());
                    break;
                default:
                    log.warn("Unknown text editor: {}, using default Windows handler", textEditor);
                    processBuilder = new ProcessBuilder("cmd", "/c", "start", documentPath.toString());
            }
        } else if (os.contains("mac")) {
            switch (textEditor) {
                case "libreoffice":
                    processBuilder = new ProcessBuilder("open", "-a", "LibreOffice", documentPath.toString());
                    break;
                case "onlyoffice":
                    processBuilder = new ProcessBuilder("open", "-a", "ONLYOFFICE", documentPath.toString());
                    break;
                case "msword":
                case "word":
                    processBuilder = new ProcessBuilder("open", "-a", "Microsoft Word", documentPath.toString());
                    break;
                case "textedit":
                    processBuilder = new ProcessBuilder("open", "-a", "TextEdit", documentPath.toString());
                    break;
                default:
                    log.warn("Unknown text editor: {}, using default macOS handler", textEditor);
                    processBuilder = new ProcessBuilder("open", documentPath.toString());
            }
        } else {
            throw new IOException("Unsupported operating system: " + os);
        }

        Process process = processBuilder.start();
        log.info("{} started with document: {}", textEditor, documentPath);
    }

    /**
     * Open the document in LibreOffice Writer (backwards compatibility)
     * @deprecated Use openInTextEditor() instead
     */
    @Deprecated
    private void openInLibreOffice(Path documentPath) throws IOException {
        openInTextEditor(documentPath, "libreoffice");
    }
}
