package com.example.bankcards.dto.response;

import com.example.bankcards.entity.TransactionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransferResponse(Long transactionId,
                               Long fromCardId,
                               Long toCardId,
                               BigDecimal amount,
                               TransactionStatus status,
                               LocalDateTime timestamp,
                               String message) {
}