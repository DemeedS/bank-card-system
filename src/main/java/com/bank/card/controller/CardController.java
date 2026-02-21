package com.bank.card.controller;

import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.User;
import com.bank.card.security.service.SecurityUtils;
import com.bank.card.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management for authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get my cards with optional status filter and pagination")
    public ResponseEntity<PageResponse<CardResponse>> getMyCards(
            @Parameter(description = "Filter by status: ACTIVE, BLOCKED, EXPIRED")
            @RequestParam(required = false) CardStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        User currentUser = securityUtils.getCurrentUser();
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(cardService.getMyCards(currentUser, status, pageable));
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Get a specific card by ID (must belong to current user)")
    public ResponseEntity<CardResponse> getMyCard(@PathVariable Long cardId) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(cardService.getMyCard(cardId, currentUser));
    }

    @PostMapping("/{cardId}/request-block")
    @Operation(summary = "Request to block one of your cards")
    public ResponseEntity<CardResponse> requestBlock(@PathVariable Long cardId) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(cardService.requestBlock(cardId, currentUser));
    }
}
