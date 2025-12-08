package io.javafleet.fleetnavigator.experts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ein Turn in der Conversation-History
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTurn {

    /**
     * User-Eingabe
     */
    private String userInput;

    /**
     * Experten-Antwort
     */
    private String expertResponse;
}
