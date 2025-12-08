package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.LetterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for LetterTemplate entity
 */
@Repository
public interface LetterTemplateRepository extends JpaRepository<LetterTemplate, Long> {

    /**
     * Find all templates ordered by name
     */
    List<LetterTemplate> findAllByOrderByNameAsc();

    /**
     * Find templates by category
     */
    List<LetterTemplate> findByCategoryOrderByNameAsc(String category);

    /**
     * Search templates by name (case-insensitive)
     */
    List<LetterTemplate> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
