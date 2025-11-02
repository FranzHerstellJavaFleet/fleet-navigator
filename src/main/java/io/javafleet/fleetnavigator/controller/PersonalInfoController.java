package io.javafleet.fleetnavigator.controller;

import io.javafleet.fleetnavigator.model.PersonalInfo;
import io.javafleet.fleetnavigator.service.PersonalInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Personal Information
 */
@RestController
@RequestMapping("/api/personal-info")
@RequiredArgsConstructor
@Slf4j
public class PersonalInfoController {

    private final PersonalInfoService personalInfoService;

    /**
     * Get personal info
     * GET /api/personal-info
     */
    @GetMapping
    public ResponseEntity<PersonalInfo> getPersonalInfo() {
        return personalInfoService.getPersonalInfo()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new PersonalInfo())); // Return empty object if not found
    }

    /**
     * Save or update personal info
     * PUT /api/personal-info
     */
    @PutMapping
    public ResponseEntity<PersonalInfo> savePersonalInfo(@RequestBody PersonalInfo personalInfo) {
        try {
            PersonalInfo saved = personalInfoService.savePersonalInfo(personalInfo);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error saving personal info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete personal info
     * DELETE /api/personal-info
     */
    @DeleteMapping
    public ResponseEntity<Void> deletePersonalInfo() {
        try {
            personalInfoService.deletePersonalInfo();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting personal info", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
