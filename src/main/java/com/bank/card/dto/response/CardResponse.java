package com.bank.card.dto.response;

import com.bank.card.entity.CardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardResponse {

    private Long id;

    /**
     * Always masked: **** **** **** 1234
     * Raw card number is NEVER returned in any response.
     */
    private String maskedCardNumber;

    private Long ownerId;
    private String ownerUsername;
    private String cardholderName;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
