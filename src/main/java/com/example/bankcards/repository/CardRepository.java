package com.example.bankcards.repository;

import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.bankcards.entity.Card;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    // Для админа
    Page<Card> findAll(Pageable pageable);

    Page<Card> findByUserId(Long userId, Pageable pageable);

    Page<Card> findByExpiryDateBefore(LocalDate date, Pageable pageable);

    List<Card> findByExpiryDateBefore(LocalDate date);

    boolean existsByCardNumberLastFourAndUserId(String lastFour, Long userId);

    // Поиск по пользователю и статусу
    Page<Card> findByUserIdAndStatus(Long userId, CardStatus status, Pageable pageable);


    // Для пользователя
    Page<Card> findByUser(User user, Pageable pageable);

    Optional<Card> findByIdAndUser(Long id, User user);

    List<Card> findByUserAndStatus(User user, CardStatus status);

    // Проверка существования карты у пользователя
    boolean existsByIdAndUser(Long id, User user);
}