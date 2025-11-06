package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.HardwareStats;
import io.javafleet.fleetnavigator.dto.LogAnalysisRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to manage Fleet Officers
 */
@Slf4j
@Service
public class FleetOfficerService {

    // In-memory storage for now (later: database)
    private final Map<String, OfficerInfo> officers = new ConcurrentHashMap<>();
    private final Map<String, HardwareStats> latestStats = new ConcurrentHashMap<>();
    private final Map<String, LogAnalysisRequest> pendingAnalyses = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> logDataBuffers = new ConcurrentHashMap<>();

    /**
     * Register a new Fleet Officer
     */
    public void registerOfficer(String officerId, String name, String description, WebSocketSession session) {
        OfficerInfo info = new OfficerInfo();
        info.setOfficerId(officerId);
        info.setName(name);
        info.setDescription(description);
        info.setStatus(OfficerStatus.ONLINE);
        info.setLastHeartbeat(LocalDateTime.now());
        info.setRegisteredAt(LocalDateTime.now());
        info.setSession(session);

        officers.put(officerId, info);
        log.info("Officer registered: {} ({})", officerId, name);
    }

    /**
     * Update hardware stats from officer
     */
    public void updateStats(HardwareStats stats) {
        latestStats.put(stats.getOfficerId(), stats);

        OfficerInfo info = officers.get(stats.getOfficerId());
        if (info != null) {
            info.setLastStatsUpdate(LocalDateTime.now());
            info.setStatus(OfficerStatus.ONLINE);
        }

        log.debug("Updated stats for officer: {}", stats.getOfficerId());
    }

    /**
     * Update heartbeat timestamp
     */
    public void updateHeartbeat(String officerId) {
        OfficerInfo info = officers.get(officerId);
        if (info != null) {
            info.setLastHeartbeat(LocalDateTime.now());
            info.setStatus(OfficerStatus.ONLINE);
        }
    }

    /**
     * Mark officer as offline
     */
    public void markOffline(String officerId) {
        OfficerInfo info = officers.get(officerId);
        if (info != null) {
            info.setStatus(OfficerStatus.OFFLINE);
            info.setSession(null);
            log.info("Officer marked as offline: {}", officerId);
        }
    }

    /**
     * Get latest hardware stats for officer
     */
    public HardwareStats getLatestStats(String officerId) {
        return latestStats.get(officerId);
    }

    /**
     * Get all officers
     */
    public List<OfficerInfo> getAllOfficers() {
        return List.copyOf(officers.values());
    }

    /**
     * Get online officers
     */
    public List<OfficerInfo> getOnlineOfficers() {
        return officers.values().stream()
                .filter(o -> o.getStatus() == OfficerStatus.ONLINE)
                .collect(Collectors.toList());
    }

    /**
     * Get officer info
     */
    public OfficerInfo getOfficerInfo(String officerId) {
        return officers.get(officerId);
    }

    /**
     * Remove officer
     */
    public void removeOfficer(String officerId) {
        officers.remove(officerId);
        latestStats.remove(officerId);
        log.info("Officer removed: {}", officerId);
    }

    /**
     * Store pending log analysis request
     */
    public void storePendingAnalysis(String sessionId, LogAnalysisRequest request) {
        pendingAnalyses.put(sessionId, request);
        logDataBuffers.put(sessionId, new StringBuilder());
        log.info("Stored pending analysis: {}", sessionId);
    }

    /**
     * Append log data chunk
     */
    public void appendLogData(String sessionId, String data) {
        StringBuilder buffer = logDataBuffers.get(sessionId);
        if (buffer != null) {
            buffer.append(data);
        }
    }

    /**
     * Get complete log data and analysis request
     */
    public String getLogData(String sessionId) {
        StringBuilder buffer = logDataBuffers.get(sessionId);
        return buffer != null ? buffer.toString() : null;
    }

    /**
     * Get pending analysis request
     */
    public LogAnalysisRequest getPendingAnalysis(String sessionId) {
        return pendingAnalyses.get(sessionId);
    }

    /**
     * Remove completed analysis
     */
    public void removePendingAnalysis(String sessionId) {
        pendingAnalyses.remove(sessionId);
        logDataBuffers.remove(sessionId);
    }

    /**
     * Officer status enum
     */
    public enum OfficerStatus {
        ONLINE,
        OFFLINE,
        ERROR
    }

    /**
     * Officer information
     */
    @lombok.Data
    public static class OfficerInfo {
        private String officerId;
        private String name;
        private String description;
        private OfficerStatus status;
        private LocalDateTime registeredAt;
        private LocalDateTime lastHeartbeat;
        private LocalDateTime lastStatsUpdate;

        @com.fasterxml.jackson.annotation.JsonIgnore
        private WebSocketSession session;
    }
}
