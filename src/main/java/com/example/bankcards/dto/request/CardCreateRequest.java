package com.example.bankcards.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class CardCreateRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotBlank(message = "Card holder name is required")
    @Size(max = 100, message = "Card holder name must be less than 100 characters")
    private String cardHolderName;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private Date expiryDate;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @DecimalMax(value = "1000000.00", message = "Balance cannot exceed 1,000,000")
    private BigDecimal initialBalance;

//    private String cardNumberEncrypted;
//
//    private String cardNumberLastFour;
//
//    private CardStatus cardStatus;
}

