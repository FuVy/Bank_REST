package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCardOperationException extends RuntimeException {
    public InvalidCardOperationException(String message) {
        super(message);
    }
}
