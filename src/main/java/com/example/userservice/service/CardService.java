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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CardService {

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        log.info("Creating card for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        if (!user.getActive()) {
            throw new BusinessException("Cannot add card to inactive user");
        }

        PaymentCard card = cardMapper.toEntity(request);
        card.setUser(user);
        card.setActive(true);
        PaymentCard saved = cardRepository.save(card);

        log.info("Card created with id: {}", saved.getId());
        return cardMapper.toResponse(saved);
    }

    public CardResponse getCardById(Long id) {
        return cardMapper.toResponse(findCardOrThrow(id));
    }

    public List<CardResponse> getCardsByUserId(Long userId) {
        return cardMapper.toResponseList(cardRepository.findByUserId(userId));
    }

    @Transactional
    public CardResponse updateCard(Long id, CardUpdateRequest request) {
        PaymentCard card = findCardOrThrow(id);
        cardMapper.updateEntity(request, card);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deactivateCard(Long id) {
        log.info("Deactivating card with id: {}", id);
        PaymentCard card = findCardOrThrow(id);
        card.setActive(false);
        cardRepository.save(card);
    }

    private PaymentCard findCardOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }
    @Transactional
    public void activateCard(Long id) {
        log.info("Activating card with id: {}", id);
        PaymentCard card = findCardOrThrow(id);
        card.setActive(true);
        cardRepository.save(card);
    }
}