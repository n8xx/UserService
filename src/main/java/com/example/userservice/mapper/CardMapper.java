package com.example.userservice.mapper;

import com.example.userservice.dto.card.CardCreateRequest;
import com.example.userservice.dto.card.CardResponse;
import com.example.userservice.dto.card.CardUpdateRequest;
import com.example.userservice.entity.PaymentCard;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "maskedNumber", expression = "java(maskCardNumber(card.getNumber()))")
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