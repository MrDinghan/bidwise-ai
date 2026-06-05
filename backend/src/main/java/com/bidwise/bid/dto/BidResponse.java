package com.bidwise.bid.dto;

import com.bidwise.bid.Bid;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single bid in a listing's history.
 */
public record BidResponse(
        Long id,
        Long listingId,
        Long bidderId,
        String bidderName,
        BigDecimal amount,
        Instant createdAt) {

    /** Maps a persisted bid to its API representation (call within a transaction). */
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getListing().getId(),
                bid.getBidder().getId(),
                bid.getBidder().getName(),
                bid.getAmount(),
                bid.getCreatedAt());
    }
}
