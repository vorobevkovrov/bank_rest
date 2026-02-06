package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardRequest;
import com.example.bankcards.entity.CardRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRequestRepository extends JpaRepository<CardRequest, Long> {

    List<CardRequest> findByStatus(CardRequestStatus status);

    boolean existsByCardAndStatus(Card card, CardRequestStatus status);
}

