package io.javafleet.fleetnavigator.dto;

import lombok.Data;

@Data
public class CreateProjectRequest {
    private String name;
    private String description;
}
