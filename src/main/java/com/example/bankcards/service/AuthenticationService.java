package com.example.bankcards.service;

import com.example.bankcards.util.JwtService;
import com.example.bankcards.dto.request.AuthenticationRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthenticationResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public AuthenticationResponse register(RegisterRequest request) {
        log.warn("⚠️  Пароли хранятся в незашифрованном виде! Это небезопасно!");

        // Проверяем, существует ли пользователь
        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            throw new RuntimeException("User already exists");
        }

        // Создаем нового пользователя (пароль хранится как есть)
        var user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword()) // ⚠️ Пароль в чистом виде!
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();

        userRepository.save(user);

        // Генерируем токены
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        log.info("User registered: {}", request.getUsername());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .username(user.getUsername())
                .role(user.getRole())
                .message("Registration successful")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.warn("⚠️  Проверка пароля в незашифрованном виде! Это небезопасно!");

        try {
            // Ищем пользователя
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));

            // ⚠️ Прямое сравнение паролей в незашифрованном виде
            if (!user.getPassword().equals(request.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }

            // Создаем Spring Security контекст для ручной аутентификации
            var authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    user.getPassword()
            );
            authenticationManager.authenticate(authentication);

            // Генерируем токены
            var accessToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);

            log.info("User authenticated: {}", request.getUsername());

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtService.getExpirationTime())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .message("Authentication successful")
                    .build();

        } catch (Exception e) {
            log.error("Authentication failed for user: {}", request.getUsername());
            throw new RuntimeException("Invalid credentials");
        }
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        try {
            if (username == null) {
                throw new RuntimeException("Invalid refresh token");
            }
            var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new RuntimeException("Invalid refresh token");
            }

            var newAccessToken = jwtService.generateToken(user);

            return AuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // тот же refresh token
                    .expiresIn(jwtService.getExpirationTime())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .message("Token refreshed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed");
        }
    }


    public AuthenticationResponse createAdmin(RegisterRequest request) {
        log.warn("⚠️  Создание администратора с незашифрованным паролем!");

        var admin = User.builder()
                .username(request.getUsername())
                .password(request.getPassword()) // ⚠️ Пароль в чистом виде!
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);

        var accessToken = jwtService.generateToken(admin);
        var refreshToken = jwtService.generateRefreshToken(admin);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .username(admin.getUsername())
                .role(admin.getRole())
                .message("Admin created successfully")
                .build();
    }

    public boolean validatePassword(String username, String password) {
        return userRepository.findByUsername(username)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }
}