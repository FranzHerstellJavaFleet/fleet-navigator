package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ChatDocument Entity - Stores uploaded document content for a chat
 * This allows the document context to persist across sessions
 */
@Entity
@Table(name = "chat_document")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "content_size")
    private Integer contentSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (content != null) {
            contentSize = content.length();
        }
    }
}
