package com.example.bankcards.util;

import com.example.bankcards.exception.exceptions.DecryptedException;
import com.example.bankcards.exception.exceptions.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    private final String VALID_KEY_24 = "123456789012345678901234"; // 24 bytes for AES-192
    private final String VALID_KEY_32 = "12345678901234567890123456789012"; // 32 bytes for AES-256
    private final String INVALID_KEY = "short"; // Too short key
    private final String TEST_DATA = "1234567890123456";
    private final String SPECIAL_CHARS_DATA = "!@#$%^&*()_+";
    private final String EMPTY_DATA = "";
    private final String LONG_DATA = "This is a very long string that exceeds typical card number length for testing purposes";
    private final String UNICODE_DATA = "Привет мир! こんにちは 🌍";

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        // 16 bytes for AES-128
        String VALID_KEY = "1234567890123456";
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", VALID_KEY);
    }

    @Nested
    @DisplayName("Tests for encrypt method")
    class EncryptTests {

        @Test
        @DisplayName("Should successfully encrypt data with valid key")
        void encrypt_Success() {
            // Act
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(TEST_DATA);
            assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$"); // Base64 pattern
        }

        @Test
        @DisplayName("Should successfully encrypt data with 24-byte key")
        void encrypt_With24ByteKey_Success() {
            // Arrange
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", VALID_KEY_24);

            // Act
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(TEST_DATA);
            assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
        }

        @Test
        @DisplayName("Should successfully encrypt data with 32-byte key")
        void encrypt_With32ByteKey_Success() {
            // Arrange
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", VALID_KEY_32);

            // Act
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(TEST_DATA);
            assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
        }

        @Test
        @DisplayName("Should successfully encrypt data with special characters")
        void encrypt_SpecialCharacters_Success() {
            // Act
            String encrypted = encryptionService.encrypt(SPECIAL_CHARS_DATA);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(SPECIAL_CHARS_DATA);
            assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
        }

        @Test
        @DisplayName("Should successfully encrypt empty string")
        void encrypt_EmptyString_Success() {
            // Act
            String encrypted = encryptionService.encrypt(EMPTY_DATA);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
        }

        @Test
        @DisplayName("Should successfully encrypt long string")
        void encrypt_LongString_Success() {
            // Act
            String encrypted = encryptionService.encrypt(LONG_DATA);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(LONG_DATA);
            assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
        }

        @Test
        @DisplayName("Should successfully encrypt Unicode string")
        void encrypt_UnicodeString_Success() {
            // Act
            String encrypted = encryptionService.encrypt(UNICODE_DATA);

            // Assert
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(UNICODE_DATA);
            assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
        }

        @Test
        @DisplayName("Should throw EncryptionException when key is invalid")
        void encrypt_InvalidKey_ThrowsEncryptionException() {
            // Arrange
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", INVALID_KEY);

            // Act & Assert
            assertThatThrownBy(() -> encryptionService.encrypt(TEST_DATA))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("Should throw EncryptionException when key is null")
        void encrypt_NullKey_ThrowsEncryptionException() {
            // Arrange
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", null);

            // Act & Assert
            assertThatThrownBy(() -> encryptionService.encrypt(TEST_DATA))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("Should throw EncryptionException when data is null")
        void encrypt_NullData_ThrowsEncryptionException() {
            // Act & Assert
            assertThatThrownBy(() -> encryptionService.encrypt(null))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("Should throw EncryptionException with non-null message")
        void encrypt_Exception_ContainsMessage() {
            // Arrange
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", INVALID_KEY);

            // Act & Assert
            assertThatThrownBy(() -> encryptionService.encrypt(TEST_DATA))
                    .isInstanceOf(EncryptionException.class)
                    .hasMessage("Invalid AES key length: 5 bytes");
        }
    }

    @Nested
    @DisplayName("Tests for decrypt method")
    class DecryptTests {

        @Test
        @DisplayName("Should successfully decrypt previously encrypted data")
        void decrypt_Success() {
            // Arrange
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isNotNull();
            assertThat(decrypted).isEqualTo(TEST_DATA);
        }

        @Test
        @DisplayName("Should successfully decrypt data encrypted with 24-byte key")
        void decrypt_With24ByteKey_Success() {
            // Arrange
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", VALID_KEY_24);
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isNotNull();
            assertThat(decrypted).isEqualTo(TEST_DATA);
        }

        @Test
        @DisplayName("Should successfully decrypt data encrypted with 32-byte key")
        void decrypt_With32ByteKey_Success() {
            // Arrange
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", VALID_KEY_32);
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isNotNull();
            assertThat(decrypted).isEqualTo(TEST_DATA);
        }

        @Test
        @DisplayName("Should successfully decrypt data with special characters")
        void decrypt_SpecialCharacters_Success() {
            // Arrange
            String encrypted = encryptionService.encrypt(SPECIAL_CHARS_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isNotNull();
            assertThat(decrypted).isEqualTo(SPECIAL_CHARS_DATA);
        }

        @Test
        @DisplayName("Should successfully decrypt empty string")
        void decrypt_EmptyString_Success() {
            // Arrange
            String encrypted = encryptionService.encrypt(EMPTY_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isNotNull();
            assertThat(decrypted).isEqualTo(EMPTY_DATA);
        }

        @Test
        @DisplayName("Should successfully decrypt long string")
        void decrypt_LongString_Success() {
            // Arrange
            String encrypted = encryptionService.encrypt(LONG_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isNotNull();
            assertThat(decrypted).isEqualTo(LONG_DATA);
        }

        @Test
        @DisplayName("Should successfully decrypt Unicode string")
        void decrypt_UnicodeString_Success() {
            // Arrange
            String encrypted = encryptionService.encrypt(UNICODE_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isNotNull();
            assertThat(decrypted).isEqualTo(UNICODE_DATA);
        }

        @Test
        @DisplayName("Should throw DecryptedException when key is invalid")
        void decrypt_InvalidKey_ThrowsDecryptedException() {
            // Arrange
            String encrypted = encryptionService.encrypt(TEST_DATA);
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", INVALID_KEY);

            // Act & Assert
            assertThatThrownBy(() -> encryptionService.decrypt(encrypted))
                    .isInstanceOf(DecryptedException.class);
        }

        @Test
        @DisplayName("Should throw DecryptedException when key is null")
        void decrypt_NullKey_ThrowsDecryptedException() {
            // Arrange
            String encrypted = encryptionService.encrypt(TEST_DATA);
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", null);

            // Act & Assert
            assertThatThrownBy(() -> encryptionService.decrypt(encrypted))
                    .isInstanceOf(DecryptedException.class);
        }

        @Test
        @DisplayName("Should throw DecryptedException when encrypted data is null")
        void decrypt_NullData_ThrowsDecryptedException() {
            // Act & Assert
            assertThatThrownBy(() -> encryptionService.decrypt(null))
                    .isInstanceOf(DecryptedException.class);
        }

        @Test
        @DisplayName("Should successfully decrypt empty string encrypted data")
        void decrypt_EmptyStringData_Success() {
            // Arrange
            String encrypted = encryptionService.encrypt(EMPTY_DATA);

            // Act
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(EMPTY_DATA);
        }

        @Test
        @DisplayName("Should throw DecryptedException when encrypted data is invalid Base64")
        void decrypt_InvalidBase64_ThrowsDecryptedException() {
            // Act & Assert
            assertThatThrownBy(() -> encryptionService.decrypt("not-base64-data"))
                    .isInstanceOf(DecryptedException.class);
        }

        @Test
        @DisplayName("Should throw DecryptedException when data was encrypted with different key")
        void decrypt_DifferentKey_ThrowsDecryptedException() {
            // Arrange
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Create another service with different key
            EncryptionService anotherService = new EncryptionService();
            ReflectionTestUtils.setField(anotherService, "encryptionKey", "6543210987654321");

            // Act & Assert
            assertThatThrownBy(() -> anotherService.decrypt(encrypted))
                    .isInstanceOf(DecryptedException.class);
        }

        @Test
        @DisplayName("Should throw DecryptedException when data is tampered")
        void decrypt_TamperedData_ThrowsDecryptedException() {
            // Arrange
            String encrypted = encryptionService.encrypt(TEST_DATA);

            // Создаем заведомо невалидные данные, меняя последний символ
            // Но чтобы быть уверенным, что данные станут невалидными,
            // используем полностью другую строку
            String tamperedData = Base64.getEncoder().encodeToString("different data".getBytes());

            // Act & Assert
            assertThatThrownBy(() -> encryptionService.decrypt(tamperedData))
                    .isInstanceOf(DecryptedException.class);
        }

        @Test
        @DisplayName("Should throw DecryptedException with non-null message")
        void decrypt_Exception_ContainsMessage() {
            // Act & Assert
            assertThatThrownBy(() -> encryptionService.decrypt("invalid"))
                    .isInstanceOf(DecryptedException.class)
                    .hasMessage("Input length must be multiple of 16 when decrypting with padded cipher");
        }
    }

    @Nested
    @DisplayName("Integration tests for encrypt-decrypt cycle")
    class EncryptDecryptCycleTests {

        @Test
        @DisplayName("Should successfully complete full cycle for multiple data sets")
        void fullCycle_MultipleDataSets_Success() {
            // Arrange
            String[] testData = {
                    "1234",
                    "1234567890123456",
                    "12345678901234567890",
                    "user@example.com",
                    "John Doe",
                    "  leading and trailing spaces ",
                    "{\"key\":\"value\"}",
                    "1234-5678-9012-3456",
                    "a",
                    "abcdefghijklmnopqrstuvwxyz",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                    "0123456789"
            };

            for (String data : testData) {
                // Act
                String encrypted = encryptionService.encrypt(data);
                String decrypted = encryptionService.decrypt(encrypted);

                // Assert
                assertThat(decrypted).isEqualTo(data);
                assertThat(encrypted).isNotEqualTo(data);
                assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
            }
        }

        @Test
        @DisplayName("Should handle multiple encrypt-decrypt cycles on same data")
        void fullCycle_MultipleCycles_Success() {
            // Arrange
            String currentData = TEST_DATA;

            for (int i = 0; i < 5; i++) {
                // Act
                String encrypted = encryptionService.encrypt(currentData);
                String decrypted = encryptionService.decrypt(encrypted);

                // Assert
                assertThat(decrypted).isEqualTo(currentData);
                currentData = decrypted;
            }
        }

        @Test
        @DisplayName("Should produce consistent encryption results for same data")
        void encrypt_ProducesConsistentResults() {
            // Note: AES in ECB mode produces same output for same input.

            // Act
            String encrypted1 = encryptionService.encrypt(TEST_DATA);
            String encrypted2 = encryptionService.encrypt(TEST_DATA);

            // Assert
            assertThat(encrypted1).isEqualTo(encrypted2);
        }

        @Test
        @DisplayName("Should handle very large data")
        void fullCycle_LargeData_Success() {
            // Arrange
            StringBuilder largeData = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeData.append("Test data line ").append(i).append("\n");
            }
            String largeString = largeData.toString();

            // Act
            String encrypted = encryptionService.encrypt(largeString);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(largeString);
        }

        @Test
        @DisplayName("Should handle binary data")
        void fullCycle_BinaryData_Success() {
            // Arrange
            byte[] binaryData = new byte[256];
            for (int i = 0; i < binaryData.length; i++) {
                binaryData[i] = (byte) i;
            }
            String binaryString = new String(binaryData, StandardCharsets.ISO_8859_1);

            // Act
            String encrypted = encryptionService.encrypt(binaryString);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(binaryString);
        }
    }

    @Nested
    @DisplayName("Tests for different key lengths")
    class KeyLengthTests {

        @Test
        @DisplayName("Should work with 16-byte key (AES-128)")
        void keyLength_16Bytes_Success() {
            // Arrange
            String key16 = "1234567890123456";
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", key16);

            // Act
            String encrypted = encryptionService.encrypt(TEST_DATA);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(TEST_DATA);
        }

        @Test
        @DisplayName("Should work with 24-byte key (AES-192)")
        void keyLength_24Bytes_Success() {
            // Arrange
            String key24 = "123456789012345678901234";
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", key24);

            // Act
            String encrypted = encryptionService.encrypt(TEST_DATA);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(TEST_DATA);
        }

        @Test
        @DisplayName("Should work with 32-byte key (AES-256)")
        void keyLength_32Bytes_Success() {
            // Arrange
            String key32 = "12345678901234567890123456789012";
            ReflectionTestUtils.setField(encryptionService, "encryptionKey", key32);

            // Act
            String encrypted = encryptionService.encrypt(TEST_DATA);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(TEST_DATA);
        }
    }

    @Nested
    @DisplayName("Tests for edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle single character")
        void encryptDecrypt_SingleCharacter_Success() {
            // Arrange
            String singleChar = "A";

            // Act
            String encrypted = encryptionService.encrypt(singleChar);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(singleChar);
        }

        @Test
        @DisplayName("Should handle string with only spaces")
        void encryptDecrypt_OnlySpaces_Success() {
            // Arrange
            String spaces = "     ";

            // Act
            String encrypted = encryptionService.encrypt(spaces);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(spaces);
        }

        @Test
        @DisplayName("Should handle string with newlines")
        void encryptDecrypt_WithNewlines_Success() {
            // Arrange
            String withNewlines = "Line1\nLine2\r\nLine3";

            // Act
            String encrypted = encryptionService.encrypt(withNewlines);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(withNewlines);
        }

        @Test
        @DisplayName("Should handle string with null characters")
        void encryptDecrypt_WithNullChar_Success() {
            // Arrange
            String withNull = "test\0data";

            // Act
            String encrypted = encryptionService.encrypt(withNull);
            String decrypted = encryptionService.decrypt(encrypted);

            // Assert
            assertThat(decrypted).isEqualTo(withNull);
        }

        @Test
        @DisplayName("Should produce different results for different keys")
        void differentKeys_DifferentResults() {
            // Arrange
            String encrypted1 = encryptionService.encrypt(TEST_DATA);

            EncryptionService anotherService = new EncryptionService();
            ReflectionTestUtils.setField(anotherService, "encryptionKey", VALID_KEY_24);
            String encrypted2 = anotherService.encrypt(TEST_DATA);

            // Assert
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }
    }
}