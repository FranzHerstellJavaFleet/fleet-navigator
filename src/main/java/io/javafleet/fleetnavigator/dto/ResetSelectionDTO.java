package io.javafleet.fleetnavigator.dto;

import lombok.Data;

/**
 * DTO for selective data reset.
 */
@Data
public class ResetSelectionDTO {
    private boolean chats;
    private boolean projects;
    private boolean customModels;
    private boolean settings;
    private boolean personalInfo;
    private boolean templates;
    private boolean stats;
}
