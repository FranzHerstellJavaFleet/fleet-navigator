package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.SystemStatus;
import io.javafleet.fleetnavigator.model.DbSizeHistory;
import io.javafleet.fleetnavigator.repository.DbSizeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for system monitoring
 * Updated to fix GPU VRAM detection for NVIDIA cards
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemService {

    private final OllamaService ollamaService;
    private final DbSizeHistoryRepository dbSizeHistoryRepository;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Value("${spring.datasource.url:jdbc:h2:file:~/.java-fleet/data/fleetnavdb}")
    private String datasourceUrl;

    private static final DateTimeFormatter GERMAN_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Berlin"));

    // Cache für Hardware-Infos (ändern sich nicht)
    private static String cachedCpuModel = null;
    private static String cachedCpuFrequency = null;
    private static String cachedGpuName = null;
    private static Long cachedGpuMemoryTotal = null;
    private static SystemInfo systemInfo = null;

    /**
     * Get current system status
     */
    public SystemStatus getSystemStatus() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        // Anzahl der verfügbaren Prozessoren (Kerne)
        int cpuCores = runtime.availableProcessors();

        // CPU usage - get accurate system CPU load
        double cpuUsage = 0.0;
        double processCpuUsage = 0.0;

        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean =
                (com.sun.management.OperatingSystemMXBean) osBean;

            // Get system-wide CPU load (0.0 to 1.0, multiply by 100 for percentage)
            double systemCpuLoad = sunOsBean.getCpuLoad();
            if (systemCpuLoad >= 0) {
                cpuUsage = systemCpuLoad * 100.0;
            } else {
                // Fallback: Use load average as rough estimate
                double loadAverage = osBean.getSystemLoadAverage();
                if (loadAverage >= 0) {
                    // Convert load average to percentage (load / cores * 100)
                    cpuUsage = (loadAverage / cpuCores) * 100.0;
                    // Cap at 100%
                    cpuUsage = Math.min(cpuUsage, 100.0);
                }
            }

            // Process CPU usage (0.0 to 1.0, multiply by 100 for percentage)
            double processCpuLoad = sunOsBean.getProcessCpuLoad();
            if (processCpuLoad >= 0) {
                processCpuUsage = processCpuLoad * 100.0;
            }
        }

        log.debug("CPU Usage: system={}%, process={}%, cores={}", cpuUsage, processCpuUsage, cpuCores);

        // CPU-Hardware-Informationen
        if (cachedCpuModel == null) {
            readCpuInfo();
        }

        // GPU-Hardware-Informationen
        // Wenn Cache ungültig ist (null oder verdächtig niedriger Wert), neu laden
        long oneGB = 1024L * 1024L * 1024L;
        if (cachedGpuName == null || (cachedGpuMemoryTotal != null && cachedGpuMemoryTotal > 0 && cachedGpuMemoryTotal < oneGB)) {
            readGpuInfo();
        }

        // System RAM (physischer Speicher)
        long systemTotalMemory = 0;
        long systemFreeMemory = 0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean =
                (com.sun.management.OperatingSystemMXBean) osBean;
            systemTotalMemory = sunOsBean.getTotalMemorySize();
            systemFreeMemory = sunOsBean.getFreeMemorySize();
        }

        // Betriebssystem-Informationen
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        // Check Ollama availability
        boolean ollamaAvailable = ollamaService.isOllamaAvailable();

        SystemStatus status = new SystemStatus();

        // Backend Version Info
        if (buildProperties != null) {
            status.setBackendVersion(buildProperties.getVersion());
            if (buildProperties.getTime() != null) {
                status.setBackendBuildTime(GERMAN_DATE_FORMAT.format(buildProperties.getTime()));
            }
        } else {
            status.setBackendVersion("dev");
            status.setBackendBuildTime("development");
        }

        status.setCpuUsage(cpuUsage);
        status.setCpuCores(cpuCores);
        status.setProcessCpuUsage(processCpuUsage);
        status.setCpuModel(cachedCpuModel);
        status.setCpuFrequency(cachedCpuFrequency);

        status.setTotalMemory(totalMemory);
        status.setFreeMemory(freeMemory);
        status.setUsedMemory(usedMemory);
        status.setSystemTotalMemory(systemTotalMemory);
        status.setSystemFreeMemory(systemFreeMemory);

        status.setOllamaAvailable(ollamaAvailable);
        status.setOllamaVersion("Unknown");
        status.setOsName(osName);
        status.setOsVersion(osVersion);

        // GPU Informationen
        status.setGpuName(cachedGpuName);
        status.setGpuMemoryTotal(cachedGpuMemoryTotal);
        log.info("Setting GPU info: name={}, memoryTotal={} bytes ({} GB)",
            cachedGpuName,
            cachedGpuMemoryTotal,
            cachedGpuMemoryTotal != null ? cachedGpuMemoryTotal / (1024.0 * 1024.0 * 1024.0) : 0);
        // GPU-Auslastung dynamisch abrufen
        readDynamicGpuStats(status);

        return status;
    }

    /**
     * Liest CPU-Informationen aus /proc/cpuinfo (Linux)
     */
    private void readCpuInfo() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("model name")) {
                    cachedCpuModel = line.split(":")[1].trim();
                } else if (line.startsWith("cpu MHz")) {
                    String mhz = line.split(":")[1].trim();
                    double ghz = Double.parseDouble(mhz) / 1000.0;
                    cachedCpuFrequency = String.format("%.2f GHz", ghz);
                    break; // Wir haben alles was wir brauchen
                }
            }
        } catch (IOException | NumberFormatException e) {
            log.warn("Could not read CPU info from /proc/cpuinfo", e);
            cachedCpuModel = "Unknown";
            cachedCpuFrequency = "Unknown";
        }

        if (cachedCpuModel == null) {
            cachedCpuModel = "Unknown";
        }
        if (cachedCpuFrequency == null) {
            cachedCpuFrequency = "Unknown";
        }
    }

    /**
     * Liest GPU-Informationen mit OSHI + nvidia-smi
     */
    private void readGpuInfo() {
        log.info("Starting GPU detection...");
        try {
            if (systemInfo == null) {
                systemInfo = new SystemInfo();
                log.info("SystemInfo initialized");
            }
            HardwareAbstractionLayer hardware = systemInfo.getHardware();

            var graphicsCards = hardware.getGraphicsCards();
            log.info("OSHI found {} graphics card(s)", graphicsCards.size());

            if (!graphicsCards.isEmpty()) {
                GraphicsCard gpu = graphicsCards.get(0); // Erste GPU verwenden
                cachedGpuName = gpu.getName();
                cachedGpuMemoryTotal = gpu.getVRam();

                log.info("OSHI GPU Name: {}", cachedGpuName);
                log.info("OSHI VRAM: {} bytes ({})", cachedGpuMemoryTotal,
                    cachedGpuMemoryTotal > 0 ? (cachedGpuMemoryTotal / (1024 * 1024 * 1024) + " GB") : "0 GB");

                // Wenn OSHI keine VRAM-Größe liefert ODER eine verdächtig niedrige Größe zurückgibt (< 1GB),
                // versuche nvidia-smi fallback (OSHI kann manchmal falsche Werte zurückgeben)
                long oneGB = 1024L * 1024L * 1024L;
                String gpuNameLower = cachedGpuName.toLowerCase();
                boolean isNvidiaGpu = gpuNameLower.contains("nvidia") || gpuNameLower.contains("geforce") || gpuNameLower.contains("rtx");
                if (isNvidiaGpu && (cachedGpuMemoryTotal == null || cachedGpuMemoryTotal == 0 || cachedGpuMemoryTotal < oneGB)) {
                    log.info("OSHI returned suspicious VRAM value ({} bytes), trying nvidia-smi fallback...", cachedGpuMemoryTotal);
                    cachedGpuMemoryTotal = readVramFromNvidiaSmi();
                    log.info("nvidia-smi VRAM: {} bytes ({})", cachedGpuMemoryTotal,
                        cachedGpuMemoryTotal > 0 ? (cachedGpuMemoryTotal / (1024 * 1024 * 1024) + " GB") : "0 GB");
                }

                log.info("GPU detected: {} with {} GB VRAM", cachedGpuName,
                    cachedGpuMemoryTotal > 0 ? (cachedGpuMemoryTotal / (1024 * 1024 * 1024)) : 0);
            } else {
                log.info("No GPU detected by OSHI");
                cachedGpuName = "No GPU detected";
                cachedGpuMemoryTotal = 0L;
            }
        } catch (Exception e) {
            log.warn("Could not read GPU info with OSHI", e);
            cachedGpuName = "Unknown";
            cachedGpuMemoryTotal = 0L;
        }
    }

    /**
     * Liest VRAM-Größe mit nvidia-smi
     */
    private Long readVramFromNvidiaSmi() {
        log.info("Attempting to read VRAM from nvidia-smi...");
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "nvidia-smi",
                "--query-gpu=memory.total",
                "--format=csv,noheader,nounits"
            });

            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                log.info("nvidia-smi output: {}", line);
                if (line != null) {
                    // nvidia-smi returns memory in MB
                    long vramMB = Long.parseLong(line.trim());
                    long vramBytes = vramMB * 1024 * 1024; // Convert to bytes
                    log.info("Parsed nvidia-smi: {} MB = {} bytes", vramMB, vramBytes);
                    return vramBytes;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.warn("Could not read VRAM from nvidia-smi: {}", e.getMessage(), e);
        }
        log.warn("nvidia-smi returned no data, defaulting to 0");
        return 0L;
    }

    /**
     * Liest dynamische GPU-Statistiken (Auslastung, Temperatur, VRAM-Nutzung)
     * Da OSHI keine GPU-Auslastung bietet, verwenden wir nvidia-smi für NVIDIA GPUs
     */
    private void readDynamicGpuStats(SystemStatus status) {
        if (cachedGpuName == null || cachedGpuName.contains("Unknown") || cachedGpuName.contains("No GPU")) {
            return;
        }

        // Für NVIDIA GPUs: nvidia-smi verwenden
        String gpuLower = cachedGpuName.toLowerCase();
        if (gpuLower.contains("nvidia") || gpuLower.contains("geforce") ||
            gpuLower.contains("rtx") || gpuLower.contains("gtx") ||
            gpuLower.contains("quadro") || gpuLower.contains("tesla")) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{
                    "nvidia-smi",
                    "--query-gpu=memory.used,temperature.gpu,utilization.gpu",
                    "--format=csv,noheader,nounits"
                });

                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null) {
                        String[] parts = line.trim().split(",\\s*");
                        if (parts.length >= 3) {
                            // VRAM used (in MB)
                            status.setGpuMemoryUsed(Long.parseLong(parts[0]) * 1024 * 1024);
                            // Temperature
                            status.setGpuTemperature(Double.parseDouble(parts[1]));
                            // Utilization
                            status.setGpuUtilization(Double.parseDouble(parts[2]));
                        }
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                log.debug("Could not read GPU stats from nvidia-smi: {}", e.getMessage());
                // Setze Defaults
                status.setGpuMemoryUsed(0L);
                status.setGpuTemperature(0.0);
                status.setGpuUtilization(0.0);
            }
        } else {
            // Für andere GPUs: Keine dynamischen Stats verfügbar
            status.setGpuMemoryUsed(0L);
            status.setGpuTemperature(0.0);
            status.setGpuUtilization(0.0);
        }
    }

    /**
     * Ermittelt die Größe der H2-Datenbank in Bytes
     */
    public long getDatabaseSizeBytes() {
        try {
            // H2 URL: jdbc:h2:file:./data/fleetnavdb oder jdbc:h2:file:/opt/fleet-navigator/data/fleetnavdb
            String dbPath = extractDbPathFromUrl(datasourceUrl);
            if (dbPath == null) {
                log.warn("Could not extract database path from URL: {}", datasourceUrl);
                return 0;
            }

            long totalSize = 0;

            // H2 erstellt mehrere Dateien: .mv.db (Hauptdatei), .trace.db (optional)
            File mvDbFile = new File(dbPath + ".mv.db");
            File traceDbFile = new File(dbPath + ".trace.db");

            if (mvDbFile.exists()) {
                totalSize += mvDbFile.length();
                log.debug("H2 main file: {} bytes", mvDbFile.length());
            }
            if (traceDbFile.exists()) {
                totalSize += traceDbFile.length();
                log.debug("H2 trace file: {} bytes", traceDbFile.length());
            }

            log.info("Database size: {} bytes", totalSize);
            return totalSize;
        } catch (Exception e) {
            log.error("Error reading database size", e);
            return 0;
        }
    }

    private String extractDbPathFromUrl(String url) {
        // jdbc:h2:file:./data/fleetnavdb -> ./data/fleetnavdb
        // jdbc:h2:file:/opt/fleet-navigator/data/fleetnavdb -> /opt/fleet-navigator/data/fleetnavdb
        if (url == null || !url.contains("jdbc:h2:file:")) {
            return null;
        }
        String path = url.replace("jdbc:h2:file:", "");
        // Entferne mögliche Parameter wie ;MODE=...
        int semicolonIndex = path.indexOf(';');
        if (semicolonIndex > 0) {
            path = path.substring(0, semicolonIndex);
        }
        return path;
    }

    // ========== Database Size History ==========

    /**
     * Scheduled job: Records database size every 30 minutes
     * fixedRate = 30 minutes in milliseconds
     */
    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void recordDatabaseSize() {
        try {
            long sizeBytes = getDatabaseSizeBytes();
            DbSizeHistory history = new DbSizeHistory(sizeBytes);
            dbSizeHistoryRepository.save(history);
            log.info("Recorded database size: {} bytes ({} MB)", sizeBytes, sizeBytes / (1024 * 1024));
        } catch (Exception e) {
            log.error("Failed to record database size", e);
        }
    }

    /**
     * Get the latest database size measurement
     */
    public DbSizeHistory getLatestDbSizeHistory() {
        return dbSizeHistoryRepository.findFirstByOrderByRecordedAtDesc().orElse(null);
    }

    /**
     * Get recent database size measurements (last 100)
     */
    public List<DbSizeHistory> getRecentDbSizeHistory() {
        return dbSizeHistoryRepository.findTop100ByOrderByRecordedAtDesc();
    }

    /**
     * Get database size measurements since a specific date
     */
    public List<DbSizeHistory> getDbSizeHistorySince(LocalDateTime since) {
        return dbSizeHistoryRepository.findByRecordedAtAfterOrderByRecordedAtAsc(since);
    }

    /**
     * Get all database size measurements (ordered by time ascending)
     */
    public List<DbSizeHistory> getAllDbSizeHistory() {
        return dbSizeHistoryRepository.findAllByOrderByRecordedAtDesc();
    }
}
