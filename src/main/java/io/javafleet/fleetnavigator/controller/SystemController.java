package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.config.FleetPathsConfiguration;
import io.javafleet.fleetnavigator.config.LlamaServerAutoStartListener;
import io.javafleet.fleetnavigator.dto.SystemStatus;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.model.DbSizeHistory;
import io.javafleet.fleetnavigator.service.LLMProviderService;
import io.javafleet.fleetnavigator.service.LlamaServerProcessManager;
import io.javafleet.fleetnavigator.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * REST Controller for system monitoring
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
public class SystemController {

    private final SystemService systemService;
    private final LLMProviderService llmProviderService;
    private final FleetPathsConfiguration pathsConfig;
    private final LlamaServerAutoStartListener llamaServerAutoStart;
    private final LlamaServerProcessManager llamaServerManager;

    @Value("${fleet-navigator.version:0.5.0}")
    private String appVersion;

    @Value("${fleet-navigator.build-time:unknown}")
    private String buildTime;

    /**
     * GET /api/system/status - Get system status
     */
    @GetMapping("/status")
    public ResponseEntity<SystemStatus> getSystemStatus() {
        log.debug("Fetching system status");
        SystemStatus status = systemService.getSystemStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/system/db-size - Get database size in bytes
     */
    @GetMapping("/db-size")
    public ResponseEntity<DbSizeResponse> getDatabaseSize() {
        log.debug("Fetching database size");
        long sizeBytes = systemService.getDatabaseSizeBytes();
        return ResponseEntity.ok(new DbSizeResponse(sizeBytes, formatSize(sizeBytes)));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public record DbSizeResponse(long sizeBytes, String formatted) {}

    /**
     * GET /api/system/db-size/history - Get database size history (last 100 measurements)
     */
    @GetMapping("/db-size/history")
    public ResponseEntity<List<DbSizeHistoryResponse>> getDatabaseSizeHistory() {
        log.debug("Fetching database size history");
        List<DbSizeHistory> history = systemService.getRecentDbSizeHistory();
        List<DbSizeHistoryResponse> response = history.stream()
            .map(h -> new DbSizeHistoryResponse(
                h.getId(),
                h.getSizeBytes(),
                formatSize(h.getSizeBytes()),
                h.getRecordedAt()
            ))
            .toList();
        return ResponseEntity.ok(response);
    }

    public record DbSizeHistoryResponse(Long id, long sizeBytes, String formatted, LocalDateTime recordedAt) {}

    /**
     * GET /api/system/version - Get application version for cache invalidation
     * Frontend uses this to detect version changes and clear browser cache
     */
    @GetMapping("/version")
    public ResponseEntity<VersionResponse> getVersion() {
        log.debug("Fetching application version: {}", appVersion);
        return ResponseEntity.ok(new VersionResponse(appVersion, buildTime, System.currentTimeMillis()));
    }

    public record VersionResponse(String version, String buildTime, long serverTime) {}

    /**
     * GET /api/system/setup-status - Check if initial setup is needed
     *
     * Setup is needed when:
     * 1. Models directory doesn't exist OR
     * 2. Models directory is empty (no .gguf files in library/ or custom/)
     *
     * This ensures the wizard only appears on truly fresh installations.
     */
    @GetMapping("/setup-status")
    public ResponseEntity<SetupStatusResponse> getSetupStatus() {
        log.debug("Checking setup status");

        Path modelsDir = pathsConfig.getResolvedModelsDir();
        Path libraryDir = modelsDir.resolve("library");
        Path customDir = modelsDir.resolve("custom");

        // Check if models directory structure exists
        boolean modelsDirExists = Files.exists(modelsDir);
        boolean libraryDirExists = Files.exists(libraryDir);

        // Count .gguf files in library and custom directories
        int ggufCount = 0;
        ggufCount += countGgufFiles(libraryDir);
        ggufCount += countGgufFiles(customDir);
        ggufCount += countGgufFiles(modelsDir); // Also check root models dir

        boolean hasGgufModels = ggufCount > 0;

        // Also check via provider (for Ollama models etc.)
        int providerModelCount = 0;
        String activeProvider = "unknown";
        try {
            activeProvider = llmProviderService.getActiveProvider().getProviderName();
            List<ModelInfo> models = llmProviderService.getAvailableModels();
            providerModelCount = models.size();
        } catch (IOException e) {
            log.debug("Could not check provider models: {}", e.getMessage());
        }

        // Has models if either GGUF files exist OR provider reports models
        boolean hasModels = hasGgufModels || providerModelCount > 0;
        int totalModelCount = Math.max(ggufCount, providerModelCount);

        // Setup needed only if NO models at all (fresh install)
        boolean needsSetup = !hasModels;

        log.info("Setup status: needsSetup={}, ggufCount={}, providerModels={}, modelsDir={}",
            needsSetup, ggufCount, providerModelCount, modelsDir);

        return ResponseEntity.ok(new SetupStatusResponse(
            needsSetup,
            hasModels,
            totalModelCount,
            activeProvider,
            appVersion,
            modelsDir.toString(),
            libraryDir.toString(),
            customDir.toString(),
            System.getProperty("os.name")
        ));
    }

    /**
     * Count .gguf files in a directory (non-recursive)
     */
    private int countGgufFiles(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return (int) files
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".gguf"))
                .count();
        } catch (IOException e) {
            log.warn("Error counting GGUF files in {}: {}", dir, e.getMessage());
            return 0;
        }
    }

    public record SetupStatusResponse(
        boolean needsSetup,
        boolean hasModels,
        int modelCount,
        String activeProvider,
        String version,
        String modelsDirectory,
        String libraryDirectory,
        String customDirectory,
        String osType
    ) {}

    /**
     * GET /api/system/paths - Get all configured paths
     * Useful for debugging and documentation
     */
    @GetMapping("/paths")
    public ResponseEntity<PathsResponse> getPaths() {
        return ResponseEntity.ok(new PathsResponse(
            pathsConfig.getResolvedBaseDir().toString(),
            pathsConfig.getResolvedDataDir().toString(),
            pathsConfig.getResolvedModelsDir().toString(),
            pathsConfig.getResolvedModelsDir().resolve("library").toString(),
            pathsConfig.getResolvedModelsDir().resolve("custom").toString(),
            pathsConfig.getResolvedLogsDir().toString(),
            pathsConfig.getResolvedConfigDir().toString(),
            System.getProperty("os.name")
        ));
    }

    public record PathsResponse(
        String baseDir,
        String dataDir,
        String modelsDir,
        String libraryDir,
        String customDir,
        String logsDir,
        String configDir,
        String osName
    ) {}

    /**
     * GET /api/system/ai-startup-status - Get AI/llama-server startup status
     *
     * Frontend uses this to show a loading animation while the AI server starts.
     * The response includes:
     * - inProgress: true while server is starting
     * - complete: true when startup finished (success or error)
     * - message: Human-readable status message
     * - error: Error message if startup failed (null otherwise)
     * - serverOnline: true if llama-server is responding to health checks
     */
    @GetMapping("/ai-startup-status")
    public ResponseEntity<AiStartupStatusResponse> getAiStartupStatus() {
        LlamaServerProcessManager.ServerStatus serverStatus = llamaServerManager.getStatus();

        return ResponseEntity.ok(new AiStartupStatusResponse(
            llamaServerAutoStart.isStartupInProgress(),
            llamaServerAutoStart.isStartupComplete(),
            llamaServerAutoStart.getStartupMessage(),
            llamaServerAutoStart.getStartupError(),
            serverStatus.isOnline(),
            serverStatus.getPort(),
            serverStatus.getModel()
        ));
    }

    public record AiStartupStatusResponse(
        boolean inProgress,
        boolean complete,
        String message,
        String error,
        boolean serverOnline,
        int port,
        String model
    ) {}
}
