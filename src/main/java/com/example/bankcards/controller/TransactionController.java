package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Управление транзакциями", description = "API для работы с финансовыми транзакциями")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Перевод между своими картами",
            description = "Перевод средств между картами, принадлежащими одному пользователю"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Перевод успешно выполнен"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к картам")
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/transfer/own")
    public ResponseEntity<TransferResponse> transferBetweenOwnCards(
            @Parameter(description = "Данные для перевода", required = true)
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("User {} transferring {} from card {} to card {}",
                userDetails.getUsername(), request.getAmount(),
                request.getFromCardId(), request.getToCardId());
        TransferResponse response = transactionService.transferBetweenOwnCards(request, userDetails);
        return ResponseEntity.ok(response);
    }

}