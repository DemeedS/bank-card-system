package com.bank.card.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {

    private String fromCardMasked;
    private String toCardMasked;
    private BigDecimal amount;
    private BigDecimal fromCardNewBalance;
    private BigDecimal toCardNewBalance;
    private OffsetDateTime transferredAt;
    private String message;
}
