package io.javafleet.fleetnavigator.experts.repository;

import io.javafleet.fleetnavigator.experts.model.Expert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für Expert Entity
 */
@Repository
public interface ExpertRepository extends JpaRepository<Expert, Long> {

    /**
     * Finde Experten nach Name
     */
    Optional<Expert> findByName(String name);

    /**
     * Prüfe ob Name existiert
     */
    boolean existsByName(String name);

    /**
     * Finde alle aktiven Experten
     */
    List<Expert> findByActiveTrue();

    /**
     * Finde alle Experten nach Rolle
     */
    List<Expert> findByRoleContainingIgnoreCase(String role);

    /**
     * Finde alle Experten sortiert nach Name
     */
    @Query("SELECT e FROM Expert e ORDER BY e.name ASC")
    List<Expert> findAllOrderByName();

    /**
     * Finde alle aktiven Experten sortiert nach Name
     */
    @Query("SELECT e FROM Expert e WHERE e.active = true ORDER BY e.name ASC")
    List<Expert> findAllActiveOrderByName();
}
