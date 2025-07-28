package com.example.bankcards.exception.user;

import com.example.bankcards.exception.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(String username) {
        super(String.format("User not found with username: %s.", username));
    }

    public UserNotFoundException(UUID id) {
        super(String.format("User not found with ID: %s.", id));
    }
}
