package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.model.LetterTemplate;
import io.javafleet.fleetnavigator.repository.LetterTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing letter templates/prompts
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LetterTemplateService {

    private final LetterTemplateRepository letterTemplateRepository;

    /**
     * Get all letter templates
     */
    @Transactional(readOnly = true)
    public List<LetterTemplate> getAllTemplates() {
        return letterTemplateRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get template by ID
     */
    @Transactional(readOnly = true)
    public Optional<LetterTemplate> getTemplateById(Long id) {
        return letterTemplateRepository.findById(id);
    }

    /**
     * Get templates by category
     */
    @Transactional(readOnly = true)
    public List<LetterTemplate> getTemplatesByCategory(String category) {
        return letterTemplateRepository.findByCategoryOrderByNameAsc(category);
    }

    /**
     * Search templates by name
     */
    @Transactional(readOnly = true)
    public List<LetterTemplate> searchTemplates(String searchTerm) {
        return letterTemplateRepository.findByNameContainingIgnoreCaseOrderByNameAsc(searchTerm);
    }

    /**
     * Create a new template
     */
    @Transactional
    public LetterTemplate createTemplate(LetterTemplate template) {
        log.info("Creating new letter template: {}", template.getName());
        return letterTemplateRepository.save(template);
    }

    /**
     * Update an existing template
     */
    @Transactional
    public LetterTemplate updateTemplate(Long id, LetterTemplate updatedTemplate) {
        return letterTemplateRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedTemplate.getName());
                    existing.setPrompt(updatedTemplate.getPrompt());
                    existing.setDescription(updatedTemplate.getDescription());
                    existing.setCategory(updatedTemplate.getCategory());
                    log.info("Updated letter template: {}", existing.getName());
                    return letterTemplateRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
    }

    /**
     * Delete a template
     */
    @Transactional
    public void deleteTemplate(Long id) {
        letterTemplateRepository.findById(id)
                .ifPresentOrElse(
                        template -> {
                            letterTemplateRepository.delete(template);
                            log.info("Deleted letter template: {}", template.getName());
                        },
                        () -> {
                            throw new RuntimeException("Template not found with id: " + id);
                        }
                );
    }

    /**
     * Get all unique categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return letterTemplateRepository.findAll().stream()
                .map(LetterTemplate::getCategory)
                .filter(category -> category != null && !category.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
    }
}
