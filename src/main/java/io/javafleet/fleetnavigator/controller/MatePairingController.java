package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.model.TrustedMate;
import io.javafleet.fleetnavigator.security.CryptoService;
import io.javafleet.fleetnavigator.security.MatePairingService;
import io.javafleet.fleetnavigator.security.MatePairingService.PairingRequest;
import io.javafleet.fleetnavigator.websocket.FleetMateWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Mate pairing and trusted mate management.
 * Provides endpoints for:
 * - Viewing pending pairing requests
 * - Approving/rejecting pairing
 * - Managing trusted mates
 */
@Slf4j
@RestController
@RequestMapping("/api/pairing")
@RequiredArgsConstructor
public class MatePairingController {

    private final MatePairingService pairingService;
    private final CryptoService cryptoService;
    private final FleetMateWebSocketHandler webSocketHandler;

    /**
     * Get Navigator's public key (for Mates to initiate pairing)
     */
    @GetMapping("/navigator-key")
    public ResponseEntity<Map<String, String>> getNavigatorPublicKey() {
        return ResponseEntity.ok(Map.of(
            "publicKey", cryptoService.getPublicKeyBase64(),
            "exchangeKey", cryptoService.getExchangePublicKeyBase64()
        ));
    }

    /**
     * Get all pending pairing requests
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PairingRequest>> getPendingRequests() {
        return ResponseEntity.ok(pairingService.getPendingRequests());
    }

    /**
     * Approve a pairing request
     */
    @PostMapping("/approve/{requestId}")
    public ResponseEntity<?> approvePairing(@PathVariable String requestId) {
        try {
            TrustedMate mate = pairingService.approvePairing(requestId);
            log.info("Pairing approved for mate: {}", mate.getMateId());

            // Send pairing_approved via WebSocket to the waiting mate
            // Also registers the mate in FleetMateService for dashboard display
            boolean sent = webSocketHandler.sendPairingApproved(
                    requestId,
                    mate.getMateId(),
                    mate.getName(),
                    cryptoService.getExchangePublicKeyBase64()
            );

            if (!sent) {
                log.warn("Could not send pairing_approved to mate {} - session may have closed", mate.getMateId());
            }

            return ResponseEntity.ok(Map.of(
                "message", "Pairing genehmigt",
                "mateId", mate.getMateId(),
                "mateName", mate.getName(),
                "mateType", mate.getMateType(),
                "websocketNotified", sent
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to approve pairing", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject a pairing request
     */
    @PostMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectPairing(@PathVariable String requestId) {
        // Send pairing_rejected via WebSocket first (before removing from pending)
        webSocketHandler.sendPairingRejected(requestId);

        pairingService.rejectPairing(requestId);
        return ResponseEntity.ok(Map.of("message", "Pairing abgelehnt"));
    }

    /**
     * Get all trusted mates
     */
    @GetMapping("/trusted")
    public ResponseEntity<List<TrustedMate>> getTrustedMates() {
        return ResponseEntity.ok(pairingService.getTrustedMates());
    }

    /**
     * Remove a trusted mate
     */
    @DeleteMapping("/trusted/{mateId}")
    public ResponseEntity<?> removeTrustedMate(@PathVariable String mateId) {
        try {
            pairingService.removeTrustedMate(mateId);
            // Also disconnect the WebSocket session
            webSocketHandler.disconnectMate(mateId);
            log.info("Mate removed and disconnected: {}", mateId);
            return ResponseEntity.ok(Map.of("message", "Mate entfernt: " + mateId));
        } catch (Exception e) {
            log.error("Failed to remove mate", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove ALL trusted mates (like Bluetooth "forget all devices")
     */
    @DeleteMapping("/trusted")
    public ResponseEntity<?> removeAllTrustedMates() {
        try {
            // Get all trusted mates before removing
            List<TrustedMate> mates = pairingService.getTrustedMates();

            // Remove from database
            pairingService.removeAllTrustedMates();

            // Disconnect all their WebSocket sessions
            for (TrustedMate mate : mates) {
                webSocketHandler.disconnectMate(mate.getMateId());
            }

            log.info("All trusted mates have been removed and disconnected");
            return ResponseEntity.ok(Map.of("message", "Alle Mates wurden vergessen"));
        } catch (Exception e) {
            log.error("Failed to remove all mates", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get summary statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        List<TrustedMate> mates = pairingService.getTrustedMates();
        List<PairingRequest> pending = pairingService.getPendingRequests();

        return ResponseEntity.ok(Map.of(
            "trustedMates", mates.size(),
            "pendingRequests", pending.size(),
            "matesByType", Map.of(
                "os", mates.stream().filter(m -> "os".equals(m.getMateType())).count(),
                "mail", mates.stream().filter(m -> "mail".equals(m.getMateType())).count(),
                "office", mates.stream().filter(m -> "office".equals(m.getMateType())).count(),
                "browser", mates.stream().filter(m -> "browser".equals(m.getMateType())).count()
            )
        ));
    }
}
