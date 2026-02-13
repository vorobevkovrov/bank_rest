package com.example.bankcards.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API для аутентификации и регистрации")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private static final String BEARER_PREFIX = "Bearer ";

    //TODO запрет на создание нового пользователя с ролью админ
    @Operation(summary = "Регистрация нового пользователя")
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @Operation(summary = "Аутентификация пользователя")
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @Operation(summary = "Создание администратора (только для существующих админов)")
    @SecurityRequirement(name = "BearerAuthentication")
    @PostMapping("/register/admin")
    public ResponseEntity<AuthenticationResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            // @RequestHeader("Authorization") String token
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        //  String username = jwtService.extractUsername(token.replace(BEARER_PREFIX, ""));
        // User currentUser = authenticationService.findByUsername(username);
        User currentUser = authenticationService.findByUsername(userDetails.getUsername());

        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create other admins");
        }
        request.setRole(Role.ADMIN);
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @Operation(summary = "Получение информации о текущем пользователе")
    @SecurityRequirement(name = "BearerAuthentication")
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Получение информации о пользователе: {}", userDetails.getUsername());
        User user = authenticationService.findByUsername(userDetails.getUsername());
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Выход из системы")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok().build();
    }
}