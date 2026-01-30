package com.example.bankcards.config;

import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.util.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр аутентификации на основе JWT (JSON Web Token) для Spring Security.
 *
 * <p>Этот фильтр перехватывает все входящие HTTP запросы и выполняет аутентификацию пользователей
 * на основе JWT токена, передаваемого в заголовке Authorization. Фильтр расширяет
 * {@link OncePerRequestFilter}, гарантируя однократное выполнение для каждого запроса.</p>
 *
 * <h2>Основные функции:</h2>
 * <ul>
 *   <li>Извлечение JWT токена из заголовка Authorization</li>
 *   <li>Валидация JWT токена с использованием {@link JwtService}</li>
 *   <li>Загрузка данных пользователя через {@link UserDetailsServiceImpl}</li>
 *   <li>Установка объекта аутентификации в {@link SecurityContextHolder}</li>
 *   <li>Исключение публичных endpoints из процесса аутентификации</li>
 * </ul>
 *
 * <h2>Поток обработки запроса:</h2>
 * <ol>
 *   <li>Проверка, нужно ли фильтровать текущий запрос ({@link #shouldNotFilter(HttpServletRequest)})</li>
 *   <li>Извлечение JWT токена из заголовка Authorization ({@link #getJwtFromRequest(HttpServletRequest)})</li>
 *   <li>Извлечение имени пользователя из токена</li>
 *   <li>Загрузка {@link UserDetails} по имени пользователя</li>
 *   <li>Валидация токена и создание {@link UsernamePasswordAuthenticationToken}</li>
 *   <li>Установка аутентификации в {@link SecurityContextHolder}</li>
 *   <li>Продолжение цепочки фильтров</li>
 * </ol>
 *
 * <h2>Формат JWT токена:</h2>
 * <pre>
 * Authorization: Bearer {jwt-token}
 * Пример: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * </pre>
 *
 * <h2>Пример конфигурации в SecurityConfig:</h2>
 * <pre>
 * {@code
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
 * }
 * </pre>
 *
 * @see OncePerRequestFilter
 * @see JwtService
 * @see UserDetailsServiceImpl
 * @see SecurityContextHolder
 * @see UsernamePasswordAuthenticationToken
 *
 * @author [Имя разработчика/команды]
 * @version 1.0
 * @since 2024-01-15
 *
 * @component Spring компонент, автоматически обнаруживаемый при сканировании компонентов
 * @see Component
 */


/**
 * Сервис для работы с JWT токенами.
 * <p>Используется для извлечения данных из токена и проверки его валидности.</p>
 *
 * @see JwtService#extractUsername(String)
 * @see JwtService#isTokenValid(String, UserDetails)
 */

/**
 * Сервис для загрузки данных пользователя.
 * <p>Обеспечивает получение {@link UserDetails} по имени пользователя
 * для последующей аутентификации и проверки прав доступа.</p>
 *
 * @see UserDetailsServiceImpl#loadUserByUsername(String)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    /**
     * Основной метод обработки фильтра для каждого HTTP запроса.
     *
     * <p>Выполняет следующие шаги:</p>
     * <ol>
     *   <li>Извлекает JWT токен из заголовка Authorization</li>
     *   <li>Если токен присутствует и пользователь еще не аутентифицирован:</li>
     *   <li>Извлекает имя пользователя из токена</li>
     *   <li>Загружает данные пользователя из UserDetailsService</li>
     *   <li>Проверяет валидность токена</li>
     *   <li>Создает объект аутентификации и устанавливает его в SecurityContext</li>
     * </ol>
     *
     * <p>Все исключения перехватываются и логируются, но не прерывают выполнение цепочки фильтров,
     * чтобы обеспечить graceful degradation.</p>
     *
     * @param request     HTTP запрос
     * @param response    HTTP ответ
     * @param filterChain цепочка фильтров Spring Security для продолжения обработки
     * @throws ServletException если возникает ошибка сервлета
     * @throws IOException      если возникает ошибка ввода-вывода
     * @see OncePerRequestFilter#doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)
     * @see SecurityContextHolder#getContext()
     * @see UsernamePasswordAuthenticationToken
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Извлечение JWT токена из запроса
            final String jwt = getJwtFromRequest(request);

            // Проверка наличия токена и отсутствия аутентификации в контексте
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Извлечение имени пользователя из токена
                final String username = jwtService.extractUsername(jwt);

                if (username != null) {
                    // Загрузка данных пользователя
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

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
                        log.debug("Authenticated user: {}", username);
                    }
                }
            }
        } catch (Exception ex) {
            // Логирование ошибки аутентификации без прерывания обработки запроса
            log.error("Failed to set user authentication in security context", ex);
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
     * @param request HTTP запрос
     * @return JWT токен без префикса "Bearer " или {@code null}, если токен отсутствует
     * @example <pre>
     * Вход:  request.getHeader("Authorization") = "Bearer eyJhbGciOiJIUzI1NiIs..."
     * Выход: "eyJhbGciOiJIUzI1NiIs..."
     * </pre>
     * @see HttpServletRequest#getHeader(String)
     * @see StringUtils#hasText(String)
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Удаление префикса "Bearer" (7 символов)
            return bearerToken.substring(7);
        }
        return null;
        // TODO null bad
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
     *   <li>{@code /api/v1/auth/**} - endpoints аутентификации (логин, регистрация)</li>
     *   <li>{@code /swagger-ui/**} - Swagger UI документация</li>
     *   <li>{@code /v3/api-docs/**} - OpenAPI спецификация</li>
     *   <li>{@code /api-docs/**} - альтернативный путь к документации API</li>
     *   <li>{@code /actuator/health} - health check endpoint для мониторинга</li>
     * </ul>
     *
     * @param request HTTP запрос для проверки
     * @return {@code true} если фильтр НЕ должен обрабатывать запрос,
     * {@code false} если фильтр должен обработать запрос
     * @example <pre>
     * /api/v1/auth/login → true (не фильтровать)
     * /api/v1/cards → false (фильтровать)
     * /swagger-ui/index.html → true (не фильтровать)
     * </pre>
     * @see OncePerRequestFilter#shouldNotFilter(HttpServletRequest)
     * @see HttpServletRequest#getServletPath()
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/api/v1/auth/") ||      // Endpoints аутентификации
                path.startsWith("/swagger-ui/") ||      // Swagger UI
                path.startsWith("/v3/api-docs/") ||     // OpenAPI спецификация
                path.startsWith("/api-docs/") ||        // Альтернативная документация
                path.equals("/actuator/health");        // Health check
    }
}