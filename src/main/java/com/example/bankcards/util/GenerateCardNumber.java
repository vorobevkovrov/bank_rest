package com.example.bankcards.util;

import org.springframework.stereotype.Component;

@Component
public class GenerateCardNumber {
    /**
     * Префикс номера карты для генерации.
     * По умолчанию используется префикс "4000" (Visa) для демонстрационных целей.
     */
    private static final String CARD_NUMBER_PREFIX = "4000"; // Visa prefix для примера

    /**
     * Генерирует 16-значный номер банковской карты, совместимый с алгоритмом Луна.
     * Номер состоит из префикса, 11 случайных цифр и контрольной цифры, вычисленной по алгоритму Луна.
     *
     * <p>Алгоритм генерации:
     * <ol>
     *   <li>Добавляется префикс {@value #CARD_NUMBER_PREFIX} (4 цифры)
     *   <li>Генерируются 11 случайных цифр (0-9)
     *   <li>Вычисляется контрольная цифра по алгоритму Луна для полученных 15 цифр
     *   <li>Контрольная цифра добавляется в конец номера
     * </ol>
     *
     * @return 16-значный номер карты в виде строки. Формат: "4000XXXXXXXXXXXX", где X - цифры
     * @see #calculateLuhnCheckDigit(String)
     */
    public String generateCardNumber() {
        // Генерация 16-значного номера карты (Luhn-совместимого)
        StringBuilder cardNumber = new StringBuilder(CARD_NUMBER_PREFIX);

        // Генерируем 11 случайных цифр (вместе с префиксом будет 15 цифр)
        for (int i = 0; i < 11; i++) {
            cardNumber.append((int) (Math.random() * 10));
        }

        // Добавляем контрольную цифру по алгоритму Луна
        String numberWithoutCheckDigit = cardNumber.toString();
        int checkDigit = calculateLuhnCheckDigit(numberWithoutCheckDigit);
        cardNumber.append(checkDigit);
        return cardNumber.toString();
    }

    /**
     * Вычисляет контрольную цифру для заданного номера по алгоритму Луна.
     *
     * <p>Алгоритм вычисления:
     * <ol>
     *   <li>Цифры обрабатываются справа налево</li>
     *   <li>Начинаем с предпоследней цифры (не удваиваем самую правую)</li>
     *   <li>Каждая вторая цифра удваивается</li>
     *   <li>Если результат удвоения больше 9, вычитается 9</li>
     *   <li>Суммируются все цифры</li>
     *   <li>Контрольная цифра вычисляется как (10 - (сумма % 10)) % 10</li>
     * </ol>
     *
     * @param number строка, содержащая цифры, для которых нужно вычислить контрольную цифру
     * @return контрольная цифра (0-9)
     */
    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        // Начинаем с false, чтобы НЕ удваивать самую правую цифру
        // (она считается позицией для контрольной цифры при вычислении)
        boolean doubleDigit = true;
        // Валидация входных данных
        if (number == null) {
            throw new IllegalArgumentException("Входная строка не может быть null");
        }

        // Проверяем, что строка содержит только цифры
        if (!number.isEmpty() && !number.matches("\\d+")) {
            throw new IllegalArgumentException("Входная строка должна содержать только цифры: " + number);
        }

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
}