package io.javafleet.fleetnavigator.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * System Health Check Service
 * Überprüft beim Start:
 * - Ollama Installation und Erreichbarkeit
 * - Verfügbare Modelle
 * - System-Ressourcen (RAM, CPU)
 */
@Service
@Slf4j
public class SystemHealthCheckService {

    private final OkHttpClient httpClient;
    private final SystemInfo systemInfo;
    private boolean ollamaAvailable = false;
    private boolean hasModels = false;
    private boolean sufficientMemory = false;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    // Mindestanforderungen
    private static final long MIN_RAM_GB = 4;
    private static final long MIN_FREE_RAM_GB = 2;
    private static final String OLLAMA_URL = "http://localhost:11434";

    public SystemHealthCheckService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        this.systemInfo = new SystemInfo();
    }

    // Health check disabled on startup for native image compatibility
    // Call this method manually via REST API: GET /api/system/health
    // @EventListener(ApplicationReadyEvent.class)
    public void performHealthCheck() {
        log.info("🔍 Starting system health check...");

        checkMemory();
        checkOllama();
        checkModels();

        logResults();
    }

    private void checkMemory() {
        try {
            HardwareAbstractionLayer hardware = systemInfo.getHardware();
            GlobalMemory memory = hardware.getMemory();

            long totalMemoryGB = memory.getTotal() / (1024L * 1024L * 1024L);
            long availableMemoryGB = memory.getAvailable() / (1024L * 1024L * 1024L);

            log.info("💾 RAM: {} GB total, {} GB available", totalMemoryGB, availableMemoryGB);

            if (totalMemoryGB < MIN_RAM_GB) {
                errors.add(String.format(
                        "Insufficient RAM: %d GB (minimum %d GB required)",
                        totalMemoryGB, MIN_RAM_GB
                ));
                sufficientMemory = false;
            } else if (availableMemoryGB < MIN_FREE_RAM_GB) {
                warnings.add(String.format(
                        "Low available RAM: %d GB (recommended %d GB)",
                        availableMemoryGB, MIN_FREE_RAM_GB
                ));
                sufficientMemory = true;
            } else {
                sufficientMemory = true;
            }
        } catch (Exception e) {
            log.warn("⚠️  Could not check system memory: {}", e.getMessage());
            warnings.add("Could not determine system memory");
        }
    }

    private void checkOllama() {
        try {
            Request request = new Request.Builder()
                    .url(OLLAMA_URL + "/api/tags")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ollamaAvailable = true;
                    log.info("✅ Ollama is running at {}", OLLAMA_URL);
                } else {
                    ollamaAvailable = false;
                    errors.add("Ollama responded with error code: " + response.code());
                }
            }
        } catch (IOException e) {
            ollamaAvailable = false;
            errors.add("Ollama is not reachable at " + OLLAMA_URL);
            log.error("❌ Ollama is not reachable: {}", e.getMessage());
        }
    }

    private void checkModels() {
        if (!ollamaAvailable) {
            errors.add("Cannot check models: Ollama is not available");
            return;
        }

        try {
            Request request = new Request.Builder()
                    .url(OLLAMA_URL + "/api/tags")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    // Simple check: if response contains "models", we assume models exist
                    hasModels = body.contains("\"models\":[") && !body.contains("\"models\":[]");

                    if (hasModels) {
                        log.info("✅ Ollama models are available");
                    } else {
                        errors.add("No Ollama models installed");
                        log.error("❌ No Ollama models found");
                    }
                } else {
                    errors.add("Could not retrieve model list");
                }
            }
        } catch (IOException e) {
            errors.add("Failed to check models: " + e.getMessage());
            log.error("❌ Failed to check models: {}", e.getMessage());
        }
    }

    private void logResults() {
        log.info("========================================");
        log.info("System Health Check Results:");
        log.info("========================================");
        log.info("Ollama Available:    {}", ollamaAvailable ? "✅" : "❌");
        log.info("Models Available:    {}", hasModels ? "✅" : "❌");
        log.info("Sufficient Memory:   {}", sufficientMemory ? "✅" : "⚠️");

        if (!warnings.isEmpty()) {
            log.warn("⚠️  Warnings:");
            warnings.forEach(w -> log.warn("  - {}", w));
        }

        if (!errors.isEmpty()) {
            log.error("❌ Errors:");
            errors.forEach(e -> log.error("  - {}", e));
        }

        if (errors.isEmpty() && warnings.isEmpty()) {
            log.info("✅ All checks passed! System is ready.");
        } else if (!errors.isEmpty()) {
            log.error("❌ System is not fully operational. Please address the errors above.");
            log.error("");
            log.error("Installation Instructions:");
            logInstallationInstructions();
        }

        log.info("========================================");
    }

    private void logInstallationInstructions() {
        Locale locale = Locale.getDefault();
        boolean isGerman = locale.getLanguage().equals("de");

        if (!ollamaAvailable) {
            if (isGerman) {
                log.error("📦 Ollama Installation:");
                log.error("   Linux:   curl -fsSL https://ollama.ai/install.sh | sh");
                log.error("   macOS:   brew install ollama");
                log.error("   Windows: https://ollama.ai/download/windows");
                log.error("");
                log.error("   Nach der Installation: ollama serve");
            } else {
                log.error("📦 Ollama Installation:");
                log.error("   Linux:   curl -fsSL https://ollama.ai/install.sh | sh");
                log.error("   macOS:   brew install ollama");
                log.error("   Windows: https://ollama.ai/download/windows");
                log.error("");
                log.error("   After installation: ollama serve");
            }
        }

        if (!hasModels && ollamaAvailable) {
            if (isGerman) {
                log.error("🤖 Modell Installation:");
                log.error("   Empfohlen für Briefe: ollama pull llama3.2");
                log.error("   Groß und leistungsstark: ollama pull llama3.1:70b");
                log.error("   Schnell und klein: ollama pull llama3.2:3b");
            } else {
                log.error("🤖 Model Installation:");
                log.error("   Recommended for letters: ollama pull llama3.2");
                log.error("   Large and powerful: ollama pull llama3.1:70b");
                log.error("   Fast and small: ollama pull llama3.2:3b");
            }
        }

        if (!sufficientMemory) {
            if (isGerman) {
                log.error("💾 Arbeitsspeicher:");
                log.error("   Mindestens {} GB RAM erforderlich", MIN_RAM_GB);
                log.error("   Für große Modelle (70B+): 32 GB+ empfohlen");
            } else {
                log.error("💾 Memory:");
                log.error("   At least {} GB RAM required", MIN_RAM_GB);
                log.error("   For large models (70B+): 32 GB+ recommended");
            }
        }
    }

    // Public getters for health status
    public boolean isOllamaAvailable() {
        return ollamaAvailable;
    }

    public boolean hasModels() {
        return hasModels;
    }

    public boolean hasSufficientMemory() {
        return sufficientMemory;
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean isHealthy() {
        return ollamaAvailable && hasModels && sufficientMemory;
    }

    public String getHealthSummary() {
        if (isHealthy()) {
            return "All systems operational";
        }
        StringBuilder sb = new StringBuilder();
        if (!ollamaAvailable) sb.append("Ollama not available. ");
        if (!hasModels) sb.append("No models installed. ");
        if (!sufficientMemory) sb.append("Insufficient memory. ");
        return sb.toString().trim();
    }
}
