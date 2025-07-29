package com.example.bankcards.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.CONFLICT)
public class CardStatusAlreadySetException extends RuntimeException {
    public CardStatusAlreadySetException(UUID id) {
        super(String.format("Card status already set for id: %s.", id));
    }
}
