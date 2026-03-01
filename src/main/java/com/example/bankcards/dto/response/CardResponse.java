package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;

@Builder
public record CardResponse(String maskedNumber,
                           String holderName,
                           Date expiryDate,
                           CardStatus status,
                           BigDecimal balance,
                           Long userId) {

}