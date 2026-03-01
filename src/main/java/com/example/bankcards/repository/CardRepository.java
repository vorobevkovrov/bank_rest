package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findAll(Pageable pageable);

    Page<Card> findByUserId(Long userId, Pageable pageable);

    boolean existsByCardNumberLastFourAndUserId(String lastFour, Long userId);

    Optional<Card> findByIdAndUser(Long id, User user);
}