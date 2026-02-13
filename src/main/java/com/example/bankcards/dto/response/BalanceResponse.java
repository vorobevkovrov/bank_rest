package com.example.bankcards.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BalanceResponse(String cardNumber,
                              String holderName,
                              BigDecimal balance) {

}
