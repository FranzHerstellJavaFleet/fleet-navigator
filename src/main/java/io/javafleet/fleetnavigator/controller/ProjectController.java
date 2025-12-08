package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for project management
 */
@RestController
@RequestMapping("/api/projects")
@Slf4j
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project
     */
    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@RequestBody CreateProjectRequest request) {
        log.info("Creating new project: {}", request.getName());
        ProjectDTO project = projectService.createProject(request);
        return ResponseEntity.ok(project);
    }

    /**
     * Get all projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project by ID
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable Long projectId) {
        ProjectDTO project = projectService.getProject(projectId);
        return ResponseEntity.ok(project);
    }

    /**
     * Update project
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long projectId,
            @RequestBody CreateProjectRequest request) {
        log.info("Updating project {}: {}", projectId, request.getName());
        ProjectDTO project = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(project);
    }

    /**
     * Delete project
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        log.info("Deleting project: {}", projectId);
        projectService.deleteProject(projectId);
        return ResponseEntity.ok().build();
    }

    /**
     * Upload context file to project
     */
    @PostMapping("/context-files")
    public ResponseEntity<ContextFileDTO> uploadContextFile(@RequestBody UploadContextFileRequest request) {
        log.info("Uploading context file '{}' to project {}", request.getFilename(), request.getProjectId());
        ContextFileDTO file = projectService.uploadContextFile(request);
        return ResponseEntity.ok(file);
    }

    /**
     * Get context file content
     */
    @GetMapping("/context-files/{fileId}/content")
    public ResponseEntity<String> getContextFileContent(@PathVariable Long fileId) {
        String content = projectService.getContextFileContent(fileId);
        return ResponseEntity.ok(content);
    }

    /**
     * Delete context file
     */
    @DeleteMapping("/context-files/{fileId}")
    public ResponseEntity<Void> deleteContextFile(@PathVariable Long fileId) {
        log.info("Deleting context file: {}", fileId);
        projectService.deleteContextFile(fileId);
        return ResponseEntity.ok().build();
    }

    /**
     * Assign chat to project
     */
    @PutMapping("/{projectId}/chats/{chatId}")
    public ResponseEntity<ChatDTO> assignChatToProject(
            @PathVariable Long projectId,
            @PathVariable Long chatId) {
        log.info("Assigning chat {} to project {}", chatId, projectId);
        ChatDTO chat = projectService.assignChatToProject(chatId, projectId);
        return ResponseEntity.ok(chat);
    }

    /**
     * Unassign chat from project
     */
    @DeleteMapping("/chats/{chatId}")
    public ResponseEntity<ChatDTO> unassignChatFromProject(@PathVariable Long chatId) {
        log.info("Unassigning chat {} from project", chatId);
        ChatDTO chat = projectService.assignChatToProject(chatId, null);
        return ResponseEntity.ok(chat);
    }

    /**
     * Get all chats for a project
     */
    @GetMapping("/{projectId}/chats")
    public ResponseEntity<List<ChatDTO>> getProjectChats(@PathVariable Long projectId) {
        List<ChatDTO> chats = projectService.getProjectChats(projectId);
        return ResponseEntity.ok(chats);
    }
}
