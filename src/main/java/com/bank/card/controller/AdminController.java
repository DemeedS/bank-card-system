package com.bank.card.controller;

import com.bank.card.dto.request.CardCreateRequest;
import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.CardStatus;
import com.bank.card.service.CardService;
import com.bank.card.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only operations for cards and users")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final CardService cardService;
    private final UserService userService;

    // ─── Card Management ─────────────────────────────────────────────────────

    @PostMapping("/cards")
    @Operation(summary = "Create a new card for a user")
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CardCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @GetMapping("/cards")
    @Operation(summary = "Get all cards with optional status filter and pagination")
    public ResponseEntity<PageResponse<CardResponse>> getAllCards(
            @Parameter(description = "Filter by status: ACTIVE, BLOCKED, EXPIRED")
            @RequestParam(required = false) CardStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(cardService.getAllCards(status, pageable));
    }

    @GetMapping("/cards/{cardId}")
    @Operation(summary = "Get any card by ID")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @PatchMapping("/cards/{cardId}/status")
    @Operation(summary = "Set card status (ACTIVE, BLOCKED, EXPIRED)")
    public ResponseEntity<CardResponse> setCardStatus(
            @PathVariable Long cardId,
            @Parameter(description = "New status") @RequestParam CardStatus status
    ) {
        return ResponseEntity.ok(cardService.setCardStatus(cardId, status));
    }

    @DeleteMapping("/cards/{cardId}")
    @Operation(summary = "Delete a card permanently")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    // ─── User Management ─────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PatchMapping("/users/{userId}/enable")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<UserResponse> toggleUserEnabled(
            @PathVariable Long userId,
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(userService.toggleUserEnabled(userId, enabled));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete a user and all their cards")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
