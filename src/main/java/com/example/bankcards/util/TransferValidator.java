package com.example.bankcards.util;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import org.springframework.security.core.userdetails.UserDetails;

public interface TransferValidator {
    void transferCardValidation(TransferRequest request, UserDetails currentUser,
                                Card fromCard, Card toCard);
}
