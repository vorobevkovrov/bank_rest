package com.example.bankcards.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Сущность, представляющая пользователя системы.
 * <p>
 * Содержит учетные данные пользователя, его роль и связанные с ним карты.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @since 1.0
 *
 * @see Role
 * @see Card
 */
@Builder
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Table(name = "users")
public class User {
    /**
     * Уникальный идентификатор пользователя.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Уникальное имя пользователя для входа в систему.
     */
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    /**
     * Зашифрованный пароль пользователя.
     * Игнорируется при JSON сериализации для безопасности.
     */
    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Роль пользователя в системе.
     * @see Role
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * Список карт, принадлежащих пользователю.
     * Связь один-ко-многим с сущностью {@link Card}.
     * Использует JsonManagedReference для управления сериализацией.
     */
    @JsonManagedReference
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Card> cards;
}