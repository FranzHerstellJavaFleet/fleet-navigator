package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Message entity
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all messages for a specific chat
     */
    List<Message> findByChatIdOrderByCreatedAtAsc(Long chatId);

    /**
     * Count total tokens for a chat
     */
    @Query("SELECT COALESCE(SUM(m.tokens), 0) FROM Message m WHERE m.chat.id = :chatId")
    Integer sumTokensByChatId(Long chatId);

    /**
     * Count total tokens for ALL chats in a project
     */
    @Query("SELECT COALESCE(SUM(m.tokens), 0) FROM Message m WHERE m.chat.project.id = :projectId")
    Integer sumTokensByProjectId(Long projectId);

    /**
     * Count messages in a chat
     */
    Long countByChatId(Long chatId);

    /**
     * Find message by downloadUrl (used to update Fleet-Mate document path)
     */
    Optional<Message> findByDownloadUrl(String downloadUrl);
}
