package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for system status monitoring
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatus {
    // Backend Version Info
    private String backendVersion;
    private String backendBuildTime;

    private Double cpuUsage;
    private Integer cpuCores;  // Anzahl der CPU-Kerne
    private Double processCpuUsage;  // CPU-Auslastung des Java-Prozesses
    private String cpuModel;  // CPU-Modell (z.B. "Intel Core i7-9700K")
    private String cpuFrequency;  // CPU-Frequenz (z.B. "3.6 GHz")

    private Long totalMemory;  // JVM Memory
    private Long freeMemory;
    private Long usedMemory;
    private Long systemTotalMemory;  // Gesamter physischer RAM
    private Long systemFreeMemory;  // Freier physischer RAM

    private Boolean ollamaAvailable;
    private String ollamaVersion;
    private String osName;  // Betriebssystem
    private String osVersion;  // OS-Version

    // GPU Information
    private String gpuName;  // GPU-Modell (z.B. "NVIDIA GeForce RTX 3080")
    private Long gpuMemoryTotal;  // VRAM gesamt in Bytes
    private Long gpuMemoryUsed;  // VRAM verwendet in Bytes
    private Double gpuTemperature;  // GPU-Temperatur in Celsius
    private Double gpuUtilization;  // GPU-Auslastung in Prozent
}
