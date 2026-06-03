package com.bidwise.auth;

/**
 * Thrown when registering with an email that already exists.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email already registered: " + email);
    }
}
