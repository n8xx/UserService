package com.example.userservice.service;

import com.example.userservice.entity.PaymentCard;
import com.example.userservice.exception.BusinessException;
import com.example.userservice.repository.specification.CardSpecification;
import com.example.userservice.dto.card.CardCreateRequest;
import com.example.userservice.dto.card.CardResponse;
import com.example.userservice.dto.card.CardUpdateRequest;
import com.example.userservice.entity.User;
import com.example.userservice.exception.CardNotFoundException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.CardMapper;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CardService {

    private static final int MAX_CARDS_PER_USER = 5;

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public CardResponse createCard(Long userId, CardCreateRequest request) {
        log.info("Creating card for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getActive()) {
            throw new BusinessException("Cannot add card to inactive user");
        }

        if (cardRepository.countByUserId(userId) >= MAX_CARDS_PER_USER) {
            throw new BusinessException("User cannot have more than " + MAX_CARDS_PER_USER + " cards");
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

    public Page<CardResponse> getCardsByUserId(Long userId, String holder, Pageable pageable) {
        Specification<PaymentCard> spec = Specification
                .where(CardSpecification.hasUserId(userId))
                .and(CardSpecification.hasHolder(holder));
        return cardRepository.findAll(spec, pageable).map(cardMapper::toResponse);
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public CardResponse updateCard(Long userId, Long id, CardUpdateRequest request) {
        log.info("Updating card with id: {}", id);
        PaymentCard card = findCardOrThrow(id);
        cardMapper.updateEntity(request, card);
        return cardMapper.toResponse(cardRepository.save(card));
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public void deactivateCard(Long userId, Long id) {
        log.info("Deactivating card with id: {}", id);
        PaymentCard card = findCardOrThrow(id);
        card.setActive(false);
        cardRepository.save(card);
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public void activateCard(Long userId, Long id) {
        log.info("Activating card with id: {}", id);
        PaymentCard card = findCardOrThrow(id);
        card.setActive(true);
        cardRepository.save(card);
    }

    private PaymentCard findCardOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }
}
