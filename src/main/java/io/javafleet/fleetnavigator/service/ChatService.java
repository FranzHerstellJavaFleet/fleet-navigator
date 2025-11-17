package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.model.Chat;
import io.javafleet.fleetnavigator.model.ContextFile;
import io.javafleet.fleetnavigator.model.GlobalStats;
import io.javafleet.fleetnavigator.model.Message;
import io.javafleet.fleetnavigator.model.Project;
import io.javafleet.fleetnavigator.model.Message.MessageRole;
import io.javafleet.fleetnavigator.repository.ChatRepository;
import io.javafleet.fleetnavigator.repository.GlobalStatsRepository;
import io.javafleet.fleetnavigator.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Path;
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
    private final GlobalStatsRepository globalStatsRepository;
    private final LLMProviderService llmProviderService; // Uses java-llama-cpp provider
    private final ModelSelectionService modelSelectionService;
    private final SettingsService settingsService;
    private final CodeGeneratorService codeGeneratorService;
    private final ZipService zipService;

    // Thread pool for handling streaming requests
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Create a new chat
     */
    @Transactional
    public ChatDTO createNewChat(NewChatRequest request) {
        Chat chat = new Chat();
        chat.setTitle(request.getTitle() != null ? request.getTitle() : "New Chat");
        chat.setModel(request.getModel() != null ? request.getModel() : "llama3.2:3b");

        chat = chatRepository.save(chat);
        log.info("Created new chat: {}", chat.getId());

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

        // Add document context if provided
        if (request.getDocumentContext() != null && !request.getDocumentContext().isEmpty()) {
            completeMessageBuilder.append("Document content:\n\n");
            completeMessageBuilder.append(request.getDocumentContext());
            completeMessageBuilder.append("\n\n---\n\n");
        }

        // Add current user message
        completeMessageBuilder.append("User question: ");
        completeMessageBuilder.append(request.getMessage());

        String completeMessage = completeMessageBuilder.toString();

        // Save user message
        Message userMessage = new Message();
        userMessage.setChat(chat);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.getMessage());  // Save only user's message, not full context
        userMessage.setTokens(llmProviderService.estimateTokens(completeMessage));
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

        // Get response from LLM provider (with request ID for cancellation)
        String response;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // Use vision API for images
            response = llmProviderService.chatWithVision(
                    modelToUse,
                    completeMessage,
                    request.getImages(),
                    request.getSystemPrompt(),
                    requestId
            );
        } else {
            // Use regular generate API
            response = llmProviderService.chat(
                    modelToUse,
                    completeMessage,
                    request.getSystemPrompt(),
                    requestId
            );
        }

        // Don't convert to HTML - frontend will handle markdown rendering
        // Save assistant message with raw markdown
        Message assistantMessage = new Message();
        assistantMessage.setChat(chat);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(response);  // Store raw markdown
        assistantMessage.setTokens(llmProviderService.estimateTokens(response));
        assistantMessage.setModelName(request.getModel());  // Store which model was used
        assistantMessage = messageRepository.save(assistantMessage);

        // Update global stats
        updateGlobalStats(assistantMessage.getTokens());

        log.info("Chat {} - Sent message and received response ({} tokens)",
                chat.getId(), assistantMessage.getTokens());

        // Check if response contains downloadable code and auto-generate download
        String downloadUrl = checkAndGenerateDownload(request.getMessage(), response);

        ChatResponse chatResponse = new ChatResponse(
                chat.getId(),
                response,  // Return raw markdown for frontend to render
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
            chat = chatRepository.findById(request.getChatId())
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + request.getChatId()));

            // Eagerly load project context while Hibernate session is still active
            if (chat.getProject() != null) {
                Project project = chat.getProject();
                // Access lazy-loaded collections to initialize them
                if (!project.getContextFiles().isEmpty()) {
                    projectContext = project.getCombinedContext();
                    projectName = project.getName();
                    projectContextSize = project.getTotalContextSize();
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

        // Make final variables for use in lambda
        final Chat finalChat = chat;
        final String finalProjectContext = projectContext;
        final String finalProjectName = projectName;
        final long finalProjectContextSize = projectContextSize;
        final String finalModel = modelToUse;
        final String finalVisionModel = modelSettings.getVisionModel();
        final boolean finalVisionChainingEnabled = modelSettings.isVisionChainingEnabled();
        final boolean finalUseSmartSelectionForVision = useSmartSelectionForVision;

        executorService.execute(() -> {
            try {

                // Load previous messages from database to maintain conversation context
                List<Message> previousMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(finalChat.getId());

                // Build complete message (with chat history, project context and document context if provided)
                StringBuilder completeMessageBuilder = new StringBuilder();

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

                // Add document context if provided
                if (request.getDocumentContext() != null && !request.getDocumentContext().isEmpty()) {
                    completeMessageBuilder.append("Document content:\n\n");
                    completeMessageBuilder.append(request.getDocumentContext());
                    completeMessageBuilder.append("\n\n---\n\n");
                }

                // Add current user message
                completeMessageBuilder.append("User question: ");
                completeMessageBuilder.append(request.getMessage());

                String completeMessage = completeMessageBuilder.toString();

                // Save user message
                Message userMessage = new Message();
                userMessage.setChat(finalChat);
                userMessage.setRole(MessageRole.USER);
                userMessage.setContent(request.getMessage());  // Save only user's message, not full context
                userMessage.setTokens(llmProviderService.estimateTokens(completeMessage));
                messageRepository.save(userMessage);

                // Auto-generate title from first message if still "New Chat"
                if (finalChat.getTitle().equals("New Chat")) {
                    finalChat.setTitle(generateTitleFromMessage(request.getMessage()));
                    chatRepository.save(finalChat);
                }

                // Send initial event with chat ID and request ID
                log.info("Starting streaming for chat {} with model {}", finalChat.getId(), finalModel);
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"chatId\":" + finalChat.getId() + ",\"requestId\":\"" + requestId + "\"}"));

                // Collect full response for database storage
                StringBuilder fullResponse = new StringBuilder();

                // Stream response from LLM provider
                String finalCompleteMessage = completeMessage;
                log.info("Calling LLM provider chatStream for model: {}", finalModel);
                log.info("📊 Request parameters - maxTokens: {}, temp: {}, topP: {}, topK: {}, repeatPenalty: {}",
                    request.getMaxTokens(), request.getTemperature(), request.getTopP(),
                    request.getTopK(), request.getRepeatPenalty());

                // Use vision streaming if images are provided
                if (request.getImages() != null && !request.getImages().isEmpty()) {
                    // Check if Vision-Chaining is enabled (from settings or request)
                    boolean visionChainingEnabled = finalVisionChainingEnabled ||
                        (request.getVisionChainEnabled() != null && request.getVisionChainEnabled());

                    if (visionChainingEnabled) {
                        // Vision-Chaining: Vision Model → Haupt-Model
                        log.info("Vision-Chaining enabled: Vision={}, Main={} (Smart Selection: {})",
                                finalVisionModel, finalModel, finalUseSmartSelectionForVision);

                        llmProviderService.chatStreamWithVisionChaining(
                                request.getVisionModel() != null ? request.getVisionModel() : finalVisionModel,  // Vision Model from settings or request
                                finalModel,  // Haupt-Model (smart selected or user chosen)
                                finalCompleteMessage,
                                request.getImages(),
                                request.getSystemPrompt(),
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
                    } else {
                        // Normal Vision Model (ohne Chaining)
                        llmProviderService.chatStreamWithVision(
                                finalModel,  // Smart selected or user chosen
                                finalCompleteMessage,
                                request.getImages(),
                                request.getSystemPrompt(),
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
                            request.getSystemPrompt(),
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
                            request.getMaxTokens(),      // Pass maxTokens from frontend
                            request.getTemperature(),    // Pass temperature
                            request.getTopP(),           // Pass topP
                            request.getTopK(),           // Pass topK
                            request.getRepeatPenalty()   // Pass repeatPenalty
                    );
                }
                log.info("LLM chatStream completed. Full response length: {}", fullResponse.length());

                // Save assistant message to database
                Message assistantMessage = new Message();
                assistantMessage.setChat(finalChat);
                assistantMessage.setRole(MessageRole.ASSISTANT);
                assistantMessage.setContent(fullResponse.toString());
                assistantMessage.setTokens(llmProviderService.estimateTokens(fullResponse.toString()));
                assistantMessage.setModelName(request.getModel());  // Store which model was used
                messageRepository.save(assistantMessage);

                // Update global stats
                updateGlobalStats(assistantMessage.getTokens());

                // Send completion event (only if not already completed)
                if (!isCompleted[0]) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("{\"tokens\":" + assistantMessage.getTokens() + "}"));
                        isCompleted[0] = true;
                        emitter.complete();
                        log.info("Streaming completed for chat {}", finalChat.getId());
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
                        // Create user-friendly error message
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("404")) {
                            errorMessage = "⚠️ LLM-Provider ist nicht erreichbar!\n\n" +
                                    "Bitte stelle sicher, dass llama.cpp läuft:\n" +
                                    "• Fleet Navigator sollte llama.cpp automatisch starten\n" +
                                    "• Prüfe die Logs für Fehler beim Start\n" +
                                    "• Stelle sicher, dass Modelle im ./models Verzeichnis vorhanden sind";
                        } else if (errorMessage != null && errorMessage.contains("Connection refused")) {
                            errorMessage = "⚠️ Verbindung zum LLM-Provider fehlgeschlagen!\n\n" +
                                    "Der LLM-Provider läuft nicht.\n" +
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
     * Delete a chat
     */
    @Transactional
    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
        log.info("Deleted chat: {}", chatId);
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
}
