package com.bidwise.realtime;

import com.bidwise.listing.Listing;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Real-time event broadcast to subscribers of a listing's STOMP topic. Sent when a
 * bid is accepted ({@code type=BID}), when an auction closes ({@code type=CLOSED}), and
 * when a closed auction is settled and sold ({@code type=SOLD}).
 *
 * <p>This is a WebSocket message payload, not a REST resource, so it is not part of
 * the OpenAPI contract — the frontend mirrors it with a hand-written type.
 */
public record BidEvent(
        Type type,
        Long listingId,
        BigDecimal currentPrice,
        long bidCount,
        Long highestBidderId,
        String highestBidderName,
        Instant endAt,
        String status,
        Instant at) {

    /** Kind of update being broadcast. */
    public enum Type {
        BID,
        CLOSED,
        SOLD
    }

    /** Snapshot event after a listing's bidding/lifecycle state changed. */
    public static BidEvent of(Type type, Listing listing, long bidCount) {
        var bidder = listing.getCurrentBidder();
        return new BidEvent(
                type,
                listing.getId(),
                listing.getCurrentPrice(),
                bidCount,
                bidder == null ? null : bidder.getId(),
                bidder == null ? null : bidder.getName(),
                listing.getEndAt(),
                listing.getStatus().name(),
                Instant.now());
    }
}
