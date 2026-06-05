package com.innowise.userservice.controller;

import com.innowise.userservice.dto.card.CardCreateRequest;
import com.innowise.userservice.dto.card.CardResponse;
import com.innowise.userservice.dto.card.CardUpdateRequest;
import com.innowise.userservice.security.AuthUser;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long userId,
            @Valid @RequestBody CardCreateRequest request,
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.createCard(userId, request, authUser.getUserId(), authUser.getRole()));
    }

    @GetMapping
    public ResponseEntity<Page<CardResponse>> getCardsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String holder,
            Pageable pageable,
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(cardService.getCardsByUserId(userId, holder, pageable, authUser.getUserId(), authUser.getRole()));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(cardService.getCardById(userId, cardId, authUser.getUserId(), authUser.getRole()));
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @Valid @RequestBody CardUpdateRequest request,
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(cardService.updateCard(userId, cardId, request, authUser.getUserId(), authUser.getRole()));
    }

    @PatchMapping("/{cardId}/deactivate")
    public ResponseEntity<Void> deactivateCard(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal AuthUser authUser) {
        cardService.deactivateCard(userId, cardId, authUser.getUserId(), authUser.getRole());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cardId}/activate")
    public ResponseEntity<Void> activateCard(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal AuthUser authUser) {
        cardService.activateCard(userId, cardId, authUser.getUserId(), authUser.getRole());
        return ResponseEntity.noContent().build();
    }
}
