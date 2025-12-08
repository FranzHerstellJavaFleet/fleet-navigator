package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.model.LetterTemplate;
import io.javafleet.fleetnavigator.service.LetterTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Letter Templates
 */
@RestController
@RequestMapping("/api/letter-templates")
@RequiredArgsConstructor
@Slf4j
public class LetterTemplateController {

    private final LetterTemplateService letterTemplateService;

    /**
     * Get all letter templates
     * GET /api/letter-templates
     */
    @GetMapping
    public ResponseEntity<List<LetterTemplate>> getAllTemplates() {
        List<LetterTemplate> templates = letterTemplateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Get template by ID
     * GET /api/letter-templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LetterTemplate> getTemplateById(@PathVariable Long id) {
        return letterTemplateService.getTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get templates by category
     * GET /api/letter-templates/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<LetterTemplate>> getTemplatesByCategory(@PathVariable String category) {
        List<LetterTemplate> templates = letterTemplateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(templates);
    }

    /**
     * Search templates
     * GET /api/letter-templates/search?q=searchTerm
     */
    @GetMapping("/search")
    public ResponseEntity<List<LetterTemplate>> searchTemplates(@RequestParam String q) {
        List<LetterTemplate> templates = letterTemplateService.searchTemplates(q);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get all categories
     * GET /api/letter-templates/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = letterTemplateService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Create a new template
     * POST /api/letter-templates
     */
    @PostMapping
    public ResponseEntity<LetterTemplate> createTemplate(@RequestBody LetterTemplate template) {
        try {
            LetterTemplate created = letterTemplateService.createTemplate(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating letter template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing template
     * PUT /api/letter-templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<LetterTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody LetterTemplate template) {
        try {
            LetterTemplate updated = letterTemplateService.updateTemplate(id, template);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating letter template", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a template
     * DELETE /api/letter-templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        try {
            letterTemplateService.deleteTemplate(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting letter template", e);
            return ResponseEntity.notFound().build();
        }
    }
}
