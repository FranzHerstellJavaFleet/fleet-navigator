package io.javafleet.fleetnavigator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing FleetCode AI coding agent sessions.
 * Handles streaming of execution steps and results to the frontend.
 */
@Slf4j
@Service
public class FleetCodeService {

    // Active SSE emitters for streaming results
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    // Session data storage
    private final Map<String, FleetCodeSession> sessions = new ConcurrentHashMap<>();

    /**
     * Create a new FleetCode session
     */
    public String createSession(String mateId, String task, String workingDir) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        FleetCodeSession session = new FleetCodeSession(sessionId, mateId, task, workingDir);
        sessions.put(sessionId, session);
        log.info("Created FleetCode session: {} for mate: {}", sessionId, mateId);
        return sessionId;
    }

    /**
     * Register SSE emitter for a session
     */
    public void registerEmitter(String sessionId, SseEmitter emitter) {
        activeEmitters.put(sessionId, emitter);
        emitter.onCompletion(() -> {
            activeEmitters.remove(sessionId);
            log.debug("SSE emitter completed for session: {}", sessionId);
        });
        emitter.onTimeout(() -> {
            activeEmitters.remove(sessionId);
            log.debug("SSE emitter timeout for session: {}", sessionId);
        });
        emitter.onError(e -> {
            activeEmitters.remove(sessionId);
            log.debug("SSE emitter error for session: {}", sessionId);
        });
    }

    /**
     * Send a step update to the frontend
     */
    public void sendStep(String sessionId, Map<String, Object> stepData) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("step")
                    .data(stepData));
                log.debug("Sent FleetCode step for session: {}", sessionId);
            } catch (IOException e) {
                log.error("Failed to send step for session {}: {}", sessionId, e.getMessage());
                activeEmitters.remove(sessionId);
            }
        }

        // Also store in session
        FleetCodeSession session = sessions.get(sessionId);
        if (session != null) {
            session.addStep(stepData);
        }
    }

    /**
     * Send the final result to the frontend
     */
    public void sendResult(String sessionId, Map<String, Object> resultData) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("result")
                    .data(resultData));
                emitter.complete();
                log.info("Sent FleetCode result for session: {}", sessionId);
            } catch (IOException e) {
                log.error("Failed to send result for session {}: {}", sessionId, e.getMessage());
            }
            activeEmitters.remove(sessionId);
        }

        // Store final result in session
        FleetCodeSession session = sessions.get(sessionId);
        if (session != null) {
            session.setResult(resultData);
            session.setCompleted(true);
        }
    }

    /**
     * Send error to frontend
     */
    public void sendError(String sessionId, String error) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", error)));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send error for session {}: {}", sessionId, e.getMessage());
            }
            activeEmitters.remove(sessionId);
        }
    }

    /**
     * Get session by ID
     */
    public FleetCodeSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Check if session exists
     */
    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * FleetCode session data
     */
    public static class FleetCodeSession {
        private final String sessionId;
        private final String mateId;
        private final String task;
        private final String workingDir;
        private final java.util.List<Map<String, Object>> steps = new java.util.ArrayList<>();
        private Map<String, Object> result;
        private boolean completed = false;
        private final long createdAt = System.currentTimeMillis();

        public FleetCodeSession(String sessionId, String mateId, String task, String workingDir) {
            this.sessionId = sessionId;
            this.mateId = mateId;
            this.task = task;
            this.workingDir = workingDir;
        }

        public void addStep(Map<String, Object> step) {
            steps.add(step);
        }

        public void setResult(Map<String, Object> result) {
            this.result = result;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getMateId() { return mateId; }
        public String getTask() { return task; }
        public String getWorkingDir() { return workingDir; }
        public java.util.List<Map<String, Object>> getSteps() { return steps; }
        public Map<String, Object> getResult() { return result; }
        public boolean isCompleted() { return completed; }
        public long getCreatedAt() { return createdAt; }
    }
}
