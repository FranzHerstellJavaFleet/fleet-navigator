package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for RAG (Retrieval-Augmented Generation) operations with Fleet Mates
 */
public class RAGRequest {

    /**
     * Request to search files on a Mate's filesystem
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchFiles {
        private String sessionId;
        private String mateId;
        private String query;
        private List<String> searchPaths;
        private List<String> fileTypes;
        private Integer maxResults;
        private Boolean searchContent;
        private Boolean caseSensitive;
    }

    /**
     * Request to read a file from a Mate's filesystem
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadFile {
        private String sessionId;
        private String mateId;
        private String path;
        private Integer maxLength;
        private Boolean extractText;
    }

    /**
     * Request to save context for an expert
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveContext {
        private String sessionId;
        private String mateId;
        private String expertId;
        private String contextName;
        private String content;
        private Map<String, Object> metadata;
    }

    /**
     * Request to load context for an expert
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoadContext {
        private String sessionId;
        private String mateId;
        private String expertId;
        private String contextName; // Empty = load all
    }

    /**
     * Request to list all contexts
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListContexts {
        private String sessionId;
        private String mateId;
        private String expertId; // Empty = list all experts
    }

    /**
     * Request to delete a context
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteContext {
        private String sessionId;
        private String mateId;
        private String expertId;
        private String contextName;
    }
}
