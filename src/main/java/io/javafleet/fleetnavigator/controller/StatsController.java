package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.StatsResponse;
import io.javafleet.fleetnavigator.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for statistics
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final ChatService chatService;

    /**
     * GET /api/stats/global - Get global statistics
     */
    @GetMapping("/global")
    public ResponseEntity<StatsResponse> getGlobalStats() {
        log.info("Fetching global statistics");
        StatsResponse stats = chatService.getGlobalStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/stats/chat/{chatId} - Get chat statistics
     */
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<StatsResponse> getChatStats(@PathVariable Long chatId) {
        log.info("Fetching statistics for chat: {}", chatId);
        StatsResponse stats = chatService.getChatStats(chatId);
        return ResponseEntity.ok(stats);
    }
}
