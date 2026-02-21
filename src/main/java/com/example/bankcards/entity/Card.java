package com.example.bankcards.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Сущность, представляющая банковскую карту в системе.
 * <p>
 * Содержит информацию о карте: номер, срок действия, статус, баланс,
 * а также связь с владельцем карты.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @since 1.0
 *
 * @see User
 * @see CardStatus
 */
@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Table(name = "cards")
public class Card {
    /**
     * Уникальный идентификатор карты.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Зашифрованный номер карты.
     * <p>
     * <strong>Важно:</strong> Номер карты хранится в зашифрованном виде
     * в соответствии с требованиями PCI DSS.
     * </p>
     */
    @Column(name = "card_number_encrypted")
    private String cardNumber;

    /**
     * Последние 4 цифры номера карты.
     * Хранятся в открытом виде для отображения в интерфейсе.
     */
    @Column(name = "card_number_last_four")
    private String cardNumberLastFour;

    /**
     * Дата истечения срока действия карты.
     */
    @Column(name = "expiry_date")
    private Date expiryDate;

    /**
     * Текущий статус карты.
     * @see CardStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CardStatus status;

    /**
     * Текущий баланс карты.
     * Хранится с точностью до 2 знаков после запятой.
     */
    @Column(name = "balance", precision = 19, scale = 2)
    private BigDecimal balance;

    /**
     * Владелец карты.
     * Связь многие-к-одному с сущностью {@link User}.
     * Использует JsonBackReference для избежания циклических ссылок при сериализации.
     */
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Имя держателя карты (как указано на карте).
     */
    @Column(name = "holder_name", nullable = false)
    private String cardHolderName;

    /**
     * Проверяет, активна ли карта.
     *
     * @return true если статус карты {@link CardStatus#ACTIVE}, иначе false
     */
    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }

    /**
     * Проверяет, заблокирована ли карта.
     *
     * @return true если статус карты {@link CardStatus#BLOCKED}, иначе false
     */
    public boolean isBlocked() {
        return status == CardStatus.BLOCKED;
    }
}