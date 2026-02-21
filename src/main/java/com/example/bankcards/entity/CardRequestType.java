package com.example.bankcards.entity;

/**
 * Перечисление типов запросов, которые могут создавать пользователи.
 * <p>
 * В текущей реализации поддерживается только один тип запроса - на блокировку карты.
 * В будущем может быть расширено для других типов операций.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @since 1.0
 *
 * @see CardRequest
 */
public enum CardRequestType {
    /** Запрос на блокировку карты */
    BLOCK
}