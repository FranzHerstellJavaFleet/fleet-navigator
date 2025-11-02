package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * Count messages in a chat
     */
    Long countByChatId(Long chatId);
}
