package com.example.bankcards.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BalanceResponse {
    private String cardNumber;
    private String holderName;
    private BigDecimal balance;
}
