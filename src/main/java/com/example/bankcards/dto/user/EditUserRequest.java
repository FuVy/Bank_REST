package com.example.bankcards.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class EditUserRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    String username;
    //new fields may be added later
}
