package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface TransactionService {
    TransferResponse transferBetweenOwnCards(TransferRequest request, UserDetails currentUser);
}
