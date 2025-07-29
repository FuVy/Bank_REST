package com.example.bankcards.service;

import com.example.bankcards.dto.security.JwtAuthResponse;
import com.example.bankcards.dto.security.LoginRequest;
import com.example.bankcards.dto.security.RegisterRequest;

public interface AuthService {
    JwtAuthResponse login(LoginRequest loginRequest);
    JwtAuthResponse register(RegisterRequest registerRequest);
}