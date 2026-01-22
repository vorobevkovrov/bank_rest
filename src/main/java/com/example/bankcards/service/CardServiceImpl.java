package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;

import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;


@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final CardMapper cardMapper;
    private static final String CARD_NUMBER_PREFIX = "4000"; // Visa prefix для примера

    @Override
    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        log.info("Creating card for user ID: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // Генерируем номер карты
        String cardNumber = generateCardNumber();
        String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);

        // Проверяем уникальность последних 4 цифр для пользователя
        if (cardRepository.existsByCardNumberLastFourAndUserId(lastFourDigits, user.getId())) {
            throw new CardException("Card with similar last four digits already exists for this user");
        }

        // Создаем карту
        CardCreateRequest card = CardCreateRequest.builder()
                .userId(user.getId())
                .cardNumberEncrypted(encryptionService.encrypt(cardNumber))
                .cardNumberLastFour(lastFourDigits)
                .expiryDate(request.getExpiryDate())
                .cardHolderName(request.getCardHolderName().toUpperCase())
                .initialBalance(request.getInitialBalance())
                .cardStatus(CardStatus.CREATED)
                .build();
        log.info("CardCreateRequest card = CardCreateRequest.builder() {} {} {} {}", card.getUserId(),
                card.getExpiryDate(), card.getCardStatus(), card.getCardNumberEncrypted());
        Card savedCard = cardRepository.save(cardMapper.cardRequestToCard(card));
        log.info("Card created successfully: ID={} ", savedCard.getId());
        return cardMapper.cardToCardResponse(savedCard);
    }

    @Override
    @Transactional
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
    @Transactional
    public void deleteCard(Long cardId) {
        log.info("Deleting card ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        // Проверяем баланс
        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new CardException("Cannot delete card with positive balance");
        }

        cardRepository.delete(card);
        log.info("Card deleted successfully: ID={}", cardId);
    }

    @Override
    @Transactional
    public CardResponse updateCard(Long cardId, CardUpdateRequest request) {
        log.info("Updating card ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        // Обновляем статус
        if (request.getStatus() != null) {
            card.setStatus(request.getStatus());
            log.info("Card {} status changed to: {}", cardId, request.getStatus());
        }
        return null;
    }

    @Override
    @Transactional
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

        return null;
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
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return cardRepository.findByUserId(userId, pageable)
                .map(cardMapper::cardToCardResponse);
    }


    // Пользовательские методы
    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCards(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return cardRepository.findByUser(user, pageable)
                .map(cardMapper::cardToCardResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getUserCardById(Long userId, Long cardId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or access denied"));

        return cardMapper.cardToCardResponse(card);
    }

    @Override
    @Transactional
    public void requestCardBlock(Long userId, Long cardId, String reason) {
        log.info("User {} requested to block card {}", userId, cardId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or access denied"));

        if (card.isBlocked()) {
            throw new CardException("Card is already blocked");
        }

        // Здесь можно добавить логику отправки уведомления администратору
        // или создать запрос на блокировку в отдельной таблице

        log.info("Block request for card {} by user {}: {}", cardId, userId, reason);
        // В реальном приложении здесь была бы асинхронная обработка запроса
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // Ежедневно в полночь
    @Transactional
    public void checkForExpiredCards() {
        log.info("Checking for expired cards...");

        LocalDate today = LocalDate.now();
        cardRepository.findByExpiryDateBefore(today).forEach(card -> {
            if (card.isActive()) {
                card.setStatus(CardStatus.EXPIRED_DATE);
                cardRepository.save(card);
                log.info("Card {} expired and marked as EXPIRED", card.getId());
            }
        });
    }

    // Вспомогательные методы
    private String generateCardNumber() {
        // Генерация 16-значного номера карты (Luhn-совместимого)
        StringBuilder cardNumber = new StringBuilder(CARD_NUMBER_PREFIX);

        // Генерируем остальные 12 цифр случайно
        for (int i = 0; i < 12; i++) {
            cardNumber.append((int) (Math.random() * 10));
        }

        // Добавляем контрольную цифру по алгоритму Луна
        String numberWithoutCheckDigit = cardNumber.toString();
        int checkDigit = calculateLuhnCheckDigit(numberWithoutCheckDigit);
        cardNumber.append(checkDigit);
        return cardNumber.toString();
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }
}