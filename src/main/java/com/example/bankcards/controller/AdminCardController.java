package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import com.example.bankcards.repository.CardRepo;
import com.example.bankcards.service.CardService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@Slf4j
@RestController
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequestMapping("/api/v1/admin/cards")
public class AdminCardController {
    private final CardService cardService;

    // CRUD для всех карт
    // Блокировка/активация карт
    @PostMapping("/create")
    public void createBankCard() {
        log.info("public void CreateBankCard(){} ");
    }

    @PutMapping("/activate")
    public void activateBankCard(Card card) {
        cardService.activateCard(card);
    }
    @PutMapping("/update")
    public void updateCard(Card card){

    }
    @DeleteMapping("/delete")
    public void deleteCard(Card card){

    }
}
