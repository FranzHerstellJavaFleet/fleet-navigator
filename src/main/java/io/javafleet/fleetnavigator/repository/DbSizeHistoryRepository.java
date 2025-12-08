package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.DbSizeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DbSizeHistory entity
 * Tracks database size measurements over time
 */
@Repository
public interface DbSizeHistoryRepository extends JpaRepository<DbSizeHistory, Long> {

    /**
     * Get the most recent measurement
     */
    Optional<DbSizeHistory> findFirstByOrderByRecordedAtDesc();

    /**
     * Get all measurements ordered by time (newest first)
     */
    List<DbSizeHistory> findAllByOrderByRecordedAtDesc();

    /**
     * Get recent measurements (for display)
     */
    List<DbSizeHistory> findTop100ByOrderByRecordedAtDesc();

    /**
     * Get measurements since a specific date
     */
    List<DbSizeHistory> findByRecordedAtAfterOrderByRecordedAtAsc(LocalDateTime since);

    /**
     * Get measurements within a date range
     */
    List<DbSizeHistory> findByRecordedAtBetweenOrderByRecordedAtAsc(LocalDateTime start, LocalDateTime end);
}
