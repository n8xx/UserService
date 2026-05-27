package com.example.userservice.dto.card;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardUpdateRequest {

    @Size(max = 255)
    private String holder;

    private Boolean active;
}