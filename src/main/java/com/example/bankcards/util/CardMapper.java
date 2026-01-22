package com.example.bankcards.util;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Mapper(componentModel = "spring")
public interface CardMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cardNumber", source = "cardNumberEncrypted")
    @Mapping(target = "cardNumberLastFour", source = "cardNumberLastFour")
    @Mapping(target = "expiryDate", source = "expiryDate")
    @Mapping(target = "status", source = "cardStatus")
    @Mapping(target = "balance", source = "initialBalance")
    @Mapping(target = "user", source = "userId")
    @Mapping(target = "cardHolderName", source = "cardHolderName")
    Card cardRequestToCard(CardCreateRequest request);

    @Mapping(target = "maskedNumber", source = "cardNumberLastFour")
    @Mapping(target = "holderName", source = "cardHolderName")
    @Mapping(target = "expiryDate", source = "expiryDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "userId", source = "user.id")
    //@Mapping(target = "holderName", source = "user.username")
    CardResponse cardToCardResponse(Card card);

    // Преобразование LocalDate -> Date
    default Date map(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // Преобразование Date -> LocalDate
    default LocalDate map(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // Преобразование Long userId -> User объект
    default User map(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = new User();
        user.setId(userId);
        return user;
    }

    // Создание маскированного номера для CardResponse
    @AfterMapping
    default void setMaskedNumber(@MappingTarget CardResponse cardResponse, Card card) {
        if (card.getCardNumberLastFour() != null && !card.getCardNumberLastFour().isEmpty()) {
            cardResponse.setMaskedNumber("**** **** **** " + card.getCardNumberLastFour());
        }
    }

    // Альтернативный вариант через выражение (можно использовать вместо @AfterMapping)
    @Named("maskNumber")
    default String maskNumber(String lastFourDigits) {
        if (lastFourDigits == null || lastFourDigits.isEmpty()) {
            return null;
        }
        return "**** **** **** " + lastFourDigits;
    }
}