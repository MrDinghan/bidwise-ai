package com.bidwise.listing;

/**
 * Raised when an operation is not valid for the listing's current status
 * (e.g. publishing a listing that is not a draft). Maps to HTTP 409.
 */
public class IllegalListingStateException extends RuntimeException {

    public IllegalListingStateException(String message) {
        super(message);
    }
}
