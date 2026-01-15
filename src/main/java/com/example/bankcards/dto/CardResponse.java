package com.example.bankcards.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardResponse {
    private Long id;
    // "**** **** **** 1234"
    private String maskedNumber;
    private String holderName;
    private LocalDate expiryDate;
    private String status;
    private BigDecimal balance;
}