package io.javafleet.fleetnavigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.javafleet.fleetnavigator.llm.dto.ModelInfo;
import io.javafleet.fleetnavigator.model.AppSettings;
import io.javafleet.fleetnavigator.repository.AppSettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Erweiterter Web Search Service für RAG (Retrieval Augmented Generation)
 *
 * Features:
 * 1. Query-Optimierung durch LLM
 * 2. Web-Scraping für vollständige Inhalte
 * 3. Zeitfilter für aktuelle Ergebnisse
 * 4. Domain-Filter (bevorzugen/ausschließen)
 * 5. Multi-Query für bessere Abdeckung
 * 6. Re-Ranking nach Relevanz
 * 7. Caching für häufige Suchen
 * 8. Automatische Sprach-Erkennung
 *
 * @author JavaFleet Systems Consulting
 */
@Service
@Slf4j
public class WebSearchService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppSettingsRepository settingsRepository;
    private final OllamaService ollamaService;
    private final LLMProviderService llmProviderService;
    private final ExecutorService searchExecutor;

    // ============ CACHE ============
    private final Cache<String, List<SearchResult>> searchCache;
    private final Cache<String, String> contentCache;

    // ============ BRAVE SEARCH API ============
    private static final String BRAVE_API_URL = "https://api.search.brave.com/res/v1/web/search";
    private static final int BRAVE_FREE_LIMIT = 2000;

    @Getter
    private String braveApiKey = "";

    @Getter
    private int searchCountThisMonth = 0;
    @Getter
    private YearMonth currentMonth = YearMonth.now();

    @Getter
    private int searxngTotalCount = 0;
    @Getter
    private int searxngMonthCount = 0;

    @Getter
    private String customSearxngInstance = "";

    // ============ FEATURE FLAGS ============
    @Getter @Setter
    private boolean queryOptimizationEnabled = true;
    @Getter @Setter
    private boolean contentScrapingEnabled = true;
    @Getter @Setter
    private boolean multiQueryEnabled = false;
    @Getter @Setter
    private boolean reRankingEnabled = true;
    @Getter @Setter
    private String queryOptimizationModel = "llama3.2:3b";

    /**
     * Das tatsächlich verwendete Modell (nach Fallback-Logik)
     */
    @Getter
    private String effectiveOptimizationModel = null;

    // ============ SEARXNG ============
    private static final List<String> DEFAULT_SEARXNG_INSTANCES = List.of(
        "https://search.sapti.me",
        "https://searx.tiekoetter.com",
        "https://priv.au",
        "https://search.ononoki.org",
        "https://search.bus-hit.me",
        "https://paulgo.io"
    );

    private List<String> searxngInstances = new ArrayList<>(DEFAULT_SEARXNG_INSTANCES);

    public WebSearchService(AppSettingsRepository settingsRepository, OllamaService ollamaService, LLMProviderService llmProviderService) {
        this.settingsRepository = settingsRepository;
        this.ollamaService = ollamaService;
        this.llmProviderService = llmProviderService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();

        // Cache: 100 Einträge, 15 Minuten TTL
        this.searchCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(15))
                .build();

        // Content Cache: 50 Seiten, 30 Minuten TTL
        this.contentCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();

        this.searchExecutor = Executors.newFixedThreadPool(3);
    }

    @PostConstruct
    @Transactional(readOnly = true)
    public void loadSettingsFromDatabase() {
        log.info("Lade Web-Suche Einstellungen aus Datenbank...");

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_BRAVE_API_KEY)
            .ifPresent(s -> {
                this.braveApiKey = s.getValue() != null ? s.getValue() : "";
                log.info("Brave API Key geladen: {}", braveApiKey.isEmpty() ? "(leer)" : "(konfiguriert)");
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_CUSTOM_INSTANCE)
            .ifPresent(s -> {
                this.customSearxngInstance = s.getValue() != null ? s.getValue() : "";
                if (!customSearxngInstance.isEmpty()) {
                    log.info("Eigene SearXNG Instanz geladen: {}", customSearxngInstance);
                }
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_INSTANCES)
            .ifPresent(s -> {
                if (s.getValue() != null && !s.getValue().isBlank()) {
                    try {
                        List<String> instances = Arrays.asList(objectMapper.readValue(s.getValue(), String[].class));
                        if (!instances.isEmpty()) {
                            this.searxngInstances = new ArrayList<>(instances);
                            log.info("SearXNG Instanzen geladen: {} Stück", instances.size());
                        }
                    } catch (Exception e) {
                        log.warn("Konnte SearXNG Instanzen nicht parsen: {}", e.getMessage());
                    }
                }
            });

        rebuildInstanceList();
        loadCounters();
        loadFeatureFlags();
        checkAndResetMonthlyCounter();
    }

    private void loadFeatureFlags() {
        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_QUERY_OPTIMIZATION)
            .ifPresent(s -> {
                this.queryOptimizationEnabled = "true".equalsIgnoreCase(s.getValue());
                log.info("Query-Optimierung: {}", queryOptimizationEnabled);
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_CONTENT_SCRAPING)
            .ifPresent(s -> {
                this.contentScrapingEnabled = "true".equalsIgnoreCase(s.getValue());
                log.info("Content-Scraping: {}", contentScrapingEnabled);
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_MULTI_QUERY)
            .ifPresent(s -> {
                this.multiQueryEnabled = "true".equalsIgnoreCase(s.getValue());
                log.info("Multi-Query: {}", multiQueryEnabled);
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_RERANKING)
            .ifPresent(s -> {
                this.reRankingEnabled = "true".equalsIgnoreCase(s.getValue());
                log.info("Re-Ranking: {}", reRankingEnabled);
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_OPTIMIZATION_MODEL)
            .ifPresent(s -> {
                if (s.getValue() != null && !s.getValue().isBlank()) {
                    this.queryOptimizationModel = s.getValue();
                    log.info("Optimierungs-Modell (konfiguriert): {}", queryOptimizationModel);
                }
            });

        // Effektives Modell bestimmen (mit Fallback)
        determineEffectiveModel();
    }

    /**
     * Bestimmt das tatsächlich zu verwendende Optimierungs-Modell
     * Falls das konfigurierte Modell nicht verfügbar ist, wird das kleinste verfügbare gewählt
     */
    private void determineEffectiveModel() {
        try {
            List<ModelInfo> allModels = llmProviderService.getAllModels();
            if (allModels.isEmpty()) {
                log.warn("⚠️ Keine Modelle verfügbar - Query-Optimierung deaktiviert");
                this.effectiveOptimizationModel = null;
                return;
            }

            // Prüfe ob das konfigurierte Modell verfügbar ist
            boolean configuredModelAvailable = allModels.stream()
                    .anyMatch(m -> m.getName().equalsIgnoreCase(queryOptimizationModel));

            if (configuredModelAvailable) {
                this.effectiveOptimizationModel = queryOptimizationModel;
                log.info("✅ Optimierungs-Modell verfügbar: {}", effectiveOptimizationModel);
                return;
            }

            // Fallback: Wähle das kleinste verfügbare Modell
            log.warn("⚠️ Konfiguriertes Modell '{}' nicht verfügbar - suche Alternative", queryOptimizationModel);
            String fallbackModel = findSmallestModel(allModels);

            if (fallbackModel != null) {
                this.effectiveOptimizationModel = fallbackModel;
                log.info("🔄 Fallback-Modell gewählt: {}", effectiveOptimizationModel);
            } else {
                log.warn("⚠️ Kein geeignetes Modell gefunden - Query-Optimierung deaktiviert");
                this.effectiveOptimizationModel = null;
            }

        } catch (Exception e) {
            log.warn("Fehler beim Bestimmen des Optimierungs-Modells: {}", e.getMessage());
            this.effectiveOptimizationModel = null;
        }
    }

    /**
     * Findet das kleinste verfügbare Modell (bevorzugt 1B-7B)
     */
    private String findSmallestModel(List<ModelInfo> models) {
        // Prioritäts-Reihenfolge für kleine, schnelle Modelle
        List<String> preferredPatterns = List.of(
            ".*1b.*", ".*2b.*", ".*3b.*",           // Sehr kleine Modelle
            ".*phi.*", ".*tinyllama.*", ".*smollm.*", // Bekannte kleine Modelle
            ".*7b.*", ".*8b.*"                       // Mittlere Modelle
        );

        for (String pattern : preferredPatterns) {
            for (ModelInfo model : models) {
                String name = model.getName().toLowerCase();
                if (name.matches(pattern) && !name.contains("vision") && !name.contains("embed")) {
                    return model.getName();
                }
            }
        }

        // Fallback: Wähle nach Größe (kleinste zuerst)
        return models.stream()
            .filter(m -> m.getSize() != null && m.getSize() > 0)
            .filter(m -> !m.getName().toLowerCase().contains("vision"))
            .filter(m -> !m.getName().toLowerCase().contains("embed"))
            .min(Comparator.comparing(ModelInfo::getSize))
            .map(ModelInfo::getName)
            .orElse(models.isEmpty() ? null : models.get(0).getName());
    }

    private void loadCounters() {
        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_MONTH)
            .ifPresent(s -> {
                if (s.getValue() != null) {
                    try {
                        this.currentMonth = YearMonth.parse(s.getValue());
                    } catch (Exception e) {
                        log.warn("Konnte Monat nicht parsen: {}", e.getMessage());
                    }
                }
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_COUNT)
            .ifPresent(s -> {
                if (s.getValue() != null) {
                    try {
                        this.searchCountThisMonth = Integer.parseInt(s.getValue());
                    } catch (Exception ignored) {}
                }
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_SEARXNG_COUNT)
            .ifPresent(s -> {
                if (s.getValue() != null) {
                    try {
                        this.searxngTotalCount = Integer.parseInt(s.getValue());
                    } catch (Exception ignored) {}
                }
            });

        settingsRepository.findByKey(AppSettings.KEY_WEBSEARCH_SEARXNG_MONTH_COUNT)
            .ifPresent(s -> {
                if (s.getValue() != null) {
                    try {
                        this.searxngMonthCount = Integer.parseInt(s.getValue());
                    } catch (Exception ignored) {}
                }
            });
    }

    private void rebuildInstanceList() {
        List<String> allInstances = new ArrayList<>();
        if (customSearxngInstance != null && !customSearxngInstance.isEmpty()) {
            allInstances.add(customSearxngInstance);
        }
        for (String instance : searxngInstances) {
            if (!instance.equals(customSearxngInstance) && !allInstances.contains(instance)) {
                allInstances.add(instance);
            }
        }
        if (allInstances.isEmpty()) {
            allInstances.addAll(DEFAULT_SEARXNG_INSTANCES);
        }
        this.searxngInstances = allInstances;
    }

    // ============ HAUPT-SUCHMETHODEN ============

    /**
     * Erweiterte Suche mit allen Features
     */
    public List<SearchResult> searchEnhanced(String userQuery, SearchOptions options) {
        log.info("🔍 Erweiterte Suche: '{}' mit Optionen: {}", userQuery, options);

        // 1. Sprache erkennen
        String language = detectLanguage(userQuery);
        log.debug("Erkannte Sprache: {}", language);

        // 2. Query optimieren (wenn aktiviert)
        String optimizedQuery = userQuery;
        if (queryOptimizationEnabled && options.isOptimizeQuery()) {
            optimizedQuery = optimizeQuery(userQuery, language);
            log.info("📝 Optimierte Query: '{}'", optimizedQuery);
        }

        // 3. Cache prüfen
        String cacheKey = buildCacheKey(optimizedQuery, options);
        List<SearchResult> cachedResults = searchCache.getIfPresent(cacheKey);
        if (cachedResults != null) {
            log.info("💾 Cache-Hit für: {}", cacheKey);
            return cachedResults;
        }

        // 4. Multi-Query (wenn aktiviert)
        List<SearchResult> results;
        if (multiQueryEnabled && options.isMultiQuery()) {
            results = executeMultiQuery(optimizedQuery, userQuery, options);
        } else {
            results = executeSingleQuery(optimizedQuery, options);
        }

        // 5. Re-Ranking (wenn aktiviert)
        if (reRankingEnabled && options.isReRank() && results.size() > 1) {
            results = reRankResults(results, userQuery);
            log.info("📊 Re-Ranking durchgeführt");
        }

        // 6. Web-Scraping für vollständige Inhalte (wenn aktiviert)
        if (contentScrapingEnabled && options.isFetchFullContent()) {
            results = enrichWithFullContent(results, options.getMaxContentLength());
            log.info("📄 Vollständige Inhalte abgerufen");
        }

        // 7. Cache speichern
        if (!results.isEmpty()) {
            searchCache.put(cacheKey, results);
        }

        return results;
    }

    /**
     * Einfache Suche (Rückwärtskompatibel)
     */
    public List<SearchResult> search(String query, int maxResults) {
        return search(query, null, maxResults);
    }

    public List<SearchResult> search(String query, List<String> domains, int maxResults) {
        SearchOptions options = new SearchOptions();
        options.setMaxResults(maxResults);
        options.setDomains(domains);
        options.setOptimizeQuery(queryOptimizationEnabled);
        options.setFetchFullContent(false); // Standard: nur Snippets
        options.setReRank(reRankingEnabled);

        return searchEnhanced(query, options);
    }

    // ============ QUERY-OPTIMIERUNG ============

    /**
     * Optimiert die Suchanfrage mit einem schnellen LLM
     * Verwendet das effektive Modell (mit automatischem Fallback)
     */
    public String optimizeQuery(String userQuery, String language) {
        return optimizeQuery(userQuery, language, null);
    }

    public String optimizeQuery(String userQuery, String language, String expertContext) {
        // Prüfe ob ein effektives Modell verfügbar ist
        if (effectiveOptimizationModel == null) {
            log.debug("Kein Optimierungs-Modell verfügbar - verwende Original-Query");
            // Wenn Expert-Kontext vorhanden, füge ihn zur Query hinzu
            if (expertContext != null && !expertContext.isBlank()) {
                return userQuery + " " + expertContext;
            }
            return userQuery;
        }

        try {
            String expertHint = "";
            if (expertContext != null && !expertContext.isBlank()) {
                expertHint = "\n6. WICHTIG: Der Benutzer spricht mit einem " + expertContext + ". " +
                             "Füge relevante Fachbegriffe aus diesem Bereich hinzu!";
            }

            String systemPrompt = """
                Du bist ein Suchquery-Optimierer. Deine Aufgabe ist es, die Benutzeranfrage in eine optimale Suchanfrage umzuwandeln.

                Regeln:
                1. Extrahiere die Kernbegriffe
                2. Entferne Füllwörter
                3. Füge relevante Synonyme hinzu (mit OR)
                4. Gib NUR die optimierte Suchanfrage zurück, NICHTS anderes
                5. Maximal 10 Wörter
                """ + expertHint + """

                Beispiele:
                - "Kannst du mir sagen wie das Wetter morgen in Berlin wird?" → "Wetter Berlin morgen Vorhersage"
                - "Was sind die besten Restaurants in München?" → "beste Restaurants München Empfehlungen Bewertungen"
                - "Wie programmiere ich eine REST API in Java?" → "Java REST API Tutorial Beispiel Spring Boot"
                - (Rechtsanwalt-Kontext) "Was kann mir im Extremfall passieren?" → "rechtliche Konsequenzen Strafe Höchststrafe Risiko"
                """;

            String prompt = "Optimiere diese Suchanfrage (Sprache: " + language + "): " + userQuery;

            log.debug("Query-Optimierung mit Modell: {}", effectiveOptimizationModel);
            String result = ollamaService.chat(effectiveOptimizationModel, prompt, systemPrompt, null);
            if (result != null && !result.isBlank() && result.length() < 200) {
                return result.trim().replaceAll("[\"']", "");
            }
        } catch (Exception e) {
            log.warn("Query-Optimierung fehlgeschlagen ({}): {}", effectiveOptimizationModel, e.getMessage());
        }
        return userQuery;
    }

    // ============ MULTI-QUERY ============

    private List<SearchResult> executeMultiQuery(String optimizedQuery, String originalQuery, SearchOptions options) {
        List<String> queries = generateQueryVariations(optimizedQuery, originalQuery);
        log.info("🔀 Multi-Query mit {} Varianten", queries.size());

        List<Future<List<SearchResult>>> futures = new ArrayList<>();
        for (String query : queries) {
            futures.add(searchExecutor.submit(() -> executeSingleQuery(query, options)));
        }

        Set<String> seenUrls = new HashSet<>();
        List<SearchResult> allResults = new ArrayList<>();

        for (Future<List<SearchResult>> future : futures) {
            try {
                List<SearchResult> results = future.get(15, TimeUnit.SECONDS);
                for (SearchResult result : results) {
                    if (!seenUrls.contains(result.url())) {
                        seenUrls.add(result.url());
                        allResults.add(result);
                    }
                }
            } catch (Exception e) {
                log.warn("Multi-Query Fehler: {}", e.getMessage());
            }
        }

        return allResults.stream()
                .limit(options.getMaxResults())
                .collect(Collectors.toList());
    }

    private List<String> generateQueryVariations(String optimizedQuery, String originalQuery) {
        List<String> variations = new ArrayList<>();
        variations.add(optimizedQuery);
        if (!optimizedQuery.equals(originalQuery)) {
            variations.add(originalQuery);
        }
        // Könnte erweitert werden mit Synonymen etc.
        return variations;
    }

    // ============ EINZELNE SUCHE ============

    private List<SearchResult> executeSingleQuery(String query, SearchOptions options) {
        String searchQuery = buildQuery(query, options.getDomains(), options.getExcludeDomains(), options.getTimeFilter());
        log.info("Web-Suche: '{}' (max {} Ergebnisse)", searchQuery, options.getMaxResults());

        checkAndResetMonthlyCounter();

        // Brave API
        if (braveApiKey != null && !braveApiKey.isBlank()) {
            try {
                List<SearchResult> results = searchWithBrave(searchQuery, options);
                if (!results.isEmpty()) {
                    incrementSearchCounter();
                    return results;
                }
            } catch (Exception e) {
                log.warn("Brave Search fehlgeschlagen: {}", e.getMessage());
            }
        }

        // SearXNG Fallback
        for (String instance : searxngInstances) {
            try {
                List<SearchResult> results = searchWithSearXNG(instance, searchQuery, options);
                if (!results.isEmpty()) {
                    incrementSearxngCounter();
                    // Domain-Filter anwenden (SearXNG ignoriert oft site: Operatoren)
                    results = filterByDomains(results, options.getDomains());
                    if (!results.isEmpty()) {
                        return results;
                    }
                    log.warn("SearXNG: Alle {} Ergebnisse durch Domain-Filter entfernt", results.size());
                }
            } catch (Exception e) {
                log.warn("SearXNG {} fehlgeschlagen: {}", instance, e.getMessage());
            }
        }

        return new ArrayList<>();
    }

    /**
     * Filtert Suchergebnisse nach erlaubten Domains
     * Wird verwendet wenn die Suchmaschine site: Operatoren ignoriert
     */
    private List<SearchResult> filterByDomains(List<SearchResult> results, List<String> allowedDomains) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return results; // Kein Filter wenn keine Domains angegeben
        }

        List<SearchResult> filtered = results.stream()
            .filter(r -> {
                String url = r.url().toLowerCase();
                return allowedDomains.stream()
                    .anyMatch(domain -> url.contains(domain.toLowerCase()));
            })
            .collect(Collectors.toList());

        log.info("Domain-Filter: {} von {} Ergebnissen behalten (erlaubt: {})",
                 filtered.size(), results.size(), allowedDomains);

        return filtered;
    }

    // ============ RE-RANKING ============

    private List<SearchResult> reRankResults(List<SearchResult> results, String userQuery) {
        // Einfaches Scoring basierend auf Keyword-Übereinstimmung
        String[] queryTerms = userQuery.toLowerCase().split("\\s+");

        return results.stream()
            .sorted((a, b) -> {
                int scoreA = calculateRelevanceScore(a, queryTerms);
                int scoreB = calculateRelevanceScore(b, queryTerms);
                return Integer.compare(scoreB, scoreA);
            })
            .collect(Collectors.toList());
    }

    private int calculateRelevanceScore(SearchResult result, String[] queryTerms) {
        int score = 0;
        String title = result.title().toLowerCase();
        String snippet = result.snippet() != null ? result.snippet().toLowerCase() : "";
        String url = result.url().toLowerCase();

        for (String term : queryTerms) {
            if (term.length() < 3) continue;
            if (title.contains(term)) score += 10;
            if (url.contains(term)) score += 5;
            if (snippet.contains(term)) score += 3;
        }

        // Bonus für bekannte Qualitätsquellen
        if (url.contains("wikipedia")) score += 15;
        if (url.contains("github")) score += 10;
        if (url.contains("stackoverflow")) score += 10;

        return score;
    }

    // ============ WEB-SCRAPING ============

    private List<SearchResult> enrichWithFullContent(List<SearchResult> results, int maxLength) {
        return results.stream()
            .map(result -> {
                String content = fetchPageContent(result.url(), maxLength);
                if (content != null && !content.isBlank()) {
                    return new SearchResult(result.title(), result.url(), content);
                }
                return result;
            })
            .collect(Collectors.toList());
    }

    /**
     * Ruft den Textinhalt einer Webseite ab
     */
    public String fetchPageContent(String url, int maxLength) {
        // Cache prüfen
        String cached = contentCache.getIfPresent(url);
        if (cached != null) {
            return cached.length() > maxLength ? cached.substring(0, maxLength) : cached;
        }

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // Entferne Scripts, Styles, Navigation etc.
            doc.select("script, style, nav, header, footer, aside, .ads, .advertisement").remove();

            // Extrahiere Hauptinhalt
            String text = doc.body().text();

            // Bereinige den Text
            text = Jsoup.clean(text, Safelist.none());
            text = text.replaceAll("\\s+", " ").trim();

            if (text.length() > maxLength) {
                text = text.substring(0, maxLength) + "...";
            }

            // Cache speichern
            contentCache.put(url, text);

            return text;
        } catch (Exception e) {
            log.debug("Konnte Seite nicht laden: {} - {}", url, e.getMessage());
            return null;
        }
    }

    // ============ SPRACH-ERKENNUNG ============

    private String detectLanguage(String text) {
        if (text == null || text.isBlank()) return "de";

        String lower = text.toLowerCase();

        // Einfache Heuristik basierend auf häufigen Wörtern
        int germanScore = 0;
        int englishScore = 0;

        String[] germanWords = {"der", "die", "das", "und", "ist", "ein", "eine", "für", "mit", "wie", "was", "kann", "nicht", "auch", "ich", "du", "wir"};
        String[] englishWords = {"the", "a", "an", "and", "is", "for", "with", "how", "what", "can", "not", "also", "i", "you", "we", "are", "have"};

        for (String word : germanWords) {
            if (lower.contains(" " + word + " ") || lower.startsWith(word + " ") || lower.endsWith(" " + word)) {
                germanScore++;
            }
        }

        for (String word : englishWords) {
            if (lower.contains(" " + word + " ") || lower.startsWith(word + " ") || lower.endsWith(" " + word)) {
                englishScore++;
            }
        }

        return germanScore >= englishScore ? "de" : "en";
    }

    // ============ BRAVE SEARCH ============

    private List<SearchResult> searchWithBrave(String query, SearchOptions options) throws Exception {
        List<SearchResult> results = new ArrayList<>();

        StringBuilder urlBuilder = new StringBuilder(BRAVE_API_URL);
        urlBuilder.append("?q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        urlBuilder.append("&count=").append(options.getMaxResults());
        urlBuilder.append("&search_lang=de&country=de");

        if (options.getTimeFilter() != null) {
            urlBuilder.append("&freshness=").append(options.getTimeFilter().getBraveValue());
        }

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .header("Accept", "application/json")
                .header("X-Subscription-Token", braveApiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 401) throw new Exception("Ungültiger API-Key");
            if (response.code() == 429) throw new Exception("Rate-Limit erreicht");
            if (!response.isSuccessful()) throw new Exception("HTTP " + response.code());

            String json = response.body().string();
            JsonNode root = objectMapper.readTree(json);
            JsonNode webResults = root.path("web").path("results");

            if (webResults.isArray()) {
                for (JsonNode item : webResults) {
                    String title = getJsonText(item, "title");
                    String resultUrl = getJsonText(item, "url");
                    String snippet = getJsonText(item, "description");

                    if (resultUrl != null && !resultUrl.isEmpty() && title != null) {
                        results.add(new SearchResult(title, resultUrl, snippet != null ? snippet : ""));
                    }
                }
            }
        }

        return results;
    }

    // ============ SEARXNG ============

    private List<SearchResult> searchWithSearXNG(String instance, String query, SearchOptions options) throws Exception {
        List<SearchResult> results = new ArrayList<>();

        StringBuilder urlBuilder = new StringBuilder(instance);
        urlBuilder.append("/search?q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        urlBuilder.append("&format=json&language=de");

        if (options.getTimeFilter() != null) {
            urlBuilder.append("&time_range=").append(options.getTimeFilter().getSearxngValue());
        }

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 429) throw new Exception("Rate-Limited");
            if (!response.isSuccessful()) throw new Exception("HTTP " + response.code());

            String responseBody = response.body().string();
            if (responseBody.trim().startsWith("<!DOCTYPE") || responseBody.contains("Too Many Requests")) {
                throw new Exception("Bot-Schutz/Rate-Limit");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultsNode = root.get("results");

            if (resultsNode != null && resultsNode.isArray()) {
                for (int i = 0; i < resultsNode.size() && results.size() < options.getMaxResults(); i++) {
                    JsonNode item = resultsNode.get(i);
                    String title = getJsonText(item, "title");
                    String resultUrl = getJsonText(item, "url");
                    String snippet = getJsonText(item, "content");

                    if (resultUrl != null && !resultUrl.isEmpty() && title != null) {
                        results.add(new SearchResult(title, resultUrl, snippet != null ? snippet : ""));
                    }
                }
            }
        }

        return results;
    }

    // ============ QUERY BUILDER ============

    private String buildQuery(String query, List<String> includeDomains, List<String> excludeDomains, TimeFilter timeFilter) {
        StringBuilder sb = new StringBuilder(query);

        if (includeDomains != null && !includeDomains.isEmpty()) {
            sb.append(" (");
            for (int i = 0; i < includeDomains.size(); i++) {
                if (i > 0) sb.append(" OR ");
                sb.append("site:").append(includeDomains.get(i));
            }
            sb.append(")");
        }

        if (excludeDomains != null && !excludeDomains.isEmpty()) {
            for (String domain : excludeDomains) {
                sb.append(" -site:").append(domain);
            }
        }

        return sb.toString();
    }

    private String buildCacheKey(String query, SearchOptions options) {
        return query.toLowerCase().trim() + "|" +
               options.getMaxResults() + "|" +
               (options.getTimeFilter() != null ? options.getTimeFilter().name() : "none");
    }

    // ============ ZÄHLER ============

    private void checkAndResetMonthlyCounter() {
        YearMonth now = YearMonth.now();
        if (!now.equals(currentMonth)) {
            log.info("Neuer Monat - Zähler zurückgesetzt");
            searchCountThisMonth = 0;
            searxngMonthCount = 0;
            currentMonth = now;
            saveCounterToDatabase();
            saveSearxngCounterToDatabase();
        }
    }

    private void incrementSearchCounter() {
        searchCountThisMonth++;
        saveCounterToDatabase();
    }

    private void incrementSearxngCounter() {
        searxngTotalCount++;
        searxngMonthCount++;
        saveSearxngCounterToDatabase();
    }

    @Transactional
    public void saveSearxngCounterToDatabase() {
        saveSetting(AppSettings.KEY_WEBSEARCH_SEARXNG_COUNT, String.valueOf(searxngTotalCount), "SearXNG Gesamtzähler");
        saveSetting(AppSettings.KEY_WEBSEARCH_SEARXNG_MONTH_COUNT, String.valueOf(searxngMonthCount), "SearXNG Monatszähler");
    }

    @Transactional
    public void saveCounterToDatabase() {
        saveSetting(AppSettings.KEY_WEBSEARCH_COUNT, String.valueOf(searchCountThisMonth), "Monatlicher Suchzähler");
        saveSetting(AppSettings.KEY_WEBSEARCH_MONTH, currentMonth.toString(), "Aktueller Monat für Zähler");
    }

    public int getRemainingSearches() {
        checkAndResetMonthlyCounter();
        return Math.max(0, BRAVE_FREE_LIMIT - searchCountThisMonth);
    }

    public int getMonthlyLimit() {
        return BRAVE_FREE_LIMIT;
    }

    public boolean isBraveConfigured() {
        return braveApiKey != null && !braveApiKey.isBlank();
    }

    // ============ SETTER ============

    @Transactional
    public void setBraveApiKey(String apiKey) {
        this.braveApiKey = apiKey != null ? apiKey.trim() : "";
        saveSetting(AppSettings.KEY_WEBSEARCH_BRAVE_API_KEY, this.braveApiKey, "Brave Search API Key");
        log.info("Brave API Key gespeichert");
    }

    @Transactional
    public void setCustomSearxngInstance(String instance) {
        this.customSearxngInstance = instance != null ? instance.trim() : "";
        saveSetting(AppSettings.KEY_WEBSEARCH_CUSTOM_INSTANCE, this.customSearxngInstance, "Eigene SearXNG Instanz");
        rebuildInstanceList();
        log.info("Eigene SearXNG Instanz gespeichert: {}", customSearxngInstance);
    }

    @Transactional
    public void setInstances(List<String> instances) {
        if (instances != null && !instances.isEmpty()) {
            this.searxngInstances = new ArrayList<>(instances);
            try {
                String json = objectMapper.writeValueAsString(instances);
                saveSetting(AppSettings.KEY_WEBSEARCH_INSTANCES, json, "SearXNG Fallback-Instanzen");
            } catch (Exception e) {
                log.error("Konnte Instanzen nicht serialisieren: {}", e.getMessage());
            }
        }
        rebuildInstanceList();
    }

    public List<String> getInstances() {
        return new ArrayList<>(searxngInstances);
    }

    public static List<String> getDefaultInstances() {
        return DEFAULT_SEARXNG_INSTANCES;
    }

    // ============ FEATURE FLAG SETTER ============

    @Transactional
    public void setQueryOptimizationEnabled(boolean enabled) {
        this.queryOptimizationEnabled = enabled;
        saveSetting(AppSettings.KEY_WEBSEARCH_QUERY_OPTIMIZATION, String.valueOf(enabled), "Query-Optimierung aktiviert");
        log.info("Query-Optimierung: {}", enabled);
    }

    @Transactional
    public void setContentScrapingEnabled(boolean enabled) {
        this.contentScrapingEnabled = enabled;
        saveSetting(AppSettings.KEY_WEBSEARCH_CONTENT_SCRAPING, String.valueOf(enabled), "Content-Scraping aktiviert");
        log.info("Content-Scraping: {}", enabled);
    }

    @Transactional
    public void setMultiQueryEnabled(boolean enabled) {
        this.multiQueryEnabled = enabled;
        saveSetting(AppSettings.KEY_WEBSEARCH_MULTI_QUERY, String.valueOf(enabled), "Multi-Query aktiviert");
        log.info("Multi-Query: {}", enabled);
    }

    @Transactional
    public void setReRankingEnabled(boolean enabled) {
        this.reRankingEnabled = enabled;
        saveSetting(AppSettings.KEY_WEBSEARCH_RERANKING, String.valueOf(enabled), "Re-Ranking aktiviert");
        log.info("Re-Ranking: {}", enabled);
    }

    @Transactional
    public void setQueryOptimizationModel(String model) {
        this.queryOptimizationModel = model != null ? model.trim() : "llama3.2:3b";
        saveSetting(AppSettings.KEY_WEBSEARCH_OPTIMIZATION_MODEL, this.queryOptimizationModel, "Modell für Query-Optimierung");
        log.info("Optimierungs-Modell (konfiguriert): {}", queryOptimizationModel);

        // Effektives Modell neu bestimmen
        determineEffectiveModel();
    }

    private void saveSetting(String key, String value, String description) {
        AppSettings setting = settingsRepository.findByKey(key).orElse(new AppSettings());
        setting.setKey(key);
        setting.setValue(value);
        setting.setDescription(description);
        settingsRepository.saveAndFlush(setting);
        log.info("DB-Einstellung gespeichert: {} = {}", key,
            value != null && value.length() > 50 ? value.substring(0, 50) + "..." : value);
    }

    private String getJsonText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    // ============ FORMAT METHODEN ============

    public String formatForContext(List<SearchResult> results) {
        return formatForContext(results, false);
    }

    public String formatForContext(List<SearchResult> results, boolean includeSourceUrls) {
        if (results.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("=== WEB-SUCHERGEBNISSE ===\n");

        if (includeSourceUrls) {
            sb.append("WICHTIG: Antworte auf DEUTSCH!\n\n");
        }

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append(String.format("**Quelle %d:** %s\n", i + 1, r.title()));
            sb.append(String.format("URL: %s\n", r.url()));

            String content = r.snippet();
            if (content != null && !content.isEmpty()) {
                // Bei vollständigen Inhalten mehr anzeigen
                int maxLen = content.length() > 500 ? 500 : content.length();
                sb.append(String.format("Inhalt: %s\n", content.substring(0, maxLen)));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public String formatSourcesFooter(List<SearchResult> results) {
        if (results == null || results.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n---\n📚 **Quellen:**\n");

        for (SearchResult r : results) {
            sb.append("- [").append(r.title()).append("](").append(r.url()).append(")\n");
        }

        return sb.toString();
    }

    // ============ SMART AUTO-SEARCH KEYWORDS ============

    /**
     * Keywords die eine explizite Recherche-Anforderung signalisieren
     * Diese lösen IMMER eine Web-Suche aus
     */
    private static final List<String> EXPLICIT_SEARCH_KEYWORDS = List.of(
        // Explizite Recherche-Aufforderungen
        "recherchiere", "recherchier", "recherche für mich",
        "such im web", "such im internet", "suche im internet",
        "google", "googel", "googlen", "googl mal",
        "schau im internet nach", "schau im web nach",
        "online nachschauen", "online recherchieren"
    );

    /**
     * Zeit-bezogene Keywords die aktuelle Informationen erfordern
     * Diese lösen eine Web-Suche aus, wenn sie mit Frage-Kontext kombiniert werden
     */
    private static final List<String> TIME_BASED_KEYWORDS = List.of(
        // Aktualität
        "aktuell", "aktuelle", "aktuellen", "aktueller",
        "neueste", "neuesten", "neuste", "neusten",
        "heute", "gestern", "vorgestern",
        "letzte woche", "letzten woche", "letzte monat", "letzten monat",
        "diese woche", "diesen monat", "dieses jahr",
        "vor kurzem", "kürzlich", "gerade eben",
        // Jahresangaben (für aktuelle Events)
        "2024", "2025", "2026",
        // News/Updates
        "nachrichten", "news", "neuigkeiten", "meldungen",
        "was gibt es neues", "was ist passiert"
    );

    /**
     * Prüft ob eine automatische Web-Suche durchgeführt werden soll
     *
     * Die Suche wird NUR ausgelöst bei:
     * 1. Expliziten Recherche-Aufforderungen (recherchiere, google, etc.)
     * 2. Zeit-bezogenen Anfragen (aktuell, letzte Woche, etc.) kombiniert mit Frage-Kontext
     *
     * Die Suche wird NICHT ausgelöst bei:
     * - Allgemeinen Fragen ohne Aktualitäts-Bezug
     * - Wissensfragen die das LLM beantworten kann
     */
    public boolean shouldAutoSearch(String message) {
        if (message == null || message.isBlank()) return false;
        String lowerMessage = message.toLowerCase();

        // NUR explizite Recherche-Aufforderung triggert Web-Suche!
        // Ansonsten muss der Nutzer den Web-Suche-Button klicken.
        //
        // Dies verhindert unerwünschte automatische Suchen bei:
        // - Normalen Fragen die das LLM selbst beantworten kann
        // - Zeit-bezogenen Fragen (der Nutzer muss explizit suchen wollen)
        //
        // Keywords: "recherchiere", "google", "suche im internet", etc.
        if (EXPLICIT_SEARCH_KEYWORDS.stream().anyMatch(lowerMessage::contains)) {
            log.debug("🔍 Auto-Search: Explizite Recherche-Aufforderung erkannt");
            return true;
        }

        // Zeit-basierte Keywords lösen KEINE automatische Suche mehr aus!
        // Der Nutzer muss explizit den Web-Suche-Button klicken oder ein
        // explizites Keyword wie "google" oder "recherchiere" verwenden.
        //
        // Begründung: Viele Zeit-bezogene Fragen können vom LLM basierend
        // auf dem Trainingswissen beantwortet werden.

        return false;
    }

    /**
     * Leert den Such-Cache
     */
    public void clearCache() {
        searchCache.invalidateAll();
        contentCache.invalidateAll();
        log.info("Such-Cache geleert");
    }

    // ============ RECORDS & ENUMS ============

    public record SearchResult(String title, String url, String snippet) {}

    public enum TimeFilter {
        DAY("pd", "day"),
        WEEK("pw", "week"),
        MONTH("pm", "month"),
        YEAR("py", "year");

        private final String braveValue;
        private final String searxngValue;

        TimeFilter(String braveValue, String searxngValue) {
            this.braveValue = braveValue;
            this.searxngValue = searxngValue;
        }

        public String getBraveValue() { return braveValue; }
        public String getSearxngValue() { return searxngValue; }
    }

    @Getter @Setter
    public static class SearchOptions {
        private int maxResults = 7;
        private List<String> domains;
        private List<String> excludeDomains;
        private TimeFilter timeFilter;
        private boolean optimizeQuery = true;
        private boolean fetchFullContent = false;
        private boolean multiQuery = false;
        private boolean reRank = true;
        private int maxContentLength = 1000;
        private String expertContext; // z.B. "Rechtsanwalt", "Steuerberater", "IT-Experte"

        public String getExpertContext() { return expertContext; }
        public void setExpertContext(String expertContext) { this.expertContext = expertContext; }

        @Override
        public String toString() {
            return String.format("SearchOptions[max=%d, optimize=%b, fullContent=%b, reRank=%b, time=%s, expert=%s]",
                maxResults, optimizeQuery, fetchFullContent, reRank, timeFilter, expertContext);
        }
    }
}
