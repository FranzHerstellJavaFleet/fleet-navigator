package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Context file entity - stores text files that provide context for a project
 */
@Entity
@Table(name = "context_files")
@Getter
@Setter
public class ContextFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String filename;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "file_type")
    private String fileType; // .txt, .md, .java, .py, etc.

    @Column(nullable = false)
    private Long size; // Size in bytes

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    /**
     * Estimate tokens in this file (rough estimate: 1 token ≈ 4 characters)
     */
    public int estimateTokens() {
        return content.length() / 4;
    }
}
