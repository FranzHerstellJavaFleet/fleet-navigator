package io.javafleet.fleetnavigator.config;

import io.javafleet.fleetnavigator.service.MateDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Listener der beim Navigator-Start Fleet Mates im Netzwerk aufweckt
 */
@Component
public class StartupDiscoveryListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupDiscoveryListener.class);
    private final MateDiscoveryService discoveryService;

    public StartupDiscoveryListener(MateDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("🚢 Fleet Navigator is ready - Broadcasting discovery signal to Mates...");

        // Sende 3 Broadcasts im Abstand von 500ms für bessere Zuverlässigkeit
        new Thread(() -> {
            try {
                // Kurze Verzögerung damit WebSocket-Server bereit ist
                Thread.sleep(1000);
                discoveryService.broadcastDiscoveryMultiple(3, 500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Discovery broadcast interrupted during startup");
            }
        }, "DiscoveryBroadcast").start();
    }
}
