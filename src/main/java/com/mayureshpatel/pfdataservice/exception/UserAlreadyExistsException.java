package com.mayureshpatel.pfdataservice.exception;

/** Thrown during registration when the requested username or email is already taken. */
public class UserAlreadyExistsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** @param message a description of which field (username or email) already exists */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
