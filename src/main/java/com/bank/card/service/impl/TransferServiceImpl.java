package com.bank.card.service.impl;

import com.bank.card.dto.request.TransferRequest;
import com.bank.card.dto.response.TransferResponse;
import com.bank.card.entity.Card;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.User;
import com.bank.card.exception.CardOperationException;
import com.bank.card.exception.InsufficientFundsException;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.repository.CardRepository;
import com.bank.card.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final CardRepository cardRepository;

    @Override
    @Transactional
    public TransferResponse transfer(TransferRequest request, User currentUser) {
        // 1. Prevent self-transfer
        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new CardOperationException("Source and destination cards must be different");
        }

        // 2. Load both cards with pessimistic locks acquired in sorted ID order to prevent deadlocks
        Long firstId  = Math.min(request.getFromCardId(), request.getToCardId());
        Long secondId = Math.max(request.getFromCardId(), request.getToCardId());

        Card firstCard = cardRepository.findByIdAndOwnerIdForUpdate(firstId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + firstId));
        Card secondCard = cardRepository.findByIdAndOwnerIdForUpdate(secondId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + secondId));

        Card fromCard = firstId.equals(request.getFromCardId()) ? firstCard : secondCard;
        Card toCard   = firstId.equals(request.getToCardId())   ? firstCard : secondCard;

        // 3. Validate source card is ACTIVE
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException(
                    "Source card is not active. Current status: " + fromCard.getStatus()
            );
        }

        // 4. Validate destination card is ACTIVE
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException(
                    "Destination card is not active. Current status: " + toCard.getStatus()
            );
        }

        // 5. Check sufficient funds
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + fromCard.getBalance()
                    + ", requested: " + request.getAmount()
            );
        }

        // 6. Execute transfer atomically
        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        log.info("Transfer completed: {} -> {} amount={} user={}",
                fromCard.getMaskedCardNumber(),
                toCard.getMaskedCardNumber(),
                request.getAmount(),
                currentUser.getUsername()
        );

        return TransferResponse.builder()
                .fromCardMasked(fromCard.getMaskedCardNumber())
                .toCardMasked(toCard.getMaskedCardNumber())
                .amount(request.getAmount())
                .fromCardNewBalance(fromCard.getBalance())
                .toCardNewBalance(toCard.getBalance())
                .transferredAt(OffsetDateTime.now())
                .message("Transfer completed successfully")
                .build();
    }
}
