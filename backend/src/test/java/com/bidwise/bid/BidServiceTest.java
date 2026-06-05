package com.bidwise.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidwise.bid.dto.BidResponse;
import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import com.bidwise.listing.Listing;
import com.bidwise.listing.ListingRepository;
import com.bidwise.realtime.BidEvent;
import com.bidwise.realtime.ListingBroadcaster;
import com.bidwise.user.Role;
import com.bidwise.user.User;
import com.bidwise.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    private static final String SELLER = "seller@example.com";
    private static final String BIDDER = "bidder@example.com";

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AtomicBidPrice atomicBidPrice;
    @Mock
    private ListingBroadcaster broadcaster;

    private BidService service;

    private User seller;
    private User bidder;

    @BeforeEach
    void setUp() {
        service = new BidService(
                listingRepository, bidRepository, userRepository, atomicBidPrice, broadcaster, 60, 120);
        seller = userWithId("Seller", SELLER, 1L);
        bidder = userWithId("Bidder", BIDDER, 2L);
    }

    private User userWithId(String name, String email, long id) {
        User user = new User(name, email, "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** A published (ACTIVE) listing owned by {@link #seller}, ending {@code endAt}. */
    private Listing activeListing(Instant endAt) {
        Listing listing = new Listing(
                seller, "Desk lamp", Category.FURNITURE, "A lamp", ItemCondition.GOOD,
                List.of(), new BigDecimal("10.00"), new BigDecimal("1.00"), "Library",
                Instant.now().plus(2, ChronoUnit.DAYS));
        listing.publish();
        ReflectionTestUtils.setField(listing, "id", 100L);
        ReflectionTestUtils.setField(listing, "endAt", endAt);
        return listing;
    }

    @Test
    void sellerCannotBidOnOwnListing() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(activeListing(future())));
        when(userRepository.findByEmail(SELLER)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> service.placeBid(100L, SELLER, new BigDecimal("11.00")))
                .isInstanceOf(InvalidBidException.class);
        verify(bidRepository, never()).save(any());
    }

    @Test
    void biddingOnEndedAuctionIsRejected() {
        Listing ended = activeListing(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(listingRepository.findById(100L)).thenReturn(Optional.of(ended));
        when(userRepository.findByEmail(BIDDER)).thenReturn(Optional.of(bidder));

        assertThatThrownBy(() -> service.placeBid(100L, BIDDER, new BigDecimal("11.00")))
                .isInstanceOf(AuctionNotActiveException.class);
        verify(bidRepository, never()).save(any());
    }

    @Test
    void tooLowBidIsRejectedWithTheCurrentBar() {
        when(listingRepository.findById(100L)).thenReturn(Optional.of(activeListing(future())));
        when(userRepository.findByEmail(BIDDER)).thenReturn(Optional.of(bidder));
        when(atomicBidPrice.tryRaise(eq(100L), any(), any()))
                .thenReturn(new AtomicBidPrice.Result(false, true, new BigDecimal("12.00")));

        assertThatThrownBy(() -> service.placeBid(100L, BIDDER, new BigDecimal("11.00")))
                .isInstanceOf(BidTooLowException.class)
                .satisfies(ex -> assertThat(((BidTooLowException) ex).getMinimumBid())
                        .isEqualByComparingTo("13.00")); // 12.00 + 1.00 increment
        verify(bidRepository, never()).save(any());
    }

    @Test
    void acceptedBidPersistsAndBroadcasts() {
        Listing listing = activeListing(future());
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(userRepository.findByEmail(BIDDER)).thenReturn(Optional.of(bidder));
        when(atomicBidPrice.tryRaise(eq(100L), any(), any()))
                .thenReturn(new AtomicBidPrice.Result(true, true, new BigDecimal("11.00")));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> {
            Bid bid = inv.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 7L);
            return bid;
        });

        BidResponse response = service.placeBid(100L, BIDDER, new BigDecimal("11.00"));

        assertThat(response.amount()).isEqualByComparingTo("11.00");
        assertThat(response.bidderId()).isEqualTo(2L);
        verify(listingRepository).recordHigherBid(eq(100L), eq(new BigDecimal("11.00")), eq(bidder), any());
        verify(broadcaster).broadcast(any(BidEvent.class));
    }

    @Test
    void lateBidExtendsTheAuction() {
        Listing listing = activeListing(Instant.now().plus(20, ChronoUnit.SECONDS)); // within 60s window
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(userRepository.findByEmail(BIDDER)).thenReturn(Optional.of(bidder));
        when(atomicBidPrice.tryRaise(eq(100L), any(), any()))
                .thenReturn(new AtomicBidPrice.Result(true, true, new BigDecimal("11.00")));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepository.extendEndAt(eq(100L), any())).thenReturn(1);

        service.placeBid(100L, BIDDER, new BigDecimal("11.00"));

        verify(listingRepository).extendEndAt(eq(100L), any());
        verify(atomicBidPrice).touchTtl(eq(100L), any());
    }

    @Test
    void freshAuctionWithDistantEndIsNotExtended() {
        Listing listing = activeListing(future());
        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(userRepository.findByEmail(BIDDER)).thenReturn(Optional.of(bidder));
        when(atomicBidPrice.tryRaise(eq(100L), any(), any()))
                .thenReturn(new AtomicBidPrice.Result(true, true, new BigDecimal("11.00")));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));

        service.placeBid(100L, BIDDER, new BigDecimal("11.00"));

        verify(listingRepository, never()).extendEndAt(anyLong(), any());
    }

    private static Instant future() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }
}
