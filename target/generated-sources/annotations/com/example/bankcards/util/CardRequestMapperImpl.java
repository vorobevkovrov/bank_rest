package com.example.bankcards.util;

import com.example.bankcards.dto.response.CardRequestResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardRequest;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-24T09:50:42+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.3 (Oracle Corporation)"
)
@Component
public class CardRequestMapperImpl implements CardRequestMapper {

    @Override
    public CardRequestResponse toResponse(CardRequest cardRequest) {
        if ( cardRequest == null ) {
            return null;
        }

        CardRequestResponse.CardRequestResponseBuilder cardRequestResponse = CardRequestResponse.builder();

        cardRequestResponse.requestId( cardRequest.getId() );
        cardRequestResponse.cardId( cardRequestCardId( cardRequest ) );
        cardRequestResponse.cardMaskedNumber( maskCardNumber( cardRequestCardCardNumber( cardRequest ) ) );
        cardRequestResponse.status( cardRequest.getStatus() );
        cardRequestResponse.createdAt( cardRequest.getCreatedAt() );

        return cardRequestResponse.build();
    }

    private Long cardRequestCardId(CardRequest cardRequest) {
        Card card = cardRequest.getCard();
        if ( card == null ) {
            return null;
        }
        return card.getId();
    }

    private String cardRequestCardCardNumber(CardRequest cardRequest) {
        Card card = cardRequest.getCard();
        if ( card == null ) {
            return null;
        }
        return card.getCardNumber();
    }
}
