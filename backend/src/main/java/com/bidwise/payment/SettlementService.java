package com.bidwise.payment;

import com.bidwise.bid.Bid;
import com.bidwise.bid.BidRepository;
import com.bidwise.listing.Listing;
import com.bidwise.user.User;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settles a closed auction: captures the winner's deposit hold, releases every other
 * bidder's hold, and marks the listing {@code SOLD}. Triggered from the close sweep
 * once an auction is {@code ENDED}.
 *
 * <p>Idempotent by the unique settlement-per-listing constraint: a re-run (e.g. a
 * redelivered close) sees an existing {@link Settlement} and does nothing. A capture
 * failure is recorded as {@link SettlementStatus#FAILED} and leaves the listing
 * {@code ENDED} so it can be retried, rather than charging losers or selling.
 */
@Service
public class SettlementService {

    private static final Logger LOG = LoggerFactory.getLogger(SettlementService.class);

    private final BidRepository bidRepository;
    private final PaymentHoldRepository holdRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentGateway gateway;

    public SettlementService(
            BidRepository bidRepository,
            PaymentHoldRepository holdRepository,
            SettlementRepository settlementRepository,
            PaymentGateway gateway) {
        this.bidRepository = bidRepository;
        this.holdRepository = holdRepository;
        this.settlementRepository = settlementRepository;
        this.gateway = gateway;
    }

    /**
     * Settles the given ended auction. No-op when the auction has no winner or was
     * already settled. Returns {@code true} when this call captured the winner.
     */
    @Transactional
    public boolean settle(Listing listing) {
        User winner = listing.getCurrentBidder();
        if (winner == null) {
            return false; // no bids — nothing to settle
        }
        if (settlementRepository.existsByListingId(listing.getId())) {
            return false; // already settled (idempotent)
        }

        BigDecimal finalAmount = listing.getCurrentPrice();
        Bid winningBid = bidRepository
                .findFirstByListingIdOrderByAmountDesc(listing.getId())
                .orElse(null);

        PaymentHold winnerHold = holdRepository
                .findByUserIdAndListingId(winner.getId(), listing.getId())
                .orElse(null);
        try {
            if (winnerHold != null) {
                gateway.capture(winnerHold.getStripePaymentIntentId());
                winnerHold.capture();
            }
        } catch (PaymentGatewayException e) {
            LOG.warn("Capture failed for listing {}: {}", listing.getId(), e.getMessage());
            settlementRepository.save(
                    new Settlement(listing, winningBid, finalAmount, SettlementStatus.FAILED));
            return false;
        }

        releaseLosers(listing.getId(), winner.getId());
        settlementRepository.save(
                new Settlement(listing, winningBid, finalAmount, SettlementStatus.CAPTURED));
        listing.markSold();
        return true;
    }

    /** Voids every authorized hold on the listing except the winner's. */
    private void releaseLosers(Long listingId, Long winnerId) {
        List<PaymentHold> holds =
                holdRepository.findByListingIdAndStatus(listingId, PaymentHoldStatus.AUTHORIZED);
        for (PaymentHold hold : holds) {
            if (hold.getUser().getId().equals(winnerId)) {
                continue;
            }
            gateway.release(hold.getStripePaymentIntentId());
            hold.voidHold();
        }
    }
}
