package com.example.bankcards.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CardNotOwnedByUser extends RuntimeException {
    public CardNotOwnedByUser(String message) {
        super(message);
    }

    public CardNotOwnedByUser(UUID cardId, UUID ownerId) {
        super(String.format("Card with ID \"%s\" isn't owned by user with ID \"%s\" or doesn't exist.", cardId, ownerId));
    }
}
