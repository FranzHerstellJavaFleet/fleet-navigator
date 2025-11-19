package io.javafleet.fleetnavigator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Hardware statistics from Fleet Mate
 */
@Data
public class HardwareStats {
    private OffsetDateTime timestamp;

    @JsonProperty("mate_id")
    private String mateId;

    private CPUStats cpu;
    private MemoryStats memory;
    private List<DiskStats> disk;
    private TemperatureStats temperature;
    private List<NetworkStats> network;
    private List<GPUStats> gpu;
    private SystemStats system;

    @Data
    public static class CPUStats {
        @JsonProperty("usage_percent")
        private double usagePercent;

        @JsonProperty("per_core")
        private List<Double> perCore;

        private int cores;
        private String model;
        private double mhz;
    }

    @Data
    public static class MemoryStats {
        private long total;
        private long available;
        private long used;

        @JsonProperty("used_percent")
        private double usedPercent;

        @JsonProperty("swap_total")
        private Long swapTotal;

        @JsonProperty("swap_used")
        private Long swapUsed;

        @JsonProperty("swap_percent")
        private Double swapPercent;
    }

    @Data
    public static class DiskStats {
        @JsonProperty("mount_point")
        private String mountPoint;

        private String device;

        @JsonProperty("fs_type")
        private String fsType;

        private long total;
        private long free;
        private long used;

        @JsonProperty("used_percent")
        private double usedPercent;
    }

    @Data
    public static class TemperatureStats {
        private List<SensorTemp> sensors;
    }

    @Data
    public static class SensorTemp {
        private String name;
        private double temperature;
        private Double high;
        private Double critical;
    }

    @Data
    public static class NetworkStats {
        @JsonProperty("interface")
        private String interfaceName;

        @JsonProperty("bytes_sent")
        private long bytesSent;

        @JsonProperty("bytes_recv")
        private long bytesRecv;

        @JsonProperty("packets_sent")
        private long packetsSent;

        @JsonProperty("packets_recv")
        private long packetsRecv;

        private long errin;
        private long errout;
    }

    @Data
    public static class GPUStats {
        private int index;
        private String name;

        @JsonProperty("utilization_gpu")
        private double utilizationGpu;

        @JsonProperty("memory_total")
        private long memoryTotal;

        @JsonProperty("memory_used")
        private long memoryUsed;

        @JsonProperty("memory_free")
        private long memoryFree;

        @JsonProperty("memory_used_percent")
        private double memoryUsedPercent;

        private double temperature;
    }

    @Data
    public static class SystemStats {
        private String hostname;
        private String os;
        private String platform;

        @JsonProperty("platform_version")
        private String platformVersion;

        @JsonProperty("kernel_version")
        private String kernelVersion;

        private long uptime;
    }
}
