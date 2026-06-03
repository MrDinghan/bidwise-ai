package com.bidwise.auth.dto;

/**
 * Returned after a successful register/login: a bearer token plus the user.
 */
public record AuthResponse(String token, UserResponse user) {
}
