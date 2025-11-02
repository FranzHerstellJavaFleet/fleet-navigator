package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Project entity - groups chats and provides shared context
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContextFile> contextFiles = new ArrayList<>();

    @OneToMany(mappedBy = "project")
    private List<Chat> chats = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get total size of all context files in bytes
     */
    public long getTotalContextSize() {
        return contextFiles.stream()
                .mapToLong(ContextFile::getSize)
                .sum();
    }

    /**
     * Get combined context from all files
     */
    public String getCombinedContext() {
        StringBuilder combined = new StringBuilder();
        combined.append("=== PROJECT CONTEXT: ").append(name).append(" ===\n\n");

        if (description != null && !description.isEmpty()) {
            combined.append("Description: ").append(description).append("\n\n");
        }

        for (ContextFile file : contextFiles) {
            combined.append("--- File: ").append(file.getFilename()).append(" ---\n");
            combined.append(file.getContent()).append("\n\n");
        }

        return combined.toString();
    }
}
