package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.ChatDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatDocumentRepository extends JpaRepository<ChatDocument, Long> {

    /**
     * Find all documents for a chat, ordered by creation date
     */
    List<ChatDocument> findByChatIdOrderByCreatedAtAsc(Long chatId);

    /**
     * Delete all documents for a chat
     */
    void deleteByChatId(Long chatId);

    /**
     * Count documents for a chat
     */
    long countByChatId(Long chatId);
}
