package com.innowise.userservice.dto.user;


import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String surname;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
}