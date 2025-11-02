package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.ModelMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ModelMetadata
 */
@Repository
public interface ModelMetadataRepository extends JpaRepository<ModelMetadata, Long> {

    /**
     * Find metadata by model name
     */
    Optional<ModelMetadata> findByName(String name);

    /**
     * Find the default model
     */
    Optional<ModelMetadata> findByIsDefaultTrue();

    /**
     * Check if a model exists by name
     */
    boolean existsByName(String name);

    /**
     * Delete metadata by model name
     */
    void deleteByName(String name);

    /**
     * Set all models to non-default
     */
    @Modifying
    @Query("UPDATE ModelMetadata m SET m.isDefault = false WHERE m.isDefault = true")
    void clearAllDefaults();
}
