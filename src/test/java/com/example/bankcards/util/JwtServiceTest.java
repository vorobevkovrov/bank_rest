package com.example.bankcards.util;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private final String SECRET_KEY = "mySuperSecretKeyForJWTGenerationThatIsAtLeast32BytesLongForHS256";
    private final String BASE64_SECRET = Base64.getEncoder().encodeToString(SECRET_KEY.getBytes());
    private final long JWT_EXPIRATION = 3600000;
    private final long REFRESH_EXPIRATION = 86400000;

    private User testUser;
    private UserDetails userDetails;
    private final Long USER_ID = 1L;
    private final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", JWT_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_EXPIRATION);

        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .role(Role.USER)
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(USERNAME)
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Nested
    @DisplayName("Tests for generateToken method")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate valid JWT token with claims")
        void generateToken_Success() {
            // Act
            String token = jwtService.generateToken(testUser);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();

            // Verify token can be parsed
            Claims claims = extractClaims(token);
            assertThat(claims.getSubject()).isEqualTo(USERNAME);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.USER.name());
            assertThat(claims.get("userId", Long.class)).isEqualTo(USER_ID);
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("Should generate token with correct expiration time")
        void generateToken_CorrectExpiration() {
            // Arrange
            Date now = new Date();

            // Act
            String token = jwtService.generateToken(testUser);
            Claims claims = extractClaims(token);

            // Assert
            long expectedExpiration = now.getTime() + JWT_EXPIRATION;
            long actualExpiration = claims.getExpiration().getTime();

            // Allow 1 second tolerance for test execution time
            assertThat(actualExpiration).isBetween(expectedExpiration - 1000, expectedExpiration + 1000);
        }

        @Test
        @DisplayName("Should have issuedAt close to current time")
        void generateToken_HasIssuedAt() {
            // Act
            String token = jwtService.generateToken(testUser);
            Date issuedAt = extractClaims(token).getIssuedAt();

            // Assert
            // Проверяем, что время создания токена отличается от текущего не более чем на 1 секунду
            assertThat(issuedAt).isCloseTo(new Date(), 1000L);
        }
    }

    @Nested
    @DisplayName("Tests for generateRefreshToken method")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Should generate valid refresh token without additional claims")
        void generateRefreshToken_Success() {
            // Act
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Assert
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken).isNotBlank();

            Claims claims = extractClaims(refreshToken);
            assertThat(claims.getSubject()).isEqualTo(USERNAME);
            assertThat(claims.get("role")).isNull();
            assertThat(claims.get("userId")).isNull();
        }

        @Test
        @DisplayName("Should generate refresh token with correct expiration")
        void generateRefreshToken_CorrectExpiration() {
            // Arrange
            Date now = new Date();

            // Act
            String refreshToken = jwtService.generateRefreshToken(testUser);
            Claims claims = extractClaims(refreshToken);

            // Assert
            long expectedExpiration = now.getTime() + REFRESH_EXPIRATION;
            long actualExpiration = claims.getExpiration().getTime();

            assertThat(actualExpiration).isBetween(expectedExpiration - 1000, expectedExpiration + 1000);
        }
    }

    @Nested
    @DisplayName("Tests for extractUsername method")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract username from token")
        void extractUsername_Success() {
            // Arrange
            String token = jwtService.generateToken(testUser);

            // Act
            String extractedUsername = jwtService.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("Should extract username from refresh token")
        void extractUsername_FromRefreshToken_Success() {
            // Arrange
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Act
            String extractedUsername = jwtService.extractUsername(refreshToken);

            // Assert
            assertThat(extractedUsername).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("Should throw exception when token is invalid")
        void extractUsername_InvalidToken_ThrowsException() {
            // Arrange
            String invalidToken = "invalid.token.here";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Tests for extractUserId method")
    class ExtractUserIdTests {

        @Test
        @DisplayName("Should extract userId from token")
        void extractUserId_Success() {
            // Arrange
            String token = jwtService.generateToken(testUser);

            // Act
            Long extractedUserId = jwtService.extractUserId(token);

            // Assert
            assertThat(extractedUserId).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Should return null when extracting userId from refresh token")
        void extractUserId_FromRefreshToken_ReturnsNull() {
            // Arrange
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Act
            Long extractedUserId = jwtService.extractUserId(refreshToken);

            // Assert
            assertThat(extractedUserId).isNull();
        }

        @Test
        @DisplayName("Should throw exception when token is invalid")
        void extractUserId_InvalidToken_ThrowsException() {
            // Arrange
            String invalidToken = "invalid.token.here";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUserId(invalidToken))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Tests for isTokenValid method")
    class IsTokenValidTests {

        @Test
        @DisplayName("Should return true for valid token")
        void isTokenValid_ValidToken_ReturnsTrue() {
            // Arrange
            String token = jwtService.generateToken(testUser);

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void isTokenValid_ExpiredToken_ReturnsFalse() throws InterruptedException {
            // Arrange
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L); // 1 millisecond
            String token = jwtService.generateToken(testUser);

            // Wait for token to expire
            Thread.sleep(10);

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false when username doesn't match")
        void isTokenValid_WrongUsername_ReturnsFalse() {
            // Arrange
            String token = jwtService.generateToken(testUser);
            UserDetails wrongUser = org.springframework.security.core.userdetails.User.builder()
                    .username("wronguser")
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();

            // Act
            boolean isValid = jwtService.isTokenValid(token, wrongUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for invalid token")
        void isTokenValid_InvalidToken_ReturnsFalse() {
            // Arrange
            String invalidToken = "invalid.token.here";

            // Act
            boolean isValid = jwtService.isTokenValid(invalidToken, userDetails);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for token expiration")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should create token that expires after specified time")
        void tokenExpiration_AfterTime_TokenExpires() throws InterruptedException {
            // 1. Ставим 1 секунду (чтобы точно успеть вызвать assertTrue)
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000L);

            String token = jwtService.generateToken(testUser);

            // 2. Сразу после создания он валиден
            assertTrue(jwtService.isTokenValid(token, userDetails));

            // 3. Ждем 1.2 секунды
            Thread.sleep(1200);

            // 4. Проверяем, что метод вернул false (так как исключение поймано внутри сервиса)
            assertFalse(jwtService.isTokenValid(token, userDetails), "Token should be invalid after expiration");
        }


        @Test
        @DisplayName("Should create refresh token that expires after specified time")
        void refreshTokenExpiration_AfterTime_TokenExpires() throws InterruptedException {
            // Arrange
            ReflectionTestUtils.setField(jwtService, "refreshExpiration", 100L); // 100 milliseconds
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Wait for expiration
            Thread.sleep(150);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(refreshToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("Tests for getExpirationTime method")
    class GetExpirationTimeTests {

        @Test
        @DisplayName("Should return configured expiration time")
        void getExpirationTime_ReturnsCorrectValue() {
            // Act
            long expirationTime = jwtService.getExpirationTime();

            // Assert
            assertThat(expirationTime).isEqualTo(JWT_EXPIRATION);
        }
    }

    @Nested
    @DisplayName("Integration tests for JWT operations")
    class IntegrationTests {

        @Test
        @DisplayName("Should generate and validate multiple tokens for same user")
        void multipleTokens_SameUser_AllValid() {
            // Arrange
            int numberOfTokens = 5;
            String[] tokens = new String[numberOfTokens];

            // Act
            for (int i = 0; i < numberOfTokens; i++) {
                tokens[i] = jwtService.generateToken(testUser);
            }

            // Assert
            for (String token : tokens) {
                assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
                assertThat(jwtService.extractUsername(token)).isEqualTo(USERNAME);
                assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID);
            }
        }

        @Test
        @DisplayName("Should generate tokens for different users")
        void tokens_DifferentUsers_Success() {
            // Arrange
            User user2 = User.builder()
                    .id(2L)
                    .username("user2")
                    .role(Role.USER)
                    .build();

            UserDetails userDetails2 = org.springframework.security.core.userdetails.User.builder()
                    .username("user2")
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();

            // Act
            String token1 = jwtService.generateToken(testUser);
            String token2 = jwtService.generateToken(user2);

            // Assert
            assertThat(jwtService.extractUsername(token1)).isEqualTo(USERNAME);
            assertThat(jwtService.extractUsername(token2)).isEqualTo("user2");

            assertThat(jwtService.isTokenValid(token1, userDetails)).isTrue();
            assertThat(jwtService.isTokenValid(token2, userDetails2)).isTrue();

            assertThat(jwtService.isTokenValid(token1, userDetails2)).isFalse();
            assertThat(jwtService.isTokenValid(token2, userDetails)).isFalse();
        }

        @Test
        @DisplayName("Should handle token with all claim types")
        void token_WithAllClaims_CanExtractAll() {
            // Arrange
            Map<String, Object> expectedClaims = new HashMap<>();
            expectedClaims.put("role", Role.ADMIN.name());
            expectedClaims.put("userId", 999L);
            expectedClaims.put("email", "test@example.com");
            expectedClaims.put("customClaim", "customValue");

            User adminUser = User.builder()
                    .id(999L)
                    .username("admin")
                    .role(Role.ADMIN)
                    .build();

            // Act
            String token = jwtService.generateToken(adminUser);

            // Assert
            assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
            assertThat(jwtService.extractUserId(token)).isEqualTo(999L);

            Claims claims = extractClaims(token);
            assertThat(claims.get("role", String.class)).isEqualTo(Role.ADMIN.name());
            assertThat(claims.get("userId", Long.class)).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("Tests for edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null user gracefully")
        void generateToken_NullUser_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> jwtService.generateToken(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null token in extract methods")
        void extractMethods_NullToken_ThrowsException() {
            assertThatThrownBy(() -> jwtService.extractUsername(null))
                    .isInstanceOf(Exception.class);

            assertThatThrownBy(() -> jwtService.extractUserId(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle empty token")
        void extractMethods_EmptyToken_ThrowsException() {
            assertThatThrownBy(() -> jwtService.extractUsername(""))
                    .isInstanceOf(Exception.class);

            assertThatThrownBy(() -> jwtService.extractUserId(""))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle malformed token")
        void extractMethods_MalformedToken_ThrowsException() {
            String[] malformedTokens = {
                    "header.payload",
                    "header.payload.signature",
                    "abc.def.ghi",
                    "....",
                    "token"
            };

            for (String malformedToken : malformedTokens) {
                assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                        .as("Token: " + malformedToken)
                        .isInstanceOf(Exception.class);
            }
        }
    }

    // Helper method to extract claims for verification
    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(BASE64_SECRET));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}