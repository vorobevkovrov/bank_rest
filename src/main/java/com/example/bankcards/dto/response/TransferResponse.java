package com.example.bankcards.dto.response;

import com.example.bankcards.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private Long transactionId;
    private Long fromCardId;
    private Long toCardId;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private String message;
}