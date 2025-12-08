package io.javafleet.fleetnavigator.experts.repository;

import io.javafleet.fleetnavigator.experts.model.ExpertMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für ExpertMode Entity
 */
@Repository
public interface ExpertModeRepository extends JpaRepository<ExpertMode, Long> {

    /**
     * Finde alle Modi eines Experten
     */
    List<ExpertMode> findByExpert_Id(Long expertId);

    /**
     * Finde alle aktiven Modi eines Experten, sortiert nach Priorität
     */
    @Query("SELECT m FROM ExpertMode m WHERE m.expert.id = :expertId AND m.active = true ORDER BY m.priority DESC, m.name ASC")
    List<ExpertMode> findActiveByExpertIdOrderByPriority(@Param("expertId") Long expertId);

    /**
     * Finde Modus nach Name und Expert
     */
    Optional<ExpertMode> findByExpert_IdAndName(Long expertId, String name);

    /**
     * Prüfe ob Modus-Name für diesen Experten existiert
     */
    boolean existsByExpert_IdAndName(Long expertId, String name);

    /**
     * Lösche alle Modi eines Experten
     */
    void deleteByExpert_Id(Long expertId);
}
