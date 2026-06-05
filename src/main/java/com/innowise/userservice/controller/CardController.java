package com.innowise.userservice.controller;

import com.innowise.userservice.dto.card.CardCreateRequest;
import com.innowise.userservice.dto.card.CardResponse;
import com.innowise.userservice.dto.card.CardUpdateRequest;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/cards")
@RequiredArgsConstructor
public class CardController {
    
    private static final String ROLE_PREFIX = "ROLE_";
    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long userId,
            @Valid @RequestBody CardCreateRequest request,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority()
                .replace(ROLE_PREFIX, "");
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(userId, request,currentUserId, role));
    }

    @GetMapping
    public ResponseEntity<Page<CardResponse>> getCardsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String holder,
            Pageable pageable,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority()
                .replace(ROLE_PREFIX, "");
        return ResponseEntity.ok(cardService.getCardsByUserId(userId, holder, pageable,currentUserId, role));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority()
                .replace(ROLE_PREFIX, "");
        return ResponseEntity.ok(cardService.getCardById(userId,cardId,currentUserId, role));
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @Valid @RequestBody CardUpdateRequest request,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority()
                .replace(ROLE_PREFIX, "");
        return ResponseEntity.ok(cardService.updateCard(userId, cardId, request,currentUserId, role));
    }

    @PatchMapping("/{cardId}/deactivate")
    public ResponseEntity<Void> deactivateCard(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority()
                .replace(ROLE_PREFIX, "");
        cardService.deactivateCard(userId, cardId,currentUserId, role);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cardId}/activate")
    public ResponseEntity<Void> activateCard(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority()
                .replace(ROLE_PREFIX, "");
        cardService.activateCard(userId, cardId,currentUserId, role);
        return ResponseEntity.noContent().build();
    }
}
