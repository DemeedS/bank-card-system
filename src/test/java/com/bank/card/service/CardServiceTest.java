package com.bank.card.service;

import com.bank.card.config.CardEncryptionService;
import com.bank.card.dto.request.CardCreateRequest;
import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.entity.Card;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.Role;
import com.bank.card.entity.User;
import com.bank.card.exception.CardOperationException;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.mapper.CardMapper;
import com.bank.card.repository.CardRepository;
import com.bank.card.repository.UserRepository;
import com.bank.card.service.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService Tests")
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardEncryptionService encryptionService;
    @Mock private CardMapper cardMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    private User testUser;
    private Card activeCard;
    private CardResponse activeCardResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .enabled(true)
                .build();

        activeCard = Card.builder()
                .id(10L)
                .encryptedCardNumber("encrypted123")
                .maskedCardNumber("**** **** **** 1234")
                .owner(testUser)
                .cardholderName("Test User")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        activeCardResponse = CardResponse.builder()
                .id(10L)
                .maskedCardNumber("**** **** **** 1234")
                .ownerId(1L)
                .ownerUsername("testuser")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    @Nested
    @DisplayName("Create Card")
    class CreateCardTests {

        private CardCreateRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = new CardCreateRequest();
            createRequest.setCardNumber("1234567890123456");
            createRequest.setOwnerId(1L);
            createRequest.setCardholderName("Test User");
            createRequest.setExpiryDate(LocalDate.now().plusYears(2));
            createRequest.setInitialBalance(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should create card successfully with ACTIVE status")
        void shouldCreateCard() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(encryptionService.encrypt("1234567890123456")).thenReturn("encrypted");
            when(encryptionService.mask("1234567890123456")).thenReturn("**** **** **** 3456");
            when(cardRepository.save(any(Card.class))).thenReturn(activeCard);
            when(cardMapper.toCardResponse(activeCard)).thenReturn(activeCardResponse);

            CardResponse response = cardService.createCard(createRequest);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
            verify(encryptionService).encrypt("1234567890123456");
            verify(encryptionService).mask("1234567890123456");
            verify(cardRepository).save(argThat(card ->
                    card.getEncryptedCardNumber().equals("encrypted") &&
                    card.getMaskedCardNumber().equals("**** **** **** 3456") &&
                    card.getBalance().equals(new BigDecimal("500.00"))
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when owner not found")
        void shouldThrowWhenOwnerNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            createRequest.setOwnerId(99L);

            assertThatThrownBy(() -> cardService.createCard(createRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set EXPIRED status when expiry date is in the past")
        void shouldSetExpiredStatusForPastExpiryDate() {
            createRequest.setExpiryDate(LocalDate.now().minusDays(1));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(encryptionService.encrypt(any())).thenReturn("enc");
            when(encryptionService.mask(any())).thenReturn("**** **** **** 3456");
            when(cardRepository.save(any(Card.class))).thenAnswer(i -> i.getArgument(0));
            when(cardMapper.toCardResponse(any())).thenReturn(activeCardResponse);

            cardService.createCard(createRequest);

            verify(cardRepository).save(argThat(card ->
                    card.getStatus() == CardStatus.EXPIRED
            ));
        }
    }

    @Nested
    @DisplayName("Set Card Status")
    class SetCardStatusTests {

        @Test
        @DisplayName("Should block an active card")
        void shouldBlockActiveCard() {
            when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenReturn(activeCard);
            when(cardMapper.toCardResponse(any())).thenReturn(activeCardResponse);

            cardService.setCardStatus(10L, CardStatus.BLOCKED);

            verify(cardRepository).save(argThat(card ->
                    card.getStatus() == CardStatus.BLOCKED
            ));
        }

        @Test
        @DisplayName("Should throw CardOperationException when activating an expired card")
        void shouldThrowWhenActivatingExpiredCard() {
            activeCard.setExpiryDate(LocalDate.now().minusDays(1));
            activeCard.setStatus(CardStatus.EXPIRED);
            when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));

            assertThatThrownBy(() -> cardService.setCardStatus(10L, CardStatus.ACTIVE))
                    .isInstanceOf(CardOperationException.class)
                    .hasMessageContaining("expired");

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown card")
        void shouldThrowForUnknownCard() {
            when(cardRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.setCardStatus(999L, CardStatus.BLOCKED))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get My Cards")
    class GetMyCardsTests {

        @Test
        @DisplayName("Should return paginated cards for current user")
        void shouldReturnMyCards() {
            Page<Card> cardPage = new PageImpl<>(List.of(activeCard), PageRequest.of(0, 10), 1);
            when(cardRepository.findByOwnerId(eq(1L), any(Pageable.class))).thenReturn(cardPage);
            when(cardMapper.toCardResponse(activeCard)).thenReturn(activeCardResponse);

            PageResponse<CardResponse> result = cardService.getMyCards(testUser, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should filter by status when provided")
        void shouldFilterByStatus() {
            Page<Card> cardPage = new PageImpl<>(List.of(activeCard));
            when(cardRepository.findByOwnerIdAndStatus(eq(1L), eq(CardStatus.ACTIVE), any()))
                    .thenReturn(cardPage);
            when(cardMapper.toCardResponse(any())).thenReturn(activeCardResponse);

            cardService.getMyCards(testUser, CardStatus.ACTIVE, PageRequest.of(0, 10));

            verify(cardRepository).findByOwnerIdAndStatus(eq(1L), eq(CardStatus.ACTIVE), any());
            verify(cardRepository, never()).findByOwnerId(any(), any());
        }
    }

    @Nested
    @DisplayName("Request Block")
    class RequestBlockTests {

        @Test
        @DisplayName("Should block card when it is ACTIVE")
        void shouldBlockActiveCard() {
            when(cardRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(activeCard));
            when(cardRepository.save(any())).thenReturn(activeCard);
            when(cardMapper.toCardResponse(any())).thenReturn(activeCardResponse);

            cardService.requestBlock(10L, testUser);

            verify(cardRepository).save(argThat(card ->
                    card.getStatus() == CardStatus.BLOCKED
            ));
        }

        @Test
        @DisplayName("Should throw CardOperationException if card already blocked")
        void shouldThrowIfAlreadyBlocked() {
            activeCard.setStatus(CardStatus.BLOCKED);
            when(cardRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(activeCard));

            assertThatThrownBy(() -> cardService.requestBlock(10L, testUser))
                    .isInstanceOf(CardOperationException.class)
                    .hasMessageContaining("already blocked");

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardOperationException if card is EXPIRED")
        void shouldThrowIfExpired() {
            activeCard.setStatus(CardStatus.EXPIRED);
            when(cardRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(activeCard));

            assertThatThrownBy(() -> cardService.requestBlock(10L, testUser))
                    .isInstanceOf(CardOperationException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if card does not belong to user")
        void shouldThrowIfCardNotOwnedByUser() {
            when(cardRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.requestBlock(10L, testUser))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
