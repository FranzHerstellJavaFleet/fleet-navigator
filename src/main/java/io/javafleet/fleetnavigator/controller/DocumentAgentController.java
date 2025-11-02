package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.DocumentAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Document Agent
 * CORS is handled globally in WebConfig
 */
@RestController
@RequestMapping("/api/agents/document")
@RequiredArgsConstructor
@Slf4j
public class DocumentAgentController {

    private final DocumentAgentService documentAgentService;

    /**
     * Generate a document from a prompt
     *
     * POST /api/agents/document/generate
     * Body: {
     *   "prompt": "Erstelle eine Kündigung für meine Hausratversicherung...",
     *   "model": "llama3.2:3b" (optional - uses configured model if not provided)
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateDocument(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String model = request.get("model");

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Prompt darf nicht leer sein"
                ));
            }

            Path documentPath;

            // If model is provided, use it; otherwise use configured model from settings
            if (model != null && !model.trim().isEmpty()) {
                log.info("Generating document with specified model: {}, Prompt length: {}", model, prompt.length());
                documentPath = documentAgentService.generateDocument(prompt, model);
            } else {
                log.info("Generating document with configured model, Prompt length: {}", prompt.length());
                documentPath = documentAgentService.generateDocument(prompt);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dokument erfolgreich erstellt und in LibreOffice geöffnet");
            response.put("path", documentPath.toString());
            response.put("filename", documentPath.getFileName().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating document", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Fehler beim Erstellen des Dokuments: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}
