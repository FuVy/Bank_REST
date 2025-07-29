package com.example.bankcards.dto.user;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
public class UserDto {
    UUID id;
    String username;
    List<String> roles;
    LocalDateTime createdAt;
}
