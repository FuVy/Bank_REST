package com.example.bankcards.dto.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class LoginRequest {
    @NotBlank(message = "Username can't be blank.")
    String username;

    @NotBlank(message = "Password can't be blank.")
    String password;
}
