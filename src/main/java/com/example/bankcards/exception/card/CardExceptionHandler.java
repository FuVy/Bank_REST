package com.example.bankcards.exception.card;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CardExceptionHandler {
    @ExceptionHandler(InvalidCardOperationException.class)
    public ResponseEntity<?> handleInvalidCardOperationException(InvalidCardOperationException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CardNotOwnedByUserException.class)
    public ResponseEntity<?> handleCardNotOwnedByUserException(CardNotOwnedByUserException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CardStatusAlreadySetException.class)
    public ResponseEntity<?> handleCardStatusAlreadySet(CardStatusAlreadySetException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    }
}
