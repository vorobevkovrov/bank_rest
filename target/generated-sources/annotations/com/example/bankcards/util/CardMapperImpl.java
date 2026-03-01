package com.example.bankcards.util;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-24T09:50:42+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.3 (Oracle Corporation)"
)
@Component
public class CardMapperImpl implements CardMapper {

    @Override
    public Card cardRequestToCard(CardCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Card.CardBuilder card = Card.builder();

        card.expiryDate( request.expiryDate() );
        card.balance( request.initialBalance() );
        card.user( map( request.userId() ) );
        card.cardHolderName( request.cardHolderName() );

        return card.build();
    }

    @Override
    public CardResponse cardToCardResponse(Card card) {
        if ( card == null ) {
            return null;
        }

        CardResponse.CardResponseBuilder cardResponse = CardResponse.builder();

        cardResponse.maskedNumber( card.getCardNumberLastFour() );
        cardResponse.holderName( card.getCardHolderName() );
        cardResponse.expiryDate( card.getExpiryDate() );
        cardResponse.status( card.getStatus() );
        cardResponse.balance( card.getBalance() );
        cardResponse.userId( cardUserId( card ) );

        return cardResponse.build();
    }

    @Override
    public Card cardUpdateRequestToCardResponse(CardUpdateRequest cardUpdateRequest) {
        if ( cardUpdateRequest == null ) {
            return null;
        }

        Card.CardBuilder card = Card.builder();

        card.expiryDate( cardUpdateRequest.expiryDate() );
        card.status( cardUpdateRequest.status() );
        card.balance( cardUpdateRequest.balance() );

        return card.build();
    }

    private Long cardUserId(Card card) {
        User user = card.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}
