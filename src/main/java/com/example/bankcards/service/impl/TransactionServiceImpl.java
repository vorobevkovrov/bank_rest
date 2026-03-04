package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.exception.exceptions.InsufficientFundsException;
import com.example.bankcards.exception.exceptions.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.TransferValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final TransferValidator transferValidator;

    @Override
    @Transactional()
    public TransferResponse transferBetweenOwnCards(TransferRequest request, UserDetails currentUser) {
        log.info("Transfer from card {} to card {} amount {}",
                request.fromCardId(), request.toCardId(), request.amount());

        Card fromCard = cardRepository.findById(request.fromCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found"));
        Card toCard = cardRepository.findById(request.toCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found"));

        transferValidator.transferCardValidation(request, currentUser,
                fromCard, toCard);

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on card");
        }
        toCard.setBalance(toCard.getBalance().add(request.amount()));
        fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
        cardRepository.save(fromCard);
        cardRepository.save(toCard);
        Transaction transaction = Transaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.amount())
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transfer completed: transaction ID {}", savedTransaction.getId());

        return TransferResponse.builder()
                .transactionId(savedTransaction.getId())
                .fromCardId(fromCard.getId())
                .toCardId(toCard.getId())
                .amount(request.amount())
                .status(TransactionStatus.COMPLETED)
                .timestamp(savedTransaction.getCreatedAt())
                .message("Transfer completed successfully")
                .build();
    }
}
