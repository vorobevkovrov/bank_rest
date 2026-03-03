package com.example.bankcards;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankCards {
    public static void main(String[] args) {
        // Загружаем .env файл
        Dotenv dotenv = Dotenv.configure()
                .directory(".")  // текущая директория
                .ignoreIfMissing()
                .load();
        // Устанавливаем все переменные
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
        SpringApplication.run(BankCards.class, args);
    }
}

