package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.GgufModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GgufModelConfigRepository extends JpaRepository<GgufModelConfig, Long> {

    /**
     * Find configuration by model name
     */
    Optional<GgufModelConfig> findByName(String name);

    /**
     * Check if a model name already exists
     */
    boolean existsByName(String name);

    /**
     * Find the default model configuration
     */
    Optional<GgufModelConfig> findByIsDefaultTrue();
}
