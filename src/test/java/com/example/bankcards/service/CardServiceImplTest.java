package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.exceptions.CardException;
import com.example.bankcards.exception.exceptions.ResourceNotFoundException;
import com.example.bankcards.exception.exceptions.UnauthorizedAccessException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.CardServiceImpl;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.GenerateCardNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private GenerateCardNumber generateCardNumber;

    @InjectMocks
    private CardServiceImpl cardService;

    private User testUser;
    private User testAdmin;
    private Card testCard;
    private CardCreateRequest cardCreateRequest;
    private CardUpdateRequest cardUpdateRequest;
    private CardResponse cardResponse;
    private UserDetails userDetails;
    private UserDetails adminDetails;
    private final Long USER_ID = 1L;
    private final Long ADMIN_ID = 2L;
    private final Long CARD_ID = 100L;
    private final String USERNAME = "testuser";
    private final String ADMIN_USERNAME = "admin";
    private final String CARD_NUMBER = "1234567890123456";
    private final String ENCRYPTED_CARD_NUMBER = "encrypted_1234567890123456";
    private final String LAST_FOUR_DIGITS = "3456";
    private final String CARD_HOLDER_NAME = "TEST USER";
    private final Date EXPIRY_DATE = Date.from(LocalDate.now().plusYears(3).atStartOfDay(ZoneId.systemDefault()).toInstant());
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private final String MASKED_NUMBER = "************3456";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .role(Role.USER)
                .build();

        testAdmin = User.builder()
                .id(ADMIN_ID)
                .username(ADMIN_USERNAME)
                .role(Role.ADMIN)
                .build();

        testCard = Card.builder()
                .id(CARD_ID)
                .user(testUser)
                .cardNumber(ENCRYPTED_CARD_NUMBER)
                .cardNumberLastFour(LAST_FOUR_DIGITS)
                .cardHolderName(CARD_HOLDER_NAME)
                .expiryDate(EXPIRY_DATE)
                .status(CardStatus.CREATED)
                .balance(INITIAL_BALANCE)
                .build();

        cardCreateRequest = CardCreateRequest.builder()
                .userId(USER_ID)
                .cardHolderName(CARD_HOLDER_NAME)
                .expiryDate(EXPIRY_DATE)
                .initialBalance(INITIAL_BALANCE)
                .build();

        cardUpdateRequest = new CardUpdateRequest(
                null,
                null,
                EXPIRY_DATE,
                CardStatus.ACTIVE,
                new BigDecimal("2000.00")
        );

        cardResponse = CardResponse.builder()
                .maskedNumber(MASKED_NUMBER)
                .holderName(CARD_HOLDER_NAME)
                .expiryDate(EXPIRY_DATE)
                .status(CardStatus.CREATED)
                .balance(INITIAL_BALANCE)
                .userId(USER_ID)
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(USERNAME)
                .password("password")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        adminDetails = org.springframework.security.core.userdetails.User.builder()
                .username(ADMIN_USERNAME)
                .password("password")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
    }

    @Nested
    @DisplayName("Tests for createCard method")
    class CreateCardTests {

        @Test
        @DisplayName("Should successfully create card when all conditions are met")
        void createCard_Success() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(generateCardNumber.generateCardNumber()).thenReturn(CARD_NUMBER);
            when(encryptionService.encrypt(CARD_NUMBER)).thenReturn(ENCRYPTED_CARD_NUMBER);
            when(cardRepository.existsByCardNumberLastFourAndUserId(LAST_FOUR_DIGITS, USER_ID)).thenReturn(false);
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);
            when(cardMapper.cardToCardResponse(any(Card.class))).thenReturn(cardResponse);

            // Act
            CardResponse result = cardService.createCard(cardCreateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.maskedNumber()).isEqualTo(MASKED_NUMBER);
            assertThat(result.holderName()).isEqualTo(CARD_HOLDER_NAME);
            assertThat(result.status()).isEqualTo(CardStatus.CREATED);
            assertThat(result.userId()).isEqualTo(USER_ID);

            verify(userRepository).findById(USER_ID);
            verify(generateCardNumber).generateCardNumber();
            verify(encryptionService).encrypt(CARD_NUMBER);
            verify(cardRepository).existsByCardNumberLastFourAndUserId(LAST_FOUR_DIGITS, USER_ID);
            verify(cardRepository).save(any(Card.class));
            verify(cardMapper).cardToCardResponse(any(Card.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void createCard_UserNotFound() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.createCard(cardCreateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: " + USER_ID);

            verify(userRepository).findById(USER_ID);
            verify(generateCardNumber, never()).generateCardNumber();
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardException when last four digits already exist for user")
        void createCard_LastFourDigitsAlreadyExist() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(generateCardNumber.generateCardNumber()).thenReturn(CARD_NUMBER);
            when(cardRepository.existsByCardNumberLastFourAndUserId(LAST_FOUR_DIGITS, USER_ID)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> cardService.createCard(cardCreateRequest))
                    .isInstanceOf(CardException.class)
                    .hasMessage("Card with similar last four digits already exists for this user");

            verify(userRepository).findById(USER_ID);
            verify(generateCardNumber).generateCardNumber();
            verify(cardRepository).existsByCardNumberLastFourAndUserId(LAST_FOUR_DIGITS, USER_ID);
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should convert card holder name to uppercase")
        void createCard_ConvertsHolderNameToUppercase() {
            // Arrange
            String lowerCaseName = "test user";
            CardCreateRequest requestWithLowerCase = CardCreateRequest.builder()
                    .userId(USER_ID)
                    .cardHolderName(lowerCaseName)
                    .expiryDate(EXPIRY_DATE)
                    .initialBalance(INITIAL_BALANCE)
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(generateCardNumber.generateCardNumber()).thenReturn(CARD_NUMBER);
            when(encryptionService.encrypt(CARD_NUMBER)).thenReturn(ENCRYPTED_CARD_NUMBER);
            when(cardRepository.existsByCardNumberLastFourAndUserId(LAST_FOUR_DIGITS, USER_ID)).thenReturn(false);

            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
                Card cardToSave = invocation.getArgument(0);
                assertThat(cardToSave.getCardHolderName()).isEqualTo(CARD_HOLDER_NAME);
                return testCard;
            });

            when(cardMapper.cardToCardResponse(any(Card.class))).thenReturn(cardResponse);

            // Act
            cardService.createCard(requestWithLowerCase);

            // Assert
            verify(cardRepository).save(argThat(card ->
                    card.getCardHolderName().equals(CARD_HOLDER_NAME)
            ));
        }
    }

    @Nested
    @DisplayName("Tests for activateCard method")
    class ActivateCardTests {

        @Test
        @DisplayName("Should successfully activate card when it exists and is not active")
        void activateCard_Success() {
            // Arrange
            testCard.setStatus(CardStatus.CREATED);
            Card activatedCard = Card.builder()
                    .id(CARD_ID)
                    .user(testUser)
                    .status(CardStatus.ACTIVE)
                    .build();

            CardResponse activatedResponse = CardResponse.builder()
                    .status(CardStatus.ACTIVE)
                    .build();

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardRepository.save(any(Card.class))).thenReturn(activatedCard);
            when(cardMapper.cardToCardResponse(activatedCard)).thenReturn(activatedResponse);

            // Act
            CardResponse result = cardService.activateCard(CARD_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(CardStatus.ACTIVE);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository).save(testCard);
            verify(cardMapper).cardToCardResponse(activatedCard);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card not found")
        void activateCard_CardNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.activateCard(CARD_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found with id: " + CARD_ID);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardException when card is already active")
        void activateCard_AlreadyActive() {
            // Arrange
            testCard.setStatus(CardStatus.ACTIVE);
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));

            // Act & Assert
            assertThatThrownBy(() -> cardService.activateCard(CARD_ID))
                    .isInstanceOf(CardException.class)
                    .hasMessage("Card is already active");

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for blockCard method")
    class BlockCardTests {

        @Test
        @DisplayName("Should successfully block card when it exists and is not blocked")
        void blockCard_Success() {
            // Arrange
            testCard.setStatus(CardStatus.ACTIVE);
            Card blockedCard = Card.builder()
                    .id(CARD_ID)
                    .user(testUser)
                    .status(CardStatus.BLOCKED)
                    .build();

            CardResponse blockedResponse = CardResponse.builder()
                    .status(CardStatus.BLOCKED)
                    .build();

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardRepository.save(any(Card.class))).thenReturn(blockedCard);
            when(cardMapper.cardToCardResponse(blockedCard)).thenReturn(blockedResponse);

            // Act
            CardResponse result = cardService.blockCard(CARD_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(CardStatus.BLOCKED);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository).save(testCard);
            verify(cardMapper).cardToCardResponse(blockedCard);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card not found")
        void blockCard_CardNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.blockCard(CARD_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found with id: " + CARD_ID);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw CardException when card is already blocked")
        void blockCard_AlreadyBlocked() {
            // Arrange
            testCard.setStatus(CardStatus.BLOCKED);
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));

            // Act & Assert
            assertThatThrownBy(() -> cardService.blockCard(CARD_ID))
                    .isInstanceOf(CardException.class)
                    .hasMessage("Card is already blocked");

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for getCardBalance method")
    class GetCardBalanceTests {

        @Test
        @DisplayName("Should return balance for card owner")
        void getCardBalance_AsOwner_Success() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(encryptionService.decrypt(ENCRYPTED_CARD_NUMBER)).thenReturn(CARD_NUMBER);

            // Act
            BalanceResponse result = cardService.getCardBalance(CARD_ID, userDetails);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.balance()).isEqualTo(INITIAL_BALANCE);
            assertThat(result.cardNumber()).isEqualTo(CARD_NUMBER);
            assertThat(result.holderName()).isEqualTo(CARD_HOLDER_NAME);

            verify(cardRepository).findById(CARD_ID);
            verify(encryptionService).decrypt(ENCRYPTED_CARD_NUMBER);
        }

        @Test
        @DisplayName("Should return balance for admin")
        void getCardBalance_AsAdmin_Success() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(encryptionService.decrypt(ENCRYPTED_CARD_NUMBER)).thenReturn(CARD_NUMBER);

            // Act
            BalanceResponse result = cardService.getCardBalance(CARD_ID, adminDetails);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.balance()).isEqualTo(INITIAL_BALANCE);
            assertThat(result.cardNumber()).isEqualTo(CARD_NUMBER);
            assertThat(result.holderName()).isEqualTo(CARD_HOLDER_NAME);

            verify(cardRepository).findById(CARD_ID);
            verify(encryptionService).decrypt(ENCRYPTED_CARD_NUMBER);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card not found")
        void getCardBalance_CardNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.getCardBalance(CARD_ID, userDetails))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found with id: " + CARD_ID);

            verify(cardRepository).findById(CARD_ID);
            verify(encryptionService, never()).decrypt(any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner and not admin")
        void getCardBalance_UnauthorizedUser() {
            // Arrange
            UserDetails anotherUser = org.springframework.security.core.userdetails.User.builder()
                    .username("anotheruser")
                    .password("password")
                    .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));

            // Act & Assert
            assertThatThrownBy(() -> cardService.getCardBalance(CARD_ID, anotherUser))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("You don't have permission to access this card ");

            verify(cardRepository).findById(CARD_ID);
            verify(encryptionService, never()).decrypt(any());
        }
    }

    @Nested
    @DisplayName("Tests for updateCard method")
    class UpdateCardTests {

        @Test
        @DisplayName("Should update all fields successfully")
        void updateCard_AllFields_Success() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);
            when(cardMapper.cardToCardResponse(any(Card.class))).thenReturn(cardResponse);

            // Act
            CardResponse result = cardService.updateCard(CARD_ID, cardUpdateRequest);

            // Assert
            assertThat(result).isNotNull();

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository).save(testCard);
            verify(cardMapper).cardToCardResponse(testCard);
        }

        @Test
        @DisplayName("Should update only expiry date")
        void updateCard_OnlyExpiryDate_Success() {
            // Arrange
            Date newExpiryDate = Date.from(LocalDate.now().plusYears(4).atStartOfDay(ZoneId.systemDefault()).toInstant());
            CardUpdateRequest request = new CardUpdateRequest(
                    null, null, newExpiryDate, null, null
            );

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);
            when(cardMapper.cardToCardResponse(any(Card.class))).thenReturn(cardResponse);

            // Act
            CardResponse result = cardService.updateCard(CARD_ID, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testCard.getExpiryDate()).isEqualTo(newExpiryDate);
            assertThat(testCard.getStatus()).isEqualTo(CardStatus.CREATED);
            assertThat(testCard.getBalance()).isEqualTo(INITIAL_BALANCE);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository).save(testCard);
        }

        @Test
        @DisplayName("Should update only status")
        void updateCard_OnlyStatus_Success() {
            // Arrange
            CardUpdateRequest request = new CardUpdateRequest(
                    null, null, null, CardStatus.ACTIVE, null
            );

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);
            when(cardMapper.cardToCardResponse(any(Card.class))).thenReturn(cardResponse);

            // Act
            CardResponse result = cardService.updateCard(CARD_ID, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
            assertThat(testCard.getExpiryDate()).isEqualTo(EXPIRY_DATE);
            assertThat(testCard.getBalance()).isEqualTo(INITIAL_BALANCE);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository).save(testCard);
        }

        @Test
        @DisplayName("Should update only balance")
        void updateCard_OnlyBalance_Success() {
            // Arrange
            BigDecimal newBalance = new BigDecimal("5000.00");
            CardUpdateRequest request = new CardUpdateRequest(
                    null, null, null, null, newBalance
            );

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);
            when(cardMapper.cardToCardResponse(any(Card.class))).thenReturn(cardResponse);

            // Act
            CardResponse result = cardService.updateCard(CARD_ID, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testCard.getBalance()).isEqualTo(newBalance);
            assertThat(testCard.getExpiryDate()).isEqualTo(EXPIRY_DATE);
            assertThat(testCard.getStatus()).isEqualTo(CardStatus.CREATED);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository).save(testCard);
        }

        @Test
        @DisplayName("Should not save when no fields to update")
        void updateCard_NoChanges_Success() {
            // Arrange
            CardUpdateRequest request = new CardUpdateRequest(
                    null, null, null, null, null
            );

            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardMapper.cardToCardResponse(any(Card.class))).thenReturn(cardResponse);

            // Act
            CardResponse result = cardService.updateCard(CARD_ID, request);

            // Assert
            assertThat(result).isNotNull();
            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository, never()).save(any());
            verify(cardMapper).cardToCardResponse(testCard);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card not found")
        void updateCard_CardNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.updateCard(CARD_ID, cardUpdateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found with id: " + CARD_ID);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests for deleteCard method")
    class DeleteCardTests {

        @Test
        @DisplayName("Should delete card successfully")
        void deleteCard_Success() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            doNothing().when(cardRepository).delete(any(Card.class));

            // Act
            cardService.deleteCard(CARD_ID);

            // Assert
            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository).delete(testCard);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card not found")
        void deleteCard_CardNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.deleteCard(CARD_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found with id: " + CARD_ID);

            verify(cardRepository).findById(CARD_ID);
            verify(cardRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Tests for getCardById method")
    class GetCardByIdTests {

        @Test
        @DisplayName("Should return card when found")
        void getCardById_Success() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
            when(cardMapper.cardToCardResponse(testCard)).thenReturn(cardResponse);

            // Act
            CardResponse result = cardService.getCardById(CARD_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.maskedNumber()).isEqualTo(MASKED_NUMBER);
            assertThat(result.holderName()).isEqualTo(CARD_HOLDER_NAME);

            verify(cardRepository).findById(CARD_ID);
            verify(cardMapper).cardToCardResponse(testCard);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when card not found")
        void getCardById_CardNotFound() {
            // Arrange
            when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.getCardById(CARD_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Card not found with id: " + CARD_ID);

            verify(cardRepository).findById(CARD_ID);
            verify(cardMapper, never()).cardToCardResponse(any());
        }
    }

    @Nested
    @DisplayName("Tests for getAllCards method")
    class GetAllCardsTests {

        @Test
        @DisplayName("Should return page of all cards")
        void getAllCards_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Card> cards = List.of(testCard);
            Page<Card> cardPage = new PageImpl<>(cards, pageable, cards.size());

            when(cardRepository.findAll(pageable)).thenReturn(cardPage);
            when(cardMapper.cardToCardResponse(testCard)).thenReturn(cardResponse);

            // Act
            Page<CardResponse> result = cardService.getAllCards(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).maskedNumber()).isEqualTo(MASKED_NUMBER);

            verify(cardRepository).findAll(pageable);
            verify(cardMapper).cardToCardResponse(testCard);
        }

        @Test
        @DisplayName("Should return empty page when no cards exist")
        void getAllCards_EmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> emptyPage = Page.empty(pageable);

            when(cardRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            Page<CardResponse> result = cardService.getAllCards(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

            verify(cardRepository).findAll(pageable);
            verify(cardMapper, never()).cardToCardResponse(any());
        }
    }

    @Nested
    @DisplayName("Tests for getCardsByUserId method")
    class GetCardsByUserIdTests {

        @Test
        @DisplayName("Should return page of cards for user")
        void getCardsByUserId_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Card> cards = List.of(testCard);
            Page<Card> cardPage = new PageImpl<>(cards, pageable, cards.size());

            when(cardRepository.findByUserId(USER_ID, pageable)).thenReturn(cardPage);
            when(cardMapper.cardToCardResponse(testCard)).thenReturn(cardResponse);

            // Act
            Page<CardResponse> result = cardService.getCardsByUserId(USER_ID, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).maskedNumber()).isEqualTo(MASKED_NUMBER);

            verify(cardRepository).findByUserId(USER_ID, pageable);
            verify(cardMapper).cardToCardResponse(testCard);
        }

        @Test
        @DisplayName("Should return empty page when user has no cards")
        void getCardsByUserId_EmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> emptyPage = Page.empty(pageable);

            when(cardRepository.findByUserId(USER_ID, pageable)).thenReturn(emptyPage);

            // Act
            Page<CardResponse> result = cardService.getCardsByUserId(USER_ID, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

            verify(cardRepository).findByUserId(USER_ID, pageable);
            verify(cardMapper, never()).cardToCardResponse(any());
        }
    }

    @Nested
    @DisplayName("Tests for getCardsByUserName method")
    class GetCardsByUserNameTests {

        @Test
        @DisplayName("Should return page of cards for username")
        void getCardsByUserName_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Card> cards = List.of(testCard);
            Page<Card> cardPage = new PageImpl<>(cards, pageable, cards.size());

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(cardRepository.findByUserId(USER_ID, pageable)).thenReturn(cardPage);
            when(cardMapper.cardToCardResponse(testCard)).thenReturn(cardResponse);

            // Act
            Page<CardResponse> result = cardService.getCardsByUserName(USERNAME, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).maskedNumber()).isEqualTo(MASKED_NUMBER);

            verify(userRepository).findByUsername(USERNAME);
            verify(cardRepository).findByUserId(USER_ID, pageable);
            verify(cardMapper).cardToCardResponse(testCard);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when username not found")
        void getCardsByUserName_UserNotFound() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cardService.getCardsByUserName(USERNAME, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Username not found: " + USERNAME);

            verify(userRepository).findByUsername(USERNAME);
            verify(cardRepository, never()).findByUserId(any(), any());
        }

        @Test
        @DisplayName("Should return empty page when user has no cards")
        void getCardsByUserName_EmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Card> emptyPage = Page.empty(pageable);

            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
            when(cardRepository.findByUserId(USER_ID, pageable)).thenReturn(emptyPage);

            // Act
            Page<CardResponse> result = cardService.getCardsByUserName(USERNAME, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();

            verify(userRepository).findByUsername(USERNAME);
            verify(cardRepository).findByUserId(USER_ID, pageable);
            verify(cardMapper, never()).cardToCardResponse(any());
        }
    }
}