package com.example.bankcards.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransferRequest(@NotNull(message = "Source card ID is required")
                              @Positive(message = "Source card ID must be positive")
                              Long fromCardId,

                              @NotNull(message = "Destination card ID is required")
                              @Positive(message = "Destination card ID must be positive")
                              Long toCardId,

                              @NotNull(message = "Amount is required")
                              @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
                              BigDecimal amount) {
}

