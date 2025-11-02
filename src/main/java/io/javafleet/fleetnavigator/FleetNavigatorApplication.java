package io.javafleet.fleetnavigator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
public class FleetNavigatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetNavigatorApplication.class, args);
    }
}
