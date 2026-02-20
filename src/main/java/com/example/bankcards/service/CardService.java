package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

public interface CardService {

    // Административные методы
    CardResponse createCard(CardCreateRequest request);

    CardResponse updateCard(Long cardId, CardUpdateRequest request);

    void deleteCard(Long cardId);

    CardResponse blockCard(Long cardId);

    CardResponse activateCard(Long cardId);

    BalanceResponse getCardBalance(Long cardId, UserDetails currentUser);

    // Просмотр карт (для админа и пользователя)
    CardResponse getCardById(Long cardId);

    Page<CardResponse> getAllCards(Pageable pageable);

    Page<CardResponse> getCardsByUserId(Long userId, Pageable pageable);

    Page<CardResponse> getCardsByUserName(String userName, Pageable pageable);

    // Пользовательские методы
    Page<CardResponse> getUserCards(Long userId, Pageable pageable);

    CardResponse getUserCardById(Long userId, Long cardId);
}

