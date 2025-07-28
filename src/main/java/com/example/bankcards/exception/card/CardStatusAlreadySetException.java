package com.example.bankcards.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CardStatusAlreadySetException extends InvalidCardOperationException {
    public CardStatusAlreadySetException(String message) {
        super(message);
    }

    public CardStatusAlreadySetException(UUID id) {
        super(String.format("Card status already set for id: %s.", id));
    }
}
