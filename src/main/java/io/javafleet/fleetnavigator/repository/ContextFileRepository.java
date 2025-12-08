package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.ContextFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContextFileRepository extends JpaRepository<ContextFile, Long> {

    /**
     * Find all context files for a specific project
     */
    List<ContextFile> findByProjectIdOrderByUploadedAtDesc(Long projectId);

    /**
     * Get total size of all context files for a project
     */
    @Query("SELECT SUM(cf.size) FROM ContextFile cf WHERE cf.project.id = :projectId")
    Long sumSizeByProjectId(Long projectId);

    /**
     * Count files in a project
     */
    Long countByProjectId(Long projectId);
}
