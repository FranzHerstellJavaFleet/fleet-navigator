package io.javafleet.fleetnavigator.config;

import io.javafleet.fleetnavigator.service.LlamaServerProcessManager;
import io.javafleet.fleetnavigator.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Startet den llama-server automatisch beim Anwendungsstart.
 *
 * Der Server wird asynchron im Hintergrund gestartet, sodass die Anwendung
 * sofort verfügbar ist. Das Frontend kann den Startup-Status abfragen und
 * eine Loading-Animation anzeigen.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.1
 */
@Component
public class LlamaServerAutoStartListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(LlamaServerAutoStartListener.class);

    private final LlamaServerProcessManager llamaServerManager;
    private final SettingsService settingsService;

    @Value("${llm.default-provider:java-llama-cpp}")
    private String defaultProvider;

    // Startup-Status für Frontend-Abfrage
    private final AtomicBoolean startupInProgress = new AtomicBoolean(false);
    private final AtomicBoolean startupComplete = new AtomicBoolean(false);
    private final AtomicReference<String> startupMessage = new AtomicReference<>("Initialisiere...");
    private final AtomicReference<String> startupError = new AtomicReference<>(null);

    public LlamaServerAutoStartListener(LlamaServerProcessManager llamaServerManager,
                                        SettingsService settingsService) {
        this.llamaServerManager = llamaServerManager;
        this.settingsService = settingsService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Ollama braucht keinen Auto-Start - hat eigenen Service
        if ("ollama".equals(defaultProvider)) {
            log.info("📦 Ollama Provider aktiv - kein llama-server Auto-Start nötig");
            startupComplete.set(true);
            return;
        }

        log.info("🚀 Fleet Navigator bereit - starte llama-server automatisch...");

        // Asynchron starten um App nicht zu blockieren
        new Thread(this::startLlamaServer, "LlamaServerAutoStart").start();
    }

    private void startLlamaServer() {
        startupInProgress.set(true);
        startupMessage.set("Suche verfügbare Modelle...");

        try {
            // Kurze Verzögerung für sauberen App-Start
            Thread.sleep(2000);

            // Prüfe ob bereits ein Server läuft
            LlamaServerProcessManager.ServerStatus status = llamaServerManager.getStatus();
            if (status.isOnline()) {
                log.info("✅ llama-server läuft bereits auf Port {}", status.getPort());
                startupMessage.set("Server läuft bereits");
                startupComplete.set(true);
                startupInProgress.set(false);
                return;
            }

            // Verfügbare Modelle prüfen
            List<String> availableModels = llamaServerManager.getAvailableModels();
            if (availableModels.isEmpty()) {
                log.warn("⚠️ Keine GGUF-Modelle gefunden in ~/.java-fleet/models/library/");
                startupMessage.set("Keine Modelle gefunden");
                startupError.set("Bitte laden Sie ein GGUF-Modell herunter");
                startupComplete.set(true);
                startupInProgress.set(false);
                return;
            }

            // Modell auswählen (gespeichertes oder erstes verfügbares)
            String modelToLoad = selectModel(availableModels);
            log.info("📦 Ausgewähltes Modell: {}", modelToLoad);

            startupMessage.set("Starte AI-Server...");

            // Server starten
            LlamaServerProcessManager.StartResult result = llamaServerManager.startServer(
                modelToLoad,
                2026,      // Standard-Port
                8192,      // Context Size
                99         // GPU Layers (alle)
            );

            if (result.isSuccess()) {
                if (result.isStillLoading()) {
                    startupMessage.set("Modell wird geladen...");
                    // Warten bis Server wirklich bereit ist
                    waitForServerReady();
                } else {
                    log.info("✅ llama-server erfolgreich gestartet auf Port {}", result.getPort());
                    startupMessage.set("AI bereit!");
                }
            } else {
                // Binary nicht gefunden = Fallback auf java-llama-cpp (kein Fehler für User)
                if (result.getMessage() != null && result.getMessage().contains("Binary nicht gefunden")) {
                    log.info("🦙 llama-server Binary nicht verfügbar - nutze java-llama-cpp (JNI) als Fallback");
                    startupMessage.set("JNI-basierte Inferenz bereit");
                    // Kein Error setzen - ist ein normaler Fallback
                } else {
                    log.error("❌ llama-server Start fehlgeschlagen: {}", result.getMessage());
                    startupError.set(result.getMessage());
                    startupMessage.set("Startfehler");
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("llama-server Auto-Start unterbrochen");
            startupError.set("Start unterbrochen");
        } catch (Exception e) {
            log.error("Fehler beim Auto-Start des llama-servers: {}", e.getMessage(), e);
            startupError.set(e.getMessage());
        } finally {
            startupComplete.set(true);
            startupInProgress.set(false);
        }
    }

    /**
     * Wählt das zu ladende Modell aus.
     * Priorität: 1. Gespeichertes Modell, 2. Erstes verfügbares
     */
    private String selectModel(List<String> availableModels) {
        // Versuche gespeichertes Modell zu laden
        String savedModel = settingsService.getSelectedModel();
        if (savedModel != null && !savedModel.isBlank()) {
            // Prüfe ob es ein GGUF-Modell ist
            for (String model : availableModels) {
                if (model.toLowerCase().contains(savedModel.toLowerCase().replace(":latest", "").replace(":", "-"))) {
                    return model;
                }
            }
        }

        // Fallback: Erstes verfügbares Modell
        return availableModels.get(0);
    }

    /**
     * Wartet bis der Server wirklich bereit ist (max 60 Sekunden)
     */
    private void waitForServerReady() {
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(1000);

                LlamaServerProcessManager.ServerStatus status = llamaServerManager.getStatus();
                if (status.isOnline()) {
                    log.info("✅ llama-server ist bereit!");
                    startupMessage.set("AI bereit!");
                    return;
                }

                // Progress-Nachricht aktualisieren
                startupMessage.set("Modell wird geladen... (" + (i + 1) + "s)");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        log.warn("⚠️ llama-server Timeout - Server antwortet nicht nach 60s");
        startupMessage.set("Server gestartet (langsam)");
    }

    // ===== Getter für Frontend-Abfrage =====

    public boolean isStartupInProgress() {
        return startupInProgress.get();
    }

    public boolean isStartupComplete() {
        return startupComplete.get();
    }

    public String getStartupMessage() {
        return startupMessage.get();
    }

    public String getStartupError() {
        return startupError.get();
    }
}
