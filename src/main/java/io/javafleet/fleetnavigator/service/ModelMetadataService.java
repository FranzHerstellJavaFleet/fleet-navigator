package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.model.ModelMetadata;
import io.javafleet.fleetnavigator.repository.ModelMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing model metadata
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelMetadataService {

    private final ModelMetadataRepository metadataRepository;

    /**
     * Get all model metadata
     */
    @Transactional(readOnly = true)
    public List<ModelMetadata> getAllMetadata() {
        return metadataRepository.findAll();
    }

    /**
     * Get metadata for a specific model
     */
    @Transactional(readOnly = true)
    public Optional<ModelMetadata> getMetadataByName(String name) {
        return metadataRepository.findByName(name);
    }

    /**
     * Get the default model
     * If no default is set, automatically sets phi:latest as default
     */
    @Transactional
    public Optional<ModelMetadata> getDefaultModel() {
        Optional<ModelMetadata> defaultModel = metadataRepository.findByIsDefaultTrue();

        // If no default model is set, automatically set phi:latest as default
        if (defaultModel.isEmpty()) {
            log.info("No default model found in DB, setting phi:latest as default");
            setDefaultModel("phi:latest");
            return metadataRepository.findByIsDefaultTrue();
        }

        return defaultModel;
    }

    /**
     * Create or update model metadata
     */
    @Transactional
    public ModelMetadata saveMetadata(ModelMetadata metadata) {
        // If setting as default, clear all other defaults
        if (metadata.getIsDefault() != null && metadata.getIsDefault()) {
            metadataRepository.clearAllDefaults();
        }

        // Check if metadata already exists
        Optional<ModelMetadata> existing = metadataRepository.findByName(metadata.getName());
        if (existing.isPresent()) {
            // Update existing
            ModelMetadata existingMetadata = existing.get();
            existingMetadata.setSize(metadata.getSize());
            existingMetadata.setDescription(metadata.getDescription());
            existingMetadata.setSpecialties(metadata.getSpecialties());
            existingMetadata.setPublisher(metadata.getPublisher());
            existingMetadata.setReleaseDate(metadata.getReleaseDate());
            existingMetadata.setLicense(metadata.getLicense());
            existingMetadata.setIsDefault(metadata.getIsDefault());
            existingMetadata.setNotes(metadata.getNotes());
            log.info("Updated metadata for model: {}", metadata.getName());
            return metadataRepository.save(existingMetadata);
        } else {
            // Create new
            log.info("Created metadata for model: {}", metadata.getName());
            return metadataRepository.save(metadata);
        }
    }

    /**
     * Set a model as default
     */
    @Transactional
    public void setDefaultModel(String modelName) {
        // Clear all defaults
        metadataRepository.clearAllDefaults();

        // Set new default
        Optional<ModelMetadata> metadata = metadataRepository.findByName(modelName);
        if (metadata.isPresent()) {
            ModelMetadata meta = metadata.get();
            meta.setIsDefault(true);
            metadataRepository.save(meta);
            log.info("Set default model to: {}", modelName);
        } else {
            // Create minimal metadata entry
            ModelMetadata newMeta = new ModelMetadata();
            newMeta.setName(modelName);
            newMeta.setIsDefault(true);
            newMeta.setCreatedAt(LocalDateTime.now());
            newMeta.setUpdatedAt(LocalDateTime.now());
            metadataRepository.save(newMeta);
            log.info("Created and set default model to: {}", modelName);
        }
    }

    /**
     * Delete model metadata
     */
    @Transactional
    public void deleteMetadata(String name) {
        metadataRepository.deleteByName(name);
        log.info("Deleted metadata for model: {}", name);
    }

    /**
     * Check if model has metadata
     */
    @Transactional(readOnly = true)
    public boolean hasMetadata(String name) {
        return metadataRepository.existsByName(name);
    }

    /**
     * Sync metadata with actual installed models
     * This will create minimal metadata entries for models that don't have metadata yet
     */
    @Transactional
    public void syncWithInstalledModels(List<String> installedModelNames) {
        for (String modelName : installedModelNames) {
            if (!metadataRepository.existsByName(modelName)) {
                ModelMetadata metadata = new ModelMetadata();
                metadata.setName(modelName);
                metadata.setIsDefault(false);
                metadata.setCreatedAt(LocalDateTime.now());
                metadata.setUpdatedAt(LocalDateTime.now());
                metadataRepository.save(metadata);
                log.info("Auto-created minimal metadata for model: {}", modelName);
            }
        }
    }
}
