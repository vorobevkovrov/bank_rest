package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.SameCardTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional()
    public TransferResponse transferBetweenOwnCards(TransferRequest request, UserDetails currentUser) {
        log.info("Transfer from card {} to card {} amount {}",
                request.getFromCardId(), request.getToCardId(), request.getAmount());

        Card fromCard = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Source card not found"));
        Card toCard = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination card not found"));
        //TODO extract to validation
        if (!fromCard.getUser().getId().equals(toCard.getUser().getId())) {
            throw new RuntimeException("Cards must belong to the same owner");
        }

        if (!fromCard.getUser().getUsername().equals(currentUser.getUsername())) {
            throw new RuntimeException("You can only transfer between your own cards");
        }

        if (fromCard.getId().equals(toCard.getId())) {
            throw new SameCardTransferException("Cannot transfer to the same card");
        }

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        if (!fromCard.isActive() || fromCard.isBlocked()) {
            throw new RuntimeException("Source card is not active");
        }
        if (!toCard.isActive() || toCard.isBlocked()) {
            throw new RuntimeException("Destination card is not active");
        }
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
