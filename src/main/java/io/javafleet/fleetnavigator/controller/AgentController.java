package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.EmailAgentSettingsDTO;
import io.javafleet.fleetnavigator.dto.DocumentAgentSettingsDTO;
import io.javafleet.fleetnavigator.dto.OSAgentSettingsDTO;
import io.javafleet.fleetnavigator.service.AgentSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Distributed Agent System.
 * CORS is handled globally in WebConfig.
 *
 * Endpoints:
 * - GET  /api/agents/email/settings
 * - PUT  /api/agents/email/settings
 * - GET  /api/agents/document/settings
 * - PUT  /api/agents/document/settings
 * - GET  /api/agents/os/settings
 * - PUT  /api/agents/os/settings
 */
@RestController
@RequestMapping("/api/agents")
@Slf4j
@RequiredArgsConstructor
public class AgentController {

    private final AgentSettingsService agentSettingsService;

    // ============================================================================
    // EMAIL AGENT
    // ============================================================================

    /**
     * Get Email Agent settings.
     * GET /api/agents/email/settings
     */
    @GetMapping("/email/settings")
    public ResponseEntity<EmailAgentSettingsDTO> getEmailAgentSettings() {
        log.info("GET /api/agents/email/settings - Getting Email Agent settings");
        EmailAgentSettingsDTO settings = agentSettingsService.getEmailAgentSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Update Email Agent settings.
     * PUT /api/agents/email/settings
     */
    @PutMapping("/email/settings")
    public ResponseEntity<EmailAgentSettingsDTO> updateEmailAgentSettings(
            @RequestBody EmailAgentSettingsDTO settings) {
        log.info("PUT /api/agents/email/settings - Updating Email Agent settings");
        EmailAgentSettingsDTO updated = agentSettingsService.updateEmailAgentSettings(settings);
        return ResponseEntity.ok(updated);
    }

    /**
     * Email Agent status endpoint (Coming Soon).
     * GET /api/agents/email/status
     */
    @GetMapping("/email/status")
    public ResponseEntity<String> getEmailAgentStatus() {
        return ResponseEntity.ok("{\"status\":\"coming_soon\",\"message\":\"Email Agent is coming soon!\"}");
    }

    // ============================================================================
    // DOCUMENT AGENT
    // ============================================================================

    /**
     * Get Document Agent settings.
     * GET /api/agents/document/settings
     */
    @GetMapping("/document/settings")
    public ResponseEntity<DocumentAgentSettingsDTO> getDocumentAgentSettings() {
        log.info("GET /api/agents/document/settings - Getting Document Agent settings");
        DocumentAgentSettingsDTO settings = agentSettingsService.getDocumentAgentSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Update Document Agent settings.
     * PUT /api/agents/document/settings
     */
    @PutMapping("/document/settings")
    public ResponseEntity<DocumentAgentSettingsDTO> updateDocumentAgentSettings(
            @RequestBody DocumentAgentSettingsDTO settings) {
        log.info("PUT /api/agents/document/settings - Updating Document Agent settings");
        DocumentAgentSettingsDTO updated = agentSettingsService.updateDocumentAgentSettings(settings);
        return ResponseEntity.ok(updated);
    }

    /**
     * Document Agent status endpoint (Coming Soon).
     * GET /api/agents/document/status
     */
    @GetMapping("/document/status")
    public ResponseEntity<String> getDocumentAgentStatus() {
        return ResponseEntity.ok("{\"status\":\"coming_soon\",\"message\":\"Document Agent is coming soon!\"}");
    }

    // ============================================================================
    // OS AGENT
    // ============================================================================

    /**
     * Get OS Agent settings.
     * GET /api/agents/os/settings
     */
    @GetMapping("/os/settings")
    public ResponseEntity<OSAgentSettingsDTO> getOSAgentSettings() {
        log.info("GET /api/agents/os/settings - Getting OS Agent settings");
        OSAgentSettingsDTO settings = agentSettingsService.getOSAgentSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Update OS Agent settings.
     * PUT /api/agents/os/settings
     */
    @PutMapping("/os/settings")
    public ResponseEntity<OSAgentSettingsDTO> updateOSAgentSettings(
            @RequestBody OSAgentSettingsDTO settings) {
        log.info("PUT /api/agents/os/settings - Updating OS Agent settings");
        OSAgentSettingsDTO updated = agentSettingsService.updateOSAgentSettings(settings);
        return ResponseEntity.ok(updated);
    }

    /**
     * OS Agent status endpoint (Coming Soon).
     * GET /api/agents/os/status
     */
    @GetMapping("/os/status")
    public ResponseEntity<String> getOSAgentStatus() {
        return ResponseEntity.ok("{\"status\":\"coming_soon\",\"message\":\"OS Agent is coming soon!\"}");
    }

    // ============================================================================
    // GENERAL
    // ============================================================================

    /**
     * Get all agents status overview.
     * GET /api/agents/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<String> getAgentsOverview() {
        log.info("GET /api/agents/overview - Getting agents overview");

        String overview = """
            {
                "agents": [
                    {
                        "name": "Email Agent",
                        "icon": "📧",
                        "status": "coming_soon",
                        "description": "Manage emails with AI assistance"
                    },
                    {
                        "name": "Document Agent",
                        "icon": "📄",
                        "status": "coming_soon",
                        "description": "Process and analyze documents"
                    },
                    {
                        "name": "OS Agent",
                        "icon": "💻",
                        "status": "coming_soon",
                        "description": "Execute OS commands in sandbox"
                    }
                ],
                "totalAgents": 3,
                "activeAgents": 0,
                "comingSoonAgents": 3
            }
            """;

        return ResponseEntity.ok(overview);
    }
}
