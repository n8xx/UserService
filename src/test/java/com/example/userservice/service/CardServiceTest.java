package com.example.userservice.service;

import com.example.userservice.dto.card.CardCreateRequest;
import com.example.userservice.dto.card.CardResponse;
import com.example.userservice.dto.card.CardUpdateRequest;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.BusinessException;
import com.example.userservice.exception.CardNotFoundException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.CardMapper;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private User user;
    private PaymentCard card;
    private CardResponse cardResponse;
    private CardCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Anna")
                .active(true)
                .build();

        card = PaymentCard.builder()
                .id(1L)
                .user(user)
                .number("1234567890123456")
                .holder("ANNA IVANOVA")
                .expirationDate(LocalDate.of(2027, 12, 1))
                .active(true)
                .build();

        cardResponse = CardResponse.builder()
                .id(1L)
                .userId(1L)
                .maskedNumber("**** **** **** 3456")
                .holder("ANNA IVANOVA")
                .active(true)
                .build();

        createRequest = new CardCreateRequest(
                "1234567890123456",
                "ANNA IVANOVA",
                LocalDate.of(2027, 12, 1),
                1L
        );
    }

    @Test
    void createCard_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardMapper.toEntity(createRequest)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.createCard(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getHolder()).isEqualTo("ANNA IVANOVA");
        verify(cardRepository).save(card);
    }

    @Test
    void createCard_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        createRequest = new CardCreateRequest(
                "1234567890123456",
                "ANNA IVANOVA",
                LocalDate.of(2027, 12, 1),
                99L
        );

        assertThatThrownBy(() -> cardService.createCard(createRequest))
                .isInstanceOf(UserNotFoundException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void createCard_inactiveUser_throwsException() {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cardService.createCard(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");

        verify(cardRepository, never()).save(any());
    }

    @Test
    void getCardById_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.getCardById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCardById_notFound_throwsException() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardById(99L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getCardsByUserId_success() {
        List<PaymentCard> cards = List.of(card);
        List<CardResponse> responses = List.of(cardResponse);

        when(cardRepository.findAllByUserId(1L)).thenReturn(cards);
        when(cardMapper.toResponseList(cards)).thenReturn(responses);

        List<CardResponse> result = cardService.getCardsByUserId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void deactivateCard_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.deactivateCard(1L);

        assertThat(card.getActive()).isFalse();
        verify(cardRepository).save(card);
    }

    @Test
    void activateCard_success() {
        card.setActive(false);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.activateCard(1L);

        assertThat(card.getActive()).isTrue();
        verify(cardRepository).save(card);
    }
}