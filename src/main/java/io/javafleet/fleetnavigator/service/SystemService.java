package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.SystemStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * Service for system monitoring
 * Updated to fix GPU VRAM detection for NVIDIA cards
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemService {

    private final OllamaService ollamaService;

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

        // CPU usage (simplified - platform dependent)
        double cpuUsage = osBean.getSystemLoadAverage();
        if (cpuUsage < 0) {
            cpuUsage = 0.0; // Not available on some systems
        }

        // Anzahl der verfügbaren Prozessoren (Kerne)
        int cpuCores = runtime.availableProcessors();

        // Versuche detaillierte CPU-Info zu bekommen (platform-specific)
        double processCpuUsage = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean =
                (com.sun.management.OperatingSystemMXBean) osBean;
            processCpuUsage = sunOsBean.getProcessCpuLoad() * 100.0;

            // System CPU usage (falls verfügbar)
            double systemCpuLoad = sunOsBean.getCpuLoad() * 100.0;
            if (systemCpuLoad >= 0) {
                cpuUsage = systemCpuLoad;
            }
        }

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
}
