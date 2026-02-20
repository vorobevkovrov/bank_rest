package com.example.bankcards.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;


import com.example.bankcards.dto.request.AuthenticationRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthenticationResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления аутентификацией и регистрацией пользователей.
 * <p>
 * Предоставляет REST API для регистрации новых пользователей, аутентификации,
 * получения информации о текущем пользователе и создания администраторов.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API для аутентификации и регистрации")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    /**
     * Регистрирует нового обычного пользователя в системе.
     * <p>
     * Этот эндпоинт доступен без аутентификации. Регистрация пользователей с ролью ADMIN
     * через этот метод запрещена и вызовет исключение. Для создания администраторов
     * используйте {@link #registerAdmin(RegisterRequest, UserDetails)}.
     * </p>
     *
     * @param request данные для регистрации (username, password, email, role)
     * @return ResponseEntity с токеном аутентификации и информацией о пользователе
     * @throws org.springframework.security.access.AccessDeniedException  если попытка создать администратора
     * @throws com.example.bankcards.exception.UserAlreadyExistsException если пользователь с таким username уже существует
     */
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает новую учетную запись с ролью USER. Администраторы не могут быть созданы через этот эндпоинт.",
            tags = {"Authentication", "Public"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован"),
                    @ApiResponse(responseCode = "400", description = "Неверные данные регистрации"),
                    @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Регистрация нового пользователя: {}", request.getUsername());
        if (request.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN can create other admins");
        }
        return ResponseEntity.ok(authenticationService.register(request));
    }

    /**
     * Аутентифицирует пользователя в системе.
     * <p>
     * Проверяет учетные данные пользователя и при успешной аутентификации
     * возвращает JWT токен для дальнейших запросов.
     * </p>
     *
     * @param request данные для аутентификации (username, password)
     * @return ResponseEntity с JWT токеном и информацией о пользователе
     * @throws org.springframework.security.authentication.BadCredentialsException если неверные учетные данные
     */
    @Operation(summary = "Аутентификация пользователя")
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    /**
     * Создает нового пользователя с ролью ADMIN.
     * <p>
     * Этот эндпоинт доступен только для уже аутентифицированных администраторов.
     * Создание администраторов через обычную регистрацию невозможно из соображений безопасности.
     * </p>
     *
     * @param request     данные для регистрации нового администратора
     * @param userDetails информация о текущем аутентифицированном пользователе
     * @return ResponseEntity с токеном аутентификации и информацией о новом администраторе
     * @throws org.springframework.security.access.AccessDeniedException  если текущий пользователь не является ADMIN
     * @throws com.example.bankcards.exception.UserAlreadyExistsException если пользователь с таким username уже существует
     */
    @Operation(summary = "Создание администратора (только для существующих админов)")
    @SecurityRequirement(name = "BearerAuthentication")
    @PostMapping("/register/admin")
    public ResponseEntity<AuthenticationResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = authenticationService.findByUsername(userDetails.getUsername());
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create other admins");
        }
        request.setRole(Role.ADMIN);
        return ResponseEntity.ok(authenticationService.register(request));
    }

    /**
     * Возвращает информацию о текущем аутентифицированном пользователе.
     * <p>
     * Пароль пользователя исключается из ответа по соображениям безопасности.
     * </p>
     *
     * @param userDetails информация о текущем аутентифицированном пользователе
     * @return ResponseEntity с данными пользователя (без пароля)
     * @throws com.example.bankcards.exception.ResourceNotFoundException если пользователь не найден
     */
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

    /**
     * Выполняет выход пользователя из системы.
     * <p>
     * В текущей реализации просто возвращает успешный ответ.
     * Для полноценной реализации требуется добавить инвалидацию JWT токена
     * (например, через черный список токенов).
     * </p>
     *
     * @param token JWT токен в формате "Bearer &lt;token&gt;"
     * @return ResponseEntity с пустым телом и статусом 200 OK
     */
    @Operation(summary = "Выход из системы")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok().build();
    }
}