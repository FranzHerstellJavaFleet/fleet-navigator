package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.EmailReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for LibreOffice integration
 * Simple HTTP endpoint - no WebSocket needed!
 */
@Slf4j
@RestController
@RequestMapping("/api/office")
@RequiredArgsConstructor
public class OfficeController {

    private final EmailReplyService emailReplyService;

    /**
     * Generate document text
     * POST /api/office/generate-document
     *
     * Body: {"prompt": "Erstelle ein Bewerbungsschreiben...", "model": "llama3.1:8b"}
     */
    @PostMapping("/generate-document")
    public ResponseEntity<Map<String, String>> generateDocument(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String model = request.get("model");

        if (prompt == null || prompt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Prompt is required"));
        }

        log.info("Generating document via REST API: promptLength={}, model={}",
                 prompt.length(), model != null ? model : "default");

        try {
            String result = emailReplyService.generateSimpleCompletion(prompt, model);

            return ResponseEntity.ok(Map.of(
                "content", result,
                "model", model != null ? model : "llama3.1:8b"
            ));

        } catch (Exception e) {
            log.error("Failed to generate document: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Fleet Navigator Office API"));
    }
}
