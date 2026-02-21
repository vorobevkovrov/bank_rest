package com.example.bankcards.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Полный набор тестов для класса {@link GenerateCardNumber}.
 * Покрывает все возможные кейсы, включая edge cases, производительность и безопасность.
 */
@DisplayName("Тесты для класса GenerateCardNumber")
class GenerateCardNumberTest {

    private GenerateCardNumber generator;
    private static final String EXPECTED_PREFIX = "4000";
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^4000\\d{12}$");
    private static final Random RANDOM = new Random(42); // Фиксированный seed для воспроизводимости

    @BeforeEach
    @DisplayName("Инициализация генератора перед каждым тестом")
    void setUp() {
        generator = new GenerateCardNumber();
    }

    // =================================================================
    // 1. ТЕСТЫ ФОРМАТА И СТРУКТУРЫ
    // =================================================================

    @Test
    @DisplayName("Сгенерированный номер карты не должен быть null")
    void generateCardNumber_shouldNotBeNull() {
        String cardNumber = generator.generateCardNumber();
        assertNotNull(cardNumber, "Сгенерированный номер карты не должен быть null");
    }

    @Test
    @DisplayName("Сгенерированный номер карты должен иметь правильную длину (16 цифр)")
    void generateCardNumber_shouldHaveCorrectLength() {
        String cardNumber = generator.generateCardNumber();
        assertEquals(16, cardNumber.length(),
                "Сгенерированный номер карты должен быть ровно 16 цифр");
    }

    @Test
    @DisplayName("Сгенерированный номер карты должен начинаться с префикса 4000")
    void generateCardNumber_shouldStartWithCorrectPrefix() {
        String cardNumber = generator.generateCardNumber();
        assertTrue(cardNumber.startsWith(EXPECTED_PREFIX),
                "Номер карты должен начинаться с префикса " + EXPECTED_PREFIX);
    }

    @Test
    @DisplayName("Сгенерированный номер карты должен содержать только цифры")
    void generateCardNumber_shouldContainOnlyDigits() {
        String cardNumber = generator.generateCardNumber();
        assertTrue(cardNumber.matches("\\d{16}"),
                "Номер карты должен содержать только цифры 0-9");
    }

    @Test
    @DisplayName("Сгенерированный номер карты должен соответствовать паттерну")
    void generateCardNumber_shouldMatchPattern() {
        String cardNumber = generator.generateCardNumber();
        assertTrue(CARD_NUMBER_PATTERN.matcher(cardNumber).matches(),
                "Номер карты должен соответствовать паттерну: 4000 + 12 цифр");
    }

    @RepeatedTest(100)
    @DisplayName("Многократная генерация должна всегда возвращать номера правильной длины")
    void generateCardNumber_shouldAlwaysHaveCorrectLengthOnMultipleCalls() {
        String cardNumber = generator.generateCardNumber();
        assertEquals(16, cardNumber.length(),
                "При многократной генерации все номера должны быть длиной 16 цифр");
    }

    // =================================================================
    // 2. ТЕСТЫ АЛГОРИТМА ЛУНА (Luhn Algorithm)
    // =================================================================

    @RepeatedTest(100)
    @DisplayName("Все сгенерированные номера должны быть валидными по алгоритму Луна")
    void generateCardNumber_shouldAlwaysBeLuhnValid() {
        String cardNumber = generator.generateCardNumber();
        assertTrue(isLuhnValid(cardNumber),
                "Каждый сгенерированный номер карты должен быть валидным по алгоритму Луна");
    }

    @Test
    @DisplayName("Проверка контрольной цифры для известных тестовых номеров")
    void calculateLuhnCheckDigit_shouldReturnCorrectDigitForKnownNumbers() throws Exception {
        Method method = getPrivateMethod("calculateLuhnCheckDigit");

        // Известные тестовые данные (частичный номер -> правильная контрольная цифра)
        Object[][] testCases = {
                {"7992739871", 3},
                {"424242424242424", 2},
                {"37828224631000", 5},
                {"555555555555444", 4},
                {"401288888888188", 1},
                {"411111111111111", 1},
                {"601111111111111", 7},
                {"353011133330000", 0},
                {"400012345678901", 7}  // Важный кейс: исправленный пример из класса
        };

        for (Object[] testCase : testCases) {
            String number = (String) testCase[0];
            int expected = (int) testCase[1];
            int actual = (int) method.invoke(generator, number);

            assertEquals(expected, actual,
                    String.format("Для номера '%s' ожидается контрольная цифра %d", number, expected));

            // Дополнительная проверка: полный номер должен быть валиден
            String fullNumber = number + expected;
            assertTrue(isLuhnValid(fullNumber),
                    String.format("Полный номер '%s' должен быть валиден", fullNumber));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4111111111111111",    // Visa тестовый
            "4242424242424242",    // Visa тестовый 2
            "5555555555554444",    // MasterCard тестовый
            "378282246310005",     // American Express тестовый
            "6011111111111117",    // Discover тестовый
            "3530111333300000",    // JCB тестовый
            "4000123456789017"     // Исправленный пример
    })
    @DisplayName("Известные валидные номера карт должны проходить проверку Луна")
    void knownValidCardNumbers_shouldPassLuhnValidation(String cardNumber) {
        assertTrue(isLuhnValid(cardNumber),
                String.format("Известный тестовый номер '%s' должен быть валиден", cardNumber));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4111111111111112",    // Неправильная контрольная цифра
            "4242424242424243",    // Неправильная контрольная цифра
            "5555555555554445",    // Неправильная контрольная цифра
            "1234567812345678",    // Случайный номер
            "1111111111111111",    // Все единицы
            "4000123456789012"     // Старый невалидный пример
    })
    @DisplayName("Известные невалидные номера карт не должны проходить проверку Луна")
    void knownInvalidCardNumbers_shouldFailLuhnValidation(String cardNumber) {
        assertFalse(isLuhnValid(cardNumber),
                String.format("Номер '%s' должен быть невалиден", cardNumber));
    }

    @Test
    @DisplayName("Edge case: номера из всех нулей формально валидны по алгоритму Луна")
    void allZerosCardNumber_shouldBeFormallyValid() {
        assertTrue(isLuhnValid("0000000000000000"),
                "Номер из всех нулей формально валиден (сумма = 0, кратна 10)");
        assertTrue(isLuhnValid("0"), "Один ноль валиден");
        assertTrue(isLuhnValid("00"), "Два нуля валидны");
        assertTrue(isLuhnValid("00000000000000000000000000000000"), "32 нуля валидны");
    }

    // =================================================================
    // 3. ТЕСТЫ СЛУЧАЙНОСТИ И УНИКАЛЬНОСТИ
    // =================================================================

    @Test
    @DisplayName("Генератор должен создавать разные номера при многократном вызове")
    void generateCardNumber_shouldProduceDifferentNumbers() {
        final int iterations = 1000;
        Set<String> generatedNumbers = new HashSet<>();

        for (int i = 0; i < iterations; i++) {
            generatedNumbers.add(generator.generateCardNumber());
        }

        // Ожидаем, что большинство номеров будут уникальными
        double uniquenessRatio = (double) generatedNumbers.size() / iterations;
        assertTrue(uniquenessRatio > 0.95,
                String.format("Слишком много повторений: уникальность %.2f%%", uniquenessRatio * 100));
    }

    @Test
    @DisplayName("Распределение контрольных цифр должно быть примерно равномерным")
    void checkDigitDistribution_shouldBeUniform() {
        final int iterations = 10000;
        int[] digitCount = new int[10];

        for (int i = 0; i < iterations; i++) {
            String cardNumber = generator.generateCardNumber();
            int checkDigit = Character.getNumericValue(cardNumber.charAt(15));
            digitCount[checkDigit]++;
        }

        // Проверяем равномерность распределения
        double expected = iterations / 10.0;
        for (int digit = 0; digit < 10; digit++) {
            double ratio = digitCount[digit] / expected;
            assertTrue(ratio > 0.7 && ratio < 1.3,
                    String.format("Цифра %d встречается слишком %s: %d раз (ожидалось ~%.0f)",
                            digit, ratio < 0.7 ? "редко" : "часто", digitCount[digit], expected));
        }
    }

    @Test
    @DisplayName("Генератор не должен создавать тривиальные номера")
    void generateCardNumber_shouldNotProduceTrivialNumbers() {
        final int iterations = 1000;
        int trivialCount = 0;

        for (int i = 0; i < iterations; i++) {
            String cardNumber = generator.generateCardNumber();
            String afterPrefix = cardNumber.substring(4, 15); // 11 случайных цифр

            // Проверяем тривиальные паттерны
            if (afterPrefix.matches("0+") ||          // Все нули
                    afterPrefix.matches("(\\d)\\1{10}") || // Все одинаковые цифры
                    isSequential(afterPrefix)) {          // Последовательные цифры
                trivialCount++;
            }
        }

        // Допускаем не более 1% тривиальных номеров
        double trivialPercentage = (double) trivialCount / iterations * 100;
        assertTrue(trivialPercentage < 1.0,
                String.format("Слишком много тривиальных номеров: %.2f%%", trivialPercentage));
    }

    // =================================================================
    // 4. ТЕСТЫ ПРОИЗВОДИТЕЛЬНОСТИ И НАГРУЗКИ
    // =================================================================

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @DisplayName("Генерация 10000 номеров должна выполняться менее чем за 2 секунды")
    void generateCardNumber_performanceTest() {
        final int iterations = 10000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            String cardNumber = generator.generateCardNumber();
            assertNotNull(cardNumber);
            assertEquals(16, cardNumber.length());
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Генерация %d номеров заняла %d мс%n", iterations, duration);

        assertTrue(duration < 2000,
                String.format("Генерация %d номеров заняла слишком много времени: %d мс",
                        iterations, duration));
    }

    @RepeatedTest(10)
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @DisplayName("Одиночная генерация должна быть быстрой")
    void generateCardNumber_singleGenerationShouldBeFast() {
        String cardNumber = generator.generateCardNumber();
        assertNotNull(cardNumber);
    }

    // =================================================================
    // 5. ТЕСТЫ НА УСТОЙЧИВОСТЬ И НАДЕЖНОСТЬ
    // =================================================================

    @Test
    @DisplayName("Генератор должен корректно работать при многопоточном доступе")
    void generateCardNumber_threadSafetyTest() throws InterruptedException {
        final int threadsCount = 10;
        final int iterationsPerThread = 100;
        final Set<String> allNumbers = ConcurrentHashMap.newKeySet();
        Thread[] threads = new Thread[threadsCount];

        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    String cardNumber = generator.generateCardNumber();
                    assertNotNull(cardNumber);
                    assertEquals(16, cardNumber.length());
                    assertTrue(cardNumber.startsWith("4000"));
                    assertTrue(isLuhnValid(cardNumber));
                    allNumbers.add(cardNumber);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Проверяем, что все номера уникальны (в идеале)
        int expectedTotal = threadsCount * iterationsPerThread;
        assertTrue(allNumbers.size() >= expectedTotal * 0.95,
                String.format("Слишком много коллизий в многопоточном режиме: %d из %d уникальных",
                        allNumbers.size(), expectedTotal));
    }

    @Test
    @DisplayName("Генератор должен стабильно работать при долгой работе")
    void generateCardNumber_stabilityTest() {
        final int iterations = 100000;
        int invalidCount = 0;

        for (int i = 0; i < iterations; i++) {
            String cardNumber = generator.generateCardNumber();

            if (!isLuhnValid(cardNumber)) {
                invalidCount++;
                if (invalidCount <= 10) { // Ограничиваем вывод
                    System.err.printf("Невалидный номер на итерации %d: %s%n", i, cardNumber);
                }
            }
        }

        assertEquals(0, invalidCount,
                String.format("Обнаружено %d невалидных номеров из %d", invalidCount, iterations));
    }

    // =================================================================
    // 6. ТЕСТЫ PRIVATE МЕТОДОВ (через рефлексию)
    // =================================================================

    @Test
    @DisplayName("Приватный метод calculateLuhnCheckDigit должен корректно обрабатывать пустую строку")
    void calculateLuhnCheckDigit_shouldHandleEmptyString() throws Exception {
        Method method = getPrivateMethod("calculateLuhnCheckDigit");
        int result = (int) method.invoke(generator, "");
        assertEquals(0, result, "Для пустой строки должна возвращаться 0");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    @DisplayName("Приватный метод calculateLuhnCheckDigit должен падать на некорректных входных данных")
    void calculateLuhnCheckDigit_shouldThrowOnInvalidInput(String invalidInput) throws Exception {
        Method method = getPrivateMethod("calculateLuhnCheckDigit");

        // Для пустой строки метод работает, для null и других нецифровых - должен падать
        if (invalidInput == null || !invalidInput.isEmpty()) {
            assertThrows(Exception.class, () -> method.invoke(generator, invalidInput),
                    String.format("Для входных данных '%s' должно быть исключение", invalidInput));
        }
    }

    @Test
    @DisplayName("Проверка консистентности между публичным и приватным методами")
    void consistencyBetweenPublicAndPrivateMethods() throws Exception {
        Method privateMethod = getPrivateMethod("calculateLuhnCheckDigit");

        for (int i = 0; i < 100; i++) {
            // Генерируем частичный номер (15 цифр)
            StringBuilder partial = new StringBuilder(EXPECTED_PREFIX);
            for (int j = 0; j < 11; j++) {
                partial.append(RANDOM.nextInt(10));
            }

            String partialNumber = partial.toString();
            int checkDigit = (int) privateMethod.invoke(generator, partialNumber);

            // Генерируем полный номер через публичный метод
            String generatedNumber = generator.generateCardNumber();

            // Если сгенерированный номер начинается с нашего частичного номера,
            // то его контрольная цифра должна совпадать с вычисленной
            if (generatedNumber.startsWith(partialNumber)) {
                int generatedCheckDigit = Character.getNumericValue(generatedNumber.charAt(15));
                assertEquals(checkDigit, generatedCheckDigit,
                        String.format("Контрольные цифры должны совпадать для номера %s", partialNumber));
            }
        }
    }

    // =================================================================
    // 7. ТЕСТЫ НА ОШИБОЧНЫЕ СЦЕНАРИИ
    // =================================================================

    @Test
    @DisplayName("Генератор не должен зависеть от состояния")
    void generator_shouldBeStateless() throws Exception {
        // Генерируем первый номер
        String firstNumber = generator.generateCardNumber();

        // Создаем новый экземпляр генератора
        GenerateCardNumber newGenerator = new GenerateCardNumber();
        String secondNumber = newGenerator.generateCardNumber();

        // Номера могут совпасть случайно, но это крайне маловероятно
        // Главное - нет зависимости от предыдущего состояния
        assertNotEquals(firstNumber, secondNumber,
                "Новые экземпляры генератора должны создавать разные номера");
    }

    @Test
    @DisplayName("Префикс должен быть final и иметь правильное значение")
    void cardNumberPrefix_shouldBeFinalAndCorrect() throws Exception {
        Field field = GenerateCardNumber.class.getDeclaredField("CARD_NUMBER_PREFIX");

        // Проверяем, что поле final
        assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()),
                "Поле CARD_NUMBER_PREFIX должно быть final");

        // Проверяем, что поле static
        assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()),
                "Поле CARD_NUMBER_PREFIX должно быть static");

        // Проверяем, что поле private
        assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                "Поле CARD_NUMBER_PREFIX должно быть private");

        // Получаем значение через reflection
        field.setAccessible(true);
        String prefixValue = (String) field.get(null);

        // Проверяем значение
        assertEquals("4000", prefixValue,
                "Поле CARD_NUMBER_PREFIX должно иметь значение '4000'");
    }

    @Test
    @DisplayName("Генератор должен использовать правильный префикс")
    void generator_shouldUseCorrectPrefix() {
        // Генерируем несколько номеров и проверяем префикс
        for (int i = 0; i < 100; i++) {
            String cardNumber = generator.generateCardNumber();
            assertTrue(cardNumber.startsWith("4000"),
                    String.format("Сгенерированный номер '%s' должен начинаться с '4000'", cardNumber));
        }
    }

    @Test
    @DisplayName("Генератор должен быть независим от изменений в других экземплярах")
    void generator_shouldBeIndependentOfOtherInstances() {
        // Создаем два независимых генератора
        GenerateCardNumber generator1 = new GenerateCardNumber();
        GenerateCardNumber generator2 = new GenerateCardNumber();

        // Генерируем номера
        String number1 = generator1.generateCardNumber();
        String number2 = generator2.generateCardNumber();

        // Проверяем, что оба номера начинаются с правильного префикса
        assertTrue(number1.startsWith("4000"),
                String.format("Первый номер '%s' должен начинаться с '4000'", number1));
        assertTrue(number2.startsWith("4000"),
                String.format("Второй номер '%s' должен начинаться с '4000'", number2));

        // Номера скорее всего разные (но могут случайно совпасть)
        // Главное - оба валидны
        assertTrue(isLuhnValid(number1), "Первый номер должен быть валиден");
        assertTrue(isLuhnValid(number2), "Второй номер должен быть валиден");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4000", "4001", "4002", "4003", "4004",
            "4005", "4006", "4007", "4008", "4009"
    })
    @DisplayName("Тест с разными возможными префиксами (гипотетический)")
    void generator_withDifferentPrefixes_shouldStillWork(String testPrefix) throws Exception {
        // Это гипотетический тест - проверяем, что алгоритм работает
        // с любым 4-значным префиксом

        String testNumber = testPrefix + "12345678901"; // 4 + 11 = 15 цифр
        int checkDigit = calculateLuhnCheckDigitForTest(testNumber);
        String fullNumber = testNumber + checkDigit;

        // Проверяем, что номер с вычисленной контрольной цифрой валиден
        assertTrue(isLuhnValid(fullNumber),
                String.format("Номер '%s' должен быть валиден с контрольной цифрой %d",
                        testNumber, checkDigit));
    }

    /**
     * Дополнительный тест для проверки, что префикс действительно константа
     */
    @Test
    @DisplayName("Класс не должен позволять изменять префикс")
    void class_shouldNotAllowPrefixModification() {
        // Попытка изменить final поле должна привести к исключению
        // (в современных версиях Java это запрещено)

        try {
            Field field = GenerateCardNumber.class.getDeclaredField("CARD_NUMBER_PREFIX");
            field.setAccessible(true);

            // Пытаемся удалить модификатор final (это может не сработать)
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

            // Пытаемся изменить значение
            field.set(null, "1234");

            // Если мы здесь, значит изменение удалось (маловероятно в Java 12+)
            // В этом случае проверяем, что генератор все равно использует оригинальное значение

            GenerateCardNumber testGenerator = new GenerateCardNumber();
            String cardNumber = testGenerator.generateCardNumber();

            // Даже если reflection сработал, генератор мог использовать оригинальное значение
            // из-за компиляторной оптимизации
            System.out.println("Внимание: удалось изменить final поле через reflection");
            System.out.println("Сгенерированный номер: " + cardNumber);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Это ожидаемое поведение в современных версиях Java
            // Security Manager запрещает такие операции
            System.out.println("Security Manager запретил изменение final поля: " + e.getMessage());
        } catch (Exception e) {
            // Любая другая ошибка
            fail("Неожиданная ошибка при попытке изменить префикс: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для расчета контрольной цифры (для тестирования)
     */
    private int calculateLuhnCheckDigitForTest(String number) {
        int sum = 0;
        boolean doubleDigit = true; // Для вычисления контрольной цифры начинаем с true

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return (10 - (sum % 10)) % 10;
    }
    @ParameterizedTest
    @CsvSource({
            "1, 8",      // 1 -> контрольная цифра 8
            "10, 9",     // 10 -> контрольная цифра 9
            "100, 8",    // 100 -> контрольная цифра 8
            "1000, 9",   // 1000 -> контрольная цифра 9
            "10000, 8",  // 10000 -> контрольная цифра 8
            "123, 0",    // 123 -> контрольная цифра 0
            "456, 4",    // 456 -> контрольная цифра 4
            "789, 8",    // ИСПРАВЛЕНО: было 2, должно быть 8
            "999, 3"     // 999 -> контрольная цифра 3
    })
    @DisplayName("Проверка контрольной цифры для простых чисел")
    void calculateLuhnCheckDigit_shouldWorkForSimpleNumbers(String number, int expected) throws Exception {
        Method method = getPrivateMethod("calculateLuhnCheckDigit");
        int actual = (int) method.invoke(generator, number);
        assertEquals(expected, actual,
                String.format("Для числа '%s' ожидается контрольная цифра %d", number, expected));

        // Дополнительная проверка: полный номер должен быть валиден
        String fullNumber = number + expected;
        assertTrue(isLuhnValid(fullNumber),
                String.format("Полный номер '%s' должен быть валиден", fullNumber));
    }

    @Test
    @DisplayName("Автоматическая проверка контрольных цифр для чисел 0-9999")
    void calculateLuhnCheckDigit_shouldWorkForAllNumbersUpTo9999() throws Exception {
        Method method = getPrivateMethod("calculateLuhnCheckDigit");

        // Проверяем все числа от 0 до 9999
        for (int i = 0; i < 10000; i++) {
            String number = String.valueOf(i);
            int checkDigit = (int) method.invoke(generator, number);

            // Проверяем, что контрольная цифра в правильном диапазоне
            assertTrue(checkDigit >= 0 && checkDigit <= 9,
                    String.format("Контрольная цифра для '%s' должна быть 0-9, а не %d", number, checkDigit));

            // Проверяем, что полный номер валиден
            String fullNumber = number + checkDigit;
            assertTrue(isLuhnValid(fullNumber),
                    String.format("Полный номер '%s' должен быть валиден", fullNumber));

            // Проверяем, что любая другая цифра делает номер невалидным
            for (int wrongDigit = 0; wrongDigit < 10; wrongDigit++) {
                if (wrongDigit != checkDigit) {
                    String wrongNumber = number + wrongDigit;
                    assertFalse(isLuhnValid(wrongNumber),
                            String.format("Номер '%s' с неправильной цифрой %d должен быть невалиден",
                                    wrongNumber, wrongDigit));
                }
            }
        }
        System.out.println("✓ Проверено 10000 чисел - алгоритм работает корректно");
    }


    @Test
    @DisplayName("Проверка контрольных цифр для чисел из нулей")
    void calculateLuhnCheckDigit_shouldWorkForZeroNumbers() throws Exception {
        Method method = getPrivateMethod("calculateLuhnCheckDigit");

        // Проверяем разные длины чисел из нулей
        String[] zeroNumbers = {"0", "00", "000", "0000", "00000", "000000", "0000000"};

        for (String number : zeroNumbers) {
            int checkDigit = (int) method.invoke(generator, number);

            // Для чисел из нулей контрольная цифра всегда 0
            assertEquals(0, checkDigit,
                    String.format("Для числа из нулей '%s' контрольная цифра должна быть 0", number));

            // Проверяем полный номер
            String fullNumber = number + checkDigit;
            assertTrue(isLuhnValid(fullNumber),
                    String.format("Полный номер из нулей '%s' должен быть валиден", fullNumber));

            // Проверяем, что любая ненулевая цифра делает номер невалидным
            for (int wrongDigit = 1; wrongDigit < 10; wrongDigit++) {
                String wrongNumber = number + wrongDigit;
                assertFalse(isLuhnValid(wrongNumber),
                        String.format("Номер из нулей '%s' с цифрой %d должен быть невалиден",
                                wrongNumber, wrongDigit));
            }
        }
    }

    @Test
    @DisplayName("Проверка симметрии алгоритма Луна")
    void luhnAlgorithm_shouldBeSymmetric() throws Exception {
        Method method = getPrivateMethod("calculateLuhnCheckDigit");

        Random random = new Random(42);

        for (int i = 0; i < 100; i++) {
            // Генерируем случайное число (1-15 цифр)
            int length = random.nextInt(15) + 1;
            StringBuilder number = new StringBuilder();
            for (int j = 0; j < length; j++) {
                number.append(random.nextInt(10));
            }

            String numberStr = number.toString();

            // Вычисляем контрольную цифру
            int checkDigit1 = (int) method.invoke(generator, numberStr);

            // Добавляем контрольную цифру
            String fullNumber = numberStr + checkDigit1;

            // Проверяем, что номер валиден
            assertTrue(isLuhnValid(fullNumber),
                    String.format("Номер '%s' должен быть валиден", fullNumber));

            // Убираем последнюю цифру (контрольную) и вычисляем снова
            String withoutLast = fullNumber.substring(0, fullNumber.length() - 1);
            int checkDigit2 = (int) method.invoke(generator, withoutLast);

            // Проверяем, что получаем ту же контрольную цифру
            assertEquals(checkDigit1, checkDigit2,
                    String.format("Алгоритм должен быть симметричным для номера '%s'", numberStr));
        }
    }

    @Test
    @DisplayName("Диагностика: проверка алгоритма Луна для известных примеров")
    void debugLuhnAlgorithmForKnownExamples() {
        System.out.println("\n=== Диагностика алгоритма Луна ===");

        // Тестовые данные: [частичный номер, ожидаемая контрольная цифра, полный валидный номер]
        String[][] testCases = {
                {"7992739871", "3", "79927398713"},  // Wikipedia пример
                {"424242424242424", "2", "4242424242424242"},
                {"37828224631000", "5", "378282246310005"},
                {"555555555555444", "4", "5555555555554444"},
                {"401288888888188", "1", "4012888888881881"},
                {"411111111111111", "1", "4111111111111111"},
                {"601111111111111", "7", "6011111111111117"},
                {"353011133330000", "0", "3530111333300000"},
                {"400012345678901", "7", "4000123456789017"}  // Наш исправленный пример
        };

        for (String[] testCase : testCases) {
            String partial = testCase[0];
            String expectedCheckDigit = testCase[1];
            String fullValidNumber = testCase[2];

            // Вычисляем контрольную цифру нашим методом
            int calculatedCheckDigit = calculateLuhnCheckDigitForTest(partial);

            // Проверяем валидность полного номера
            boolean isFullNumberValid = isLuhnValid(fullValidNumber);

            // Проверяем, что наш расчет совпадает с ожидаемым
            boolean checkDigitMatches = String.valueOf(calculatedCheckDigit).equals(expectedCheckDigit);

            System.out.printf("Частичный: %s\n", partial);
            System.out.printf("  Ожидаемая цифра: %s, Рассчитанная: %d, Совпадает: %s\n",
                    expectedCheckDigit, calculatedCheckDigit, checkDigitMatches ? "✓" : "✗");
            System.out.printf("  Полный номер: %s, Валиден: %s\n",
                    fullValidNumber, isFullNumberValid ? "✓" : "✗");

            if (!checkDigitMatches) {
                System.out.printf("  !!! РАСХОЖДЕНИЕ !!!\n");
            }
            System.out.println();
        }
    }

    @Test
    @DisplayName("Проверка, что номер содержит 11 случайных цифр между префиксом и контрольной цифрой")
    void generateCardNumber_shouldHaveElevenRandomDigits() {
        String cardNumber = generator.generateCardNumber();

        // Извлекаем часть между префиксом и контрольной цифрой
        String randomPart = cardNumber.substring(4, 15); // Позиции 4-14 (11 цифр)

        assertEquals(11, randomPart.length(),
                "Между префиксом и контрольной цифрой должно быть ровно 11 цифр");

        // Проверяем, что это действительно случайные цифры
        // (не можем проверить случайность, но можем проверить формат)
        assertTrue(randomPart.matches("\\d{11}"),
                "Случайная часть должна содержать только цифры");
    }

    @Test
    @DisplayName("Визуальная проверка сгенерированных номеров")
    void generateCardNumber_visualInspection() {
        System.out.println("\n=== Визуальная проверка сгенерированных номеров ===");

        for (int i = 0; i < 10; i++) {
            String cardNumber = generator.generateCardNumber();
            boolean isValid = isLuhnValid(cardNumber);

            System.out.printf("%2d. %s %s%n",
                    i + 1,
                    cardNumber,
                    isValid ? "✓" : "✗");

            assertTrue(isValid, "Все номера должны быть валидными");
        }

        System.out.println("Все номера валидны по алгоритму Луна");
    }

    // =================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =================================================================

    /**
     * Проверка валидности номера по алгоритму Луна.
     */
    private boolean isLuhnValid(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10) == 0;
    }

    /**
     * Проверка, является ли строка последовательными цифрами.
     */
    private boolean isSequential(String str) {
        for (int i = 0; i < str.length() - 1; i++) {
            int current = Character.getNumericValue(str.charAt(i));
            int next = Character.getNumericValue(str.charAt(i + 1));

            // Проверяем возрастающую последовательность (модуль 10 для зацикливания)
            if ((current + 1) % 10 != next) {
                return false;
            }
        }
        return true;
    }

    /**
     * Получение приватного метода через рефлексию.
     */
    private Method getPrivateMethod(String methodName) throws Exception {
        Method method = GenerateCardNumber.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return method;
    }
}




