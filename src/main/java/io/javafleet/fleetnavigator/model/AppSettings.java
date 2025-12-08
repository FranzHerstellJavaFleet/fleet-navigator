package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Application settings stored in database.
 * Allows runtime configuration of smart model selection.
 */
@Entity
@Table(name = "app_settings")
@Data
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Settings keys constants
    public static final String KEY_MODEL_SELECTION_ENABLED = "model.selection.enabled";
    public static final String KEY_CODE_MODEL = "model.selection.code";
    public static final String KEY_FAST_MODEL = "model.selection.fast";
    public static final String KEY_VISION_MODEL = "model.selection.vision";
    public static final String KEY_EMAIL_MODEL = "model.selection.email"; // Email classification model
    public static final String KEY_LOG_ANALYSIS_MODEL = "model.selection.log_analysis"; // Log analysis model
    public static final String KEY_DOCUMENT_MODEL = "model.selection.document"; // Document/Brief generation model
    public static final String KEY_DEFAULT_MODEL = "model.default";
    public static final String KEY_SELECTED_MODEL = "model.selected"; // User's last selected model
    public static final String KEY_SELECTED_EXPERT = "expert.selected"; // User's last selected expert ID
    public static final String KEY_SHOW_WELCOME_TILES = "ui.showWelcomeTiles"; // Show welcome tiles on start
    public static final String KEY_SHOW_TOP_BAR = "ui.showTopBar"; // Show top navigation bar
    public static final String KEY_UI_THEME = "ui.theme"; // UI Theme (tech-dark, tech-light, lawyer-dark, lawyer-light)
    public static final String KEY_VISION_CHAINING_ENABLED = "vision.chaining.enabled";
    public static final String KEY_VISION_CHAINING_SMART_SELECTION = "vision.chaining.smart.selection";
    public static final String KEY_ACTIVE_PROVIDER = "llm.provider.active"; // Active LLM Provider (ollama, llamacpp, openai)
    public static final String KEY_AUTO_SWITCH_VISION_ON_IMAGE = "model.auto_switch.vision_on_image"; // Auto-switch to vision model on image upload

    // Web Search Settings
    public static final String KEY_WEBSEARCH_BRAVE_API_KEY = "websearch.brave.apikey";
    public static final String KEY_WEBSEARCH_CUSTOM_INSTANCE = "websearch.searxng.custom";
    public static final String KEY_WEBSEARCH_INSTANCES = "websearch.searxng.instances";  // JSON array
    public static final String KEY_WEBSEARCH_COUNT = "websearch.count";  // Monthly Brave search counter
    public static final String KEY_WEBSEARCH_MONTH = "websearch.month";  // Current month for counter reset
    public static final String KEY_WEBSEARCH_SEARXNG_COUNT = "websearch.searxng.count";  // Total SearXNG searches
    public static final String KEY_WEBSEARCH_SEARXNG_MONTH_COUNT = "websearch.searxng.month.count";  // Monthly SearXNG searches

    // Web Search Feature Flags
    public static final String KEY_WEBSEARCH_QUERY_OPTIMIZATION = "websearch.feature.queryOptimization";
    public static final String KEY_WEBSEARCH_CONTENT_SCRAPING = "websearch.feature.contentScraping";
    public static final String KEY_WEBSEARCH_MULTI_QUERY = "websearch.feature.multiQuery";
    public static final String KEY_WEBSEARCH_RERANKING = "websearch.feature.reRanking";
    public static final String KEY_WEBSEARCH_OPTIMIZATION_MODEL = "websearch.feature.optimizationModel";

    // File Search (OS Mate RAG) Settings
    public static final String KEY_FILESEARCH_FOLDERS = "filesearch.folders"; // JSON array of folder configs
    public static final String KEY_FILESEARCH_ENABLED = "filesearch.enabled";
}
