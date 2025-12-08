package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI-powered log analysis
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogAnalysisRequest {
    private String mateId;
    private String logPath;
    private String mode;           // "smart", "full", "errors-only", "last-n"
    private Integer lines;         // Only for "last-n" mode
    private String model;          // Ollama model (e.g., "llama3.2:3b")
    private String prompt;         // Custom analysis prompt (optional)
}
