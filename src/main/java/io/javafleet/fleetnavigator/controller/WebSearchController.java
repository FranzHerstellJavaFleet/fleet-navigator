package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.service.WebSearchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller für Web-Suche Einstellungen
 * - Brave Search API Key Management
 * - Suchzähler
 * - SearXNG Instanzen (Fallback)
 *
 * Alle Einstellungen werden persistent in der Datenbank gespeichert.
 */
@RestController
@RequestMapping("/api/search")
@Slf4j
@RequiredArgsConstructor
public class WebSearchController {

    private final WebSearchService webSearchService;

    /**
     * GET /api/search/settings - Aktuelle Sucheinstellungen abrufen
     */
    @GetMapping("/settings")
    public ResponseEntity<WebSearchSettingsDTO> getSettings() {
        log.info("GET /api/search/settings");

        WebSearchSettingsDTO dto = new WebSearchSettingsDTO();
        dto.setBraveApiKey(maskApiKey(webSearchService.getBraveApiKey()));
        dto.setBraveConfigured(webSearchService.isBraveConfigured());
        dto.setSearchCount(webSearchService.getSearchCountThisMonth());
        dto.setSearchLimit(webSearchService.getMonthlyLimit());
        dto.setRemainingSearches(webSearchService.getRemainingSearches());
        dto.setCurrentMonth(webSearchService.getCurrentMonth().toString());
        dto.setCustomSearxngInstance(webSearchService.getCustomSearxngInstance());
        dto.setSearxngInstances(webSearchService.getInstances());
        // SearXNG Zähler
        dto.setSearxngTotalCount(webSearchService.getSearxngTotalCount());
        dto.setSearxngMonthCount(webSearchService.getSearxngMonthCount());

        // Feature Flags
        dto.setQueryOptimizationEnabled(webSearchService.isQueryOptimizationEnabled());
        dto.setContentScrapingEnabled(webSearchService.isContentScrapingEnabled());
        dto.setMultiQueryEnabled(webSearchService.isMultiQueryEnabled());
        dto.setReRankingEnabled(webSearchService.isReRankingEnabled());
        dto.setQueryOptimizationModel(webSearchService.getQueryOptimizationModel());
        dto.setEffectiveOptimizationModel(webSearchService.getEffectiveOptimizationModel());

        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/search/settings - Sucheinstellungen speichern (persistent in DB)
     */
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> saveSettings(@RequestBody WebSearchSettingsDTO settings) {
        log.info("POST /api/search/settings - Speichere persistent in Datenbank");

        // Brave API Key speichern (nur wenn nicht maskiert)
        if (settings.getBraveApiKey() != null && !settings.getBraveApiKey().contains("****")) {
            webSearchService.setBraveApiKey(settings.getBraveApiKey().trim());
            log.info("Brave API Key in DB gespeichert");
        }

        // Eigene SearXNG Instanz speichern (persistent)
        String customInstance = settings.getCustomSearxngInstance() != null
                ? settings.getCustomSearxngInstance().trim()
                : "";
        webSearchService.setCustomSearxngInstance(customInstance);

        // Instanzliste aufbauen: Eigene Instanz zuerst, dann Fallbacks
        java.util.ArrayList<String> allInstances = new java.util.ArrayList<>();
        if (!customInstance.isEmpty()) {
            allInstances.add(customInstance);
            log.info("Eigene SearXNG Instanz (persistent): {}", customInstance);
        }
        if (settings.getSearxngInstances() != null) {
            for (String instance : settings.getSearxngInstances()) {
                if (instance != null && !instance.trim().isEmpty()
                        && !instance.equals(customInstance)) {
                    allInstances.add(instance.trim());
                }
            }
        }

        // Fallback auf Defaults wenn leer
        if (allInstances.isEmpty()) {
            allInstances.addAll(WebSearchService.getDefaultInstances());
        }

        webSearchService.setInstances(allInstances);
        log.info("SearXNG Instanzen in DB gespeichert: {} gesamt", allInstances.size());

        // Feature Flags speichern
        webSearchService.setQueryOptimizationEnabled(settings.isQueryOptimizationEnabled());
        webSearchService.setContentScrapingEnabled(settings.isContentScrapingEnabled());
        webSearchService.setMultiQueryEnabled(settings.isMultiQueryEnabled());
        webSearchService.setReRankingEnabled(settings.isReRankingEnabled());
        if (settings.getQueryOptimizationModel() != null && !settings.getQueryOptimizationModel().isBlank()) {
            webSearchService.setQueryOptimizationModel(settings.getQueryOptimizationModel());
        }
        log.info("Feature-Flags gespeichert");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "braveConfigured", webSearchService.isBraveConfigured(),
                "customSearxngInstance", webSearchService.getCustomSearxngInstance(),
                "persistent", true
        ));
    }

    /**
     * GET /api/search/status - Schneller Status-Check für Zähler-Anzeige
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("braveConfigured", webSearchService.isBraveConfigured());
        status.put("searchCount", webSearchService.getSearchCountThisMonth());
        status.put("searchLimit", webSearchService.getMonthlyLimit());
        status.put("remaining", webSearchService.getRemainingSearches());
        status.put("month", webSearchService.getCurrentMonth().toString());
        status.put("searxngTotalCount", webSearchService.getSearxngTotalCount());
        status.put("searxngMonthCount", webSearchService.getSearxngMonthCount());
        status.put("customSearxngInstance", webSearchService.getCustomSearxngInstance());
        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/search/test - Testet die Suchfunktion
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSearch(@RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "test");
        log.info("POST /api/search/test - Query: {}", query);

        try {
            var results = webSearchService.search(query, 3);

            if (!results.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "resultCount", results.size(),
                        "source", webSearchService.isBraveConfigured() ? "Brave" : "SearXNG",
                        "results", results.stream().map(r -> Map.of(
                                "title", r.title(),
                                "url", r.url()
                        )).toList()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Keine Ergebnisse gefunden"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Maskiert den API-Key für die Anzeige (zeigt nur die ersten 8 Zeichen)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 8) + "****";
    }

    /**
     * DTO für Web-Suche Einstellungen
     */
    @Data
    public static class WebSearchSettingsDTO {
        // Brave API
        private String braveApiKey;
        private boolean braveConfigured;

        // Brave Suchzähler
        private int searchCount;
        private int searchLimit;
        private int remainingSearches;
        private String currentMonth;

        // SearXNG
        private String customSearxngInstance;  // Eigene Instanz (Priorität 1)
        private List<String> searxngInstances; // Öffentliche Fallback-Instanzen

        // SearXNG Suchzähler
        private int searxngTotalCount;   // Gesamte SearXNG-Suchen
        private int searxngMonthCount;   // SearXNG-Suchen diesen Monat

        // Feature Flags
        private boolean queryOptimizationEnabled;
        private boolean contentScrapingEnabled;
        private boolean multiQueryEnabled;
        private boolean reRankingEnabled;
        private String queryOptimizationModel;
        private String effectiveOptimizationModel;  // Das tatsächlich verwendete Modell (nach Fallback)
    }
}
