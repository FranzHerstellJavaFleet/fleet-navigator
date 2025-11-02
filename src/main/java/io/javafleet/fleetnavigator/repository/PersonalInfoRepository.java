package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.PersonalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for PersonalInfo entity
 */
@Repository
public interface PersonalInfoRepository extends JpaRepository<PersonalInfo, Long> {
}
