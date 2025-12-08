package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.CreateCustomModelRequest;
import io.javafleet.fleetnavigator.dto.UpdateCustomModelRequest;
import io.javafleet.fleetnavigator.model.CustomModel;
import io.javafleet.fleetnavigator.repository.CustomModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing custom Ollama models
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomModelService {

    private final CustomModelRepository customModelRepository;
    private final OllamaService ollamaService;

    /**
     * Get all custom models ordered by creation date
     */
    @Transactional(readOnly = true)
    public List<CustomModel> getAllCustomModels() {
        return customModelRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * Get custom model by ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomModel> getCustomModelById(Long id) {
        return customModelRepository.findById(id);
    }

    /**
     * Get custom model by name
     */
    @Transactional(readOnly = true)
    public Optional<CustomModel> getCustomModelByName(String name) {
        return customModelRepository.findByName(name);
    }

    /**
     * Create a new custom model
     * Generates Modelfile and creates model in Ollama
     */
    @Transactional
    public CustomModel createCustomModel(CreateCustomModelRequest request) {
        // Check if name already exists
        if (customModelRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Model with name '" + request.getName() + "' already exists");
        }

        // Generate Modelfile
        String modelfile = generateModelfile(
                request.getBaseModel(),
                request.getSystemPrompt(),
                request.getTemperature(),
                request.getTopP(),
                request.getTopK(),
                request.getRepeatPenalty()
        );

        // Create entity
        CustomModel customModel = new CustomModel();
        customModel.setName(request.getName());
        customModel.setBaseModel(request.getBaseModel());
        customModel.setSystemPrompt(request.getSystemPrompt());
        customModel.setDescription(request.getDescription());
        customModel.setTemperature(request.getTemperature());
        customModel.setTopP(request.getTopP());
        customModel.setTopK(request.getTopK());
        customModel.setRepeatPenalty(request.getRepeatPenalty());
        customModel.setNumPredict(request.getNumPredict());
        customModel.setNumCtx(request.getNumCtx());
        customModel.setModelfile(modelfile);
        customModel.setVersion(1);

        // Save to database
        CustomModel saved = customModelRepository.save(customModel);
        log.info("Created custom model in database: {}", saved.getName());

        return saved;
    }

    /**
     * Update a custom model (creates new version)
     */
    @Transactional
    public CustomModel updateCustomModel(Long id, UpdateCustomModelRequest request) {
        CustomModel original = customModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Custom model not found: " + id));

        // Generate new name if not provided
        String newName = request.getName() != null && !request.getName().isEmpty()
                ? request.getName()
                : generateVersionedName(original.getName(), original.getVersion() + 1);

        // Check if new name already exists
        if (customModelRepository.existsByName(newName) && !newName.equals(original.getName())) {
            throw new IllegalArgumentException("Model with name '" + newName + "' already exists");
        }

        // Generate new Modelfile with updated parameters
        String modelfile = generateModelfile(
                original.getBaseModel(), // Keep same base model
                request.getSystemPrompt() != null ? request.getSystemPrompt() : original.getSystemPrompt(),
                request.getTemperature() != null ? request.getTemperature() : original.getTemperature(),
                request.getTopP() != null ? request.getTopP() : original.getTopP(),
                request.getTopK() != null ? request.getTopK() : original.getTopK(),
                request.getRepeatPenalty() != null ? request.getRepeatPenalty() : original.getRepeatPenalty()
        );

        // Create new version
        CustomModel newVersion = new CustomModel();
        newVersion.setName(newName);
        newVersion.setBaseModel(original.getBaseModel());
        newVersion.setSystemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt() : original.getSystemPrompt());
        newVersion.setDescription(request.getDescription() != null ? request.getDescription() : original.getDescription());
        newVersion.setTemperature(request.getTemperature() != null ? request.getTemperature() : original.getTemperature());
        newVersion.setTopP(request.getTopP() != null ? request.getTopP() : original.getTopP());
        newVersion.setTopK(request.getTopK() != null ? request.getTopK() : original.getTopK());
        newVersion.setRepeatPenalty(request.getRepeatPenalty() != null ? request.getRepeatPenalty() : original.getRepeatPenalty());
        newVersion.setNumPredict(request.getNumPredict() != null ? request.getNumPredict() : original.getNumPredict());
        newVersion.setNumCtx(request.getNumCtx() != null ? request.getNumCtx() : original.getNumCtx());
        newVersion.setModelfile(modelfile);
        newVersion.setParentModelId(original.getId());
        newVersion.setVersion(original.getVersion() + 1);

        CustomModel saved = customModelRepository.save(newVersion);
        log.info("Created new version of custom model: {} (v{})", saved.getName(), saved.getVersion());

        return saved;
    }

    /**
     * Delete custom model by ID
     */
    @Transactional
    public void deleteCustomModel(Long id) {
        customModelRepository.deleteById(id);
        log.info("Deleted custom model with ID: {}", id);
    }

    /**
     * Delete custom model by name
     */
    @Transactional
    public void deleteCustomModelByName(String name) {
        customModelRepository.deleteByName(name);
        log.info("Deleted custom model: {}", name);
    }

    /**
     * Get ancestry chain for a custom model
     * Returns list from root to current model
     */
    @Transactional(readOnly = true)
    public List<CustomModel> getAncestry(Long id) {
        List<CustomModel> ancestry = new ArrayList<>();
        CustomModel current = customModelRepository.findById(id).orElse(null);

        while (current != null) {
            ancestry.add(0, current); // Add at beginning to maintain order
            if (current.getParentModelId() != null) {
                current = customModelRepository.findById(current.getParentModelId()).orElse(null);
            } else {
                break;
            }
        }

        return ancestry;
    }

    /**
     * Generate Modelfile content
     */
    public String generateModelfile(String baseModel, String systemPrompt,
                                      Double temperature, Double topP,
                                      Integer topK, Double repeatPenalty) {
        // Validate baseModel is not empty
        if (baseModel == null || baseModel.trim().isEmpty()) {
            throw new IllegalArgumentException("Base model is required and cannot be empty");
        }

        StringBuilder modelfile = new StringBuilder();

        // FROM directive (required)
        modelfile.append("FROM ").append(baseModel.trim()).append("\n\n");

        // SYSTEM directive (optional)
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            modelfile.append("SYSTEM \"\"\"")
                    .append(systemPrompt.trim())
                    .append("\"\"\"\n\n");
        }

        // PARAMETER directives (optional)
        if (temperature != null) {
            modelfile.append("PARAMETER temperature ").append(temperature).append("\n");
        }

        if (topP != null) {
            modelfile.append("PARAMETER top_p ").append(topP).append("\n");
        }

        if (topK != null) {
            modelfile.append("PARAMETER top_k ").append(topK).append("\n");
        }

        if (repeatPenalty != null) {
            modelfile.append("PARAMETER repeat_penalty ").append(repeatPenalty).append("\n");
        }

        return modelfile.toString();
    }

    /**
     * Generate versioned name (e.g. "nova:latest" -> "nova:v2")
     */
    private String generateVersionedName(String originalName, int newVersion) {
        // Remove existing version tag if present
        String baseName = originalName;
        if (originalName.contains(":")) {
            baseName = originalName.substring(0, originalName.lastIndexOf(":"));
        }

        return baseName + ":v" + newVersion;
    }
}
