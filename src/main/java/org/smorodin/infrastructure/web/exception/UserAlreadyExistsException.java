package org.smorodin.infrastructure.web.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String username) {
        super("User with this name already exists: " + username);
    }
}