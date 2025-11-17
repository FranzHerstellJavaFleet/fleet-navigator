package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for generated downloads
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDownload {
    private String downloadId;
    private String filename;
    private String downloadUrl;
    private long sizeBytes;
    private String sizeHumanReadable;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
