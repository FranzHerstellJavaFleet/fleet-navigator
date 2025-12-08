package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * Find chat by ID with Project eagerly loaded (JOIN FETCH)
     * Avoids LazyInitializationException when accessing project outside transaction
     */
    @Query("SELECT c FROM Chat c LEFT JOIN FETCH c.project p LEFT JOIN FETCH p.contextFiles WHERE c.id = :id")
    Optional<Chat> findByIdWithProject(@Param("id") Long id);
}
