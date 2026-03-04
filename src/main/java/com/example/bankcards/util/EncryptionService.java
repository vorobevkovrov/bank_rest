package com.example.bankcards.util;

import com.example.bankcards.exception.exceptions.DecryptedException;
import com.example.bankcards.exception.exceptions.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Сервис для шифрования и дешифрования конфиденциальных данных.
 * <p>
 * Использует алгоритм AES (Advanced Encryption Standard) для обеспечения
 * безопасности данных. Основное применение - шифрование номеров банковских карт
 * в соответствии с требованиями PCI DSS.
 * </p>
 *
 * <h2>Особенности:</h2>
 * <ul>
 *   <li>Используется симметричное шифрование с одним ключом</li>
 *   <li>Ключ шифрования загружается из конфигурации (${encryption.key})</li>
 *   <li>Результат шифрования кодируется в Base64 для удобного хранения</li>
 *   <li>При ошибках шифрования/дешифрования выбрасывается {@link EncryptionException/DecryptedException}</li>
 * </ul>
 *
 * <h2>Важно:</h2>
 * <p>
 * Ключ шифрования должен быть длиной 16, 24 или 32 байта для AES-128, AES-192 или AES-256 соответственно.
 * Хранение ключа должно быть безопасным (использовать переменные окружения или защищенное хранилище).
 * </p>
 *
 * @author Maxim Vorobev
 * @version 1.0
 * @see javax.crypto.Cipher
 * @see javax.crypto.spec.SecretKeySpec
 * @since 1.0
 */
@Service
public class EncryptionService {

    /**
     * Ключ шифрования, загружаемый из конфигурации.
     * Должен быть указан в application.properties или application.yml как {@code encryption.key}.
     */
    @Value("${encryption.key}")
    private String encryptionKey;

    /**
     * Алгоритм шифрования - AES (Advanced Encryption Standard).
     */
    private static final String ALGORITHM = "AES";

    /**
     * Шифрует переданные данные с использованием AES алгоритма.
     * <p>
     * Процесс шифрования:
     * <ol>
     *   <li>Создание ключа из строки {@code encryptionKey} с использованием UTF-8 кодировки</li>
     *   <li>Инициализация шифра в режиме ENCRYPT_MODE</li>
     *   <li>Шифрование данных</li>
     *   <li>Кодирование результата в Base64 для безопасного хранения</li>
     * </ol>
     * </p>
     *
     * @param data исходные данные для шифрования (например, номер карты)
     * @return зашифрованные данные, закодированные в Base64
     * @throws EncryptionException если произошла ошибка при шифровании (неверный ключ, проблемы с алгоритмом и т.д.)
     * @example <pre>
     * EncryptionService service = new EncryptionService();
     * String encrypted = service.encrypt("1234567890123456");
     * // encrypted = "7B5pZ7yX3kL9mN2qR4tV8wY1zA6cD8fG="
     * </pre>
     */
    public String encrypt(String data) {
        try {
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new EncryptionException(e.getMessage());
        }
    }

    /**
     * Дешифрует ранее зашифрованные данные.
     * <p>
     * Процесс дешифрования:
     * <ol>
     *   <li>Создание ключа из строки {@code encryptionKey} с использованием UTF-8 кодировки</li>
     *   <li>Инициализация шифра в режиме DECRYPT_MODE</li>
     *   <li>Декодирование входных данных из Base64</li>
     *   <li>Дешифрование данных</li>
     *   <li>Преобразование результата в строку</li>
     * </ol>
     * </p>
     *
     * @param encryptedData зашифрованные данные в формате Base64
     * @return расшифрованные исходные данные
     * @throws DecryptedException если произошла ошибка при дешифровании (неверный ключ, поврежденные данные и т.д.)
     * @example <pre>
     * EncryptionService service = new EncryptionService();
     * String decrypted = service.decrypt("7B5pZ7yX3kL9mN2qR4tV8wY1zA6cD8fG=");
     * // decrypted = "1234567890123456"
     * </pre>
     */
    public String decrypt(String encryptedData) {
        try {
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted);
        } catch (Exception e) {
            throw new DecryptedException(e.getMessage());
        }
    }
}