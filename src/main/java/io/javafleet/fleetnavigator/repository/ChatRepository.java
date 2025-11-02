package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Chat entity
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * Find all chats ordered by most recently updated
     */
    List<Chat> findAllByOrderByUpdatedAtDesc();

    /**
     * Find chats by model
     */
    List<Chat> findByModel(String model);

    /**
     * Find all chats belonging to a specific project
     */
    List<Chat> findByProjectId(Long projectId);
}
