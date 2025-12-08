package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for HuggingFace model metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HuggingFaceModelInfo {
    private String id;                      // e.g. "Qwen/Qwen2.5-3B-Instruct-GGUF"
    private String modelId;                 // Same as id
    private String author;                  // e.g. "Qwen"
    private String name;                    // e.g. "Qwen2.5-3B-Instruct-GGUF"
    private String displayName;             // User-friendly name
    private String description;             // Full README/model card description
    private String shortDescription;        // Short description

    private LocalDateTime createdAt;        // When model was created
    private LocalDateTime lastModified;     // Last update
    private LocalDateTime trainedDate;      // When model was trained (if available)

    private List<String> tags;              // e.g. ["gguf", "text-generation", "german"]
    private List<String> languages;         // Supported languages
    private String pipeline_tag;            // e.g. "text-generation"
    private String library_name;            // e.g. "transformers", "gguf"

    private Long downloads;                 // Number of downloads
    private Long likes;                     // Number of likes

    private List<String> siblings;          // Available files (e.g., different quantizations)
    private Long modelSize;                 // Size in bytes (if available)
    private String license;                 // License type

    private Boolean private_model;          // Is private
    private Boolean gated;                  // Requires approval

    // Additional metadata
    private String cardData;                // Full model card in markdown
    private String readme;                  // README content
}
