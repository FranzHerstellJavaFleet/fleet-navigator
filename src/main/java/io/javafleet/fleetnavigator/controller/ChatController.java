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
     * DELETE /api/chat/{chatId} - Delete a chat
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        log.info("Deleting chat: {}", chatId);
        chatService.deleteChat(chatId);
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
}
