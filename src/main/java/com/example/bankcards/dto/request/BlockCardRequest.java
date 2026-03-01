package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BlockCardRequest(@NotNull(message = "Card ID is required")
                               Long cardId) {

}