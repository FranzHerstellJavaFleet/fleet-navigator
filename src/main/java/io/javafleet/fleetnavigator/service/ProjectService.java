package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.*;
import io.javafleet.fleetnavigator.model.Chat;
import io.javafleet.fleetnavigator.model.ContextFile;
import io.javafleet.fleetnavigator.model.Project;
import io.javafleet.fleetnavigator.repository.ChatRepository;
import io.javafleet.fleetnavigator.repository.ContextFileRepository;
import io.javafleet.fleetnavigator.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing projects and their context files
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ContextFileRepository contextFileRepository;
    private final ChatRepository chatRepository;

    /**
     * Create a new project
     */
    @Transactional
    public ProjectDTO createProject(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());

        project = projectRepository.save(project);
        log.info("Created new project: {} (ID: {})", project.getName(), project.getId());

        return mapToProjectDTO(project);
    }

    /**
     * Get all projects
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::mapToProjectDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get project by ID with full details
     */
    @Transactional(readOnly = true)
    public ProjectDTO getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        return mapToProjectDTO(project);
    }

    /**
     * Update project details
     */
    @Transactional
    public ProjectDTO updateProject(Long projectId, CreateProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        project = projectRepository.save(project);
        log.info("Updated project: {} (ID: {})", project.getName(), project.getId());

        return mapToProjectDTO(project);
    }

    /**
     * Delete project
     */
    @Transactional
    public void deleteProject(Long projectId) {
        // Unlink all chats from this project
        List<Chat> chats = chatRepository.findByProjectId(projectId);
        for (Chat chat : chats) {
            chat.setProject(null);
        }
        chatRepository.saveAll(chats);

        // Delete project (cascade will delete context files)
        projectRepository.deleteById(projectId);
        log.info("Deleted project: {}", projectId);
    }

    /**
     * Upload a context file to a project
     */
    @Transactional
    public ContextFileDTO uploadContextFile(UploadContextFileRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));

        ContextFile contextFile = new ContextFile();
        contextFile.setProject(project);
        contextFile.setFilename(request.getFilename());
        contextFile.setContent(request.getContent());
        contextFile.setFileType(request.getFileType());
        contextFile.setSize((long) request.getContent().length());

        contextFile = contextFileRepository.save(contextFile);
        log.info("Uploaded context file '{}' to project '{}' ({} bytes)",
                contextFile.getFilename(), project.getName(), contextFile.getSize());

        return mapToContextFileDTO(contextFile);
    }

    /**
     * Get content of a specific context file
     */
    @Transactional(readOnly = true)
    public String getContextFileContent(Long fileId) {
        ContextFile file = contextFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Context file not found: " + fileId));
        return file.getContent();
    }

    /**
     * Delete a context file
     */
    @Transactional
    public void deleteContextFile(Long fileId) {
        contextFileRepository.deleteById(fileId);
        log.info("Deleted context file: {}", fileId);
    }

    /**
     * Assign a chat to a project
     */
    @Transactional
    public ChatDTO assignChatToProject(Long chatId, Long projectId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        if (projectId == null) {
            // Unassign from project
            chat.setProject(null);
            log.info("Unassigned chat {} from project", chatId);
        } else {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

            chat.setProject(project);
            log.info("Assigned chat {} to project '{}'", chatId, project.getName());
        }

        chat = chatRepository.save(chat);

        // Return simple ChatDTO (reuse existing mapping from ChatService)
        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setTitle(chat.getTitle());
        dto.setModel(chat.getModel());
        dto.setCreatedAt(chat.getCreatedAt());
        dto.setUpdatedAt(chat.getUpdatedAt());

        return dto;
    }

    /**
     * Get all chats for a project
     */
    @Transactional(readOnly = true)
    public List<ChatDTO> getProjectChats(Long projectId) {
        List<Chat> chats = chatRepository.findByProjectId(projectId);
        return chats.stream()
                .map(chat -> {
                    ChatDTO dto = new ChatDTO();
                    dto.setId(chat.getId());
                    dto.setTitle(chat.getTitle());
                    dto.setModel(chat.getModel());
                    dto.setCreatedAt(chat.getCreatedAt());
                    dto.setUpdatedAt(chat.getUpdatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Map Project entity to ProjectDTO
     */
    private ProjectDTO mapToProjectDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());

        // Map context files
        List<ContextFileDTO> contextFileDTOs = project.getContextFiles().stream()
                .map(this::mapToContextFileDTO)
                .collect(Collectors.toList());
        dto.setContextFiles(contextFileDTOs);

        // Map chat IDs
        List<Long> chatIds = project.getChats().stream()
                .map(Chat::getId)
                .collect(Collectors.toList());
        dto.setChatIds(chatIds);

        // Calculate total context size and estimated tokens
        dto.setTotalContextSize(project.getTotalContextSize());
        dto.setEstimatedTokens(project.getContextFiles().stream()
                .mapToInt(ContextFile::estimateTokens)
                .sum());

        return dto;
    }

    /**
     * Map ContextFile entity to ContextFileDTO
     */
    private ContextFileDTO mapToContextFileDTO(ContextFile file) {
        ContextFileDTO dto = new ContextFileDTO();
        dto.setId(file.getId());
        dto.setFilename(file.getFilename());
        dto.setFileType(file.getFileType());
        dto.setSize(file.getSize());
        dto.setEstimatedTokens(file.estimateTokens());
        dto.setUploadedAt(file.getUploadedAt());
        return dto;
    }
}
