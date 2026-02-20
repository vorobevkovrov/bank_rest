package com.example.bankcards.security;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Реализация сервиса для загрузки данных пользователя в Spring Security.
 * <p>
 * Этот класс является ключевым компонентом аутентификации в приложении.
 * Он загружает пользователя из базы данных по имени пользователя и преобразует
 * его в объект {@link UserDetails}, который используется Spring Security
 * для аутентификации и авторизации.
 * </p>
 *
 * <h2>Процесс загрузки пользователя:</h2>
 * <ol>
 *   <li>Поиск пользователя в базе данных по username</li>
 *   <li>Если пользователь не найден - выбрасывается {@link UsernameNotFoundException}</li>
 *   <li>Если пользователь найден - создается объект {@link UserDetails} с:
 *     <ul>
 *       <li>Именем пользователя</li>
 *       <li>Паролем (из базы данных)</li>
 *       <li>Ролью пользователя, преобразованной в GrantedAuthority</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Важные замечания:</h2>
 * <ul>
 *   <li><strong>Безопасность:</strong> Пароль передается в том виде, в котором хранится в БД.
 *       Если пароли хранятся в незашифрованном виде, это критическая уязвимость!</li>
 *   <li><strong>Роли:</strong> Роли автоматически преобразуются в формат "ROLE_XXX",
 *       который требуется Spring Security для работы с hasRole()</li>
 * </ul>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see org.springframework.security.core.userdetails.UserDetailsService
 * @see org.springframework.security.core.userdetails.UserDetails
 * @see com.example.bankcards.entity.User
 * @see com.example.bankcards.repository.UserRepository
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    /**
     * Репозиторий для доступа к данным пользователей.
     * <p>
     * Используется для поиска пользователя по имени пользователя в базе данных.
     * Инжектируется через конструктор с помощью Lombok {@link RequiredArgsConstructor}.
     * </p>
     */
    private final UserRepository userRepository;

    /**
     * Загружает пользователя по его имени пользователя.
     * <p>
     * Этот метод используется Spring Security в процессе аутентификации.
     * </p>
     *
     * <h3>Алгоритм работы:</h3>
     * <ul>
     *   <li>Поиск пользователя в БД по {@code username}</li>
     *   <li>Если пользователь найден - создается {@link UserDetails} с его данными</li>
     *   <li>Если пользователь не найден - выбрасывается {@link UsernameNotFoundException}</li>
     * </ul>
     *
     * <h3>Возвращаемый объект UserDetails содержит:</h3>
     * <ul>
     *   <li><strong>username:</strong> имя пользователя для аутентификации</li>
     *   <li><strong>password:</strong> пароль (должен быть зашифрован в БД!)</li>
     *   <li><strong>authorities:</strong> список прав доступа (роль пользователя)</li>
     * </ul>
     *
     * @param username имя пользователя для поиска (не может быть null)
     * @return {@link UserDetails} объект с данными пользователя для Spring Security
     * @throws UsernameNotFoundException если пользователь с указанным username не найден в базе данных
     * @apiNote <strong>Важно:</strong> Пароль должен храниться в базе данных в зашифрованном виде!
     * Использование незашифрованных паролей - критическая уязвимость безопасности.
     * @see org.springframework.security.core.userdetails.UserDetails
     * @see org.springframework.security.core.userdetails.UsernameNotFoundException
     * @see com.example.bankcards.entity.User
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        log.debug("User loaded successfully: {} with role: {}", username, user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
