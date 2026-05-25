package com.example.userservice.mapper;

import com.example.userservice.dto.card.CardCreateRequest;
import com.example.userservice.dto.card.CardResponse;
import com.example.userservice.dto.card.CardUpdateRequest;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-25T02:09:17+0300",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.5 (Oracle Corporation)"
)
@Component
public class CardMapperImpl implements CardMapper {

    @Override
    public CardResponse toResponse(PaymentCard card) {
        if ( card == null ) {
            return null;
        }

        CardResponse.CardResponseBuilder cardResponse = CardResponse.builder();

        cardResponse.userId( cardUserId( card ) );
        cardResponse.id( card.getId() );
        cardResponse.holder( maskCardNumber( card.getHolder() ) );
        cardResponse.expirationDate( card.getExpirationDate() );
        cardResponse.active( card.getActive() );
        cardResponse.createdAt( card.getCreatedAt() );
        cardResponse.updatedAt( card.getUpdatedAt() );

        cardResponse.maskedNumber( maskCardNumber(card.getNumber()) );

        return cardResponse.build();
    }

    @Override
    public List<CardResponse> toResponseList(List<PaymentCard> cards) {
        if ( cards == null ) {
            return null;
        }

        List<CardResponse> list = new ArrayList<CardResponse>( cards.size() );
        for ( PaymentCard paymentCard : cards ) {
            list.add( toResponse( paymentCard ) );
        }

        return list;
    }

    @Override
    public PaymentCard toEntity(CardCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        PaymentCard.PaymentCardBuilder paymentCard = PaymentCard.builder();

        paymentCard.number( maskCardNumber( request.getNumber() ) );
        paymentCard.holder( maskCardNumber( request.getHolder() ) );
        paymentCard.expirationDate( request.getExpirationDate() );

        return paymentCard.build();
    }

    @Override
    public void updateEntity(CardUpdateRequest request, PaymentCard card) {
        if ( request == null ) {
            return;
        }

        if ( request.getHolder() != null ) {
            card.setHolder( maskCardNumber( request.getHolder() ) );
        }
    }

    private Long cardUserId(PaymentCard paymentCard) {
        User user = paymentCard.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}
