package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.exceptions.CardException;
import com.example.bankcards.exception.exceptions.ResourceNotFoundException;
import com.example.bankcards.exception.exceptions.UnauthorizedAccessException;
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


/**
 * Реализация сервиса для управления банковскими картами.
 * <p>
 * Предоставляет функционал для создания, активации, блокировки, обновления, удаления
 * и получения информации о картах. Включает проверки прав доступа и бизнес-логику
 * операций с картами.
 * </p>
 *
 * <h2>Основные возможности:</h2>
 * <ul>
 *   <li>Создание новой карты с генерацией номера и проверкой уникальности</li>
 *   <li>Активация созданной карты</li>
 *   <li>Блокировка активной карты</li>
 *   <li>Получение баланса карты (с проверкой прав доступа)</li>
 *   <li>Обновление параметров карты (срок действия, статус, баланс)</li>
 *   <li>Удаление карты</li>
 *   <li>Поиск карт по различным критериям</li>
 * </ul>
 *
 * <h2>Безопасность:</h2>
 * <ul>
 *   <li>Пользователи могут получать доступ только к своим картам</li>
 *   <li>Администраторы имеют доступ ко всем картам</li>
 *   <li>Номера карт хранятся в зашифрованном виде</li>
 * </ul>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see CardService
 * @see Card
 * @see CardRepository
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final CardMapper cardMapper;
    private final GenerateCardNumber generateCardNumber;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Процесс создания карты:
     * <ol>
     *   <li>Поиск пользователя по ID</li>
     *   <li>Генерация уникального номера карты</li>
     *   <li>Проверка уникальности последних 4 цифр для пользователя</li>
     *   <li>Шифрование номера карты</li>
     *   <li>Создание сущности Card со статусом {@link CardStatus#CREATED}</li>
     *   <li>Сохранение в базу данных</li>
     * </ol>
     * </p>
     *
     * @param request запрос на создание карты (содержит userId, имя держателя, срок действия, начальный баланс)
     * @return созданная карта в формате {@link CardResponse}
     * @throws ResourceNotFoundException если пользователь не найден
     * @throws CardException если последние 4 цифры уже существуют у пользователя
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * Активирует карту, изменяя её статус с {@link CardStatus#CREATED} на {@link CardStatus#ACTIVE}.
     * Карта должна существовать и не быть уже активной.
     * </p>
     *
     * @param cardId идентификатор карты для активации
     * @return активированная карта в формате {@link CardResponse}
     * @throws ResourceNotFoundException если карта не найдена
     * @throws CardException если карта уже активна
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * Возвращает баланс карты с расшифрованным номером.
     * Выполняет проверку прав доступа:
     * <ul>
     *   <li>Администраторы имеют доступ к любой карте</li>
     *   <li>Обычные пользователи могут просматривать только свои карты</li>
     * </ul>
     * </p>
     *
     * @param cardId идентификатор карты
     * @param currentUser данные текущего аутентифицированного пользователя
     * @return информация о балансе карты {@link BalanceResponse}
     * @throws ResourceNotFoundException если карта не найдена
     * @throws UnauthorizedAccessException если у пользователя нет прав на просмотр карты
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * Обновляет поля карты, если они предоставлены в запросе:
     * <ul>
     *   <li>Срок действия ({@code expiryDate})</li>
     *   <li>Статус ({@code status})</li>
     *   <li>Баланс ({@code balance})</li>
     * </ul>
     * Сохранение в базу данных происходит только при наличии изменений.
     * </p>
     *
     * @param cardId идентификатор карты для обновления
     * @param request запрос с обновляемыми полями
     * @return обновленная карта в формате {@link CardResponse}
     * @throws ResourceNotFoundException если карта не найдена
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * Физически удаляет карту из базы данных.
     * <strong>Внимание:</strong> Операция необратима.
     * </p>
     *
     * @param cardId идентификатор карты для удаления
     * @throws ResourceNotFoundException если карта не найдена
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCard(Long cardId) {
        log.info("Deleting card ID: {}", cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        cardRepository.delete(card);
        log.info("Card deleted successfully: ID={}", cardId);
    }


    /**
     * {@inheritDoc}
     *
     * <p>
     * Блокирует карту, изменяя её статус на {@link CardStatus#BLOCKED}.
     * Карта должна существовать и не быть уже заблокированной.
     * </p>
     *
     * @param cardId идентификатор карты для блокировки
     * @return заблокированная карта в формате {@link CardResponse}
     * @throws ResourceNotFoundException если карта не найдена
     * @throws CardException если карта уже заблокирована
     */
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


    /**
     * {@inheritDoc}
     *
     * <p>
     * Возвращает информацию о карте по её идентификатору.
     * </p>
     *
     * @param cardId идентификатор карты
     * @return информация о карте в формате {@link CardResponse}
     * @throws ResourceNotFoundException если карта не найдена
     */
    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        return cardMapper.cardToCardResponse(card);
    }


    /**
     * {@inheritDoc}
     *
     * <p>
     * Возвращает постраничный список всех карт в системе.
     * </p>
     *
     * @param pageable параметры пагинации (номер страницы, размер, сортировка)
     * @return страница с картами в формате {@link CardResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(cardMapper::cardToCardResponse);

    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Возвращает постраничный список карт конкретного пользователя по его ID.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с картами пользователя в формате {@link CardResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsByUserId(Long userId, Pageable pageable) {
        return cardRepository.findByUserId(userId, pageable)
                .map(cardMapper::cardToCardResponse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Возвращает постраничный список карт конкретного пользователя по его имени.
     * Сначала находит пользователя по имени, затем запрашивает его карты.
     * </p>
     *
     * @param userName имя пользователя
     * @param pageable параметры пагинации
     * @return страница с картами пользователя в формате {@link CardResponse}
     * @throws ResourceNotFoundException если пользователь с указанным именем не найден
     */
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

    /**
     * Проверяет права доступа текущего пользователя к карте.
     * <p>
     * Логика проверки:
     * <ul>
     *   <li>Если пользователь администратор - доступ разрешен</li>
     *   <li>Если пользователь владелец карты - доступ разрешен</li>
     *   <li>В остальных случаях - доступ запрещен</li>
     * </ul>
     * </p>
     *
     * @param card карта, к которой запрашивается доступ
     * @param currentUser данные текущего аутентифицированного пользователя
     * @throws UnauthorizedAccessException если у пользователя нет прав доступа к карте
     */
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