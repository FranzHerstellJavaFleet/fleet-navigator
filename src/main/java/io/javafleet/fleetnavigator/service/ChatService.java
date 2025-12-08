package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.util.FleetUtils;
import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import io.javafleet.fleetnavigator.experts.repository.ExpertModeRepository;
import io.javafleet.fleetnavigator.experts.repository.ExpertRepository;
import io.javafleet.fleetnavigator.experts.runtime.ExpertRuntime;
import io.javafleet.fleetnavigator.experts.runtime.ExpertRuntimeFactory;
import io.javafleet.fleetnavigator.model.AppSettings;
import io.javafleet.fleetnavigator.model.Chat;
import io.javafleet.fleetnavigator.model.ChatDocument;
import io.javafleet.fleetnavigator.model.ContextFile;
import io.javafleet.fleetnavigator.model.GlobalStats;
import io.javafleet.fleetnavigator.model.Message;
import io.javafleet.fleetnavigator.model.Project;
import io.javafleet.fleetnavigator.model.Message.MessageRole;
import io.javafleet.fleetnavigator.repository.ChatDocumentRepository;
import io.javafleet.fleetnavigator.repository.ChatRepository;
import io.javafleet.fleetnavigator.repository.GlobalStatsRepository;
import io.javafleet.fleetnavigator.repository.MessageRepository;
import io.javafleet.fleetnavigator.websocket.FleetMateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for managing chats and messages
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ChatDocumentRepository chatDocumentRepository;
    private final GlobalStatsRepository globalStatsRepository;
    private final LLMProviderService llmProviderService; // Uses java-llama-cpp provider
    private final ModelSelectionService modelSelectionService;
    private final SettingsService settingsService;
    private final CodeGeneratorService codeGeneratorService;
    private final ZipService zipService;
    private final WebSearchService webSearchService;  // Web Search RAG
    private final ExpertRepository expertRepository;  // Expert System für searchDomains
    private final ExpertModeRepository expertModeRepository;  // Expert Modi für Keyword-Erkennung
    private final ExpertRuntimeFactory expertRuntimeFactory;  // Expert Runtime Factory
    private final DocumentGeneratorService documentGeneratorService;  // Briefe und PDFs generieren
    private final FleetMateService fleetMateService;  // Fleet-Mate Management
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Fleet-Mate WebSocket Handler (set via setter to avoid circular dependency)
    private FleetMateWebSocketHandler fleetMateWebSocketHandler;

    // Thread pool for handling streaming requests
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Pending document generation requests: sessionId -> PendingDocument
    // Used to track Fleet-Mate document generation and send file links back to frontend
    private final java.util.concurrent.ConcurrentHashMap<String, PendingDocument> pendingDocuments =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Pending document generation request
     */
    public static class PendingDocument {
        private final SseEmitter emitter;
        private final Long chatId;
        private final String expertName;
        private final long createdAt;

        public PendingDocument(SseEmitter emitter, Long chatId, String expertName) {
            this.emitter = emitter;
            this.chatId = chatId;
            this.expertName = expertName;
            this.createdAt = System.currentTimeMillis();
        }

        public SseEmitter getEmitter() { return emitter; }
        public Long getChatId() { return chatId; }
        public String getExpertName() { return expertName; }
        public long getCreatedAt() { return createdAt; }
    }

    /**
     * Set FleetMateWebSocketHandler (used to avoid circular dependency)
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setFleetMateWebSocketHandler(@org.springframework.context.annotation.Lazy FleetMateWebSocketHandler handler) {
        this.fleetMateWebSocketHandler = handler;
    }

    /**
     * Convert file metadata list to JSON string for storage
     */
    private String serializeFileMetadata(List<FileMetadata> fileMetadata) {
        if (fileMetadata == null || fileMetadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fileMetadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize file metadata: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create a new chat
     */
    @Transactional
    public ChatDTO createNewChat(NewChatRequest request) {
        Chat chat = new Chat();
        chat.setTitle(request.getTitle() != null ? request.getTitle() : "New Chat");
        chat.setModel(request.getModel() != null ? request.getModel() : "llama3.2:3b");
        chat.setExpertId(request.getExpertId());  // Kann null sein für normale Modelle

        chat = chatRepository.save(chat);
        log.info("Created new chat: {} with expertId: {}", chat.getId(), chat.getExpertId());

        return mapToChatDTO(chat);
    }

    /**
     * Send a message and get response from LLM (java-llama-cpp)
     */
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) throws IOException {
        // Generate request ID for tracking
        String requestId = UUID.randomUUID().toString();

        // Get or create chat
        Chat chat;
        if (request.getChatId() != null) {
            chat = chatRepository.findById(request.getChatId())
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + request.getChatId()));
        } else {
            // Create new chat
            chat = new Chat();
            chat.setTitle("New Chat");
            chat.setModel(request.getModel() != null ? request.getModel() : "llama3.2:3b");
            chat = chatRepository.save(chat);
        }

        // Load previous messages from database to maintain conversation context
        List<Message> previousMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId());

        // Build complete message (with chat history, project context and document context if provided)
        StringBuilder completeMessageBuilder = new StringBuilder();

        // Kontext-Zusammenfassung einfügen (falls vorhanden - nach Löschen von Nachrichten)
        if (chat.getContextSummary() != null && !chat.getContextSummary().isBlank()) {
            completeMessageBuilder.append("=== KONTEXT-ZUSAMMENFASSUNG (aus vorherigem Gespräch) ===\n");
            completeMessageBuilder.append(chat.getContextSummary());
            completeMessageBuilder.append("\n\n---\n\n");
            log.info("Kontext-Zusammenfassung für Chat {} eingefügt", chat.getId());
        }

        // Add chat history if this is an existing conversation
        if (!previousMessages.isEmpty()) {
            completeMessageBuilder.append("Previous conversation:\n\n");
            for (Message msg : previousMessages) {
                String roleName = msg.getRole() == MessageRole.USER ? "User" : "Assistant";
                completeMessageBuilder.append(roleName).append(": ");
                completeMessageBuilder.append(msg.getContent());
                completeMessageBuilder.append("\n\n");
            }
            completeMessageBuilder.append("---\n\n");
            log.info("Added {} previous messages to conversation context", previousMessages.size());
        }

        // Add project context if chat is assigned to a project
        if (chat.getProject() != null && !chat.getProject().getContextFiles().isEmpty()) {
            completeMessageBuilder.append(chat.getProject().getCombinedContext());
            completeMessageBuilder.append("\n\n---\n\n");
            log.info("Added project context '{}' ({} bytes) to message",
                    chat.getProject().getName(), chat.getProject().getTotalContextSize());
        }

        // URL-Erkennung: Wenn URLs in der Nachricht sind, Inhalte abrufen
        List<String> detectedUrls = extractUrls(request.getMessage());
        if (!detectedUrls.isEmpty()) {
            log.info("🔗 {} URL(s) in Nachricht erkannt - lade Inhalte", detectedUrls.size());
            completeMessageBuilder.append("=== WEBSEITEN-INHALTE ===\n");
            for (String url : detectedUrls) {
                String content = webSearchService.fetchPageContent(url, 3000);
                if (content != null && !content.isBlank()) {
                    completeMessageBuilder.append("\n**Quelle:** ").append(url).append("\n");
                    completeMessageBuilder.append("**Inhalt:**\n").append(content).append("\n\n");
                    log.info("📄 Inhalt von {} geladen ({} Zeichen)", url, content.length());
                } else {
                    completeMessageBuilder.append("\n**Quelle:** ").append(url).append("\n");
                    completeMessageBuilder.append("**Hinweis:** Inhalt konnte nicht geladen werden.\n\n");
                    log.warn("⚠️ Konnte Inhalt von {} nicht laden", url);
                }
            }
            completeMessageBuilder.append("---\n\n");
        }

        // Automatische Keyword-Erkennung für Web-Suche (RAG-Modus)
        boolean autoWebSearch = !Boolean.TRUE.equals(request.getWebSearchEnabled())
                && webSearchService.shouldAutoSearch(request.getMessage());

        // Web Search RAG: Suche im Web vor LLM-Anfrage
        // WICHTIG: fetchFullContent=true lädt die vollständigen Seiteninhalte!
        boolean includeSourceUrls = Boolean.TRUE.equals(request.getIncludeSourceUrls());
        boolean webSearchActive = false;  // Track ob Web-Suche durchgeführt wurde
        List<WebSearchService.SearchResult> searchResultsForFooter = null;  // Für Quellen-Anhang

        // PRIORITÄT 1: Hochgeladene Dokumente ZUERST (höchste Priorität für RAG)
        // Load saved documents from database for this chat (persistent context)
        List<ChatDocument> savedDocuments = chatDocumentRepository.findByChatIdOrderByCreatedAtAsc(chat.getId());
        boolean hasDocuments = !savedDocuments.isEmpty();
        if (hasDocuments) {
            completeMessageBuilder.append("=== WICHTIG: HOCHGELADENE DOKUMENTE (Primärer Kontext) ===\n\n");
            completeMessageBuilder.append("⚠️ DIESE DOKUMENTE HABEN PRIORITÄT! Beantworte Fragen primär basierend auf diesen Dokumenten.\n\n");
            for (ChatDocument doc : savedDocuments) {
                completeMessageBuilder.append("**Dokument:** ").append(doc.getFileName()).append("\n");
                completeMessageBuilder.append(doc.getContent()).append("\n\n");
            }
            completeMessageBuilder.append("---\n\n");
            log.info("Loaded {} saved documents from database for chat {} (PRIMARY CONTEXT)", savedDocuments.size(), chat.getId());
        }

        // Add NEW document context if provided (and save it for future sessions)
        if (request.getDocumentContext() != null && !request.getDocumentContext().isEmpty()) {
            // Extract file name from fileMetadata if available
            String fileName = "Dokument";
            String fileType = "unknown";
            if (request.getFileMetadata() != null && !request.getFileMetadata().isEmpty()) {
                FileMetadata meta = request.getFileMetadata().get(0);
                if (meta.getName() != null) fileName = meta.getName();
                if (meta.getType() != null) fileType = meta.getType();
            }

            // Save document to database for persistence
            ChatDocument newDoc = new ChatDocument();
            newDoc.setChat(chat);
            newDoc.setFileName(fileName);
            newDoc.setFileType(fileType);
            newDoc.setContent(request.getDocumentContext());
            chatDocumentRepository.save(newDoc);
            log.info("Saved document '{}' ({}) to database for chat {}", fileName, fileType, chat.getId());

            completeMessageBuilder.append("=== NEUES DOKUMENT ===\n\n");
            completeMessageBuilder.append("**Dokument:** ").append(fileName).append("\n");
            completeMessageBuilder.append(request.getDocumentContext());
            completeMessageBuilder.append("\n\n---\n\n");
            hasDocuments = true;
        }

        // PRIORITÄT 2: Web-Suche (nur wenn explizit angefordert oder auto-search ohne Dokumente)
        // Bei vorhandenen Dokumenten ist Web-Suche NUR ergänzend!
        if (Boolean.TRUE.equals(request.getWebSearchEnabled()) || (autoWebSearch && !hasDocuments)) {
            int maxResults = request.getMaxSearchResults() != null ? request.getMaxSearchResults() : 5;

            // Search-Domains ermitteln: Request > Expert > null
            List<String> effectiveSearchDomains = request.getSearchDomains();
            if ((effectiveSearchDomains == null || effectiveSearchDomains.isEmpty()) && request.getExpertId() != null) {
                // Lade Experten-Domains wenn keine im Request und expertId gesetzt
                Expert expert = expertRepository.findById(request.getExpertId()).orElse(null);
                if (expert != null && expert.getSearchDomains() != null && !expert.getSearchDomains().isBlank()) {
                    effectiveSearchDomains = expert.getSearchDomainsAsList();
                    log.info("Verwende Experten-Domains für {}: {}", expert.getName(), effectiveSearchDomains);
                }
            }

            // Erweiterte Suche mit vollständigem Content-Abruf
            WebSearchService.SearchOptions searchOptions = new WebSearchService.SearchOptions();
            searchOptions.setMaxResults(maxResults);
            searchOptions.setDomains(effectiveSearchDomains);
            searchOptions.setFetchFullContent(true);
            searchOptions.setMaxContentLength(2000);
            searchOptions.setOptimizeQuery(true);
            searchOptions.setReRank(true);

            var webSearchResults = webSearchService.searchEnhanced(request.getMessage(), searchOptions);

            if (!webSearchResults.isEmpty()) {
                if (hasDocuments) {
                    completeMessageBuilder.append("=== ERGÄNZENDE WEB-SUCHE (Sekundärer Kontext) ===\n\n");
                    completeMessageBuilder.append("⚠️ Hinweis: Die hochgeladenen Dokumente haben Priorität! Web-Ergebnisse nur als Ergänzung nutzen.\n\n");
                }
                String searchContext = webSearchService.formatForContext(webSearchResults, includeSourceUrls);
                completeMessageBuilder.append(searchContext);
                completeMessageBuilder.append("\n\n");
                webSearchActive = true;
                searchResultsForFooter = webSearchResults;
                log.info("Web-Suche ({}): {} Ergebnisse als {} Kontext",
                        autoWebSearch ? "auto-keyword" : "explicit", webSearchResults.size(),
                        hasDocuments ? "SEKUNDÄRER" : "primärer");
            }
        }

        // WICHTIG: Erinnerung an URL-Links direkt vor der Nutzerfrage (LLM beachtet das besser)
        if (includeSourceUrls) {
            completeMessageBuilder.append("\n🔗 ERINNERUNG: Füge die Markdown-Links [Titel](URL) aus den Suchergebnissen in deine Antwort ein!\n\n");
        }

        // Add current user message (enhanced for document requests with expert directory)
        String messageForLlm = enhanceMessageForDocumentRequest(request.getMessage(), request.getExpertId());
        completeMessageBuilder.append("User question: ");
        completeMessageBuilder.append(messageForLlm);

        String completeMessage = completeMessageBuilder.toString();

        // Save user message (original, without enhancement)
        Message userMessage = new Message();
        userMessage.setChat(chat);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.getMessage());  // Save only user's message, not full context
        userMessage.setTokens(llmProviderService.estimateTokens(completeMessage));
        userMessage.setAttachments(serializeFileMetadata(request.getFileMetadata()));
        userMessage = messageRepository.save(userMessage);

        // Auto-generate title from first message if still "New Chat"
        if (chat.getTitle().equals("New Chat")) {
            chat.setTitle(generateTitleFromMessage(request.getMessage()));
            chat = chatRepository.save(chat);
        }

        // Smart model selection: auto-select best model if user didn't specify one
        String modelToUse;
        if (request.getModel() != null) {
            // User explicitly chose a model
            modelToUse = request.getModel();
            log.info("Using user-selected model: {}", modelToUse);
        } else {
            // Use smart selection based on prompt content
            String defaultModel = chat.getModel();
            modelToUse = modelSelectionService.selectModel(request.getMessage(), defaultModel);
            ModelSelectionService.TaskType taskType = modelSelectionService.getTaskType(request.getMessage());
            log.info("Smart model selection: {} (task type: {})", modelToUse, taskType);
        }

        // Load model selection settings for vision chaining configuration
        var modelSettings = settingsService.getModelSelectionSettings();

        // Modifiziere System-Prompt wenn Web-Suche mit URLs aktiv ist
        String systemPromptToUse = request.getSystemPrompt();
        if (webSearchActive && includeSourceUrls) {
            String urlInstruction = "\n\n🔗 WICHTIG: Du hast Web-Suchergebnisse erhalten. " +
                "Füge in deiner Antwort klickbare Markdown-Links ein: [Titel](URL). " +
                "Antworte immer auf DEUTSCH!";
            if (systemPromptToUse != null && !systemPromptToUse.isBlank()) {
                systemPromptToUse = systemPromptToUse + urlInstruction;
            } else {
                systemPromptToUse = "Du bist ein hilfreicher Assistent." + urlInstruction;
            }
            log.info("System-Prompt erweitert mit URL-Anweisung für Web-Suche");
        } else {
            // KEINE Web-Suche aktiv: Vermeide erfundene URLs
            String noFakeUrlInstruction = "\n\n⚠️ WICHTIG: Erfinde NIEMALS URLs oder Weblinks! " +
                "Wenn du keine verifizierte Quelle hast, erwähne keine Websites. " +
                "Nutze nur URLs, die dir explizit durch Web-Suche bereitgestellt wurden.";
            if (systemPromptToUse != null && !systemPromptToUse.isBlank()) {
                systemPromptToUse = systemPromptToUse + noFakeUrlInstruction;
            } else {
                systemPromptToUse = "Du bist ein hilfreicher Assistent." + noFakeUrlInstruction;
            }
        }

        // Get response from LLM provider (with request ID for cancellation)
        String response;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // Check if Vision-Chaining is enabled (from settings or request)
            boolean visionChainingEnabled = modelSettings.isVisionChainingEnabled() ||
                (request.getVisionChainEnabled() != null && request.getVisionChainEnabled());

            if (visionChainingEnabled) {
                // Vision-Chaining: Vision Model → Haupt-Model
                String visionModel = request.getVisionModel() != null ? request.getVisionModel() : modelSettings.getVisionModel();
                log.info("Vision-Chaining (Non-Stream): Vision={}, Main={}", visionModel, modelToUse);

                // Neue Methode mit Vision-Ergebnis Rückgabe
                var visionResult = llmProviderService.chatWithVisionChainingFull(
                        visionModel,
                        modelToUse,
                        completeMessage,
                        request.getImages(),
                        systemPromptToUse,
                        requestId
                );
                response = visionResult.response();

                // Vision/OCR-Ergebnis als Dokument speichern für späteren Kontext
                if (visionResult.visionOutput() != null && !visionResult.visionOutput().isBlank()) {
                    String imageName = "Bild-Analyse";
                    if (request.getFileMetadata() != null && !request.getFileMetadata().isEmpty()) {
                        imageName = request.getFileMetadata().get(0).getName();
                    }
                    ChatDocument visionDoc = new ChatDocument();
                    visionDoc.setChat(chat);
                    visionDoc.setFileName(imageName + " (OCR/Vision)");
                    visionDoc.setFileType("vision-ocr");
                    visionDoc.setContent(visionResult.visionOutput());
                    chatDocumentRepository.save(visionDoc);
                    log.info("Vision/OCR-Ergebnis als Dokument '{}' gespeichert ({} Zeichen)",
                            visionDoc.getFileName(), visionResult.visionOutput().length());
                }
            } else {
                // Direct vision API (model must be vision-capable)
                response = llmProviderService.chatWithVision(
                        modelToUse,
                        completeMessage,
                        request.getImages(),
                        systemPromptToUse,
                        requestId
                );
            }
        } else {
            // Use regular generate API
            response = llmProviderService.chat(
                    modelToUse,
                    completeMessage,
                    systemPromptToUse,
                    requestId
            );
        }

        // Don't convert to HTML - frontend will handle markdown rendering
        // Save assistant message with raw markdown

        // Automatisch Quellen-Links am Ende anhängen wenn Web-Suche aktiv war
        String finalResponse = response;
        if (searchResultsForFooter != null && !searchResultsForFooter.isEmpty()) {
            String sourcesFooter = webSearchService.formatSourcesFooter(searchResultsForFooter);
            finalResponse = response + sourcesFooter;
            log.info("Quellen-Footer mit {} Links an Antwort angehängt", searchResultsForFooter.size());
        }

        Message assistantMessage = new Message();
        assistantMessage.setChat(chat);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(finalResponse);  // Store raw markdown with sources footer
        assistantMessage.setTokens(llmProviderService.estimateTokens(response));
        assistantMessage.setModelName(request.getModel());  // Store which model was used
        assistantMessage = messageRepository.save(assistantMessage);

        // Update global stats
        updateGlobalStats(assistantMessage.getTokens());

        log.info("Chat {} - Sent message and received response ({} tokens)",
                chat.getId(), assistantMessage.getTokens());

        // Check if response contains downloadable code and auto-generate download
        String downloadUrl = checkAndGenerateDownload(request.getMessage(), finalResponse);

        // Check for document generation request (Brief, PDF-Zusammenfassung)
        // Funktioniert auch ohne Experten (mit Default-Werten)
        if (downloadUrl == null) {
            downloadUrl = checkAndGenerateDocument(request.getMessage(), finalResponse, request.getExpertId());
        }

        ChatResponse chatResponse = new ChatResponse(
                chat.getId(),
                finalResponse,  // Return raw markdown with sources footer
                assistantMessage.getTokens(),
                chat.getModel(),
                requestId  // Include request ID for tracking
        );

        // Add download URL if generated
        if (downloadUrl != null) {
            chatResponse.setDownloadUrl(downloadUrl);
        }

        return chatResponse;
    }

    /**
     * Send a message with STREAMING enabled
     * Returns an SseEmitter that will send chunks in real-time
     */
    public SseEmitter sendMessageStream(ChatRequest request) {
        // Generate request ID for tracking
        String requestId = UUID.randomUUID().toString();

        // Create SSE emitter with 30 minute timeout (for large generation tasks)
        SseEmitter emitter = new SseEmitter(1_800_000L);

        // Track emitter completion state
        final boolean[] isCompleted = {false};

        // Add timeout and completion callbacks
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out for request: {}", requestId);
            isCompleted[0] = true;
            llmProviderService.cancelRequest(requestId);
        });

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for request: {}", requestId);
            isCompleted[0] = true;
        });

        emitter.onError((ex) -> {
            log.error("SSE emitter error for request: {}", requestId, ex);
            isCompleted[0] = true;
            llmProviderService.cancelRequest(requestId);
        });

        // IMPORTANT: Load chat WITH project and context files BEFORE async execution
        // to avoid LazyInitializationException
        Chat chat;
        String projectContext = null;
        String projectName = null;
        long projectContextSize = 0;

        if (request.getChatId() != null) {
            // Use findByIdWithProject to eagerly load Project + ContextFiles (avoids LazyInitializationException)
            chat = chatRepository.findByIdWithProject(request.getChatId())
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + request.getChatId()));

            // Project and ContextFiles are already loaded via JOIN FETCH
            if (chat.getProject() != null) {
                Project project = chat.getProject();
                if (project.getContextFiles() != null && !project.getContextFiles().isEmpty()) {
                    projectContext = project.getCombinedContext();
                    projectName = project.getName();
                    projectContextSize = project.getTotalContextSize();
                    log.info("📁 Project context loaded: {} ({} bytes)", projectName, projectContextSize);
                }
            }
        } else {
            // Create new chat
            chat = new Chat();
            chat.setTitle("New Chat");
            chat.setModel(request.getModel() != null ? request.getModel() : "qwen2.5-coder:7b");
            chat = chatRepository.save(chat);
        }

        // Load model selection settings for vision chaining configuration
        var modelSettings = settingsService.getModelSelectionSettings();

        // Smart model selection: auto-select best model if user didn't specify one
        String modelToUse;
        boolean useSmartSelectionForVision = false;

        if (request.getModel() != null) {
            // User explicitly chose a model
            modelToUse = request.getModel();
            log.info("Using user-selected model: {}", modelToUse);
        } else {
            // Check if this is a vision chaining request with smart selection enabled
            if (request.getImages() != null && !request.getImages().isEmpty() &&
                request.getVisionChainEnabled() != null && request.getVisionChainEnabled() &&
                modelSettings.isVisionChainingSmartSelection()) {
                // Vision chaining with smart selection: select main model based on prompt
                useSmartSelectionForVision = true;
                String defaultModel = chat.getModel();
                modelToUse = modelSelectionService.selectModel(request.getMessage(), defaultModel);
                ModelSelectionService.TaskType taskType = modelSelectionService.getTaskType(request.getMessage());
                log.info("Vision-Chaining Smart Selection: {} (task type: {})", modelToUse, taskType);
            } else {
                // Regular smart selection or no selection
                String defaultModel = chat.getModel();
                modelToUse = modelSelectionService.selectModel(request.getMessage(), defaultModel);
                ModelSelectionService.TaskType taskType = modelSelectionService.getTaskType(request.getMessage());
                log.info("Smart model selection: {} (task type: {})", modelToUse, taskType);
            }
        }

        // ===== ExpertRuntime Integration =====
        // Wenn ein Experte ausgewählt ist, verwende ExpertRuntime für korrektes Model-Mapping
        ExpertRuntime expertRuntime = null;
        if (request.getExpertId() != null) {
            var runtimeOpt = expertRuntimeFactory.getRuntime(
                request.getExpertId(),
                request.getActiveExpertModeId(),
                request.getCpuOnly()
            );
            if (runtimeOpt.isPresent()) {
                expertRuntime = runtimeOpt.get();
                // Überschreibe modelToUse mit dem korrekt aufgelösten Pfad
                if (expertRuntime.getResolvedModelPath() != null) {
                    modelToUse = expertRuntime.getResolvedModelPath().toString();
                    log.info("🎓 ExpertRuntime Model: {}", modelToUse);
                } else if (expertRuntime.getModelName() != null) {
                    modelToUse = expertRuntime.getModelName();
                    log.info("🎓 ExpertRuntime Model (Name): {}", modelToUse);
                }
            }
        }
        final ExpertRuntime finalExpertRuntime = expertRuntime;

        // Make final variables for use in lambda
        final Chat finalChat = chat;
        final String finalProjectContext = projectContext;
        final String finalProjectName = projectName;
        final long finalProjectContextSize = projectContextSize;
        final String finalModel = modelToUse;
        final String finalVisionModel = modelSettings.getVisionModel();
        final boolean finalVisionChainingEnabled = modelSettings.isVisionChainingEnabled();
        final boolean finalUseSmartSelectionForVision = useSmartSelectionForVision;

        // Expert-Einstellungen: Lade aus ExpertRuntime oder direkt aus DB
        Integer expertNumCtx = null;
        Integer expertMaxTokens = null;
        if (finalExpertRuntime != null) {
            // Verwende ExpertRuntime (bereits aufgelöst)
            expertNumCtx = finalExpertRuntime.getContextSize();
            expertMaxTokens = finalExpertRuntime.getMaxTokens();
            log.info("📏 ExpertRuntime {}: numCtx={}, maxTokens={}",
                finalExpertRuntime.getName(), expertNumCtx, expertMaxTokens);
        } else if (request.getExpertId() != null) {
            // Fallback: Direkt aus DB laden
            Expert expert = expertRepository.findById(request.getExpertId()).orElse(null);
            if (expert != null) {
                if (expert.getDefaultNumCtx() != null) {
                    expertNumCtx = expert.getDefaultNumCtx();
                }
                if (expert.getDefaultMaxTokens() != null) {
                    expertMaxTokens = expert.getDefaultMaxTokens();
                }
                log.info("📏 Expert {}: numCtx={}, maxTokens={}", expert.getName(), expertNumCtx, expertMaxTokens);
            }
        }
        final Integer finalExpertNumCtx = expertNumCtx;
        // Verwende Experten-maxTokens falls Request keinen Wert hat
        final Integer finalMaxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : expertMaxTokens;

        // Expert-Modus-Erkennung: Erkennt passenden Modus basierend auf Keywords
        String modeSwitchNotice = null;
        String expertSystemPrompt = request.getSystemPrompt();  // Default
        if (request.getExpertId() != null) {
            Expert expert = expertRepository.findById(request.getExpertId()).orElse(null);
            if (expert != null) {
                ExpertModeResult modeResult = detectAndUpdateExpertMode(chat, request.getExpertId(), request.getMessage());
                if (modeResult.switchNotice() != null) {
                    modeSwitchNotice = modeResult.switchNotice();
                }
                // Baue System-Prompt mit Experten-Basis + Modus-Addition
                expertSystemPrompt = buildExpertSystemPrompt(expert, modeResult.mode());
                log.info("🎓 Expert-Modus: {} (Prompt-Länge: {})",
                    modeResult.mode() != null ? modeResult.mode().getName() : "Allgemein",
                    expertSystemPrompt != null ? expertSystemPrompt.length() : 0);
            }
        }
        final String finalModeSwitchNotice = modeSwitchNotice;
        final String finalExpertSystemPrompt = expertSystemPrompt;

        // URL-Erkennung: Wenn URLs in der Nachricht sind, Inhalte abrufen
        String urlContext = null;
        List<String> detectedUrlsStreaming = extractUrls(request.getMessage());
        if (!detectedUrlsStreaming.isEmpty()) {
            log.info("🔗 {} URL(s) in Nachricht erkannt (Streaming) - lade Inhalte", detectedUrlsStreaming.size());
            StringBuilder urlContextBuilder = new StringBuilder("=== WEBSEITEN-INHALTE ===\n");
            for (String url : detectedUrlsStreaming) {
                String content = webSearchService.fetchPageContent(url, 3000);
                if (content != null && !content.isBlank()) {
                    urlContextBuilder.append("\n**Quelle:** ").append(url).append("\n");
                    urlContextBuilder.append("**Inhalt:**\n").append(content).append("\n\n");
                    log.info("📄 Inhalt von {} geladen ({} Zeichen)", url, content.length());
                } else {
                    urlContextBuilder.append("\n**Quelle:** ").append(url).append("\n");
                    urlContextBuilder.append("**Hinweis:** Inhalt konnte nicht geladen werden.\n\n");
                    log.warn("⚠️ Konnte Inhalt von {} nicht laden", url);
                }
            }
            urlContextBuilder.append("---\n\n");
            urlContext = urlContextBuilder.toString();
        }
        final String finalUrlContext = urlContext;

        // Web Search RAG: Suche vor async execution durchführen
        // WICHTIG: fetchFullContent=true lädt die vollständigen Seiteninhalte!
        String webSearchContext = null;
        boolean includeSourceUrlsStreaming = Boolean.TRUE.equals(request.getIncludeSourceUrls());
        List<WebSearchService.SearchResult> streamingSearchResults = null;  // Für Quellen-Footer
        if (Boolean.TRUE.equals(request.getWebSearchEnabled())) {
            int maxResults = request.getMaxSearchResults() != null ? request.getMaxSearchResults() : 5;

            // Search-Domains ermitteln: Request > Expert > null
            List<String> effectiveSearchDomainsStreaming = request.getSearchDomains();
            if ((effectiveSearchDomainsStreaming == null || effectiveSearchDomainsStreaming.isEmpty()) && request.getExpertId() != null) {
                Expert expert = expertRepository.findById(request.getExpertId()).orElse(null);
                if (expert != null && expert.getSearchDomains() != null && !expert.getSearchDomains().isBlank()) {
                    effectiveSearchDomainsStreaming = expert.getSearchDomainsAsList();
                    log.info("Verwende Experten-Domains (Streaming) für {}: {}", expert.getName(), effectiveSearchDomainsStreaming);
                }
            }

            // Erweiterte Suche mit vollständigem Content-Abruf
            WebSearchService.SearchOptions searchOptions = new WebSearchService.SearchOptions();
            searchOptions.setMaxResults(maxResults);
            searchOptions.setDomains(effectiveSearchDomainsStreaming);
            searchOptions.setFetchFullContent(true);  // Vollständige Seiteninhalte laden!
            searchOptions.setMaxContentLength(2000);  // Max 2000 Zeichen pro Seite
            searchOptions.setOptimizeQuery(true);
            searchOptions.setReRank(true);

            var webSearchResults = webSearchService.searchEnhanced(request.getMessage(), searchOptions);

            if (!webSearchResults.isEmpty()) {
                // Wenn includeSourceUrls aktiv, erhält das LLM Anweisungen die URLs einzubauen
                webSearchContext = webSearchService.formatForContext(webSearchResults, includeSourceUrlsStreaming);
                streamingSearchResults = webSearchResults;  // Für Quellen-Footer speichern
                log.info("Web-Suche (Streaming): {} Ergebnisse MIT VOLLSTÄNDIGEM INHALT als RAG-Kontext (includeSourceUrls={})",
                        webSearchResults.size(), includeSourceUrlsStreaming);
            }
        }
        final String finalWebSearchContext = webSearchContext;
        final boolean finalIncludeSourceUrls = includeSourceUrlsStreaming;
        final List<WebSearchService.SearchResult> finalSearchResults = streamingSearchResults;

        // Modifiziere System-Prompt wenn Web-Suche mit URLs aktiv ist (Streaming)
        // Verwende Expert-System-Prompt falls erkannt, sonst Request-Prompt
        String systemPromptForStreaming = finalExpertSystemPrompt != null ? finalExpertSystemPrompt : request.getSystemPrompt();
        if (webSearchContext != null && includeSourceUrlsStreaming) {
            String urlInstruction = "\n\n🔗 WICHTIG: Du hast Web-Suchergebnisse erhalten. " +
                "Füge in deiner Antwort klickbare Markdown-Links ein: [Titel](URL). " +
                "Antworte immer auf DEUTSCH!";
            if (systemPromptForStreaming != null && !systemPromptForStreaming.isBlank()) {
                systemPromptForStreaming = systemPromptForStreaming + urlInstruction;
            } else {
                systemPromptForStreaming = "Du bist ein hilfreicher Assistent." + urlInstruction;
            }
            log.info("System-Prompt (Streaming) erweitert mit URL-Anweisung für Web-Suche");
        } else {
            // KEINE Web-Suche aktiv: Vermeide erfundene URLs (Streaming)
            String noFakeUrlInstruction = "\n\n⚠️ WICHTIG: Erfinde NIEMALS URLs oder Weblinks! " +
                "Wenn du keine verifizierte Quelle hast, erwähne keine Websites. " +
                "Nutze nur URLs, die dir explizit durch Web-Suche bereitgestellt wurden.";
            if (systemPromptForStreaming != null && !systemPromptForStreaming.isBlank()) {
                systemPromptForStreaming = systemPromptForStreaming + noFakeUrlInstruction;
            } else {
                systemPromptForStreaming = "Du bist ein hilfreicher Assistent." + noFakeUrlInstruction;
            }
        }
        final String finalSystemPrompt = systemPromptForStreaming;

        executorService.execute(() -> {
            try {

                // Load previous messages from database to maintain conversation context
                List<Message> previousMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(finalChat.getId());

                // Build complete message (with chat history, project context and document context if provided)
                StringBuilder completeMessageBuilder = new StringBuilder();

                // Kontext-Zusammenfassung einfügen (falls vorhanden - nach Löschen von Nachrichten)
                if (finalChat.getContextSummary() != null && !finalChat.getContextSummary().isBlank()) {
                    completeMessageBuilder.append("=== KONTEXT-ZUSAMMENFASSUNG (aus vorherigem Gespräch) ===\n");
                    completeMessageBuilder.append(finalChat.getContextSummary());
                    completeMessageBuilder.append("\n\n---\n\n");
                    log.info("Kontext-Zusammenfassung für Streaming-Chat {} eingefügt", finalChat.getId());
                }

                // Add chat history if this is an existing conversation
                if (!previousMessages.isEmpty()) {
                    completeMessageBuilder.append("Previous conversation:\n\n");
                    for (Message msg : previousMessages) {
                        String roleName = msg.getRole() == MessageRole.USER ? "User" : "Assistant";
                        completeMessageBuilder.append(roleName).append(": ");
                        completeMessageBuilder.append(msg.getContent());
                        completeMessageBuilder.append("\n\n");
                    }
                    completeMessageBuilder.append("---\n\n");
                    log.info("Added {} previous messages to conversation context", previousMessages.size());
                }

                // Add project context if available (loaded before async execution)
                if (finalProjectContext != null) {
                    completeMessageBuilder.append(finalProjectContext);
                    completeMessageBuilder.append("\n\n---\n\n");
                    log.info("Added project context '{}' ({} bytes) to streaming message",
                            finalProjectName, finalProjectContextSize);
                }

                // PRIORITÄT 1: Hochgeladene Dokumente ZUERST (höchste Priorität für RAG)
                // Load saved documents from database for this chat (persistent context)
                List<ChatDocument> savedDocs = chatDocumentRepository.findByChatIdOrderByCreatedAtAsc(finalChat.getId());
                boolean streamHasDocuments = !savedDocs.isEmpty();
                if (streamHasDocuments) {
                    completeMessageBuilder.append("=== WICHTIG: HOCHGELADENE DOKUMENTE (Primärer Kontext) ===\n\n");
                    completeMessageBuilder.append("⚠️ DIESE DOKUMENTE HABEN PRIORITÄT! Beantworte Fragen primär basierend auf diesen Dokumenten.\n\n");
                    for (ChatDocument doc : savedDocs) {
                        completeMessageBuilder.append("**Dokument:** ").append(doc.getFileName()).append("\n");
                        completeMessageBuilder.append(doc.getContent()).append("\n\n");
                    }
                    completeMessageBuilder.append("---\n\n");
                    log.info("Loaded {} saved documents from database for streaming chat {} (PRIMARY CONTEXT)", savedDocs.size(), finalChat.getId());
                }

                // Add NEW document context if provided (and save it for future sessions)
                if (request.getDocumentContext() != null && !request.getDocumentContext().isEmpty()) {
                    // Extract file name from fileMetadata if available
                    String fileName = "Dokument";
                    String fileType = "unknown";
                    if (request.getFileMetadata() != null && !request.getFileMetadata().isEmpty()) {
                        FileMetadata meta = request.getFileMetadata().get(0);
                        if (meta.getName() != null) fileName = meta.getName();
                        if (meta.getType() != null) fileType = meta.getType();
                    }

                    // Save document to database for persistence
                    ChatDocument newDoc = new ChatDocument();
                    newDoc.setChat(finalChat);
                    newDoc.setFileName(fileName);
                    newDoc.setFileType(fileType);
                    newDoc.setContent(request.getDocumentContext());
                    chatDocumentRepository.save(newDoc);
                    log.info("Saved document '{}' to database for streaming chat {}", fileName, finalChat.getId());

                    completeMessageBuilder.append("=== NEUES DOKUMENT ===\n\n");
                    completeMessageBuilder.append("**Dokument:** ").append(fileName).append("\n");
                    completeMessageBuilder.append(request.getDocumentContext());
                    completeMessageBuilder.append("\n\n---\n\n");
                    streamHasDocuments = true;
                }

                // Add URL content context if URLs were detected (NACH Dokumenten)
                if (finalUrlContext != null) {
                    completeMessageBuilder.append(finalUrlContext);
                    completeMessageBuilder.append("\n\n");
                }

                // PRIORITÄT 2: Web search RAG context (NACH Dokumenten, als Ergänzung)
                if (finalWebSearchContext != null) {
                    if (streamHasDocuments) {
                        completeMessageBuilder.append("=== ERGÄNZENDE WEB-SUCHE (Sekundärer Kontext) ===\n\n");
                        completeMessageBuilder.append("⚠️ Hinweis: Die hochgeladenen Dokumente haben Priorität! Web-Ergebnisse nur als Ergänzung nutzen.\n\n");
                    }
                    completeMessageBuilder.append(finalWebSearchContext);
                    completeMessageBuilder.append("\n\n");
                }

                // WICHTIG: Erinnerung an URL-Links direkt vor der Nutzerfrage (LLM beachtet das besser)
                if (finalIncludeSourceUrls && finalWebSearchContext != null) {
                    completeMessageBuilder.append("\n🔗 ERINNERUNG: Füge die Markdown-Links [Titel](URL) aus den Suchergebnissen in deine Antwort ein!\n\n");
                }

                // Add current user message (enhanced for document requests with expert directory)
                String messageForLlm = enhanceMessageForDocumentRequest(request.getMessage(), request.getExpertId());
                completeMessageBuilder.append("User question: ");
                completeMessageBuilder.append(messageForLlm);

                String completeMessage = completeMessageBuilder.toString();

                // Save user message (original, without enhancement)
                Message userMessage = new Message();
                userMessage.setChat(finalChat);
                userMessage.setRole(MessageRole.USER);
                userMessage.setContent(request.getMessage());  // Save only user's message, not full context
                userMessage.setTokens(llmProviderService.estimateTokens(completeMessage));
                userMessage.setAttachments(serializeFileMetadata(request.getFileMetadata()));
                messageRepository.save(userMessage);

                // Auto-generate title from first message if still "New Chat"
                if (finalChat.getTitle().equals("New Chat")) {
                    finalChat.setTitle(generateTitleFromMessage(request.getMessage()));
                    chatRepository.save(finalChat);
                }

                // Check if this is a document request BEFORE streaming starts
                // This allows frontend to show document card immediately instead of text
                DocumentGeneratorService.DocumentRequest docRequest = documentGeneratorService.detectDocumentRequest(request.getMessage());
                boolean isDocumentRequest = docRequest != null;
                String documentType = docRequest != null ? docRequest.type().name() : null;

                // Send initial event with chat ID, request ID, document flag, and type
                log.info("Starting streaming for chat {} with model {} (document: {}, type: {})", finalChat.getId(), finalModel, isDocumentRequest, documentType);
                String startData = isDocumentRequest
                    ? "{\"chatId\":" + finalChat.getId() + ",\"requestId\":\"" + requestId + "\",\"isDocumentRequest\":true,\"documentType\":\"" + documentType + "\"}"
                    : "{\"chatId\":" + finalChat.getId() + ",\"requestId\":\"" + requestId + "\",\"isDocumentRequest\":false}";
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data(startData));

                // Collect full response for database storage
                StringBuilder fullResponse = new StringBuilder();

                // Modus-Wechsel-Hinweis als ersten Chunk senden (falls Modus gewechselt hat)
                if (finalModeSwitchNotice != null) {
                    log.info("📋 Modus-Wechsel-Hinweis: {}", finalModeSwitchNotice);
                    emitter.send(SseEmitter.event()
                            .name("chunk")
                            .data(finalModeSwitchNotice));
                    fullResponse.append(finalModeSwitchNotice);
                }

                // Stream response from LLM provider
                String finalCompleteMessage = completeMessage;
                log.info("Calling LLM provider chatStream for model: {}", finalModel);
                log.info("📊 Request parameters - maxTokens: {}, temp: {}, topP: {}, topK: {}, repeatPenalty: {}",
                    request.getMaxTokens(), request.getTemperature(), request.getTopP(),
                    request.getTopK(), request.getRepeatPenalty());
                log.info("🔍 DEBUG request.getCpuOnly() = {}", request.getCpuOnly());

                // Use vision streaming if images are provided
                if (request.getImages() != null && !request.getImages().isEmpty()) {
                    // Check if Vision-Chaining is enabled (from settings or request)
                    boolean visionChainingEnabled = finalVisionChainingEnabled ||
                        (request.getVisionChainEnabled() != null && request.getVisionChainEnabled());

                    if (visionChainingEnabled) {
                        // Vision-Chaining: Vision Model → Haupt-Model
                        log.info("Vision-Chaining enabled: Vision={}, Main={} (Smart Selection: {})",
                                finalVisionModel, finalModel, finalUseSmartSelectionForVision);

                        // Callback für Vision/OCR-Ergebnis Speicherung
                        final String[] visionOutputHolder = {null};

                        llmProviderService.chatStreamWithVisionChaining(
                                request.getVisionModel() != null ? request.getVisionModel() : finalVisionModel,  // Vision Model from settings or request
                                finalModel,  // Haupt-Model (smart selected or user chosen)
                                finalCompleteMessage,
                                request.getImages(),
                                finalSystemPrompt,
                                requestId,
                                chunk -> {
                                    // Check if emitter is already completed
                                    if (isCompleted[0]) {
                                        log.warn("Emitter already completed, skipping chunk");
                                        return;
                                    }
                                    try {
                                        log.debug("Sending chunk to frontend: {}", chunk);
                                        emitter.send(SseEmitter.event()
                                                .name("chunk")
                                                .data(chunk));
                                        fullResponse.append(chunk);
                                    } catch (IOException e) {
                                        log.error("Error sending chunk", e);
                                        if (!isCompleted[0]) {
                                            isCompleted[0] = true;
                                            emitter.completeWithError(e);
                                        }
                                    }
                                },
                                request.getShowIntermediateOutput() != null && request.getShowIntermediateOutput(),
                                // Vision/OCR Callback - speichert das Ergebnis
                                visionOutput -> {
                                    visionOutputHolder[0] = visionOutput;
                                    // Speichere Vision/OCR-Ergebnis als Dokument
                                    if (visionOutput != null && !visionOutput.isBlank()) {
                                        String imageName = "Bild-Analyse";
                                        if (request.getFileMetadata() != null && !request.getFileMetadata().isEmpty()) {
                                            imageName = request.getFileMetadata().get(0).getName();
                                        }
                                        ChatDocument visionDoc = new ChatDocument();
                                        visionDoc.setChat(finalChat);
                                        visionDoc.setFileName(imageName + " (OCR/Vision)");
                                        visionDoc.setFileType("vision-ocr");
                                        visionDoc.setContent(visionOutput);
                                        chatDocumentRepository.save(visionDoc);
                                        log.info("Vision/OCR-Ergebnis als Dokument '{}' gespeichert ({} Zeichen)",
                                                visionDoc.getFileName(), visionOutput.length());
                                    }
                                }
                        );
                    } else {
                        // Normal Vision Model (ohne Chaining)
                        llmProviderService.chatStreamWithVision(
                                finalModel,  // Smart selected or user chosen
                                finalCompleteMessage,
                                request.getImages(),
                                finalSystemPrompt,
                                requestId,
                                chunk -> {
                                    // Check if emitter is already completed
                                    if (isCompleted[0]) {
                                        log.warn("Emitter already completed, skipping chunk");
                                        return;
                                    }
                                    try {
                                        log.debug("Sending chunk to frontend: {}", chunk);
                                        emitter.send(SseEmitter.event()
                                                .name("chunk")
                                                .data(chunk));
                                        fullResponse.append(chunk);
                                    } catch (IOException e) {
                                        log.error("Error sending chunk", e);
                                        if (!isCompleted[0]) {
                                            isCompleted[0] = true;
                                            emitter.completeWithError(e);
                                        }
                                    }
                                }
                        );
                    }
                } else {
                    llmProviderService.chatStream(
                            finalModel,  // Smart selected or user chosen
                            finalCompleteMessage,
                            finalSystemPrompt,
                            requestId,
                            chunk -> {
                                // Check if emitter is already completed
                                if (isCompleted[0]) {
                                    log.warn("Emitter already completed, skipping chunk");
                                    return;
                                }
                                try {
                                    log.debug("Sending chunk to frontend: {}", chunk);
                                    emitter.send(SseEmitter.event()
                                            .name("chunk")
                                            .data(chunk));
                                    fullResponse.append(chunk);
                                } catch (IOException e) {
                                    log.error("Error sending chunk", e);
                                    if (!isCompleted[0]) {
                                        isCompleted[0] = true;
                                        emitter.completeWithError(e);
                                    }
                                }
                            },
                            finalMaxTokens,              // Pass maxTokens (Expert > Request)
                            request.getTemperature(),    // Pass temperature
                            request.getTopP(),           // Pass topP
                            request.getTopK(),           // Pass topK
                            request.getRepeatPenalty(),  // Pass repeatPenalty
                            finalExpertNumCtx,           // Pass numCtx from Expert settings
                            request.getCpuOnly()         // Pass cpuOnly flag (für Demos ohne NVIDIA GPU)
                    );
                }
                log.info("LLM chatStream completed. Full response length: {}", fullResponse.length());

                // Automatisch Quellen-Footer anhängen und an Frontend senden
                String sourcesFooter = "";
                if (finalSearchResults != null && !finalSearchResults.isEmpty()) {
                    sourcesFooter = webSearchService.formatSourcesFooter(finalSearchResults);
                    // Sende Quellen-Footer als zusätzlichen Chunk an Frontend
                    if (!isCompleted[0]) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(sourcesFooter));
                            fullResponse.append(sourcesFooter);
                            log.info("Quellen-Footer mit {} Links gestreamt", finalSearchResults.size());
                        } catch (IOException e) {
                            log.warn("Konnte Quellen-Footer nicht senden: {}", e.getMessage());
                            fullResponse.append(sourcesFooter);  // Trotzdem zur DB-Speicherung anhängen
                        }
                    } else {
                        fullResponse.append(sourcesFooter);  // Für DB-Speicherung
                    }
                }

                // Check for document generation request BEFORE saving message
                // Funktioniert auch ohne Experten (mit Default-Werten)
                String documentDownloadUrl = checkAndGenerateDocument(
                    request.getMessage(),
                    fullResponse.toString(),
                    request.getExpertId()
                );

                // Save assistant message to database
                Message assistantMessage = new Message();
                assistantMessage.setChat(finalChat);
                assistantMessage.setRole(MessageRole.ASSISTANT);

                // For generated documents: don't store full content (it's redundant - already in chat context)
                // Only store a placeholder - the frontend will show the document card based on downloadUrl
                String contentToStore = documentDownloadUrl != null
                    ? "[Dokument generiert]"  // Minimal placeholder - full content already in conversation
                    : fullResponse.toString();
                assistantMessage.setContent(contentToStore);
                assistantMessage.setTokens(llmProviderService.estimateTokens(contentToStore));
                assistantMessage.setModelName(request.getModel());  // Store which model was used
                assistantMessage.setDownloadUrl(documentDownloadUrl);  // Save download URL for document
                messageRepository.save(assistantMessage);

                // Update global stats
                updateGlobalStats(assistantMessage.getTokens());

                // Send completion event (only if not already completed)
                if (!isCompleted[0]) {
                    try {
                        // Calculate total tokens in chat for context usage display
                        int totalChatTokens = messageRepository.findByChatIdOrderByCreatedAtAsc(finalChat.getId())
                                .stream()
                                .mapToInt(m -> m.getTokens() != null ? m.getTokens() : 0)
                                .sum();

                        // Get max context from expert if available
                        Integer maxContextTokens = null;
                        if (request.getExpertId() != null) {
                            Expert expert = expertRepository.findById(request.getExpertId()).orElse(null);
                            if (expert != null && expert.getDefaultNumCtx() != null) {
                                maxContextTokens = expert.getDefaultNumCtx();
                            }
                        }

                        // Build done event JSON with context usage info
                        StringBuilder doneJson = new StringBuilder();
                        doneJson.append("{\"tokens\":").append(assistantMessage.getTokens());
                        doneJson.append(",\"totalChatTokens\":").append(totalChatTokens);
                        if (maxContextTokens != null) {
                            doneJson.append(",\"maxContextTokens\":").append(maxContextTokens);
                        }
                        if (documentDownloadUrl != null) {
                            doneJson.append(",\"downloadUrl\":\"").append(documentDownloadUrl).append("\"");
                        }
                        doneJson.append("}");

                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(doneJson.toString()));
                        isCompleted[0] = true;
                        emitter.complete();
                        log.info("Streaming completed for chat {} (tokens: {}/{}, downloadUrl: {})",
                                finalChat.getId(), totalChatTokens, maxContextTokens, documentDownloadUrl);
                    } catch (Exception e) {
                        log.error("Error sending completion event", e);
                        // Don't try to complete with error if already completed
                    }
                } else {
                    log.warn("Emitter was already completed before sending done event");
                }

            } catch (Exception e) {
                log.error("Error during streaming", e);
                if (!isCompleted[0]) {
                    try {
                        // Create user-friendly error message based on active provider
                        String errorMessage = e.getMessage();
                        String providerName = llmProviderService.getActiveProviderName();

                        if (errorMessage != null && errorMessage.contains("404")) {
                            if ("ollama".equalsIgnoreCase(providerName)) {
                                errorMessage = "⚠️ Modell nicht gefunden!\n\n" +
                                        "Das ausgewählte Modell ist in Ollama nicht verfügbar.\n" +
                                        "• Prüfe ob das Modell korrekt installiert ist: ollama list\n" +
                                        "• Installiere das Modell mit: ollama pull <modellname>\n" +
                                        "• Prüfe die Schreibweise des Modellnamens";
                            } else {
                                errorMessage = "⚠️ LLM-Provider ist nicht erreichbar!\n\n" +
                                        "Bitte stelle sicher, dass " + providerName + " läuft:\n" +
                                        "• Fleet Navigator sollte den Provider automatisch starten\n" +
                                        "• Prüfe die Logs für Fehler beim Start\n" +
                                        "• Stelle sicher, dass Modelle verfügbar sind";
                            }
                        } else if (errorMessage != null && errorMessage.contains("Connection refused")) {
                            errorMessage = "⚠️ Verbindung zum LLM-Provider fehlgeschlagen!\n\n" +
                                    "Der Provider '" + providerName + "' läuft nicht.\n" +
                                    "Bitte prüfe die Fleet Navigator Logs und starte neu.";
                        }

                        // Escape quotes for JSON
                        String escapedMessage = errorMessage.replace("\"", "\\\"").replace("\n", "\\n");

                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"" + escapedMessage + "\"}"));
                    } catch (IOException ex) {
                        log.error("Error sending error event", ex);
                    }
                    isCompleted[0] = true;
                    emitter.completeWithError(e);
                } else {
                    log.warn("Emitter was already completed, cannot send error event");
                }
            }
        });

        return emitter;
    }

    /**
     * Abort an active request
     */
    public boolean abortRequest(String requestId) {
        log.info("Aborting request: {}", requestId);
        return llmProviderService.cancelRequest(requestId);
    }

    /**
     * Get chat history
     */
    @Transactional(readOnly = true)
    public ChatDTO getChatHistory(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        return mapToChatDTO(chat);
    }

    /**
     * Get all chats
     */
    @Transactional(readOnly = true)
    public List<ChatDTO> getAllChats() {
        return chatRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::mapToChatDTO)
                .collect(Collectors.toList());
    }

    /**
     * Rename a chat
     */
    @Transactional
    public ChatDTO renameChat(Long chatId, String newTitle) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        chat.setTitle(newTitle);
        chat = chatRepository.save(chat);
        log.info("Renamed chat {} to: {}", chatId, newTitle);

        return mapToChatDTO(chat);
    }

    /**
     * Update chat's model
     */
    @Transactional
    public ChatDTO updateChatModel(Long chatId, String modelName) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        chat.setModel(modelName);
        chat = chatRepository.save(chat);
        log.info("Updated chat {} model to: {}", chatId, modelName);

        return mapToChatDTO(chat);
    }

    /**
     * Update chat expert
     */
    @Transactional
    public ChatDTO updateChatExpert(Long chatId, Long expertId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        chat.setExpertId(expertId);
        chat = chatRepository.save(chat);
        log.info("Updated chat {} expert to: {}", chatId, expertId);

        return mapToChatDTO(chat);
    }

    /**
     * Delete a chat
     */
    @Transactional
    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
        log.info("Deleted chat: {}", chatId);
    }

    /**
     * Delete a single message from a chat
     * Nach dem Löschen wird automatisch eine Kontext-Zusammenfassung erstellt
     */
    @Transactional
    public void deleteMessage(Long chatId, Long messageId) {
        // Verify the message belongs to the chat
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Nachricht nicht gefunden: " + messageId));

        if (!message.getChat().getId().equals(chatId)) {
            throw new IllegalArgumentException("Nachricht gehört nicht zu diesem Chat");
        }

        Chat chat = message.getChat();
        messageRepository.delete(message);
        log.info("Deleted message {} from chat {}", messageId, chatId);

        // Regeneriere Kontext-Zusammenfassung asynchron
        executorService.execute(() -> {
            try {
                regenerateContextSummary(chatId);
            } catch (Exception e) {
                log.warn("Konnte Kontext-Zusammenfassung nicht regenerieren: {}", e.getMessage());
            }
        });
    }

    /**
     * Regeneriert die Kontext-Zusammenfassung für einen Chat
     * Wird nach dem Löschen von Nachrichten aufgerufen
     */
    @Transactional
    public void regenerateContextSummary(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat nicht gefunden: " + chatId));

        // Lade verbleibende Nachrichten
        List<Message> remainingMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        // Lade Dokumente
        List<ChatDocument> documents = chatDocumentRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        if (remainingMessages.isEmpty() && documents.isEmpty()) {
            // Kein Kontext mehr vorhanden
            chat.setContextSummary(null);
            chatRepository.save(chat);
            log.info("Chat {} hat keinen Kontext mehr - Zusammenfassung gelöscht", chatId);
            return;
        }

        // Baue Kontext für Zusammenfassung
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Fasse den folgenden Chat-Verlauf kurz zusammen. ");
        contextBuilder.append("Behalte alle wichtigen Fakten, Themen und den fachlichen Kontext bei.\n\n");

        // Dokumente hinzufügen
        if (!documents.isEmpty()) {
            contextBuilder.append("=== DOKUMENTE ===\n");
            for (ChatDocument doc : documents) {
                contextBuilder.append("Dokument '").append(doc.getFileName()).append("': ");
                // Nur erste 500 Zeichen pro Dokument für Zusammenfassung
                String content = doc.getContent();
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                contextBuilder.append(content).append("\n\n");
            }
        }

        // Nachrichten hinzufügen
        if (!remainingMessages.isEmpty()) {
            contextBuilder.append("=== CHAT-VERLAUF ===\n");
            for (Message msg : remainingMessages) {
                String role = msg.getRole() == Message.MessageRole.USER ? "Benutzer" : "Experte";
                contextBuilder.append(role).append(": ").append(msg.getContent()).append("\n\n");
            }
        }

        contextBuilder.append("\n=== AUFGABE ===\n");
        contextBuilder.append("Erstelle eine kurze Zusammenfassung (max. 200 Wörter) die folgendes enthält:\n");
        contextBuilder.append("1. Das Hauptthema/Problem des Gesprächs\n");
        contextBuilder.append("2. Wichtige Fakten aus den Dokumenten\n");
        contextBuilder.append("3. Den fachlichen Kontext (z.B. rechtlich, medizinisch, steuerlich)\n");
        contextBuilder.append("4. Offene Fragen oder nächste Schritte\n");

        try {
            // Verwende ein schnelles Modell für die Zusammenfassung
            String summaryModel = "llama3.2:3b";  // Schnelles kleines Modell
            String summary = llmProviderService.chat(summaryModel, contextBuilder.toString(),
                "Du bist ein Assistent der Chat-Verläufe zusammenfasst. Antworte nur mit der Zusammenfassung, ohne Einleitung.", null);

            if (summary != null && !summary.isBlank()) {
                chat.setContextSummary(summary.trim());
                chatRepository.save(chat);
                log.info("Kontext-Zusammenfassung für Chat {} regeneriert ({} Zeichen)", chatId, summary.length());
            }
        } catch (Exception e) {
            log.warn("Fehler bei Kontext-Zusammenfassung für Chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Get chat statistics
     */
    @Transactional(readOnly = true)
    public StatsResponse getChatStats(Long chatId) {
        Integer totalTokens = messageRepository.sumTokensByChatId(chatId);
        Long messageCount = messageRepository.countByChatId(chatId);

        return new StatsResponse(
                totalTokens != null ? totalTokens.longValue() : 0L,
                messageCount != null ? messageCount.intValue() : 0,
                1
        );
    }

    /**
     * Get global statistics
     */
    @Transactional(readOnly = true)
    public StatsResponse getGlobalStats() {
        GlobalStats stats = globalStatsRepository.findFirstByOrderByIdAsc()
                .orElse(new GlobalStats());

        long chatCount = chatRepository.count();

        return new StatsResponse(
                stats.getTotalTokens(),
                stats.getTotalMessages(),
                (int) chatCount
        );
    }

    /**
     * Update global statistics
     */
    private void updateGlobalStats(int tokens) {
        GlobalStats stats = globalStatsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    GlobalStats newStats = new GlobalStats();
                    return globalStatsRepository.save(newStats);
                });

        stats.incrementStats(tokens);
        globalStatsRepository.save(stats);
    }

    /**
     * Map Chat entity to ChatDTO
     */
    private ChatDTO mapToChatDTO(Chat chat) {
        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setTitle(chat.getTitle());
        dto.setModel(chat.getModel());
        dto.setCreatedAt(chat.getCreatedAt());
        dto.setUpdatedAt(chat.getUpdatedAt());

        // Add project info if chat is assigned to a project
        if (chat.getProject() != null) {
            dto.setProjectId(chat.getProject().getId());
            dto.setProjectName(chat.getProject().getName());

            // Calculate total tokens from project context files
            int projectTokens = chat.getProject().getContextFiles().stream()
                    .mapToInt(ContextFile::estimateTokens)
                    .sum();
            dto.setProjectTokens(projectTokens);

            // Calculate total tokens from ALL chats in the project
            Integer projectTotalChatTokens = messageRepository.sumTokensByProjectId(chat.getProject().getId());
            dto.setProjectTotalChatTokens(projectTotalChatTokens != null ? projectTotalChatTokens : 0);

            // Count chats in project
            dto.setProjectChatCount(chat.getProject().getChats().size());
        }

        // Load messages
        List<MessageDTO> messageDTOs = messageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId())
                .stream()
                .map(this::mapToMessageDTO)
                .collect(Collectors.toList());
        dto.setMessages(messageDTOs);

        // Calculate total tokens
        Integer totalTokens = messageRepository.sumTokensByChatId(chat.getId());
        dto.setTotalTokens(totalTokens != null ? totalTokens : 0);

        // Expert info
        dto.setExpertId(chat.getExpertId());

        // Expert Mode info
        dto.setActiveExpertModeId(chat.getActiveExpertModeId());
        dto.setActiveExpertModeName(chat.getActiveExpertModeName() != null ? chat.getActiveExpertModeName() : "Allgemein");

        return dto;
    }

    /**
     * Map Message entity to MessageDTO
     */
    private MessageDTO mapToMessageDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setTokens(message.getTokens());
        dto.setModelName(message.getModelName());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setAttachments(message.getAttachments());
        dto.setDownloadUrl(message.getDownloadUrl());  // For document downloads
        return dto;
    }

    /**
     * Generate a title from the first message (first 3-4 words or first sentence)
     */
    private String generateTitleFromMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "New Chat";
        }

        // Clean up the message
        String cleaned = message.trim();

        // Take first sentence if it's short enough
        int firstPeriod = cleaned.indexOf('.');
        int firstQuestion = cleaned.indexOf('?');
        int firstExclamation = cleaned.indexOf('!');

        int endOfSentence = Math.min(
            firstPeriod != -1 ? firstPeriod : Integer.MAX_VALUE,
            Math.min(
                firstQuestion != -1 ? firstQuestion : Integer.MAX_VALUE,
                firstExclamation != -1 ? firstExclamation : Integer.MAX_VALUE
            )
        );

        if (endOfSentence != Integer.MAX_VALUE && endOfSentence <= 50) {
            return cleaned.substring(0, endOfSentence);
        }

        // Otherwise take first 3-4 words
        String[] words = cleaned.split("\\s+");
        int wordCount = Math.min(4, words.length);
        StringBuilder title = new StringBuilder();

        for (int i = 0; i < wordCount; i++) {
            if (i > 0) title.append(" ");
            title.append(words[i]);
        }

        // Limit to 50 characters
        String result = title.toString();
        if (result.length() > 50) {
            result = result.substring(0, 47) + "...";
        }

        return result.isEmpty() ? "New Chat" : result;
    }

    /**
     * Convert Markdown to HTML with full formatting support
     */
    private String convertToHtml(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return plainText;
        }

        // Step 1: Extract and protect code blocks
        java.util.List<String> codeBlocks = new java.util.ArrayList<>();
        java.util.regex.Pattern codeBlockPattern = java.util.regex.Pattern.compile("```([\\s\\S]*?)```");
        java.util.regex.Matcher codeBlockMatcher = codeBlockPattern.matcher(plainText);
        StringBuffer protectedBuffer = new StringBuffer();

        while (codeBlockMatcher.find()) {
            codeBlocks.add(codeBlockMatcher.group());
            codeBlockMatcher.appendReplacement(protectedBuffer, "###CODEBLOCK" + (codeBlocks.size() - 1) + "###");
        }
        codeBlockMatcher.appendTail(protectedBuffer);
        String protectedText = protectedBuffer.toString();

        // Step 2: Extract and protect inline code
        java.util.List<String> inlineCodes = new java.util.ArrayList<>();
        java.util.regex.Pattern inlineCodePattern = java.util.regex.Pattern.compile("`([^`]+)`");
        java.util.regex.Matcher inlineCodeMatcher = inlineCodePattern.matcher(protectedText);
        StringBuffer protectedBuffer2 = new StringBuffer();

        while (inlineCodeMatcher.find()) {
            inlineCodes.add(inlineCodeMatcher.group(1));
            inlineCodeMatcher.appendReplacement(protectedBuffer2, "###INLINECODE" + (inlineCodes.size() - 1) + "###");
        }
        inlineCodeMatcher.appendTail(protectedBuffer2);
        protectedText = protectedBuffer2.toString();

        // Step 3: Escape HTML special characters
        String escaped = protectedText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        // Step 4: Convert Markdown to HTML (line by line to handle headings, lists, etc.)
        String[] lines = escaped.split("\n");
        StringBuilder htmlBuilder = new StringBuilder();
        boolean inList = false;
        boolean inOrderedList = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Handle headings (# ## ###)
            if (trimmed.startsWith("### ")) {
                htmlBuilder.append("<h3>").append(trimmed.substring(4)).append("</h3>\n");
            } else if (trimmed.startsWith("## ")) {
                htmlBuilder.append("<h2>").append(trimmed.substring(3)).append("</h2>\n");
            } else if (trimmed.startsWith("# ")) {
                htmlBuilder.append("<h1>").append(trimmed.substring(2)).append("</h1>\n");
            }
            // Handle unordered lists (- or *)
            else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) {
                    htmlBuilder.append("<ul>\n");
                    inList = true;
                }
                htmlBuilder.append("<li>").append(trimmed.substring(2)).append("</li>\n");
            }
            // Handle ordered lists (1. 2. 3.)
            else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                if (!inOrderedList) {
                    htmlBuilder.append("<ol>\n");
                    inOrderedList = true;
                }
                String content = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                htmlBuilder.append("<li>").append(content).append("</li>\n");
            }
            // Handle blockquotes (> )
            else if (trimmed.startsWith("> ")) {
                htmlBuilder.append("<blockquote>").append(trimmed.substring(2)).append("</blockquote>\n");
            }
            // Handle horizontal rules (---)
            else if (trimmed.equals("---") || trimmed.equals("***")) {
                htmlBuilder.append("<hr>\n");
            }
            // Normal text
            else {
                // Close lists if needed
                if (inList && !trimmed.startsWith("- ") && !trimmed.startsWith("* ")) {
                    htmlBuilder.append("</ul>\n");
                    inList = false;
                }
                if (inOrderedList && !trimmed.matches("^\\d+\\.\\s+.*")) {
                    htmlBuilder.append("</ol>\n");
                    inOrderedList = false;
                }

                if (!trimmed.isEmpty()) {
                    htmlBuilder.append("<p>").append(line).append("</p>\n");
                } else {
                    htmlBuilder.append("<br>\n");
                }
            }
        }

        // Close any open lists
        if (inList) {
            htmlBuilder.append("</ul>\n");
        }
        if (inOrderedList) {
            htmlBuilder.append("</ol>\n");
        }

        String html = htmlBuilder.toString();

        // Step 5: Convert inline Markdown formatting
        // Bold: **text** or __text__
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("__(.+?)__", "<strong>$1</strong>");

        // Italic: *text* or _text_ (but not in already matched bold)
        html = html.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");
        html = html.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "<em>$1</em>");

        // Strikethrough: ~~text~~
        html = html.replaceAll("~~(.+?)~~", "<del>$1</del>");

        // Links: [text](url)
        html = html.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");

        // Step 6: Restore inline code
        for (int i = 0; i < inlineCodes.size(); i++) {
            String code = inlineCodes.get(i)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            html = html.replace("###INLINECODE" + i + "###",
                    "<code style=\"background: #f3f4f6; dark:background: #374151; padding: 0.2rem 0.4rem; " +
                    "border-radius: 0.25rem; font-family: monospace; font-size: 0.875em;\">" + code + "</code>");
        }

        // Step 7: Restore code blocks with proper language class for Highlight.js
        for (int i = 0; i < codeBlocks.size(); i++) {
            String codeBlock = codeBlocks.get(i);

            // Extract language and code - flexible pattern for newlines
            String langPattern = "```(\\w+)?\\s*([\\s\\S]*?)```";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(langPattern);
            java.util.regex.Matcher matcher = pattern.matcher(codeBlock);

            String formattedCode;
            if (matcher.find()) {
                String lang = matcher.group(1) != null ? matcher.group(1) : "";
                String code = matcher.group(2).trim(); // Trim whitespace

                // Escape HTML in code - but keep newlines intact for Highlight.js
                code = code.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;");

                // Build language class for Highlight.js
                String langClass = !lang.isEmpty() ? " class=\"language-" + lang + "\"" : "";

                // Format as HTML code block (no <br> tags, Highlight.js handles formatting)
                formattedCode = "<pre><code" + langClass + ">" + code + "</code></pre>";
            } else {
                // Fallback: treat entire block as code without language
                String code = codeBlock.replace("```", "").trim();
                code = code.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;");

                formattedCode = "<pre><code>" + code + "</code></pre>";
            }

            html = html.replace("###CODEBLOCK" + i + "###", formattedCode);
        }

        // Step 8: Clean up empty paragraphs
        html = html.replace("<p></p>", "");
        html = html.replace("<p><br>\n</p>", "");
        html = html.replaceAll("<p>\\s*</p>", "");

        return html;
    }

    /**
     * Check if AI response contains downloadable code and generate download
     *
     * @param userMessage The original user message
     * @param aiResponse  The AI response
     * @return Download URL if generated, null otherwise
     */
    private String checkAndGenerateDownload(String userMessage, String aiResponse) {
        try {
            // Check if user requested a downloadable project
            String lowerMessage = userMessage.toLowerCase();
            boolean wantsDownload = lowerMessage.contains("als zip") ||
                                    lowerMessage.contains("zum download") ||
                                    lowerMessage.contains("herunterladen") ||
                                    lowerMessage.contains("download") ||
                                    lowerMessage.contains("erstelle") && (
                                            lowerMessage.contains("projekt") ||
                                            lowerMessage.contains("maven") ||
                                            lowerMessage.contains("spring boot") ||
                                            lowerMessage.contains("application")
                                    );

            // Also check if response contains multiple code blocks (likely a project)
            int codeBlockCount = countCodeBlocks(aiResponse);
            boolean hasMultipleFiles = codeBlockCount >= 3;

            if (!wantsDownload && !hasMultipleFiles) {
                return null;
            }

            log.info("Detected download request - generating project ZIP (code blocks: {})", codeBlockCount);

            // Generate project from AI response
            Path projectPath = codeGeneratorService.generateProject(aiResponse);

            // Create ZIP
            String downloadId = UUID.randomUUID().toString();
            Path zipPath = Path.of(codeGeneratorService.getTempDirectory(), downloadId + ".zip");
            zipService.createZip(projectPath, zipPath);

            // Return download URL
            String downloadUrl = "/api/downloads/" + downloadId;
            log.info("Generated download: {}", downloadUrl);

            return downloadUrl;

        } catch (Exception e) {
            log.error("Failed to generate download", e);
            return null;
        }
    }

    /**
     * Count code blocks in markdown
     */
    private int countCodeBlocks(String markdown) {
        int count = 0;
        String[] lines = markdown.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                count++;
            }
        }
        return count / 2; // Each block has start and end
    }

    /**
     * Modifiziert die Benutzeranfrage wenn ein Dokument angefordert wird
     * Fügt Anweisungen hinzu, damit der Experte nur den Inhalt schreibt
     */
    public String enhanceMessageForDocumentRequest(String userMessage) {
        return enhanceMessageForDocumentRequest(userMessage, null);
    }

    /**
     * Modifiziert die Benutzeranfrage wenn ein Dokument angefordert wird
     * Fügt Anweisungen hinzu mit dem Experten-spezifischen Dokumentenverzeichnis
     */
    public String enhanceMessageForDocumentRequest(String userMessage, Long expertId) {
        DocumentGeneratorService.DocumentRequest docRequest =
            documentGeneratorService.detectDocumentRequest(userMessage);

        if (docRequest == null) {
            return userMessage; // Keine Änderung
        }

        // Füge klare Anweisungen hinzu - NUR den Brieftext schreiben!
        // Das UI zeigt den Speicherpfad automatisch an - der LLM soll das NICHT erwähnen!
        StringBuilder enhancement = new StringBuilder();
        enhancement.append("\n\n[SYSTEM-ANWEISUNG - STRIKT BEFOLGEN:\n\n");
        enhancement.append("Du schreibst NUR den reinen Brieftext - SONST NICHTS!\n\n");
        enhancement.append("⛔ ABSOLUT VERBOTEN:\n");
        enhancement.append("- KEINE Emojis! (Professionelle Geschäftsbriefe enthalten niemals Emojis)\n");
        enhancement.append("- KEINE URLs oder Links generieren (NIEMALS example.com, localhost, oder ähnliches!)\n");
        enhancement.append("- KEINE Download-Hinweise, Pfadangaben oder Speicherorte erwähnen!\n");
        enhancement.append("- KEINE Sätze wie 'Das Dokument wurde gespeichert' oder ähnliches!\n");
        enhancement.append("- KEINE Anleitungen zur Dateierstellung\n");
        enhancement.append("- KEIN Markdown, XML, HTML oder Codeblöcke\n");
        enhancement.append("- KEINE Erklärungen was du tust oder was das System tut\n");
        enhancement.append("- KEINE Erwähnung von Dateiformaten (.odt, .docx, .pdf)\n");
        enhancement.append("- NICHTS nach der Unterschrift!\n\n");
        enhancement.append("✅ NUR DAS IST ERLAUBT:\n");
        enhancement.append("- Der reine Brieftext in normaler Schrift\n");
        enhancement.append("- Beginne DIREKT mit der Anrede ('Sehr geehrte...', 'Guten Tag...')\n");
        enhancement.append("- Ende mit Grußformel und Unterschrift - DANN SOFORT AUFHÖREN!]");

        return userMessage + enhancement.toString();
    }

    /**
     * Check if user requested a document (Brief, PDF) and generate it
     *
     * @param userMessage The user's message
     * @param aiResponse  The AI's response (content for the document)
     * @param expertId    The expert ID (for letterhead etc.)
     * @return Download URL if generated, null otherwise
     */
    private String checkAndGenerateDocument(String userMessage, String aiResponse, Long expertId) {
        try {
            // Detect document request
            DocumentGeneratorService.DocumentRequest docRequest =
                documentGeneratorService.detectDocumentRequest(userMessage);

            if (docRequest == null) {
                return null; // No document request detected
            }

            // Get expert for letterhead (oder Default-Werte)
            Expert expert = null;
            if (expertId != null) {
                expert = expertRepository.findById(expertId).orElse(null);
            }

            // Wenn kein Experte, erstelle Default-Experten für Briefkopf
            if (expert == null) {
                expert = new Expert();
                expert.setName("Fleet Navigator");
                expert.setRole("KI-Assistent");
                log.info("Kein Experte ausgewählt - verwende Default-Briefkopf");
            }

            log.info("Document request detected: {} ({})", docRequest.type(), docRequest.purpose());

            // DISABLED: Fleet-Mate document generation creates only plain text files
            // instead of proper ODT/DOCX format. Using DocumentGeneratorService instead
            // which creates real ODT files with Liberation Sans font and DIN 5008 format.
            //
            // See: Fleet-Mate-Linux/internal/commands/file_search.go line 1241:
            // "This creates a simple text file - for proper ODT we'd need the full ZIP structure"
            //
            // Re-enable when Fleet-Mate implements proper ODT generation.
            /*
            List<FleetMateService.MateInfo> onlineMates = fleetMateService.getOnlineMates();
            if (!onlineMates.isEmpty() && expert.getDocumentDirectory() != null
                    && !expert.getDocumentDirectory().isBlank()) {

                // Find the right Fleet-Mate to use
                String targetMateId = selectMateForDocumentGeneration(expert, onlineMates);

                if (targetMateId != null) {
                    String docSessionId = sendDocumentToFleetMate(
                        targetMateId,
                        aiResponse,
                        docRequest.type().name().toLowerCase(),
                        extractSubject(userMessage),
                        expert.getName(),
                        expert.getRole(),
                        expert.getDocumentDirectory()
                    );

                    if (docSessionId != null) {
                        log.info("Document sent to Fleet-Mate {} for local generation in ~/Dokumente/Fleet-Navigator/{}/",
                                targetMateId, expert.getDocumentDirectory());
                        // Return special marker with sessionId so frontend can track
                        return "fleet-mate://" + docSessionId;
                    }
                }
            }
            */

            // Generate document locally with download URL (proper ODT with Liberation Sans + DIN 5008)
            DocumentGeneratorService.GeneratedDocument doc;

            if (docRequest.type() == DocumentGeneratorService.DocumentType.ODT ||
                docRequest.type() == DocumentGeneratorService.DocumentType.DOCX) {
                // Generate letter in requested format (ODT or DOCX)
                doc = documentGeneratorService.generateLetter(
                    expert,
                    aiResponse,
                    null, // recipient - could be extracted from message
                    extractSubject(userMessage), // subject from user message
                    docRequest.type() // ODT oder DOCX
                );
            } else if (docRequest.type() == DocumentGeneratorService.DocumentType.PDF) {
                // Generate PDF summary
                doc = documentGeneratorService.generatePdfSummary(
                    expert,
                    aiResponse,
                    "Zusammenfassung"
                );
            } else {
                return null;
            }

            if (doc != null) {
                String downloadUrl = "/api/downloads/doc/" + doc.id();
                log.info("Document generated: {} -> {}", doc.filename(), downloadUrl);
                return downloadUrl;
            }

        } catch (Exception e) {
            log.error("Failed to generate document", e);
        }

        return null;
    }

    /**
     * Select the right Fleet-Mate for document generation
     *
     * Logic:
     * 1. If expert has preferredMateId and that mate is online → use it
     * 2. Filter for OS-type mates (start with "os-" or have "desktop" in name)
     * 3. If only one OS mate → use it
     * 4. If multiple OS mates → use first one (TODO: frontend selection)
     *
     * @param expert The expert requesting document generation
     * @param onlineMates List of online Fleet-Mates
     * @return The mate ID to use, or null if none suitable
     */
    private String selectMateForDocumentGeneration(Expert expert, List<FleetMateService.MateInfo> onlineMates) {
        // 1. Check if expert has a preferred mate that is online
        if (expert.getPreferredMateId() != null && !expert.getPreferredMateId().isBlank()) {
            for (FleetMateService.MateInfo mate : onlineMates) {
                if (mate.getMateId().equals(expert.getPreferredMateId())) {
                    log.debug("Using expert's preferred mate: {}", mate.getMateId());
                    return mate.getMateId();
                }
            }
            log.warn("Expert's preferred mate {} is not online", expert.getPreferredMateId());
        }

        // 2. Filter for OS-type mates (they have document generation capability)
        List<FleetMateService.MateInfo> osMates = onlineMates.stream()
                .filter(m -> m.getMateId().startsWith("os-") ||
                             m.getName().toLowerCase().contains("desktop") ||
                             m.getName().toLowerCase().contains("ubuntu") ||
                             m.getName().toLowerCase().contains("windows"))
                .toList();

        if (osMates.isEmpty()) {
            log.warn("No OS-type Fleet-Mate available for document generation");
            return null;
        }

        // 3. If only one → use it
        if (osMates.size() == 1) {
            log.debug("Using only available OS mate: {}", osMates.get(0).getMateId());
            return osMates.get(0).getMateId();
        }

        // 4. Multiple mates → use first one (TODO: let user select in settings)
        log.info("Multiple OS mates available ({}), using first: {}", osMates.size(), osMates.get(0).getMateId());
        return osMates.get(0).getMateId();
    }

    /**
     * Send document generation request to Fleet-Mate
     * Fleet-Mate will create the document and save it locally on user's computer
     *
     * @param mateId    The Fleet-Mate ID to send to
     * @param content   The document content (text from expert)
     * @param format    The format (odt, docx, pdf)
     * @param title     Document title/subject
     * @param expertName Expert name for letterhead
     * @param expertRole Expert role for letterhead
     * @param docDirectory Expert's document directory (e.g., "Roland")
     * @return sessionId if sent successfully, null otherwise
     */
    private String sendDocumentToFleetMate(String mateId, String content, String format,
                                            String title, String expertName, String expertRole,
                                            String docDirectory) {
        if (fleetMateWebSocketHandler == null) {
            log.warn("FleetMateWebSocketHandler not available");
            return null;
        }

        try {
            // Generate unique session ID for this document request
            String sessionId = java.util.UUID.randomUUID().toString();

            // Build payload for generate_document command
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("sessionId", sessionId);
            payload.put("content", content);
            payload.put("format", format);
            payload.put("title", title);
            payload.put("expertName", expertName);
            payload.put("expertRole", expertRole);
            payload.put("docDirectory", docDirectory);  // z.B. "Roland" -> ~/Dokumente/Fleet-Navigator/Roland/
            payload.put("openAfter", true);  // Öffne Dokument nach Erstellung

            MateCommand command = new MateCommand("generate_document", payload);
            fleetMateWebSocketHandler.sendCommandAuto(mateId, command);

            log.info("📄 Sent generate_document to Fleet-Mate {}: {} {} for {} (session: {})",
                    mateId, format, title, expertName, sessionId);
            return sessionId;

        } catch (Exception e) {
            log.error("Failed to send document to Fleet-Mate: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Handle document_generated response from Fleet-Mate
     * Called by FleetMateWebSocketHandler when Fleet-Mate confirms document creation
     *
     * @param sessionId The document session ID
     * @param filePath  The local file path where document was saved
     * @param success   Whether document was created successfully
     * @param error     Error message if failed
     */
    public void handleDocumentGenerated(String sessionId, String filePath, boolean success, String error) {
        log.info("📄 Document generated response: session={}, path={}, success={}", sessionId, filePath, success);

        if (success && filePath != null) {
            // Find the message with this session ID and update with real file path
            String oldDownloadUrl = "fleet-mate://" + sessionId;
            String newDownloadUrl = "fleet-mate://" + filePath;

            messageRepository.findByDownloadUrl(oldDownloadUrl).ifPresent(message -> {
                message.setDownloadUrl(newDownloadUrl);
                messageRepository.save(message);
                log.info("📄 Message {} updated with real file path: {}", message.getId(), filePath);
            });

            log.info("📄 Dokument erstellt: file://{}", filePath);
        } else {
            log.warn("Document generation failed: {}", error);
        }
    }

    /**
     * Extract a subject/topic from the user message
     */
    private String extractSubject(String message) {
        if (message == null) return "Beratungsschreiben";

        // Remove common phrases
        String cleaned = message
            .replaceAll("(?i)kannst du mir ", "")
            .replaceAll("(?i)bitte ", "")
            .replaceAll("(?i)schreib mir ", "")
            .replaceAll("(?i)erstell mir ", "")
            .replaceAll("(?i)formulier ", "")
            .replaceAll("(?i)einen brief ", "")
            .replaceAll("(?i)ein schreiben ", "")
            .replaceAll("(?i)als download ", "")
            .replaceAll("(?i)zum download ", "")
            .replaceAll("(?i)als docx ", "")
            .replaceAll("(?i)als word ", "")
            .trim();

        // Take first 50 chars
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 47) + "...";
        }

        return cleaned.isEmpty() ? "Beratungsschreiben" : cleaned;
    }

    /**
     * Extract URLs from a message
     * Detects http:// and https:// URLs
     */
    private List<String> extractUrls(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }

        List<String> urls = new java.util.ArrayList<>();
        // Regex für URLs (http:// und https://)
        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );

        java.util.regex.Matcher matcher = urlPattern.matcher(message);
        while (matcher.find()) {
            String url = matcher.group(1);
            // Entferne trailing Satzzeichen die nicht zur URL gehören
            while (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith("!") || url.endsWith("?")) {
                url = url.substring(0, url.length() - 1);
            }
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }

        return urls;
    }

    /**
     * Generate an expert summary PDF from a chat
     * The expert summarizes all questions and answers into a final report
     *
     * @param chatId   The chat ID
     * @param expertId The expert ID
     * @return PDF as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateExpertSummaryPdf(Long chatId, Long expertId) throws IOException {
        log.info("Generating expert summary PDF for chat {} with expert {}", chatId, expertId);

        // Get chat and messages
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat nicht gefunden: " + chatId));
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        if (messages.isEmpty()) {
            throw new IllegalArgumentException("Chat hat keine Nachrichten");
        }

        // Get expert
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Experte nicht gefunden: " + expertId));

        // Build conversation content for summarization
        StringBuilder conversationBuilder = new StringBuilder();
        conversationBuilder.append("=== GESPRÄCHSVERLAUF ===\n\n");

        for (Message msg : messages) {
            String role = msg.getRole() == MessageRole.USER ? "Mandant/Kunde" : expert.getName();
            conversationBuilder.append(role).append(":\n");
            conversationBuilder.append(msg.getContent()).append("\n\n");
        }

        // Build summarization prompt - Focus on SYNTHESIS, not chronological repetition
        String summaryPrompt = String.format("""
            Du bist %s, %s.

            WICHTIG: Erstelle einen ABSCHLUSSBERICHT - keine Wiedergabe des Gesprächsverlaufs!

            Deine Aufgabe ist es, das folgende Beratungsgespräch zu ANALYSIEREN und die WESENTLICHEN ERKENNTNISSE zusammenzufassen.

            REGELN:
            - KEINE chronologische Wiedergabe ("Zuerst wurde gefragt..., dann...")
            - KEINE Wiederholung von Fragen und Antworten
            - Stattdessen: Synthese der Kernpunkte und Schlussfolgerungen
            - Schreibe so, als würdest du einem Kollegen die Essenz des Falls erklären

            STRUKTUR DES BERICHTS:

            1. **Sachverhalt**
            Beschreibe in 2-3 Sätzen worum es geht. Was ist die Ausgangssituation?

            2. **Kernfragen**
            Welche zentralen Fragen wurden geklärt? (Stichpunkte)

            3. **Fachliche Einschätzung**
            Deine professionelle Bewertung der Situation. Was sind die wichtigsten rechtlichen/fachlichen Aspekte?

            4. **Empfehlungen**
            Konkrete Handlungsempfehlungen für den Mandanten/Kunden.

            5. **Offene Punkte** (falls vorhanden)
            Was muss noch geklärt werden? Welche Informationen fehlen?

            Schreibe prägnant und professionell. Maximal 1 Seite.

            %s
            """, expert.getName(), expert.getRole(), conversationBuilder.toString());

        // Generate summary using the expert's model
        String summary = llmProviderService.chat(
                expert.getBaseModel(),
                summaryPrompt,
                expert.getBasePrompt(),
                null
        );

        if (summary == null || summary.isBlank()) {
            throw new RuntimeException("Konnte keine Zusammenfassung generieren");
        }

        log.info("Generated summary with {} characters", summary.length());

        // Generate PDF
        return generatePdfFromSummary(expert, summary, chat.getTitle());
    }

    /**
     * Generate a PDF document from the summary
     */
    private byte[] generatePdfFromSummary(Expert expert, String summary, String chatTitle) throws IOException {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        // Build HTML content
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Abschlussbericht - %s</title>
                <style>
                    @page {
                        margin: 2.5cm;
                        size: A4;
                    }
                    body {
                        font-family: 'DejaVu Sans', Arial, sans-serif;
                        font-size: 11pt;
                        line-height: 1.6;
                        color: #333;
                    }
                    .header {
                        border-bottom: 3px solid #6B21A8;
                        padding-bottom: 20px;
                        margin-bottom: 30px;
                    }
                    .expert-name {
                        font-size: 24pt;
                        font-weight: bold;
                        color: #6B21A8;
                        margin: 0;
                    }
                    .expert-role {
                        font-size: 14pt;
                        color: #666;
                        margin: 5px 0 0 0;
                    }
                    .report-title {
                        font-size: 18pt;
                        font-weight: bold;
                        color: #333;
                        margin: 30px 0 20px 0;
                        text-align: center;
                    }
                    .meta-info {
                        background: #f5f3ff;
                        border-left: 4px solid #6B21A8;
                        padding: 15px;
                        margin-bottom: 30px;
                    }
                    .meta-info p {
                        margin: 5px 0;
                        font-size: 10pt;
                    }
                    .content {
                        text-align: justify;
                    }
                    .content p {
                        margin-bottom: 15px;
                    }
                    .footer {
                        position: fixed;
                        bottom: 0;
                        left: 0;
                        right: 0;
                        text-align: center;
                        font-size: 9pt;
                        color: #999;
                        border-top: 1px solid #ddd;
                        padding-top: 10px;
                    }
                    h2 {
                        color: #6B21A8;
                        font-size: 14pt;
                        margin-top: 25px;
                        margin-bottom: 10px;
                        border-bottom: 1px solid #e5e5e5;
                        padding-bottom: 5px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <p class="expert-name">%s</p>
                    <p class="expert-role">%s</p>
                </div>

                <div class="report-title">ABSCHLUSSBERICHT</div>

                <div class="meta-info">
                    <p><strong>Datum:</strong> %s um %s Uhr</p>
                    <p><strong>Betreff:</strong> %s</p>
                </div>

                <div class="content">
                    %s
                </div>

                <div class="footer">
                    Erstellt von Fleet Navigator | %s, %s | Seite 1
                </div>
            </body>
            </html>
            """,
                FleetUtils.escapeHtml(chatTitle),
                FleetUtils.escapeHtml(expert.getName()),
                FleetUtils.escapeHtml(expert.getRole()),
                now.format(dateFormatter),
                now.format(timeFormatter),
                FleetUtils.escapeHtml(chatTitle),
                formatSummaryAsHtml(summary),
                FleetUtils.escapeHtml(expert.getName()),
                FleetUtils.escapeHtml(expert.getRole())
        );

        // Convert HTML to PDF using iText
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        com.itextpdf.html2pdf.HtmlConverter.convertToPdf(html, outputStream);

        byte[] pdfBytes = outputStream.toByteArray();
        log.info("Generated PDF with {} bytes", pdfBytes.length);

        return pdfBytes;
    }

    /**
     * Format summary text as HTML paragraphs
     */
    private String formatSummaryAsHtml(String summary) {
        if (summary == null || summary.isBlank()) {
            return "<p>Keine Zusammenfassung verfügbar.</p>";
        }

        // Escape HTML special characters
        String escaped = FleetUtils.escapeHtml(summary);

        // Convert bold markers (**text**) to HTML - do this before line processing
        escaped = escaped.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");

        // Convert numbered sections to HTML headings
        StringBuilder htmlBuilder = new StringBuilder();
        String[] lines = escaped.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Check for numbered headings (1. **Title** or 1. Title)
            if (trimmed.matches("^\\d+\\.\\s+<strong>.*</strong>.*")) {
                // Already has bold tags from conversion
                String content = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                htmlBuilder.append("<h2>").append(content).append("</h2>\n");
            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                // Numbered line without bold
                String content = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                // Check if it looks like a heading (ends with no punctuation or colon)
                if (!content.endsWith(".") && !content.endsWith("?") && !content.endsWith("!")) {
                    htmlBuilder.append("<h2>").append(content).append("</h2>\n");
                } else {
                    htmlBuilder.append("<p>").append(trimmed).append("</p>\n");
                }
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                // Bullet points
                htmlBuilder.append("<p>• ").append(trimmed.substring(2)).append("</p>\n");
            } else {
                // Normal paragraph
                htmlBuilder.append("<p>").append(trimmed).append("</p>\n");
            }
        }

        return htmlBuilder.toString();
    }

    // escapeHtml jetzt zentral in FleetUtils

    // ==================== EXPERT MODE DETECTION ====================

    /**
     * Ergebnis der Modus-Erkennung
     */
    public record ExpertModeResult(
        ExpertMode mode,
        boolean modeChanged,
        String switchNotice
    ) {}

    /**
     * Erkennt den passenden Experten-Modus basierend auf Keywords in der Nachricht.
     * Speichert den Modus im Chat und gibt einen Hinweis zurück wenn der Modus wechselt.
     *
     * @param chat Der aktuelle Chat
     * @param expertId ID des ausgewählten Experten (null = kein Experte)
     * @param message Die User-Nachricht
     * @return ExpertModeResult mit Modus, ob gewechselt wurde, und Hinweis-Text
     */
    @Transactional
    public ExpertModeResult detectAndUpdateExpertMode(Chat chat, Long expertId, String message) {
        if (expertId == null) {
            // Kein Experte ausgewählt - kein Modus
            return new ExpertModeResult(null, false, null);
        }

        // Lade alle aktiven Modi des Experten
        List<ExpertMode> modes = expertModeRepository.findActiveByExpertIdOrderByPriority(expertId);
        if (modes.isEmpty()) {
            return new ExpertModeResult(null, false, null);
        }

        // Suche passenden Modus basierend auf Keywords
        String lowerMessage = message.toLowerCase();
        ExpertMode detectedMode = null;

        for (ExpertMode mode : modes) {
            if (matchesKeywords(mode, lowerMessage)) {
                detectedMode = mode;
                break;
            }
        }

        // Kein Keyword-Match? Verwende aktuellen Chat-Modus oder Default
        if (detectedMode == null) {
            if (chat.getActiveExpertModeId() != null) {
                // Behalte aktuellen Modus bei
                detectedMode = expertModeRepository.findById(chat.getActiveExpertModeId()).orElse(null);
                if (detectedMode != null) {
                    return new ExpertModeResult(detectedMode, false, null);
                }
            }
            // Fallback: Erster Modus (normalerweise "Allgemein")
            detectedMode = modes.get(0);
        }

        // Prüfe ob Modus gewechselt hat
        Long previousModeId = chat.getActiveExpertModeId();
        boolean modeChanged = previousModeId == null || !previousModeId.equals(detectedMode.getId());

        String switchNotice = null;
        if (modeChanged) {
            // Modus im Chat speichern
            chat.setActiveExpertModeId(detectedMode.getId());
            chat.setActiveExpertModeName(detectedMode.getName());
            chatRepository.save(chat);

            // Hinweis nur wenn nicht "Allgemein" und nicht erster Wechsel von null
            if (!"Allgemein".equals(detectedMode.getName()) || previousModeId != null) {
                switchNotice = "📋 *Ich wechsle in den Modus " + detectedMode.getName() + "*\n\n";
                log.info("Experten-Modus gewechselt: {} → {} (Chat {})",
                    previousModeId != null ? chat.getActiveExpertModeName() : "Allgemein",
                    detectedMode.getName(),
                    chat.getId());
            }
        }

        return new ExpertModeResult(detectedMode, modeChanged, switchNotice);
    }

    /**
     * Prüft ob die Nachricht Keywords des Modus enthält
     */
    private boolean matchesKeywords(ExpertMode mode, String lowerMessage) {
        String[] keywords = mode.getKeywordsArray();
        if (keywords == null || keywords.length == 0) {
            return false;
        }
        for (String keyword : keywords) {
            String trimmedKeyword = keyword.trim().toLowerCase();
            if (!trimmedKeyword.isEmpty() && lowerMessage.contains(trimmedKeyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Baut den System-Prompt mit Experten-Basis + Modus-Addition
     */
    public String buildExpertSystemPrompt(Expert expert, ExpertMode mode) {
        if (expert == null) {
            return null;
        }

        StringBuilder prompt = new StringBuilder(expert.getBasePrompt());

        if (mode != null && mode.getPromptAddition() != null && !mode.getPromptAddition().isBlank()) {
            prompt.append("\n\n=== AKTUELLER MODUS: ")
                  .append(mode.getName())
                  .append(" ===\n")
                  .append(mode.getPromptAddition());
        }

        return prompt.toString();
    }

}
