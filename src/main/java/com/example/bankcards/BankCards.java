package com.example.bankcards;

import com.example.bankcards.dto.response.AuthenticationResponse;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(AuthenticationResponse.class)
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

