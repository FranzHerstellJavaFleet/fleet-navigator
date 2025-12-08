package io.javafleet.fleetnavigator.security;

import io.javafleet.fleetnavigator.model.TrustedMate;
import io.javafleet.fleetnavigator.repository.TrustedMateRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing Fleet Mate pairing and authentication.
 * Implements a Bluetooth-like pairing flow:
 * 1. Mate sends pairing request with its public key
 * 2. Navigator shows 6-digit pairing code
 * 3. User confirms in Web UI
 * 4. Navigator and Mate exchange keys and establish encrypted channel
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatePairingService {

    private final CryptoService cryptoService;
    private final TrustedMateRepository trustedMateRepository;

    // Pending pairing requests (requestId -> PairingRequest)
    private final Map<String, PairingRequest> pendingRequests = new ConcurrentHashMap<>();

    // Active sessions with authenticated mates (mateId -> shared secret)
    private final Map<String, byte[]> activeSessionSecrets = new ConcurrentHashMap<>();

    // Session nonces for authentication (mateId -> nonce)
    private final Map<String, String> sessionNonces = new ConcurrentHashMap<>();

    /**
     * Create a new pairing request
     * Returns the request ID and pairing code
     */
    public PairingResponse createPairingRequest(String mateName, String mateType,
                                                 String matePublicKey, String mateExchangeKey,
                                                 String ipv4, String ipv6) {
        // Check if already paired
        if (trustedMateRepository.existsByPublicKey(matePublicKey)) {
            TrustedMate existing = trustedMateRepository.findByPublicKey(matePublicKey).orElse(null);
            if (existing != null) {
                log.info("Mate {} already paired, returning existing pairing", existing.getMateId());
                return PairingResponse.builder()
                        .requestId(null)
                        .mateId(existing.getMateId())
                        .pairingCode(null)
                        .navigatorPublicKey(cryptoService.getPublicKeyBase64())
                        .navigatorExchangeKey(cryptoService.getExchangePublicKeyBase64())
                        .status(PairingStatus.ALREADY_PAIRED)
                        .build();
            }
        }

        String requestId = UUID.randomUUID().toString();
        String pairingCode = cryptoService.generatePairingCode(matePublicKey);

        PairingRequest request = new PairingRequest();
        request.setRequestId(requestId);
        request.setMateName(mateName);
        request.setMateType(mateType);
        request.setMatePublicKey(matePublicKey);
        request.setMateExchangeKey(mateExchangeKey);
        request.setPairingCode(pairingCode);
        request.setFingerprint(cryptoService.generateFingerprint(matePublicKey));
        request.setIpv4(ipv4);
        request.setIpv6(ipv6);
        request.setCreatedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24h timeout

        pendingRequests.put(requestId, request);

        log.info("Created pairing request {} for mate '{}' (type: {}), code: {}",
                requestId, mateName, mateType, pairingCode);

        return PairingResponse.builder()
                .requestId(requestId)
                .pairingCode(pairingCode)
                .navigatorPublicKey(cryptoService.getPublicKeyBase64())
                .navigatorExchangeKey(cryptoService.getExchangePublicKeyBase64())
                .status(PairingStatus.PENDING)
                .build();
    }

    /**
     * Get all pending pairing requests
     */
    public List<PairingRequest> getPendingRequests() {
        // Clean expired requests
        LocalDateTime now = LocalDateTime.now();
        pendingRequests.entrySet().removeIf(e -> e.getValue().getExpiresAt().isBefore(now));

        return new ArrayList<>(pendingRequests.values());
    }

    /**
     * Approve a pairing request (called from Web UI)
     */
    @Transactional
    public TrustedMate approvePairing(String requestId) {
        PairingRequest request = pendingRequests.remove(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Pairing request not found or expired: " + requestId);
        }

        if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Pairing request expired: " + requestId);
        }

        // Generate unique mateId
        String mateId = generateMateId(request.getMateName(), request.getMateType());

        // Derive shared secret
        byte[] sharedSecret;
        try {
            sharedSecret = cryptoService.deriveSharedSecret(request.getMateExchangeKey());
        } catch (Exception e) {
            log.error("Failed to derive shared secret", e);
            throw new RuntimeException("Key exchange failed", e);
        }

        // Create trusted mate
        TrustedMate mate = TrustedMate.builder()
                .mateId(mateId)
                .name(request.getMateName())
                .mateType(request.getMateType())
                .publicKey(request.getMatePublicKey())
                .exchangePublicKey(request.getMateExchangeKey())
                .sharedSecret(Base64.getEncoder().encodeToString(sharedSecret))
                .pairedAt(LocalDateTime.now())
                .enabled(true)
                .build();

        mate = trustedMateRepository.save(mate);

        log.info("Approved pairing for mate: {} ({})", mateId, request.getMateName());

        return mate;
    }

    /**
     * Reject a pairing request
     */
    public void rejectPairing(String requestId) {
        PairingRequest request = pendingRequests.remove(requestId);
        if (request != null) {
            log.info("Rejected pairing request for mate: {}", request.getMateName());
        }
    }

    /**
     * Authenticate a mate using challenge-response
     * Returns a session token if successful
     */
    public AuthenticationResult authenticate(String mateId, String publicKey, String signature, String nonce) {
        // Find trusted mate
        Optional<TrustedMate> mateOpt = trustedMateRepository.findByMateId(mateId);
        if (mateOpt.isEmpty()) {
            log.warn("Authentication failed: unknown mateId {}", mateId);
            return AuthenticationResult.failed("Unknown mate");
        }

        TrustedMate mate = mateOpt.get();

        // Verify public key matches
        if (!mate.getPublicKey().equals(publicKey)) {
            log.warn("Authentication failed: public key mismatch for {}", mateId);
            return AuthenticationResult.failed("Public key mismatch");
        }

        // Check if mate is enabled
        if (!mate.getEnabled()) {
            log.warn("Authentication failed: mate {} is disabled", mateId);
            return AuthenticationResult.failed("Mate is disabled");
        }

        // Verify the nonce was issued by us
        String expectedNonce = sessionNonces.get(mateId);
        if (expectedNonce == null || !expectedNonce.equals(nonce)) {
            log.warn("Authentication failed: invalid nonce for {}", mateId);
            return AuthenticationResult.failed("Invalid nonce");
        }

        // Verify signature
        String message = mateId + ":" + nonce;
        if (!cryptoService.verify(message, signature, publicKey)) {
            log.warn("Authentication failed: invalid signature for {}", mateId);
            return AuthenticationResult.failed("Invalid signature");
        }

        // Clear used nonce
        sessionNonces.remove(mateId);

        // Store session secret
        byte[] sharedSecret = Base64.getDecoder().decode(mate.getSharedSecret());
        activeSessionSecrets.put(mateId, sharedSecret);

        // Update last auth time
        mate.setLastAuthAt(LocalDateTime.now());
        trustedMateRepository.save(mate);

        log.info("Mate {} authenticated successfully", mateId);

        return AuthenticationResult.success(mateId, mate.getName());
    }

    /**
     * Generate a challenge nonce for authentication
     */
    public String generateAuthChallenge(String mateId) {
        String nonce = cryptoService.generateNonce();
        sessionNonces.put(mateId, nonce);
        return nonce;
    }

    /**
     * Check if a mate is authenticated in the current session
     */
    public boolean isAuthenticated(String mateId) {
        return activeSessionSecrets.containsKey(mateId);
    }

    /**
     * Get the shared secret for an authenticated mate
     */
    public byte[] getSessionSecret(String mateId) {
        return activeSessionSecrets.get(mateId);
    }

    /**
     * End a mate's session
     */
    public void endSession(String mateId) {
        activeSessionSecrets.remove(mateId);
        sessionNonces.remove(mateId);
    }

    /**
     * Get all trusted mates
     */
    public List<TrustedMate> getTrustedMates() {
        return trustedMateRepository.findAllOrderByLastSeen();
    }

    /**
     * Remove a trusted mate
     */
    @Transactional
    public void removeTrustedMate(String mateId) {
        endSession(mateId);
        trustedMateRepository.deleteByMateId(mateId);
        log.info("Removed trusted mate: {}", mateId);
    }

    /**
     * Remove ALL trusted mates (like Bluetooth "forget all devices")
     */
    @Transactional
    public void removeAllTrustedMates() {
        // End all active sessions
        activeSessionSecrets.clear();
        sessionNonces.clear();
        pendingRequests.clear();

        // Delete all from database
        long count = trustedMateRepository.count();
        trustedMateRepository.deleteAll();
        log.info("Removed all {} trusted mates", count);
    }

    /**
     * Update mate's last seen timestamp
     */
    @Transactional
    public void updateLastSeen(String mateId) {
        trustedMateRepository.findByMateId(mateId).ifPresent(mate -> {
            mate.setLastSeenAt(LocalDateTime.now());
            trustedMateRepository.save(mate);
        });
    }

    /**
     * Generate a unique mateId
     */
    private String generateMateId(String name, String type) {
        String base = type + "-" + name.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String mateId = base;
        int counter = 1;

        while (trustedMateRepository.existsByMateId(mateId)) {
            mateId = base + "-" + counter++;
        }

        return mateId;
    }

    // ========== DTOs ==========

    @Data
    public static class PairingRequest {
        private String requestId;
        private String mateName;
        private String mateType;
        private String matePublicKey;
        private String mateExchangeKey;
        private String pairingCode;
        private String fingerprint;  // Short hash of public key for verification
        private String ipv4;         // IPv4 address of the mate
        private String ipv6;         // IPv6 address of the mate
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
    }

    @Data
    @lombok.Builder
    public static class PairingResponse {
        private String requestId;
        private String mateId;
        private String pairingCode;
        private String navigatorPublicKey;
        private String navigatorExchangeKey;
        private PairingStatus status;
    }

    public enum PairingStatus {
        PENDING,
        APPROVED,
        REJECTED,
        ALREADY_PAIRED,
        EXPIRED
    }

    @Data
    @lombok.Builder
    public static class AuthenticationResult {
        private boolean success;
        private String mateId;
        private String mateName;
        private String error;

        public static AuthenticationResult success(String mateId, String mateName) {
            return AuthenticationResult.builder()
                    .success(true)
                    .mateId(mateId)
                    .mateName(mateName)
                    .build();
        }

        public static AuthenticationResult failed(String error) {
            return AuthenticationResult.builder()
                    .success(false)
                    .error(error)
                    .build();
        }
    }
}
