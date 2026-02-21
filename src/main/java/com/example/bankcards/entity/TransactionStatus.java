package com.example.bankcards.entity;

/**
 * Перечисление статусов финансовой транзакции.
 * <p>
 * Определяет возможные состояния транзакции в процессе ее выполнения:
 * <ul>
 *   <li>{@link #PENDING} - транзакция создана, но еще не обработана</li>
 *   <li>{@link #COMPLETED} - транзакция успешно выполнена</li>
 *   <li>{@link #FAILED} - транзакция завершилась ошибкой</li>
 *   <li>{@link #CANCELLED} - транзакция отменена</li>
 * </ul>
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @since 1.0
 *
 * @see Transaction
 */
public enum TransactionStatus {
    /** Транзакция в процессе обработки */
    PENDING,

    /** Транзакция успешно завершена */
    COMPLETED,

    /** Транзакция не выполнена из-за ошибки */
    FAILED,

    /** Транзакция отменена до завершения */
    CANCELLED
}