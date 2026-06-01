package com.innowise.userservice.mapper;

import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.dto.card.CardCreateRequest;
import com.innowise.userservice.dto.card.CardResponse;
import com.innowise.userservice.dto.card.CardUpdateRequest;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "maskedNumber", expression = "java(maskCardNumber(card.getNumber()))")
    @Mapping(target = "holder", source = "card.holder")  // добавь это
    CardResponse toResponse(PaymentCard card);

    List<CardResponse> toResponseList(List<PaymentCard> cards);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    PaymentCard toEntity(CardCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "expirationDate", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(CardUpdateRequest request, @MappingTarget PaymentCard card);

    default String maskCardNumber(String number) {
        if (number == null || number.length() < 4) return "****";
        return "**** **** **** " + number.substring(number.length() - 4);
    }
}