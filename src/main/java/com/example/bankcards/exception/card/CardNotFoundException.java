package com.example.bankcards.exception.card;

import com.example.bankcards.exception.ResourceNotFoundException;

import java.util.UUID;

public class CardNotFoundException extends ResourceNotFoundException {
    public CardNotFoundException(String message) {
        super(message);
    }

    public CardNotFoundException(UUID id) {
        super(String.format("Card not found with ID: %s.", id));
    }
}
