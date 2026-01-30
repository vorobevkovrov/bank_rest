package com.example.bankcards.util;

import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.entity.CardRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CardRequestMapper {
    @Mapping(source = "id", target = "requestId")
    @Mapping(source = "card.id", target = "cardId")
    @Mapping(source = "card.cardNumber", target = "cardMaskedNumber",
            qualifiedByName = "maskCardNumber")
    CardRequestResponse toResponse(CardRequest cardRequest);

    @Named("maskCardNumber")
    default String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return null;
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}
