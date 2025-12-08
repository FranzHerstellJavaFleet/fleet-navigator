package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.RAGRequest;
import io.javafleet.fleetnavigator.dto.RAGResponse;
import io.javafleet.fleetnavigator.service.FleetMateService;
import io.javafleet.fleetnavigator.service.RAGService;
import io.javafleet.fleetnavigator.websocket.FleetMateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST Controller for RAG (Retrieval-Augmented Generation) operations.
 * Allows searching files, reading content, and managing expert contexts via Fleet Mates.
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;
    private final FleetMateService fleetMateService;
    private final FleetMateWebSocketHandler webSocketHandler;

    // ==================== Search Files ====================

    /**
     * Search files on a Mate's filesystem
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchFiles(@RequestBody RAGRequest.SearchFiles request) {
        log.info("RAG search request: mateId={}, query='{}'", request.getMateId(), request.getQuery());

        // Validate mate is connected
        if (!webSocketHandler.isMateConnected(request.getMateId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Mate nicht verbunden: " + request.getMateId()
            ));
        }

        try {
            CompletableFuture<RAGResponse.SearchFilesResponse> future = ragService.searchFiles(request);
            RAGResponse.SearchFilesResponse response = future.join();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Suche fehlgeschlagen: " + e.getMessage()
            ));
        }
    }

    /**
     * Search files with simple GET request
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchFilesGet(
            @RequestParam String mateId,
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int maxResults,
            @RequestParam(defaultValue = "false") boolean searchContent
    ) {
        RAGRequest.SearchFiles request = RAGRequest.SearchFiles.builder()
                .mateId(mateId)
                .query(query)
                .maxResults(maxResults)
                .searchContent(searchContent)
                .build();

        return searchFiles(request);
    }

    // ==================== Read File ====================

    /**
     * Read file content from a Mate's filesystem
     */
    @PostMapping("/read")
    public ResponseEntity<?> readFile(@RequestBody RAGRequest.ReadFile request) {
        log.info("RAG read request: mateId={}, path='{}'", request.getMateId(), request.getPath());

        if (!webSocketHandler.isMateConnected(request.getMateId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Mate nicht verbunden: " + request.getMateId()
            ));
        }

        try {
            CompletableFuture<RAGResponse.FileContentResponse> future = ragService.readFile(request);
            RAGResponse.FileContentResponse response = future.join();

            if (response.getError() != null && !response.getError().isEmpty()) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Read failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Lesen fehlgeschlagen: " + e.getMessage()
            ));
        }
    }

    /**
     * Read file with simple GET request
     */
    @GetMapping("/read")
    public ResponseEntity<?> readFileGet(
            @RequestParam String mateId,
            @RequestParam String path,
            @RequestParam(defaultValue = "50000") int maxLength
    ) {
        RAGRequest.ReadFile request = RAGRequest.ReadFile.builder()
                .mateId(mateId)
                .path(path)
                .maxLength(maxLength)
                .extractText(true)
                .build();

        return readFile(request);
    }

    // ==================== Context Management ====================

    /**
     * Save context for an expert
     */
    @PostMapping("/context/save")
    public ResponseEntity<?> saveContext(@RequestBody RAGRequest.SaveContext request) {
        log.info("RAG save context: mateId={}, expert={}, name='{}'",
                request.getMateId(), request.getExpertId(), request.getContextName());

        if (!webSocketHandler.isMateConnected(request.getMateId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Mate nicht verbunden: " + request.getMateId()
            ));
        }

        try {
            CompletableFuture<RAGResponse.SaveContextResponse> future = ragService.saveContext(request);
            RAGResponse.SaveContextResponse response = future.join();

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Save context failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Kontext speichern fehlgeschlagen: " + e.getMessage()
            ));
        }
    }

    /**
     * Load context for an expert
     */
    @PostMapping("/context/load")
    public ResponseEntity<?> loadContext(@RequestBody RAGRequest.LoadContext request) {
        log.info("RAG load context: mateId={}, expert={}, name='{}'",
                request.getMateId(), request.getExpertId(), request.getContextName());

        if (!webSocketHandler.isMateConnected(request.getMateId())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Mate nicht verbunden: " + request.getMateId()
            ));
        }

        try {
            CompletableFuture<RAGResponse.LoadContextResponse> future = ragService.loadContext(request);
            RAGResponse.LoadContextResponse response = future.join();

            if (response.getError() != null && !response.getError().isEmpty()) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Load context failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Kontext laden fehlgeschlagen: " + e.getMessage()
            ));
        }
    }

    /**
     * Load context with GET request
     */
    @GetMapping("/context/load")
    public ResponseEntity<?> loadContextGet(
            @RequestParam String mateId,
            @RequestParam String expertId,
            @RequestParam(required = false) String contextName
    ) {
        RAGRequest.LoadContext request = RAGRequest.LoadContext.builder()
                .mateId(mateId)
                .expertId(expertId)
                .contextName(contextName)
                .build();

        return loadContext(request);
    }

    /**
     * List all contexts
     */
    @GetMapping("/context/list")
    public ResponseEntity<?> listContexts(
            @RequestParam String mateId,
            @RequestParam(required = false) String expertId
    ) {
        log.info("RAG list contexts: mateId={}, expert={}", mateId, expertId);

        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Mate nicht verbunden: " + mateId
            ));
        }

        try {
            RAGRequest.ListContexts request = RAGRequest.ListContexts.builder()
                    .mateId(mateId)
                    .expertId(expertId)
                    .build();

            CompletableFuture<RAGResponse.ListContextsResponse> future = ragService.listContexts(request);
            RAGResponse.ListContextsResponse response = future.join();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("List contexts failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Kontexte auflisten fehlgeschlagen: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete a context
     */
    @DeleteMapping("/context")
    public ResponseEntity<?> deleteContext(
            @RequestParam String mateId,
            @RequestParam String expertId,
            @RequestParam String contextName
    ) {
        log.info("RAG delete context: mateId={}, expert={}, name='{}'", mateId, expertId, contextName);

        if (!webSocketHandler.isMateConnected(mateId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Mate nicht verbunden: " + mateId
            ));
        }

        try {
            RAGRequest.DeleteContext request = RAGRequest.DeleteContext.builder()
                    .mateId(mateId)
                    .expertId(expertId)
                    .contextName(contextName)
                    .build();

            CompletableFuture<RAGResponse.DeleteContextResponse> future = ragService.deleteContext(request);
            RAGResponse.DeleteContextResponse response = future.join();

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Delete context failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Kontext löschen fehlgeschlagen: " + e.getMessage()
            ));
        }
    }

    // ==================== Status ====================

    /**
     * Get RAG-capable mates (online mates with RAG support)
     */
    @GetMapping("/mates")
    public ResponseEntity<?> getRAGMates() {
        List<Map<String, Object>> mates = fleetMateService.getOnlineMates().stream()
                .map(m -> Map.<String, Object>of(
                        "mateId", m.getMateId(),
                        "name", m.getName(),
                        "status", m.getStatus().name()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "mates", mates,
                "count", mates.size()
        ));
    }
}
