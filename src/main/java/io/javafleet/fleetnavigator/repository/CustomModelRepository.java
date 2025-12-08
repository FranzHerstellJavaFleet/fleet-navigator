package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.CustomModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CustomModel entity
 */
@Repository
public interface CustomModelRepository extends JpaRepository<CustomModel, Long> {

    /**
     * Find custom model by name
     */
    Optional<CustomModel> findByName(String name);

    /**
     * Check if custom model exists by name
     */
    boolean existsByName(String name);

    /**
     * Delete custom model by name
     */
    void deleteByName(String name);

    /**
     * Find all custom models derived from a specific base model
     */
    List<CustomModel> findByBaseModel(String baseModel);

    /**
     * Find all versions of a custom model (by parent chain)
     */
    List<CustomModel> findByParentModelId(Long parentModelId);

    /**
     * Find latest version of a custom model family
     */
    @Query("SELECT cm FROM CustomModel cm WHERE cm.parentModelId = :parentId ORDER BY cm.version DESC")
    List<CustomModel> findLatestVersionsByParent(Long parentId);

    /**
     * Get all custom models ordered by creation date (newest first)
     */
    @Query("SELECT cm FROM CustomModel cm ORDER BY cm.createdAt DESC")
    List<CustomModel> findAllOrderByCreatedAtDesc();
}
