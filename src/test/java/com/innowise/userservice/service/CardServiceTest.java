package com.innowise.userservice.service;

import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.dto.card.CardCreateRequest;
import com.innowise.userservice.dto.card.CardResponse;
import com.innowise.userservice.dto.card.CardUpdateRequest;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long CARD_ID = 1L;
    private static final String ADMIN = "ADMIN";
    private static final String USER = "USER";

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
                .id(USER_ID)
                .name("Anna")
                .active(true)
                .build();

        card = PaymentCard.builder()
                .id(CARD_ID)
                .user(user)
                .number("1234567890123456")
                .holder("ANNA IVANOVA")
                .expirationDate(LocalDate.of(2027, 12, 1))
                .active(true)
                .build();

        cardResponse = CardResponse.builder()
                .id(CARD_ID)
                .userId(USER_ID)
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
    @DisplayName("createCard: success for own user")
    void createCard_success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(cardMapper.toEntity(createRequest)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.createCard(USER_ID, createRequest, USER_ID, USER);

        assertThat(result).isNotNull();
        assertThat(result.getHolder()).isEqualTo("ANNA IVANOVA");
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("createCard: success for admin accessing another user")
    void createCard_successForAdmin() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(cardMapper.toEntity(createRequest)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.createCard(USER_ID, createRequest, OTHER_USER_ID, ADMIN);

        assertThat(result).isNotNull();
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("createCard: throws AccessDeniedException for non-owner user")
    void createCard_throwsAccessDenied_forNonOwner() {
        assertThatThrownBy(() -> cardService.createCard(USER_ID, createRequest, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCard: throws UserNotFoundException when user not found")
    void createCard_userNotFound_throwsException() {
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(OTHER_USER_ID, createRequest, OTHER_USER_ID, USER))
                .isInstanceOf(UserNotFoundException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCard: throws BusinessException for inactive user")
    void createCard_inactiveUser_throwsException() {
        user.setActive(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cardService.createCard(USER_ID, createRequest, USER_ID, USER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCard: throws BusinessException when max cards reached")
    void createCard_maxCardsReached_throwsException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserId(USER_ID)).thenReturn(5L);

        assertThatThrownBy(() -> cardService.createCard(USER_ID, createRequest, USER_ID, USER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCardById: success for own user")
    void getCardById_success() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.getCardById(USER_ID, CARD_ID, USER_ID, USER);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CARD_ID);
    }

    @Test
    @DisplayName("getCardById: throws AccessDeniedException for non-owner user")
    void getCardById_throwsAccessDenied_forNonOwner() {
        assertThatThrownBy(() -> cardService.getCardById(USER_ID, CARD_ID, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getCardById: success for admin accessing another user")
    void getCardById_successForAdmin() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.getCardById(USER_ID, CARD_ID, OTHER_USER_ID, ADMIN);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getCardById: throws CardNotFoundException when not found")
    void getCardById_notFound_throwsException() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardById(USER_ID, CARD_ID, USER_ID, USER))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    @DisplayName("getCardsByUserId: success for own user")
    void getCardsByUserId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> page = new PageImpl<>(List.of(card));
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        Page<CardResponse> result = cardService.getCardsByUserId(USER_ID, null, pageable, USER_ID, USER);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getHolder()).isEqualTo("ANNA IVANOVA");
    }

    @Test
    @DisplayName("getCardsByUserId: throws AccessDeniedException for non-owner user")
    void getCardsByUserId_throwsAccessDenied_forNonOwner() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> cardService.getCardsByUserId(USER_ID, null, pageable, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateCard: success for own user")
    void updateCard_success() {
        CardUpdateRequest updateRequest = new CardUpdateRequest("NEW HOLDER", null);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(cardResponse);

        CardResponse result = cardService.updateCard(USER_ID, CARD_ID, updateRequest, USER_ID, USER);

        assertThat(result).isNotNull();
        verify(cardMapper).updateEntity(updateRequest, card);
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("updateCard: throws AccessDeniedException for non-owner user")
    void updateCard_throwsAccessDenied_forNonOwner() {
        CardUpdateRequest updateRequest = new CardUpdateRequest("NEW HOLDER", null);

        assertThatThrownBy(() -> cardService.updateCard(USER_ID, CARD_ID, updateRequest, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateCard: throws CardNotFoundException when not found")
    void updateCard_notFound_throwsException() {
        CardUpdateRequest updateRequest = new CardUpdateRequest("NEW HOLDER", null);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.updateCard(USER_ID, CARD_ID, updateRequest, USER_ID, USER))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    @DisplayName("deactivateCard: success for own user")
    void deactivateCard_success() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

        cardService.deactivateCard(USER_ID, CARD_ID, USER_ID, USER);

        assertThat(card.getActive()).isFalse();
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("deactivateCard: throws AccessDeniedException for non-owner user")
    void deactivateCard_throwsAccessDenied_forNonOwner() {
        assertThatThrownBy(() -> cardService.deactivateCard(USER_ID, CARD_ID, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("activateCard: success for own user")
    void activateCard_success() {
        card.setActive(false);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));

        cardService.activateCard(USER_ID, CARD_ID, USER_ID, USER);

        assertThat(card.getActive()).isTrue();
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("activateCard: throws AccessDeniedException for non-owner user")
    void activateCard_throwsAccessDenied_forNonOwner() {
        assertThatThrownBy(() -> cardService.activateCard(USER_ID, CARD_ID, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);
    }
}