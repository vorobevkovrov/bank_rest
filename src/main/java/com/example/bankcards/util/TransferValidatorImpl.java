package com.example.bankcards.util;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class TransferValidatorImpl implements TransferValidator {

    public void transferCardValidation(TransferRequest request, UserDetails currentUser,
                                       Card fromCard, Card toCard) {

        validateSameOwner(fromCard, toCard);
        validateCardOwnership(fromCard, currentUser);
        validateNotSameCard(fromCard, toCard);
        validateSufficientFunds(fromCard, request.getAmount());
        validateCardActive(fromCard, "source");
        validateCardActive(toCard, "destination");
    }

    private void validateSameOwner(Card fromCard, Card toCard) {
        Long fromUserId = fromCard.getUser().getId();
        Long toUserId = toCard.getUser().getId();

        if (!fromUserId.equals(toUserId)) {
            log.warn("Transfer validation failed: cards belong to different users ({} and {})",
                    fromUserId, toUserId);
            throw new TransferValidationException(
                    String.format("Cards must belong to the same owner. Card1 user: %d, Card2 user: %d",
                            fromUserId, toUserId));
        }
    }

    private void validateCardOwnership(Card card, UserDetails currentUser) {
        String cardOwnerUsername = card.getUser().getUsername();
        String currentUsername = currentUser.getUsername();

        if (!cardOwnerUsername.equals(currentUsername)) {
            log.warn("Unauthorized access attempt: {} trying to access card of {}",
                    currentUsername, cardOwnerUsername);
            throw new UnauthorizedCardAccessException(
                    String.format("You don't have permission to access card %d", card.getId()));
        }
    }

    private void validateNotSameCard(Card fromCard, Card toCard) {
        if (fromCard.getId().equals(toCard.getId())) {
            log.warn("Attempt to transfer to the same card: {}", fromCard.getId());
            throw new SameCardTransferException(
                    String.format("Cannot transfer from card %d to itself", fromCard.getId()));
        }
    }

    private void validateSufficientFunds(Card card, BigDecimal amount) {
        BigDecimal availableBalance = card.getBalance();

        if (availableBalance.compareTo(amount) < 0) {
            log.warn("Insufficient funds: card={}, available={}, required={}",
                    card.getId(), availableBalance, amount);
            throw new InsufficientFundsException(
                    String.format("Insufficient funds on card %d. Available: %s, Required: %s",
                            card.getId(), availableBalance, amount));
        }
    }

    private void validateCardActive(Card card, String cardType) {
        if (!card.isActive()) {
            log.warn("{} card {} is not active", cardType, card.getId());
            throw new CardNotActiveException(
                    String.format("%s card %d is not active", capitalize(cardType), card.getId()));
        }

        if (card.isBlocked()) {
            log.warn("{} card {} is blocked", cardType, card.getId());
            throw new CardBlockedException(
                    String.format("%s card %d is blocked", capitalize(cardType), card.getId()));
        }
    }


    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
