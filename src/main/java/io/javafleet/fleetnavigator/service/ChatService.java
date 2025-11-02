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
    private final OllamaService ollamaService;
    private final ModelSelectionService modelSelectionService;
    private final SettingsService settingsService;

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
     * Send a message and get response from Ollama
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

        // Build complete message (with project context and document context if provided)
        StringBuilder completeMessageBuilder = new StringBuilder();

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

        // Add user message
        completeMessageBuilder.append("User question: ");
        completeMessageBuilder.append(request.getMessage());

        String completeMessage = completeMessageBuilder.toString();

        // Save user message
        Message userMessage = new Message();
        userMessage.setChat(chat);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.getMessage());  // Save only user's message, not full context
        userMessage.setTokens(ollamaService.estimateTokens(completeMessage));
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

        // Get response from Ollama (with request ID for cancellation)
        String response;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // Use vision API for images
            response = ollamaService.chatWithVision(
                    modelToUse,
                    completeMessage,
                    request.getImages(),
                    request.getSystemPrompt(),
                    requestId
            );
        } else {
            // Use regular generate API
            response = ollamaService.chat(
                    modelToUse,
                    completeMessage,
                    request.getSystemPrompt(),
                    requestId
            );
        }

        // Save assistant message
        Message assistantMessage = new Message();
        assistantMessage.setChat(chat);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(response);
        assistantMessage.setTokens(ollamaService.estimateTokens(response));
        assistantMessage = messageRepository.save(assistantMessage);

        // Update global stats
        updateGlobalStats(assistantMessage.getTokens());

        log.info("Chat {} - Sent message and received response ({} tokens)",
                chat.getId(), assistantMessage.getTokens());

        return new ChatResponse(
                chat.getId(),
                response,
                assistantMessage.getTokens(),
                chat.getModel(),
                requestId  // Include request ID for tracking
        );
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
            ollamaService.cancelRequest(requestId);
        });

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for request: {}", requestId);
            isCompleted[0] = true;
        });

        emitter.onError((ex) -> {
            log.error("SSE emitter error for request: {}", requestId, ex);
            isCompleted[0] = true;
            ollamaService.cancelRequest(requestId);
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

                // Build complete message (with project context and document context if provided)
                StringBuilder completeMessageBuilder = new StringBuilder();

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

                // Add user message
                completeMessageBuilder.append("User question: ");
                completeMessageBuilder.append(request.getMessage());

                String completeMessage = completeMessageBuilder.toString();

                // Save user message
                Message userMessage = new Message();
                userMessage.setChat(finalChat);
                userMessage.setRole(MessageRole.USER);
                userMessage.setContent(request.getMessage());  // Save only user's message, not full context
                userMessage.setTokens(ollamaService.estimateTokens(completeMessage));
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

                // Stream response from Ollama
                String finalCompleteMessage = completeMessage;
                log.info("Calling Ollama chatStream for model: {}", finalModel);
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

                        ollamaService.chatStreamWithVisionChaining(
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
                        ollamaService.chatStreamWithVision(
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
                    ollamaService.chatStream(
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
                log.info("Ollama chatStream completed. Full response length: {}", fullResponse.length());

                // Save assistant message to database
                Message assistantMessage = new Message();
                assistantMessage.setChat(finalChat);
                assistantMessage.setRole(MessageRole.ASSISTANT);
                assistantMessage.setContent(fullResponse.toString());
                assistantMessage.setTokens(ollamaService.estimateTokens(fullResponse.toString()));
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
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"" + e.getMessage() + "\"}"));
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
        return ollamaService.cancelRequest(requestId);
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
}
