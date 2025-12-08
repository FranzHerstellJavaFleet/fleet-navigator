package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.SystemPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemPromptTemplateRepository extends JpaRepository<SystemPromptTemplate, Long> {

    List<SystemPromptTemplate> findAllByOrderByCreatedAtDesc();

    SystemPromptTemplate findByIsDefaultTrue();
}
