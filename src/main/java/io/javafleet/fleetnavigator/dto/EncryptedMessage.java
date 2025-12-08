package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for encrypted WebSocket messages between Navigator and Mates.
 *
 * Protocol:
 * 1. After successful authentication, both sides can send encrypted messages
 * 2. The outer wrapper identifies the message as encrypted
 * 3. The payload is AES-256-GCM encrypted JSON
 * 4. Only authenticated mates can send/receive encrypted messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedMessage {

    /**
     * Always "encrypted" for encrypted messages
     */
    private String type;

    /**
     * The mate ID (sender or recipient)
     */
    private String mateId;

    /**
     * Base64 encoded encrypted payload (contains the actual message JSON)
     */
    private String payload;

    /**
     * Optional: Sequence number for replay protection
     */
    private Long sequence;

    /**
     * Create an encrypted message wrapper
     */
    public static EncryptedMessage wrap(String mateId, String encryptedPayload) {
        return EncryptedMessage.builder()
                .type("encrypted")
                .mateId(mateId)
                .payload(encryptedPayload)
                .build();
    }

    /**
     * Check if this is an encrypted message
     */
    public boolean isEncrypted() {
        return "encrypted".equals(type);
    }
}
