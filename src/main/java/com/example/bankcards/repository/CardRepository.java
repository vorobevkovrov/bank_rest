package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    // Для админа
    Page<Card> findAll(Pageable pageable);

    Page<Card> findByUserId(Long userId, Pageable pageable);

    Page<Card> findByCardHolderName(String userName, Pageable pageable);

    List<Card> findByExpiryDateBefore(LocalDate date);

    boolean existsByCardNumberLastFourAndUserId(String lastFour, Long userId);

    // Для пользователя

    Optional<Card> findByIdAndUser(Long id, User user);

    // Проверка существования карты у пользователя
    boolean existsByIdAndUser(Long id, User user);
}