package com.bidwise.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidwise.auth.dto.AuthResponse;
import com.bidwise.auth.dto.RegisterRequest;
import com.bidwise.listing.dto.CreateListingRequest;
import com.bidwise.listing.dto.ListingPageResponse;
import com.bidwise.listing.dto.ListingResponse;
import com.bidwise.listing.dto.UpdateListingRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
 * Exercises the listing lifecycle over HTTP (security + JPA on H2): create a draft,
 * confirm it is not yet searchable, publish it, then find it via public search;
 * plus the auth/ownership guards (anonymous create -> 401, non-owner edit -> 403).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ListingFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String registerAndGetToken(String name, String email) {
        ResponseEntity<AuthResponse> response = rest.postForEntity(
                "/api/auth/register",
                new RegisterRequest(name, email, "password123"),
                AuthResponse.class);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().token();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private CreateListingRequest cameraRequest() {
        return new CreateListingRequest(
                "Vintage camera",
                Category.ELECTRONICS,
                "Retro film camera in great shape",
                ItemCondition.LIKE_NEW,
                List.of("https://img.example/cam.jpg"),
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                "Downtown pickup",
                Instant.now().plus(3, ChronoUnit.DAYS));
    }

    @Test
    void draftBecomesSearchableOnlyAfterPublish() {
        String token = registerAndGetToken("Seller", "seller@example.com");

        ResponseEntity<ListingResponse> created = rest.exchange(
                "/api/listings", HttpMethod.POST,
                new HttpEntity<>(cameraRequest(), bearer(token)), ListingResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().status()).isEqualTo(ListingStatus.DRAFT);
        Long id = created.getBody().id();

        // Drafts are not returned by the public search.
        ResponseEntity<ListingPageResponse> beforePublish =
                rest.getForEntity("/api/listings?q=camera", ListingPageResponse.class);
        assertThat(beforePublish.getBody()).isNotNull();
        assertThat(beforePublish.getBody().totalElements()).isZero();

        // Publish, then it is searchable without a token, with filters + sort.
        ResponseEntity<ListingResponse> published = rest.exchange(
                "/api/listings/" + id + "/publish", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), ListingResponse.class);
        assertThat(published.getBody()).isNotNull();
        assertThat(published.getBody().status()).isEqualTo(ListingStatus.ACTIVE);

        ResponseEntity<ListingPageResponse> search = rest.getForEntity(
                "/api/listings?q=camera&category=ELECTRONICS&sort=ENDING_SOON",
                ListingPageResponse.class);
        assertThat(search.getBody()).isNotNull();
        assertThat(search.getBody().totalElements()).isEqualTo(1);
        assertThat(search.getBody().content().get(0).title()).isEqualTo("Vintage camera");
    }

    @Test
    void anonymousCannotCreateAListing() {
        ResponseEntity<String> response =
                rest.postForEntity("/api/listings", cameraRequest(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void nonOwnerCannotEditAListing() {
        String ownerToken = registerAndGetToken("Owner", "owner@example.com");
        ResponseEntity<ListingResponse> created = rest.exchange(
                "/api/listings", HttpMethod.POST,
                new HttpEntity<>(cameraRequest(), bearer(ownerToken)), ListingResponse.class);
        assertThat(created.getBody()).isNotNull();
        Long id = created.getBody().id();

        String intruderToken = registerAndGetToken("Intruder", "intruder@example.com");
        var update = new UpdateListingRequest(
                "Hijacked title", Category.OTHER, "nope", ItemCondition.POOR, List.of(),
                new BigDecimal("1.00"), new BigDecimal("1.00"), null, null);

        ResponseEntity<String> response = rest.exchange(
                "/api/listings/" + id, HttpMethod.PUT,
                new HttpEntity<>(update, bearer(intruderToken)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
