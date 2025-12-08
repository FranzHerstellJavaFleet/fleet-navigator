package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * REST Controller for chat operations
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat/send - Send a message (non-streaming)
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        try {
            log.info("Sending message to chat: {}", request.getChatId());
            ChatResponse response = chatService.sendMessage(request);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error communicating with Ollama", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/chat/send-stream - Send a message with STREAMING
     */
    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@RequestBody ChatRequest request) {
        log.info("Sending streaming message to chat: {}", request.getChatId());
        return chatService.sendMessageStream(request);
    }

    /**
     * POST /api/chat/new - Create a new chat
     */
    @PostMapping("/new")
    public ResponseEntity<ChatDTO> createNewChat(@RequestBody NewChatRequest request) {
        log.info("Creating new chat: {}", request.getTitle());
        ChatDTO chat = chatService.createNewChat(request);
        return ResponseEntity.ok(chat);
    }

    /**
     * GET /api/chat/history/{chatId} - Get chat history
     */
    @GetMapping("/history/{chatId}")
    public ResponseEntity<ChatDTO> getChatHistory(@PathVariable Long chatId) {
        log.info("Fetching history for chat: {}", chatId);
        try {
            ChatDTO chat = chatService.getChatHistory(chatId);
            return ResponseEntity.ok(chat);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/chat/all - Get all chats
     */
    @GetMapping("/all")
    public ResponseEntity<List<ChatDTO>> getAllChats() {
        log.info("Fetching all chats");
        List<ChatDTO> chats = chatService.getAllChats();
        return ResponseEntity.ok(chats);
    }

    /**
     * PATCH /api/chat/{chatId}/rename - Rename a chat
     */
    @PatchMapping("/{chatId}/rename")
    public ResponseEntity<ChatDTO> renameChat(
            @PathVariable Long chatId,
            @RequestBody RenameChatRequest request) {
        log.info("Renaming chat {} to: {}", chatId, request.getNewTitle());
        try {
            ChatDTO chat = chatService.renameChat(chatId, request.getNewTitle());
            return ResponseEntity.ok(chat);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH /api/chat/{chatId}/model - Update chat model
     */
    @PatchMapping("/{chatId}/model")
    public ResponseEntity<ChatDTO> updateChatModel(
            @PathVariable Long chatId,
            @RequestBody UpdateModelRequest request) {
        log.info("Updating chat {} model to: {}", chatId, request.getModel());
        try {
            ChatDTO chat = chatService.updateChatModel(chatId, request.getModel());
            return ResponseEntity.ok(chat);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH /api/chat/{chatId}/expert - Update chat expert
     */
    @PatchMapping("/{chatId}/expert")
    public ResponseEntity<ChatDTO> updateChatExpert(
            @PathVariable Long chatId,
            @RequestBody java.util.Map<String, Long> request) {
        Long expertId = request.get("expertId");  // kann null sein um Experte zu entfernen
        log.info("Updating chat {} expert to: {}", chatId, expertId);
        try {
            ChatDTO chat = chatService.updateChatExpert(chatId, expertId);
            return ResponseEntity.ok(chat);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/chat/{chatId} - Delete a chat
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        log.info("Deleting chat: {}", chatId);
        chatService.deleteChat(chatId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/chat/{chatId}/messages/{messageId} - Delete a single message
     */
    @DeleteMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId) {
        log.info("Deleting message {} from chat {}", messageId, chatId);
        chatService.deleteMessage(chatId, messageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/chat/abort/{requestId} - Abort an active request
     */
    @PostMapping("/abort/{requestId}")
    public ResponseEntity<String> abortRequest(@PathVariable String requestId) {
        log.info("Abort request received for: {}", requestId);
        boolean success = chatService.abortRequest(requestId);

        if (success) {
            return ResponseEntity.ok("Request aborted successfully");
        } else {
            return ResponseEntity.ok("Request not found or already completed");
        }
    }

    /**
     * POST /api/chat/{chatId}/expert-summary-pdf - Generate expert summary PDF
     */
    @PostMapping("/{chatId}/expert-summary-pdf")
    public ResponseEntity<byte[]> generateExpertSummaryPdf(
            @PathVariable Long chatId,
            @RequestBody java.util.Map<String, Long> request) {
        log.info("Generating expert summary PDF for chat: {}", chatId);

        Long expertId = request.get("expertId");
        if (expertId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] pdfBytes = chatService.generateExpertSummaryPdf(chatId, expertId);

            // Generate filename
            String filename = String.format("Abschlussbericht_%s.pdf",
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            log.warn("Bad request for PDF generation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating expert summary PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
