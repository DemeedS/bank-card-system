package com.bank.card.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardCreateRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(
        regexp = "^[0-9]{16}$",
        message = "Card number must be exactly 16 digits with no spaces"
    )
    private String cardNumber;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    @NotBlank(message = "Cardholder name is required")
    @Size(min = 2, max = 100, message = "Cardholder name must be between 2 and 100 characters")
    private String cardholderName;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Balance format is invalid")
    private BigDecimal initialBalance = BigDecimal.ZERO;
}
