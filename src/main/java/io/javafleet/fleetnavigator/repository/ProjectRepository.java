package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all projects ordered by updated date (most recent first)
     */
    List<Project> findAllByOrderByUpdatedAtDesc();

    /**
     * Find projects by name containing (case-insensitive search)
     */
    List<Project> findByNameContainingIgnoreCase(String name);
}
