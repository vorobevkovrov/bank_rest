package com.example.bankcards.controller;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@PreAuthorize("hasRole('ROLE_USER')")
@RestController
@RequestMapping("api/v1/cards")
public class UserCartController {
    // Просмотр своих карт
    // Запрос на блокировку
    // Пагинация и поиск

    @GetMapping("/all")
    public void getAllCards(User user) {
        log.info("public void GetAllCards user{}", user);
    }

    @PostMapping("/block")
    public void blockCard(Card card) {
        log.info("public void blockCard(Card card) {}", card);
    }

    @PostMapping("/transfer")
    public void transfer(Card from, Card to) {
        log.info("public void transfer(from,  to) {} {}", from, to);
        //return transferService.transfer(request, getCurrentUserId());
    }

    @GetMapping("/balance")
    public void getBalance(Card card) {
        log.info("public void getBalance(Card card) {}", card);
    }


}
