package com.example.userservice.controller;

import com.example.userservice.dto.card.CardCreateRequest;
import com.example.userservice.dto.card.CardResponse;
import com.example.userservice.dto.card.CardUpdateRequest;
import com.example.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CardResponse>> getCardsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getCardsByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardUpdateRequest request) {
        return ResponseEntity.ok(cardService.updateCard(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(@PathVariable Long id) {
        cardService.deactivateCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }
}