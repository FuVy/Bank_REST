package com.example.bankcards.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class RegisterRequest {
    @NotBlank(message = "Username can't be blank.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    String username;

    @NotBlank(message = "Password can't be blank.")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long.")
    String password;
}
