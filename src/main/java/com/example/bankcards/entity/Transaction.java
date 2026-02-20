package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность, представляющая финансовую транзакцию между картами.
 * <p>
 * Содержит информацию о переводе средств: карта-источник, карта-назначение,
 * сумма, статус и время выполнения.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see Card
 * @see TransactionStatus
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "transactions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    /**
     * Уникальный идентификатор транзакции.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Карта-источник (с которой списываются средства).
     * Связь многие-к-одному с сущностью {@link Card}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_card_id", nullable = false)
    private Card fromCard;

    /**
     * Карта-назначение (на которую зачисляются средства).
     * Связь многие-к-одному с сущностью {@link Card}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_id", nullable = false)
    private Card toCard;

    /**
     * Сумма транзакции.
     * Должна быть положительным числом.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Статус транзакции.
     *
     * @see TransactionStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /**
     * Дата и время создания транзакции.
     * Устанавливается автоматически при сохранении.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}