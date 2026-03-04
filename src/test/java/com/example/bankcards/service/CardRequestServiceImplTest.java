package com.example.bankcards.service;

import com.example.bankcards.dto.request.BlockCardRequest;
import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.entity.*;
import com.example.bankcards.exception.exceptions.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardRequestRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.CardRequestServiceImpl;
import com.example.bankcards.util.CardRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardRequestServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardRequestRepository cardRequestRepository;

    @Mock
    private CardRequestMapper cardRequestMapper;

    @InjectMocks
    private CardRequestServiceImpl cardRequestService;

    private User testUser;
    private User testAdmin;
    private Card testCard;
    private CardRequest testCardRequest;
    private BlockCardRequest blockCardRequest;
    private CardRequestResponse cardRequestResponse;
    private final String USERNAME = "testuser";
    private final String ADMIN_USERNAME = "admin";
    private final Long CARD_ID = 1L;
    private final Long REQUEST_ID = 100L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username(USERNAME)
                .role(Role.USER)
                .build();

        testAdmin = User.builder()
                .id(2L)
                .username(ADMIN_USERNAME)
                .role(Role.ADMIN)
                .build();

        testCard = Card.builder()
                .id(CARD_ID)
                .user(testUser)
                .cardNumber("encrypted_card_number_123")
                .cardNumberLastFour("3456")
                .cardHolderName("TEST USER")
                .expiryDate(new Date())
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        testCardRequest = CardRequest.builder()
                .id(REQUEST_ID)
                .card(testCard)
                .user(testUser)
                .requestType(CardRequestType.BLOCK)
                .status(CardRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        blockCardRequest = BlockCardRequest.builder()
                .cardId(CARD_ID)
                .build();

        cardRequestResponse = CardRequestResponse.builder()
                .requestId(REQUEST_ID)
                .cardId(CARD_ID)
                .cardMaskedNumber("************3456")
                .status(CardRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Tests for requestCardBlock method")
    class RequestCardBlockTests {

        @Test
        @DisplayName("Should successfully create block request when all conditions are met")
        void requestCardBlock_Success() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(cardRequestRepository.existsByCardAndStatus(testCard, CardRequestStatus.PENDING))
                    .thenReturn(false);
            when(cardRequestRepository.save(any(CardRequest.class))).thenReturn(testCardRequest);
            when(cardRequestMapper.toResponse(any(CardRequest.class))).thenReturn(cardRequestResponse);

            // Act
            CardRequestResponse result = cardRequestService.requestCardBlock(blockCardRequest, USERNAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.requestId()).isEqualTo(REQUEST_ID);
            assertThat(result.cardId()).isEqualTo(CARD_ID);
            assertThat(result.cardMaskedNumber()).isEqualTo("************3456");
            assertThat(result.status()).isEqualTo(CardRequestStatus.PENDING);

            verify(cardRepository).findById(CARD_ID);
            verify(userRepository).findByUsername(USERNAME);
            verify(cardRequestRepository).existsByCardAndStatus(testCard, CardRequestStatus.PENDING);
            verify(cardRequestRepository).save(any(CardRequest.class));
            verify(cardRequestMapper).toResponse(any(CardRequest.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card not found")
        void requestCardBlock_CardNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.requestCardBlock(blockCardRequest, USERNAME))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found");

            verify(cardRepository).findById(CARD_ID);
            verify(userRepository, never()).findByUsername(anyString());
            verify(cardRequestRepository, never()).existsByCardAndStatus(any(), any());
            verify(cardRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void requestCardBlock_UserNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.requestCardBlock(blockCardRequest, USERNAME))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(cardRepository).findById(CARD_ID);
            verify(userRepository).findByUsername(USERNAME);
            verify(cardRequestRepository, never()).existsByCardAndStatus(any(), any());
            verify(cardRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw SameCardTransferException when user tries to block card they don't own")
        void requestCardBlock_UserDoesNotOwnCard() {
            // Arrange
            User anotherUser = User.builder()
                    .id(999L)
                    .username("another")
                    .role(Role.USER)
                    .build();

            Card anotherUserCard = Card.builder()
                    .id(CARD_ID)
                    .user(anotherUser)
                    .cardNumber("encrypted_card_number_456")
                    .cardNumberLastFour("7890")
                    .cardHolderName("ANOTHER USER")
                    .status(CardStatus.ACTIVE)
                    .build();

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(anotherUserCard));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.requestCardBlock(blockCardRequest, USERNAME))
                    .isInstanceOf(SameCardTransferException.class)
                    .hasMessage("This is not your card");

            verify(cardRepository).findById(CARD_ID);
            verify(userRepository).findByUsername(USERNAME);
            verify(cardRequestRepository, never()).existsByCardAndStatus(any(), any());
            verify(cardRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardNotActiveException when card is already blocked")
        void requestCardBlock_CardAlreadyBlocked() {
            // Arrange
            testCard.setStatus(CardStatus.BLOCKED);

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.requestCardBlock(blockCardRequest, USERNAME))
                    .isInstanceOf(CardNotActiveException.class)
                    .hasMessage("Card is already blocked");

            verify(cardRepository).findById(CARD_ID);
            verify(userRepository).findByUsername(USERNAME);
            verify(cardRequestRepository, never()).existsByCardAndStatus(any(), any());
            verify(cardRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DuplicateRequestException when pending request already exists")
        void requestCardBlock_DuplicatePendingRequest() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(cardRequestRepository.existsByCardAndStatus(testCard, CardRequestStatus.PENDING))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.requestCardBlock(blockCardRequest, USERNAME))
                    .isInstanceOf(DuplicateRequestException.class)
                    .hasMessage("There is already a pending request for this card");

            verify(cardRepository).findById(CARD_ID);
            verify(userRepository).findByUsername(USERNAME);
            verify(cardRequestRepository).existsByCardAndStatus(testCard, CardRequestStatus.PENDING);
            verify(cardRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for approveBlockRequest method")
    class ApproveBlockRequestTests {

        @Test
        @DisplayName("Should successfully approve pending block request")
        void approveBlockRequest_Success() {
            // Arrange
            when(cardRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(testCardRequest));
            when(cardRequestRepository.save(any(CardRequest.class))).thenReturn(testCardRequest);
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);
            when(cardRequestMapper.toResponse(any(CardRequest.class))).thenReturn(cardRequestResponse);

            // Act
            CardRequestResponse result = cardRequestService.approveBlockRequest(REQUEST_ID, ADMIN_USERNAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.requestId()).isEqualTo(REQUEST_ID);

            assertThat(testCardRequest.getStatus()).isEqualTo(CardRequestStatus.APPROVED);
            assertThat(testCard.getStatus()).isEqualTo(CardStatus.BLOCKED);

            verify(cardRequestRepository).findById(REQUEST_ID);
            verify(cardRequestRepository).save(testCardRequest);
            verify(cardRepository).save(testCard);
            verify(cardRequestMapper).toResponse(testCardRequest);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when request not found")
        void approveBlockRequest_RequestNotFound() {
            // Arrange
            when(cardRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.approveBlockRequest(REQUEST_ID, ADMIN_USERNAME))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Request with id not found");

            verify(cardRequestRepository).findById(REQUEST_ID);
            verify(cardRequestRepository, never()).save(any());
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardRequestStatusException when request is not pending")
        void approveBlockRequest_RequestNotPending() {
            // Arrange
            testCardRequest.setStatus(CardRequestStatus.APPROVED);
            when(cardRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(testCardRequest));

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.approveBlockRequest(REQUEST_ID, ADMIN_USERNAME))
                    .isInstanceOf(CardRequestStatusException.class)
                    .hasMessage("Request is not pending");

            verify(cardRequestRepository).findById(REQUEST_ID);
            verify(cardRequestRepository, never()).save(any());
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardRequestStatusException when request is rejected")
        void approveBlockRequest_RequestRejected() {
            // Arrange
            testCardRequest.setStatus(CardRequestStatus.REJECTED);
            when(cardRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(testCardRequest));

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.approveBlockRequest(REQUEST_ID, ADMIN_USERNAME))
                    .isInstanceOf(CardRequestStatusException.class)
                    .hasMessage("Request is not pending");

            verify(cardRequestRepository).findById(REQUEST_ID);
            verify(cardRequestRepository, never()).save(any());
            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for rejectBlockRequest method")
    class RejectBlockRequestTests {

        @Test
        @DisplayName("Should successfully reject pending block request")
        void rejectBlockRequest_Success() {
            // Arrange
            when(cardRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(testCardRequest));
            when(cardRequestRepository.save(any(CardRequest.class))).thenReturn(testCardRequest);
            when(cardRequestMapper.toResponse(any(CardRequest.class))).thenReturn(cardRequestResponse);

            // Act
            CardRequestResponse result = cardRequestService.rejectBlockRequest(REQUEST_ID, ADMIN_USERNAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.requestId()).isEqualTo(REQUEST_ID);
            assertThat(testCardRequest.getStatus()).isEqualTo(CardRequestStatus.REJECTED);
            assertThat(testCard.getStatus()).isEqualTo(CardStatus.ACTIVE);

            verify(cardRequestRepository).findById(REQUEST_ID);
            verify(cardRequestRepository).save(testCardRequest);
            verify(cardRepository, never()).save(any());
            verify(cardRequestMapper).toResponse(testCardRequest);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when request not found")
        void rejectBlockRequest_RequestNotFound() {
            // Arrange
            when(cardRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardRequestService.rejectBlockRequest(REQUEST_ID, ADMIN_USERNAME))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Request not found");
            verify(cardRequestRepository).findById(REQUEST_ID);
            verify(cardRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject request even if it's not pending")
        void rejectBlockRequest_RequestNotPending() {
            // Arrange
            testCardRequest.setStatus(CardRequestStatus.APPROVED);
            when(cardRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(testCardRequest));
            when(cardRequestRepository.save(any(CardRequest.class))).thenReturn(testCardRequest);
            when(cardRequestMapper.toResponse(any(CardRequest.class))).thenReturn(cardRequestResponse);

            // Act
            CardRequestResponse result = cardRequestService.rejectBlockRequest(REQUEST_ID, ADMIN_USERNAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testCardRequest.getStatus()).isEqualTo(CardRequestStatus.REJECTED);

            verify(cardRequestRepository).findById(REQUEST_ID);
            verify(cardRequestRepository).save(testCardRequest);
            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for getAllPendingRequest method")
    class GetAllPendingRequestTests {

        @Test
        @DisplayName("Should return list of all pending requests")
        void getAllPendingRequest_Success() {
            // Arrange
            Card secondCard = Card.builder()
                    .id(2L)
                    .user(testUser)
                    .cardNumber("encrypted_card_number_789")
                    .cardNumberLastFour("9012")
                    .cardHolderName("TEST USER")
                    .status(CardStatus.ACTIVE)
                    .build();

            CardRequest secondRequest = CardRequest.builder()
                    .id(200L)
                    .card(secondCard)
                    .user(testUser)
                    .requestType(CardRequestType.BLOCK)
                    .status(CardRequestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            List<CardRequest> pendingRequests = List.of(testCardRequest, secondRequest);

            CardRequestResponse secondResponse = CardRequestResponse.builder()
                    .requestId(200L)
                    .cardId(2L)
                    .cardMaskedNumber("************9012")
                    .status(CardRequestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(cardRequestRepository.findByStatus(CardRequestStatus.PENDING))
                    .thenReturn(pendingRequests);
            when(cardRequestMapper.toResponse(testCardRequest)).thenReturn(cardRequestResponse);
            when(cardRequestMapper.toResponse(secondRequest)).thenReturn(secondResponse);

            // Act
            List<CardRequestResponse> result = cardRequestService.getAllPendingRequest(ADMIN_USERNAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(cardRequestResponse, secondResponse);
            assertThat(result.get(0).cardMaskedNumber()).isEqualTo("************3456");
            assertThat(result.get(1).cardMaskedNumber()).isEqualTo("************9012");

            verify(cardRequestRepository).findByStatus(CardRequestStatus.PENDING);
            verify(cardRequestMapper, times(2)).toResponse(any(CardRequest.class));
        }

        @Test
        @DisplayName("Should return empty list when no pending requests exist")
        void getAllPendingRequest_EmptyList() {
            // Arrange
            when(cardRequestRepository.findByStatus(CardRequestStatus.PENDING))
                    .thenReturn(List.of());

            // Act
            List<CardRequestResponse> result = cardRequestService.getAllPendingRequest(ADMIN_USERNAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(cardRequestRepository).findByStatus(CardRequestStatus.PENDING);
            verify(cardRequestMapper, never()).toResponse(any());
        }
    }
}