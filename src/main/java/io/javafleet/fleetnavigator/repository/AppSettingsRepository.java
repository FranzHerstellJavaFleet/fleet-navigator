package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
    Optional<AppSettings> findByKey(String key);
}
