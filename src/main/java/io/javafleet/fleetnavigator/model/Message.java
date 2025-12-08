package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message Entity - Represents a single message in a chat
 */
@Entity
@Table(name = "message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column
    private Integer tokens;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * JSON array of file attachment metadata.
     * Format: [{"name":"file.pdf","type":"pdf","size":12345}, ...]
     * Stores only metadata, not file content.
     */
    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments;

    /**
     * Download URL for generated documents.
     * Can be:
     * - "/api/downloads/doc/{id}" for server-generated documents
     * - "fleet-mate://{sessionId}" for locally saved documents via Fleet-Mate
     */
    @Column(name = "download_url", length = 500)
    private String downloadUrl;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Message Role - USER or ASSISTANT
     */
    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
