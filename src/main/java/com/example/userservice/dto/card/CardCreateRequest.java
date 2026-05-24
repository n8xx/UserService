package com.example.userservice.dto.card;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
    private String number;

    @NotBlank(message = "Cardholder name is required")
    private String holder;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Card must not be expired")
    private LocalDate expirationDate;

    @NotNull(message = "User ID is required")
    private Long userId;
}