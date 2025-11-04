package io.javafleet.fleetnavigator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a chat's model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModelRequest {
    private String model;
}
