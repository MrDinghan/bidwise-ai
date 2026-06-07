package com.bidwise.bid;

import com.bidwise.bid.dto.BidPageResponse;
import com.bidwise.bid.dto.BidResponse;
import com.bidwise.listing.Listing;
import com.bidwise.listing.ListingAccessDeniedException;
import com.bidwise.listing.ListingNotFoundException;
import com.bidwise.listing.ListingRepository;
import com.bidwise.listing.ListingStatus;
import com.bidwise.payment.DepositRequiredException;
import com.bidwise.payment.PaymentHoldRepository;
import com.bidwise.payment.PaymentHoldStatus;
import com.bidwise.realtime.BidEvent;
import com.bidwise.realtime.ListingBroadcaster;
import com.bidwise.user.User;
import com.bidwise.user.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Placing and reading bids. The hot path is: validate against Postgres, raise the
 * price atomically in Redis (race-free across instances), then mirror the accepted
 * bid back into Postgres (durable history + denormalized current state) and broadcast
 * it to subscribers. Late bids extend the auction (anti-snipe).
 */
@Service
public class BidService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ListingRepository listingRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final PaymentHoldRepository holdRepository;
    private final AtomicBidPrice atomicBidPrice;
    private final ListingBroadcaster broadcaster;
    private final Duration antiSnipeWindow;
    private final Duration antiSnipeExtension;

    public BidService(
            ListingRepository listingRepository,
            BidRepository bidRepository,
            UserRepository userRepository,
            PaymentHoldRepository holdRepository,
            AtomicBidPrice atomicBidPrice,
            ListingBroadcaster broadcaster,
            @Value("${auction.anti-snipe.window-seconds:60}") long antiSnipeWindowSeconds,
            @Value("${auction.anti-snipe.extension-seconds:120}") long antiSnipeExtensionSeconds) {
        this.listingRepository = listingRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.holdRepository = holdRepository;
        this.atomicBidPrice = atomicBidPrice;
        this.broadcaster = broadcaster;
        this.antiSnipeWindow = Duration.ofSeconds(antiSnipeWindowSeconds);
        this.antiSnipeExtension = Duration.ofSeconds(antiSnipeExtensionSeconds);
    }

    /**
     * Places a bid on an active auction. Throws {@link InvalidBidException} (400) for
     * the seller bidding on their own lot, {@link AuctionNotActiveException} (409) when
     * the auction is not open, and {@link BidTooLowException} (409) when the amount no
     * longer clears the bar.
     */
    @Transactional
    public BidResponse placeBid(Long listingId, String bidderEmail, BigDecimal amount) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        User bidder = userRepository.findByEmail(bidderEmail)
                .orElseThrow(ListingAccessDeniedException::new);

        Instant now = Instant.now();
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new AuctionNotActiveException("This auction is not open for bidding");
        }
        if (listing.getEndAt() == null || !listing.getEndAt().isAfter(now)) {
            throw new AuctionNotActiveException("This auction has ended");
        }
        if (listing.isOwnedBy(bidder.getId())) {
            throw new InvalidBidException("You cannot bid on your own listing");
        }
        // A buyer must hold an authorized deposit before bidding (pre-auth gate, P3).
        if (!holdRepository.existsByUserIdAndListingIdAndStatus(
                bidder.getId(), listingId, PaymentHoldStatus.AUTHORIZED)) {
            throw new DepositRequiredException();
        }

        BigDecimal increment = listing.getBidIncrement();
        AtomicBidPrice.Result result = raisePrice(listing, amount, increment, now);
        if (!result.accepted()) {
            throw new BidTooLowException(result.currentPrice().add(increment));
        }

        // Accepted: persist history, then mirror current state onto the listing.
        Bid bid = bidRepository.save(new Bid(listing, bidder, amount));
        BidResponse response = BidResponse.from(bid); // build while entities are managed
        listingRepository.recordHigherBid(listingId, amount, bidder, now);
        applyAntiSnipe(listingId, listing.getEndAt(), now);

        broadcastState(listingId, BidEvent.Type.BID);
        return response;
    }

    /** Public bid history for a listing, newest (highest) first. */
    @Transactional(readOnly = true)
    public BidPageResponse history(Long listingId, int page, int size) {
        if (!listingRepository.existsById(listingId)) {
            throw new ListingNotFoundException(listingId);
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        return BidPageResponse.from(
                bidRepository.findByListingIdOrderByAmountDesc(listingId, pageable));
    }

    private AtomicBidPrice.Result raisePrice(
            Listing listing, BigDecimal amount, BigDecimal increment, Instant now) {
        Duration ttl = ttlUntil(listing.getEndAt(), now);
        atomicBidPrice.seed(listing.getId(), seedFloor(listing, increment), ttl);
        AtomicBidPrice.Result result = atomicBidPrice.tryRaise(listing.getId(), amount, increment);
        if (!result.initialized()) {
            // Key vanished between seed and eval (e.g. eviction) — reseed once and retry.
            atomicBidPrice.seed(listing.getId(), seedFloor(listing, increment), ttl);
            result = atomicBidPrice.tryRaise(listing.getId(), amount, increment);
            if (!result.initialized()) {
                throw new AuctionNotActiveException("This auction is not accepting bids right now");
            }
        }
        return result;
    }

    /** Floor a new bid must exceed by one increment: start-minus-increment, or current price. */
    private static BigDecimal seedFloor(Listing listing, BigDecimal increment) {
        return listing.getBidCount() == 0
                ? listing.getStartPrice().subtract(increment)
                : listing.getCurrentPrice();
    }

    private void applyAntiSnipe(Long listingId, Instant currentEnd, Instant now) {
        if (currentEnd == null || Duration.between(now, currentEnd).compareTo(antiSnipeWindow) > 0) {
            return;
        }
        Instant newEnd = now.plus(antiSnipeExtension);
        if (listingRepository.extendEndAt(listingId, newEnd) > 0) {
            atomicBidPrice.touchTtl(listingId, ttlUntil(newEnd, now));
        }
    }

    private void broadcastState(Long listingId, BidEvent.Type type) {
        long bidCount = bidRepository.countByListingId(listingId);
        Listing fresh = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        broadcaster.broadcast(BidEvent.of(type, fresh, bidCount));
    }

    private static Duration ttlUntil(Instant end, Instant now) {
        Duration until = Duration.between(now, end);
        return (until.isNegative() ? Duration.ZERO : until).plusHours(1);
    }

    private static int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
