package io.javafleet.fleetnavigator.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Message from Fleet Mate to Navigator
 * Supports both "data" and "payload" field names for compatibility with different Mate implementations.
 */
@Data
public class MateMessage {
    private String type;

    @JsonProperty("mate_id")
    @JsonAlias("mateId")  // Go sends camelCase
    private String mateId;

    @JsonAlias("payload")  // Accept both "data" and "payload" from Go clients
    private Object data;

    private OffsetDateTime timestamp;
}
