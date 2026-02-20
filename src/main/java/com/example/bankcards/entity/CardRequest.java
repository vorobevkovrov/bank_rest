package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;

/**
 * Сущность, представляющая запрос на операцию с картой.
 * <p>
 * Используется для обработки запросов пользователей, требующих подтверждения администратора.
 * Например, запрос на блокировку карты создается пользователем и рассматривается администратором.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see Card
 * @see User
 * @see CardRequestType
 * @see CardRequestStatus
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "card_requests")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRequest {

    /**
     * Уникальный идентификатор запроса.
     * Автоматически генерируется базой данных.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Карта, к которой относится запрос.
     * Связь многие-к-одному с сущностью {@link Card}.
     * Загружается лениво (LAZY) для оптимизации производительности.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /**
     * Пользователь, создавший запрос.
     * Связь многие-к-одному с сущностью {@link User}.
     * Загружается лениво (LAZY) для оптимизации производительности.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Тип запроса (например, BLOCK - блокировка карты).
     * Хранится в БД как строка.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private CardRequestType requestType;

    /**
     * Статус запроса:
     * <ul>
     *   <li>PENDING - ожидает рассмотрения</li>
     *   <li>APPROVED - одобрен администратором</li>
     *   <li>REJECTED - отклонен администратором</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardRequestStatus status;

    /**
     * Дата и время создания запроса.
     * Устанавливается автоматически при сохранении и не обновляется.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления запроса.
     * Автоматически обновляется при каждом изменении.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}