package com.example.bankcards.util;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.exception.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Реализация валидатора для операций перевода средств между картами.
 * Выполняет комплексную проверку всех условий, необходимых для выполнения безопасного перевода.
 *
 * Валидация включает проверки:
 * <ul>
 *   <li>Принадлежности карт одному владельцу</li>
 *   <li>Прав доступа текущего пользователя</li>
 *   <li>Что карты не совпадают</li>
 *   <li>Достаточности средств на карте отправителя</li>
 *   <li>Активности обеих карт (не заблокированы и активны)</li>
 * </ul>
 */
@Component
@Slf4j
public class TransferValidatorImpl implements TransferValidator {

    /**
     * Выполняет полную валидацию перевода средств между картами.
     * Последовательно проверяет все необходимые условия для выполнения перевода.
     *
     * @param request объект запроса на перевод, содержащий сумму и идентификаторы карт
     * @param currentUser детали текущего аутентифицированного пользователя
     * @param fromCard карта-источник (с которой списываются средства)
     * @param toCard карта-назначение (на которую зачисляются средства)
     * @throws TransferValidationException если карты принадлежат разным владельцам
     * @throws UnauthorizedCardAccessException если текущий пользователь не имеет прав на карту
     * @throws SameCardTransferException если попытка перевода на ту же самую карту
     * @throws InsufficientFundsException если недостаточно средств на карте-источнике
     * @throws CardNotActiveException если карта-источник или карта-назначение не активна
     * @throws CardBlockedException если карта-источник или карта-назначение заблокирована
     */
    public void transferCardValidation(TransferRequest request, UserDetails currentUser,
                                       Card fromCard, Card toCard) {

        validateSameOwner(fromCard, toCard);
        validateCardOwnership(fromCard, currentUser);
        validateNotSameCard(fromCard, toCard);
        validateSufficientFunds(fromCard, request.amount());
        validateCardActive(fromCard, "source");
        validateCardActive(toCard, "destination");
    }

    /**
     * Проверяет, что обе карты принадлежат одному владельцу.
     *
     * @param fromCard карта-источник
     * @param toCard карта-назначение
     * @throws TransferValidationException если владельцы карт различаются
     */
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

    /**
     * Проверяет, что текущий пользователь имеет права доступа к карте.
     *
     * @param card проверяемая карта
     * @param currentUser детали текущего пользователя
     * @throws UnauthorizedCardAccessException если пользователь не является владельцем карты
     */
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

    /**
     * Проверяет, что карта-источник и карта-назначение не являются одной и той же картой.
     *
     * @param fromCard карта-источник
     * @param toCard карта-назначение
     * @throws SameCardTransferException если идентификаторы карт совпадают
     */
    private void validateNotSameCard(Card fromCard, Card toCard) {
        if (fromCard.getId().equals(toCard.getId())) {
            log.warn("Attempt to transfer to the same card: {}", fromCard.getId());
            throw new SameCardTransferException(
                    String.format("Cannot transfer from card %d to itself", fromCard.getId()));
        }
    }

    /**
     * Проверяет достаточность средств на карте-источнике для выполнения перевода.
     *
     * @param card карта-источник
     * @param amount сумма перевода
     * @throws InsufficientFundsException если доступный баланс меньше суммы перевода
     */
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

    /**
     * Проверяет, что карта активна и не заблокирована.
     *
     * @param card проверяемая карта
     * @param cardType тип карты ("source" или "destination") для информативного сообщения
     * @throws CardNotActiveException если карта не активна
     * @throws CardBlockedException если карта заблокирована
     */
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

    /**
     * Преобразует первую букву строки в заглавную.
     * Вспомогательный метод для форматирования сообщений об ошибках.
     *
     * @param str исходная строка
     * @return строка с заглавной первой буквой или исходная строка, если она null или пустая
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}