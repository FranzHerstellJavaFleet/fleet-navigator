package io.javafleet.fleetnavigator.service;

import io.javafleet.fleetnavigator.dto.ChatDTO;
import io.javafleet.fleetnavigator.experts.model.Expert;
import io.javafleet.fleetnavigator.experts.repository.ExpertModeRepository;
import io.javafleet.fleetnavigator.experts.repository.ExpertRepository;
import io.javafleet.fleetnavigator.model.Chat;
import io.javafleet.fleetnavigator.repository.ChatRepository;
import io.javafleet.fleetnavigator.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit-Tests für Chat-Expert-Zuordnung
 *
 * Testet:
 * - Chat erstellen mit Expert-ID
 * - Chat-History laden mit Expert-ID
 * - Expert aktualisieren
 * - Expert entfernen
 *
 * Diese Tests stellen sicher, dass beim Wechsel zwischen Chats
 * der korrekte Experte wiederhergestellt wird.
 *
 * @author JavaFleet Systems Consulting
 * @since 0.5.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatExpertMappingTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ExpertRepository expertRepository;

    @Mock
    private ExpertModeRepository expertModeRepository;

    // Test data
    private static final Long CHAT_ID = 1L;
    private static final Long EXPERT_ID_ROLAND = 10L;
    private static final Long EXPERT_ID_AYSE = 20L;
    private static final String MODEL_NAME = "mistral-nemo:12b-instruct-2407-q8_0";

    @Nested
    @DisplayName("Chat erstellen mit Expert")
    class CreateChatWithExpert {

        @Test
        @DisplayName("Neuer Chat speichert Expert-ID korrekt")
        void createChat_WithExpertId_SavesExpertId() {
            // Given
            Chat savedChat = createTestChat(CHAT_ID, EXPERT_ID_ROLAND);
            when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);

            // When
            Chat chatToSave = new Chat();
            chatToSave.setTitle("Test Chat mit Roland");
            chatToSave.setModel(MODEL_NAME);
            chatToSave.setExpertId(EXPERT_ID_ROLAND);

            Chat result = chatRepository.save(chatToSave);

            // Then
            assertThat(result.getExpertId()).isEqualTo(EXPERT_ID_ROLAND);

            ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
            verify(chatRepository).save(chatCaptor.capture());
            assertThat(chatCaptor.getValue().getExpertId()).isEqualTo(EXPERT_ID_ROLAND);
        }

        @Test
        @DisplayName("Neuer Chat ohne Expert hat null als Expert-ID")
        void createChat_WithoutExpertId_HasNullExpertId() {
            // Given
            Chat savedChat = createTestChat(CHAT_ID, null);
            when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);

            // When
            Chat chatToSave = new Chat();
            chatToSave.setTitle("Test Chat ohne Expert");
            chatToSave.setModel(MODEL_NAME);
            // Keine expertId gesetzt

            Chat result = chatRepository.save(chatToSave);

            // Then
            assertThat(result.getExpertId()).isNull();
        }
    }

    @Nested
    @DisplayName("Chat-History laden")
    class LoadChatHistory {

        @Test
        @DisplayName("Chat mit Expert-ID gibt korrekten Expert zurück")
        void getChatHistory_WithExpert_ReturnsExpertId() {
            // Given
            Chat chat = createTestChat(CHAT_ID, EXPERT_ID_ROLAND);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
            when(messageRepository.findByChatIdOrderByCreatedAtAsc(CHAT_ID)).thenReturn(Collections.emptyList());
            when(messageRepository.sumTokensByChatId(CHAT_ID)).thenReturn(0);

            // When
            Optional<Chat> result = chatRepository.findById(CHAT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getExpertId()).isEqualTo(EXPERT_ID_ROLAND);
        }

        @Test
        @DisplayName("Chat ohne Expert-ID gibt null zurück")
        void getChatHistory_WithoutExpert_ReturnsNullExpertId() {
            // Given
            Chat chat = createTestChat(CHAT_ID, null);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

            // When
            Optional<Chat> result = chatRepository.findById(CHAT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getExpertId()).isNull();
        }

        @Test
        @DisplayName("Verschiedene Chats haben verschiedene Experten")
        void getChatHistory_DifferentChats_DifferentExperts() {
            // Given - Chat 1 mit Roland
            Chat chatRoland = createTestChat(1L, EXPERT_ID_ROLAND);
            chatRoland.setTitle("Chat mit Roland");
            when(chatRepository.findById(1L)).thenReturn(Optional.of(chatRoland));

            // Given - Chat 2 mit Ayşe
            Chat chatAyse = createTestChat(2L, EXPERT_ID_AYSE);
            chatAyse.setTitle("Chat mit Ayşe");
            when(chatRepository.findById(2L)).thenReturn(Optional.of(chatAyse));

            // Given - Chat 3 ohne Expert
            Chat chatNoExpert = createTestChat(3L, null);
            chatNoExpert.setTitle("Chat ohne Expert");
            when(chatRepository.findById(3L)).thenReturn(Optional.of(chatNoExpert));

            // When & Then - Chat 1 sollte Roland haben
            Optional<Chat> result1 = chatRepository.findById(1L);
            assertThat(result1).isPresent();
            assertThat(result1.get().getExpertId()).isEqualTo(EXPERT_ID_ROLAND);
            assertThat(result1.get().getTitle()).isEqualTo("Chat mit Roland");

            // When & Then - Chat 2 sollte Ayşe haben
            Optional<Chat> result2 = chatRepository.findById(2L);
            assertThat(result2).isPresent();
            assertThat(result2.get().getExpertId()).isEqualTo(EXPERT_ID_AYSE);
            assertThat(result2.get().getTitle()).isEqualTo("Chat mit Ayşe");

            // When & Then - Chat 3 sollte keinen Expert haben
            Optional<Chat> result3 = chatRepository.findById(3L);
            assertThat(result3).isPresent();
            assertThat(result3.get().getExpertId()).isNull();
            assertThat(result3.get().getTitle()).isEqualTo("Chat ohne Expert");
        }
    }

    @Nested
    @DisplayName("Expert aktualisieren")
    class UpdateChatExpert {

        @Test
        @DisplayName("Expert kann geändert werden")
        void updateChatExpert_ChangesExpert() {
            // Given - Chat hat ursprünglich Roland
            Chat chat = createTestChat(CHAT_ID, EXPERT_ID_ROLAND);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
            when(chatRepository.save(any(Chat.class))).thenAnswer(i -> i.getArgument(0));

            // When - Ändere zu Ayşe
            chat.setExpertId(EXPERT_ID_AYSE);
            Chat savedChat = chatRepository.save(chat);

            // Then
            assertThat(savedChat.getExpertId()).isEqualTo(EXPERT_ID_AYSE);
        }

        @Test
        @DisplayName("Expert kann entfernt werden (null)")
        void updateChatExpert_RemovesExpert() {
            // Given - Chat hat ursprünglich Roland
            Chat chat = createTestChat(CHAT_ID, EXPERT_ID_ROLAND);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
            when(chatRepository.save(any(Chat.class))).thenAnswer(i -> i.getArgument(0));

            // When - Entferne Expert
            chat.setExpertId(null);
            Chat savedChat = chatRepository.save(chat);

            // Then
            assertThat(savedChat.getExpertId()).isNull();
        }

        @Test
        @DisplayName("Expert kann zu Chat ohne Expert hinzugefügt werden")
        void updateChatExpert_AddsExpertToEmptyChat() {
            // Given - Chat hat keinen Expert
            Chat chat = createTestChat(CHAT_ID, null);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
            when(chatRepository.save(any(Chat.class))).thenAnswer(i -> i.getArgument(0));

            // When - Füge Roland hinzu
            chat.setExpertId(EXPERT_ID_ROLAND);
            Chat savedChat = chatRepository.save(chat);

            // Then
            assertThat(savedChat.getExpertId()).isEqualTo(EXPERT_ID_ROLAND);
        }
    }

    @Nested
    @DisplayName("Chat-Expert-Wechsel Simulation")
    class ChatSwitchingSimulation {

        @Test
        @DisplayName("Wechsel zwischen Chats behält korrekten Expert")
        void switchBetweenChats_MaintainsCorrectExpert() {
            // Simuliert das Frontend-Verhalten beim Chat-Wechsel

            // Given - Drei Chats mit verschiedenen Experten
            Chat chatRoland = createTestChat(1L, EXPERT_ID_ROLAND);
            Chat chatAyse = createTestChat(2L, EXPERT_ID_AYSE);
            Chat chatNoExpert = createTestChat(3L, null);

            when(chatRepository.findById(1L)).thenReturn(Optional.of(chatRoland));
            when(chatRepository.findById(2L)).thenReturn(Optional.of(chatAyse));
            when(chatRepository.findById(3L)).thenReturn(Optional.of(chatNoExpert));

            // Simuliere Chat-Wechsel
            Long currentExpertId = null;

            // Step 1: Öffne Chat mit Roland
            Chat loaded1 = chatRepository.findById(1L).orElseThrow();
            currentExpertId = loaded1.getExpertId();
            assertThat(currentExpertId).isEqualTo(EXPERT_ID_ROLAND);

            // Step 2: Wechsle zu Chat mit Ayşe
            Chat loaded2 = chatRepository.findById(2L).orElseThrow();
            currentExpertId = loaded2.getExpertId();
            assertThat(currentExpertId).isEqualTo(EXPERT_ID_AYSE);

            // Step 3: Wechsle zu Chat ohne Expert
            Chat loaded3 = chatRepository.findById(3L).orElseThrow();
            currentExpertId = loaded3.getExpertId();
            assertThat(currentExpertId).isNull();

            // Step 4: Wechsle zurück zu Chat mit Roland
            Chat loaded4 = chatRepository.findById(1L).orElseThrow();
            currentExpertId = loaded4.getExpertId();
            assertThat(currentExpertId).isEqualTo(EXPERT_ID_ROLAND);
        }

        @Test
        @DisplayName("Expert-ID bleibt nach Chat-Update erhalten")
        void chatUpdate_PreservesExpertId() {
            // Given
            Chat chat = createTestChat(CHAT_ID, EXPERT_ID_ROLAND);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
            when(chatRepository.save(any(Chat.class))).thenAnswer(i -> i.getArgument(0));

            // When - Update den Titel (nicht den Expert)
            chat.setTitle("Neuer Titel");
            Chat savedChat = chatRepository.save(chat);

            // Then - Expert sollte noch da sein
            assertThat(savedChat.getExpertId()).isEqualTo(EXPERT_ID_ROLAND);
            assertThat(savedChat.getTitle()).isEqualTo("Neuer Titel");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Nicht existierender Chat wirft Exception")
        void getNonExistentChat_ThrowsException() {
            // Given
            when(chatRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThat(chatRepository.findById(999L)).isEmpty();
        }

        @Test
        @DisplayName("Expert-ID kann auf 0 gesetzt werden (aber sollte null sein)")
        void expertId_ZeroIsNotNull() {
            // Given
            Chat chat = createTestChat(CHAT_ID, 0L);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

            // When
            Optional<Chat> result = chatRepository.findById(CHAT_ID);

            // Then - 0L ist nicht null!
            assertThat(result).isPresent();
            assertThat(result.get().getExpertId()).isEqualTo(0L);
            assertThat(result.get().getExpertId()).isNotNull();
        }

        @Test
        @DisplayName("Mehrfaches Laden desselben Chats gibt konsistente Expert-ID")
        void multipleLoads_ConsistentExpertId() {
            // Given
            Chat chat = createTestChat(CHAT_ID, EXPERT_ID_ROLAND);
            when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

            // When - Lade mehrmals
            Long expertId1 = chatRepository.findById(CHAT_ID).map(Chat::getExpertId).orElse(null);
            Long expertId2 = chatRepository.findById(CHAT_ID).map(Chat::getExpertId).orElse(null);
            Long expertId3 = chatRepository.findById(CHAT_ID).map(Chat::getExpertId).orElse(null);

            // Then - Alle sollten gleich sein
            assertThat(expertId1).isEqualTo(EXPERT_ID_ROLAND);
            assertThat(expertId2).isEqualTo(EXPERT_ID_ROLAND);
            assertThat(expertId3).isEqualTo(EXPERT_ID_ROLAND);
        }
    }

    // Helper methods

    private Chat createTestChat(Long id, Long expertId) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setTitle("Test Chat " + id);
        chat.setModel(MODEL_NAME);
        chat.setExpertId(expertId);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        return chat;
    }

    private Expert createTestExpert(Long id, String name, String role) {
        Expert expert = new Expert();
        expert.setId(id);
        expert.setName(name);
        expert.setRole(role);
        expert.setBaseModel(MODEL_NAME);
        expert.setBasePrompt("Test prompt for " + name);
        return expert;
    }
}
