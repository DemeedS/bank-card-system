package com.bank.card.controller;

import com.bank.card.dto.request.TransferRequest;
import com.bank.card.dto.response.TransferResponse;
import com.bank.card.entity.User;
import com.bank.card.security.service.SecurityUtils;
import com.bank.card.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Transfer funds between your own cards")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Transfer funds between two of your own cards")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(transferService.transfer(request, currentUser));
    }
}
