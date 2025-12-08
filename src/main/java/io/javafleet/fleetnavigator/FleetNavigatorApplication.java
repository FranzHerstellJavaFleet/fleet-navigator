package io.javafleet.fleetnavigator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Fleet Navigator - Navigate your AI fleet
 *
 * A modern web interface for Ollama LLM models with context management,
 * streaming support, and system monitoring.
 *
 * @author JavaFleet Systems Consulting
 * @version 0.1.0-SNAPSHOT
 */
@SpringBootApplication
@EnableScheduling
public class FleetNavigatorApplication {

    private static final int DEFAULT_PORT = 2025;

    public static void main(String[] args) {
        // Check if port is already in use
        if (isPortInUse(DEFAULT_PORT)) {
            System.err.println("\n╔════════════════════════════════════════════════════════════╗");
            System.err.println("║  ⚠️  FEHLER: Fleet Navigator läuft bereits!              ║");
            System.err.println("║                                                            ║");
            System.err.println("║  Port 2025 ist bereits belegt.                            ║");
            System.err.println("║                                                            ║");
            System.err.println("║  Öffne: http://localhost:2025                             ║");
            System.err.println("║                                                            ║");
            System.err.println("║  Zum Beenden der laufenden Instanz:                       ║");
            System.err.println("║  → pkill -f fleet-navigator                                ║");
            System.err.println("╚════════════════════════════════════════════════════════════╝\n");
            System.exit(1);
        }

        SpringApplication.run(FleetNavigatorApplication.class, args);
    }

    /**
     * Checks if a port is already in use
     */
    private static boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            // Port is available
            return false;
        } catch (IOException e) {
            // Port is in use
            return true;
        }
    }
}
