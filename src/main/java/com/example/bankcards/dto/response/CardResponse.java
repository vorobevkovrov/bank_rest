package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CardResponse {
    private String maskedNumber;
    private String holderName;
    private Date expiryDate;
    private CardStatus status;
    private BigDecimal balance;
    private Long userId;
}