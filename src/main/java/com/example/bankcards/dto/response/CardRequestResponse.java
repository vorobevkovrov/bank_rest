package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardRequestStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CardRequestResponse(Long requestId,
                                  Long cardId,
                                  String cardMaskedNumber,
                                  CardRequestStatus status,
                                  LocalDateTime createdAt) {

}