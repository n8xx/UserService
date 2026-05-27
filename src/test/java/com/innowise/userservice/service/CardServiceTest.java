package com.example.userservice.service;

import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.BusinessException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.CardMapper;
import com.example.userservice.dto.card.CardCreateRequest;
import com.example.userservice.dto.card.CardResponse;
import com.example.userservice.dto.card.CardUpdateRequest;
import com.example.userservice.exception.CardNotFoundException;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                LocalDate.of(2027, 12, 1)
        );
    }

    @Test
    void createCard_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserId(1L)).thenReturn(0L);
        when(cardMapper.toEntity(createRequest)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.createCard(1L, createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getHolder()).isEqualTo("ANNA IVANOVA");
        verify(cardRepository).save(card);
    }

    @Test
    void createCard_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(99L, createRequest))
                .isInstanceOf(UserNotFoundException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void createCard_inactiveUser_throwsException() {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cardService.createCard(1L, createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");

        verify(cardRepository, never()).save(any());
    }

    @Test
    void createCard_maxCardsReached_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserId(1L)).thenReturn(5L);

        assertThatThrownBy(() -> cardService.createCard(1L, createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5");

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
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> page = new PageImpl<>(List.of(card));
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        Page<CardResponse> result = cardService.getCardsByUserId(1L, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getHolder()).isEqualTo("ANNA IVANOVA");
    }

    @Test
    void updateCard_success() {
        CardUpdateRequest updateRequest = new CardUpdateRequest("NEW HOLDER", null);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.updateCard(1L, 1L, updateRequest);

        assertThat(result).isNotNull();
        verify(cardMapper).updateEntity(updateRequest, card);
        verify(cardRepository).save(card);
    }

    @Test
    void updateCard_notFound_throwsException() {
        CardUpdateRequest updateRequest = new CardUpdateRequest("NEW HOLDER", null);
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.updateCard(1L, 99L, updateRequest))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void deactivateCard_success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.deactivateCard(1L, 1L);

        assertThat(card.getActive()).isFalse();
        verify(cardRepository).save(card);
    }

    @Test
    void activateCard_success() {
        card.setActive(false);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.activateCard(1L, 1L);

        assertThat(card.getActive()).isTrue();
        verify(cardRepository).save(card);
    }
}
