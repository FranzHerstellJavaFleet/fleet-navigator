package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DbSizeHistory Entity - Tracks database size over time
 * Records are created every 30 minutes for monitoring growth
 */
@Entity
@Table(name = "db_size_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbSizeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    /**
     * Convenience constructor
     */
    public DbSizeHistory(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
        this.recordedAt = LocalDateTime.now();
    }
}
