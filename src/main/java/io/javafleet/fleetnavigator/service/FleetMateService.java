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
 * Service to manage Fleet Mates
 */
@Slf4j
@Service
public class FleetMateService {

    // In-memory storage for now (later: database)
    private final Map<String, MateInfo> mates = new ConcurrentHashMap<>();
    private final Map<String, HardwareStats> latestStats = new ConcurrentHashMap<>();
    private final Map<String, LogAnalysisRequest> pendingAnalyses = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> logDataBuffers = new ConcurrentHashMap<>();

    /**
     * Register a new Fleet Mate
     */
    public void registerMate(String mateId, String name, String description, WebSocketSession session) {
        registerMate(mateId, name, description, null, session);
    }

    /**
     * Register a new Fleet Mate with preferred model
     */
    public void registerMate(String mateId, String name, String description, String preferredModel, WebSocketSession session) {
        MateInfo info = new MateInfo();
        info.setMateId(mateId);
        info.setName(name);
        info.setDescription(description);
        info.setPreferredModel(preferredModel);
        info.setStatus(MateStatus.ONLINE);
        info.setLastHeartbeat(LocalDateTime.now());
        info.setRegisteredAt(LocalDateTime.now());
        info.setSession(session);

        mates.put(mateId, info);
        log.info("Mate registered: {} ({}) - Model: {}", mateId, name, preferredModel != null ? preferredModel : "default");
    }

    /**
     * Update hardware stats from mate
     */
    public void updateStats(HardwareStats stats) {
        if (stats == null || stats.getMateId() == null) {
            log.warn("Received null stats or stats with null mateId");
            return;
        }

        latestStats.put(stats.getMateId(), stats);

        MateInfo info = mates.get(stats.getMateId());
        if (info != null) {
            info.setLastStatsUpdate(LocalDateTime.now());
            info.setStatus(MateStatus.ONLINE);
        }

        log.debug("Updated stats for mate: {}", stats.getMateId());
    }

    /**
     * Update heartbeat timestamp
     */
    public void updateHeartbeat(String mateId) {
        MateInfo info = mates.get(mateId);
        if (info != null) {
            info.setLastHeartbeat(LocalDateTime.now());
            info.setStatus(MateStatus.ONLINE);
        }
    }

    /**
     * Mark mate as offline
     */
    public void markOffline(String mateId) {
        MateInfo info = mates.get(mateId);
        if (info != null) {
            info.setStatus(MateStatus.OFFLINE);
            info.setSession(null);
            log.info("Mate marked as offline: {}", mateId);
        }
    }

    /**
     * Get latest hardware stats for mate
     */
    public HardwareStats getLatestStats(String mateId) {
        return latestStats.get(mateId);
    }

    /**
     * Get all mates
     */
    public List<MateInfo> getAllMates() {
        return List.copyOf(mates.values());
    }

    /**
     * Get online mates
     */
    public List<MateInfo> getOnlineMates() {
        return mates.values().stream()
                .filter(m -> m.getStatus() == MateStatus.ONLINE)
                .collect(Collectors.toList());
    }

    /**
     * Get mate info
     */
    public MateInfo getMateInfo(String mateId) {
        return mates.get(mateId);
    }

    /**
     * Remove mate
     */
    public void removeMate(String mateId) {
        mates.remove(mateId);
        latestStats.remove(mateId);
        log.info("Mate removed: {}", mateId);
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
     * Mate status enum
     */
    public enum MateStatus {
        ONLINE,
        OFFLINE,
        ERROR
    }

    /**
     * Mate information
     */
    @lombok.Data
    public static class MateInfo {
        private String mateId;
        private String name;
        private String description;
        private MateStatus status;
        private LocalDateTime registeredAt;
        private LocalDateTime lastHeartbeat;
        private LocalDateTime lastStatsUpdate;
        private String preferredModel;  // Mate's preferred AI model

        @com.fasterxml.jackson.annotation.JsonIgnore
        private WebSocketSession session;
    }
}
