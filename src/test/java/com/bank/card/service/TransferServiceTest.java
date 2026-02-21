package com.bank.card.service;

import com.bank.card.dto.request.TransferRequest;
import com.bank.card.dto.response.TransferResponse;
import com.bank.card.entity.Card;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.Role;
import com.bank.card.entity.User;
import com.bank.card.exception.CardOperationException;
import com.bank.card.exception.InsufficientFundsException;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.repository.CardRepository;
import com.bank.card.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Tests")
class TransferServiceTest {

    @Mock private CardRepository cardRepository;

    @InjectMocks
    private TransferServiceImpl transferService;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private TransferRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .enabled(true)
                .build();

        fromCard = Card.builder()
                .id(1L)
                .maskedCardNumber("**** **** **** 1111")
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .expiryDate(LocalDate.now().plusYears(2))
                .build();

        toCard = Card.builder()
                .id(2L)
                .maskedCardNumber("**** **** **** 2222")
                .owner(testUser)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .expiryDate(LocalDate.now().plusYears(2))
                .build();

        validRequest = new TransferRequest();
        validRequest.setFromCardId(1L);
        validRequest.setToCardId(2L);
        validRequest.setAmount(new BigDecimal("200.00"));
    }

    @Nested
    @DisplayName("Successful Transfer")
    class SuccessfulTransferTests {

        @Test
        @DisplayName("Should transfer funds and update both balances correctly")
        void shouldTransferSuccessfully() {
            when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));
            when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransferResponse response = transferService.transfer(validRequest, testUser);

            assertThat(response.getAmount()).isEqualByComparingTo("200.00");
            assertThat(response.getFromCardNewBalance()).isEqualByComparingTo("800.00");
            assertThat(response.getToCardNewBalance()).isEqualByComparingTo("700.00");
            assertThat(response.getFromCardMasked()).isEqualTo("**** **** **** 1111");
            assertThat(response.getToCardMasked()).isEqualTo("**** **** **** 2222");
            assertThat(response.getMessage()).contains("successfully");

            verify(cardRepository, times(2)).save(any(Card.class));
        }

        @Test
        @DisplayName("Should allow exact balance transfer (zero remaining)")
        void shouldAllowExactBalanceTransfer() {
            validRequest.setAmount(new BigDecimal("1000.00"));
            when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));
            when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TransferResponse response = transferService.transfer(validRequest, testUser);

            assertThat(response.getFromCardNewBalance()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Transfer Validation Failures")
    class TransferValidationTests {

        @Test
        @DisplayName("Should throw CardOperationException when transferring to same card")
        void shouldThrowOnSameCardTransfer() {
            validRequest.setToCardId(1L); // same as fromCardId

            assertThatThrownBy(() -> transferService.transfer(validRequest, testUser))
                    .isInstanceOf(CardOperationException.class)
                    .hasMessageContaining("different");

            verify(cardRepository, never()).findByIdAndOwnerId(any(), any());
        }

        @Test
        @DisplayName("Should throw InsufficientFundsException when balance too low")
        void shouldThrowOnInsufficientFunds() {
            validRequest.setAmount(new BigDecimal("9999.00"));
            when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

            assertThatThrownBy(() -> transferService.transfer(validRequest, testUser))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds");

            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardOperationException when source card is BLOCKED")
        void shouldThrowWhenSourceCardBlocked() {
            fromCard.setStatus(CardStatus.BLOCKED);
            when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

            assertThatThrownBy(() -> transferService.transfer(validRequest, testUser))
                    .isInstanceOf(CardOperationException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Should throw CardOperationException when destination card is BLOCKED")
        void shouldThrowWhenDestinationCardBlocked() {
            toCard.setStatus(CardStatus.BLOCKED);
            when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(fromCard));
            when(cardRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(toCard));

            assertThatThrownBy(() -> transferService.transfer(validRequest, testUser))
                    .isInstanceOf(CardOperationException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card does not belong to user")
        void shouldThrowWhenCardNotOwned() {
            when(cardRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferService.transfer(validRequest, testUser))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Source card");
        }
    }
}
