package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.model.PersonalInfo;
import io.javafleet.fleetnavigator.repository.PersonalInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing personal information
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalInfoService {

    private final PersonalInfoRepository personalInfoRepository;

    /**
     * Get personal info (we store only one record with ID=1)
     */
    @Transactional(readOnly = true)
    public Optional<PersonalInfo> getPersonalInfo() {
        return personalInfoRepository.findById(1L);
    }

    /**
     * Save or update personal info
     */
    @Transactional
    public PersonalInfo savePersonalInfo(PersonalInfo personalInfo) {
        // Always use ID=1 to ensure we have only one record
        personalInfo.setId(1L);
        log.info("Saving personal info for: {} {}", personalInfo.getFirstName(), personalInfo.getLastName());
        return personalInfoRepository.save(personalInfo);
    }

    /**
     * Check if personal info exists
     */
    @Transactional(readOnly = true)
    public boolean hasPersonalInfo() {
        return personalInfoRepository.existsById(1L);
    }

    /**
     * Delete personal info
     */
    @Transactional
    public void deletePersonalInfo() {
        personalInfoRepository.deleteById(1L);
        log.info("Personal info deleted");
    }
}
