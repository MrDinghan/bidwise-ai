package com.bidwise.bid;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidwise.auth.dto.AuthResponse;
import com.bidwise.auth.dto.RegisterRequest;
import com.bidwise.bid.dto.BidPageResponse;
import com.bidwise.bid.dto.BidResponse;
import com.bidwise.bid.dto.PlaceBidRequest;
import com.bidwise.listing.AuctionCloseService;
import com.bidwise.listing.AuctionDuration;
import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import com.bidwise.listing.Listing;
import com.bidwise.listing.ListingRepository;
import com.bidwise.listing.ListingStatus;
import com.bidwise.listing.dto.CreateListingRequest;
import com.bidwise.listing.dto.ListingResponse;
import com.bidwise.payment.PaymentHoldRepository;
import com.bidwise.payment.PaymentHoldStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end bidding over HTTP against a real Redis (Testcontainers), exercising the
 * atomic price path that H2 alone cannot: opening bid, too-low rejection, outbidding,
 * ownership/auth guards, public history, and scheduled-style auto-close with a winner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class BiddingFlowIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // Own in-memory DB so published lots don't leak into other suites' searches.
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:bidding;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private AuctionCloseService auctionCloseService;
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private PaymentHoldRepository holdRepository;

    private AuthResponse register(String name, String email) {
        ResponseEntity<AuthResponse> response = rest.postForEntity(
                "/api/auth/register",
                new RegisterRequest(name, email, "password123"),
                AuthResponse.class);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private CreateListingRequest listingRequest() {
        return new CreateListingRequest(
                "Vintage camera", Category.ELECTRONICS, "Retro film camera",
                ItemCondition.LIKE_NEW, List.of("https://img.example/cam.jpg"),
                new BigDecimal("10.00"), new BigDecimal("1.00"), "Downtown",
                AuctionDuration.THREE_DAYS);
    }

    private Long createAndPublish(String token) {
        ResponseEntity<ListingResponse> created = rest.exchange(
                "/api/listings", HttpMethod.POST,
                new HttpEntity<>(listingRequest(), bearer(token)), ListingResponse.class);
        Long id = created.getBody().id();
        rest.exchange("/api/listings/" + id + "/publish", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), ListingResponse.class);
        return id;
    }

    /** Authorizes the bidding deposit (required before placing a bid). */
    private ResponseEntity<String> placeDeposit(Long listingId, String token) {
        return rest.exchange(
                "/api/listings/" + listingId + "/deposit", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), String.class);
    }

    private ResponseEntity<BidResponse> bid(Long listingId, String token, String amount) {
        return rest.exchange(
                "/api/listings/" + listingId + "/bids", HttpMethod.POST,
                new HttpEntity<>(new PlaceBidRequest(new BigDecimal(amount)), bearer(token)),
                BidResponse.class);
    }

    private ListingResponse getListing(Long id) {
        return rest.getForEntity("/api/listings/" + id, ListingResponse.class).getBody();
    }

    @Test
    void biddersRaiseThePriceAndGuardsHold() {
        String seller = register("Seller", "seller@bid.com").token();
        AuthResponse alice = register("Alice", "alice@bid.com");
        AuthResponse bob = register("Bob", "bob@bid.com");
        Long id = createAndPublish(seller);

        // A bid without an authorized deposit is rejected (402 Payment Required).
        assertThat(bid(id, alice.token(), "10.00").getStatusCode())
                .isEqualTo(HttpStatus.PAYMENT_REQUIRED);

        // Buyers authorize a deposit before bidding.
        assertThat(placeDeposit(id, alice.token()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        placeDeposit(id, bob.token());

        // Opening bid clears at the start price.
        ResponseEntity<BidResponse> opening = bid(id, alice.token(), "10.00");
        assertThat(opening.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(opening.getBody().amount()).isEqualByComparingTo("10.00");

        ListingResponse afterOpening = getListing(id);
        assertThat(afterOpening.currentPrice()).isEqualByComparingTo("10.00");
        assertThat(afterOpening.currentBidderId()).isEqualTo(alice.user().id());
        assertThat(afterOpening.bidCount()).isEqualTo(1);

        // Same amount no longer clears the bar (min is now 11.00).
        assertThat(bid(id, alice.token(), "10.00").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Bob outbids.
        assertThat(bid(id, bob.token(), "11.00").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ListingResponse afterOutbid = getListing(id);
        assertThat(afterOutbid.currentPrice()).isEqualByComparingTo("11.00");
        assertThat(afterOutbid.currentBidderId()).isEqualTo(bob.user().id());
        assertThat(afterOutbid.bidCount()).isEqualTo(2);

        // Seller cannot bid on their own lot.
        assertThat(bid(id, seller, "20.00").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Anonymous cannot bid.
        ResponseEntity<String> anon = rest.postForEntity(
                "/api/listings/" + id + "/bids",
                new PlaceBidRequest(new BigDecimal("20.00")), String.class);
        assertThat(anon.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Public history, newest/highest first.
        BidPageResponse history = rest.getForEntity(
                "/api/listings/" + id + "/bids", BidPageResponse.class).getBody();
        assertThat(history.totalElements()).isEqualTo(2);
        assertThat(history.content().get(0).amount()).isEqualByComparingTo("11.00");
        assertThat(history.content().get(1).amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void auctionWithBidsClosesAndSettlesAsSold() {
        String seller = register("Seller2", "seller2@bid.com").token();
        AuthResponse alice = register("Alice2", "alice2@bid.com");
        Long id = createAndPublish(seller);
        placeDeposit(id, alice.token());
        bid(id, alice.token(), "10.00");

        expireNow(id);
        auctionCloseService.closeDueAuctions();

        // Close → settle: the winner's deposit is captured and the lot is SOLD.
        ListingResponse closed = getListing(id);
        assertThat(closed.status()).isEqualTo(ListingStatus.SOLD);
        assertThat(closed.currentBidderId()).isEqualTo(alice.user().id());
        assertThat(holdRepository
                .findByUserIdAndListingId(alice.user().id(), id)
                .orElseThrow()
                .getStatus()).isEqualTo(PaymentHoldStatus.CAPTURED);
    }

    @Test
    void auctionWithoutBidsClosesAsClosed() {
        String seller = register("Seller3", "seller3@bid.com").token();
        Long id = createAndPublish(seller);

        expireNow(id);
        auctionCloseService.closeDueAuctions();

        ListingResponse closed = getListing(id);
        assertThat(closed.status()).isEqualTo(ListingStatus.CLOSED);
        assertThat(closed.currentBidderId()).isNull();
    }

    /** Pushes a listing's end time into the past so the close sweep picks it up. */
    private void expireNow(Long id) {
        Listing listing = listingRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(listing, "endAt", Instant.now().minusSeconds(5));
        listingRepository.save(listing);
    }
}
