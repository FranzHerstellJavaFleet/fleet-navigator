package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for generating code files and projects from AI responses
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGeneratorService {

    private final ObjectMapper objectMapper;
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/fleet-navigator-generated/";

    /**
     * Generates files from structured AI response
     *
     * @param aiResponse The AI response containing code/project structure
     * @return Path to the generated project directory
     */
    public Path generateProject(String aiResponse) throws IOException {
        log.info("Starting project generation from AI response");

        // Create unique project directory
        String projectId = UUID.randomUUID().toString();
        Path projectPath = Paths.get(TEMP_DIR, projectId);
        Files.createDirectories(projectPath);

        try {
            // Try to parse as JSON structure
            if (aiResponse.trim().startsWith("{")) {
                JsonNode root = objectMapper.readTree(aiResponse);

                // Check if it's a structured project
                if (root.has("project")) {
                    JsonNode project = root.get("project");
                    String projectName = project.has("name") ? project.get("name").asText() : "generated-project";

                    // Create project directory
                    Path projectDir = projectPath.resolve(projectName);
                    Files.createDirectories(projectDir);

                    // Generate files from structure
                    if (project.has("files")) {
                        generateFilesFromJson(project.get("files"), projectDir);
                    }

                    log.info("Generated structured project: {} at {}", projectName, projectDir);
                    return projectDir;
                }
            }

            // Fallback: Extract code blocks from markdown
            return generateFromMarkdown(aiResponse, projectPath);

        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON, falling back to markdown extraction", e);
            return generateFromMarkdown(aiResponse, projectPath);
        }
    }

    /**
     * Generate files from JSON structure
     */
    private void generateFilesFromJson(JsonNode filesNode, Path basePath) throws IOException {
        if (filesNode.isArray()) {
            for (JsonNode fileNode : filesNode) {
                String filePath = fileNode.get("path").asText();
                String content = fileNode.get("content").asText();

                createFile(basePath, filePath, content);
            }
        }
    }

    /**
     * Extract code blocks from markdown and create files
     */
    private Path generateFromMarkdown(String markdown, Path basePath) throws IOException {
        log.info("Extracting code blocks from markdown response");

        // Simple markdown code block extraction
        String[] lines = markdown.split("\n");
        StringBuilder currentContent = new StringBuilder();
        String currentFilename = null;
        String currentLanguage = null;
        String previousLine = null;
        boolean inCodeBlock = false;
        int fileCounter = 1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.trim().startsWith("```")) {
                if (!inCodeBlock) {
                    // Start of code block
                    inCodeBlock = true;
                    String header = line.substring(3).trim();

                    // Extract language
                    if (!header.isEmpty()) {
                        String[] parts = header.split("\\s+");
                        currentLanguage = parts[0];

                        // Check if filename is specified in header
                        if (parts.length > 1) {
                            currentFilename = parts[1];
                        } else {
                            // Try to extract filename from previous line
                            currentFilename = extractFilenameFromContext(previousLine, i - 1, lines);

                            if (currentFilename == null) {
                                // Fallback: generate filename based on language
                                currentFilename = "file" + fileCounter + getExtension(currentLanguage);
                                fileCounter++;
                            }
                        }
                    } else {
                        currentFilename = "file" + fileCounter + ".txt";
                        fileCounter++;
                    }
                    currentContent = new StringBuilder();
                } else {
                    // End of code block - save file
                    inCodeBlock = false;
                    if (currentFilename != null) {
                        createFile(basePath, currentFilename, currentContent.toString());
                        log.info("Created file: {}", currentFilename);
                    }
                    currentFilename = null;
                    currentLanguage = null;
                }
            } else if (inCodeBlock) {
                currentContent.append(line).append("\n");
            }

            previousLine = line;
        }

        // Handle case where markdown doesn't close properly
        if (inCodeBlock && currentFilename != null) {
            createFile(basePath, currentFilename, currentContent.toString());
        }

        // If no files were created, create a single README.txt with the full response
        File[] files = basePath.toFile().listFiles();
        if (files == null || files.length == 0) {
            createFile(basePath, "response.txt", markdown);
            log.info("No code blocks found, created response.txt");
        }

        return basePath;
    }

    /**
     * Extract filename from context (previous lines)
     * Looks for patterns like:
     * - "pom.xml" (on previous line)
     * - "src/main/java/App.java" (path on previous line)
     * - "### HelloController.java" (in heading)
     */
    private String extractFilenameFromContext(String previousLine, int lineIndex, String[] allLines) {
        if (previousLine == null || previousLine.trim().isEmpty()) {
            return null;
        }

        String trimmed = previousLine.trim();

        // Check if previous line looks like a filename
        // Pattern 1: Just a filename like "pom.xml" or "Application.java"
        if (trimmed.matches("[a-zA-Z0-9_\\-\\.]+\\.[a-zA-Z]{2,10}")) {
            return trimmed;
        }

        // Pattern 2: Path like "src/main/java/App.java"
        if (trimmed.contains("/") && trimmed.matches(".*[a-zA-Z0-9_\\-]+\\.[a-zA-Z]{2,10}$")) {
            return trimmed;
        }

        // Pattern 3: Heading like "### HelloController.java" or "## pom.xml"
        if (trimmed.startsWith("#")) {
            String headingText = trimmed.replaceFirst("^#+\\s*", "").trim();
            if (headingText.matches("[a-zA-Z0-9_/\\-\\.]+\\.[a-zA-Z]{2,10}")) {
                return headingText;
            }
        }

        // Pattern 4: Look 2-3 lines back for filename
        for (int i = lineIndex - 1; i >= Math.max(0, lineIndex - 3); i--) {
            String backLine = allLines[i].trim();
            if (backLine.matches("[a-zA-Z0-9_/\\-\\.]+\\.[a-zA-Z]{2,10}")) {
                return backLine;
            }
        }

        return null;
    }

    /**
     * Create a file with given content
     */
    private void createFile(Path basePath, String relativePath, String content) throws IOException {
        Path filePath = basePath.resolve(relativePath);

        // Create parent directories if needed
        Files.createDirectories(filePath.getParent());

        // Write content
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content);
        }

        log.debug("Created file: {}", filePath);
    }

    /**
     * Get file extension for language
     */
    private String getExtension(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> ".java";
            case "xml" -> ".xml";
            case "json" -> ".json";
            case "yaml", "yml" -> ".yml";
            case "properties" -> ".properties";
            case "javascript", "js" -> ".js";
            case "typescript", "ts" -> ".ts";
            case "python", "py" -> ".py";
            case "html" -> ".html";
            case "css" -> ".css";
            case "sql" -> ".sql";
            case "sh", "bash" -> ".sh";
            case "dockerfile" -> "Dockerfile";
            case "markdown", "md" -> ".md";
            default -> ".txt";
        };
    }

    /**
     * Clean up old generated projects (older than 1 hour)
     */
    public void cleanupOldProjects() {
        try {
            File tempDir = new File(TEMP_DIR);
            if (!tempDir.exists()) return;

            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);

            File[] projects = tempDir.listFiles();
            if (projects == null) return;

            for (File project : projects) {
                if (project.lastModified() < oneHourAgo) {
                    deleteDirectory(project);
                    log.info("Cleaned up old project: {}", project.getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to cleanup old projects", e);
        }
    }

    /**
     * Delete directory recursively
     */
    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    Files.delete(file.toPath());
                }
            }
        }
        Files.delete(directory.toPath());
    }

    /**
     * Get temporary directory path
     */
    public String getTempDirectory() {
        return TEMP_DIR;
    }
}
