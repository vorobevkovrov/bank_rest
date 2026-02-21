package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.exception.ResourceNotFoundException;
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
                request.getFromCardId(), request.getToCardId(), request.getAmount());

        Card fromCard = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found"));
        Card toCard = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found"));

        transferValidator.transferCardValidation(request, currentUser,
                fromCard, toCard);

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));
        cardRepository.save(fromCard);
        cardRepository.save(toCard);
        Transaction transaction = Transaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transfer completed: transaction ID {}", savedTransaction.getId());

        return TransferResponse.builder()
                .transactionId(savedTransaction.getId())
                .fromCardId(fromCard.getId())
                .toCardId(toCard.getId())
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .timestamp(savedTransaction.getCreatedAt())
                .message("Transfer completed successfully")
                .build();
    }
}
