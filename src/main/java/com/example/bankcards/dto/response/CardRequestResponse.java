package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestResponse {
    private Long requestId;
    private Long cardId;
    private String cardMaskedNumber;
    private CardRequestStatus status;
    private LocalDateTime createdAt;
}