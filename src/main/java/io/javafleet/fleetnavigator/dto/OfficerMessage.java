package io.javafleet.fleetnavigator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Message from Fleet Officer to Navigator
 */
@Data
public class OfficerMessage {
    private String type;

    @JsonProperty("officer_id")
    private String officerId;

    private Object data;
    private OffsetDateTime timestamp;
}
