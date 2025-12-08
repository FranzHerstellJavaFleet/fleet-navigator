package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for RAG (Retrieval-Augmented Generation) responses from Fleet Mates
 */
public class RAGResponse {

    /**
     * Single file search result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileSearchResult {
        private String path;
        private String name;
        private String extension;
        private Long size;
        private LocalDateTime modifiedAt;
        private String matchType; // "name" or "content"
        private String snippet;
        private Double score;
    }

    /**
     * File search response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchFilesResponse {
        private String sessionId;
        private List<FileSearchResult> results;
        private Integer totalFound;
        private Long searchTimeMs;
        private String query;
    }

    /**
     * File content response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileContentResponse {
        private String sessionId;
        private String path;
        private String name;
        private String content;
        private String contentType; // "text", "extracted", "binary"
        private Long size;
        private LocalDateTime modifiedAt;
        private Boolean truncated;
        private String error;
    }

    /**
     * Context save response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveContextResponse {
        private String sessionId;
        private String expertId;
        private String contextName;
        private String path;
        private Boolean success;
        private String error;
    }

    /**
     * Single context entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextEntry {
        private String name;
        private String content;
        private Map<String, Object> metadata;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
    }

    /**
     * Context load response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoadContextResponse {
        private String sessionId;
        private String expertId;
        private List<ContextEntry> contexts;
        private String error;
    }

    /**
     * Context info for listing
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextInfo {
        private String expertId;
        private String name;
        private Long size;
        private LocalDateTime modifiedAt;
    }

    /**
     * Context list response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListContextsResponse {
        private String sessionId;
        private List<ContextInfo> contexts;
    }

    /**
     * Context delete response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteContextResponse {
        private String sessionId;
        private String expertId;
        private String contextName;
        private Boolean success;
        private String error;
    }
}
