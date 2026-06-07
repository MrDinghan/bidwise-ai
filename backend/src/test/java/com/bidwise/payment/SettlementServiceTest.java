package com.bidwise.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidwise.bid.BidRepository;
import com.bidwise.listing.AuctionDuration;
import com.bidwise.listing.Category;
import com.bidwise.listing.ItemCondition;
import com.bidwise.listing.Listing;
import com.bidwise.listing.ListingStatus;
import com.bidwise.user.Role;
import com.bidwise.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private BidRepository bidRepository;
    @Mock
    private PaymentHoldRepository holdRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private PaymentGateway gateway;

    private SettlementService service;

    private User seller;
    private User winner;
    private User loser;

    @BeforeEach
    void setUp() {
        service = new SettlementService(bidRepository, holdRepository, settlementRepository, gateway);
        seller = userWithId("Seller", "seller@x.com", 1L);
        winner = userWithId("Winner", "winner@x.com", 2L);
        loser = userWithId("Loser", "loser@x.com", 3L);
    }

    private User userWithId(String name, String email, long id) {
        User user = new User(name, email, "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** An ENDED listing whose highest bidder is {@code currentBidder}. */
    private Listing endedListing(User currentBidder) {
        Listing listing = new Listing(
                seller, "Lamp", Category.FURNITURE, "A lamp", ItemCondition.GOOD,
                List.of(), new BigDecimal("10.00"), new BigDecimal("1.00"), "Library",
                AuctionDuration.THREE_DAYS);
        listing.publish();
        ReflectionTestUtils.setField(listing, "id", 100L);
        ReflectionTestUtils.setField(listing, "currentBidder", currentBidder);
        ReflectionTestUtils.setField(listing, "currentPrice", new BigDecimal("11.00"));
        ReflectionTestUtils.setField(listing, "status", ListingStatus.ENDED);
        return listing;
    }

    private PaymentHold hold(User user, Listing listing, String intentId) {
        return new PaymentHold(user, listing, intentId, new BigDecimal("1.00"), "hold:" + intentId);
    }

    @Test
    void capturesWinnerReleasesLosersAndMarksSold() {
        Listing listing = endedListing(winner);
        PaymentHold winnerHold = hold(winner, listing, "pi_winner");
        PaymentHold loserHold = hold(loser, listing, "pi_loser");

        when(settlementRepository.existsByListingId(100L)).thenReturn(false);
        when(bidRepository.findFirstByListingIdOrderByAmountDesc(100L)).thenReturn(Optional.empty());
        when(holdRepository.findByUserIdAndListingId(2L, 100L)).thenReturn(Optional.of(winnerHold));
        when(holdRepository.findByListingIdAndStatus(100L, PaymentHoldStatus.AUTHORIZED))
                .thenReturn(List.of(winnerHold, loserHold));

        boolean settled = service.settle(listing);

        assertThat(settled).isTrue();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.SOLD);
        assertThat(winnerHold.getStatus()).isEqualTo(PaymentHoldStatus.CAPTURED);
        assertThat(loserHold.getStatus()).isEqualTo(PaymentHoldStatus.VOIDED);
        verify(gateway).capture("pi_winner");
        verify(gateway).release("pi_loser");
        verify(gateway, never()).release("pi_winner");

        ArgumentCaptor<Settlement> saved = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SettlementStatus.CAPTURED);
        assertThat(saved.getValue().getFinalAmount()).isEqualByComparingTo("11.00");
    }

    @Test
    void noWinnerIsNotSettled() {
        Listing listing = endedListing(null);

        boolean settled = service.settle(listing);

        assertThat(settled).isFalse();
        verify(settlementRepository, never()).save(any());
        verify(gateway, never()).capture(any());
    }

    @Test
    void alreadySettledListingIsSkipped() {
        Listing listing = endedListing(winner);
        when(settlementRepository.existsByListingId(100L)).thenReturn(true);

        boolean settled = service.settle(listing);

        assertThat(settled).isFalse();
        verify(gateway, never()).capture(any());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void captureFailureRecordsFailedSettlementAndLeavesListingEnded() {
        Listing listing = endedListing(winner);
        PaymentHold winnerHold = hold(winner, listing, "pi_winner");
        when(settlementRepository.existsByListingId(100L)).thenReturn(false);
        when(bidRepository.findFirstByListingIdOrderByAmountDesc(100L)).thenReturn(Optional.empty());
        when(holdRepository.findByUserIdAndListingId(2L, 100L)).thenReturn(Optional.of(winnerHold));
        doThrow(new PaymentGatewayException("card declined")).when(gateway).capture("pi_winner");

        boolean settled = service.settle(listing);

        assertThat(settled).isFalse();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ENDED);
        verify(gateway, never()).release(any());

        ArgumentCaptor<Settlement> saved = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SettlementStatus.FAILED);
    }
}
