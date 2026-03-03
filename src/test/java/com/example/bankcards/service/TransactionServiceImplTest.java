package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.service.impl.TransactionServiceImpl;
import com.example.bankcards.util.TransferValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransferValidator transferValidator;
    @Mock
    private UserDetails currentUser;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransferRequest request;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        request = new TransferRequest(1L, 2L, new BigDecimal("500.00"));

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setBalance(new BigDecimal("1000.00"));

        toCard = new Card();
        toCard.setId(2L);
        toCard.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void transferBetweenOwnCards_Success() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        Transaction savedTransaction = Transaction.builder()
                .id(100L)
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.amount())
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        TransferResponse response = transactionService.transferBetweenOwnCards(request, currentUser);

        // Assert
        assertAll(
                () -> assertEquals(new BigDecimal("500.00"), fromCard.getBalance(), "Баланс отправителя должен уменьшиться"),
                () -> assertEquals(new BigDecimal("600.00"), toCard.getBalance(), "Баланс получателя должен увеличиться"),
                () -> assertEquals(TransactionStatus.COMPLETED, response.status()),
                () -> verify(cardRepository, times(1)).save(fromCard),
                () -> verify(cardRepository, times(1)).save(toCard),
                () -> verify(transferValidator).transferCardValidation(eq(request), eq(currentUser), any(), any())
        );
    }

    @Test
    void transferBetweenOwnCards_InsufficientFunds_ThrowsException() {
        // Arrange
        fromCard.setBalance(new BigDecimal("100.00")); // Меньше чем 500 в реквесте
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(InsufficientFundsException.class,
                () -> transactionService.transferBetweenOwnCards(request, currentUser));

        verify(transactionRepository, never()).save(any());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_CardNotFound_ThrowsException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.transferBetweenOwnCards(request, currentUser));
    }
}
