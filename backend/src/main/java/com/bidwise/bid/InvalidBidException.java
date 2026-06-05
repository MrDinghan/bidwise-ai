package com.bidwise.bid;

/**
 * Raised when a bid is invalid on its face — e.g. the seller bidding on their own
 * listing, or a non-positive amount. Maps to HTTP 400.
 */
public class InvalidBidException extends RuntimeException {

    public InvalidBidException(String message) {
        super(message);
    }
}
