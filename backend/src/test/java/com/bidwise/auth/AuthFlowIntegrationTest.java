package com.bidwise.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidwise.auth.dto.AuthResponse;
import com.bidwise.auth.dto.LoginRequest;
import com.bidwise.auth.dto.RegisterRequest;
import com.bidwise.auth.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises the full auth chain over HTTP (security filter + JWT + JPA on H2):
 * register, reject unauthenticated /me, accept authenticated /me, and login.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void registerThenAccessProtectedEndpointThenLogin() {
        var register = new RegisterRequest("Bob", "bob@example.com", "password123");

        ResponseEntity<AuthResponse> registerResponse =
                rest.postForEntity("/api/auth/register", register, AuthResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        String token = registerResponse.getBody().token();
        assertThat(token).isNotBlank();

        // /me without a token is rejected.
        ResponseEntity<String> unauthorized =
                rest.getForEntity("/api/me", String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // /me with the bearer token returns the current user.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<UserResponse> me = rest.exchange(
                "/api/me", HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().email()).isEqualTo("bob@example.com");
        assertThat(me.getBody().role()).isEqualTo("USER");

        // Login with the same credentials succeeds.
        ResponseEntity<AuthResponse> login = rest.postForEntity(
                "/api/auth/login", new LoginRequest("bob@example.com", "password123"),
                AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        assertThat(login.getBody().token()).isNotBlank();
    }

    @Test
    void duplicateRegistrationIsRejected() {
        var register = new RegisterRequest("Carol", "carol@example.com", "password123");
        rest.postForEntity("/api/auth/register", register, AuthResponse.class);

        ResponseEntity<String> second =
                rest.postForEntity("/api/auth/register", register, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
