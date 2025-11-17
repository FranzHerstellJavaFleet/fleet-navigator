package io.javafleet.fleetnavigator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Message from Fleet Mate to Navigator
 */
@Data
public class MateMessage {
    private String type;

    @JsonProperty("mate_id")
    private String mateId;

    private Object data;
    private OffsetDateTime timestamp;
}
