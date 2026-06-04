package com.bidwise.listing;

/**
 * Raised when a user tries to modify a listing they do not own. Maps to HTTP 403.
 */
public class ListingAccessDeniedException extends RuntimeException {

    public ListingAccessDeniedException() {
        super("You do not own this listing");
    }
}
