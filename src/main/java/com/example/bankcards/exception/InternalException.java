package com.example.bankcards.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternalException extends RuntimeException {
    public InternalException(String message) {
        super(message);
        log.error(message);
    }
}
