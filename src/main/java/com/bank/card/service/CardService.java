package com.bank.card.service;

import com.bank.card.dto.request.CardCreateRequest;
import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.User;
import org.springframework.data.domain.Pageable;

public interface CardService {

    // Admin operations
    CardResponse createCard(CardCreateRequest request);
    CardResponse setCardStatus(Long cardId, CardStatus status);
    void deleteCard(Long cardId);
    PageResponse<CardResponse> getAllCards(CardStatus statusFilter, Pageable pageable);

    // User operations
    PageResponse<CardResponse> getMyCards(User currentUser, CardStatus statusFilter, Pageable pageable);
    CardResponse getMyCard(Long cardId, User currentUser);
    CardResponse requestBlock(Long cardId, User currentUser);
    CardResponse getCardById(Long cardId);
}
