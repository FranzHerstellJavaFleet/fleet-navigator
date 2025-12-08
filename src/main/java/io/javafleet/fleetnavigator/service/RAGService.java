package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javafleet.fleetnavigator.dto.MateCommand;
import io.javafleet.fleetnavigator.dto.RAGRequest;
import io.javafleet.fleetnavigator.dto.RAGResponse;
import io.javafleet.fleetnavigator.websocket.FleetMateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for RAG (Retrieval-Augmented Generation) operations via Fleet Mates.
 * Handles file search, file reading, and context management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final FleetMateWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    // Pending requests waiting for responses
    private final Map<String, CompletableFuture<RAGResponse.SearchFilesResponse>> pendingSearchRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<RAGResponse.FileContentResponse>> pendingReadRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<RAGResponse.SaveContextResponse>> pendingSaveRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<RAGResponse.LoadContextResponse>> pendingLoadRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<RAGResponse.ListContextsResponse>> pendingListRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<RAGResponse.DeleteContextResponse>> pendingDeleteRequests = new ConcurrentHashMap<>();

    private static final long TIMEOUT_SECONDS = 30;

    // ==================== Search Files ====================

    /**
     * Search files on a Mate's filesystem
     */
    public CompletableFuture<RAGResponse.SearchFilesResponse> searchFiles(RAGRequest.SearchFiles request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        request.setSessionId(sessionId);

        CompletableFuture<RAGResponse.SearchFilesResponse> future = new CompletableFuture<>();
        pendingSearchRequests.put(sessionId, future);

        // Build command payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("query", request.getQuery());
        if (request.getSearchPaths() != null) payload.put("searchPaths", request.getSearchPaths());
        if (request.getFileTypes() != null) payload.put("fileTypes", request.getFileTypes());
        if (request.getMaxResults() != null) payload.put("maxResults", request.getMaxResults());
        if (request.getSearchContent() != null) payload.put("searchContent", request.getSearchContent());
        if (request.getCaseSensitive() != null) payload.put("caseSensitive", request.getCaseSensitive());

        MateCommand command = new MateCommand("search_files", payload);

        log.info("Sending search_files command to mate {}: query='{}', sessionId={}",
                request.getMateId(), request.getQuery(), sessionId);

        webSocketHandler.sendCommandAuto(request.getMateId(), command);

        // Timeout handling
        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    pendingSearchRequests.remove(sessionId);
                    if (ex != null) {
                        log.warn("Search request timed out: {}", sessionId);
                    }
                });

        return future;
    }

    /**
     * Handle search results from Mate
     */
    @SuppressWarnings("unchecked")
    public void handleSearchResults(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        CompletableFuture<RAGResponse.SearchFilesResponse> future = pendingSearchRequests.get(sessionId);

        if (future == null) {
            log.warn("No pending search request for sessionId: {}", sessionId);
            return;
        }

        try {
            List<RAGResponse.FileSearchResult> results = new ArrayList<>();
            List<Map<String, Object>> rawResults = (List<Map<String, Object>>) data.get("results");

            if (rawResults != null) {
                for (Map<String, Object> r : rawResults) {
                    results.add(RAGResponse.FileSearchResult.builder()
                            .path((String) r.get("path"))
                            .name((String) r.get("name"))
                            .extension((String) r.get("extension"))
                            .size(getLong(r.get("size")))
                            .modifiedAt(parseDateTime(r.get("modifiedAt")))
                            .matchType((String) r.get("matchType"))
                            .snippet((String) r.get("snippet"))
                            .score(getDouble(r.get("score")))
                            .build());
                }
            }

            RAGResponse.SearchFilesResponse response = RAGResponse.SearchFilesResponse.builder()
                    .sessionId(sessionId)
                    .results(results)
                    .totalFound(getInt(data.get("totalFound")))
                    .searchTimeMs(getLong(data.get("searchTimeMs")))
                    .query((String) data.get("query"))
                    .build();

            future.complete(response);
            log.info("Search completed: {} results for session {}", results.size(), sessionId);

        } catch (Exception e) {
            log.error("Error processing search results: {}", e.getMessage());
            future.completeExceptionally(e);
        }
    }

    // ==================== Read File ====================

    /**
     * Read file content from a Mate's filesystem
     */
    public CompletableFuture<RAGResponse.FileContentResponse> readFile(RAGRequest.ReadFile request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        request.setSessionId(sessionId);

        CompletableFuture<RAGResponse.FileContentResponse> future = new CompletableFuture<>();
        pendingReadRequests.put(sessionId, future);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("path", request.getPath());
        if (request.getMaxLength() != null) payload.put("maxLength", request.getMaxLength());
        if (request.getExtractText() != null) payload.put("extractText", request.getExtractText());

        MateCommand command = new MateCommand("read_file", payload);

        log.info("Sending read_file command to mate {}: path='{}', sessionId={}",
                request.getMateId(), request.getPath(), sessionId);

        webSocketHandler.sendCommandAuto(request.getMateId(), command);

        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    pendingReadRequests.remove(sessionId);
                    if (ex != null) {
                        log.warn("Read request timed out: {}", sessionId);
                    }
                });

        return future;
    }

    /**
     * Handle file content from Mate
     */
    public void handleFileContent(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        CompletableFuture<RAGResponse.FileContentResponse> future = pendingReadRequests.get(sessionId);

        if (future == null) {
            log.warn("No pending read request for sessionId: {}", sessionId);
            return;
        }

        try {
            RAGResponse.FileContentResponse response = RAGResponse.FileContentResponse.builder()
                    .sessionId(sessionId)
                    .path((String) data.get("path"))
                    .name((String) data.get("name"))
                    .content((String) data.get("content"))
                    .contentType((String) data.get("contentType"))
                    .size(getLong(data.get("size")))
                    .modifiedAt(parseDateTime(data.get("modifiedAt")))
                    .truncated(getBoolean(data.get("truncated")))
                    .error((String) data.get("error"))
                    .build();

            future.complete(response);
            log.info("File read completed: {} ({} chars) for session {}",
                    response.getName(), response.getContent() != null ? response.getContent().length() : 0, sessionId);

        } catch (Exception e) {
            log.error("Error processing file content: {}", e.getMessage());
            future.completeExceptionally(e);
        }
    }

    // ==================== Save Context ====================

    /**
     * Save context for an expert
     */
    public CompletableFuture<RAGResponse.SaveContextResponse> saveContext(RAGRequest.SaveContext request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        request.setSessionId(sessionId);

        CompletableFuture<RAGResponse.SaveContextResponse> future = new CompletableFuture<>();
        pendingSaveRequests.put(sessionId, future);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("expertId", request.getExpertId());
        payload.put("contextName", request.getContextName());
        payload.put("content", request.getContent());
        if (request.getMetadata() != null) payload.put("metadata", request.getMetadata());

        MateCommand command = new MateCommand("save_context", payload);

        log.info("Sending save_context command to mate {}: expert={}, context={}, sessionId={}",
                request.getMateId(), request.getExpertId(), request.getContextName(), sessionId);

        webSocketHandler.sendCommandAuto(request.getMateId(), command);

        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    pendingSaveRequests.remove(sessionId);
                    if (ex != null) {
                        log.warn("Save context request timed out: {}", sessionId);
                    }
                });

        return future;
    }

    /**
     * Handle save context response from Mate
     */
    public void handleContextSaved(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        CompletableFuture<RAGResponse.SaveContextResponse> future = pendingSaveRequests.get(sessionId);

        if (future == null) {
            log.warn("No pending save request for sessionId: {}", sessionId);
            return;
        }

        try {
            RAGResponse.SaveContextResponse response = RAGResponse.SaveContextResponse.builder()
                    .sessionId(sessionId)
                    .expertId((String) data.get("expertId"))
                    .contextName((String) data.get("contextName"))
                    .path((String) data.get("path"))
                    .success(getBoolean(data.get("success")))
                    .error((String) data.get("error"))
                    .build();

            future.complete(response);
            log.info("Context saved: expert={}, name={}, success={}",
                    response.getExpertId(), response.getContextName(), response.getSuccess());

        } catch (Exception e) {
            log.error("Error processing save context response: {}", e.getMessage());
            future.completeExceptionally(e);
        }
    }

    // ==================== Load Context ====================

    /**
     * Load context for an expert
     */
    public CompletableFuture<RAGResponse.LoadContextResponse> loadContext(RAGRequest.LoadContext request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        request.setSessionId(sessionId);

        CompletableFuture<RAGResponse.LoadContextResponse> future = new CompletableFuture<>();
        pendingLoadRequests.put(sessionId, future);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("expertId", request.getExpertId());
        if (request.getContextName() != null) payload.put("contextName", request.getContextName());

        MateCommand command = new MateCommand("load_context", payload);

        log.info("Sending load_context command to mate {}: expert={}, sessionId={}",
                request.getMateId(), request.getExpertId(), sessionId);

        webSocketHandler.sendCommandAuto(request.getMateId(), command);

        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    pendingLoadRequests.remove(sessionId);
                    if (ex != null) {
                        log.warn("Load context request timed out: {}", sessionId);
                    }
                });

        return future;
    }

    /**
     * Handle load context response from Mate
     */
    @SuppressWarnings("unchecked")
    public void handleContextLoaded(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        CompletableFuture<RAGResponse.LoadContextResponse> future = pendingLoadRequests.get(sessionId);

        if (future == null) {
            log.warn("No pending load request for sessionId: {}", sessionId);
            return;
        }

        try {
            List<RAGResponse.ContextEntry> contexts = new ArrayList<>();
            List<Map<String, Object>> rawContexts = (List<Map<String, Object>>) data.get("contexts");

            if (rawContexts != null) {
                for (Map<String, Object> c : rawContexts) {
                    contexts.add(RAGResponse.ContextEntry.builder()
                            .name((String) c.get("name"))
                            .content((String) c.get("content"))
                            .metadata((Map<String, Object>) c.get("metadata"))
                            .createdAt(parseDateTime(c.get("createdAt")))
                            .modifiedAt(parseDateTime(c.get("modifiedAt")))
                            .build());
                }
            }

            RAGResponse.LoadContextResponse response = RAGResponse.LoadContextResponse.builder()
                    .sessionId(sessionId)
                    .expertId((String) data.get("expertId"))
                    .contexts(contexts)
                    .error((String) data.get("error"))
                    .build();

            future.complete(response);
            log.info("Context loaded: expert={}, {} contexts", response.getExpertId(), contexts.size());

        } catch (Exception e) {
            log.error("Error processing load context response: {}", e.getMessage());
            future.completeExceptionally(e);
        }
    }

    // ==================== List Contexts ====================

    /**
     * List all contexts
     */
    public CompletableFuture<RAGResponse.ListContextsResponse> listContexts(RAGRequest.ListContexts request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        request.setSessionId(sessionId);

        CompletableFuture<RAGResponse.ListContextsResponse> future = new CompletableFuture<>();
        pendingListRequests.put(sessionId, future);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        if (request.getExpertId() != null) payload.put("expertId", request.getExpertId());

        MateCommand command = new MateCommand("list_contexts", payload);

        log.info("Sending list_contexts command to mate {}: sessionId={}", request.getMateId(), sessionId);

        webSocketHandler.sendCommandAuto(request.getMateId(), command);

        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    pendingListRequests.remove(sessionId);
                    if (ex != null) {
                        log.warn("List contexts request timed out: {}", sessionId);
                    }
                });

        return future;
    }

    /**
     * Handle list contexts response from Mate
     */
    @SuppressWarnings("unchecked")
    public void handleContextsList(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        CompletableFuture<RAGResponse.ListContextsResponse> future = pendingListRequests.get(sessionId);

        if (future == null) {
            log.warn("No pending list request for sessionId: {}", sessionId);
            return;
        }

        try {
            List<RAGResponse.ContextInfo> contexts = new ArrayList<>();
            List<Map<String, Object>> rawContexts = (List<Map<String, Object>>) data.get("contexts");

            if (rawContexts != null) {
                for (Map<String, Object> c : rawContexts) {
                    contexts.add(RAGResponse.ContextInfo.builder()
                            .expertId((String) c.get("expertId"))
                            .name((String) c.get("name"))
                            .size(getLong(c.get("size")))
                            .modifiedAt(parseDateTime(c.get("modifiedAt")))
                            .build());
                }
            }

            RAGResponse.ListContextsResponse response = RAGResponse.ListContextsResponse.builder()
                    .sessionId(sessionId)
                    .contexts(contexts)
                    .build();

            future.complete(response);
            log.info("Contexts listed: {} entries", contexts.size());

        } catch (Exception e) {
            log.error("Error processing list contexts response: {}", e.getMessage());
            future.completeExceptionally(e);
        }
    }

    // ==================== Delete Context ====================

    /**
     * Delete a context
     */
    public CompletableFuture<RAGResponse.DeleteContextResponse> deleteContext(RAGRequest.DeleteContext request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        request.setSessionId(sessionId);

        CompletableFuture<RAGResponse.DeleteContextResponse> future = new CompletableFuture<>();
        pendingDeleteRequests.put(sessionId, future);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("expertId", request.getExpertId());
        payload.put("contextName", request.getContextName());

        MateCommand command = new MateCommand("delete_context", payload);

        log.info("Sending delete_context command to mate {}: expert={}, context={}, sessionId={}",
                request.getMateId(), request.getExpertId(), request.getContextName(), sessionId);

        webSocketHandler.sendCommandAuto(request.getMateId(), command);

        future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    pendingDeleteRequests.remove(sessionId);
                    if (ex != null) {
                        log.warn("Delete context request timed out: {}", sessionId);
                    }
                });

        return future;
    }

    /**
     * Handle delete context response from Mate
     */
    public void handleContextDeleted(Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        CompletableFuture<RAGResponse.DeleteContextResponse> future = pendingDeleteRequests.get(sessionId);

        if (future == null) {
            log.warn("No pending delete request for sessionId: {}", sessionId);
            return;
        }

        try {
            RAGResponse.DeleteContextResponse response = RAGResponse.DeleteContextResponse.builder()
                    .sessionId(sessionId)
                    .expertId((String) data.get("expertId"))
                    .contextName((String) data.get("contextName"))
                    .success(getBoolean(data.get("success")))
                    .error((String) data.get("error"))
                    .build();

            future.complete(response);
            log.info("Context deleted: expert={}, name={}, success={}",
                    response.getExpertId(), response.getContextName(), response.getSuccess());

        } catch (Exception e) {
            log.error("Error processing delete context response: {}", e.getMessage());
            future.completeExceptionally(e);
        }
    }

    // ==================== Helper Methods ====================

    private Long getLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return ((Double) value).longValue();
        return null;
    }

    private Integer getInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Double) return ((Double) value).intValue();
        return null;
    }

    private Double getDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        return null;
    }

    private Boolean getBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                return LocalDateTime.parse((String) value);
            }
            if (value instanceof Long) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) value), ZoneId.systemDefault());
            }
        } catch (Exception e) {
            log.debug("Could not parse datetime: {}", value);
        }
        return null;
    }
}
