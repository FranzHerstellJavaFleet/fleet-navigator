package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.model.Chat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

/**
 * JUnit-Tests für Type-Safe Expert-ID Vergleiche
 *
 * Diese Tests stellen sicher, dass Expert-ID Vergleiche korrekt funktionieren,
 * auch wenn die IDs verschiedene Typen haben (Long, Integer, String).
 *
 * Das Problem tritt auf wenn:
 * - Backend sendet Long als JSON Number
 * - Frontend interpretiert als JavaScript Number
 * - Beim Vergleich === können unterschiedliche Typen fehlschlagen
 *
 * Lösung: Immer Number() oder Long.valueOf() für Vergleiche verwenden.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.0
 */
class ExpertIdTypeSafetyTest {

    private static final Long EXPERT_ID_LONG = 10L;
    private static final Integer EXPERT_ID_INT = 10;
    private static final String EXPERT_ID_STRING = "10";

    @Nested
    @DisplayName("Long vs Long Vergleiche")
    class LongComparisons {

        @Test
        @DisplayName("Gleiche Long-Werte sind gleich")
        void sameValues_AreEqual() {
            Long id1 = 10L;
            Long id2 = 10L;

            assertThat(id1).isEqualTo(id2);
            assertThat(Objects.equals(id1, id2)).isTrue();
        }

        @Test
        @DisplayName("Verschiedene Long-Werte sind ungleich")
        void differentValues_AreNotEqual() {
            Long id1 = 10L;
            Long id2 = 20L;

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("null Long-Wert wird korrekt behandelt")
        void nullValue_HandledCorrectly() {
            Long id1 = null;
            Long id2 = 10L;

            assertThat(Objects.equals(id1, id2)).isFalse();
            assertThat(Objects.equals(id2, id1)).isFalse();
            assertThat(Objects.equals(id1, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("Type Conversion Vergleiche")
    class TypeConversionComparisons {

        @Test
        @DisplayName("Long.valueOf() konvertiert String korrekt")
        void valueOf_ConvertsString() {
            Long fromString = Long.valueOf(EXPERT_ID_STRING);

            assertThat(fromString).isEqualTo(EXPERT_ID_LONG);
        }

        @Test
        @DisplayName("Long.valueOf() konvertiert Integer korrekt")
        void valueOf_ConvertsInteger() {
            Long fromInt = Long.valueOf(EXPERT_ID_INT);

            assertThat(fromInt).isEqualTo(EXPERT_ID_LONG);
        }

        @Test
        @DisplayName("Long und Integer sind vergleichbar")
        void longAndInteger_AreComparable() {
            // In Java: Long.equals(Integer) ist false!
            assertThat(EXPERT_ID_LONG.equals(EXPERT_ID_INT)).isFalse();

            // Aber mit longValue() Vergleich funktioniert es
            assertThat(EXPERT_ID_LONG.longValue()).isEqualTo(EXPERT_ID_INT.longValue());
        }
    }

    @Nested
    @DisplayName("Expert-Suche in Liste")
    class ExpertListSearch {

        @Test
        @DisplayName("Expert wird mit korrekter ID gefunden")
        void findExpert_WithCorrectId() {
            List<Expert> experts = createTestExperts();

            Expert found = experts.stream()
                    .filter(e -> Objects.equals(e.getId(), EXPERT_ID_LONG))
                    .findFirst()
                    .orElse(null);

            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("Roland Navarro");
        }

        @Test
        @DisplayName("Expert wird mit String-ID gefunden (nach Konvertierung)")
        void findExpert_WithStringId_AfterConversion() {
            List<Expert> experts = createTestExperts();
            Long searchId = Long.valueOf(EXPERT_ID_STRING);

            Expert found = experts.stream()
                    .filter(e -> Objects.equals(e.getId(), searchId))
                    .findFirst()
                    .orElse(null);

            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("Roland Navarro");
        }

        @Test
        @DisplayName("Expert wird NICHT gefunden mit falschem Type-Vergleich")
        void findExpert_FailsWithWrongTypeComparison() {
            List<Expert> experts = createTestExperts();

            // Simuliere JavaScript's === Verhalten wo "10" !== 10
            Expert found = experts.stream()
                    .filter(e -> {
                        // String.equals(Long) ist IMMER false
                        return EXPERT_ID_STRING.equals(e.getId());
                    })
                    .findFirst()
                    .orElse(null);

            // Das sollte null sein, weil String.equals(Long) fehlschlägt
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("Safe Type Comparison findet Expert immer")
        void safeTypeComparison_AlwaysFindsExpert() {
            List<Expert> experts = createTestExperts();

            // Safe comparison: Beide zu Long konvertieren
            Expert found = findExpertSafe(experts, EXPERT_ID_STRING);

            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("Roland Navarro");
        }
    }

    @Nested
    @DisplayName("Chat-Expert Zuordnung")
    class ChatExpertAssignment {

        @Test
        @DisplayName("Chat.expertId wird korrekt verglichen")
        void chatExpertId_ComparedCorrectly() {
            Chat chat = new Chat();
            chat.setExpertId(EXPERT_ID_LONG);

            List<Expert> experts = createTestExperts();

            Expert found = experts.stream()
                    .filter(e -> Objects.equals(e.getId(), chat.getExpertId()))
                    .findFirst()
                    .orElse(null);

            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(chat.getExpertId());
        }

        @Test
        @DisplayName("Chat ohne Expert-ID gibt null zurück")
        void chatWithoutExpert_ReturnsNull() {
            Chat chat = new Chat();
            chat.setExpertId(null);

            List<Expert> experts = createTestExperts();

            Expert found = experts.stream()
                    .filter(e -> Objects.equals(e.getId(), chat.getExpertId()))
                    .findFirst()
                    .orElse(null);

            assertThat(found).isNull();
        }

        @Test
        @DisplayName("Nicht existierende Expert-ID gibt null zurück")
        void nonExistentExpertId_ReturnsNull() {
            Chat chat = new Chat();
            chat.setExpertId(999L);

            List<Expert> experts = createTestExperts();

            Expert found = experts.stream()
                    .filter(e -> Objects.equals(e.getId(), chat.getExpertId()))
                    .findFirst()
                    .orElse(null);

            assertThat(found).isNull();
        }
    }

    // Helper methods

    private List<Expert> createTestExperts() {
        Expert roland = new Expert();
        roland.setId(10L);
        roland.setName("Roland Navarro");
        roland.setRole("Rechtsanwalt");

        Expert ayse = new Expert();
        ayse.setId(20L);
        ayse.setName("Ayşe Yılmaz");
        ayse.setRole("Marketing-Spezialistin");

        Expert max = new Expert();
        max.setId(30L);
        max.setName("Max Mustermann");
        max.setRole("Entwickler");

        return Arrays.asList(roland, ayse, max);
    }

    /**
     * Safe Expert lookup that handles type conversion
     * Simuliert das Frontend-Verhalten mit Number() Konvertierung
     */
    private Expert findExpertSafe(List<Expert> experts, Object expertId) {
        if (expertId == null) return null;

        Long numericId;
        try {
            if (expertId instanceof Long) {
                numericId = (Long) expertId;
            } else if (expertId instanceof Integer) {
                numericId = ((Integer) expertId).longValue();
            } else if (expertId instanceof String) {
                numericId = Long.valueOf((String) expertId);
            } else {
                numericId = Long.valueOf(expertId.toString());
            }
        } catch (NumberFormatException e) {
            return null;
        }

        final Long searchId = numericId;
        return experts.stream()
                .filter(e -> Objects.equals(e.getId(), searchId))
                .findFirst()
                .orElse(null);
    }
}
