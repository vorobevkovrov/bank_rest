package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.UnauthorizedAccessException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.GenerateCardNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final CardMapper cardMapper;
    private final GenerateCardNumber generateCardNumber;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardResponse createCard(CardCreateRequest request) {
        log.info("Creating card for user ID: {}", request.userId());

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));

        // Генерируем номер карты
        String cardNumber = generateCardNumber.generateCardNumber();
        String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);

        // Проверяем уникальность последних 4 цифр для пользователя
        if (cardRepository.existsByCardNumberLastFourAndUserId(lastFourDigits, user.getId())) {
            throw new CardException("Card with similar last four digits already exists for this user");
        }

        // Создаем сущность Card из запроса
        Card card = Card.builder()
                .user(user)
                .cardNumber(encryptionService.encrypt(cardNumber))
                .cardNumberLastFour(lastFourDigits)
                .expiryDate(request.expiryDate())
                .cardHolderName(request.cardHolderName().toUpperCase())
                .balance(request.initialBalance())
                .status(CardStatus.CREATED)
                .build();

        log.info("Creating card entity: UserID={}, ExpiryDate={}, Status={}, LastFourDigits={}",
                user.getId(), request.expiryDate(), CardStatus.CREATED, lastFourDigits);

        Card savedCard = cardRepository.save(card);
        log.info("Card created successfully: ID={}", savedCard.getId());

        return cardMapper.cardToCardResponse(savedCard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardResponse activateCard(Long cardId) {
        log.info("Activating card ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (card.isActive()) {
            throw new CardException("Card is already active");
        }
        card.setStatus(CardStatus.ACTIVE);
        Card activatedCard = cardRepository.save(card);
        log.info("Card activated successfully: ID={}", cardId);
        return cardMapper.cardToCardResponse(activatedCard);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getCardBalance(Long cardId, UserDetails currentUser) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
        checkAccessRights(card, currentUser);
        return BalanceResponse.builder()
                .balance(card.getBalance())
                .cardNumber(encryptionService.decrypt(card.getCardNumber()))
                .holderName(card.getCardHolderName())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardResponse updateCard(Long cardId, CardUpdateRequest request) {
        log.info("Updating card ID: {}", cardId);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        log.info("Found card: ID={}, Status={}, User={}",
                card.getId(), card.getStatus(), card.getUser().getId());

        boolean updated = false;
        // 3. Обновляем поля, если они предоставлены
        if (request.expiryDate() != null) {
            card.setExpiryDate(request.expiryDate());
            updated = true;
            log.info("Updated expiry date to: {}", request.expiryDate());
        }

        if (request.status() != null) {
            card.setStatus(request.status());
            updated = true;
            log.info("Updated status to: {}", request.status());
        }

        if (request.balance() != null) {
            card.setBalance(request.balance());
            updated = true;
            log.info("Updated balance from {} to {}", card.getBalance(), request.balance());
        }

        // 4. Сохраняем только если были изменения
        if (updated) {
            card = cardRepository.save(card);
            log.info("Card updated successfully: ID={}", card.getId());
        } else {
            log.info("No changes detected for card ID: {}", cardId);
        }
        // 5. Возвращаем обновленную карту
        return cardMapper.cardToCardResponse(card);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCard(Long cardId) {
        log.info("Deleting card ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        cardRepository.delete(card);
        log.info("Card deleted successfully: ID={}", cardId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardResponse blockCard(Long cardId) {
        log.info("Blocking card ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        if (card.isBlocked()) {
            throw new CardException("Card is already blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        Card blockedCard = cardRepository.save(card);
        log.info("Card blocked successfully: ID={}", cardId);
        return cardMapper.cardToCardResponse(blockedCard);
    }


    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        return cardMapper.cardToCardResponse(card);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(cardMapper::cardToCardResponse);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsByUserId(Long userId, Pageable pageable) {
        return cardRepository.findByUserId(userId, pageable)
                .map(cardMapper::cardToCardResponse);
    }

    @Override
    public Page<CardResponse> getCardsByUserName(String userName, Pageable pageable) {
        // Сначала находим пользователя
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("Username not found: " + userName));

        // Ищем карты по ID пользователя, а не по имени владельца!
        Page<CardResponse> cardResponsePage = cardRepository.findByUserId(user.getId(), pageable)
                .map(cardMapper::cardToCardResponse);
        log.info("User {} (ID: {}) retrieved {} cards", userName, user.getId(), cardResponsePage.getTotalElements());
        return cardResponsePage;
    }

    private void checkAccessRights(Card card, UserDetails currentUser) {
        // Проверяем, является ли пользователь ADMIN
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            log.debug("Admin access granted for card ID: {}", card.getId());
            return; // ADMIN имеет доступ ко всем картам
        }

        // Проверяем, является ли пользователь владельцем карты
        User cardOwner = card.getUser();
        String currentUsername = currentUser.getUsername();

        if (!cardOwner.getUsername().equals(currentUsername)) {
            log.warn("Unauthorized access attempt: user={}, card owner={}",
                    currentUsername, cardOwner.getUsername());
            throw new UnauthorizedAccessException(
                    "You don't have permission to access this card ");
        }

        log.debug("User access granted for card ID: {}", card.getId());
    }

}