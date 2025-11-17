package io.javafleet.fleetnavigator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Service für Fleet Mate Discovery via UDP Broadcast
 */
@Service
public class MateDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(MateDiscoveryService.class);
    private static final int DISCOVERY_PORT = 9090;
    private static final String DISCOVERY_MESSAGE = "FLEET_NAVIGATOR_READY";

    /**
     * Sendet UDP Broadcast an alle Mates im Netzwerk
     * Mates im Listener Mode werden dadurch aufgeweckt und versuchen Reconnect
     */
    public void broadcastDiscovery() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] buffer = DISCOVERY_MESSAGE.getBytes();

            // 1. Sende an localhost (für lokale Mates)
            try {
                InetAddress localhostAddr = InetAddress.getByName("127.0.0.1");
                DatagramPacket localhostPacket = new DatagramPacket(buffer, buffer.length, localhostAddr, DISCOVERY_PORT);
                socket.send(localhostPacket);
                log.info("✅ UDP Discovery sent to localhost: '{}' to {}:{}",
                         DISCOVERY_MESSAGE, localhostAddr.getHostAddress(), DISCOVERY_PORT);
            } catch (Exception e) {
                log.warn("Failed to send to localhost: {}", e.getMessage());
            }

            // 2. Sende Broadcast an alle im lokalen Netzwerk
            try {
                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                DatagramPacket broadcastPacket = new DatagramPacket(buffer, buffer.length, broadcastAddr, DISCOVERY_PORT);
                socket.send(broadcastPacket);
                log.info("✅ UDP Discovery broadcast sent: '{}' to {}:{}",
                         DISCOVERY_MESSAGE, broadcastAddr.getHostAddress(), DISCOVERY_PORT);
            } catch (Exception e) {
                log.warn("Failed to send broadcast: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Failed to send UDP discovery: {}", e.getMessage(), e);
        }
    }

    /**
     * Sendet mehrere Discovery Broadcasts in kurzen Abständen
     * für bessere Zuverlässigkeit
     */
    public void broadcastDiscoveryMultiple(int count, long delayMs) {
        log.info("🔍 Broadcasting {} discovery messages with {}ms delay...", count, delayMs);

        for (int i = 1; i <= count; i++) {
            log.info("Broadcast {}/{}", i, count);
            broadcastDiscovery();

            if (i < count) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Discovery broadcast interrupted");
                    break;
                }
            }
        }

        log.info("✅ Discovery broadcast sequence completed");
    }
}
