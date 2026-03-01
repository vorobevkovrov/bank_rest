package com.example.bankcards.config;

import com.example.bankcards.exception.ErrorResponse;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.util.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Фильтр аутентификации на основе JWT (JSON Web Token) для Spring Security.
 * Этот фильтр перехватывает все входящие HTTP запросы и выполняет аутентификацию пользователей
 * на основе JWT токена, передаваемого в заголовке Authorization. Фильтр расширяет
 * {@link OncePerRequestFilter}, гарантируя однократное выполнение для каждого запроса.
 *
 * <h2>Основные функции:</h2>
 * <ul>
 *   <li>Извлечение JWT токена из заголовка Authorization</li>
 *   <li>Валидация JWT токена с использованием {@link JwtService}</li>
 *   <li>Загрузка данных пользователя через {@link UserDetailsServiceImpl}</li>
 *   <li>Установка объекта аутентификации в {@link SecurityContextHolder}</li>
 *   <li>Обработка ошибок валидации токена (истекший/невалидный токен)</li>
 *   <li>Исключение публичных endpoints из процесса аутентификации</li>
 * </ul>
 *
 * <h2>Поток обработки запроса:</h2>
 * <ol>
 *   <li>Проверка необходимости фильтрации запроса ({@link #shouldNotFilter(HttpServletRequest)})</li>
 *   <li>Извлечение JWT токена из заголовка Authorization ({@link #getJwtFromRequest(HttpServletRequest)})</li>
 *   <li>При наличии токена - извлечение имени пользователя</li>
 *   <li>Загрузка {@link UserDetails} по имени пользователя</li>
 *   <li>Валидация токена</li>
 *   <li>При успешной валидации - создание и установка аутентификации</li>
 *   <li>При ошибках валидации - формирование ошибки через {@link #handleInvalidToken} или {@link #handleExpiredToken}</li>
 *   <li>Продолжение цепочки фильтров</li>
 * </ol>
 *
 * <h2>Обработка ошибок:</h2>
 * <ul>
 *   <li>{@link ExpiredJwtException} - перехватывается и обрабатывается методом {@link #handleExpiredToken},
 *       возвращает 401 Unauthorized с информацией об истекшем токене</li>
 *   <li>{@link JwtException} - перехватывается и обрабатывается методом {@link #handleInvalidToken},
 *       возвращает 401 Unauthorized с информацией о невалидном токене</li>
 * </ul>
 *
 * <h2>Формат JWT токена:</h2>
 * <pre>
 * Authorization: Bearer {jwt-token}
 * Пример: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * </pre>
 *
 * <h2>Пример конфигурации в SecurityConfig:</h2>
 * <pre>{@code
 * @Configuration
 * @EnableWebSecurity
 * public class SecurityConfig {
 *
 *     @Bean
 *     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 *         http
 *             .addFilterBefore(jwtAuthenticationFilter,
 *                 UsernamePasswordAuthenticationFilter.class)
 *             // остальная конфигурация
 *     }
 * }
 * }</pre>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see OncePerRequestFilter
 * @see JwtService
 * @see UserDetailsServiceImpl
 * @see SecurityContextHolder
 * @see UsernamePasswordAuthenticationToken
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    /**
     * Основной метод обработки фильтра для каждого HTTP запроса.
     *
     * <p>Выполняет следующие шаги:</p>
     * <ol>
     *   <li>Извлекает JWT токен из заголовка Authorization с помощью {@link #getJwtFromRequest}</li>
     *   <li>Если токен присутствует и пользователь еще не аутентифицирован:
     *     <ul>
     *       <li>Извлекает имя пользователя из токена через {@link JwtService#extractUsername}</li>
     *       <li>Загружает данные пользователя через {@link UserDetailsServiceImpl#loadUserByUsername}</li>
     *       <li>Проверяет валидность токена через {@link JwtService#isTokenValid}</li>
     *       <li>При успешной проверке создает {@link UsernamePasswordAuthenticationToken}</li>
     *       <li>Устанавливает аутентификацию в {@link SecurityContextHolder}</li>
     *     </ul>
     *   </li>
     *   <li>При возникновении {@link ExpiredJwtException} вызывает {@link #handleExpiredToken}</li>
     *   <li>При возникновении {@link JwtException} вызывает {@link #handleInvalidToken}</li>
     *   <li>Продолжает обработку запроса следующими фильтрами</li>
     * </ol>
     *
     * <p>В случае ошибок валидации токена выполнение цепочки прерывается (return после обработки ошибки),
     * и клиенту возвращается соответствующий ответ с HTTP статусом 401.</p>
     *
     * @param request     HTTP запрос
     * @param response    HTTP ответ
     * @param filterChain цепочка фильтров Spring Security для продолжения обработки
     * @throws ServletException если возникает ошибка сервлета
     * @throws IOException      если возникает ошибка ввода-вывода
     * @see #getJwtFromRequest(HttpServletRequest)
     * @see #handleExpiredToken(ExpiredJwtException, HttpServletResponse, HttpServletRequest)
     * @see #handleInvalidToken(JwtException, HttpServletResponse, HttpServletRequest)
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getServletPath();
        log.debug("Processing request: {}", path);
        try {
            // Извлечение JWT токена из запроса
            final String jwt = getJwtFromRequest(request);
            log.debug("JWT token extracted: {}", jwt != null ? "present" : "null");
            // Проверка наличия токена и отсутствия аутентификации в контексте
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("No valid JWT token found");
                // Извлечение имени пользователя из токена
                final String username = jwtService.extractUsername(jwt);
                log.debug("Username extracted from JWT: {}", username);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    log.debug("UserDetails loaded: {}", userDetails != null ? userDetails.getUsername() : "null");
                    // Проверка валидности токена
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        // Создание объекта аутентификации
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        // Добавление деталей аутентификации
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // Установка аутентификации в контекст безопасности
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("Successfully authenticated user: {} with authorities: {}",
                                username, userDetails.getAuthorities());
                    }
                }
            }
        } catch (ExpiredJwtException ex) {
            handleExpiredToken(ex, response, request);
            // Логирование ошибки аутентификации без прерывания обработки запроса
            log.error("JWT Token expired {}", ex.getMessage());
            return;

        } catch (JwtException ex) {
            handleInvalidToken(ex, response, request);
            log.error("JWT Token invalid {}", ex.getMessage());
            return; // Прерываем выполнение
        }
        // Продолжение обработки запроса следующими фильтрами в цепочке
        filterChain.doFilter(request, response);
    }

    /**
     * Извлекает JWT токен из заголовка Authorization HTTP запроса.
     *
     * <p>Ожидает токен в формате: {@code Bearer {jwt-token}}.
     * Если заголовок отсутствует или имеет неверный формат, возвращает {@code null}.</p>
     *
     * <p>Метод выполняет следующие проверки:</p>
     * <ul>
     *   <li>Наличие заголовка "Authorization"</li>
     *   <li>Непустое значение заголовка</li>
     *   <li>Наличие префикса "Bearer "</li>
     * </ul>
     *
     * @param request HTTP запрос
     * @return JWT токен без префикса "Bearer " или {@code null}, если токен отсутствует
     * @example <pre>
     * Вход:  request.getHeader("Authorization") = "Bearer eyJhbGciOiJIUzI1NiIs..."
     * Выход: "eyJhbGciOiJIUzI1NiIs..."
     *
     * Вход:  request.getHeader("Authorization") = null
     * Выход: null
     * </pre>
     * @see HttpServletRequest#getHeader(String)
     * @see StringUtils#hasText(String)
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization header: {}", bearerToken);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Удаление префикса "Bearer" (7 символов)
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Определяет, должен ли фильтр обрабатывать указанный запрос.
     *
     * <p>Запросы к публичным endpoints исключаются из процесса аутентификации.
     * Это повышает производительность и предотвращает ненужную обработку
     * публично доступных ресурсов.</p>
     *
     * <h2>Публичные endpoints (не требуют аутентификации):</h2>
     * <ul>
     *   <li>{@code /api/v1/auth/login} - endpoint входа в систему</li>
     *   <li>{@code /swagger-ui/**} - Swagger UI документация</li>
     *   <li>{@code /v3/api-docs/**} - OpenAPI спецификация</li>
     *   <li>{@code /api-docs/**} - альтернативный путь к документации API</li>
     *   <li>{@code /actuator/health} - health check endpoint для мониторинга</li>
     * </ul>
     *
     * @param request HTTP запрос для проверки
     * @return {@code true} если фильтр НЕ должен обрабатывать запрос (публичный endpoint),
     * {@code false} если фильтр должен обработать запрос (требуется аутентификация)
     * @example <pre>
     * /api/v1/auth/login    → true (не фильтровать)
     * /api/v1/cards         → false (фильтровать)
     * /swagger-ui/index.html → true (не фильтровать)
     * /actuator/health      → true (не фильтровать)
     * </pre>
     * @see OncePerRequestFilter#shouldNotFilter(HttpServletRequest)
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
// Исключаем только PUBLIC endpoints, НЕ требующие аутентификации
        return path.startsWith("/api/v1/auth/login") || // Endpoints аутентификации
                //     path.startsWith("/api/v1/auth/register/admin") ||
                path.startsWith("/swagger-ui/") ||      // Swagger UI
                path.startsWith("/v3/api-docs/") ||     // OpenAPI спецификация
                path.startsWith("/api-docs/") ||        // Альтернативная документация
                path.equals("/actuator/health");        // Health check
    }

    /**
     * Обрабатывает случай невалидного JWT токена.
     *
     * <p>Формирует и отправляет HTTP ответ с статусом 401 (Unauthorized) и деталями ошибки
     * в формате JSON. Используется для обработки любых JWT исключений, кроме {@link ExpiredJwtException}.</p>
     *
     * <p>Структура ответа:</p>
     * <pre>
     * {
     *   "timestamp": "2024-01-15T10:30:45",
     *   "status": 401,
     *   "error": "Invalid token",
     *   "message": "JWT signature does not match...",
     *   "path": "/api/v1/cards"
     * }
     * </pre>
     *
     * @param ex       исключение JwtException с деталями ошибки
     * @param response HTTP ответ для отправки клиенту
     * @param request  HTTP запрос для получения информации о пути
     * @throws IOException если возникает ошибка при записи в response
     * @see ErrorResponse
     * @see HttpStatus#UNAUTHORIZED
     */
    private void handleInvalidToken(JwtException ex,
                                    HttpServletResponse response,
                                    HttpServletRequest request) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Invalid token")
                .message(ex.getMessage())
                .path(request.getServletPath())
                .build();

        log.error("Invalid token for path: {}", request.getServletPath());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    /**
     * Обрабатывает случай истекшего JWT токена.
     *
     * <p>Формирует и отправляет HTTP ответ с статусом 401 (Unauthorized) и деталями ошибки
     * в формате JSON. Специализированная обработка для {@link ExpiredJwtException}.</p>
     *
     * <p>Структура ответа:</p>
     * <pre>
     * {
     *   "timestamp": "2024-01-15T10:30:45",
     *   "status": 401,
     *   "error": "Token expired",
     *   "message": "JWT expired at 2024-01-15T10:00:00. Current time: 2024-01-15T10:30:45",
     *   "path": "/api/v1/cards"
     * }
     * </pre>
     *
     * <p>В отличие от {@link #handleInvalidToken}, этот метод использует специфичную
     * информацию из {@link ExpiredJwtException}, такую как время истечения токена,
     * для формирования более информативного сообщения об ошибке.</p>
     *
     * @param ex       исключение ExpiredJwtException с деталями об истекшем токене
     * @param response HTTP ответ для отправки клиенту
     * @param request  HTTP запрос для получения информации о пути
     * @throws IOException если возникает ошибка при записи в response
     * @see ExpiredJwtException
     * @see ErrorResponse
     * @see HttpStatus#UNAUTHORIZED
     */
    private void handleExpiredToken(ExpiredJwtException ex,
                                    HttpServletResponse response,
                                    HttpServletRequest request) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Token expired")
                .message(ex.getMessage())
                .path(request.getServletPath())
                .build();

        log.error("Token expired for path: {}", request.getServletPath());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}