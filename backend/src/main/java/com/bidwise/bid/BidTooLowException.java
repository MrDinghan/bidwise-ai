package com.bidwise.bid;

import java.math.BigDecimal;

/**
 * Raised when a bid does not clear the current bar (start price for the first bid,
 * or current price + increment thereafter) — typically because another bid raised
 * the price first. Maps to HTTP 409 so the client knows to refresh and retry; the
 * minimum acceptable bid is carried in {@link #getMinimumBid()}.
 */
public class BidTooLowException extends RuntimeException {

    private final transient BigDecimal minimumBid;

    public BidTooLowException(BigDecimal minimumBid) {
        super("Bid is too low; the minimum acceptable bid is now " + minimumBid);
        this.minimumBid = minimumBid;
    }

    public BigDecimal getMinimumBid() {
        return minimumBid;
    }
}
