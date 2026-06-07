package com.bidwise.listing;

import com.bidwise.bid.AtomicBidPrice;
import com.bidwise.bid.BidRepository;
import com.bidwise.payment.SettlementService;
import com.bidwise.realtime.BidEvent;
import com.bidwise.realtime.ListingBroadcaster;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes auctions whose time is up: an auction with bids becomes {@code ENDED} and is
 * settled (winner captured, losers released, listing {@code SOLD}); one with no bids
 * becomes {@code CLOSED}. Each closed auction's Redis price key is dropped and a
 * close/sold event is broadcast. Invoked on a schedule (see {@code AuctionCloseScheduler})
 * and directly in tests.
 */
@Service
public class AuctionCloseService {

    private final ListingRepository listingRepository;
    private final BidRepository bidRepository;
    private final AtomicBidPrice atomicBidPrice;
    private final ListingBroadcaster broadcaster;
    private final SettlementService settlementService;

    public AuctionCloseService(
            ListingRepository listingRepository,
            BidRepository bidRepository,
            AtomicBidPrice atomicBidPrice,
            ListingBroadcaster broadcaster,
            SettlementService settlementService) {
        this.listingRepository = listingRepository;
        this.bidRepository = bidRepository;
        this.atomicBidPrice = atomicBidPrice;
        this.broadcaster = broadcaster;
        this.settlementService = settlementService;
    }

    /**
     * Ends every active auction past its end time. Returns how many were closed.
     * Runs in one transaction; a rare optimistic-lock clash with an in-flight bid
     * simply defers that batch to the next run.
     */
    @Transactional
    public int closeDueAuctions() {
        List<Listing> due = listingRepository
                .findByStatusAndEndAtLessThanEqual(ListingStatus.ACTIVE, Instant.now());
        for (Listing listing : due) {
            listing.endAuction();
            atomicBidPrice.clear(listing.getId());
            long bidCount = bidRepository.countByListingId(listing.getId());
            // Settle immediately: capture the winner, release losers, mark SOLD. A
            // capture failure is recorded inside settle() and leaves the listing ENDED.
            boolean sold = settlementService.settle(listing);
            BidEvent.Type type = sold ? BidEvent.Type.SOLD : BidEvent.Type.CLOSED;
            broadcaster.broadcast(BidEvent.of(type, listing, bidCount));
        }
        return due.size();
    }
}
