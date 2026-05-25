package com.example.userservice.mapper;

import com.example.userservice.dto.card.CardResponse;
import com.example.userservice.dto.user.UserCreateRequest;
import com.example.userservice.dto.user.UserResponse;
import com.example.userservice.dto.user.UserUpdateRequest;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-25T02:09:18+0300",
    comments = "version: 1.6.2, compiler: javac, environment: Java 21.0.5 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.name( request.getName() );
        user.surname( request.getSurname() );
        user.birthDate( request.getBirthDate() );
        user.email( request.getEmail() );

        return user.build();
    }

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.cards( paymentCardListToCardResponseList( user.getPaymentCards() ) );
        userResponse.id( user.getId() );
        userResponse.name( user.getName() );
        userResponse.surname( user.getSurname() );
        userResponse.birthDate( user.getBirthDate() );
        userResponse.email( user.getEmail() );
        userResponse.active( user.getActive() );
        userResponse.createdAt( user.getCreatedAt() );
        userResponse.updatedAt( user.getUpdatedAt() );

        return userResponse.build();
    }

    @Override
    public List<UserResponse> toResponseList(List<User> users) {
        if ( users == null ) {
            return null;
        }

        List<UserResponse> list = new ArrayList<UserResponse>( users.size() );
        for ( User user : users ) {
            list.add( toResponse( user ) );
        }

        return list;
    }

    @Override
    public void updateEntity(UserUpdateRequest request, User user) {
        if ( request == null ) {
            return;
        }

        if ( request.getName() != null ) {
            user.setName( request.getName() );
        }
        if ( request.getSurname() != null ) {
            user.setSurname( request.getSurname() );
        }
        if ( request.getBirthDate() != null ) {
            user.setBirthDate( request.getBirthDate() );
        }
    }

    protected CardResponse paymentCardToCardResponse(PaymentCard paymentCard) {
        if ( paymentCard == null ) {
            return null;
        }

        CardResponse.CardResponseBuilder cardResponse = CardResponse.builder();

        cardResponse.id( paymentCard.getId() );
        cardResponse.holder( paymentCard.getHolder() );
        cardResponse.expirationDate( paymentCard.getExpirationDate() );
        cardResponse.active( paymentCard.getActive() );
        cardResponse.createdAt( paymentCard.getCreatedAt() );
        cardResponse.updatedAt( paymentCard.getUpdatedAt() );

        return cardResponse.build();
    }

    protected List<CardResponse> paymentCardListToCardResponseList(List<PaymentCard> list) {
        if ( list == null ) {
            return null;
        }

        List<CardResponse> list1 = new ArrayList<CardResponse>( list.size() );
        for ( PaymentCard paymentCard : list ) {
            list1.add( paymentCardToCardResponse( paymentCard ) );
        }

        return list1;
    }
}
