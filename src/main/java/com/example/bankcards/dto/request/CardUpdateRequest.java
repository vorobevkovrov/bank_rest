package com.example.bankcards.dto.request;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Date;

public record CardUpdateRequest(Long id,
                                Date date,
                                @Future(message = "Expiry date must be in the future") Date expiryDate,
                                CardStatus status,
                                @Positive(message = "Balance must be positive") BigDecimal balance) {
}
