package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.CardRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CardService {
    private final CardRepository cardRepo;

    public void createCard() {
    }

    public void activateCard(Card card) {
       // cardRepo.activateCard(card);
    }

    public void blocCard() {
    }

    public void deleteCard() {
    }
}
