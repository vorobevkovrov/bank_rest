package com.example.bankcards.controller;

import com.example.bankcards.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("api/v1/cart")
public class CartController {
    @GetMapping("/all")
    public void GetAllCards(User user) {
        log.info("public void GetAllCards user{}", user);
    }
}
