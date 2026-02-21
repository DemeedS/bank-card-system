package com.bank.card.service.impl;

import com.bank.card.config.CardEncryptionService;
import com.bank.card.dto.request.CardCreateRequest;
import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.entity.Card;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.User;
import com.bank.card.exception.CardOperationException;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.mapper.CardMapper;
import com.bank.card.repository.CardRepository;
import com.bank.card.repository.UserRepository;
import com.bank.card.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptionService encryptionService;
    private final CardMapper cardMapper;

    // ─── Admin Operations ────────────────────────────────────────────────────

    @Override
    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getOwnerId()
                ));

        String encryptedNumber = encryptionService.encrypt(request.getCardNumber());
        String maskedNumber = encryptionService.mask(request.getCardNumber());

        // Determine initial status — expired if expiry date already passed
        CardStatus initialStatus = request.getExpiryDate().isBefore(LocalDate.now())
                ? CardStatus.EXPIRED
                : CardStatus.ACTIVE;

        Card card = Card.builder()
                .encryptedCardNumber(encryptedNumber)
                .maskedCardNumber(maskedNumber)
                .owner(owner)
                .cardholderName(request.getCardholderName())
                .expiryDate(request.getExpiryDate())
                .status(initialStatus)
                .balance(request.getInitialBalance())
                .build();

        Card saved = cardRepository.save(card);
        log.info("Card created with id={} for owner id={}", saved.getId(), owner.getId());
        return cardMapper.toCardResponse(saved);
    }

    @Override
    @Transactional
    public CardResponse setCardStatus(Long cardId, CardStatus status) {
        Card card = findCardOrThrow(cardId);

        // Cannot activate an expired card
        if (status == CardStatus.ACTIVE && card.isExpired()) {
            throw new CardOperationException(
                    "Cannot activate card id=" + cardId + " because it is expired"
            );
        }

        card.setStatus(status);
        Card saved = cardRepository.save(card);
        log.info("Card id={} status changed to {}", cardId, status);
        return cardMapper.toCardResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = findCardOrThrow(cardId);
        cardRepository.delete(card);
        log.info("Card id={} deleted", cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getAllCards(CardStatus statusFilter, Pageable pageable) {
        Page<Card> page;
        if (statusFilter != null) {
            page = cardRepository.findAll(
                    (root, query, cb) -> cb.equal(root.get("status"), statusFilter),
                    pageable
            );
        } else {
            page = cardRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(cardMapper::toCardResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        return cardMapper.toCardResponse(findCardOrThrow(cardId));
    }

    // ─── User Operations ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getMyCards(User currentUser, CardStatus statusFilter, Pageable pageable) {
        // Auto-expire cards whose expiry date has passed
        Page<Card> page;
        if (statusFilter != null) {
            page = cardRepository.findByOwnerIdAndStatus(currentUser.getId(), statusFilter, pageable);
        } else {
            page = cardRepository.findByOwnerId(currentUser.getId(), pageable);
        }

        return PageResponse.from(page.map(card -> {
            syncExpiredStatus(card);
            return cardMapper.toCardResponse(card);
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getMyCard(Long cardId, User currentUser) {
        Card card = cardRepository.findByIdAndOwnerId(cardId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card not found with id: " + cardId
                ));
        syncExpiredStatus(card);
        return cardMapper.toCardResponse(card);
    }

    @Override
    @Transactional
    public CardResponse requestBlock(Long cardId, User currentUser) {
        Card card = cardRepository.findByIdAndOwnerId(cardId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card not found with id: " + cardId
                ));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardOperationException("Card id=" + cardId + " is already blocked");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new CardOperationException("Card id=" + cardId + " is already expired and cannot be blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        Card saved = cardRepository.save(card);
        log.info("User id={} requested block on card id={}", currentUser.getId(), cardId);
        return cardMapper.toCardResponse(saved);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Card findCardOrThrow(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card not found with id: " + cardId
                ));
    }

    /**
     * If a card's expiry date has passed but status is still ACTIVE,
     * update it to EXPIRED on the fly and persist the change.
     */
    @Transactional
    protected void syncExpiredStatus(Card card) {
        if (card.getStatus() == CardStatus.ACTIVE && card.isExpired()) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
        }
    }
}
