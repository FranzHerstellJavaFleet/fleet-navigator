package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for file attachment metadata.
 * Used to track which files were uploaded with a message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    private String name;      // Original filename
    private String type;      // File type: "pdf", "image", "text", "json", "xml", "csv", "html"
    private Long size;        // File size in bytes
}
