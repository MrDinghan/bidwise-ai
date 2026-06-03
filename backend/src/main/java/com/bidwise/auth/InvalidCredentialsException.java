package com.bidwise.auth;

/**
 * Thrown when login credentials do not match.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
