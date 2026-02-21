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
    date = "2026-02-21T18:26:37+0300",
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

        card.expiryDate( request.getExpiryDate() );
        card.balance( request.getInitialBalance() );
        card.user( map( request.getUserId() ) );
        card.cardHolderName( request.getCardHolderName() );

        return card.build();
    }

    @Override
    public CardResponse cardToCardResponse(Card card) {
        if ( card == null ) {
            return null;
        }

        CardResponse cardResponse = new CardResponse();

        cardResponse.setMaskedNumber( card.getCardNumberLastFour() );
        cardResponse.setHolderName( card.getCardHolderName() );
        cardResponse.setExpiryDate( card.getExpiryDate() );
        cardResponse.setStatus( card.getStatus() );
        cardResponse.setBalance( card.getBalance() );
        cardResponse.setUserId( cardUserId( card ) );

        setMaskedNumber( cardResponse, card );

        return cardResponse;
    }

    @Override
    public Card cardUpdateRequestToCardResponse(CardUpdateRequest cardUpdateRequest) {
        if ( cardUpdateRequest == null ) {
            return null;
        }

        Card.CardBuilder card = Card.builder();

        card.expiryDate( cardUpdateRequest.getExpiryDate() );
        card.status( cardUpdateRequest.getStatus() );
        card.balance( cardUpdateRequest.getBalance() );

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
