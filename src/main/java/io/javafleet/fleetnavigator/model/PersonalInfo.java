package io.javafleet.fleetnavigator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for storing personal information for document generation
 */
@Entity
@Table(name = "personal_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Personal Details
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "title")
    private String title; // Dr., Prof., etc.

    // Address
    @Column(name = "street")
    private String street;

    @Column(name = "house_number")
    private String houseNumber;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    // Contact
    @Column(name = "phone")
    private String phone;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "email")
    private String email;

    // Additional
    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "company")
    private String company;

    @Column(name = "position")
    private String position;

    // Metadata
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get full name with optional title
     */
    public String getFullName() {
        StringBuilder name = new StringBuilder();
        if (title != null && !title.trim().isEmpty()) {
            name.append(title).append(" ");
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName).append(" ");
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            name.append(lastName);
        }
        return name.toString().trim();
    }

    /**
     * Get full address
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (street != null && !street.trim().isEmpty()) {
            address.append(street);
            if (houseNumber != null && !houseNumber.trim().isEmpty()) {
                address.append(" ").append(houseNumber);
            }
            address.append("\n");
        }
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            address.append(postalCode).append(" ");
        }
        if (city != null && !city.trim().isEmpty()) {
            address.append(city);
        }
        return address.toString().trim();
    }
}
