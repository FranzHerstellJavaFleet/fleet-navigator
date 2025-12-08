package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.GlobalStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for GlobalStats entity
 */
@Repository
public interface GlobalStatsRepository extends JpaRepository<GlobalStats, Long> {

    /**
     * Get the single global stats instance (there should only be one row)
     */
    Optional<GlobalStats> findFirstByOrderByIdAsc();
}
