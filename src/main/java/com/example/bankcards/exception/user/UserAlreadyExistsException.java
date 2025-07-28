package com.example.bankcards.exception.user;

import com.example.bankcards.exception.ResourceAlreadyExistsException;

public class UserAlreadyExistsException extends ResourceAlreadyExistsException {
    public UserAlreadyExistsException(String username) {
        super(String.format("User with username \"%s\" already exists.", username));
    }
}
