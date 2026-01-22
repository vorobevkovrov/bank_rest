package com.example.bankcards.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;


import com.example.bankcards.dto.request.AuthenticationRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthenticationResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.AuthenticationService;
import com.example.bankcards.util.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API для аутентификации и регистрации")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    @Operation(summary = "Регистрация нового пользователя")
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        // По умолчанию регистрируем как USER
        // ADMIN может быть создан только другим ADMIN через отдельный эндпоинт
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @Operation(summary = "Аутентификация пользователя")
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @Operation(summary = "Обновление JWT токена")
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestHeader("Authorization") String refreshToken
    ) {
        // Убираем "Bearer " префикс если есть
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken));
    }

    @Operation(summary = "Создание администратора (только для существующих админов)")
    @PostMapping("/register/admin")
    public ResponseEntity<AuthenticationResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String token
    ) {
        // Проверяем, что текущий пользователь - ADMIN
        String username = jwtService.extractUsername(token.replace("Bearer ", ""));
        User currentUser = authenticationService.findByUsername(username);

        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create other admins");
        }

        // Устанавливаем роль ADMIN для нового пользователя
        request.setRole(Role.ADMIN);

        return ResponseEntity.ok(authenticationService.register(request));
    }

    @Operation(summary = "Получение информации о текущем пользователе")
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(
            @RequestHeader("Authorization") String token
    ) {
        String username = jwtService.extractUsername(token.replace("Bearer ", ""));
        User user = authenticationService.findByUsername(username);

        // Очищаем пароль перед отправкой
        user.setPassword(null);

        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Выход из системы")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token
    ) {
        // В простой реализации JWT статичен,
        // но можно добавить token в blacklist или уменьшить срок жизни
        return ResponseEntity.ok().build();
    }
}