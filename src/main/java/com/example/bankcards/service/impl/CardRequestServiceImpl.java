package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.CardNotActiveException;
import com.example.bankcards.exception.DuplicateRequestException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.SameCardTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardRequestRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardRequestService;
import com.example.bankcards.util.CardRequestMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardRequestServiceImpl implements CardRequestService {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardRequestRepository cardRequestRepository;
    private final CardRequestMapper cardRequestMapper;

    public CardRequestResponse requestCardBlock(BlockCardRequest request, String username) {
        log.info("User {} requesting block for card {}", username, request.getCardId());

        Card card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!card.getUser().getId().equals(user.getId())) {
            throw new SameCardTransferException("This is not your card");
        }

        if (card.isBlocked()) {
            throw new CardNotActiveException("Card is already blocked");
        }

        boolean hasPendingRequest = cardRequestRepository.existsByCardAndStatus(
                card, CardRequestStatus.PENDING);

        if (hasPendingRequest) {
            throw new DuplicateRequestException("There is already a pending request for this card");
        }

        CardRequest cardRequest = CardRequest.builder()
                .card(card)
                .user(user)
                .requestType(CardRequestType.BLOCK)
                .status(CardRequestStatus.PENDING)
                .build();

        CardRequest savedRequest = cardRequestRepository.save(cardRequest);

        log.info("Block request created: ID={}, Card={}, User={}",
                savedRequest.getId(), card.getId(), user.getId());

        return CardRequestResponse.builder()
                .requestId(savedRequest.getId())
                .cardId(card.getId())
                .cardMaskedNumber("**** **** **** " + card.getCardNumberLastFour())
                .status(CardRequestStatus.PENDING)
                .createdAt(savedRequest.getCreatedAt())
                .build();
    }

    @Transactional
    public CardRequestResponse approveBlockRequest(Long requestId, String adminUsername) {
        log.info("Admin {} approving block request {}", adminUsername, requestId);

        CardRequest cardRequest = cardRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (cardRequest.getStatus() != CardRequestStatus.PENDING) {
            throw new RuntimeException("Request is not pending");
        }
        cardRequest.setStatus(CardRequestStatus.APPROVED);

        Card card = cardRequest.getCard();
        card.setStatus(CardStatus.BLOCKED);
        cardRequestRepository.save(cardRequest);
        cardRepository.save(card);

        log.info("Block request approved: Request={}, Card={}", requestId, card.getId());

        return CardRequestResponse.builder()
                .requestId(cardRequest.getId())
                .cardId(card.getId())
                .cardMaskedNumber("**** **** **** " + card.getCardNumberLastFour())
                .status(CardRequestStatus.APPROVED)
                .createdAt(cardRequest.getCreatedAt())
                .build();
    }

    @Transactional
    public CardRequestResponse rejectBlockRequest(Long requestId, String adminUsername) {
        log.info("Admin {} rejecting block request {}", adminUsername, requestId);

        CardRequest cardRequest = cardRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        cardRequest.setStatus(CardRequestStatus.REJECTED);

        cardRequestRepository.save(cardRequest);
        log.info("Admin {} rejected block request {}", adminUsername, requestId);
        return CardRequestResponse.builder()
                .requestId(cardRequest.getId())
                .cardId(cardRequest.getCard().getId())
                .status(CardRequestStatus.REJECTED)
                .createdAt(cardRequest.getCreatedAt())
                .build();
    }

    @Override
    public List<CardRequestResponse> getAllPendingRequest(String adminUsername) {
        List<CardRequest> pendingRequests = cardRequestRepository.findByStatus(CardRequestStatus.PENDING);
        return pendingRequests.stream().map(cardRequestMapper::toResponse).toList();
    }
}

