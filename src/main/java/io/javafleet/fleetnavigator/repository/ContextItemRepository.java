package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.ContextItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ContextItem entity
 */
@Repository
public interface ContextItemRepository extends JpaRepository<ContextItem, Long> {

    /**
     * Find all context items for a specific chat
     */
    List<ContextItem> findByChatIdOrderByCreatedAtAsc(Long chatId);
}
