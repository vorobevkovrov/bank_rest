package com.example.bankcards.dto.request;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CardUpdateRequest {
    private Long id;
    private Date date;
    private Date expiryDate;
    private CardStatus status;
    private BigDecimal balance;
}
