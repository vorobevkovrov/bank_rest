package com.example.bankcards.service.impl;

import com.example.bankcards.exception.exceptions.InvalidCredentialsException;
import com.example.bankcards.exception.exceptions.UserAlreadyExistsException;
import com.example.bankcards.service.AuthenticationService;
import com.example.bankcards.util.JwtService;
import com.example.bankcards.dto.request.AuthenticationRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthenticationResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.exceptions.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Реализация сервиса аутентификации для управления регистрацией и аутентификацией пользователей.
 * Сервис предоставляет методы для регистрации новых пользователей и аутентификации существующих.
 *
 * <p>ВНИМАНИЕ: Текущая реализация хранит пароли в незашифрованном виде, что является серьезной уязвимостью безопасности.
 * Необходимо использовать кодирование паролей с помощью BCryptPasswordEncoder или аналогичного механизма.</p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see AuthenticationService
 * @see User
 * @see AuthenticationResponse
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Tag(name = "Authentication Service", description = "API для регистрации и аутентификации пользователей")
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Находит пользователя по имени пользователя.
     *
     * @param username имя пользователя для поиска
     * @return найденный пользователь
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Override
    @Operation(
            summary = "Поиск пользователя по имени",
            description = "Возвращает информацию о пользователе по указанному имени пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public User findByUsername(
            @Parameter(description = "Имя пользователя", required = true, example = "john_doe")
            String username
    ) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    /**
     * Регистрирует нового пользователя в системе.
     * Создает учетную запись пользователя с предоставленными учетными данными.
     *
     * @param request запрос на регистрацию, содержащий учетные данные пользователя
     * @return ответ аутентификации с токенами доступа и обновления
     * @throws UserAlreadyExistsException если пользователь с таким именем уже существует
     * @apiNote ВНИМАНИЕ: Этот метод хранит пароли в незашифрованном виде.
     * Необходимо исправить эту уязвимость безопасности.
     */
    @Override
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Регистрирует нового пользователя в системе и возвращает JWT токены"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Пользователь с таким именем уже существует")
    })
    public AuthenticationResponse register(
            @Parameter(description = "Данные для регистрации", required = true)
            RegisterRequest request
    ) {
        log.warn("⚠️  Пароли хранятся в незашифрованном виде! Это небезопасно!");
        Optional<User> existingUser = userRepository.findByUsername(request.username());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }
        var user = User.builder()
                .username(request.username())
                .password(request.password()) // ⚠️ Пароль в чистом виде!
                .role(request.role() != null ? request.role() : Role.USER)
                .build();

        userRepository.save(user);
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        log.info("User registered: {}", request.username());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .username(user.getUsername())
                .role(user.getRole())
                .message("Registration successful")
                .build();
    }

    /**
     * Аутентифицирует пользователя по предоставленным учетным данным.
     * Проверяет имя пользователя и пароль, затем выдает JWT токены.
     *
     * @param request запрос на аутентификацию, содержащий учетные данные
     * @return ответ аутентификации с токенами доступа и обновления
     * @throws InvalidCredentialsException если учетные данные неверны
     * @apiNote ВНИМАНИЕ: Этот метод проверяет пароли в незашифрованном виде.
     * Необходимо исправить эту уязвимость безопасности.
     */
    @Override
    @Operation(
            summary = "Аутентификация пользователя",
            description = "Аутентифицирует пользователя по имени и паролю, возвращает JWT токены"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аутентификация успешна"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    public AuthenticationResponse authenticate(
            @Parameter(description = "Учетные данные для аутентификации", required = true)
            AuthenticationRequest request
    ) {
        log.warn("⚠️  Проверка пароля в незашифрованном виде! Это небезопасно!");
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username, user with this username not found"));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        log.info("User authenticated: {}", request.username());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpirationTime())
                .username(user.getUsername())
                .role(user.getRole())
                .message("Authentication successful")
                .build();

//        } catch (Exception e) {
//            log.error("Authentication failed for user: {}", request.username());
//            throw new InvalidCredentialsException("Authentication failed: " + e.getMessage());
//        }
    }
}