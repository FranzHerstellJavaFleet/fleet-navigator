package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.FileSearchService;
import io.javafleet.fleetnavigator.service.FileSearchService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API Controller for File Search functionality (OS Mate RAG)
 * Manages search folders and performs local document searches
 */
@RestController
@RequestMapping("/api/file-search")
@RequiredArgsConstructor
@Slf4j
public class FileSearchController {

    private final FileSearchService fileSearchService;

    // ==================== Search ====================

    /**
     * Search files by name and/or content
     */
    @GetMapping("/search")
    public ResponseEntity<List<FileSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "true") boolean searchByName,
            @RequestParam(defaultValue = "true") boolean searchByContent,
            @RequestParam(defaultValue = "20") int maxResults
    ) {
        log.info("File search: query='{}', byName={}, byContent={}, max={}",
                query, searchByName, searchByContent, maxResults);

        SearchOptions options = new SearchOptions();
        options.setSearchByName(searchByName);
        options.setSearchByContent(searchByContent);
        options.setMaxResults(maxResults);

        List<FileSearchResult> results = fileSearchService.search(query, options);
        log.info("Found {} results", results.size());

        return ResponseEntity.ok(results);
    }

    /**
     * Search and return formatted context for LLM
     */
    @GetMapping("/search/context")
    public ResponseEntity<Map<String, Object>> searchWithContext(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults,
            @RequestParam(defaultValue = "50000") int maxContentLength
    ) {
        SearchOptions options = new SearchOptions();
        options.setMaxResults(maxResults);

        List<FileSearchResult> results = fileSearchService.search(query, options);
        String summary = fileSearchService.formatResultsAsContext(results);
        String fullContent = fileSearchService.getFilesContentForRAG(results, maxContentLength);

        return ResponseEntity.ok(Map.of(
                "results", results,
                "summary", summary,
                "fullContent", fullContent,
                "resultCount", results.size()
        ));
    }

    // ==================== Folder Management ====================

    /**
     * Get all configured search folders
     */
    @GetMapping("/folders")
    public ResponseEntity<List<FileSearchConfig>> getFolders() {
        return ResponseEntity.ok(fileSearchService.getSearchFolders());
    }

    /**
     * Add a new search folder
     */
    @PostMapping("/folders")
    public ResponseEntity<?> addFolder(@RequestBody AddFolderRequest request) {
        try {
            FileSearchConfig config = new FileSearchConfig();
            config.setFolderPath(request.folderPath);
            config.setName(request.name != null ? request.name : extractFolderName(request.folderPath));
            config.setRecursive(request.recursive != null ? request.recursive : true);
            config.setMaxDepth(request.maxDepth != null ? request.maxDepth : 10);
            config.setSearchContent(request.searchContent != null ? request.searchContent : true);

            if (request.allowedExtensions != null && !request.allowedExtensions.isEmpty()) {
                config.setAllowedExtensions(request.allowedExtensions);
            }

            fileSearchService.addSearchFolder(config);

            log.info("Added search folder: {} -> {}", config.getName(), config.getFolderPath());
            return ResponseEntity.ok(config);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid folder: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove a search folder
     */
    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<?> removeFolder(@PathVariable String folderId) {
        fileSearchService.removeSearchFolder(folderId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Ordner entfernt"));
    }

    /**
     * Update folder settings
     */
    @PutMapping("/folders/{folderId}")
    public ResponseEntity<?> updateFolder(
            @PathVariable String folderId,
            @RequestBody UpdateFolderRequest request
    ) {
        return fileSearchService.getSearchFolder(folderId)
                .map(config -> {
                    if (request.name != null) config.setName(request.name);
                    if (request.recursive != null) config.setRecursive(request.recursive);
                    if (request.maxDepth != null) config.setMaxDepth(request.maxDepth);
                    if (request.searchContent != null) config.setSearchContent(request.searchContent);
                    if (request.enabled != null) config.setEnabled(request.enabled);
                    if (request.allowedExtensions != null) {
                        config.setAllowedExtensions(request.allowedExtensions);
                    }
                    // Persist changes
                    fileSearchService.updateSearchFolder(config);
                    return ResponseEntity.ok(config);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Indexing ====================

    /**
     * Trigger re-indexing for a folder
     */
    @PostMapping("/folders/{folderId}/reindex")
    public ResponseEntity<?> reindexFolder(@PathVariable String folderId) {
        return fileSearchService.getSearchFolder(folderId)
                .map(config -> {
                    fileSearchService.indexFolderAsync(folderId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Indexierung gestartet für: " + config.getName()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Trigger re-indexing for all folders
     */
    @PostMapping("/reindex-all")
    public ResponseEntity<?> reindexAll() {
        fileSearchService.indexAllFolders();
        return ResponseEntity.ok(Map.of("success", true, "message", "Indexierung für alle Ordner gestartet"));
    }

    // ==================== Status ====================

    /**
     * Get file search service status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(fileSearchService.getStatus());
    }

    // ==================== Helper Methods ====================

    private String extractFolderName(String path) {
        if (path == null || path.isEmpty()) return "Unbekannt";
        String[] parts = path.replace("\\", "/").split("/");
        return parts[parts.length - 1];
    }

    // ==================== Request DTOs ====================

    public static class AddFolderRequest {
        public String folderPath;
        public String name;
        public Boolean recursive;
        public Integer maxDepth;
        public Boolean searchContent;
        public Set<String> allowedExtensions;
    }

    public static class UpdateFolderRequest {
        public String name;
        public Boolean recursive;
        public Integer maxDepth;
        public Boolean searchContent;
        public Boolean enabled;
        public Set<String> allowedExtensions;
    }
}
