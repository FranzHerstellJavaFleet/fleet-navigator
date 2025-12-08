package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a trusted Fleet Mate.
 * Stores the Mate's public key and shared secret for secure communication.
 * Each Mate belongs to a specific User for isolation.
 */
@Entity
@Table(name = "trusted_mates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"mate_id", "user_id"})  // mateId unique per user
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustedMate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner of this Mate - for user isolation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    /**
     * Unique identifier for the mate (e.g., "linux-mate-1", "office-mate-writer")
     * Note: unique per user, not globally
     */
    @Column(name = "mate_id", nullable = false)
    private String mateId;

    /**
     * Human-readable name for the mate
     */
    @Column(nullable = false)
    private String name;

    /**
     * Description of the mate's function
     */
    @Column(length = 500)
    private String description;

    /**
     * Type of mate (os, mail, office, browser)
     */
    @Column(nullable = false)
    private String mateType;

    /**
     * Mate's Ed25519 public key (Base64 encoded)
     */
    @Column(nullable = false, length = 100)
    private String publicKey;

    /**
     * Mate's X25519 exchange public key (Base64 encoded)
     */
    @Column(length = 100)
    private String exchangePublicKey;

    /**
     * Derived shared secret for AES encryption (Base64 encoded)
     * Computed once during pairing
     */
    @Column(length = 100)
    private String sharedSecret;

    /**
     * Mate's preferred AI model
     */
    @Column
    private String preferredModel;

    /**
     * Host address of the mate (for remote connections)
     */
    @Column
    private String hostAddress;

    /**
     * Port of the mate service
     */
    @Column
    private Integer hostPort;

    /**
     * When the mate was first paired
     */
    @Column(nullable = false)
    private LocalDateTime pairedAt;

    /**
     * Last successful authentication
     */
    @Column
    private LocalDateTime lastAuthAt;

    /**
     * Last heartbeat received
     */
    @Column
    private LocalDateTime lastSeenAt;

    /**
     * Whether the mate is currently enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Capabilities of the mate (JSON array, e.g., ["file_search", "log_analysis"])
     */
    @Column(length = 1000)
    private String capabilities;

    @PrePersist
    protected void onCreate() {
        if (pairedAt == null) {
            pairedAt = LocalDateTime.now();
        }
    }
}
