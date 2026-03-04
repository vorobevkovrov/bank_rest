package com.example.bankcards.util;

import com.example.bankcards.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для работы с JWT токенами.
 * Отвечает за генерацию, валидацию и извлечение информации из JWT токенов.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Генерирует JWT токен доступа для пользователя.
     * Включает в себя дополнительные claims: роль пользователя и его идентификатор.
     *
     * @param user объект пользователя, для которого генерируется токен
     * @return строковое представление JWT токена
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Генерирует refresh токен для обновления токена доступа.
     * Содержит только основную информацию о пользователе без дополнительных claims.
     *
     * @param user объект пользователя, для которого генерируется refresh токен
     * @return строковое представление refresh токена
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Извлекает имя пользователя из JWT токена.
     *
     * @param token JWT токен
     * @return имя пользователя (subject)
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Извлекает идентификатор пользователя из JWT токена.
     *
     * @param token JWT токен
     * @return идентификатор пользователя
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Проверяет валидность JWT токена.
     * Токен считается валидным, если имя пользователя в токене совпадает с именем пользователя
     * в UserDetails и токен не истек.
     *
     * @param token JWT токен для проверки
     * @param userDetails детали пользователя из Spring Security
     * @return true если токен валидный, false в противном случае
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Если токен просрочен, возвращаем false, а не выбрасываем исключение
            return false;
        } catch (Exception e) {
            // Любая другая ошибка (неверная подпись, формат и т.д.) — тоже невалиден
            return false;
        }
    }

    /**
     * Проверяет, истек ли срок действия токена.
     *
     * @param token JWT токен для проверки
     * @return true если токен истек, false если действителен
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Извлекает все claims из JWT токена.
     *
     * @param token JWT токен
     * @return объект Claims, содержащий все данные токена
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Получает секретный ключ для подписи JWT токенов.
     * Декодирует base64 строку в байты и создает HMAC ключ.
     *
     * @return SecretKey для подписи токенов
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Возвращает время жизни токена доступа в миллисекундах.
     *
     * @return время жизни токена доступа
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }
}