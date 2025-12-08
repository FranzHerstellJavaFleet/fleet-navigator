package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.config.ModelSelectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Smart model selection service that automatically chooses the best model
 * based on the prompt content and task type.
 * Inspired by local-llm-demo-full project.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSelectionService {

    private final ModelSelectionProperties properties;
    private final SettingsService settingsService;

    // Code-related keywords
    private static final List<String> CODE_KEYWORDS = Arrays.asList(
        "code", "function", "class", "method", "variable", "bug", "error",
        "implement", "refactor", "debug", "algorithm", "programming",
        "java", "javascript", "python", "typescript", "vue", "react",
        "spring", "boot", "api", "database", "sql", "git", "maven",
        "npm", "package", "import", "export", "const", "let", "var"
    );

    // Pattern for code blocks
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
        "```|`[^`]+`|\\{|\\}|\\(|\\)|;|=>|::|->",
        Pattern.MULTILINE
    );

    // Pattern for technical terms
    private static final Pattern TECHNICAL_PATTERN = Pattern.compile(
        "\\b(HTTP|REST|API|JSON|XML|HTML|CSS|SQL|NoSQL)\\b",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Select the best model based on prompt content.
     * Results are cached to improve performance.
     *
     * @param prompt The user's prompt
     * @param defaultModel The default model if no specific routing applies
     * @return The recommended model name
     */
    @Cacheable(value = "modelSelection", key = "#prompt.hashCode()")
    public String selectModel(String prompt, String defaultModel) {
        // Load settings from database (cached by Spring)
        var settings = settingsService.getModelSelectionSettings();

        // Check if smart model selection is enabled
        if (!settings.isEnabled()) {
            log.debug("Smart model selection is disabled, using default model: {}", defaultModel);
            return defaultModel;
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            return defaultModel;
        }

        String lowerPrompt = prompt.toLowerCase();

        // Check if it's a code-related task
        if (isCodeRelated(lowerPrompt, prompt)) {
            log.info("Code-related prompt detected, routing to code model: {}", settings.getCodeModel());
            return settings.getCodeModel();
        }

        // Check if it's a simple Q&A
        if (isSimpleQuestion(lowerPrompt)) {
            log.info("Simple question detected, routing to fast model: {}", settings.getFastModel());
            return settings.getFastModel();
        }

        // Default to the user-specified or system default model
        log.info("Using default model: {}", defaultModel);
        return defaultModel;
    }

    /**
     * Check if the prompt is code-related.
     */
    private boolean isCodeRelated(String lowerPrompt, String originalPrompt) {
        // Check for code keywords
        long keywordCount = CODE_KEYWORDS.stream()
            .filter(lowerPrompt::contains)
            .count();

        if (keywordCount >= 2) {
            return true;
        }

        // Check for code blocks or technical patterns
        if (CODE_BLOCK_PATTERN.matcher(originalPrompt).find()) {
            return true;
        }

        // Check for technical terms
        if (TECHNICAL_PATTERN.matcher(originalPrompt).find()) {
            return true;
        }

        return false;
    }

    /**
     * Check if the prompt is a simple question.
     */
    private boolean isSimpleQuestion(String lowerPrompt) {
        // Short prompts are likely simple questions
        if (lowerPrompt.length() < 100) {
            return lowerPrompt.contains("was ist") ||
                   lowerPrompt.contains("was bedeutet") ||
                   lowerPrompt.contains("erkläre") ||
                   lowerPrompt.contains("what is") ||
                   lowerPrompt.contains("what does") ||
                   lowerPrompt.contains("explain") ||
                   lowerPrompt.contains("?");
        }
        return false;
    }


    /**
     * Get task type for logging/metrics.
     */
    public TaskType getTaskType(String prompt) {
        String lowerPrompt = prompt.toLowerCase();

        if (isCodeRelated(lowerPrompt, prompt)) {
            return TaskType.CODE;
        } else if (isSimpleQuestion(lowerPrompt)) {
            return TaskType.SIMPLE_QA;
        } else {
            return TaskType.COMPLEX;
        }
    }

    public enum TaskType {
        CODE,       // Code generation, debugging, technical tasks
        SIMPLE_QA,  // Simple questions, definitions
        COMPLEX     // Complex tasks, analysis, reasoning
    }
}
