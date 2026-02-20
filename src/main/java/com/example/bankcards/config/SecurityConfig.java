package com.example.bankcards.config;

import com.example.bankcards.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация безопасности приложения.
 * <p>
 * Этот класс настраивает Spring Security для приложения банковских карт.
 * Он определяет правила авторизации, механизм аутентификации через JWT,
 * настройки stateless сессий и цепочку фильтров безопасности.
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Hidden // Скрываем этот класс из документации Swagger, так как это конфигурация
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Создает и настраивает основную цепочку фильтров безопасности.
     * <p>
     * Конфигурация включает:
     * <ul>
     *   <li>Отключение CSRF защиты (для REST API)</li>
     *   <li>Настройка публичных эндпоинтов (аутентификация, Swagger)</li>
     *   <li>Ограничение доступа к админским эндпоинтам только для роли ADMIN</li>
     *   <li>Настройка stateless сессий</li>
     *   <li>Добавление JWT фильтра перед стандартным фильтром аутентификации</li>
     * </ul>
     * </p>
     *
     * @param http объект HttpSecurity для настройки
     * @return настроенная цепочка фильтров безопасности
     * @throws Exception если возникает ошибка при конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/authenticate",
                                "/api/v1/auth/register")
                        .permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/webjars/**",
                                "/swagger-resources/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**",
                                "/api/v1/admin/register/admin").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Создает провайдер аутентификации на основе DAO.
     * <p>
     * Использует {@link DaoAuthenticationProvider} для проверки учетных данных
     * пользователя через {@link UserDetailsServiceImpl} и сравнения паролей
     * с помощью {@link PasswordEncoder}.
     * </p>
     *
     * @return настроенный провайдер аутентификации
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Предоставляет менеджер аутентификации.
     * <p>
     * Менеджер аутентификации используется для обработки запросов на аутентификацию.
     * Получается из конфигурации аутентификации Spring.
     * </p>
     *
     * @param config конфигурация аутентификации
     * @return менеджер аутентификации
     * @throws Exception если возникает ошибка при получении менеджера
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Создает кодировщик паролей.
     * <p>
     * <strong>ВНИМАНИЕ:</strong> В текущей реализации используется {@link NoOpPasswordEncoder},
     * который НЕ шифрует пароли и хранит их в открытом виде. Это крайне небезопасно!
     * </p>
     * <p>
     * <strong>Рекомендация:</strong> Замените на {@code BCryptPasswordEncoder} в продакшене:
     * <pre>
     * return new BCryptPasswordEncoder();
     * </pre>
     * </p>
     *
     * @deprecated Используйте только для разработки и тестирования. Не применять в продакшене!
     * @return кодировщик паролей (небезопасная версия)
     */
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        // ⚠️ ОПАСНО: NoOpPasswordEncoder не шифрует пароли!
        // Используйте только для тестирования
        return NoOpPasswordEncoder.getInstance();
    }
}