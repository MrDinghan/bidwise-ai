package com.bidwise.bid;

/**
 * Raised when a bid targets a listing that is not open for bidding (not ACTIVE, or
 * its end time has already passed). Maps to HTTP 409.
 */
public class AuctionNotActiveException extends RuntimeException {

    public AuctionNotActiveException(String message) {
        super(message);
    }
}
