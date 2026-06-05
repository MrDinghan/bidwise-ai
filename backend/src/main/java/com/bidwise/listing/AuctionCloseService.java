package com.bidwise.listing;

import com.bidwise.bid.AtomicBidPrice;
import com.bidwise.bid.BidRepository;
import com.bidwise.realtime.BidEvent;
import com.bidwise.realtime.ListingBroadcaster;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes auctions whose time is up: an auction with bids becomes {@code ENDED} (a
 * winner is recorded, awaiting settlement in P3); one with no bids becomes
 * {@code CLOSED}. Each closed auction's Redis price key is dropped and a close event
 * is broadcast. Invoked on a schedule (see {@code AuctionCloseScheduler}) and directly
 * in tests.
 */
@Service
public class AuctionCloseService {

    private final ListingRepository listingRepository;
    private final BidRepository bidRepository;
    private final AtomicBidPrice atomicBidPrice;
    private final ListingBroadcaster broadcaster;

    public AuctionCloseService(
            ListingRepository listingRepository,
            BidRepository bidRepository,
            AtomicBidPrice atomicBidPrice,
            ListingBroadcaster broadcaster) {
        this.listingRepository = listingRepository;
        this.bidRepository = bidRepository;
        this.atomicBidPrice = atomicBidPrice;
        this.broadcaster = broadcaster;
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
            broadcaster.broadcast(BidEvent.of(BidEvent.Type.CLOSED, listing, bidCount));
        }
        return due.size();
    }
}
