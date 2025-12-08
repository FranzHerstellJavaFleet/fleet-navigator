package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.config.LLMConfigProperties;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verwaltet den externen llama-server Prozess (Start/Stop/Restart)
 *
 * Der llama-server wird als Subprocess gestartet und kann über die REST API
 * gesteuert werden. Der Prozess wird automatisch beendet wenn Fleet Navigator
 * stoppt.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlamaServerProcessManager {

    private final LLMConfigProperties config;

    private Process llamaServerProcess;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private int currentPort = 2026;
    private String currentModel = null;
    private Thread outputThread;
    private Thread errorThread;

    // Default paths
    private static final String DEFAULT_BINARY_PATH = "./bin/llama-server";
    private static final String DEFAULT_MODELS_DIR = System.getProperty("user.home") + "/.java-fleet/models/library";
    private static final int DEFAULT_PORT = 2026;
    private static final int DEFAULT_CONTEXT_SIZE = 8192;
    private static final int DEFAULT_GPU_LAYERS = 99;

    /**
     * Startet den llama-server mit dem angegebenen Modell
     *
     * @param modelPath Pfad zum GGUF-Modell (kann relativ oder absolut sein)
     * @param port      Server Port (default: 2026)
     * @param contextSize Context Size in Tokens
     * @param gpuLayers Anzahl GPU Layers (-1=auto, 0=CPU, 99=alle)
     * @return StartResult mit Erfolg/Fehler-Informationen
     */
    public StartResult startServer(String modelPath, Integer port, Integer contextSize, Integer gpuLayers) {
        StartResult result = new StartResult();

        // Bereits laufend?
        if (isRunning.get() && llamaServerProcess != null && llamaServerProcess.isAlive()) {
            result.setSuccess(false);
            result.setMessage("llama-server läuft bereits auf Port " + currentPort);
            result.setPort(currentPort);
            result.setAlreadyRunning(true);
            return result;
        }

        // Parameter normalisieren
        int serverPort = port != null ? port : DEFAULT_PORT;
        int ctxSize = contextSize != null ? contextSize : DEFAULT_CONTEXT_SIZE;
        int ngl = gpuLayers != null ? gpuLayers : DEFAULT_GPU_LAYERS;

        // Modell-Pfad auflösen
        String resolvedModelPath = resolveModelPath(modelPath);
        if (resolvedModelPath == null) {
            result.setSuccess(false);
            result.setMessage("Modell nicht gefunden: " + modelPath);
            return result;
        }

        // Binary-Pfad ermitteln
        String binaryPath = resolveBinaryPath();
        if (binaryPath == null) {
            result.setSuccess(false);
            result.setMessage("llama-server Binary nicht gefunden. Bitte installieren Sie llama.cpp.");
            return result;
        }

        // Port freigeben falls belegt
        killProcessOnPort(serverPort);

        try {
            // Kommando zusammenbauen
            List<String> command = buildCommand(binaryPath, resolvedModelPath, serverPort, ctxSize, ngl);

            log.info("Starte llama-server: {}", String.join(" ", command));

            // ProcessBuilder konfigurieren
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir")));

            // Umgebungsvariablen setzen (LD_LIBRARY_PATH für CUDA)
            pb.environment().put("LD_LIBRARY_PATH", "./bin:" + System.getenv().getOrDefault("LD_LIBRARY_PATH", ""));

            // Prozess starten
            llamaServerProcess = pb.start();
            currentPort = serverPort;
            currentModel = resolvedModelPath;

            // Output-Threads starten (non-blocking logging)
            startOutputThreads();

            // Warten auf Server-Start (max 30 Sekunden)
            boolean started = waitForServerStart(serverPort, 30);

            if (started) {
                isRunning.set(true);
                result.setSuccess(true);
                result.setMessage("llama-server erfolgreich gestartet");
                result.setPort(serverPort);
                result.setModel(resolvedModelPath);
                result.setPid(llamaServerProcess.pid());
                log.info("llama-server gestartet auf Port {} mit Modell {}", serverPort, resolvedModelPath);
            } else {
                // Server hat nicht geantwortet - trotzdem als gestartet markieren
                // (manche Modelle brauchen länger zum Laden)
                isRunning.set(true);
                result.setSuccess(true);
                result.setMessage("llama-server gestartet (Modell wird noch geladen...)");
                result.setPort(serverPort);
                result.setModel(resolvedModelPath);
                result.setPid(llamaServerProcess.pid());
                result.setStillLoading(true);
                log.warn("llama-server gestartet aber Health-Check noch nicht erfolgreich - Modell lädt vermutlich noch");
            }

        } catch (Exception e) {
            log.error("Fehler beim Starten des llama-server: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Fehler beim Starten: " + e.getMessage());
        }

        return result;
    }

    /**
     * Stoppt den llama-server
     */
    public StopResult stopServer() {
        StopResult result = new StopResult();

        if (!isRunning.get() || llamaServerProcess == null) {
            // Versuche trotzdem den Port zu killen (falls extern gestartet)
            killProcessOnPort(currentPort);
            result.setSuccess(true);
            result.setMessage("llama-server war nicht gestartet (Port wurde freigegeben)");
            return result;
        }

        try {
            log.info("Stoppe llama-server auf Port {}", currentPort);

            // Graceful shutdown versuchen
            llamaServerProcess.destroy();

            // Warten auf Beendigung (max 5 Sekunden)
            boolean terminated = llamaServerProcess.waitFor(5, TimeUnit.SECONDS);

            if (!terminated) {
                // Force kill
                llamaServerProcess.destroyForcibly();
                llamaServerProcess.waitFor(2, TimeUnit.SECONDS);
            }

            // Port sicherheitshalber freigeben
            killProcessOnPort(currentPort);

            isRunning.set(false);
            llamaServerProcess = null;

            // Output-Threads stoppen
            if (outputThread != null) outputThread.interrupt();
            if (errorThread != null) errorThread.interrupt();

            result.setSuccess(true);
            result.setMessage("llama-server erfolgreich gestoppt");
            log.info("llama-server gestoppt");

        } catch (Exception e) {
            log.error("Fehler beim Stoppen des llama-server: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Fehler beim Stoppen: " + e.getMessage());
        }

        return result;
    }

    /**
     * Startet den llama-server neu (Stop + Start)
     */
    public StartResult restartServer(String modelPath, Integer port, Integer contextSize, Integer gpuLayers) {
        log.info("Neustart des llama-server angefordert");

        // Zuerst stoppen
        stopServer();

        // Kurz warten
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Dann starten
        return startServer(modelPath, port, contextSize, gpuLayers);
    }

    /**
     * Prüft ob der llama-server läuft und erreichbar ist
     */
    public ServerStatus getStatus() {
        ServerStatus status = new ServerStatus();
        status.setPort(currentPort);
        status.setModel(currentModel);

        // Prozess-Status
        boolean processAlive = llamaServerProcess != null && llamaServerProcess.isAlive();
        status.setProcessRunning(processAlive);

        if (processAlive) {
            status.setPid(llamaServerProcess.pid());
        }

        // Health-Check
        status.setOnline(checkHealth(currentPort));
        status.setRunning(status.isOnline() || processAlive);

        return status;
    }

    /**
     * Gibt verfügbare GGUF-Modelle zurück
     */
    public List<String> getAvailableModels() {
        List<String> models = new ArrayList<>();

        Path modelsDir = Paths.get(DEFAULT_MODELS_DIR);
        if (Files.exists(modelsDir)) {
            try {
                Files.list(modelsDir)
                    .filter(p -> p.toString().toLowerCase().endsWith(".gguf"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .forEach(models::add);
            } catch (IOException e) {
                log.warn("Konnte Modell-Verzeichnis nicht lesen: {}", e.getMessage());
            }
        }

        return models;
    }

    // ===== Private Hilfsmethoden =====

    private List<String> buildCommand(String binaryPath, String modelPath, int port, int contextSize, int gpuLayers) {
        List<String> cmd = new ArrayList<>();
        cmd.add(binaryPath);
        cmd.add("-m");
        cmd.add(modelPath);
        cmd.add("--port");
        cmd.add(String.valueOf(port));
        cmd.add("--ctx-size");
        cmd.add(String.valueOf(contextSize));
        cmd.add("-ngl");
        cmd.add(String.valueOf(gpuLayers));
        cmd.add("--host");
        cmd.add("0.0.0.0"); // Von überall erreichbar
        return cmd;
    }

    private String resolveModelPath(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            // Default-Modell suchen
            List<String> models = getAvailableModels();
            if (!models.isEmpty()) {
                return Paths.get(DEFAULT_MODELS_DIR, models.get(0)).toString();
            }
            return null;
        }

        // Absoluter Pfad?
        Path path = Paths.get(modelPath);
        if (Files.exists(path)) {
            return path.toAbsolutePath().toString();
        }

        // Relativer Pfad im Models-Verzeichnis?
        Path inModelsDir = Paths.get(DEFAULT_MODELS_DIR, modelPath);
        if (Files.exists(inModelsDir)) {
            return inModelsDir.toAbsolutePath().toString();
        }

        // Nur Dateiname? (Suche in Models-Dir)
        try {
            Path found = Files.list(Paths.get(DEFAULT_MODELS_DIR))
                .filter(p -> p.getFileName().toString().contains(modelPath))
                .findFirst()
                .orElse(null);
            if (found != null) {
                return found.toAbsolutePath().toString();
            }
        } catch (IOException e) {
            log.warn("Fehler beim Suchen des Modells: {}", e.getMessage());
        }

        return null;
    }

    private String resolveBinaryPath() {
        // 1. Konfigurierter Pfad
        String configPath = config.getLlamacpp().getBinaryPath();
        if (configPath != null && Files.exists(Paths.get(configPath))) {
            return configPath;
        }

        // 2. Standard-Pfade prüfen
        String[] possiblePaths = {
            "./bin/llama-server",
            "/usr/local/bin/llama-server",
            "/opt/fleet-navigator/bin/llama-server",
            System.getProperty("user.home") + "/.local/bin/llama-server"
        };

        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        return null;
    }

    private void killProcessOnPort(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "fuser -k " + port + "/tcp 2>/dev/null || true");
            Process p = pb.start();
            p.waitFor(5, TimeUnit.SECONDS);
            Thread.sleep(500); // Kurz warten bis Port frei
        } catch (Exception e) {
            log.debug("Port-Kill fehlgeschlagen (ignoriert): {}", e.getMessage());
        }
    }

    private boolean waitForServerStart(int port, int maxSeconds) {
        for (int i = 0; i < maxSeconds; i++) {
            if (checkHealth(port)) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean checkHealth(int port) {
        try {
            URL url = new URL("http://localhost:" + port + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void startOutputThreads() {
        // Stdout logging
        outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(llamaServerProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[llama-server] {}", line);
                }
            } catch (IOException e) {
                // Stream closed - normal bei Shutdown
            }
        }, "llama-server-stdout");
        outputThread.setDaemon(true);
        outputThread.start();

        // Stderr logging
        errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(llamaServerProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // llama-server gibt viel auf stderr aus (auch normale Logs)
                    if (line.contains("error") || line.contains("Error") || line.contains("ERROR")) {
                        log.warn("[llama-server] {}", line);
                    } else {
                        log.debug("[llama-server] {}", line);
                    }
                }
            } catch (IOException e) {
                // Stream closed - normal bei Shutdown
            }
        }, "llama-server-stderr");
        errorThread.setDaemon(true);
        errorThread.start();
    }

    /**
     * Cleanup bei Application-Shutdown
     */
    @PreDestroy
    public void cleanup() {
        log.info("Fleet Navigator wird beendet - stoppe llama-server");
        stopServer();
    }

    // ===== DTOs =====

    @Data
    public static class StartResult {
        private boolean success;
        private String message;
        private int port;
        private String model;
        private long pid;
        private boolean alreadyRunning;
        private boolean stillLoading;
    }

    @Data
    public static class StopResult {
        private boolean success;
        private String message;
    }

    @Data
    public static class ServerStatus {
        private boolean running;
        private boolean online;
        private boolean processRunning;
        private int port;
        private String model;
        private long pid;
    }
}
