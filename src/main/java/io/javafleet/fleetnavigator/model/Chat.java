package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat Entity - Represents a conversation with an LLM model
 */
@Entity
@Table(name = "chat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContextItem> contextItems = new ArrayList<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatDocument> documents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Kontext-Zusammenfassung - wird nach dem Löschen von Nachrichten regeneriert
     * Damit der Experte den Kontext behält auch wenn Nachrichten gelöscht wurden
     */
    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    /**
     * ID des ausgewählten Experten für diesen Chat
     * null = kein Experte, normales Modell wird verwendet
     */
    @Column(name = "expert_id")
    private Long expertId;

    /**
     * Aktiver Experten-Modus für diesen Chat
     * Wird beim ersten Keyword-Match fixiert und bei Modus-Wechsel aktualisiert
     * null = "Allgemein" (Default-Modus)
     */
    @Column(name = "active_expert_mode_id")
    private Long activeExpertModeId;

    /**
     * Name des aktiven Modus (für schnellen Zugriff ohne DB-Lookup)
     */
    @Column(name = "active_expert_mode_name", length = 100)
    private String activeExpertModeName;

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
     * Helper method to add a message to this chat
     */
    public void addMessage(Message message) {
        messages.add(message);
        message.setChat(this);
    }

    /**
     * Helper method to add a context item to this chat
     */
    public void addContextItem(ContextItem contextItem) {
        contextItems.add(contextItem);
        contextItem.setChat(this);
    }
}
