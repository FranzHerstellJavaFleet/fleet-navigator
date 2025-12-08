package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * GlobalStats Entity - Tracks global usage statistics
 */
@Entity
@Table(name = "global_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens = 0L;

    @Column(name = "total_messages", nullable = false)
    private Integer totalMessages = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Increment statistics
     */
    public void incrementStats(int tokens) {
        this.totalTokens += tokens;
        this.totalMessages++;
    }
}
